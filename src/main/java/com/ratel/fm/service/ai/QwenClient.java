package com.ratel.fm.service.ai;

import com.ratel.fm.common.concurrent.NamedDaemonThreadFactory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 千问 OpenAI 兼容接口客户端。
 */
@Component
public class QwenClient {

    /**
     * 字段 properties：保存 properties 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AiProperties properties;
    /**
     * 字段 httpClient：保存 httpClient 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final HttpClient httpClient;
    /**
     * 字段 executor：千问 HTTP 客户端专用线程池，避免默认线程池在多次超时后堆积不可控工作线程。
     */
    private final ExecutorService executor;
    /**
     * 字段 requestSemaphore：千问请求并发闸门，避免用户连续点击或多个页面同时调用时耗尽堆内存。
     */
    private final Semaphore requestSemaphore;
    /**
     * 字段 consecutiveFailures：记录连续失败次数，用于触发短时熔断。
     */
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    /**
     * 字段 circuitOpenUntilMillis：熔断截止时间戳；当前时间小于该值时直接拒绝外部调用。
     */
    private volatile long circuitOpenUntilMillis;

    /**
     * 构造 QwenClient 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public QwenClient(AiProperties properties) {
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(
                Math.max(1, properties.getQwen().getExecutorThreads()),
                new NamedDaemonThreadFactory("qwen-http-")
        );
        this.requestSemaphore = new Semaphore(Math.max(1, properties.getQwen().getMaxConcurrentRequests()));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(3, properties.getQwen().getRequestTimeoutSeconds())))
                .executor(executor)
                .build();
    }

    /**
     * 执行 available 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean available() {
        return properties.getQwen().isEnabled()
                && properties.getQwen().getApiKey() != null
                && !properties.getQwen().getApiKey().isBlank();
    }

    /**
     * 执行 chatModel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String chatModel() {
        return properties.getQwen().getChatModel();
    }

    /**
     * 获取视觉模型名称。
     *
     * <p>实现步骤：读取 app.ai.qwen.vision-model；未配置时回退 qwen-vl-plus，避免普通问答模型误用于图片识别。</p>
     */
    public String visionModel() {
        String model = properties.getQwen().getVisionModel();
        return model == null || model.isBlank() ? "qwen-vl-plus" : model.trim();
    }

    /**
     * 执行 embeddingModel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String embeddingModel() {
        return properties.getQwen().getEmbeddingModel();
    }

    /**
     * 执行 embedding 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public List<Double> embedding(String text) {
        if (!available()) {
            return List.of();
        }
        // 变量说明：payload 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject payload = new JSONObject();
        payload.put("model", properties.getQwen().getEmbeddingModel());
        payload.put("input", text == null ? "" : text);
        // 变量说明：response 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject response = post("/embeddings", payload);
        // 变量说明：data 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray data = response.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return List.of();
        }
        // 变量说明：embedding 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray embedding = data.getJSONObject(0).getJSONArray("embedding");
        if (embedding == null) {
            return List.of();
        }
        // 变量说明：values 保存当前步骤计算、查询或转换得到的中间结果。
        List<Double> values = new ArrayList<>(embedding.size());
        for (int i = 0; i < embedding.size(); i++) {
            values.add(embedding.getDoubleValue(i));
        }
        return values;
    }

    /**
     * 执行 chat 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, false);
    }

    /**
     * 执行 chat 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String chat(String systemPrompt, String userPrompt, boolean enableSearch) {
        if (!available()) {
            return "";
        }
        return chatInternal(systemPrompt, userPrompt, enableSearch);
    }

    /**
     * 使用千问视觉模型识别图片和文本附件。
     *
     * <p>实现步骤：
     * 1. 组装 system 消息约束模型只输出 JSON；
     * 2. user 消息中先放文字提示，再按文件顺序追加图片 data URL 或文本内容；
     * 3. 调用 OpenAI 兼容 chat/completions 接口；
     * 4. 返回模型第一条消息内容，业务层负责 JSON 解析和字段兜底。</p>
     */
    public String vision(String systemPrompt, String userPrompt, List<VisionInput> inputs) {
        if (!available()) {
            return "";
        }
        JSONArray messages = new JSONArray();
        messages.add(message("system", systemPrompt));
        JSONArray userContent = new JSONArray();
        userContent.add(textPart(userPrompt));
        if (inputs != null) {
            for (VisionInput input : inputs) {
                if (input == null) {
                    continue;
                }
                if (input.imageDataUrl() != null && !input.imageDataUrl().isBlank()) {
                    userContent.add(imagePart(input.imageDataUrl()));
                } else if (input.text() != null && !input.text().isBlank()) {
                    userContent.add(textPart("文件：" + input.fileName() + "\n" + input.text()));
                }
            }
        }
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userContent);
        messages.add(userMessage);

        JSONObject payload = new JSONObject();
        payload.put("model", visionModel());
        payload.put("messages", messages);
        payload.put("temperature", 0.1);
        payload.put("max_tokens", maxOutputTokens());
        payload.put("stream", false);

        JSONObject response = post("/chat/completions", payload);
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message == null ? "" : message.getString("content");
    }

    /**
     * 执行 chatInternal 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String chatInternal(String systemPrompt, String userPrompt, boolean enableSearch) {
        // 变量说明：messages 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray messages = new JSONArray();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));

        // 变量说明：payload 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject payload = new JSONObject();
        payload.put("model", properties.getQwen().getChatModel());
        payload.put("messages", messages);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", maxOutputTokens());
        payload.put("stream", false);
        if (enableSearch) {
            payload.put("enable_search", true);
        }

        JSONObject response;
        try {
            response = post("/chat/completions", payload);
        } catch (BusinessException ex) {
            if (!enableSearch) {
                throw ex;
            }
            return chatInternal(systemPrompt, userPrompt, false);
        }
        // 变量说明：choices 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        // 变量说明：message 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message == null ? "" : message.getString("content");
    }

    /**
     * 执行 message 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private JSONObject message(String role, String content) {
        // 变量说明：message 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", truncate(content, maxPromptChars()));
        return message;
    }

    /**
     * 构造多模态文本片段。
     *
     * <p>实现步骤：设置 type=text，并把空值兜底为空字符串。</p>
     */
    private JSONObject textPart(String text) {
        JSONObject part = new JSONObject();
        part.put("type", "text");
        part.put("text", truncate(text, maxPromptChars()));
        return part;
    }

    /**
     * 构造多模态图片片段。
     *
     * <p>实现步骤：设置 type=image_url，图片地址使用 data URL，兼容前端上传后由服务端转 base64 的临时识别场景。</p>
     */
    private JSONObject imagePart(String dataUrl) {
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", dataUrl);
        JSONObject part = new JSONObject();
        part.put("type", "image_url");
        part.put("image_url", imageUrl);
        return part;
    }

    /**
     * 执行 post 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private JSONObject post(String path, JSONObject payload) {
        if (circuitOpen()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ResponseCode.LOAD_CLIENT_ERROR,
                    "千问接口连续超时，系统已短暂熔断，请稍后再试。");
        }
        boolean acquired = false;
        try {
            acquired = requestSemaphore.tryAcquire(Math.max(1, timeoutSeconds()), TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, ResponseCode.LOAD_CLIENT_ERROR,
                        "当前 AI 请求较多，请稍后再试。");
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getQwen().getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString()))
                    .build();
            // 变量说明：response 保存当前步骤计算、查询或转换得到的中间结果。
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                recordFailure();
                throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                        "千问接口暂时不可用，请稍后再试。");
            }
            String body = response.body() == null ? "" : response.body();
            if (body.length() > maxResponseChars()) {
                recordFailure();
                throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                        "千问接口返回内容过大，本次回答已中止。");
            }
            recordSuccess();
            return JSON.parseObject(body);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            recordFailure();
            throw new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                    "千问接口暂时不可用或响应超时，请稍后再试。");
        } finally {
            if (acquired) {
                requestSemaphore.release();
            }
        }
    }

    /**
     * 执行 baseUrl 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String baseUrl() {
        // 变量说明：value 保存当前步骤计算、查询或转换得到的中间结果。
        String value = properties.getQwen().getBaseUrl();
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * 判断千问接口是否处于短时熔断状态。
     *
     * <p>实现步骤：
     * 1. 读取熔断截止时间；
     * 2. 与当前系统时间比较；
     * 3. 熔断未结束时拒绝新请求，熔断结束后允许请求尝试恢复。</p>
     */
    private boolean circuitOpen() {
        return circuitOpenUntilMillis > System.currentTimeMillis();
    }

    /**
     * 记录千问请求成功。
     *
     * <p>实现步骤：清空连续失败次数和熔断截止时间，让后续请求正常进入外部调用。</p>
     */
    private void recordSuccess() {
        consecutiveFailures.set(0);
        circuitOpenUntilMillis = 0L;
    }

    /**
     * 记录千问请求失败并在达到阈值时打开熔断。
     *
     * <p>实现步骤：
     * 1. 连续失败计数加一；
     * 2. 达到阈值后设置熔断截止时间；
     * 3. 熔断期间后续请求直接走降级回答，避免线程和内存继续堆积。</p>
     */
    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        int threshold = Math.max(1, properties.getQwen().getFailureThreshold());
        if (failures >= threshold) {
            circuitOpenUntilMillis = System.currentTimeMillis()
                    + Duration.ofSeconds(Math.max(5, properties.getQwen().getCircuitBreakerSeconds())).toMillis();
        }
    }

    /**
     * 获取请求超时时间。
     *
     * <p>实现步骤：读取配置并限制最小 3 秒，避免错误配置导致请求立即失败。</p>
     */
    private int timeoutSeconds() {
        return Math.max(3, properties.getQwen().getRequestTimeoutSeconds());
    }

    /**
     * 获取模型最大输出 token 数。
     *
     * <p>实现步骤：读取配置并限制最小 256，避免模型输出过短影响业务回答。</p>
     */
    private int maxOutputTokens() {
        return Math.max(256, properties.getQwen().getMaxOutputTokens());
    }

    /**
     * 获取提示词最大字符数。
     *
     * <p>实现步骤：读取配置并限制最小 4000，避免上下文过度截断造成回答依据不足。</p>
     */
    private int maxPromptChars() {
        return Math.max(4000, properties.getQwen().getMaxPromptChars());
    }

    /**
     * 获取模型响应最大字符数。
     *
     * <p>实现步骤：读取配置并限制最小 20000，避免正常回答被误判为异常响应。</p>
     */
    private int maxResponseChars() {
        return Math.max(20000, properties.getQwen().getMaxResponseChars());
    }

    /**
     * 按最大字符数截断文本。
     *
     * <p>实现步骤：
     * 1. 空值兜底为空字符串；
     * 2. 文本长度在限制内直接返回；
     * 3. 超出限制时保留前半段和后半段，避免关键单号只出现在结尾时丢失。</p>
     */
    private String truncate(String value, int maxChars) {
        String text = value == null ? "" : value;
        if (text.length() <= maxChars) {
            return text;
        }
        int half = Math.max(1, maxChars / 2);
        return text.substring(0, half)
                + "\n\n[上下文过长，已截断中间内容]\n\n"
                + text.substring(text.length() - half);
    }

    /**
     * 视觉识别输入。
     *
     * <p>实现目的：统一承载待识别文件名、图片 data URL 和 PDF 文本，供凭证导入等多模态场景使用。</p>
     */
    public record VisionInput(
            /** 原始文件名，便于模型在多文件识别时区分来源。 */
            String fileName,
            /** 图片文件转换后的 data URL；文本型 PDF 为空。 */
            String imageDataUrl,
            /** 文本型 PDF 或其他已抽取内容；图片文件为空。 */
            String text
    ) {
    }
}
