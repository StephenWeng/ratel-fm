package com.ratel.fm.common.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创建带稳定业务前缀的守护线程，统一异步客户端和后台任务的线程命名规则。
 *
 * <p>线程序号从 1 开始递增，便于日志、线程转储和监控指标定位具体线程；
 * 守护线程不会阻止 Spring Boot 进程正常退出。</p>
 */
public final class NamedDaemonThreadFactory implements ThreadFactory {

    /** 线程名称业务前缀，由调用方按线程池职责提供。 */
    private final String prefix;

    /** 当前工厂内的线程序号，不同线程池分别计数。 */
    private final AtomicInteger sequence = new AtomicInteger(1);

    /**
     * 创建命名守护线程工厂。
     *
     * @param prefix 线程名称前缀，例如 {@code ollama-http-}
     */
    public NamedDaemonThreadFactory(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("线程名称前缀不能为空");
        }
        this.prefix = prefix;
    }

    /** 创建下一个守护线程，并追加当前线程池内的递增序号。 */
    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, prefix + sequence.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
