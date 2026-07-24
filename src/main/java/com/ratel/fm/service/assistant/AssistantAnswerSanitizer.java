package com.ratel.fm.service.assistant;

import org.springframework.stereotype.Component;

/**
 * ratel 助手输出清洗器。
 *
 * <p>集中处理模型可能泄露的内部思考、复盘草稿和危险确认措辞，保证流式输出、最终响应和会话摘要使用同一套安全规则。</p>
 */
@Component
public class AssistantAnswerSanitizer {

    /**
     * 清理模型可能泄露的内部思考、复盘草稿和 think 标签。
     */
    public String sanitizeAnswer(String value) {
        String text = value(value).replace("\r\n", "\n").trim();
        if (text.isBlank()) {
            return "";
        }
        text = text
                .replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?is)<think>.*$", "")
                .replaceAll("(?is)```\\s*think\\s*.*?```", "")
                .replace("</think>", "")
                .trim();
        boolean hasInternalMarker = containsAny(text,
                "重新读一下用户的问题",
                "我需要",
                "用户问的是",
                "这可能是一个",
                "不能简单",
                "先看",
                "让我");
        int lastConclusion = text.lastIndexOf("结论：");
        if (hasInternalMarker && lastConclusion > 0) {
            text = text.substring(lastConclusion).trim();
        }
        return text
                .replaceAll("(?m)^\\s*---\\s*$", "")
                .replaceAll("(?m)^\\s*(现在重新读一下用户的问题|用户问的是|我需要|让我|先看).*$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 过滤历史摘要中的确认动作，避免下一轮继承危险意图。
     */
    public String stripDangerousConfirmation(String value) {
        String text = value(value);
        if (text.isBlank()) {
            return "";
        }
        return text
                .replace("确认删除", "要求删除")
                .replace("确认修改", "要求修改")
                .replace("确认新增", "要求新增")
                .replace("确认审批", "要求审批")
                .replace("确认取消", "要求取消")
                .replace("确认执行", "要求执行");
    }

    private boolean containsAny(String source, String... items) {
        if (source == null || source.isBlank()) {
            return false;
        }
        for (String item : items) {
            if (source.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
