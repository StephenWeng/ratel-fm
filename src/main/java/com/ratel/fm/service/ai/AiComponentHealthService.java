package com.ratel.fm.service.ai;

import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.repository.knowledge.KnowledgeDocumentRepository;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.service.knowledge.QdrantKnowledgeClient;
import com.ratel.fm.web.dto.ai.AiStatusDtos.AiComponentStatusItem;
import com.ratel.fm.web.dto.ai.AiStatusDtos.AiComponentStatusResponse;
import com.ratel.fm.web.dto.ai.AiStatusDtos.AiKnowledgeSourceCount;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * AI 组件健康状态服务。
 *
 * <p>集中检查大模型、embedding、向量库和知识索引状态，避免故障排查分散在日志、浏览器控制台和配置文件中。</p>
 */
@Service
public class AiComponentHealthService {

    private final AiProperties aiProperties;
    private final LargeModelRouter largeModelRouter;
    private final OllamaClient ollamaClient;
    private final QwenClient qwenClient;
    private final QdrantKnowledgeClient qdrantKnowledgeClient;
    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public AiComponentHealthService(
            AiProperties aiProperties,
            LargeModelRouter largeModelRouter,
            OllamaClient ollamaClient,
            QwenClient qwenClient,
            QdrantKnowledgeClient qdrantKnowledgeClient,
            KnowledgeIndexService knowledgeIndexService,
            KnowledgeDocumentRepository knowledgeDocumentRepository
    ) {
        this.aiProperties = aiProperties;
        this.largeModelRouter = largeModelRouter;
        this.ollamaClient = ollamaClient;
        this.qwenClient = qwenClient;
        this.qdrantKnowledgeClient = qdrantKnowledgeClient;
        this.knowledgeIndexService = knowledgeIndexService;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    /**
     * 汇总当前 AI 组件状态。
     */
    public AiComponentStatusResponse status() {
        List<AiComponentStatusItem> components = new ArrayList<>();
        components.add(modelStatus());
        components.add(ollamaStatus());
        components.add(qwenStatus());
        components.add(visionStatus());
        components.add(embeddingStatus());
        components.add(vectorStoreStatus());
        components.add(indexStatus());
        components.add(streamStatus());
        components.add(agentStatus());

        String vectorProvider = normalize(aiProperties.getKnowledge().getVectorDatabaseProvider(), "qdrant");
        long indexCount = safeIndexCount();
        return new AiComponentStatusResponse(
                largeModelRouter.selectedProviderCode(),
                vectorProvider,
                safePrimaryModel(),
                ollamaClient.embeddingModel(),
                aiProperties.getAssistant().isStreamEnabled(),
                aiProperties.getAgent().isEnabled(),
                indexCount,
                knowledgeIndexService.lastRebuildAt(),
                knowledgeIndexService.lastRebuildError(),
                OffsetDateTime.now(),
                components,
                sourceTypeCounts(vectorProvider)
        );
    }

    private AiComponentStatusItem modelStatus() {
        String provider = largeModelRouter.selectedProviderCode();
        boolean chatAvailable = largeModelRouter.available(AiModelUseCase.CHAT);
        boolean commandAvailable = largeModelRouter.available(AiModelUseCase.COMMAND);
        boolean reasoningAvailable = largeModelRouter.available(AiModelUseCase.REASONING);
        String detail = provider
                + " / 聊天模型：" + safePrimaryModel(AiModelUseCase.CHAT)
                + "（" + statusText(chatAvailable) + "）"
                + "，指令模型：" + safePrimaryModel(AiModelUseCase.COMMAND)
                + "（" + statusText(commandAvailable) + "）"
                + "，推理模型：" + safePrimaryModel(AiModelUseCase.REASONING)
                + "（" + statusText(reasoningAvailable) + "）";
        if (chatAvailable || commandAvailable || reasoningAvailable) {
            return item("large-model", "大模型路由", "UP", detail);
        }
        return item("large-model", "大模型路由", "DOWN", "当前选择 " + provider + "，但各业务场景模型均不可用。");
    }

    private AiComponentStatusItem ollamaStatus() {
        if (!aiProperties.getOllama().isEnabled()) {
            return item("ollama", "Ollama 模型服务", "DISABLED", "配置已关闭 Ollama。");
        }
        if (ollamaClient.available()) {
            return item("ollama", "Ollama 模型服务", "UP", "服务可用：" + ollamaClient.baseUrlForDisplay() + "，聊天模型：" + ollamaClient.chatModel());
        }
        return item("ollama", "Ollama 模型服务", "WARN",
                "未检测到可用模型服务：" + ollamaClient.baseUrlForDisplay()
                        + "。请确认 Ollama 已启动、监听内网地址、模型已下载，并放行 TCP 11434。");
    }

    private AiComponentStatusItem qwenStatus() {
        if (!aiProperties.getQwen().isEnabled()) {
            return item("qwen", "千问云端模型", "DISABLED", "配置已关闭千问。");
        }
        if (qwenClient.available()) {
            return item("qwen", "千问云端模型", "UP", "API Key 已配置，聊天模型：" + qwenClient.chatModel());
        }
        return item("qwen", "千问云端模型", "WARN", "未配置可用 API Key，选择 qwen provider 时将不可用。");
    }

    private AiComponentStatusItem visionStatus() {
        if (ollamaClient.visionAvailable()) {
            return item("local-vision", "本地视觉/OCR模型", "UP", "模型可用：" + ollamaClient.visionModel());
        }
        if (qwenClient.available()) {
            return item("local-vision", "本地视觉/OCR模型", "WARN",
                    "未检测到本地视觉模型：" + ollamaClient.visionModel() + "，OCR 会回退千问视觉模型。");
        }
        return item("local-vision", "本地视觉/OCR模型", "DOWN",
                "未检测到本地视觉模型：" + ollamaClient.visionModel() + "，且未配置 QWEN_API_KEY，图片 OCR/凭证识别会失败。");
    }

    private AiComponentStatusItem embeddingStatus() {
        if (ollamaClient.embeddingAvailable()) {
            return item("embedding", "本地 Embedding", "UP", "模型可用：" + ollamaClient.embeddingModel());
        }
        String vectorProvider = normalize(aiProperties.getKnowledge().getVectorDatabaseProvider(), "qdrant");
        String status = "qdrant".equals(vectorProvider) ? "DOWN" : "WARN";
        return item("embedding", "本地 Embedding", status, "未检测到 embedding 模型：" + ollamaClient.embeddingModel());
    }

    private AiComponentStatusItem vectorStoreStatus() {
        String provider = normalize(aiProperties.getKnowledge().getVectorDatabaseProvider(), "qdrant");
        if ("qdrant".equals(provider)) {
            try {
                long count = qdrantKnowledgeClient.count();
                return item("vector-store", "向量库", "UP", "Qdrant 可用，当前分片数：" + count);
            } catch (RuntimeException ex) {
                return item("vector-store", "向量库", "DOWN", messageOrDefault(ex, "Qdrant 不可用。"));
            }
        }
        return item("vector-store", "向量库", "UP", "当前使用内置 H2 向量库。");
    }

    private AiComponentStatusItem indexStatus() {
        long count = safeIndexCount();
        if (count > 0) {
            return item("knowledge-index", "知识索引", "UP", "当前分片数：" + count);
        }
        String error = knowledgeIndexService.lastRebuildError();
        if (error != null && !error.isBlank()) {
            return item("knowledge-index", "知识索引", "WARN", "当前分片为空，最近重建失败：" + error);
        }
        return item("knowledge-index", "知识索引", "WARN", "当前分片为空，请在智能检索中执行重建索引。");
    }

    private AiComponentStatusItem streamStatus() {
        if (aiProperties.getAssistant().isStreamEnabled()) {
            return item("assistant-stream", "助手流式输出", "UP", "已开启流式输出，客户端断开时服务端会停止写入。");
        }
        return item("assistant-stream", "助手流式输出", "DISABLED", "当前关闭流式输出，使用普通请求返回。");
    }

    private AiComponentStatusItem agentStatus() {
        if (aiProperties.getAgent().isEnabled()) {
            String detail = aiProperties.getAgent().isSelfCheckEnabled()
                    ? "业务 Agent 已开启，关键计划启用自检。"
                    : "业务 Agent 已开启，但关键计划自检已关闭。";
            return item("business-agent", "业务 Agent", "UP", detail);
        }
        return item("business-agent", "业务 Agent", "DISABLED", "配置已关闭业务 Agent，前端应隐藏入口并避免调用 Agent 接口。");
    }

    private long safeIndexCount() {
        try {
            return knowledgeIndexService.count();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private List<AiKnowledgeSourceCount> sourceTypeCounts(String vectorProvider) {
        try {
            if ("qdrant".equals(vectorProvider)) {
                Map<String, Long> counts = new TreeMap<>();
                int maxPoints = Math.max(1000, aiProperties.getKnowledge().getMaxDocuments());
                for (QdrantKnowledgeClient.ScoredPoint point : qdrantKnowledgeClient.scrollPayloads(maxPoints)) {
                    String sourceType = point.payload() == null ? "" : point.payload().getString("sourceType");
                    if (sourceType == null || sourceType.isBlank()) {
                        continue;
                    }
                    counts.merge(sourceType, 1L, Long::sum);
                }
                return counts.entrySet().stream()
                        .map(item -> new AiKnowledgeSourceCount(item.getKey(), item.getValue()))
                        .toList();
            }
            return knowledgeDocumentRepository.countBySourceType().stream()
                    .map(item -> new AiKnowledgeSourceCount(item.getSourceType().name(), item.getCount()))
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private String safePrimaryModel() {
        return safePrimaryModel(AiModelUseCase.CHAT);
    }

    private String safePrimaryModel(AiModelUseCase useCase) {
        try {
            return largeModelRouter.primaryModel(useCase);
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private String statusText(boolean available) {
        return available ? "可用" : "不可用";
    }

    private AiComponentStatusItem item(String code, String name, String status, String detail) {
        return new AiComponentStatusItem(code, name, status, detail);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String messageOrDefault(RuntimeException ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
