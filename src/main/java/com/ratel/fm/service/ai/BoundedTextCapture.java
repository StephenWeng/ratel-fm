package com.ratel.fm.service.ai;

/**
 * 流式回答服务端轻量缓存。
 *
 * <p>该对象只限制服务端为最终元数据和会话摘要保留的文本样本，不限制实际下发给浏览器的流式内容。</p>
 */
public final class BoundedTextCapture {

    private final int maxChars;
    private final StringBuilder text = new StringBuilder();
    private boolean truncated;

    public BoundedTextCapture(int maxChars) {
        this.maxChars = Math.max(0, maxChars);
    }

    public void append(String value) {
        if (value == null || value.isBlank() || maxChars <= 0) {
            return;
        }
        int remaining = maxChars - text.length();
        if (remaining <= 0) {
            truncated = true;
            return;
        }
        if (value.length() <= remaining) {
            text.append(value);
            return;
        }
        text.append(value, 0, remaining);
        truncated = true;
    }

    public String text() {
        if (!truncated) {
            return text.toString();
        }
        return text + "\n\n[后续内容已继续流式发送，服务端未继续缓存完整回答]";
    }
}
