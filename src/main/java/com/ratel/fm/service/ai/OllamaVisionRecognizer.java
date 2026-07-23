package com.ratel.fm.service.ai;

import com.ratel.fm.service.ai.QwenClient.VisionInput;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地 Ollama 视觉模型识别实现。
 *
 * <p>实现目的：承接本地优先的图片 OCR 和凭证识别策略，避免配置了云端密钥时绕过本地模型。</p>
 */
@Component
@Order(10)
public class OllamaVisionRecognizer implements AiVisionRecognizer {

    /** Ollama 客户端，用于检查本地视觉模型和调用 /api/chat 多模态请求。 */
    private final OllamaClient ollamaClient;

    /**
     * 构造本地视觉模型识别器。
     *
     * <p>实现步骤：注入已有 Ollama 客户端，复用模型可用性、熔断和 HTTP 请求逻辑。</p>
     */
    public OllamaVisionRecognizer(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public String code() {
        return "ollama-vision";
    }

    @Override
    public String displayName() {
        return "本地 Ollama 视觉模型";
    }

    @Override
    public boolean available() {
        return ollamaClient.visionAvailable();
    }

    @Override
    public String recognize(String systemPrompt, String userPrompt, List<VisionInput> inputs) {
        return ollamaClient.vision(systemPrompt, userPrompt, inputs);
    }

    @Override
    public String unavailableReason() {
        return "未检测到本地视觉模型 " + ollamaClient.visionModel() + "，请启动 Ollama 并确认模型已下载。";
    }
}
