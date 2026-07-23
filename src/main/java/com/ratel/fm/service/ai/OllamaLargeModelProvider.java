package com.ratel.fm.service.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 本地 Ollama 大模型提供方。
 *
 * <p>按业务场景选择本地模型，适合无云端 token 或要求本地部署的场景。</p>
 */
@Component
public class OllamaLargeModelProvider implements LargeModelProvider {

    /**
     * 字段 ollamaClient：封装 Ollama HTTP API、模型探测、并发保护和熔断。
     */
    private final OllamaClient ollamaClient;

    /**
     * 构造 OllamaLargeModelProvider 实例。
     */
    public OllamaLargeModelProvider(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public String providerCode() {
        return "ollama";
    }

    @Override
    public boolean available() {
        return ollamaClient.available();
    }

    @Override
    public boolean available(AiModelUseCase useCase) {
        return ollamaClient.hasAnyModel(candidateModels(useCase));
    }

    @Override
    public String primaryModel(AiModelUseCase useCase) {
        return switch (useCase) {
            case COMMAND -> valueOrDefault(ollamaClient.commandModel(), ollamaClient.chatModel());
            case REASONING -> valueOrDefault(ollamaClient.reasoningModel(), ollamaClient.chatModel());
            case CHAT, QUERY_REWRITE -> valueOrDefault(ollamaClient.chatModel(), "qwen2.5:7b");
        };
    }

    @Override
    public List<String> candidateModels(AiModelUseCase useCase) {
        return switch (useCase) {
            case COMMAND -> List.of(ollamaClient.commandModel(), ollamaClient.chatModel(), ollamaClient.reasoningModel());
            case REASONING -> List.of(ollamaClient.reasoningModel(), ollamaClient.chatModel(), ollamaClient.commandModel());
            case CHAT, QUERY_REWRITE -> List.of(ollamaClient.chatModel(), ollamaClient.commandModel(), ollamaClient.reasoningModel());
        };
    }

    @Override
    public String displayName(AiModelUseCase useCase, String routeLabel) {
        String selectedModel = ollamaClient.resolveModel(candidateModels(useCase));
        String model = selectedModel.isBlank() ? primaryModel(useCase) : selectedModel;
        return "Ollama / " + model + "（" + routeLabel + "）";
    }

    @Override
    public String chat(AiModelUseCase useCase, String systemPrompt, String userPrompt, boolean webSearch) {
        return ollamaClient.chat(candidateModels(useCase), systemPrompt, userPrompt);
    }

    @Override
    public String chatStream(
            AiModelUseCase useCase,
            String systemPrompt,
            String userPrompt,
            boolean webSearch,
            Consumer<String> contentConsumer,
            AiStreamCancellation cancellation,
            int captureChars
    ) {
        return ollamaClient.chatStream(candidateModels(useCase), systemPrompt, userPrompt, contentConsumer, cancellation, captureChars);
    }

    /**
     * 返回非空模型名；为空时使用 fallback。
     */
    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
