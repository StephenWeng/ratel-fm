package com.ratel.fm.service.ai;

import com.ratel.fm.service.ai.QwenClient.VisionInput;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 千问云端视觉模型识别实现。
 *
 * <p>实现目的：作为本地 OCR/视觉模型不可用时的第二顺位兜底能力，仍然只通过环境变量或外置配置读取密钥。</p>
 */
@Component
@Order(20)
public class QwenVisionRecognizer implements AiVisionRecognizer {

    /** 千问客户端，用于检查 API Key 和调用千问视觉模型接口。 */
    private final QwenClient qwenClient;

    /**
     * 构造千问视觉识别器。
     *
     * <p>实现步骤：注入已有千问客户端，复用 API Key 检查、HTTP 请求和响应解析逻辑。</p>
     */
    public QwenVisionRecognizer(QwenClient qwenClient) {
        this.qwenClient = qwenClient;
    }

    @Override
    public String code() {
        return "qwen-vision";
    }

    @Override
    public String displayName() {
        return "千问视觉模型";
    }

    @Override
    public boolean available() {
        return qwenClient.available();
    }

    @Override
    public String recognize(String systemPrompt, String userPrompt, List<VisionInput> inputs) {
        return qwenClient.vision(systemPrompt, userPrompt, inputs);
    }

    @Override
    public String unavailableReason() {
        return "未配置可用 QWEN_API_KEY，无法回退千问视觉模型。";
    }
}
