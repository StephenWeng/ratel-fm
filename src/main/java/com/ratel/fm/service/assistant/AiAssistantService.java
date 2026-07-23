package com.ratel.fm.service.assistant;

import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.service.ai.AiModelUseCase;
import com.ratel.fm.service.ai.AiStreamCancellation;
import com.ratel.fm.service.ai.AiStreamCancelledException;
import com.ratel.fm.service.ai.LargeModelRouter;
import com.ratel.fm.service.knowledge.KnowledgeSearchService;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.AiAssistantResponse;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.AiCitation;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResult;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.WebSearchResult;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * ratel助手服务，基于知识检索、本地 Ollama 和千问模型回答业务问题。
 */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    /**
     * 业务编号识别正则，用于从用户提问中提取单号并提高本地知识检索命中率。
     */
    private static final Pattern BUSINESS_TOKEN_PATTERN = Pattern.compile("(?i)[a-z]{1,12}\\d{4,}|\\d{4,}");
    /**
     * 常量 MAX_LOCAL_CONTEXTS_FOR_MODEL：发送给模型的本地知识最大条数，降低单次问答 token 消耗。
     */
    private static final int MAX_LOCAL_CONTEXTS_FOR_MODEL = 5;
    /**
     * 常量 MAX_WEB_CONTEXTS_FOR_MODEL：发送给模型的互联网来源最大条数，降低互联网模式 token 消耗。
     */
    private static final int MAX_WEB_CONTEXTS_FOR_MODEL = 3;
    /**
     * 常量 MAX_LOCAL_CONTENT_CHARS：单条本地知识进入模型前的最大字符数。
     */
    private static final int MAX_LOCAL_CONTENT_CHARS = 900;
    /**
     * 常量 MAX_WEB_SUMMARY_CHARS：单条互联网摘要进入模型前的最大字符数。
     */
    private static final int MAX_WEB_SUMMARY_CHARS = 600;
    /**
     * 常量 MAX_SYSTEM_CONTEXT_CHARS：实时系统上下文进入模型前的最大字符数。
     */
    private static final int MAX_SYSTEM_CONTEXT_CHARS = 5000;

    /**
     * 字段 knowledgeSearchService：保存 knowledgeSearchService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final KnowledgeSearchService knowledgeSearchService;
    /**
     * 字段 systemContextService：保存 systemContextService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final SystemContextService systemContextService;
    /**
     * 字段 webSearchService：保存 webSearchService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final WebSearchService webSearchService;
    /**
     * 字段 largeModelRouter：按配置选择当前大模型提供方，避免业务代码散落 Qwen/Ollama 判断。
     */
    private final LargeModelRouter largeModelRouter;
    /**
     * 字段 aiProperties：保存 AI 助手会话上下文配置。
     */
    private final AiProperties aiProperties;

    /**
     * 构造 AiAssistantService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public AiAssistantService(
            KnowledgeSearchService knowledgeSearchService,
            SystemContextService systemContextService,
            WebSearchService webSearchService,
            LargeModelRouter largeModelRouter,
            AiProperties aiProperties
    ) {
        this.knowledgeSearchService = knowledgeSearchService;
        this.systemContextService = systemContextService;
        this.webSearchService = webSearchService;
        this.largeModelRouter = largeModelRouter;
        this.aiProperties = aiProperties;
    }

    /**
     * 执行 ask 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public AiAssistantResponse ask(String question, String mode) {
        return ask(question, mode, "", List.of());
    }

    /**
     * 执行 ask 方法。
     *
     * <p>实现步骤：
     * 1. 接收当前问题、检索模式、会话短摘要和最近历史消息；
     * 2. 先裁剪会话上下文，再重新检索实时系统上下文和知识上下文；
     * 3. 生成回答后返回更新后的会话短摘要，供前端下一轮追问使用。</p>
     */
    public AiAssistantResponse ask(
            String question,
            String mode,
            String conversationSummary,
            List<ConversationMessage> conversationMessages
    ) {
        // 变量说明：normalizedQuestion 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedQuestion = question == null ? "" : question.trim();
        // 变量说明：normalizedMode 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedMode = normalizeMode(mode);
        ModelRoute modelRoute = selectModelRoute(normalizedQuestion, normalizedMode);
        AssistantAnswerPlan plan = prepareAnswerPlan(
                normalizedQuestion,
                normalizedMode,
                modelRoute,
                conversationSummary,
                conversationMessages
        );
        String answer;
        if (plan.directAnswer() != null) {
            answer = plan.directAnswer();
        } else {
            answer = askAvailableModel(
                    normalizedQuestion,
                    normalizedMode,
                    modelRoute,
                    plan.webMode(),
                    plan.conversationContext().promptText(),
                    plan.systemContext(),
                    plan.contexts(),
                    plan.webResults()
            );
            if (answer == null || answer.isBlank()) {
                answer = fallbackAnswer(modelRoute, normalizedMode, plan.systemContext(), plan.contexts(), plan.webResults());
            }
        }
        return new AiAssistantResponse(
                question,
                answer,
                modelAvailable(modelRoute),
                activeModelName(modelRoute),
                normalizedMode,
                plan.citations(),
                suggestions(normalizedMode, plan.contexts(), plan.webResults()),
                updateConversationSummary(plan.conversationContext(), normalizedQuestion, answer),
                plan.conversationContext().recentRawRounds()
        );
    }

    /**
     * 准备回答上下文。
     */
    public AssistantAnswerPlan prepareAnswerPlan(
            String question,
            String mode,
            ModelRoute modelRoute,
            String conversationSummary,
            List<ConversationMessage> conversationMessages
    ) {
        String normalizedQuestion = question == null ? "" : question.trim();
        String normalizedMode = normalizeMode(mode);
        ConversationContext conversationContext = buildConversationContext(
                conversationSummary,
                conversationMessages,
                normalizedQuestion
        );
        String retrievalQuestion = retrievalQuestion(normalizedQuestion, conversationContext);
        boolean localMode = !"web".equals(normalizedMode);
        boolean webMode = !"local".equals(normalizedMode) && !"command".equals(normalizedMode);
        String systemContext = localMode ? systemContextService.buildContext(retrievalQuestion) : "";
        List<KnowledgeSearchResult> contexts = localMode
                ? knowledgeSearchService.searchForContext(retrievalQuestion)
                : List.of();
        contexts = prioritizeExactMatches(normalizedQuestion, contexts);
        Set<String> businessTokens = extractBusinessTokens(normalizedQuestion);
        boolean requiresExactLocalRecord = localMode && requiresExactLocalRecord(normalizedQuestion, businessTokens);
        boolean hasExactLocalRecord = contexts.stream().anyMatch(item -> exactSourceNo(businessTokens, item));
        if (requiresExactLocalRecord && !hasExactLocalRecord) {
            contexts = List.of();
        }
        List<WebSearchResult> webResults = webMode
                ? webSearchService.search(normalizedQuestion)
                : List.of();
        List<AiCitation> citations = new ArrayList<>();
        citations.addAll(contexts.stream()
                .map(this::toCitation)
                .toList());
        citations.addAll(webResults.stream()
                .map(this::toCitation)
                .toList());
        String directAnswer = null;
        if (localMode && asksForFileExistence(normalizedQuestion, conversationContext) && webResults.isEmpty()) {
            directAnswer = fileExistenceAnswer(contexts);
        } else if (requiresExactLocalRecord && !hasExactLocalRecord && webResults.isEmpty()) {
            directAnswer = exactRecordNotFoundAnswer(businessTokens);
        } else if (canAnswerFromExactLocalContext(normalizedQuestion, normalizedMode, contexts, webResults, businessTokens)) {
            directAnswer = exactLocalContextAnswer(contexts, businessTokens);
        } else if (contexts.isEmpty() && webResults.isEmpty() && systemContext.isBlank()) {
            directAnswer = noContextAnswer(normalizedMode);
        }
        return new AssistantAnswerPlan(
                normalizedQuestion,
                normalizedMode,
                modelRoute,
                conversationContext,
                webMode,
                systemContext,
                contexts,
                webResults,
                citations,
                directAnswer
        );
    }

    /**
     * 选择本次回答模型路由。
     */
    public ModelRoute route(String question, String mode) {
        return selectModelRoute(question == null ? "" : question.trim(), normalizeMode(mode));
    }

    /**
     * 基于准备好的上下文流式生成回答。
     */
    public String askAvailableModelStream(
            AssistantAnswerPlan plan,
            Consumer<String> contentConsumer,
            AiStreamCancellation cancellation,
            int captureChars
    ) {
        if (plan.directAnswer() != null) {
            contentConsumer.accept(plan.directAnswer());
            return plan.directAnswer();
        }
        String prompt = modelPrompt(plan);
        boolean attemptedModel = false;
        if (largeModelRouter.available(plan.modelRoute().useCase())) {
            try {
                attemptedModel = true;
                String answer = largeModelRouter.chatStream(
                        plan.modelRoute().useCase(),
                        systemPrompt(),
                        prompt,
                        plan.webMode(),
                        contentConsumer,
                        cancellation,
                        captureChars
                );
                if (answer != null && !answer.isBlank()) {
                    return answer;
                }
            } catch (AiStreamCancelledException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                log.warn("Assistant stream model call failed: provider={}, model={}, reason={}",
                        plan.modelRoute().provider(), plan.modelRoute().primaryModel(), ex.getMessage(), ex);
                // 当前配置的大模型调用失败时返回安全兜底，不切换其他 provider，便于排查实际链路。
                String answer = modelFailureFallbackAnswer(plan.modelRoute(), plan.mode(), ex.getMessage());
                contentConsumer.accept(answer);
                return answer;
            }
        }
        cancellation.throwIfCancelled();
        String answer = attemptedModel
                ? modelFailureFallbackAnswer(plan.modelRoute(), plan.mode(), "模型没有返回内容")
                : fallbackAnswer(plan.modelRoute(), plan.mode(), plan.systemContext(), plan.contexts(), plan.webResults());
        contentConsumer.accept(answer);
        return answer;
    }

    /**
     * 根据回答生成统一响应对象。
     */
    public AiAssistantResponse responseFromPlan(AssistantAnswerPlan plan, String answer) {
        return new AiAssistantResponse(
                plan.question(),
                answer,
                modelAvailable(plan.modelRoute()),
                activeModelName(plan.modelRoute()),
                plan.mode(),
                plan.citations(),
                suggestions(plan.mode(), plan.contexts(), plan.webResults()),
                updateConversationSummary(plan.conversationContext(), plan.question(), answer),
                plan.conversationContext().recentRawRounds()
        );
    }

    /**
     * 调用当前可用模型生成回答。
     *
     * <p>实现步骤：
     * 1. 构造统一的 system/user prompt；
     * 2. 根据配置的大模型 provider 调用对应实现；
     * 3. provider 不可用或失败时返回配置提示；
     * 4. 不直接展示内部上下文，避免无模型时越权暴露资料。</p>
     */
    private String askAvailableModel(
            String question,
            String mode,
            ModelRoute modelRoute,
            boolean webMode,
            String conversationContext,
            String systemContext,
            List<KnowledgeSearchResult> contexts,
            List<WebSearchResult> webResults
    ) {
        /**
         * 变量 prompt：保存发送给模型的用户提示词。
         */
        String prompt = userPrompt(
                question,
                mode,
                conversationContext,
                compactSystemContext(systemContext),
                compactLocalContexts(contexts),
                compactWebResults(webResults)
        );
        boolean attemptedModel = false;
        if (largeModelRouter.available(modelRoute.useCase())) {
            try {
                attemptedModel = true;
                /**
                 * 变量 answer：保存当前配置的大模型返回的回答。
                 */
                String answer = largeModelRouter.chat(modelRoute.useCase(), systemPrompt(), prompt, webMode);
                if (answer != null && !answer.isBlank()) {
                    return answer;
                }
            } catch (RuntimeException ex) {
                log.warn("Assistant model call failed: provider={}, model={}, reason={}",
                        modelRoute.provider(), modelRoute.primaryModel(), ex.getMessage(), ex);
                // 当前配置的 provider 不可用或超时时，不隐式切换其他模型，返回明确诊断提示。
                return modelFailureFallbackAnswer(modelRoute, mode, ex.getMessage());
            }
        }
        if (attemptedModel) {
            return modelFailureFallbackAnswer(modelRoute, mode, "模型没有返回内容");
        }
        return fallbackAnswer(modelRoute, mode, systemContext, contexts, webResults);
    }

    /**
     * 构造发送给模型的用户提示词。
     */
    private String modelPrompt(AssistantAnswerPlan plan) {
        return userPrompt(
                plan.question(),
                plan.mode(),
                plan.conversationContext().promptText(),
                compactSystemContext(plan.systemContext()),
                compactLocalContexts(plan.contexts()),
                compactWebResults(plan.webResults())
        );
    }

    /**
     * 判断当前是否存在可用于生成回答的模型。
     *
     * <p>实现步骤：只检查当前配置的大模型 provider，避免配置为 Qwen 时被 Ollama 状态误导。</p>
     */
    private boolean modelAvailable(ModelRoute modelRoute) {
        return largeModelRouter.available(modelRoute.useCase());
    }

    /**
     * 获取当前助手优先显示的模型名称。
     *
     * <p>实现步骤：通过大模型路由器返回当前 provider 的模型显示名称。</p>
     */
    private String activeModelName(ModelRoute modelRoute) {
        return largeModelRouter.displayName(modelRoute.useCase(), modelRoute.label());
    }

    /**
     * 执行 systemPrompt 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String systemPrompt() {
        return """
                你是 Ratel FM 财务 ERP 的企业知识问答助手。
                只能根据用户当前权限下的实时系统上下文、知识上下文和互联网检索上下文回答，不要编造不存在的单据、金额、日期、链接或结论。
                如果上下文不足，明确说明缺少依据，并给出下一步查询建议。
                如果用户询问具体单号、编码、运单号或流水号，必须优先使用完全匹配该编号的上下文；没有完全匹配时，不得把相似编号当成同一条数据。
                回答具体日期、金额、数量和状态时，必须能在上下文中找到原文依据；找不到时回答“当前上下文未提供该字段”。
                会话上下文只用于理解追问里的“它、上一个、刚才”等指代，不得把会话摘要当作实时业务事实依据。
                对新增、修改、删除、审批、确认、取消等动作，不得从会话上下文继承确认意图；必须以用户当前这一次问题中的明确表达为准。
                对“本月、这个月、当月”按实时系统上下文中的本月范围理解。
                对互联网资料必须结合来源标题、链接和网页正文片段说明依据；搜索来源不足时明确说明“互联网检索未提供足够依据”，不要把模型常识当作检索结论。
                内部系统数据和互联网资料冲突时，要区分“系统内数据”和“互联网资料”。
                对金额、日期、单号、状态要保持原文准确。
                回答使用中文，结构清晰，先给结论，再列依据。
                """;
    }

    /**
     * 执行 userPrompt 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String userPrompt(
            String question,
            String mode,
            String conversationContext,
            String systemContext,
            List<KnowledgeSearchResult> contexts,
            List<WebSearchResult> webResults
    ) {
        String contextText = contexts.stream()
                .map(item -> """
                        [来源ID:%s][类型:%s][单号:%s][标题:%s][相关度:%.4f]
                        摘要: %s
                        关键内容: %s
                        """.formatted(item.id(), item.category(), item.sourceNo(), item.title(), item.score(), item.summary(), item.content()))
                .collect(Collectors.joining("\n---\n"));
        String webContextText = webResults.stream()
                .map(item -> """
                        [互联网来源:%s][标题:%s][相关度:%.4f]
                        链接: %s
                        摘要: %s
                        """.formatted(item.source(), item.title(), item.score(), item.url(), item.summary()))
                .collect(Collectors.joining("\n---\n"));
        return """
                用户问题：
                %s

                检索模式：
                %s

                会话上下文：
                %s

                实时系统上下文：
                %s

                本地知识上下文：
                %s

                互联网检索上下文：
                %s

                请基于上述上下文回答。要求：
                1. 回答尽量简洁，只展示“结论”和“关键依据”，关键依据控制在 2-5 条；
                2. 会话上下文只用于理解追问指代，不用于替代实时系统上下文或本地知识上下文；
                3. 涉及系统内统计数量时优先使用“实时系统上下文”的汇总数据；
                4. 涉及具体单据、附件或明细时结合“本地知识上下文”；
                5. 涉及外部政策、行业资料、公开网页或最新公共信息时结合“互联网检索上下文”的标题、链接和摘要，并列出关键来源；
                6. 不要粘贴本地知识、附件、技术文档、代码块、Markdown 表格或长段原文；
                7. 不要使用常识或猜测补齐系统内缺失字段，也不要把未在上下文中出现的网页内容当作依据。
                """.formatted(question, modeLabel(mode), conversationContext.isBlank() ? "无" : conversationContext,
                systemContext.isBlank() ? "无" : systemContext,
                contextText.isBlank() ? "无" : contextText, webContextText.isBlank() ? "无" : webContextText);
    }

    /**
     * 判断是否可以跳过模型并使用本地精确命中内容直接回答。
     *
     * <p>实现步骤：
     * 1. 仅对本地模式或没有互联网结果的混合模式启用，避免用户明确需要互联网资料时跳过搜索来源；
     * 2. 要求问题中存在业务单号、流水号、凭证号等明确编号；
     * 3. 要求检索结果里存在完全匹配编号的本地知识；
     * 4. 命中后直接模板化回答，节省一次千问调用和对应 token。</p>
     */
    private boolean canAnswerFromExactLocalContext(
            String question,
            String mode,
            List<KnowledgeSearchResult> contexts,
            List<WebSearchResult> webResults,
            Set<String> businessTokens
    ) {
        if (businessTokens.isEmpty() || contexts.isEmpty()) {
            return false;
        }
        if ("web".equals(mode) || !webResults.isEmpty()) {
            return false;
        }
        return requiresExactLocalRecord(question, businessTokens)
                && contexts.stream().anyMatch(item -> exactSourceNo(businessTokens, item));
    }

    /**
     * 基于本地精确命中知识生成直接回答。
     *
     * <p>实现步骤：
     * 1. 筛选与用户输入编号完全匹配的知识结果；
     * 2. 最多展示前三条关键依据，避免回答过长；
     * 3. 使用标题和摘要组成可追溯结论，不直接粘贴知识库长原文；
     * 4. 明确提示该回答未额外调用大模型，便于用户理解结果来源。</p>
     */
    private String exactLocalContextAnswer(List<KnowledgeSearchResult> contexts, Set<String> businessTokens) {
        String evidence = contexts.stream()
                .filter(item -> exactSourceNo(businessTokens, item))
                .limit(3)
                .map(item -> "- %s%s：%s。%s".formatted(
                        value(item.category()),
                        value(item.sourceNo()).isBlank() ? "" : " " + value(item.sourceNo()),
                        value(item.title()),
                        truncate(firstAvailable(item.summary(), item.content()), 180)
                ))
                .collect(Collectors.joining("\n"));
        return """
                结论：
                已在本地知识库中找到完全匹配的系统记录。

                关键依据：
                %s

                说明：该结果来自本地精确命中，未额外调用大模型。
                """.formatted(evidence.isBlank() ? "- 暂无可展示依据。" : evidence);
    }

    /**
     * 压缩实时系统上下文。
     *
     * <p>实现步骤：
     * 1. 空上下文直接返回空字符串；
     * 2. 保留前部汇总内容；
     * 3. 超长时截断，避免实时统计全部进入 prompt。</p>
     */
    private String compactSystemContext(String systemContext) {
        return truncate(value(systemContext), MAX_SYSTEM_CONTEXT_CHARS);
    }

    /**
     * 压缩本地知识上下文。
     *
     * <p>实现步骤：
     * 1. 按相关度排序后的结果只取前几条；
     * 2. 每条保留标题、摘要和截断后的关键内容；
     * 3. 构造新的 KnowledgeSearchResult，既保留引用信息，又降低发送给模型的 token。</p>
     */
    private List<KnowledgeSearchResult> compactLocalContexts(List<KnowledgeSearchResult> contexts) {
        return contexts.stream()
                .limit(MAX_LOCAL_CONTEXTS_FOR_MODEL)
                .map(item -> new KnowledgeSearchResult(
                        item.id(),
                        item.type(),
                        item.sourceId(),
                        item.sourceNo(),
                        item.title(),
                        item.category(),
                        truncate(value(item.summary()), 300),
                        truncate(preferContent(item), MAX_LOCAL_CONTENT_CHARS),
                        item.score(),
                        item.routePath()
                ))
                .toList();
    }

    /**
     * 压缩互联网检索上下文。
     *
     * <p>实现步骤：
     * 1. 只取前三条互联网来源；
     * 2. 保留标题、链接、来源和截断摘要；
     * 3. 防止网页正文片段过长进入模型。</p>
     */
    private List<WebSearchResult> compactWebResults(List<WebSearchResult> webResults) {
        return webResults.stream()
                .limit(MAX_WEB_CONTEXTS_FOR_MODEL)
                .map(item -> new WebSearchResult(
                        item.title(),
                        item.url(),
                        truncate(value(item.summary()), MAX_WEB_SUMMARY_CHARS),
                        item.source(),
                        item.score()
                ))
                .toList();
    }

    /**
     * 选择发送给模型的本地知识正文。
     *
     * <p>实现步骤：优先使用完整内容；内容为空时回退摘要，避免 prompt 出现空知识段。</p>
     */
    private String preferContent(KnowledgeSearchResult item) {
        String content = value(item.content());
        return content.isBlank() ? value(item.summary()) : content;
    }

    /**
     * 生成模型未配置时的兜底回答。
     *
     * <p>实现步骤：
     * 1. 不拼接实时系统上下文、知识上下文和互联网资料，避免无模型时把内部上下文直接展示给用户；
     * 2. 告知本地 Ollama 需要先下载当前配置模型；
     * 3. 同时保留千问 API Key 的配置提示，便于用户选择云端模型。</p>
     */
    private String fallbackAnswer(ModelRoute modelRoute, String mode, String systemContext, List<KnowledgeSearchResult> contexts, List<WebSearchResult> webResults) {
        return missingModelConfigurationAnswer(modelRoute);
    }

    /**
     * 生成模型调用失败时的兜底回答。
     *
     * <p>实现步骤：
     * 1. 不展示已检索到的内部上下文，避免把仅供模型使用的业务资料直接暴露在回答中；
     * 2. 提醒用户检查本地模型下载、Ollama 服务状态或千问 Key；
     * 3. 返回简短可操作提示，让用户先恢复模型能力再重新提问。</p>
     */
    private String modelFailureFallbackAnswer(ModelRoute modelRoute, String mode, String failureReason) {
        return modelTemporarilyUnavailableAnswer(modelRoute, failureReason);
    }

    /**
     * 生成未配置任何可用模型时的用户提示。
     *
     * <p>实现步骤：
     * 1. 读取当前配置的大模型 provider 和模型名；
     * 2. 给出 Ollama 或 Qwen 对应配置方向；
     * 3. 明确说明不会在无模型时直接展示内部上下文。</p>
     */
    private String missingModelConfigurationAnswer(ModelRoute modelRoute) {
        String model = modelRoute.primaryModel();
        String provider = modelRoute.provider();
        return """
                当前还不能生成 AI 回答：配置的大模型提供方 `%s` 暂不可用。

                请按当前 provider 检查：
                1. provider=ollama：启动 `ratel-fm-ollama`，确认 Ratel FM 配置的 Ollama 地址可访问，并确认模型 `%s` 已下载；
                2. provider=qwen：配置 `QWEN_API_KEY`，并确认 `QWEN_ENABLED=true`。

                本次问题被识别为“%s”场景，优先模型为 `%s`。
                为避免误导和越权展示，系统不会在没有可用模型时直接展示内部系统上下文。
                """.formatted(provider, model, modelRoute.label(), model);
    }

    /**
     * 生成模型临时不可用时的用户提示。
     *
     * <p>实现步骤：
     * 1. 读取当前配置的大模型 provider 和模型名；
     * 2. 提醒用户按 provider 检查模型、进程或 API Key；
     * 3. 不展示模型 prompt 上下文，避免用户把原始上下文当成模型结论。</p>
     */
    private String modelTemporarilyUnavailableAnswer(ModelRoute modelRoute, String failureReason) {
        String model = modelRoute.primaryModel();
        String provider = modelRoute.provider();
        String reason = failureReason == null || failureReason.isBlank() ? "未返回具体错误原因" : failureReason;
        return """
                AI 模型暂时不可用，未能生成回答。

                本次失败原因：%s

                请检查：
                1. 当前 provider 是否配置为 `%s`；
                2. provider=ollama 时，Ollama 是否已启动、监听地址是否允许 Ratel FM 访问、模型 `%s` 是否已下载；
                3. provider=qwen 时，`QWEN_API_KEY` 是否配置正确。

                为避免误导和越权展示，本次不会直接展示内部系统上下文。请修复模型配置后重新提问。
                """.formatted(reason, provider, model);
    }

    /**
     * 选择本次问答的大模型路由。
     *
     * <p>实现步骤：
     * 1. 语音命令、菜单跳转、填表等短指令优先使用轻量命令模型；
     * 2. 报表分析、原因解释、趋势判断等复杂问题优先使用推理模型；
     * 3. 普通业务问答优先使用默认聊天模型；
     * 4. 路由只描述场景和候选模型，具体 provider 由 LargeModelRouter 按配置选择。</p>
     */
    private ModelRoute selectModelRoute(String question, String mode) {
        if (isCommandQuestion(question, mode)) {
            return modelRoute("语音/操作指令", AiModelUseCase.COMMAND);
        }
        if (isReasoningQuestion(question)) {
            return modelRoute("复杂分析", AiModelUseCase.REASONING);
        }
        return modelRoute("业务问答", AiModelUseCase.CHAT);
    }

    /**
     * 创建当前 provider 下的模型路由。
     *
     * <p>实现步骤：记录业务场景、provider、主模型和候选模型，供回答、流式输出和前端展示复用。</p>
     */
    private ModelRoute modelRoute(String label, AiModelUseCase useCase) {
        return new ModelRoute(
                label,
                useCase,
                largeModelRouter.selectedProviderCode(),
                primaryModel(largeModelRouter.primaryModel(useCase)),
                largeModelRouter.candidateModels(useCase)
        );
    }

    /**
     * 判断问题是否属于操作指令类场景。
     *
     * <p>实现步骤：结合模式、短文本长度和菜单/填表/按钮动词判断，供模型路由选择轻量模型。</p>
     */
    private boolean isCommandQuestion(String question, String mode) {
        String text = value(question);
        if ("command".equals(mode)) {
            return true;
        }
        return text.length() <= 80 && containsAny(text,
                "打开", "进入", "跳转", "切换", "新增", "编辑", "查询", "搜索", "筛选",
                "填写", "填充", "选择", "点击", "关闭", "取消", "重置", "导出", "下载");
    }

    /**
     * 判断问题是否属于复杂分析类场景。
     *
     * <p>实现步骤：按财务分析、原因解释、趋势对比和风险判断关键词识别复杂推理需求。</p>
     */
    private boolean isReasoningQuestion(String question) {
        String text = value(question);
        return containsAny(text,
                "分析", "原因", "为什么", "趋势", "预测", "风险", "异常", "对比",
                "同比", "环比", "占比", "报表", "利润", "现金流", "资产负债",
                "试算平衡", "逾期", "到期", "汇总", "统计", "建议", "优化");
    }

    /**
     * 生成模型路由主模型名称。
     *
     * <p>实现步骤：空模型名回退默认业务问答模型，保证用户提示里的 pull 命令可执行。</p>
     */
    private String primaryModel(String model) {
        return model == null || model.isBlank() ? "qwen2.5:7b" : model;
    }

    /**
     * ModelRoute 数据传输记录。
     *
     * <p>用于承载模型路由标签、业务场景、provider、主模型和候选列表，便于按配置选择不同大模型实现。</p>
     */
    public record ModelRoute(String label, AiModelUseCase useCase, String provider, String primaryModel, List<String> models) {
    }

    /**
     * AssistantAnswerPlan 数据传输记录。
     *
     * <p>用于复用非流式与流式接口的检索、权限过滤、会话上下文和直接回答判断。</p>
     */
    public record AssistantAnswerPlan(
            String question,
            String mode,
            ModelRoute modelRoute,
            ConversationContext conversationContext,
            boolean webMode,
            String systemContext,
            List<KnowledgeSearchResult> contexts,
            List<WebSearchResult> webResults,
            List<AiCitation> citations,
            String directAnswer
    ) {
    }

    /**
     * 执行 toCitation 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private AiCitation toCitation(KnowledgeSearchResult item) {
        return new AiCitation(
                item.id(),
                item.type(),
                item.sourceId(),
                item.sourceNo(),
                item.title(),
                item.category(),
                item.summary(),
                item.score(),
                item.routePath(),
                null
        );
    }

    /**
     * 执行 toCitation 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private AiCitation toCitation(WebSearchResult item) {
        return new AiCitation(
                null,
                "WEB",
                null,
                item.url(),
                item.title(),
                "互联网",
                item.summary(),
                item.score(),
                null,
                item.url()
        );
    }

    /**
     * 执行 suggestions 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<String> suggestions(String mode, List<KnowledgeSearchResult> contexts, List<WebSearchResult> webResults) {
        if (contexts.isEmpty() && webResults.isEmpty()) {
            return List.of("重建知识索引", "尝试输入单号或往来单位", "检查当前账号是否有对应模块权限");
        }
        if ("web".equals(mode)) {
            return List.of("打开互联网来源复核", "补充关键词继续检索", "切换混合检索关联本地业务数据");
        }
        if ("hybrid".equals(mode)) {
            return List.of("区分本地数据和互联网资料", "打开引用来源复核", "补充日期范围或业务单号继续追问");
        }
        return List.of(
                "查看引用来源对应单据",
                "补充日期范围或业务单号继续追问",
                "对检索结果中的金额、状态和到期日期做复核"
        );
    }

    /**
     * 构造会话上下文。
     *
     * <p>实现步骤：
     * 1. 如果配置关闭会话上下文，则返回空上下文；
     * 2. 按配置保留最近若干轮用户/助手原文；
     * 3. 对摘要、单条消息和整体上下文做字符级裁剪，避免小模型上下文过大。</p>
     */
    private ConversationContext buildConversationContext(
            String conversationSummary,
            List<ConversationMessage> conversationMessages,
            String currentQuestion
    ) {
        AiProperties.Assistant config = assistantConfig();
        if (!config.isConversationEnabled()) {
            return new ConversationContext("", "", List.of(), 0);
        }
        int rounds = Math.max(0, config.getRecentRawRounds());
        int messageLimit = rounds * 2;
        List<ConversationMessage> recentMessages = sanitizeMessages(conversationMessages, messageLimit, config.getMaxMessageChars());
        String summary = truncate(value(conversationSummary), Math.max(0, config.getMaxSummaryChars()));
        String recentText = recentMessages.stream()
                .map(item -> (isAssistantRole(item.role()) ? "助手" : "用户") + ": " + item.content())
                .collect(Collectors.joining("\n"));
        String promptText = lines(
                "会话短摘要: " + (summary.isBlank() ? "无" : summary),
                "最近原文轮次上限: " + rounds,
                "最近原文:",
                recentText.isBlank() ? "无" : recentText,
                "当前问题会重新检索实时系统上下文和知识库；历史会话只用于理解追问，不作为增删改确认依据。",
                containsDangerousAction(currentQuestion)
                        ? "当前问题包含可能的增删改/确认动作词，必须仅依据当前问题判断是否明确要求执行。"
                        : ""
        );
        return new ConversationContext(
                truncate(promptText, Math.max(0, config.getMaxContextChars())),
                summary,
                recentMessages,
                rounds
        );
    }

    /**
     * 更新会话短摘要。
     */
    private String updateConversationSummary(ConversationContext context, String question, String answer) {
        AiProperties.Assistant config = assistantConfig();
        if (!config.isConversationEnabled()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        if (!context.summary().isBlank()) {
            lines.add(context.summary());
        }
        String topicLine = conciseConversationLine(question, answer);
        if (!topicLine.isBlank()) {
            lines.add(topicLine);
        }
        Set<String> businessTokens = extractBusinessTokens(question + " " + answer);
        if (!businessTokens.isEmpty()) {
            lines.add("已提到的业务编号: " + String.join("、", businessTokens));
        }
        String summary = lines.stream()
                .map(this::stripDangerousConfirmation)
                .filter(item -> !item.isBlank())
                .distinct()
                .collect(Collectors.joining("\n"));
        return truncate(summary, Math.max(0, config.getMaxSummaryChars()));
    }

    /**
     * 构造单轮摘要行。
     */
    private String conciseConversationLine(String question, String answer) {
        String safeQuestion = stripDangerousConfirmation(truncate(value(question), 260));
        String safeAnswer = stripDangerousConfirmation(truncate(value(answer), 420));
        if (safeQuestion.isBlank() && safeAnswer.isBlank()) {
            return "";
        }
        return "最近关注: 用户问“" + safeQuestion + "”；助手答复要点“" + safeAnswer + "”";
    }

    /**
     * 清理历史消息并按配置裁剪。
     */
    private List<ConversationMessage> sanitizeMessages(List<ConversationMessage> messages, int limit, int maxMessageChars) {
        if (messages == null || messages.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<ConversationMessage> sanitized = messages.stream()
                .filter(item -> item != null && value(item.content()).trim().length() > 0)
                .map(item -> new ConversationMessage(
                        isAssistantRole(item.role()) ? "assistant" : "user",
                        truncate(stripDangerousConfirmation(item.content()), Math.max(0, maxMessageChars))
                ))
                .filter(item -> !item.content().isBlank())
                .toList();
        if (sanitized.size() <= limit) {
            return sanitized;
        }
        return sanitized.subList(sanitized.size() - limit, sanitized.size());
    }

    /**
     * 过滤历史摘要中的确认动作，避免下一轮继承危险意图。
     */
    private String stripDangerousConfirmation(String value) {
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

    /**
     * 判断文本是否包含可能的增删改确认动作。
     */
    private boolean containsDangerousAction(String text) {
        return containsAny(value(text), "删除", "作废", "取消", "审批", "确认", "提交", "新增", "修改", "编辑", "保存", "批量");
    }

    /**
     * 判断消息角色是否为助手。
     */
    private boolean isAssistantRole(String role) {
        return "assistant".equalsIgnoreCase(value(role).trim());
    }

    /**
     * 构造检索用问题。
     */
    private String retrievalQuestion(String question, ConversationContext conversationContext) {
        if (!looksLikeFollowUp(question)) {
            return question;
        }
        String recentUserContext = conversationContext.recentMessages().stream()
                .filter(item -> !isAssistantRole(item.role()))
                .map(ConversationMessage::content)
                .filter(item -> !item.isBlank())
                .reduce((previous, current) -> previous + "\n" + current)
                .orElse("");
        return truncate(lines(
                question,
                conversationContext.summary().isBlank() ? "" : "会话摘要用于补充追问指代: " + conversationContext.summary(),
                recentUserContext.isBlank() ? "" : "最近用户问题用于补充追问指代:\n" + recentUserContext
        ), 1200);
    }

    /**
     * 判断当前问题是否像追问。
     */
    private boolean looksLikeFollowUp(String question) {
        String text = value(question).trim();
        return text.length() <= 80 && containsAny(text, "它", "这个", "那个", "上一个", "上一条", "刚才", "这些", "他们", "对应", "继续", "再查", "详情", "我问的是", "有没有文件", "是否有文件", "有没有资料", "是否有资料");
    }

    /**
     * 获取 AI 助手配置，避免配置绑定为空导致空指针。
     */
    private AiProperties.Assistant assistantConfig() {
        return aiProperties.getAssistant() == null ? new AiProperties.Assistant() : aiProperties.getAssistant();
    }

    /**
     * 拼接非空行。
     */
    private String lines(Object... values) {
        List<String> lines = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = value.toString();
            if (!text.isBlank()) {
                lines.add(text);
            }
        }
        return String.join("\n", lines);
    }

    /**
     * 执行 normalizeMode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "hybrid";
        }
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (List.of("local", "web", "hybrid", "command").contains(normalized)) {
            return normalized;
        }
        return "hybrid";
    }

    /**
     * 执行 modeLabel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String modeLabel(String mode) {
        return switch (mode) {
            case "local" -> "本地知识库";
            case "web" -> "互联网检索";
            case "command" -> "语音/操作指令";
            default -> "混合检索";
        };
    }

    /**
     * 执行 noContextAnswer 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String noContextAnswer(String mode) {
        if ("web".equals(mode)) {
            return webSearchService.enabled()
                    ? "互联网检索没有找到可用来源，无法给出可靠回答。请换一个更具体的关键词，或稍后重试。"
                    : "互联网检索未启用，无法查询外部信息。";
        }
        if ("local".equals(mode)) {
            return "当前系统没有检索到可作为依据的业务数据或知识文档，无法给出可靠回答。请先重建知识索引，或换一个更具体的问题。";
        }
        return "当前系统和互联网检索都没有找到可作为依据的资料，无法给出可靠回答。请补充关键词、单号、日期或切换检索模式。";
    }

    /**
     * 判断用户是否只是在确认系统中有没有相关文件或资料。
     */
    private boolean asksForFileExistence(String question, ConversationContext conversationContext) {
        String text = value(question).trim();
        String contextText = value(conversationContext.summary()) + "\n" + conversationContext.recentMessages().stream()
                .filter(item -> !isAssistantRole(item.role()))
                .map(ConversationMessage::content)
                .collect(Collectors.joining("\n"));
        boolean asksExistence = containsAny(text, "有没有", "是否有", "有没有文件", "有没有资料", "有关于", "有哪些文件", "哪些文件", "找文件", "查文件", "我问的是");
        boolean fileScope = containsAny(text + "\n" + contextText, "文件", "资料", "文档", "附件", "知识库");
        return asksExistence && fileScope;
    }

    /**
     * 文件存在性问题只回答是否找到和关键依据，不展开文件正文。
     */
    private String fileExistenceAnswer(List<KnowledgeSearchResult> contexts) {
        List<KnowledgeSearchResult> fileResults = contexts.stream()
                .filter(item -> containsAny(value(item.type()) + value(item.category()) + value(item.title()) + value(item.summary()), "ATTACHMENT", "LOCAL_KNOWLEDGE", "文件", "资料", "文档", "附件", "知识库", ".pdf", ".doc", ".docx", ".txt", ".md"))
                .limit(3)
                .toList();
        if (fileResults.isEmpty()) {
            return """
                    结论：
                    当前本地知识库没有检索到明确匹配的文件。

                    关键依据：
                    - 本次检索没有返回可识别为文件、资料、文档或附件的结果。
                    - 可以换用文件名、主题关键词或上传人继续检索。
                    """;
        }
        String evidence = fileResults.stream()
                .map(item -> "- %s：%s%s".formatted(
                        value(item.category()).isBlank() ? "本地知识库" : value(item.category()),
                        value(item.title()).isBlank() ? "未命名文件" : value(item.title()),
                        value(item.sourceNo()).isBlank() ? "" : "（" + value(item.sourceNo()) + "）"
                ))
                .distinct()
                .collect(Collectors.joining("\n"));
        return """
                结论：
                有，当前本地知识库检索到相关文件。

                关键依据：
                %s
                """.formatted(evidence);
    }

    /**
     * 执行 exactRecordNotFoundAnswer 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String exactRecordNotFoundAnswer(Set<String> tokens) {
        // 变量说明：tokenText 保存当前步骤计算、查询或转换得到的中间结果。
        String tokenText = tokens.isEmpty() ? "该编号" : String.join("、", tokens);
        return "当前系统未检索到与 " + tokenText + " 完全匹配的业务记录，无法回答该单据的具体字段。请核对单号、运单号、流水号或关联业务单号，或先重建知识索引后再查询。";
    }

    /**
     * 执行 prioritizeExactMatches 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<KnowledgeSearchResult> prioritizeExactMatches(String question, List<KnowledgeSearchResult> contexts) {
        if (contexts.isEmpty()) {
            return contexts;
        }
        // 变量说明：tokens 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> tokens = extractBusinessTokens(question);
        if (tokens.isEmpty()) {
            return contexts;
        }
        return contexts.stream()
                .sorted(Comparator.comparing((KnowledgeSearchResult item) -> exactSourceNo(tokens, item) ? 0 : 1)
                        .thenComparing(KnowledgeSearchResult::score, Comparator.reverseOrder()))
                .toList();
    }

    /**
     * 执行 exactSourceNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean exactSourceNo(Set<String> tokens, KnowledgeSearchResult item) {
        return item.sourceNo() != null && tokens.stream().anyMatch(token -> item.sourceNo().equalsIgnoreCase(token));
    }

    /**
     * 执行 requiresExactLocalRecord 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean requiresExactLocalRecord(String question, Set<String> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }
        if (tokens.stream().anyMatch(this::containsLetter)) {
            return true;
        }
        return containsAny(question, "单号", "编号", "流水号", "运单", "订单", "凭证号", "采购单", "物流单", "库存流水", "关联业务单号");
    }

    /**
     * 执行 containsLetter 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean containsLetter(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行 containsAny 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
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

    /**
     * 空值转空字符串。
     *
     * <p>实现步骤：判断传入文本是否为 null；为空时返回空字符串；非空时原样返回，方便后续截断和拼接。</p>
     */
    private String value(String value) {
        return value == null ? "" : value;
    }

    /**
     * 返回第一个非空文本。
     *
     * <p>实现步骤：按调用方给定顺序查找；遇到非空字符串立即返回；全部为空时返回空字符串。</p>
     */
    private String firstAvailable(String... values) {
        for (String item : values) {
            if (item != null && !item.isBlank()) {
                return item;
            }
        }
        return "";
    }

    /**
     * 按字符数截断文本。
     *
     * <p>实现步骤：
     * 1. 空值兜底为空字符串；
     * 2. 未超过上限时原样返回；
     * 3. 超过上限时保留前部内容并追加省略标记，减少发送给模型和前端展示的冗余文本。</p>
     */
    private String truncate(String value, int maxChars) {
        String text = value(value).trim();
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars)) + "...";
    }

    /**
     * 执行 extractBusinessTokens 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Set<String> extractBusinessTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        // 变量说明：matcher 保存当前步骤计算、查询或转换得到的中间结果。
        Matcher matcher = BUSINESS_TOKEN_PATTERN.matcher(value);
        // 变量说明：tokens 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> tokens = new LinkedHashSet<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /**
     * ConversationMessage 数据传输记录。
     *
     * <p>用于承载前端传入的最近会话消息。</p>
     */
    public record ConversationMessage(String role, String content) {
    }

    /**
     * ConversationContext 数据传输记录。
     *
     * <p>用于承载裁剪后的会话上下文。</p>
     */
    public record ConversationContext(
            String promptText,
            String summary,
            List<ConversationMessage> recentMessages,
            int recentRawRounds
    ) {
    }
}
