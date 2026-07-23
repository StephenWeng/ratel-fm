package com.ratel.fm.service.ai;

/**
 * AI 流式请求取消令牌。
 *
 * <p>用于在浏览器断开、SSE 超时或服务端主动中止时，及时关闭上游模型 HTTP 响应流。</p>
 */
public interface AiStreamCancellation {

    /**
     * 判断当前流式请求是否已经被取消。
     */
    boolean isCancelled();

    /**
     * 注册取消回调。若注册时已经取消，回调会立即执行。
     */
    void onCancel(Runnable callback);

    /**
     * 已取消时抛出轻量异常，中止当前流式读取。
     */
    default void throwIfCancelled() {
        if (isCancelled()) {
            throw new AiStreamCancelledException();
        }
    }
}
