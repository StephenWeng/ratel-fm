package com.ratel.fm.service.ai;

import com.ratel.fm.service.ai.QwenClient.VisionInput;

import java.util.List;

/**
 * AI 视觉识别能力接口。
 *
 * <p>实现目的：把本地 Ollama 视觉模型、千问视觉模型等不同 OCR/多模态实现隔离开，
 * 上层服务只按策略顺序调用能力，不直接耦合具体供应商。</p>
 */
public interface AiVisionRecognizer {

    /**
     * 返回识别器编码。
     *
     * <p>实现步骤：具体实现返回稳定编码，用于错误提示、日志和状态排查。</p>
     */
    String code();

    /**
     * 返回识别器显示名称。
     *
     * <p>实现步骤：具体实现返回用户可理解的中文名称，用于失败原因汇总。</p>
     */
    String displayName();

    /**
     * 判断当前识别器是否具备可调用条件。
     *
     * <p>实现步骤：具体实现检查模型服务、模型是否下载、API Key 等前置条件，避免上层盲目调用。</p>
     */
    boolean available();

    /**
     * 执行图片 OCR 或多模态结构化识别。
     *
     * <p>实现步骤：具体实现把系统提示词、用户提示词和图片/文本输入转换为供应商请求，返回模型输出文本。</p>
     */
    String recognize(String systemPrompt, String userPrompt, List<VisionInput> inputs);

    /**
     * 返回不可用时的明确说明。
     *
     * <p>实现步骤：具体实现说明缺少的配置、模型或服务，供最终失败提示直接展示给用户。</p>
     */
    String unavailableReason();
}
