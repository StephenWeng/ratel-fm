package com.ratel.fm.service.ai;

import com.ratel.fm.common.concurrent.NamedDaemonThreadFactory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 本地 Ollama 聊天模型客户端。
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    /**
     * 字段 properties：保存 AI 配置，供客户端读取本地模型地址、模型名称和保护参数。
     */
    private final AiProperties properties;
    /**
     * 字段 httpClient：保存 Java HTTP 客户端，用于访问本地 Ollama 服务。
     */
    private final HttpClient httpClient;
    /**
     * 字段 executor：Ollama HTTP 客户端专用线程池，避免使用默认线程池造成不可控堆积。
     */
    private final ExecutorService executor;
    /**
     * 字段 requestSemaphore：本地模型请求并发闸门，保护笔记本 CPU、内存和模型推理进程。
     */
    private final Semaphore requestSemaphore;
    /**
     * 字段 consecutiveFailures：记录连续失败次数，用于触发短时熔断。
     */
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    /**
     * 字段 circuitOpenUntilMillis：熔断截止时间戳；当前时间小于该值时直接跳过 Ollama 调用。
     */
    private volatile long circuitOpenUntilMillis;
    /**
     * 字段 modelProbeCheckedAtMillis：记录最近一次本地模型列表探测时间，避免每次提问重复访问 /api/tags。
     */
    private volatile long modelProbeCheckedAtMillis;
    /**
     * 字段 cachedModels：缓存当前 Ollama 服务已下载的模型名称列表。
     */
    private volatile List<String> cachedModels = List.of();

    /**
     * 构造 OllamaClient 实例。
     *
     * <p>实现步骤：
     * 1. 保存 AI 配置；
     * 2. 创建固定大小守护线程池；
     * 3. 创建带连接超时的 HTTP 客户端和并发闸门。</p>
     */
    public OllamaClient(AiProperties properties) {
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(
                Math.max(1, properties.getOllama().getExecutorThreads()),
                new NamedDaemonThreadFactory("ollama-http-")
        );
        this.requestSemaphore = new Semaphore(Math.max(1, properties.getOllama().getMaxConcurrentRequests()));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(2, timeoutSeconds())))
                .executor(executor)
                .build();
    }

    /**
     * 判断当前配置是否允许尝试 Ollama。
     *
     * <p>实现步骤：检查开关、模型名称、服务地址和熔断状态；再用短时缓存探测 /api/tags，确保服务已启动且模型已下载。</p>
     */
    public boolean available() {
        return properties.getOllama().isEnabled()
                && !model().isBlank()
                && !baseUrl().isBlank()
                && !circuitOpen()
                && !availableModels().isEmpty();
    }

    /**
     * 获取当前 Ollama 聊天模型名称。
     *
     * <p>实现步骤：读取配置；为空时返回空字符串，让调用方显示为未配置本地模型。</p>
     */
    public String chatModel() {
        return model();
    }

    /**
     * 获取语音命令和短指令优先使用的模型名称。
     *
     * <p>实现步骤：读取 app.ai.ollama.command-model；为空时回退默认聊天模型。</p>
     */
    public String commandModel() {
        return normalizedModel(properties.getOllama().getCommandModel(), model());
    }

    /**
     * 获取复杂推理优先使用的模型名称。
     *
     * <p>实现步骤：读取 app.ai.ollama.reasoning-model；为空时回退默认聊天模型。</p>
     */
    public String reasoningModel() {
        return normalizedModel(properties.getOllama().getReasoningModel(), model());
    }

    /**
     * 获取本地视觉模型名称。
     *
     * <p>实现步骤：读取 app.ai.ollama.vision-model；未配置时回退 qwen2.5vl:7b，作为本地 OCR 默认视觉模型。</p>
     */
    public String visionModel() {
        return normalizedModel(properties.getOllama().getVisionModel(), "qwen2.5vl:7b");
    }

    /**
     * 获取本地 embedding 模型名称。
     *
     * <p>实现步骤：读取 app.ai.ollama.embedding-model；为空时返回默认 bge-m3:latest。</p>
     */
    public String embeddingModel() {
        return normalizedModel(properties.getOllama().getEmbeddingModel(), "bge-m3:latest");
    }

    /**
     * 返回当前配置的 Ollama HTTP 地址，用于状态页展示和部署排查。
     */
    public String baseUrlForDisplay() {
        return baseUrl();
    }

    /**
     * 判断指定模型是否已经下载。
     *
     * <p>实现步骤：读取模型列表缓存并做忽略大小写匹配，供业务层展示当前实际路由模型。</p>
     */
    public boolean hasModel(String modelName) {
        return !resolveModel(List.of(modelName)).isBlank();
    }

    /**
     * 判断候选模型中是否至少有一个可用于当前场景。
     *
     * <p>实现步骤：读取 Ollama 已下载模型列表；逐个匹配候选模型；命中任意模型即认为该业务场景可调用。</p>
     */
    public boolean hasAnyModel(List<String> modelNames) {
        if (!properties.getOllama().isEnabled() || baseUrl().isBlank() || circuitOpen()) {
            return false;
        }
        List<String> models = availableModels();
        if (models.isEmpty()) {
            return false;
        }
        for (String expected : modelNames == null ? List.<String>of() : modelNames) {
            String normalized = normalizedModel(expected, "");
            if (normalized.isBlank()) {
                continue;
            }
            for (String available : models) {
                if (available.equalsIgnoreCase(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回当前 Ollama 服务已下载模型名称列表。
     *
     * <p>实现步骤：复用内部 `/api/tags` 探测缓存，只暴露不可变副本，供状态页完整展示本地模型清单。</p>
     */
    public List<String> installedModels() {
        if (!properties.getOllama().isEnabled() || baseUrl().isBlank() || circuitOpen()) {
            return List.of();
        }
        return List.copyOf(availableModels());
    }

    /**
     * 判断本地 embedding 模型是否可用。
     *
     * <p>实现步骤：检查 Ollama 开关、服务地址、熔断状态和模型列表，避免索引阶段误调用未下载模型。</p>
     */
    public boolean embeddingAvailable() {
        return properties.getOllama().isEnabled()
                && !embeddingModel().isBlank()
                && !baseUrl().isBlank()
                && !circuitOpen()
                && availableModels().stream().anyMatch(item -> item.equalsIgnoreCase(embeddingModel()));
    }

    /**
     * 判断本地视觉模型是否可用。
     *
     * <p>实现步骤：检查 Ollama 开关、服务地址、熔断状态和模型列表，避免 OCR 阶段误调用未下载模型。</p>
     */
    public boolean visionAvailable() {
        return properties.getOllama().isEnabled()
                && !visionModel().isBlank()
                && !baseUrl().isBlank()
                && !circuitOpen()
                && availableModels().stream().anyMatch(item -> item.equalsIgnoreCase(visionModel()));
    }

    /**
     * 使用本地 Ollama 生成文本向量。
     *
     * <p>实现步骤：调用 /api/embed；若运行时是旧版 Ollama 不支持该接口，则回退 /api/embeddings。</p>
     */
    public List<Double> embedding(String text) {
        if (!embeddingAvailable()) {
            return List.of();
        }
        JSONObject payload = new JSONObject();
        payload.put("model", embeddingModel());
        payload.put("input", text == null ? "" : text);
        try {
            JSONObject response = post("/api/embed", payload);
            List<Double> embedding = parseEmbedding(response);
            if (!embedding.isEmpty()) {
                return embedding;
            }
        } catch (RuntimeException ignored) {
            // Fall back to the legacy endpoint below.
        }
        JSONObject legacyPayload = new JSONObject();
        legacyPayload.put("model", embeddingModel());
        legacyPayload.put("prompt", text == null ? "" : text);
        return parseEmbedding(post("/api/embeddings", legacyPayload));
    }

    /**
     * 按业务场景解析本次实际使用的模型。
     *
     * <p>实现步骤：按候选顺序查找已下载模型；命中失败时降级到任一已下载模型；仍失败时返回空字符串。</p>
     */
    public String resolveModel(List<String> preferredModels) {
        return resolveAvailableModel(preferredModels);
    }

    /**
     * 执行本地模型聊天。
     *
     * <p>实现步骤：
     * 1. 未启用或熔断时返回空字符串；
     * 2. 组装 Ollama /api/chat 请求体；
     * 3. 发送非流式请求并返回 message.content。</p>
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(List.of(model()), systemPrompt, userPrompt);
    }

    /**
     * 使用候选模型列表执行本地模型聊天。
     *
     * <p>实现步骤：
     * 1. 按候选模型顺序选择已下载模型；
     * 2. 候选均不存在时降级到任一已下载模型；
     * 3. 使用最终模型名称写入 /api/chat 请求体。</p>
     */
    public String chat(List<String> preferredModels, String systemPrompt, String userPrompt) {
        if (!available()) {
            return "";
        }
        String selectedModel = resolveAvailableModel(preferredModels);
        if (selectedModel.isBlank()) {
            return "";
        }

        /**
         * 变量 messages：保存发送给 Ollama 的 system 和 user 消息集合。
         */
        JSONArray messages = chatMessages(systemPrompt, userPrompt);

        /**
         * 变量 payload：保存 Ollama /api/chat 请求体。
         */
        JSONObject payload = new JSONObject();
        payload.put("model", selectedModel);
        payload.put("messages", messages);
        payload.put("stream", false);
        payload.put("options", options(0.2));
        applyKeepAlive(payload);

        /**
         * 变量 response：保存 Ollama 响应 JSON。
         */
        JSONObject response = post("/api/chat", payload);
        /**
         * 变量 message：保存响应中的 assistant 消息对象。
         */
        JSONObject message = response.getJSONObject("message");
        return message == null ? "" : message.getString("content");
    }

    /**
     * 使用本地 Ollama 视觉模型识别图片或结构化提取文本。
     */
    public String vision(String systemPrompt, String userPrompt, List<QwenClient.VisionInput> inputs) {
        if (!visionAvailable()) {
            return "";
        }
        JSONArray messages = new JSONArray();
        messages.add(message("system", systemPrompt));
        JSONObject userMessage = message("user", userPrompt);
        JSONArray images = new JSONArray();
        if (inputs != null) {
            StringBuilder textBuilder = new StringBuilder(userMessage.getString("content"));
            for (QwenClient.VisionInput input : inputs) {
                if (input == null) {
                    continue;
                }
                if (input.imageDataUrl() != null && !input.imageDataUrl().isBlank()) {
                    String imageBase64 = imageBase64(input.imageDataUrl());
                    if (!imageBase64.isBlank()) {
                        images.add(imageBase64);
                    }
                } else if (input.text() != null && !input.text().isBlank()) {
                    textBuilder.append("\n\n文件：").append(input.fileName()).append('\n').append(input.text());
                }
            }
            userMessage.put("content", truncate(textBuilder.toString(), maxPromptChars()));
        }
        if (!images.isEmpty()) {
            userMessage.put("images", images);
        }
        messages.add(userMessage);

        JSONObject payload = new JSONObject();
        payload.put("model", visionModel());
        payload.put("messages", messages);
        payload.put("stream", false);
        payload.put("options", options(0.1));
        applyKeepAlive(payload);

        JSONObject response = post("/api/chat", payload);
        JSONObject message = response.getJSONObject("message");
        return message == null ? "" : message.getString("content");
    }

    /**
     * 使用候选模型列表执行流式聊天。
     *
     * <p>实现步骤：
     * 1. 复用本地模型可用性、模型路由、熔断和并发闸门；
     * 2. 请求 Ollama /api/chat stream=true；
     * 3. 逐行解析 NDJSON，只把增量内容交给调用方；
     * 4. 取消或浏览器断开时关闭响应输入流，避免 Ollama 继续生成并占用本机资源。</p>
     */
    public String chatStream(
            List<String> preferredModels,
            String systemPrompt,
            String userPrompt,
            Consumer<String> contentConsumer,
            AiStreamCancellation cancellation,
            int captureChars
    ) {
        if (!available()) {
            return "";
        }
        String selectedModel = resolveAvailableModel(preferredModels);
        if (selectedModel.isBlank()) {
            return "";
        }
        JSONArray messages = chatMessages(systemPrompt, userPrompt);

        JSONObject payload = new JSONObject();
        payload.put("model", selectedModel);
        payload.put("messages", messages);
        payload.put("stream", true);
        payload.put("options", options(0.2));
        applyKeepAlive(payload);

        return postStream("/api/chat", payload, contentConsumer, cancellation, captureChars);
    }

    /**
     * 构造 Ollama 推理参数。
     *
     * <p>实现步骤：统一写入 temperature、num_predict 和 num_ctx，避免不同调用入口参数不一致导致响应过慢。</p>
     */
    private JSONObject options(double temperature) {
        JSONObject options = new JSONObject();
        options.put("temperature", temperature);
        options.put("num_predict", maxOutputTokens());
        options.put("num_ctx", contextWindowTokens());
        return options;
    }

    /**
     * 写入 Ollama 模型常驻配置。
     *
     * <p>实现步骤：keep_alive 为空时不写入，非空时交给 Ollama 原生解析，减少连续请求的模型装载耗时。</p>
     */
    private void applyKeepAlive(JSONObject payload) {
        String keepAlive = keepAlive();
        if (!keepAlive.isBlank()) {
            payload.put("keep_alive", keepAlive);
        }
    }

    /**
     * 构造 Ollama 消息对象。
     *
     * <p>实现步骤：设置 role 和截断后的 content，避免本地模型收到过长上下文。</p>
     */
    private JSONObject message(String role, String content) {
        /**
         * 变量 message：保存发送给 Ollama 的单条消息。
         */
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", truncate(content, maxPromptChars()));
        return message;
    }

    /**
     * 按上下文窗口为 system 与 user 消息分配统一字符预算。
     * 中文字符通常接近一个 token，因此必须为模型输出和消息封装预留空间，不能对两条消息分别套用最大值。
     */
    private JSONArray chatMessages(String systemPrompt, String userPrompt) {
        int tokenBudget = Math.max(1800, contextWindowTokens() - maxOutputTokens() - 384);
        int totalCharBudget = Math.min(maxPromptChars(), tokenBudget);
        int systemBudget = Math.min(Math.max(400, totalCharBudget / 4), 900);
        String system = truncate(systemPrompt, systemBudget);
        int userBudget = Math.max(1200, totalCharBudget - system.length());
        JSONArray messages = new JSONArray();
        messages.add(message("system", system));
        messages.add(message("user", truncate(userPrompt, userBudget)));
        return messages;
    }

    /**
     * 从 data URL 中提取 Ollama images 字段需要的纯 base64。
     */
    private String imageBase64(String dataUrl) {
        String value = dataUrl == null ? "" : dataUrl.trim();
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(comma + 1).trim() : value;
    }

    /**
     * 解析 Ollama embedding 响应。
     */
    private List<Double> parseEmbedding(JSONObject response) {
        if (response == null || response.isEmpty()) {
            return List.of();
        }
        JSONArray embedding = response.getJSONArray("embedding");
        if (embedding == null) {
            JSONArray embeddings = response.getJSONArray("embeddings");
            if (embeddings != null && !embeddings.isEmpty()) {
                Object first = embeddings.get(0);
                if (first instanceof JSONArray array) {
                    embedding = array;
                }
            }
        }
        if (embedding == null || embedding.isEmpty()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>(embedding.size());
        for (int i = 0; i < embedding.size(); i++) {
            values.add(embedding.getDoubleValue(i));
        }
        return values;
    }

    /**
     * 发送 Ollama HTTP POST 请求。
     *
     * <p>实现步骤：
     * 1. 熔断打开时直接抛出业务异常；
     * 2. 获取并发许可，避免本机模型过载；
     * 3. 发送请求、检查状态码和响应大小；
     * 4. 成功时解析 JSON，失败时记录失败并抛出统一异常。</p>
     */
    private JSONObject post(String path, JSONObject payload) {
        if (circuitOpen()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ResponseCode.LOAD_CLIENT_ERROR,
                    "Ollama 模型服务连续不可用，系统已短暂熔断，请稍后再试。");
        }
        boolean acquired = false;
        try {
            acquired = requestSemaphore.tryAcquire(Math.max(1, timeoutSeconds()), TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, ResponseCode.LOAD_CLIENT_ERROR,
                        "当前本地 AI 请求较多，请稍后再试。");
            }
            /**
             * 变量 request：保存发往 Ollama 的 HTTP 请求。
             */
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString()))
                    .build();
            /**
             * 变量 response：保存 Ollama HTTP 响应。
             */
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                recordFailure();
                throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                        "Ollama 模型服务暂时不可用，请确认配置地址可访问、服务已启动且模型已下载。");
            }
            /**
             * 变量 body：保存响应体文本，用于大小检查和 JSON 解析。
             */
            String body = response.body() == null ? "" : response.body();
            if (body.length() > maxResponseChars()) {
                recordFailure();
                throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                        "Ollama 模型服务返回内容过大，本次回答已中止。");
            }
            recordSuccess();
            return JSON.parseObject(body);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            recordFailure();
            log.warn("Ollama request failed: baseUrl={}, path={}, reason={}", baseUrl(), path, ex.getMessage(), ex);
            if (isTimeout(ex)) {
                throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, ResponseCode.LOAD_CLIENT_ERROR,
                        "Ollama 模型已连接，但推理超过 " + timeoutSeconds()
                                + " 秒。请降低并发或上下文长度、释放 CPU/内存资源，或适当增大 FM_AI_OLLAMA_TIMEOUT_SECONDS 后重试。");
            }
            throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                    "无法连接 Ollama 模型服务，请检查配置地址、监听地址、防火墙和服务进程。");
        } finally {
            if (acquired) {
                requestSemaphore.release();
            }
        }
    }

    /**
     * 发送 Ollama 流式 HTTP POST 请求。
     */
    private String postStream(
            String path,
            JSONObject payload,
            Consumer<String> contentConsumer,
            AiStreamCancellation cancellation,
            int captureChars
    ) {
        if (circuitOpen()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ResponseCode.LOAD_CLIENT_ERROR,
                    "Ollama 模型服务连续不可用，系统已短暂熔断，请稍后再试。");
        }
        boolean acquired = false;
        AtomicReference<InputStream> responseStream = new AtomicReference<>();
        AtomicReference<CompletableFuture<HttpResponse<InputStream>>> responseFutureRef = new AtomicReference<>();
        try {
            acquired = requestSemaphore.tryAcquire(Math.max(1, timeoutSeconds()), TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, ResponseCode.LOAD_CLIENT_ERROR,
                        "当前本地 AI 请求较多，请稍后再试。");
            }
            cancellation.throwIfCancelled();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString()))
                    .build();
            CompletableFuture<HttpResponse<InputStream>> responseFuture =
                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
            responseFutureRef.set(responseFuture);
            cancellation.onCancel(() -> {
                CompletableFuture<HttpResponse<InputStream>> future = responseFutureRef.get();
                if (future != null) {
                    future.cancel(true);
                }
                closeQuietly(responseStream.get());
            });
            HttpResponse<InputStream> response = responseFuture.get(timeoutSeconds(), TimeUnit.SECONDS);
            InputStream bodyStream = response.body();
            responseStream.set(bodyStream);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                closeQuietly(bodyStream);
                recordFailure();
                throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                        "Ollama 模型服务暂时不可用，请确认配置地址可访问、服务已启动且模型已下载。");
            }
            BoundedTextCapture capture = new BoundedTextCapture(captureChars);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    cancellation.throwIfCancelled();
                    readStreamLine(line, contentConsumer, capture);
                }
            }
            recordSuccess();
            return capture.text();
        } catch (AiStreamCancelledException ex) {
            throw ex;
        } catch (CancellationException ex) {
            throw new AiStreamCancelledException();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            recordFailure();
            if (isTimeout(ex)) {
                throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, ResponseCode.LOAD_CLIENT_ERROR,
                        "Ollama 模型已连接，但流式推理超过 " + timeoutSeconds()
                                + " 秒。请降低并发或上下文长度、释放 CPU/内存资源，或适当增大 FM_AI_OLLAMA_TIMEOUT_SECONDS 后重试。");
            }
            throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                    "无法连接 Ollama 模型服务，请检查配置地址、监听地址、防火墙和服务进程。");
        } finally {
            CompletableFuture<HttpResponse<InputStream>> future = responseFutureRef.get();
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            closeQuietly(responseStream.get());
            if (acquired) {
                requestSemaphore.release();
            }
        }
    }

    /** 判断异常链是否由 HTTP 或异步等待超时引起。 */
    private boolean isTimeout(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof HttpTimeoutException || cursor instanceof TimeoutException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    /**
     * 解析 Ollama NDJSON 流中的一行。
     */
    private void readStreamLine(String line, Consumer<String> contentConsumer, BoundedTextCapture capture) {
        if (line == null || line.isBlank()) {
            return;
        }
        JSONObject chunk = JSON.parseObject(line);
        if (Boolean.TRUE.equals(chunk.getBoolean("done"))) {
            return;
        }
        JSONObject message = chunk.getJSONObject("message");
        String content = message == null ? "" : message.getString("content");
        if (content == null || content.isBlank()) {
            return;
        }
        capture.append(content);
        contentConsumer.accept(content);
    }

    /**
     * 关闭输入流，忽略关闭异常。
     */
    private void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取规范化后的 Ollama 服务地址。
     *
     * <p>实现步骤：读取配置；为空时回退默认局域网模型服务器地址；末尾斜杠会被移除，便于拼接接口路径。</p>
     */
    private String baseUrl() {
        /**
         * 变量 value：保存配置中的 Ollama 服务地址。
         */
        String value = properties.getOllama().getBaseUrl();
        if (value == null || value.isBlank()) {
            value = "http://10.105.12.136:11434";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * 获取规范化后的 Ollama 模型名称。
     *
     * <p>实现步骤：读取配置；为空时返回空字符串，避免无模型名时访问本地接口。</p>
     */
    private String model() {
        /**
         * 变量 value：保存配置中的 Ollama 模型名称。
         */
        String value = properties.getOllama().getChatModel();
        return value == null ? "" : value.trim();
    }

    /**
     * 判断 Ollama 是否处于短时熔断状态。
     *
     * <p>实现步骤：读取熔断截止时间并与当前时间比较，未过冷却期时返回 true。</p>
     */
    private boolean circuitOpen() {
        return circuitOpenUntilMillis > System.currentTimeMillis();
    }

    /**
     * 记录 Ollama 请求成功。
     *
     * <p>实现步骤：清空连续失败次数和熔断截止时间，让后续请求继续优先走本地模型。</p>
     */
    private void recordSuccess() {
        consecutiveFailures.set(0);
        circuitOpenUntilMillis = 0L;
        modelProbeCheckedAtMillis = System.currentTimeMillis();
    }

    /**
     * 记录 Ollama 请求失败并在达到阈值时打开熔断。
     *
     * <p>实现步骤：连续失败计数加一；达到配置阈值时按冷却秒数设置熔断截止时间。</p>
     */
    private void recordFailure() {
        /**
         * 变量 failures：保存本地模型连续失败次数。
         */
        int failures = consecutiveFailures.incrementAndGet();
        modelProbeCheckedAtMillis = 0L;
        cachedModels = List.of();
        /**
         * 变量 threshold：保存触发熔断的失败次数阈值。
         */
        int threshold = Math.max(1, properties.getOllama().getFailureThreshold());
        if (failures >= threshold) {
            circuitOpenUntilMillis = System.currentTimeMillis()
                    + Duration.ofSeconds(Math.max(5, properties.getOllama().getCircuitBreakerSeconds())).toMillis();
        }
    }

    /**
     * 获取请求超时时间。
     *
     * <p>实现步骤：读取配置并设置最小 2 秒，避免本地服务未启动时长时间阻塞页面。</p>
     */
    private int timeoutSeconds() {
        return Math.max(2, properties.getOllama().getRequestTimeoutSeconds());
    }

    /**
     * 获取模型最大输出 token 数。
     *
     * <p>实现步骤：读取配置并设置最小 128，避免回答过短影响业务使用。</p>
     */
    private int maxOutputTokens() {
        return Math.max(128, properties.getOllama().getMaxOutputTokens());
    }

    /**
     * 获取 Ollama 上下文窗口 token 数。
     *
     * <p>实现步骤：读取配置并限制最小 2048，避免误配置过小影响业务问答和知识引用。</p>
     */
    private int contextWindowTokens() {
        return Math.max(2048, properties.getOllama().getContextWindowTokens());
    }

    /**
     * 获取模型常驻时间。
     *
     * <p>实现步骤：读取配置并去除首尾空白，空值表示不覆盖 Ollama 默认行为。</p>
     */
    private String keepAlive() {
        String value = properties.getOllama().getKeepAlive();
        return value == null ? "" : value.trim();
    }

    /**
     * 获取提示词最大字符数。
     *
     * <p>实现步骤：读取配置并设置最小 3000，保留必要的业务上下文和引用资料。</p>
     */
    private int maxPromptChars() {
        return Math.max(3000, properties.getOllama().getMaxPromptChars());
    }

    /**
     * 获取模型响应最大字符数。
     *
     * <p>实现步骤：读取配置并设置最小 10000，避免正常中文回答被误判为过大。</p>
     */
    private int maxResponseChars() {
        return Math.max(10000, properties.getOllama().getMaxResponseChars());
    }

    /**
     * 读取当前 Ollama 服务已下载模型。
     *
     * <p>实现步骤：
     * 1. 10 秒内复用上次探测结果，避免连续提问时频繁访问本地端口；
     * 2. 缓存过期后调用 /api/tags；
     * 3. 返回模型名称列表，业务层按场景自行选择最合适模型。</p>
     */
    private List<String> availableModels() {
        /**
         * 变量 now：保存当前时间戳，用于判断模型列表探测缓存是否过期。
         */
        long now = System.currentTimeMillis();
        if (now - modelProbeCheckedAtMillis < 10_000L) {
            return cachedModels;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - modelProbeCheckedAtMillis < 10_000L) {
                return cachedModels;
            }
            cachedModels = probeAvailableModels();
            modelProbeCheckedAtMillis = now;
            return cachedModels;
        }
    }

    /**
     * 按候选模型列表解析可用模型。
     *
     * <p>实现步骤：
     * 1. 读取当前已下载模型列表；
     * 2. 对候选模型按顺序进行忽略大小写匹配；
     * 3. 候选均不可用时返回第一个已下载模型作为兜底。</p>
     */
    private String resolveAvailableModel(List<String> preferredModels) {
        List<String> models = availableModels();
        if (models.isEmpty()) {
            return "";
        }
        for (String preferred : preferredModels == null ? List.<String>of() : preferredModels) {
            String normalized = normalizedModel(preferred, "");
            if (normalized.isBlank()) {
                continue;
            }
            for (String available : models) {
                if (available.equalsIgnoreCase(normalized)) {
                    return available;
                }
            }
        }
        return models.getFirst();
    }

    /**
     * 调用 Ollama 模型列表接口读取已下载模型。
     *
     * <p>实现步骤：
     * 1. 调用 /api/tags 获取本地模型列表；
     * 2. 解析 models[].name；
     * 3. 返回非空模型名称列表，供模型路由选择。</p>
     */
    private List<String> probeAvailableModels() {
        try {
            /**
             * 变量 request：保存 Ollama 模型列表请求。
             */
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(Math.min(3, timeoutSeconds())))
                    .GET()
                    .build();
            /**
             * 变量 response：保存 Ollama 模型列表响应。
             */
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            /**
             * 变量 body：保存解析后的模型列表 JSON。
             */
            JSONObject body = JSON.parseObject(response.body() == null ? "{}" : response.body());
            JSONArray models = body.getJSONArray("models");
            if (models == null || models.isEmpty()) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            for (int i = 0; i < models.size(); i++) {
                JSONObject item = models.getJSONObject(i);
                String name = item == null ? "" : item.getString("name");
                if (name != null && !name.isBlank()) {
                    names.add(name.trim());
                }
            }
            return names;
        } catch (Exception ex) {
            log.warn("Ollama model probe failed: baseUrl={}, reason={}", baseUrl(), ex.getMessage());
            return List.of();
        }
    }

    /**
     * 规范化模型名称。
     *
     * <p>实现步骤：空值使用兜底值；非空值去除首尾空白，避免环境变量多空格导致匹配失败。</p>
     */
    private String normalizedModel(String value, String fallback) {
        String selected = value == null || value.isBlank() ? fallback : value;
        return selected == null ? "" : selected.trim();
    }

    /**
     * 按最大字符数截断文本。
     *
     * <p>实现步骤：
     * 1. 空值兜底为空字符串；
     * 2. 文本长度在限制内直接返回；
     * 3. 超出限制时保留前半段和后半段，避免关键单号只出现在结尾时丢失。</p>
     */
    private String truncate(String value, int maxChars) {
        /**
         * 变量 text：保存待截断文本。
         */
        String text = value == null ? "" : value;
        if (text.length() <= maxChars) {
            return text;
        }
        /**
         * 变量 half：保存前后两段各自保留的字符数。
         */
        int half = Math.max(1, maxChars / 2);
        return text.substring(0, half)
                + "\n\n[上下文过长，已截断中间内容]\n\n"
                + text.substring(text.length() - half);
    }

}
