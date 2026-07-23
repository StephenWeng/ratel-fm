package com.ratel.fm.service.ai;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 大模型提供方路由器。
 *
 * <p>根据 app.ai.model.provider 选择唯一实现，不在业务层做隐式降级，便于部署排查和后续扩展。</p>
 */
@Service
public class LargeModelRouter {

    /**
     * 字段 aiProperties：读取当前大模型提供方配置。
     */
    private final AiProperties aiProperties;
    /**
     * 字段 providers：按 providerCode 索引所有大模型实现。
     */
    private final Map<String, LargeModelProvider> providers;

    /**
     * 构造 LargeModelRouter 实例。
     */
    public LargeModelRouter(AiProperties aiProperties, List<LargeModelProvider> providers) {
        this.aiProperties = aiProperties;
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        item -> normalizeProvider(item.providerCode()),
                        item -> item
                ));
    }

    /**
     * 返回当前配置的大模型提供方编码。
     */
    public String selectedProviderCode() {
        return normalizeProvider(aiProperties.getModel().getProvider());
    }

    /**
     * 判断当前配置的大模型是否可用。
     */
    public boolean available() {
        LargeModelProvider provider = providers.get(selectedProviderCode());
        return provider != null && provider.available();
    }

    /**
     * 判断当前配置的大模型在指定业务场景下是否可用。
     *
     * <p>实现步骤：先解析当前 provider，再委托 provider 按场景模型候选列表判断，避免状态页只看到聊天模型可用而复杂分析模型不可用。</p>
     */
    public boolean available(AiModelUseCase useCase) {
        LargeModelProvider provider = providers.get(selectedProviderCode());
        return provider != null && provider.available(useCase);
    }

    /**
     * 返回当前配置下指定场景的主模型名称。
     */
    public String primaryModel(AiModelUseCase useCase) {
        return selectedProvider().primaryModel(useCase);
    }

    /**
     * 返回当前配置下指定场景的候选模型。
     */
    public List<String> candidateModels(AiModelUseCase useCase) {
        return selectedProvider().candidateModels(useCase);
    }

    /**
     * 返回当前配置下指定场景的模型显示名。
     */
    public String displayName(AiModelUseCase useCase, String routeLabel) {
        LargeModelProvider provider = providers.get(selectedProviderCode());
        if (provider == null) {
            return "未配置模型提供方：" + selectedProviderCode();
        }
        if (!provider.available()) {
            return "未配置可用模型：" + provider.displayName(useCase, routeLabel);
        }
        return provider.displayName(useCase, routeLabel);
    }

    /**
     * 调用当前配置的大模型执行非流式对话。
     */
    public String chat(AiModelUseCase useCase, String systemPrompt, String userPrompt, boolean webSearch) {
        return selectedProvider().chat(useCase, systemPrompt, userPrompt, webSearch);
    }

    /**
     * 调用当前配置的大模型执行流式对话。
     */
    public String chatStream(
            AiModelUseCase useCase,
            String systemPrompt,
            String userPrompt,
            boolean webSearch,
            Consumer<String> contentConsumer,
            AiStreamCancellation cancellation,
            int captureChars
    ) {
        return selectedProvider().chatStream(useCase, systemPrompt, userPrompt, webSearch, contentConsumer, cancellation, captureChars);
    }

    /**
     * 获取当前配置的 provider；未知 provider 直接抛业务异常，避免误走其他模型。
     */
    private LargeModelProvider selectedProvider() {
        String providerCode = selectedProviderCode();
        LargeModelProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ResponseCode.FAILED,
                    "未知的大模型提供方：" + providerCode + "，请配置为 ollama 或 qwen。");
        }
        return provider;
    }

    /**
     * 规范化 provider 配置值。
     */
    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "ollama";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
