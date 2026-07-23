package com.ratel.fm.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 检索、本地 Ollama 和千问模型配置。
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * 大模型提供方选择配置，控制通用对话走本地 Ollama 还是千问云端。
     */
    private Model model = new Model();
    /**
     * 千问模型配置，包含 API Key、模型名称、Embedding 模型和请求超时时间。
     */
    private Qwen qwen = new Qwen();
    /**
     * 本地 Ollama 模型配置，用于在没有云端 token 时优先提供免费本地问答能力。
     */
    private Ollama ollama = new Ollama();
    /**
     * 本地知识库配置，控制启动重建、文档数量、上下文条数和分片策略。
     */
    private Knowledge knowledge = new Knowledge();
    /**
     * 互联网检索配置，控制搜索提供商、搜索密钥、接口地址和结果数量。
     */
    private WebSearch webSearch = new WebSearch();
    /**
     * ratel助手会话上下文配置，控制最近原文轮次和摘要长度。
     */
    private Assistant assistant = new Assistant();
    /**
     * 业务 Agent 配置，控制 Agent 是否启用以及关键动作保护策略。
     */
    private Agent agent = new Agent();

    /**
     * 获取大模型提供方选择配置。
     *
     * <p>实现步骤：返回 Spring 配置绑定后的 provider 选择，业务层通过 LargeModelRouter 查找具体实现。</p>
     */
    public Model getModel() {
        return model;
    }

    /**
     * 设置大模型提供方选择配置。
     *
     * <p>实现步骤：保存 Spring 配置绑定传入的模型提供方，允许部署时在 ollama 和 qwen 之间切换。</p>
     */
    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * 获取千问模型配置。
     *
     * <p>实现步骤：返回 Spring 配置绑定后的千问配置对象，供千问客户端读取云端模型和保护参数。</p>
     */
    public Qwen getQwen() {
        return qwen;
    }

    /**
     * 设置千问模型配置。
     *
     * <p>实现步骤：保存 Spring 配置绑定传入的千问配置对象，允许部署环境覆盖云端模型参数。</p>
     */
    public void setQwen(Qwen qwen) {
        this.qwen = qwen;
    }

    /**
     * 获取本地 Ollama 模型配置。
     *
     * <p>实现步骤：返回配置绑定后的 Ollama 配置对象，供本地模型客户端读取地址、模型和保护参数。</p>
     */
    public Ollama getOllama() {
        return ollama;
    }

    /**
     * 设置本地 Ollama 模型配置。
     *
     * <p>实现步骤：保存 Spring 配置绑定传入的 Ollama 配置对象，允许部署环境覆盖默认本地模型。</p>
     */
    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    /**
     * 获取知识索引和智能检索配置。
     *
     * <p>实现步骤：返回配置绑定后的知识库参数，供索引构建和检索服务选择 H2 或 Qdrant。</p>
     */
    public Knowledge getKnowledge() {
        return knowledge;
    }

    /**
     * 设置知识索引和智能检索配置。
     *
     * <p>实现步骤：保存 Spring 配置绑定传入的知识库参数，允许现场调整索引规模和向量库提供方。</p>
     */
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    /**
     * 获取互联网检索配置。
     *
     * <p>实现步骤：返回配置绑定后的搜索提供方、接口地址、密钥和限流参数。</p>
     */
    public WebSearch getWebSearch() {
        return webSearch;
    }

    /**
     * 设置互联网检索配置。
     *
     * <p>实现步骤：保存 Spring 配置绑定传入的搜索参数，允许部署时切换 Tavily、Bing 或 DuckDuckGo。</p>
     */
    public void setWebSearch(WebSearch webSearch) {
        this.webSearch = webSearch;
    }

    /**
     * 获取 ratel助手会话上下文配置。
     */
    public Assistant getAssistant() {
        return assistant;
    }

    /**
     * 设置 ratel助手会话上下文配置。
     */
    public void setAssistant(Assistant assistant) {
        this.assistant = assistant;
    }

    /**
     * 获取业务 Agent 配置。
     */
    public Agent getAgent() {
        return agent;
    }

    /**
     * 设置业务 Agent 配置。
     */
    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    /**
     * 大模型提供方选择配置。
     *
     * <p>用于控制通用对话、ratel助手和智能检索 query 改写调用哪个大模型实现；后续新增 deepseek、glm 时扩展 provider 即可。</p>
     */
    public static class Model {
        /**
         * 字段 provider：当前启用的大模型提供方，支持 ollama 或 qwen。
         */
        private String provider = "ollama";

        /**
         * 获取当前大模型提供方。
         *
         * <p>实现步骤：返回配置值；为空时路由器会回退到 ollama。</p>
         */
        public String getProvider() {
            return provider;
        }

        /**
         * 设置当前大模型提供方。
         *
         * <p>实现步骤：保存部署配置传入的 provider 编码，由路由器按编码选择实现类。</p>
         */
        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    /**
     * 千问云端模型配置。
     *
     * <p>用于承载 DashScope OpenAI 兼容接口参数；为空 API Key 时系统仍可使用本地 Ollama，不把云端模型作为必需依赖。</p>
     */
    public static class Qwen {
        /**
         * 字段 enabled：是否允许调用千问云端接口，现场无 token 时可关闭。
         */
        private boolean enabled = true;
        /**
         * 字段 apiKey：DashScope API Key，只允许通过环境变量注入，不能写入源码或配置文件。
         */
        private String apiKey;
        /**
         * 字段 baseUrl：千问 OpenAI 兼容接口地址，默认使用 DashScope compatible-mode。
         */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        /**
         * 字段 chatModel：千问对话模型名称，用于云端问答和检索 query 改写。
         */
        private String chatModel = "qwen-plus";
        /**
         * 字段 visionModel：保存视觉模型名称，用于图片或扫描件凭证识别。
         */
        private String visionModel = "qwen-vl-plus";
        /**
         * 字段 embeddingModel：千问兼容 embedding 模型配置；当前知识索引默认走本地 Ollama，不依赖该项。
         */
        private String embeddingModel = "text-embedding-v4";
        /**
         * 字段 requestTimeoutSeconds：千问 HTTP 请求超时时间，防止外部网络卡顿长期占用线程。
         */
        private int requestTimeoutSeconds = 30;
        /**
         * 字段 maxConcurrentRequests：限制同一时间发往千问的请求数量，防止接口超时堆积拖垮服务端。
         */
        private int maxConcurrentRequests = 2;
        /**
         * 字段 executorThreads：千问 HTTP 客户端专用线程数，避免使用默认线程池无限堆积工作线程。
         */
        private int executorThreads = 4;
        /**
         * 字段 failureThreshold：连续失败达到该次数后短时间熔断千问调用，避免反复超时导致内存上涨。
         */
        private int failureThreshold = 3;
        /**
         * 字段 circuitBreakerSeconds：千问连续失败后暂停调用的秒数。
         */
        private int circuitBreakerSeconds = 60;
        /**
         * 字段 maxPromptChars：单次文本提示词最大字符数，限制系统上下文和知识片段过大。
         */
        private int maxPromptChars = 24000;
        /**
         * 字段 maxResponseChars：单次模型响应最大字符数，避免异常响应体一次性占用过多堆内存。
         */
        private int maxResponseChars = 120000;
        /**
         * 字段 maxOutputTokens：限制模型输出长度，降低长答案和异常重试造成的内存压力。
         */
        private int maxOutputTokens = 1200;

        /**
         * 判断是否启用千问云端调用。
         *
         * <p>实现步骤：返回配置值；调用端还会结合 API Key 是否存在判断云端能力是否真正可用。</p>
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用千问云端调用。
         *
         * <p>实现步骤：保存部署配置，现场无 token 或要求纯本地模型时可关闭。</p>
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取千问 API Key。
         *
         * <p>实现步骤：返回环境变量注入的密钥；为空时千问客户端应判定为不可用。</p>
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * 设置千问 API Key。
         *
         * <p>实现步骤：保存 Spring 从环境变量绑定的密钥，不应在配置文件中明文维护。</p>
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * 获取千问 OpenAI 兼容接口地址。
         *
         * <p>实现步骤：返回 baseUrl，客户端在其后拼接 chat/completions 等接口路径。</p>
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置千问 OpenAI 兼容接口地址。
         *
         * <p>实现步骤：保存部署配置传入的地址，便于私有代理或兼容网关接入。</p>
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * 获取千问对话模型名称。
         *
         * <p>实现步骤：返回云端问答和 query 改写使用的模型名。</p>
         */
        public String getChatModel() {
            return chatModel;
        }

        /**
         * 设置千问对话模型名称。
         *
         * <p>实现步骤：保存部署配置传入的模型名，允许按成本或能力切换。</p>
         */
        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        /**
         * 获取凭证图片识别使用的视觉模型名称。
         *
         * <p>实现步骤：返回配置中的视觉模型；为空时由调用方回退到 qwen-vl-plus。</p>
         */
        public String getVisionModel() {
            return visionModel;
        }

        /**
         * 设置凭证图片识别使用的视觉模型名称。
         *
         * <p>实现步骤：保存外部配置传入的模型名，允许部署时按可用模型调整。</p>
         */
        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }

        /**
         * 获取千问 embedding 模型名称。
         *
         * <p>实现步骤：返回云端 embedding 配置；当前知识索引默认使用本地 Ollama embedding。</p>
         */
        public String getEmbeddingModel() {
            return embeddingModel;
        }

        /**
         * 设置千问 embedding 模型名称。
         *
         * <p>实现步骤：保存云端 embedding 模型配置，保留后续兼容扩展入口。</p>
         */
        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        /**
         * 获取千问请求超时时间。
         *
         * <p>实现步骤：返回单次 HTTP 请求允许等待的秒数，避免外部接口卡住服务线程。</p>
         */
        public int getRequestTimeoutSeconds() {
            return requestTimeoutSeconds;
        }

        /**
         * 设置千问请求超时时间。
         *
         * <p>实现步骤：保存部署配置传入的超时秒数，用于网络不稳定场景下快速释放资源。</p>
         */
        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }

        /**
         * 获取千问最大并发请求数。
         *
         * <p>实现步骤：返回配置值，调用方再做最小值兜底。</p>
         */
        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        /**
         * 设置千问最大并发请求数。
         *
         * <p>实现步骤：保存部署配置传入的并发上限，用于保护服务端内存和外部接口。</p>
         */
        public void setMaxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
        }

        /**
         * 获取千问 HTTP 客户端专用线程数。
         *
         * <p>实现步骤：返回配置值，调用方再做最小值兜底。</p>
         */
        public int getExecutorThreads() {
            return executorThreads;
        }

        /**
         * 设置千问 HTTP 客户端专用线程数。
         *
         * <p>实现步骤：保存部署配置传入的线程数，避免默认线程池不可控增长。</p>
         */
        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
        }

        /**
         * 获取千问连续失败熔断阈值。
         *
         * <p>实现步骤：返回配置值，调用方据此判断是否短时停止调用外部模型。</p>
         */
        public int getFailureThreshold() {
            return failureThreshold;
        }

        /**
         * 设置千问连续失败熔断阈值。
         *
         * <p>实现步骤：保存配置值，允许现场按接口稳定性调整熔断敏感度。</p>
         */
        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        /**
         * 获取千问熔断持续秒数。
         *
         * <p>实现步骤：返回配置值，调用方将其转换为时间戳进行短路判断。</p>
         */
        public int getCircuitBreakerSeconds() {
            return circuitBreakerSeconds;
        }

        /**
         * 设置千问熔断持续秒数。
         *
         * <p>实现步骤：保存配置值，用于连续超时后的冷却恢复。</p>
         */
        public void setCircuitBreakerSeconds(int circuitBreakerSeconds) {
            this.circuitBreakerSeconds = circuitBreakerSeconds;
        }

        /**
         * 获取单次提示词最大字符数。
         *
         * <p>实现步骤：返回配置值，千问客户端按该值截断文本提示词。</p>
         */
        public int getMaxPromptChars() {
            return maxPromptChars;
        }

        /**
         * 设置单次提示词最大字符数。
         *
         * <p>实现步骤：保存配置值，防止知识上下文过大导致请求体和堆内存膨胀。</p>
         */
        public void setMaxPromptChars(int maxPromptChars) {
            this.maxPromptChars = maxPromptChars;
        }

        /**
         * 获取单次响应最大字符数。
         *
         * <p>实现步骤：返回配置值，千问客户端按该值检查模型响应体。</p>
         */
        public int getMaxResponseChars() {
            return maxResponseChars;
        }

        /**
         * 设置单次响应最大字符数。
         *
         * <p>实现步骤：保存配置值，避免异常响应体被完整保留在内存中。</p>
         */
        public void setMaxResponseChars(int maxResponseChars) {
            this.maxResponseChars = maxResponseChars;
        }

        /**
         * 获取模型最大输出 token 数。
         *
         * <p>实现步骤：返回配置值，千问客户端写入 chat/completions 请求。</p>
         */
        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        /**
         * 设置模型最大输出 token 数。
         *
         * <p>实现步骤：保存配置值，控制模型回答长度和服务端内存占用。</p>
         */
        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }
    }

    /**
     * Ollama 类。
     *
     * <p>用于承载本地 Ollama 聊天模型配置，默认不需要 API Key，适合离线或无 token 场景。</p>
     */
    public static class Ollama {
        /**
         * 字段 enabled：控制 AI 助手是否优先尝试本地 Ollama 模型。
         */
        private boolean enabled = true;
        /**
         * 字段 baseUrl：保存本地 Ollama 服务地址，默认指向局域网模型服务器。
         */
        private String baseUrl = "http://10.105.12.136:11434";
        /**
         * 字段 chatModel：保存 Ollama 聊天模型名称，部署前需在本机 pull 对应模型。
         */
        private String chatModel = "qwen2.5:7b";
        /**
         * 字段 commandModel：保存短语音命令、菜单跳转和填表意图识别优先使用的轻量模型。
         */
        private String commandModel = "llama3.2:3b";
        /**
         * 字段 reasoningModel：保存报表分析、原因解释、趋势判断等复杂推理优先使用的模型。
         */
        private String reasoningModel = "deepseek-r1:8b";
        /**
         * 字段 visionModel：保存本地图片 OCR 和凭证识别优先使用的视觉模型。
         */
        private String visionModel = "qwen2.5vl:7b";
        /**
         * 字段 requestTimeoutSeconds：保存本地模型单次请求超时时间，避免未启动 Ollama 时长时间阻塞。
         */
        private int requestTimeoutSeconds = 8;
        /**
         * 字段 maxConcurrentRequests：限制同一时间发往 Ollama 的请求数量，保护本机 CPU 和内存。
         */
        private int maxConcurrentRequests = 1;
        /**
         * 字段 executorThreads：Ollama HTTP 客户端专用线程数。
         */
        private int executorThreads = 2;
        /**
         * 字段 failureThreshold：连续失败达到该次数后短时熔断本地模型调用。
         */
        private int failureThreshold = 2;
        /**
         * 字段 circuitBreakerSeconds：Ollama 连续失败后暂停调用的秒数。
         */
        private int circuitBreakerSeconds = 30;
        /**
         * 字段 maxPromptChars：单次提示词最大字符数，避免本地小模型上下文过大。
         */
        private int maxPromptChars = 12000;
        /**
         * 字段 maxResponseChars：单次模型响应最大字符数，避免异常响应体占用过多内存。
         */
        private int maxResponseChars = 80000;
        /**
         * 字段 maxOutputTokens：限制本地模型输出长度，降低本机推理时间。
         */
        private int maxOutputTokens = 800;
        /**
         * 字段 contextWindowTokens：限制 Ollama 推理上下文窗口，避免本地模型因上下文过大显著变慢。
         */
        private int contextWindowTokens = 4096;
        /**
         * 字段 keepAlive：Ollama 模型常驻时间，降低连续问答时反复装载模型的等待。
         */
        private String keepAlive = "30m";
        /**
         * 字段 embeddingModel：本地 Ollama embedding 模型名称，用于知识索引和语义检索。
         */
        private String embeddingModel = "bge-m3:latest";

        /**
         * 判断是否启用本地 Ollama 模型。
         *
         * <p>实现步骤：返回配置值，调用方据此决定是否优先尝试本地模型。</p>
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用本地 Ollama 模型。
         *
         * <p>实现步骤：保存部署配置，允许现场关闭本地模型并只使用云端兜底。</p>
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 Ollama 服务地址。
         *
         * <p>实现步骤：返回配置值，客户端会在调用时补齐接口路径。</p>
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置 Ollama 服务地址。
         *
         * <p>实现步骤：保存部署配置传入的本地或局域网 Ollama 地址。</p>
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * 获取 Ollama 聊天模型名称。
         *
         * <p>实现步骤：返回配置值，客户端写入 /api/chat 请求。</p>
         */
        public String getChatModel() {
            return chatModel;
        }

        /**
         * 设置 Ollama 聊天模型名称。
         *
         * <p>实现步骤：保存部署配置传入的本地模型名称。</p>
         */
        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        /**
         * 获取语音命令和短指令优先使用的模型名称。
         *
         * <p>实现步骤：返回配置值；客户端会在模型不存在时自动降级到其他已下载模型。</p>
         */
        public String getCommandModel() {
            return commandModel;
        }

        /**
         * 设置语音命令和短指令优先使用的模型名称。
         *
         * <p>实现步骤：保存部署配置传入的本地轻量模型名称。</p>
         */
        public void setCommandModel(String commandModel) {
            this.commandModel = commandModel;
        }

        /**
         * 获取复杂分析和推理优先使用的模型名称。
         *
         * <p>实现步骤：返回配置值；调用方按业务问题类型选择该模型。</p>
         */
        public String getReasoningModel() {
            return reasoningModel;
        }

        /**
         * 设置复杂分析和推理优先使用的模型名称。
         *
         * <p>实现步骤：保存部署配置传入的本地推理模型名称。</p>
         */
        public void setReasoningModel(String reasoningModel) {
            this.reasoningModel = reasoningModel;
        }

        /**
         * 获取本地视觉模型名称。
         */
        public String getVisionModel() {
            return visionModel;
        }

        /**
         * 设置本地视觉模型名称。
         */
        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }

        /**
         * 获取本地 embedding 模型名称。
         *
         * <p>实现步骤：返回配置值，OllamaClient 会写入 /api/embed 请求。</p>
         */
        public String getEmbeddingModel() {
            return embeddingModel;
        }

        /**
         * 设置本地 embedding 模型名称。
         *
         * <p>实现步骤：保存部署配置传入的 embedding 模型名称。</p>
         */
        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        /**
         * 获取请求超时时间。
         *
         * <p>实现步骤：返回配置值，客户端会设置最小兜底值。</p>
         */
        public int getRequestTimeoutSeconds() {
            return requestTimeoutSeconds;
        }

        /**
         * 设置请求超时时间。
         *
         * <p>实现步骤：保存部署配置传入的超时秒数，用于控制本地模型等待时间。</p>
         */
        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }

        /**
         * 获取最大并发请求数。
         *
         * <p>实现步骤：返回配置值，调用方据此创建并发闸门。</p>
         */
        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        /**
         * 设置最大并发请求数。
         *
         * <p>实现步骤：保存部署配置，避免多用户同时触发本地模型导致机器卡顿。</p>
         */
        public void setMaxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
        }

        /**
         * 获取 HTTP 客户端线程数。
         *
         * <p>实现步骤：返回配置值，客户端会设置最小兜底值。</p>
         */
        public int getExecutorThreads() {
            return executorThreads;
        }

        /**
         * 设置 HTTP 客户端线程数。
         *
         * <p>实现步骤：保存部署配置，控制本地模型请求线程池规模。</p>
         */
        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
        }

        /**
         * 获取连续失败熔断阈值。
         *
         * <p>实现步骤：返回配置值，客户端据此判断是否短时停止调用 Ollama。</p>
         */
        public int getFailureThreshold() {
            return failureThreshold;
        }

        /**
         * 设置连续失败熔断阈值。
         *
         * <p>实现步骤：保存部署配置，允许按本地模型稳定性调整熔断敏感度。</p>
         */
        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        /**
         * 获取熔断持续秒数。
         *
         * <p>实现步骤：返回配置值，客户端将其转换为熔断截止时间。</p>
         */
        public int getCircuitBreakerSeconds() {
            return circuitBreakerSeconds;
        }

        /**
         * 设置熔断持续秒数。
         *
         * <p>实现步骤：保存配置值，用于本地模型连续失败后的冷却恢复。</p>
         */
        public void setCircuitBreakerSeconds(int circuitBreakerSeconds) {
            this.circuitBreakerSeconds = circuitBreakerSeconds;
        }

        /**
         * 获取单次提示词最大字符数。
         *
         * <p>实现步骤：返回配置值，客户端按该值截断系统上下文和知识片段。</p>
         */
        public int getMaxPromptChars() {
            return maxPromptChars;
        }

        /**
         * 设置单次提示词最大字符数。
         *
         * <p>实现步骤：保存配置值，防止本地模型上下文过大造成推理缓慢。</p>
         */
        public void setMaxPromptChars(int maxPromptChars) {
            this.maxPromptChars = maxPromptChars;
        }

        /**
         * 获取单次响应最大字符数。
         *
         * <p>实现步骤：返回配置值，客户端按该值检查模型响应体大小。</p>
         */
        public int getMaxResponseChars() {
            return maxResponseChars;
        }

        /**
         * 设置单次响应最大字符数。
         *
         * <p>实现步骤：保存配置值，避免异常响应体被完整保留在内存中。</p>
         */
        public void setMaxResponseChars(int maxResponseChars) {
            this.maxResponseChars = maxResponseChars;
        }

        /**
         * 获取模型最大输出 token 数。
         *
         * <p>实现步骤：返回配置值，客户端写入 Ollama options.num_predict。</p>
         */
        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        /**
         * 设置模型最大输出 token 数。
         *
         * <p>实现步骤：保存配置值，控制本地模型回答长度和推理耗时。</p>
         */
        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        /**
         * 获取 Ollama 推理上下文窗口 token 数。
         *
         * <p>实现步骤：返回配置值，客户端写入 options.num_ctx。</p>
         */
        public int getContextWindowTokens() {
            return contextWindowTokens;
        }

        /**
         * 设置 Ollama 推理上下文窗口 token 数。
         *
         * <p>实现步骤：保存部署配置，现场可按机器性能在 4096、8192 等档位调整。</p>
         */
        public void setContextWindowTokens(int contextWindowTokens) {
            this.contextWindowTokens = contextWindowTokens;
        }

        /**
         * 获取 Ollama 模型常驻时间。
         *
         * <p>实现步骤：返回 keep_alive 配置值，客户端写入 Ollama 请求体。</p>
         */
        public String getKeepAlive() {
            return keepAlive;
        }

        /**
         * 设置 Ollama 模型常驻时间。
         *
         * <p>实现步骤：保存部署配置，支持 10m、30m、1h 等 Ollama 原生格式。</p>
         */
        public void setKeepAlive(String keepAlive) {
            this.keepAlive = keepAlive;
        }
    }

    /**
     * 知识索引和向量检索配置。
     *
     * <p>用于控制索引重建、分片规模、H2/Qdrant 向量库互斥选择，以及 Qdrant 调用保护参数。</p>
     */
    public static class Knowledge {
        /**
         * 字段 autoRebuildOnStartup：是否在应用启动后自动重建知识索引；生产环境通常关闭，避免启动链路执行大量模型调用。
         */
        private boolean autoRebuildOnStartup;
        /**
         * 字段 autoRebuildWhenEmpty：知识向量库为空时是否自动重建一次；用于首次部署后把初始化数据写入当前选择的向量库。
         */
        private boolean autoRebuildWhenEmpty = true;
        /**
         * 字段 startupRebuildInitialDelaySeconds：空索引首次检查前等待外部 Qdrant 和 Ollama 完成启动的秒数。
         */
        private int startupRebuildInitialDelaySeconds = 15;
        /**
         * 字段 startupRebuildMaxAttempts：空索引初始化失败后的最大尝试次数，避免一次启动竞态导致索引永久为空。
         */
        private int startupRebuildMaxAttempts = 12;
        /**
         * 字段 startupRebuildRetryDelaySeconds：相邻空索引初始化尝试之间的等待秒数。
         */
        private int startupRebuildRetryDelaySeconds = 15;
        /**
         * 字段 includeAdministrativeDivisions：是否把四万余条全国行政区划逐条生成 embedding，笔记本部署默认关闭。
         */
        private boolean includeAdministrativeDivisions;
        /**
         * 字段 maxDocuments：单次索引构建最多生成的知识分片数量，默认覆盖全国乡镇字典和业务数据。
         */
        private int maxDocuments = 100000;
        /**
         * 字段 maxContextDocuments：ratel助手单次问答最多引用的本地知识上下文条数。
         */
        private int maxContextDocuments = 8;
        /**
         * 字段 chunkSize：知识文本分片目标长度，影响 embedding 粒度和回答上下文大小。
         */
        private int chunkSize = 1200;
        /**
         * 字段 chunkOverlap：相邻知识分片重叠长度，降低关键句被切断导致召回失败的概率。
         */
        private int chunkOverlap = 120;
        /**
         * 字段 embeddingEnabled：H2 向量库模式下是否调用本地 Ollama embedding 生成向量，默认关闭以避免重建索引时占用过多本机资源。
         */
        private boolean embeddingEnabled;
        /**
         * 字段 queryRewriteModelEnabled：控制检索 query 改写是否调用大模型，默认关闭以降低每次检索的 token 消耗。
         */
        private boolean queryRewriteModelEnabled;
        /**
         * 字段 vectorDatabaseProvider：向量数据库提供方，支持 h2 或 qdrant；默认 qdrant，两者互斥。
         */
        private String vectorDatabaseProvider = "qdrant";
        /**
         * 字段 qdrantBaseUrl：Qdrant HTTP API 地址，默认指向局域网向量库服务器。
         */
        private String qdrantBaseUrl = "http://10.105.12.136:6333";
        /**
         * 字段 qdrantCollectionName：Qdrant 知识索引集合名称。
         */
        private String qdrantCollectionName = "ratel_fm_knowledge";
        /**
         * 字段 qdrantRequestTimeoutSeconds：Qdrant 单次请求超时时间。
         */
        private int qdrantRequestTimeoutSeconds = 10;
        /**
         * 字段 qdrantMaxConcurrentRequests：Qdrant 最大并发请求数。
         */
        private int qdrantMaxConcurrentRequests = 2;
        /**
         * 字段 qdrantExecutorThreads：Qdrant HTTP 客户端固定线程数。
         */
        private int qdrantExecutorThreads = 2;
        /**
         * 字段 qdrantBatchSize：Qdrant 批量写入点数量。
         */
        private int qdrantBatchSize = 64;
        /**
         * 字段 qdrantFailureThreshold：Qdrant 连续失败后触发熔断的次数。
         */
        private int qdrantFailureThreshold = 2;
        /**
         * 字段 qdrantCircuitBreakerSeconds：Qdrant 熔断持续秒数。
         */
        private int qdrantCircuitBreakerSeconds = 30;
        /**
         * 字段 qdrantMaxResponseChars：Qdrant 单次响应体最大字符数。
         */
        private int qdrantMaxResponseChars = 200000;

        /**
         * 判断启动时是否自动重建知识索引。
         *
         * <p>实现步骤：返回配置值；大数据量或 Qdrant 模式下建议人工触发重建，避免启动时间过长。</p>
         */
        public boolean isAutoRebuildOnStartup() {
            return autoRebuildOnStartup;
        }

        /**
         * 设置启动时是否自动重建知识索引。
         *
         * <p>实现步骤：保存部署配置；开启后 CommandLineRunner 会在应用启动完成前触发全量重建。</p>
         */
        public void setAutoRebuildOnStartup(boolean autoRebuildOnStartup) {
            this.autoRebuildOnStartup = autoRebuildOnStartup;
        }

        /**
         * 判断知识向量库为空时是否自动重建一次。
         *
         * <p>实现步骤：返回配置值；该开关用于首次部署，执行失败只写日志，不应阻塞主应用启动。</p>
         */
        public boolean isAutoRebuildWhenEmpty() {
            return autoRebuildWhenEmpty;
        }

        /**
         * 设置知识向量库为空时是否自动重建一次。
         *
         * <p>实现步骤：保存部署配置；关闭后需要管理员在智能检索或 ratel助手页面手工点击重建索引。</p>
         */
        public void setAutoRebuildWhenEmpty(boolean autoRebuildWhenEmpty) {
            this.autoRebuildWhenEmpty = autoRebuildWhenEmpty;
        }

        /** 获取空索引首次初始化前的等待秒数。 */
        public int getStartupRebuildInitialDelaySeconds() {
            return startupRebuildInitialDelaySeconds;
        }

        /** 设置空索引首次初始化前的等待秒数。 */
        public void setStartupRebuildInitialDelaySeconds(int startupRebuildInitialDelaySeconds) {
            this.startupRebuildInitialDelaySeconds = startupRebuildInitialDelaySeconds;
        }

        /** 获取空索引初始化的最大尝试次数。 */
        public int getStartupRebuildMaxAttempts() {
            return startupRebuildMaxAttempts;
        }

        /** 设置空索引初始化的最大尝试次数。 */
        public void setStartupRebuildMaxAttempts(int startupRebuildMaxAttempts) {
            this.startupRebuildMaxAttempts = startupRebuildMaxAttempts;
        }

        /** 获取空索引初始化失败后的重试间隔秒数。 */
        public int getStartupRebuildRetryDelaySeconds() {
            return startupRebuildRetryDelaySeconds;
        }

        /** 设置空索引初始化失败后的重试间隔秒数。 */
        public void setStartupRebuildRetryDelaySeconds(int startupRebuildRetryDelaySeconds) {
            this.startupRebuildRetryDelaySeconds = startupRebuildRetryDelaySeconds;
        }

        /** 判断全量索引是否包含全国行政区划明细。 */
        public boolean isIncludeAdministrativeDivisions() {
            return includeAdministrativeDivisions;
        }

        /** 设置全量索引是否包含全国行政区划明细。 */
        public void setIncludeAdministrativeDivisions(boolean includeAdministrativeDivisions) {
            this.includeAdministrativeDivisions = includeAdministrativeDivisions;
        }

        /**
         * 获取单次索引构建的最大分片数。
         *
         * <p>实现步骤：返回全量或增量索引可写入的分片上限，防止异常数据量导致内存暴涨。</p>
         */
        public int getMaxDocuments() {
            return maxDocuments;
        }

        /**
         * 设置单次索引构建的最大分片数。
         *
         * <p>实现步骤：保存部署配置传入的上限，用于控制 H2 表大小和 Qdrant 批量写入规模。</p>
         */
        public void setMaxDocuments(int maxDocuments) {
            this.maxDocuments = maxDocuments;
        }

        /**
         * 获取发送给助手的最大知识上下文条数。
         *
         * <p>实现步骤：返回 ratel助手拼接知识片段时的条数上限，避免提示词过长。</p>
         */
        public int getMaxContextDocuments() {
            return maxContextDocuments;
        }

        /**
         * 设置发送给助手的最大知识上下文条数。
         *
         * <p>实现步骤：保存部署配置传入的条数，平衡回答依据完整度和模型上下文长度。</p>
         */
        public void setMaxContextDocuments(int maxContextDocuments) {
            this.maxContextDocuments = maxContextDocuments;
        }

        /**
         * 获取知识正文分片长度。
         *
         * <p>实现步骤：返回单个知识分片的目标字符数，索引服务会再做最小值兜底。</p>
         */
        public int getChunkSize() {
            return chunkSize;
        }

        /**
         * 设置知识正文分片长度。
         *
         * <p>实现步骤：保存部署配置传入的字符数，较小值会增加 embedding 调用次数。</p>
         */
        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        /**
         * 获取知识正文分片重叠长度。
         *
         * <p>实现步骤：返回相邻分片保留的重叠字符数，索引服务会限制不超过分片一半。</p>
         */
        public int getChunkOverlap() {
            return chunkOverlap;
        }

        /**
         * 设置知识正文分片重叠长度。
         *
         * <p>实现步骤：保存部署配置传入的字符数，便于保留跨分片语义连续性。</p>
         */
        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }

        /**
         * 判断是否启用知识库向量生成。
         *
         * <p>实现步骤：返回配置值；关闭时系统使用关键词和规则召回，避免索引重建阶段高频调用外部模型。</p>
         */
        public boolean isEmbeddingEnabled() {
            return embeddingEnabled;
        }

        /**
         * 设置是否启用知识库向量生成。
         *
         * <p>实现步骤：保存部署配置，现场需要语义召回时可通过环境变量开启。</p>
         */
        public void setEmbeddingEnabled(boolean embeddingEnabled) {
            this.embeddingEnabled = embeddingEnabled;
        }

        /**
         * 判断 query 改写是否调用大模型。
         *
         * <p>实现步骤：返回配置值；关闭时仅使用规则改写，避免每次智能检索都消耗模型 token。</p>
         */
        public boolean isQueryRewriteModelEnabled() {
            return queryRewriteModelEnabled;
        }

        /**
         * 设置 query 改写是否调用大模型。
         *
         * <p>实现步骤：保存部署配置，现场需要更强召回时可通过环境变量开启。</p>
         */
        public void setQueryRewriteModelEnabled(boolean queryRewriteModelEnabled) {
            this.queryRewriteModelEnabled = queryRewriteModelEnabled;
        }

        /**
         * 获取当前向量库提供方。
         *
         * <p>实现步骤：返回 `h2` 或 `qdrant`；两者互斥，调用方不能在 Qdrant 不可用时回退 H2。</p>
         */
        public String getVectorDatabaseProvider() {
            return vectorDatabaseProvider;
        }

        /**
         * 设置当前向量库提供方。
         *
         * <p>实现步骤：保存部署配置传入的提供方名称；现场切换到 Qdrant 后需要重建知识索引。</p>
         */
        public void setVectorDatabaseProvider(String vectorDatabaseProvider) {
            this.vectorDatabaseProvider = vectorDatabaseProvider;
        }

        /**
         * 获取 Qdrant HTTP API 地址。
         *
         * <p>实现步骤：返回后端访问独立 Qdrant 包的 baseUrl，客户端会补齐 collection 路径。</p>
         */
        public String getQdrantBaseUrl() {
            return qdrantBaseUrl;
        }

        /**
         * 设置 Qdrant HTTP API 地址。
         *
         * <p>实现步骤：保存部署环境中的 Qdrant 地址，默认指向局域网内的独立 Qdrant 服务。</p>
         */
        public void setQdrantBaseUrl(String qdrantBaseUrl) {
            this.qdrantBaseUrl = qdrantBaseUrl;
        }

        /**
         * 获取 Qdrant collection 名称。
         *
         * <p>实现步骤：返回知识向量集合名称，重建索引时会重建该 collection。</p>
         */
        public String getQdrantCollectionName() {
            return qdrantCollectionName;
        }

        /**
         * 设置 Qdrant collection 名称。
         *
         * <p>实现步骤：保存部署配置传入的集合名，便于同一 Qdrant 实例隔离不同环境。</p>
         */
        public void setQdrantCollectionName(String qdrantCollectionName) {
            this.qdrantCollectionName = qdrantCollectionName;
        }

        /**
         * 获取 Qdrant 请求超时时间。
         *
         * <p>实现步骤：返回单次 HTTP 请求等待秒数，Qdrant 不可用时用于快速失败。</p>
         */
        public int getQdrantRequestTimeoutSeconds() {
            return qdrantRequestTimeoutSeconds;
        }

        /**
         * 设置 Qdrant 请求超时时间。
         *
         * <p>实现步骤：保存部署配置传入的超时秒数，避免慢请求长期占用连接。</p>
         */
        public void setQdrantRequestTimeoutSeconds(int qdrantRequestTimeoutSeconds) {
            this.qdrantRequestTimeoutSeconds = qdrantRequestTimeoutSeconds;
        }

        /**
         * 获取 Qdrant 最大并发请求数。
         *
         * <p>实现步骤：返回后端对 Qdrant 的并发闸门大小，保护本机连接数和 Qdrant 进程。</p>
         */
        public int getQdrantMaxConcurrentRequests() {
            return qdrantMaxConcurrentRequests;
        }

        /**
         * 设置 Qdrant 最大并发请求数。
         *
         * <p>实现步骤：保存部署配置传入的并发上限。</p>
         */
        public void setQdrantMaxConcurrentRequests(int qdrantMaxConcurrentRequests) {
            this.qdrantMaxConcurrentRequests = qdrantMaxConcurrentRequests;
        }

        /**
         * 获取 Qdrant HTTP 客户端线程数。
         *
         * <p>实现步骤：返回固定线程池大小，避免 Qdrant 请求创建过多工作线程。</p>
         */
        public int getQdrantExecutorThreads() {
            return qdrantExecutorThreads;
        }

        /**
         * 设置 Qdrant HTTP 客户端线程数。
         *
         * <p>实现步骤：保存部署配置传入的线程数。</p>
         */
        public void setQdrantExecutorThreads(int qdrantExecutorThreads) {
            this.qdrantExecutorThreads = qdrantExecutorThreads;
        }

        /**
         * 获取 Qdrant 批量写入大小。
         *
         * <p>实现步骤：返回索引重建时每批 upsert 的 point 数，平衡吞吐和单次请求体大小。</p>
         */
        public int getQdrantBatchSize() {
            return qdrantBatchSize;
        }

        /**
         * 设置 Qdrant 批量写入大小。
         *
         * <p>实现步骤：保存部署配置传入的批量点数量。</p>
         */
        public void setQdrantBatchSize(int qdrantBatchSize) {
            this.qdrantBatchSize = qdrantBatchSize;
        }

        /**
         * 获取 Qdrant 连续失败熔断阈值。
         *
         * <p>实现步骤：返回触发短时熔断的连续失败次数。</p>
         */
        public int getQdrantFailureThreshold() {
            return qdrantFailureThreshold;
        }

        /**
         * 设置 Qdrant 连续失败熔断阈值。
         *
         * <p>实现步骤：保存部署配置传入的失败次数，避免 Qdrant 故障时反复打满请求。</p>
         */
        public void setQdrantFailureThreshold(int qdrantFailureThreshold) {
            this.qdrantFailureThreshold = qdrantFailureThreshold;
        }

        /**
         * 获取 Qdrant 熔断持续时间。
         *
         * <p>实现步骤：返回熔断后暂停访问 Qdrant 的秒数。</p>
         */
        public int getQdrantCircuitBreakerSeconds() {
            return qdrantCircuitBreakerSeconds;
        }

        /**
         * 设置 Qdrant 熔断持续时间。
         *
         * <p>实现步骤：保存部署配置传入的暂停秒数。</p>
         */
        public void setQdrantCircuitBreakerSeconds(int qdrantCircuitBreakerSeconds) {
            this.qdrantCircuitBreakerSeconds = qdrantCircuitBreakerSeconds;
        }

        /**
         * 获取 Qdrant 响应体最大字符数。
         *
         * <p>实现步骤：返回后端允许读取的最大响应长度，避免异常大响应占用堆内存。</p>
         */
        public int getQdrantMaxResponseChars() {
            return qdrantMaxResponseChars;
        }

        /**
         * 设置 Qdrant 响应体最大字符数。
         *
         * <p>实现步骤：保存部署配置传入的响应体上限。</p>
         */
        public void setQdrantMaxResponseChars(int qdrantMaxResponseChars) {
            this.qdrantMaxResponseChars = qdrantMaxResponseChars;
        }
    }

    /**
     * 互联网检索配置。
     *
     * <p>用于控制 ratel助手是否补充外部网页结果、使用哪个搜索提供方，以及响应体大小和并发保护参数。</p>
     */
    public static class WebSearch {
        /**
         * 字段 enabled：是否启用互联网检索；关闭时 ratel助手只使用系统上下文和本地知识。
         */
        private boolean enabled = true;
        /**
         * 字段 provider：搜索提供方，支持 tavily、bing、duckduckgo 等实现分支。
         */
        private String provider = "tavily";
        /**
         * 字段 tavilyEndpoint：Tavily Search API 地址。
         */
        private String tavilyEndpoint = "https://api.tavily.com/search";
        /**
         * 字段 tavilyApiKey：Tavily API Key，只允许通过环境变量注入。
         */
        private String tavilyApiKey;
        /**
         * 字段 bingEndpoint：Bing Search API 地址。
         */
        private String bingEndpoint = "https://api.bing.microsoft.com/v7.0/search";
        /**
         * 字段 bingApiKey：Bing API Key，只允许通过环境变量注入。
         */
        private String bingApiKey;
        /**
         * 字段 maxResults：单次互联网检索最多返回的来源数量。
         */
        private int maxResults = 5;
        /**
         * 字段 requestTimeoutSeconds：搜索接口请求超时时间，避免外部网络卡顿长期占用线程。
         */
        private int requestTimeoutSeconds = 8;
        /**
         * 字段 maxConcurrentRequests：互联网检索最大并发数，避免搜索超时堆积大量网页响应。
         */
        private int maxConcurrentRequests = 2;
        /**
         * 字段 executorThreads：互联网检索 HTTP 客户端线程数。
         */
        private int executorThreads = 4;
        /**
         * 字段 maxResponseBytes：搜索接口响应体最大读取字节数。
         */
        private int maxResponseBytes = 524288;
        /**
         * 字段 maxPageBytes：网页正文抓取最大读取字节数。
         */
        private int maxPageBytes = 262144;
        /**
         * 字段 maxFetchPages：单次检索最多补充抓取正文的网页数量。
         */
        private int maxFetchPages = 2;
        /**
         * 字段 includeRawContent：是否要求 Tavily 返回原始网页正文，默认关闭以减少内存占用。
         */
        private boolean includeRawContent;

        /**
         * 判断是否启用互联网检索。
         *
         * <p>实现步骤：返回配置开关，调用方据此决定是否访问外部搜索服务。</p>
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用互联网检索。
         *
         * <p>实现步骤：保存部署配置传入的开关。</p>
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取互联网检索提供方。
         *
         * <p>实现步骤：返回 provider 配置，调用方按值选择 Tavily、Bing 或其他实现。</p>
         */
        public String getProvider() {
            return provider;
        }

        /**
         * 设置互联网检索提供方。
         *
         * <p>实现步骤：保存部署配置传入的 provider 名称。</p>
         */
        public void setProvider(String provider) {
            this.provider = provider;
        }

        /**
         * 获取 Tavily API 地址。
         *
         * <p>实现步骤：返回 Tavily endpoint，WebSearchService 会拼装请求体。</p>
         */
        public String getTavilyEndpoint() {
            return tavilyEndpoint;
        }

        /**
         * 设置 Tavily API 地址。
         *
         * <p>实现步骤：保存部署配置传入的 Tavily endpoint。</p>
         */
        public void setTavilyEndpoint(String tavilyEndpoint) {
            this.tavilyEndpoint = tavilyEndpoint;
        }

        /**
         * 获取 Tavily API Key。
         *
         * <p>实现步骤：返回环境变量注入的密钥；为空时 Tavily 分支不可用。</p>
         */
        public String getTavilyApiKey() {
            return tavilyApiKey;
        }

        /**
         * 设置 Tavily API Key。
         *
         * <p>实现步骤：保存 Spring 配置绑定传入的密钥值。</p>
         */
        public void setTavilyApiKey(String tavilyApiKey) {
            this.tavilyApiKey = tavilyApiKey;
        }

        /**
         * 获取 Bing API 地址。
         *
         * <p>实现步骤：返回 Bing endpoint，WebSearchService 会拼装查询参数。</p>
         */
        public String getBingEndpoint() {
            return bingEndpoint;
        }

        /**
         * 设置 Bing API 地址。
         *
         * <p>实现步骤：保存部署配置传入的 Bing endpoint。</p>
         */
        public void setBingEndpoint(String bingEndpoint) {
            this.bingEndpoint = bingEndpoint;
        }

        /**
         * 获取 Bing API Key。
         *
         * <p>实现步骤：返回环境变量注入的密钥；为空时 Bing 分支不可用。</p>
         */
        public String getBingApiKey() {
            return bingApiKey;
        }

        /**
         * 设置 Bing API Key。
         *
         * <p>实现步骤：保存 Spring 配置绑定传入的密钥值。</p>
         */
        public void setBingApiKey(String bingApiKey) {
            this.bingApiKey = bingApiKey;
        }

        /**
         * 获取单次互联网检索最大结果数。
         *
         * <p>实现步骤：返回配置值，调用方会再做安全上限裁剪。</p>
         */
        public int getMaxResults() {
            return maxResults;
        }

        /**
         * 设置单次互联网检索最大结果数。
         *
         * <p>实现步骤：保存部署配置传入的结果数量。</p>
         */
        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        /**
         * 获取互联网检索请求超时时间。
         *
         * <p>实现步骤：返回配置值，用于外部搜索请求和网页抓取超时控制。</p>
         */
        public int getRequestTimeoutSeconds() {
            return requestTimeoutSeconds;
        }

        /**
         * 设置互联网检索请求超时时间。
         *
         * <p>实现步骤：保存部署配置传入的超时秒数。</p>
         */
        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }

        /**
         * 获取互联网检索最大并发请求数。
         *
         * <p>实现步骤：返回配置值，调用方用最小值兜底后创建并发闸门。</p>
         */
        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        /**
         * 设置互联网检索最大并发请求数。
         *
         * <p>实现步骤：保存配置值，防止外部搜索接口超时时请求堆积。</p>
         */
        public void setMaxConcurrentRequests(int maxConcurrentRequests) {
            this.maxConcurrentRequests = maxConcurrentRequests;
        }

        /**
         * 获取互联网检索 HTTP 客户端线程数。
         *
         * <p>实现步骤：返回配置值，调用方创建固定大小线程池。</p>
         */
        public int getExecutorThreads() {
            return executorThreads;
        }

        /**
         * 设置互联网检索 HTTP 客户端线程数。
         *
         * <p>实现步骤：保存配置值，避免默认线程池增长不可控。</p>
         */
        public void setExecutorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
        }

        /**
         * 获取搜索接口响应体最大读取字节数。
         *
         * <p>实现步骤：返回配置值，调用方按该限制读取 InputStream。</p>
         */
        public int getMaxResponseBytes() {
            return maxResponseBytes;
        }

        /**
         * 设置搜索接口响应体最大读取字节数。
         *
         * <p>实现步骤：保存配置值，用于限制 Tavily、Bing 或 DuckDuckGo 响应大小。</p>
         */
        public void setMaxResponseBytes(int maxResponseBytes) {
            this.maxResponseBytes = maxResponseBytes;
        }

        /**
         * 获取网页正文抓取最大读取字节数。
         *
         * <p>实现步骤：返回配置值，调用方按该限制截断网页内容。</p>
         */
        public int getMaxPageBytes() {
            return maxPageBytes;
        }

        /**
         * 设置网页正文抓取最大读取字节数。
         *
         * <p>实现步骤：保存配置值，避免单个网页响应过大占用堆内存。</p>
         */
        public void setMaxPageBytes(int maxPageBytes) {
            this.maxPageBytes = maxPageBytes;
        }

        /**
         * 获取单次检索最多正文抓取页数。
         *
         * <p>实现步骤：返回配置值，调用方按该值限制二次抓取数量。</p>
         */
        public int getMaxFetchPages() {
            return maxFetchPages;
        }

        /**
         * 设置单次检索最多正文抓取页数。
         *
         * <p>实现步骤：保存配置值，用于控制互联网检索成本和内存占用。</p>
         */
        public void setMaxFetchPages(int maxFetchPages) {
            this.maxFetchPages = maxFetchPages;
        }

        /**
         * 判断 Tavily 是否返回 raw_content。
         *
         * <p>实现步骤：返回配置值，默认关闭以避免大段网页正文进入内存。</p>
         */
        public boolean isIncludeRawContent() {
            return includeRawContent;
        }

        /**
         * 设置 Tavily 是否返回 raw_content。
         *
         * <p>实现步骤：保存配置值，确需更强互联网召回时可在部署环境开启。</p>
         */
        public void setIncludeRawContent(boolean includeRawContent) {
            this.includeRawContent = includeRawContent;
        }
    }

    /**
     * Assistant 类。
     *
     * <p>用于控制 ratel助手多轮会话上下文。系统只把摘要和最近若干轮原文作为追问辅助，
     * 每次回答仍重新检索实时系统数据和知识库。</p>
     */
    public static class Assistant {
        /**
         * 字段 conversationEnabled：控制是否启用 ratel助手会话上下文。
         */
        private boolean conversationEnabled = true;
        /**
         * 字段 recentRawRounds：保留最近几轮用户/助手原文，按轮次计算，一轮包含一次用户提问和一次助手回答。
         */
        private int recentRawRounds = 4;
        /**
         * 字段 maxSummaryChars：会话摘要最大字符数。
         */
        private int maxSummaryChars = 1200;
        /**
         * 字段 maxMessageChars：单条历史消息最大字符数。
         */
        private int maxMessageChars = 800;
        /**
         * 字段 maxContextChars：拼入模型 prompt 的会话上下文最大字符数。
         */
        private int maxContextChars = 3000;
        /**
         * 字段 streamEnabled：控制 ratel助手是否启用 SSE 流式输出接口。
         */
        private boolean streamEnabled = true;
        /**
         * 字段 streamTimeoutSeconds：单次流式连接最长存活时间，避免网络卡顿导致连接长期占用。
         */
        private int streamTimeoutSeconds = 90;
        /**
         * 字段 streamHeartbeatSeconds：SSE 心跳间隔，避免代理或浏览器误判空闲断开。
         */
        private int streamHeartbeatSeconds = 10;
        /**
         * 字段 maxConcurrentStreams：全局最大流式连接数，防止连接和线程被耗尽。
         */
        private int maxConcurrentStreams = 4;
        /**
         * 字段 maxStreamsPerUser：同一用户最大流式连接数，防止单账号重复打开多个回答。
         */
        private int maxStreamsPerUser = 1;
        /**
         * 字段 streamExecutorThreads：流式发送专用线程数，和普通 Web 请求线程隔离。
         */
        private int streamExecutorThreads = 4;
        /**
         * 字段 streamCaptureChars：服务端为最终元数据和会话摘要保留的回答样本长度，不限制实际流式输出。
         */
        private int streamCaptureChars = 12000;

        /**
         * 判断是否启用会话上下文。
         */
        public boolean isConversationEnabled() {
            return conversationEnabled;
        }

        /**
         * 设置是否启用会话上下文。
         */
        public void setConversationEnabled(boolean conversationEnabled) {
            this.conversationEnabled = conversationEnabled;
        }

        /**
         * 获取最近保留原文轮次。
         */
        public int getRecentRawRounds() {
            return recentRawRounds;
        }

        /**
         * 设置最近保留原文轮次。
         */
        public void setRecentRawRounds(int recentRawRounds) {
            this.recentRawRounds = recentRawRounds;
        }

        /**
         * 获取会话摘要最大字符数。
         */
        public int getMaxSummaryChars() {
            return maxSummaryChars;
        }

        /**
         * 设置会话摘要最大字符数。
         */
        public void setMaxSummaryChars(int maxSummaryChars) {
            this.maxSummaryChars = maxSummaryChars;
        }

        /**
         * 获取单条历史消息最大字符数。
         */
        public int getMaxMessageChars() {
            return maxMessageChars;
        }

        /**
         * 设置单条历史消息最大字符数。
         */
        public void setMaxMessageChars(int maxMessageChars) {
            this.maxMessageChars = maxMessageChars;
        }

        /**
         * 获取会话上下文最大字符数。
         */
        public int getMaxContextChars() {
            return maxContextChars;
        }

        /**
         * 设置会话上下文最大字符数。
         */
        public void setMaxContextChars(int maxContextChars) {
            this.maxContextChars = maxContextChars;
        }

        /**
         * 判断是否启用流式输出。
         */
        public boolean isStreamEnabled() {
            return streamEnabled;
        }

        /**
         * 设置是否启用流式输出。
         */
        public void setStreamEnabled(boolean streamEnabled) {
            this.streamEnabled = streamEnabled;
        }

        /**
         * 获取单次流式连接最长存活时间。
         */
        public int getStreamTimeoutSeconds() {
            return streamTimeoutSeconds;
        }

        /**
         * 设置单次流式连接最长存活时间。
         */
        public void setStreamTimeoutSeconds(int streamTimeoutSeconds) {
            this.streamTimeoutSeconds = streamTimeoutSeconds;
        }

        /**
         * 获取 SSE 心跳间隔。
         */
        public int getStreamHeartbeatSeconds() {
            return streamHeartbeatSeconds;
        }

        /**
         * 设置 SSE 心跳间隔。
         */
        public void setStreamHeartbeatSeconds(int streamHeartbeatSeconds) {
            this.streamHeartbeatSeconds = streamHeartbeatSeconds;
        }

        /**
         * 获取全局最大流式连接数。
         */
        public int getMaxConcurrentStreams() {
            return maxConcurrentStreams;
        }

        /**
         * 设置全局最大流式连接数。
         */
        public void setMaxConcurrentStreams(int maxConcurrentStreams) {
            this.maxConcurrentStreams = maxConcurrentStreams;
        }

        /**
         * 获取同一用户最大流式连接数。
         */
        public int getMaxStreamsPerUser() {
            return maxStreamsPerUser;
        }

        /**
         * 设置同一用户最大流式连接数。
         */
        public void setMaxStreamsPerUser(int maxStreamsPerUser) {
            this.maxStreamsPerUser = maxStreamsPerUser;
        }

        /**
         * 获取流式发送专用线程数。
         */
        public int getStreamExecutorThreads() {
            return streamExecutorThreads;
        }

        /**
         * 设置流式发送专用线程数。
         */
        public void setStreamExecutorThreads(int streamExecutorThreads) {
            this.streamExecutorThreads = streamExecutorThreads;
        }

        /**
         * 获取服务端流式回答样本缓存长度。
         */
        public int getStreamCaptureChars() {
            return streamCaptureChars;
        }

        /**
         * 设置服务端流式回答样本缓存长度。
         */
        public void setStreamCaptureChars(int streamCaptureChars) {
            this.streamCaptureChars = streamCaptureChars;
        }
    }

    /**
     * 业务 Agent 配置。
     *
     * <p>用于控制采购、物流、库存、应收应付、财务和审批等业务 Agent 是否开放，以及关键动作是否必须执行自检。</p>
     */
    public static class Agent {
        /**
         * 字段 enabled：业务 Agent 总开关，默认开启。
         */
        private boolean enabled = true;
        /**
         * 字段 selfCheckEnabled：关键 Agent 自检开关，默认开启。
         */
        private boolean selfCheckEnabled = true;

        /**
         * 判断业务 Agent 是否启用。
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置业务 Agent 是否启用。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 判断关键 Agent 自检是否启用。
         */
        public boolean isSelfCheckEnabled() {
            return selfCheckEnabled;
        }

        /**
         * 设置关键 Agent 自检是否启用。
         */
        public void setSelfCheckEnabled(boolean selfCheckEnabled) {
            this.selfCheckEnabled = selfCheckEnabled;
        }
    }
}
