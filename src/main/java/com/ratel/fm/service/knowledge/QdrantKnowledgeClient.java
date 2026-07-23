package com.ratel.fm.service.knowledge;

import com.ratel.fm.common.concurrent.NamedDaemonThreadFactory;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Qdrant 知识向量库客户端。
 */
@Component
public class QdrantKnowledgeClient {

    /** 向量检索响应直接带回 payload，单次召回数量必须克制，避免大内容分片撑爆响应体。 */
    private static final int MAX_SEARCH_LIMIT = 80;
    /** 关键词补召回通过 scroll 扫描 payload，按小批量读取可避免单次响应过大。 */
    private static final int DEFAULT_SCROLL_BATCH_LIMIT = 24;

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Semaphore requestSemaphore;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile long circuitOpenUntilMillis;

    public QdrantKnowledgeClient(AiProperties properties) {
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(
                Math.max(1, config().getQdrantExecutorThreads()),
                new NamedDaemonThreadFactory("qdrant-http-")
        );
        this.requestSemaphore = new Semaphore(Math.max(1, config().getQdrantMaxConcurrentRequests()));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(2, timeoutSeconds())))
                .executor(executor)
                .build();
    }

    /**
     * 判断当前向量库是否选择 Qdrant。
     */
    public boolean selected() {
        return "qdrant".equalsIgnoreCase(value(config().getVectorDatabaseProvider()));
    }

    /**
     * 重建 Qdrant 集合并写入全部知识分片。
     */
    public void replaceAll(List<KnowledgeDocument> documents) {
        if (!selected()) {
            return;
        }
        if (documents == null || documents.isEmpty()) {
            deleteCollection();
            return;
        }
        int vectorSize = firstVectorSize(documents);
        if (vectorSize <= 0) {
            throw qdrantException("Qdrant 模式需要可用的本地 embedding 向量，请先启动 Ollama 并下载 embedding 模型。", null);
        }
        deleteCollection();
        createCollection(vectorSize);
        upsert(documents);
    }

    /**
     * 写入或覆盖一批知识分片。
     */
    public void upsert(List<KnowledgeDocument> documents) {
        if (!selected() || documents == null || documents.isEmpty()) {
            return;
        }
        int vectorSize = firstVectorSize(documents);
        if (vectorSize <= 0) {
            throw qdrantException("Qdrant 模式需要可用的本地 embedding 向量，请先启动 Ollama 并下载 embedding 模型。", null);
        }
        createCollection(vectorSize);
        int batchSize = Math.max(1, config().getQdrantBatchSize());
        for (int start = 0; start < documents.size(); start += batchSize) {
            List<KnowledgeDocument> batch = documents.subList(start, Math.min(documents.size(), start + batchSize));
            JSONArray points = new JSONArray();
            for (KnowledgeDocument document : batch) {
                JSONObject point = toPoint(document);
                if (point != null) {
                    points.add(point);
                }
            }
            if (points.isEmpty()) {
                continue;
            }
            JSONObject payload = new JSONObject();
            payload.put("points", points);
            send("PUT", "/collections/" + collectionName() + "/points?wait=true", payload, false);
        }
        recordSuccess();
    }

    /**
     * 删除指定来源类型的知识分片。
     */
    public void deleteBySourceType(KnowledgeSourceType sourceType) {
        if (!selected() || sourceType == null) {
            return;
        }
        deleteByFilter(match("sourceType", sourceType.name()));
    }

    /**
     * 删除指定业务记录的知识分片。
     */
    public void deleteBySourceTypeAndSourceId(KnowledgeSourceType sourceType, Long sourceId) {
        if (!selected() || sourceType == null || sourceId == null) {
            return;
        }
        deleteByFilter(match("sourceType", sourceType.name()), match("sourceId", sourceId));
    }

    /**
     * 统计 Qdrant 中的知识分片数量。
     */
    public long count() {
        if (!selected()) {
            return 0;
        }
        JSONObject payload = new JSONObject();
        payload.put("exact", true);
        JSONObject response;
        try {
            response = send("POST", "/collections/" + collectionName() + "/points/count", payload, true);
        } catch (QdrantCollectionMissingException ex) {
            return 0;
        }
        JSONObject result = response.getJSONObject("result");
        return result == null ? 0 : Math.max(0, result.getLongValue("count"));
    }

    /**
     * 按向量召回知识分片。
     */
    public List<ScoredPoint> search(List<Double> vector, int limit) {
        if (!selected()) {
            return List.of();
        }
        if (vector == null || vector.isEmpty()) {
            throw qdrantException("Qdrant 检索需要查询向量，请先确认本地 embedding 模型可用。", null);
        }
        int requestLimit = Math.max(1, Math.min(MAX_SEARCH_LIMIT, limit));
        JSONObject response;
        while (true) {
            JSONObject payload = new JSONObject();
            payload.put("vector", toVector(vector));
            payload.put("limit", requestLimit);
            payload.put("with_payload", true);
            payload.put("with_vector", false);
            try {
                response = send("POST", "/collections/" + collectionName() + "/points/search", payload, true);
                break;
            } catch (QdrantCollectionMissingException ex) {
                return List.of();
            } catch (BusinessException ex) {
                if (responseTooLarge(ex) && requestLimit > 1) {
                    requestLimit = Math.max(1, requestLimit / 2);
                    continue;
                }
                throw ex;
            }
        }
        JSONArray results = response.getJSONArray("result");
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .filter(JSONObject.class::isInstance)
                .map(JSONObject.class::cast)
                .map(item -> new ScoredPoint(
                        value(item.getString("id")),
                        item.getDoubleValue("score"),
                        item.getJSONObject("payload") == null ? new JSONObject() : item.getJSONObject("payload")
                ))
                .toList();
    }

    /**
     * 分页读取 Qdrant payload，用于在 Qdrant 模式下补充关键词确定性召回。
     *
     * <p>实现步骤：
     * 1. 使用 `/points/scroll` 只读取 payload，不读取向量，控制单次响应大小；
     * 2. 按 `maxPoints` 限制最多扫描的分片数量，避免一次检索拖慢 Qdrant；
     * 3. 返回的数据仍在业务层执行权限、账套和关键词评分过滤，不回退 H2。</p>
     */
    public List<ScoredPoint> scrollPayloads(int maxPoints) {
        if (!selected() || maxPoints <= 0) {
            return List.of();
        }
        int remaining = Math.max(1, maxPoints);
        Object offset = null;
        java.util.ArrayList<ScoredPoint> points = new java.util.ArrayList<>();
        int batchLimit = Math.min(DEFAULT_SCROLL_BATCH_LIMIT, remaining);
        while (remaining > 0) {
            JSONObject payload = new JSONObject();
            payload.put("limit", Math.min(batchLimit, remaining));
            payload.put("with_payload", true);
            payload.put("with_vector", false);
            if (offset != null) {
                payload.put("offset", offset);
            }
            JSONObject response;
            try {
                response = send("POST", "/collections/" + collectionName() + "/points/scroll", payload, true);
            } catch (QdrantCollectionMissingException ex) {
                return List.of();
            } catch (BusinessException ex) {
                if (responseTooLarge(ex) && batchLimit > 1) {
                    batchLimit = Math.max(1, batchLimit / 2);
                    continue;
                }
                throw ex;
            }
            JSONObject result = response.getJSONObject("result");
            if (result == null) {
                break;
            }
            JSONArray batch = result.getJSONArray("points");
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (Object item : batch) {
                if (!(item instanceof JSONObject point)) {
                    continue;
                }
                points.add(new ScoredPoint(
                        value(point.getString("id")),
                        0D,
                        point.getJSONObject("payload") == null ? new JSONObject() : point.getJSONObject("payload")
                ));
            }
            remaining = maxPoints - points.size();
            offset = result.get("next_page_offset");
            if (offset == null) {
                break;
            }
        }
        return points;
    }

    private void deleteByFilter(JSONObject... conditions) {
        JSONArray must = new JSONArray();
        for (JSONObject condition : conditions) {
            must.add(condition);
        }
        JSONObject filter = new JSONObject();
        filter.put("must", must);
        JSONObject payload = new JSONObject();
        payload.put("filter", filter);
        try {
            send("POST", "/collections/" + collectionName() + "/points/delete?wait=true", payload, false);
        } catch (QdrantCollectionMissingException ex) {
            return;
        }
        recordSuccess();
    }

    private void deleteCollection() {
        try {
            send("DELETE", "/collections/" + collectionName(), null, false);
            recordSuccess();
        } catch (QdrantCollectionMissingException ex) {
            return;
        } catch (BusinessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (!message.contains("404")) {
                throw ex;
            }
        }
    }

    private void createCollection(int vectorSize) {
        JSONObject vectorConfig = new JSONObject();
        vectorConfig.put("size", vectorSize);
        vectorConfig.put("distance", "Cosine");
        JSONObject payload = new JSONObject();
        payload.put("vectors", vectorConfig);
        try {
            send("PUT", "/collections/" + collectionName(), payload, false);
            recordSuccess();
        } catch (BusinessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (!message.contains("409") && !message.contains("already exists")) {
                throw ex;
            }
        }
    }

    private JSONObject toPoint(KnowledgeDocument document) {
        if (document == null || document.getEmbeddingJson() == null) {
            return null;
        }
        JSONArray vector = parseVector(document.getEmbeddingJson());
        if (vector.isEmpty()) {
            return null;
        }
        JSONObject payload = new JSONObject();
        payload.put("sourceType", document.getSourceType() == null ? "" : document.getSourceType().name());
        payload.put("sourceId", document.getSourceId());
        payload.put("sourceNo", value(document.getSourceNo()));
        payload.put("title", value(document.getTitle()));
        payload.put("category", value(document.getCategory()));
        payload.put("summary", value(document.getSummary()));
        payload.put("content", value(document.getContent()));
        payload.put("metadata", value(document.getMetadata()));
        payload.put("permissionCode", document.getPermissionCode() == null ? "" : document.getPermissionCode().name());
        payload.put("organizationCode", value(document.getOrganizationCode()));
        payload.put("contentHash", value(document.getContentHash()));
        payload.put("embeddingModel", value(document.getEmbeddingModel()));
        payload.put("chunkIndex", document.getChunkIndex() == null ? 0 : document.getChunkIndex());

        JSONObject point = new JSONObject();
        point.put("id", pointId(document));
        point.put("vector", vector);
        point.put("payload", payload);
        return point;
    }

    private String pointId(KnowledgeDocument document) {
        String sourceType = document.getSourceType() == null ? "" : document.getSourceType().name();
        String sourceId = document.getSourceId() == null ? "" : document.getSourceId().toString();
        String chunkIndex = document.getChunkIndex() == null ? "0" : document.getChunkIndex().toString();
        String contentHash = value(document.getContentHash());
        String raw = String.join(":", "ratel-fm-knowledge", sourceType, sourceId, chunkIndex, contentHash);
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private JSONObject match(String key, Object value) {
        JSONObject match = new JSONObject();
        match.put("value", value);
        JSONObject condition = new JSONObject();
        condition.put("key", key);
        condition.put("match", match);
        return condition;
    }

    private JSONObject send(String method, String path, JSONObject payload, boolean expectBody) {
        if (circuitOpen()) {
            throw qdrantException("Qdrant 连续不可用，系统已短暂熔断，请稍后再试。", null);
        }
        boolean acquired = false;
        try {
            acquired = requestSemaphore.tryAcquire(Math.max(1, timeoutSeconds()), TimeUnit.SECONDS);
            if (!acquired) {
                throw qdrantException("当前 Qdrant 请求较多，请稍后再试。", null);
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + path))
                    .timeout(Duration.ofSeconds(Math.max(2, timeoutSeconds())));
            if (payload == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(payload.toJSONString(), StandardCharsets.UTF_8))
                        .header("Content-Type", "application/json");
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body();
            if (status < 200 || status >= 300) {
                if (collectionMissing(status, body)) {
                    throw collectionMissingException();
                }
                recordFailure();
                throw qdrantException("Qdrant HTTP " + status + ": " + truncate(body, 500), null);
            }
            if (body.length() > Math.max(1024, config().getQdrantMaxResponseChars())) {
                throw qdrantException("Qdrant 响应体过大，本次请求已中止。", null);
            }
            recordSuccess();
            return expectBody && !body.isBlank() ? JSONObject.parseObject(body) : new JSONObject();
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            recordFailure();
            throw qdrantException("Qdrant 请求被中断。", ex);
        } catch (Exception ex) {
            recordFailure();
            throw qdrantException("Qdrant 未启动或响应超时，请检查独立 Qdrant 服务。", ex);
        } finally {
            if (acquired) {
                requestSemaphore.release();
            }
        }
    }

    private int firstVectorSize(List<KnowledgeDocument> documents) {
        for (KnowledgeDocument document : documents) {
            if (document == null || document.getEmbeddingJson() == null) {
                continue;
            }
            int size = parseVector(document.getEmbeddingJson()).size();
            if (size > 0) {
                return size;
            }
        }
        return 0;
    }

    private JSONArray parseVector(String embeddingJson) {
        try {
            return JSONArray.parseArray(embeddingJson);
        } catch (RuntimeException ex) {
            return new JSONArray();
        }
    }

    private JSONArray toVector(List<Double> vector) {
        JSONArray array = new JSONArray();
        array.addAll(vector);
        return array;
    }

    private void recordSuccess() {
        consecutiveFailures.set(0);
    }

    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= Math.max(1, config().getQdrantFailureThreshold())) {
            circuitOpenUntilMillis = System.currentTimeMillis() + Math.max(1, config().getQdrantCircuitBreakerSeconds()) * 1000L;
            consecutiveFailures.set(0);
        }
    }

    private boolean circuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntilMillis;
    }

    private String baseUrl() {
        return value(config().getQdrantBaseUrl()).replaceAll("/+$", "");
    }

    private String collectionName() {
        return value(config().getQdrantCollectionName());
    }

    private int timeoutSeconds() {
        return Math.max(2, config().getQdrantRequestTimeoutSeconds());
    }

    private AiProperties.Knowledge config() {
        return properties.getKnowledge() == null ? new AiProperties.Knowledge() : properties.getKnowledge();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean collectionMissing(int status, String body) {
        if (status != 404) {
            return false;
        }
        String normalizedBody = value(body);
        String collection = collectionName();
        return normalizedBody.contains("Not found: Collection")
                || normalizedBody.contains("Collection `" + collection + "`")
                || (normalizedBody.contains("Collection `") && normalizedBody.contains("doesn't exist"));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean responseTooLarge(BusinessException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        return message.contains("响应体过大");
    }

    private QdrantCollectionMissingException collectionMissingException() {
        return new QdrantCollectionMissingException("Qdrant 知识集合尚未创建，请先重建 AI 知识索引。");
    }

    private BusinessException qdrantException(String message, Throwable cause) {
        BusinessException exception = new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR, message);
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    public record ScoredPoint(String id, double score, JSONObject payload) {
    }

    private static final class QdrantCollectionMissingException extends BusinessException {

        private QdrantCollectionMissingException(String message) {
            super(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR, message);
        }
    }
}
