package com.ratel.fm.service.assistant;

import com.ratel.fm.common.concurrent.NamedDaemonThreadFactory;

import com.alibaba.fastjson2.JSON;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.ai.AiStreamCancellation;
import com.ratel.fm.service.ai.AiStreamCancelledException;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.AiAssistantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ratel助手 SSE 流式输出协调服务。
 *
 * <p>该服务集中处理流式连接并发限制、单用户限制、心跳、超时、客户端断开和上游模型取消，避免慢网络拖垮 Web 容器。</p>
 */
@Service
public class AiAssistantStreamService {

    private static final long DONE_COMPLETE_DELAY_MILLIS = 150L;

    private final AiAssistantService aiAssistantService;
    private final AiProperties aiProperties;
    private final Semaphore streamSemaphore;
    private final Map<String, AtomicInteger> userStreams = new java.util.concurrent.ConcurrentHashMap<>();
    private final ExecutorService streamExecutor;
    private final ScheduledExecutorService scheduler;

    public AiAssistantStreamService(AiAssistantService aiAssistantService, AiProperties aiProperties) {
        this.aiAssistantService = aiAssistantService;
        this.aiProperties = aiProperties;
        int maxStreams = maxConcurrentStreams();
        this.streamSemaphore = new Semaphore(maxStreams);
        this.streamExecutor = new ThreadPoolExecutor(
                streamThreads(),
                streamThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxStreams),
                new NamedDaemonThreadFactory("assistant-stream-"),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.scheduler = java.util.concurrent.Executors.newScheduledThreadPool(
                Math.max(2, Math.min(maxStreams, 4)),
                new NamedDaemonThreadFactory("assistant-stream-watch-")
        );
    }

    /**
     * 启动助手流式回答。
     */
    public SseEmitter stream(
            String question,
            String mode,
            String conversationSummary,
            List<AiAssistantService.ConversationMessage> conversationMessages
    ) {
        AiProperties.Assistant config = assistantConfig();
        if (!config.isStreamEnabled()) {
            return rejectedEmitter("ratel助手流式输出未启用");
        }
        String normalizedQuestion = question == null ? "" : question.trim();
        String normalizedMode = mode == null || mode.isBlank() ? "hybrid" : mode.trim();
        List<AiAssistantService.ConversationMessage> safeConversationMessages =
                conversationMessages == null ? List.of() : conversationMessages;
        String userKey = currentUserKey();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        try {
            acquirePermit(userKey);
        } catch (BusinessException ex) {
            return rejectedEmitter(userMessage(ex));
        }
        StreamCancellationToken cancellation = new StreamCancellationToken();
        StreamSession session = new StreamSession(
                new SseEmitter(timeoutMillis() + 5000L),
                userKey,
                cancellation
        );
        registerLifecycle(session);
        try {
            streamExecutor.execute(() -> runStream(
                    session,
                    normalizedQuestion,
                    normalizedMode,
                    conversationSummary,
                    safeConversationMessages,
                    securityContext
            ));
        } catch (RuntimeException ex) {
            session.sendQuietly("error", Map.of("message", "ratel助手流式请求较多，请稍后再试。"));
            session.complete();
        }
        return session.emitter();
    }

    /**
     * 执行流式输出工作线程。
     */
    private void runStream(
            StreamSession session,
            String question,
            String mode,
            String conversationSummary,
            List<AiAssistantService.ConversationMessage> conversationMessages,
            SecurityContext securityContext
    ) {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(securityContext);
        ScheduledFuture<?> heartbeat = scheduleHeartbeat(session);
        ScheduledFuture<?> timeout = scheduleTimeout(session);
        session.onFinish(() -> {
            heartbeat.cancel(true);
            timeout.cancel(true);
        });
        try {
            AiAssistantService.ModelRoute modelRoute = aiAssistantService.route(question, mode);
            AiAssistantService.AssistantAnswerPlan plan = aiAssistantService.prepareAnswerPlan(
                    question,
                    mode,
                    modelRoute,
                    conversationSummary,
                    conversationMessages
            );
            session.send("meta", aiAssistantService.responseFromPlan(plan, ""));
            String answer = aiAssistantService.askAvailableModelStream(
                    plan,
                    content -> {
                        session.cancellation().throwIfCancelled();
                        session.send("delta", Map.of("content", content));
                    },
                    session.cancellation(),
                    streamCaptureChars()
            );
            session.cancellation().throwIfCancelled();
            AiAssistantResponse response = aiAssistantService.responseFromPlan(plan, answer);
            session.send("done", response);
            completeAfterDone(session);
        } catch (AiStreamCancelledException ignored) {
            session.complete();
        } catch (RuntimeException ex) {
            if (!session.cancellation().isCancelled()) {
                session.sendQuietly("error", Map.of("message", userMessage(ex)));
            }
            session.complete();
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }

    /**
     * 延迟关闭成功流，给浏览器留出刷出 done 事件的时间，降低 chunked 响应尾部被判异常的概率。
     */
    private void completeAfterDone(StreamSession session) {
        session.cancel();
        scheduler.schedule(session::complete, DONE_COMPLETE_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * 返回可被前端 SSE 解析器消费的立即失败响应。
     */
    private SseEmitter rejectedEmitter(String message) {
        SseEmitter emitter = new SseEmitter(5000L);
        try {
            emitter.send(SseEmitter.event().name("error").data(JSON.toJSONString(Map.of("message", message))));
        } catch (IOException | IllegalStateException ignored) {
        } finally {
            emitter.complete();
        }
        return emitter;
    }

    /**
     * 注册 SseEmitter 生命周期回调。
     */
    private void registerLifecycle(StreamSession session) {
        session.emitter().onCompletion(session::finish);
        session.emitter().onTimeout(() -> {
            session.cancel();
            session.complete();
        });
        session.emitter().onError(error -> {
            session.cancel();
            session.finish();
        });
    }

    /**
     * 申请全局和单用户流式许可。
     */
    private void acquirePermit(String userKey) {
        if (!streamSemaphore.tryAcquire()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, ResponseCode.LOAD_CLIENT_ERROR,
                    "当前 ratel助手流式请求较多，请稍后再试。");
        }
        AtomicInteger count = userStreams.computeIfAbsent(userKey, ignored -> new AtomicInteger());
        while (true) {
            int current = count.get();
            if (current >= maxStreamsPerUser()) {
                streamSemaphore.release();
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, ResponseCode.LOAD_CLIENT_ERROR,
                        "当前账号已有 ratel助手回答正在生成，请稍后再试。");
            }
            if (count.compareAndSet(current, current + 1)) {
                return;
            }
        }
    }

    /**
     * 释放全局和单用户流式许可。
     */
    private void releasePermit(String userKey) {
        streamSemaphore.release();
        AtomicInteger count = userStreams.get(userKey);
        if (count == null) {
            return;
        }
        if (count.decrementAndGet() <= 0) {
            userStreams.remove(userKey, count);
        }
    }

    /**
     * 定时发送 SSE 心跳。
     */
    private ScheduledFuture<?> scheduleHeartbeat(StreamSession session) {
        long heartbeatSeconds = heartbeatSeconds();
        return scheduler.scheduleAtFixedRate(() -> {
            if (!session.cancellation().isCancelled()) {
                session.sendQuietly("heartbeat", Map.of("time", System.currentTimeMillis()));
            }
        }, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
    }

    /**
     * 定时取消超时流式请求。
     */
    private ScheduledFuture<?> scheduleTimeout(StreamSession session) {
        return scheduler.schedule(() -> {
            if (!session.cancellation().isCancelled()) {
                session.sendQuietly("error", Map.of("message", "ratel助手回答超时，本次流式连接已关闭，请稍后重试。"));
                session.cancel();
                session.complete();
            }
        }, timeoutMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 当前用户流式限流键。
     */
    private String currentUserKey() {
        CurrentUser user = SecurityUtils.currentUser();
        String sessionId = user.sessionId() == null ? "" : user.sessionId();
        return user.id() + ":" + user.username() + ":" + sessionId;
    }

    /**
     * 转换可展示错误消息。
     */
    private String userMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "ratel助手暂时无法完成本次流式回答，请稍后重试。";
        }
        return message;
    }

    private AiProperties.Assistant assistantConfig() {
        return aiProperties.getAssistant() == null ? new AiProperties.Assistant() : aiProperties.getAssistant();
    }

    private int maxConcurrentStreams() {
        return Math.max(1, assistantConfig().getMaxConcurrentStreams());
    }

    private int maxStreamsPerUser() {
        return Math.max(1, assistantConfig().getMaxStreamsPerUser());
    }

    private int streamThreads() {
        return Math.max(1, assistantConfig().getStreamExecutorThreads());
    }

    private long timeoutMillis() {
        return Duration.ofSeconds(Math.max(10, assistantConfig().getStreamTimeoutSeconds())).toMillis();
    }

    private long heartbeatSeconds() {
        return Math.max(3, assistantConfig().getStreamHeartbeatSeconds());
    }

    private int streamCaptureChars() {
        return Math.max(1000, assistantConfig().getStreamCaptureChars());
    }

    /**
     * 单次流式会话。
     */
    private final class StreamSession {
        private final SseEmitter emitter;
        private final String userKey;
        private final StreamCancellationToken cancellation;
        private final AtomicBoolean finished = new AtomicBoolean();
        private final CopyOnWriteArrayList<Runnable> finishCallbacks = new CopyOnWriteArrayList<>();
        private final ReentrantLock sendLock = new ReentrantLock();

        private StreamSession(SseEmitter emitter, String userKey, StreamCancellationToken cancellation) {
            this.emitter = emitter;
            this.userKey = userKey;
            this.cancellation = cancellation;
        }

        private SseEmitter emitter() {
            return emitter;
        }

        private StreamCancellationToken cancellation() {
            return cancellation;
        }

        private void onFinish(Runnable callback) {
            if (finished.get()) {
                callback.run();
                return;
            }
            finishCallbacks.add(callback);
        }

        private void send(String event, Object data) {
            cancellation.throwIfCancelled();
            sendLock.lock();
            try {
                emitter.send(SseEmitter.event().name(event).data(JSON.toJSONString(data)));
            } catch (IOException | IllegalStateException ex) {
                cancel();
                throw new AiStreamCancelledException();
            } finally {
                sendLock.unlock();
            }
        }

        private void sendQuietly(String event, Object data) {
            if (!sendLock.tryLock()) {
                return;
            }
            try {
                if (!cancellation.isCancelled()) {
                    emitter.send(SseEmitter.event().name(event).data(JSON.toJSONString(data)));
                }
            } catch (IOException | IllegalStateException ignored) {
                cancel();
            } catch (RuntimeException ignored) {
            } finally {
                sendLock.unlock();
            }
        }

        private void cancel() {
            cancellation.cancel();
        }

        private void complete() {
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
            } finally {
                finish();
            }
        }

        private void finish() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            cancellation.cancel();
            finishCallbacks.forEach(Runnable::run);
            releasePermit(userKey);
        }
    }

    /**
     * 单次流式请求取消令牌。
     */
    private static final class StreamCancellationToken implements AiStreamCancellation {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final CopyOnWriteArrayList<Runnable> callbacks = new CopyOnWriteArrayList<>();

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public void onCancel(Runnable callback) {
            if (cancelled.get()) {
                callback.run();
                return;
            }
            callbacks.add(callback);
            if (cancelled.get() && callbacks.remove(callback)) {
                callback.run();
            }
        }

        private void cancel() {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            callbacks.forEach(Runnable::run);
        }
    }
}
