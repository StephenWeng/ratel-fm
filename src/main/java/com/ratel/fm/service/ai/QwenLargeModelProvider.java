package com.ratel.fm.service.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 千问云端大模型提供方。
 *
 * <p>只负责通用文本对话和 query 改写；凭证图片识别仍由 QwenClient 的视觉接口单独处理。</p>
 */
@Component
public class QwenLargeModelProvider implements LargeModelProvider {

    /**
     * 字段 qwenClient：封装 DashScope OpenAI 兼容接口、并发保护和熔断。
     */
    private final QwenClient qwenClient;

    /**
     * 构造 QwenLargeModelProvider 实例。
     */
    public QwenLargeModelProvider(QwenClient qwenClient) {
        this.qwenClient = qwenClient;
    }

    @Override
    public String providerCode() {
        return "qwen";
    }

    @Override
    public boolean available() {
        return qwenClient.available();
    }

    @Override
    public String primaryModel(AiModelUseCase useCase) {
        String model = qwenClient.chatModel();
        return model == null || model.isBlank() ? "qwen-plus" : model;
    }

    @Override
    public List<String> candidateModels(AiModelUseCase useCase) {
        return List.of(primaryModel(useCase));
    }

    @Override
    public String displayName(AiModelUseCase useCase, String routeLabel) {
        return "Qwen / " + primaryModel(useCase) + "（" + routeLabel + "）";
    }

    @Override
    public String chat(AiModelUseCase useCase, String systemPrompt, String userPrompt, boolean webSearch) {
        return qwenClient.chat(systemPrompt, userPrompt, webSearch);
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
        cancellation.throwIfCancelled();
        String answer = chat(useCase, systemPrompt, userPrompt, webSearch);
        cancellation.throwIfCancelled();
        if (answer != null && !answer.isBlank()) {
            contentConsumer.accept(answer);
        }
        return answer;
    }
}
