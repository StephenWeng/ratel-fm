package com.ratel.fm.service.ai;

import java.util.List;
import java.util.function.Consumer;

/**
 * 大模型提供方统一接口。
 *
 * <p>业务服务只依赖本接口，不直接关心当前使用 Qwen、Ollama 还是后续新增的其他模型提供方。</p>
 */
public interface LargeModelProvider {

    /**
     * 返回提供方配置编码，例如 ollama、qwen。
     */
    String providerCode();

    /**
     * 判断当前提供方是否具备可调用的对话能力。
     */
    boolean available();

    /**
     * 判断当前提供方在指定业务场景下是否具备可调用能力。
     *
     * <p>实现步骤：默认复用 provider 级别可用性；需要精确校验场景模型的提供方可覆盖该方法。</p>
     */
    default boolean available(AiModelUseCase useCase) {
        return available();
    }

    /**
     * 返回指定业务场景的主模型名称。
     */
    String primaryModel(AiModelUseCase useCase);

    /**
     * 返回指定业务场景的候选模型列表。
     */
    List<String> candidateModels(AiModelUseCase useCase);

    /**
     * 返回前端和日志可读的模型显示名称。
     */
    String displayName(AiModelUseCase useCase, String routeLabel);

    /**
     * 执行非流式对话。
     */
    String chat(AiModelUseCase useCase, String systemPrompt, String userPrompt, boolean webSearch);

    /**
     * 执行流式对话；不支持原生流式的提供方可以一次性返回完整内容。
     */
    String chatStream(
            AiModelUseCase useCase,
            String systemPrompt,
            String userPrompt,
            boolean webSearch,
            Consumer<String> contentConsumer,
            AiStreamCancellation cancellation,
            int captureChars
    );
}
