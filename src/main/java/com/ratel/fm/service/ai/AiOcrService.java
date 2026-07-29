package com.ratel.fm.service.ai;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.service.ai.QwenClient.VisionInput;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片 OCR 和多模态识别路由服务。
 *
 * <p>实现目的：统一承接本地知识库图片 OCR、凭证识别等多模态入口，按已排序的识别器列表执行
 * “本地优先、云端兜底、全部失败则明确提示”的策略。</p>
 */
@Service
public class AiOcrService {

    /** 已按 @Order 排序的视觉识别器列表，第一顺位为本地 Ollama，第二顺位为千问。 */
    private final List<AiVisionRecognizer> recognizers;

    /**
     * 构造 OCR 策略服务。
     *
     * <p>实现步骤：注入所有 {@link AiVisionRecognizer} 实现，Spring 按 {@code @Order} 保持调用顺序。</p>
     */
    public AiOcrService(List<AiVisionRecognizer> recognizers) {
        this.recognizers = recognizers;
    }

    /**
     * 执行 OCR 或多模态结构化识别。
     *
     * <p>实现步骤：
     * 1. 遍历已排序识别器，跳过不可用实现并记录原因；
     * 2. 对可用实现发起识别，返回第一个非空结果；
     * 3. 单个实现调用失败时记录失败原因并继续尝试下一顺位；
     * 4. 所有实现都不可用或无结果时，抛出包含完整原因的业务异常。</p>
     */
    public String recognize(String systemPrompt, String userPrompt, List<VisionInput> inputs) {
        List<String> reasons = new ArrayList<>();
        int availableCount = 0;
        for (AiVisionRecognizer recognizer : recognizers) {
            if (!recognizer.available()) {
                reasons.add(recognizer.displayName() + "不可用：" + recognizer.unavailableReason());
                continue;
            }
            availableCount++;
            try {
                String answer = recognizer.recognize(systemPrompt, userPrompt, inputs);
                if (answer != null && !answer.isBlank()) {
                    return answer;
                }
                reasons.add(recognizer.displayName() + "未返回有效识别结果。");
            } catch (RuntimeException ex) {
                reasons.add(recognizer.displayName() + "调用失败：" + readableMessage(ex));
            }
        }
        if (availableCount > 0) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ResponseCode.LOAD_CLIENT_ERROR,
                    "OCR 模型已检测为可用，但本次识别执行失败。可能是 CPU、内存或磁盘资源不足，模型服务繁忙或请求超时；请释放资源后重试。详情："
                            + reasons.stream().filter(item -> item != null && !item.isBlank()).collect(Collectors.joining("；")));
        }
        throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                "当前没有可用的 OCR 能力：" + reasons.stream().filter(item -> item != null && !item.isBlank())
                        .collect(Collectors.joining("；")));
    }

    /**
     * 提取异常中的用户可读原因。
     *
     * <p>实现步骤：优先使用异常 message；为空时退回异常类名，避免失败原因为空。</p>
     */
    private String readableMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
