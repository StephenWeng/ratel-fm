package com.ratel.fm.service.knowledge;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import com.ratel.fm.repository.knowledge.KnowledgeDocumentRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.ai.AiModelUseCase;
import com.ratel.fm.service.ai.LargeModelRouter;
import com.ratel.fm.service.ai.OllamaClient;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResponse;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 知识混合检索服务。
 */
@Service
public class KnowledgeSearchService {

    /**
     * 业务编号识别正则，用于将用户查询中的单号、凭证号等关键词加入召回条件。
     */
    private static final Pattern BUSINESS_TOKEN_PATTERN = Pattern.compile("(?i)[a-z]{1,12}\\d{4,}|\\d{4,}");
    /**
     * 常量 CONTEXT_SCORE_THRESHOLD：AI 助手上下文最低相关度，低于该值且无精确单号命中的结果不进入 prompt。
     */
    private static final double CONTEXT_SCORE_THRESHOLD = 0.28D;
    /**
     * 常量 MAX_QUERY_VARIANTS：单次检索最多保留的 query 改写数量，限制模型改写或规则扩展导致的召回成本。
     */
    private static final int MAX_QUERY_VARIANTS = 6;
    /**
     * Qdrant 关键词补召回最多扫描的 payload 分片数，避免一次自然语言检索长时间占用 Qdrant。
     */
    private static final int MAX_QDRANT_KEYWORD_SCAN_POINTS = 600;
    /**
     * ratel助手上下文候选池下限，先扩大召回再精裁，避免直接命中被向量相似度排序挤出 prompt。
     */
    private static final int MIN_ASSISTANT_CONTEXT_CANDIDATES = 80;
    /**
     * 智能检索页面允许的最大关键词长度，和 ratel助手问题输入保持一致。
     */
    private static final int MAX_SEARCH_KEYWORD_LENGTH = 500;
    /**
     * H2 关键词检索每个核心词最多预取的候选分片数，避免大字典场景每次把全量知识表读入内存。
     */
    private static final int MAX_H2_KEYWORD_CANDIDATES_PER_TERM = 1200;

    /**
     * 字段 knowledgeRepository：H2 向量库模式下读取当前用户可见知识分片。
     */
    private final KnowledgeDocumentRepository knowledgeRepository;
    /**
     * 字段 knowledgeIndexService：用于轻量检查当前向量库索引是否可访问，不在检索链路触发重建。
     */
    private final KnowledgeIndexService knowledgeIndexService;
    /**
     * 字段 largeModelRouter：按配置选择 query 改写使用的对话模型提供方。
     */
    private final LargeModelRouter largeModelRouter;
    /**
     * 字段 ollamaClient：本地 Ollama 客户端，用于生成检索查询向量。
     */
    private final OllamaClient ollamaClient;
    /**
     * 字段 qdrantKnowledgeClient：Qdrant 向量库客户端，启用外部向量库时用于语义召回。
     */
    private final QdrantKnowledgeClient qdrantKnowledgeClient;
    /**
     * 字段 backendRouter：集中处理 H2/Qdrant 读侧后端和检索模式判断。
     */
    private final KnowledgeSearchBackendRouter backendRouter;
    /**
     * 字段 aiProperties：读取检索分片、embedding、query 改写和向量库配置。
     */
    private final AiProperties aiProperties;

    /**
     * 构造 KnowledgeSearchService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public KnowledgeSearchService(
            KnowledgeDocumentRepository knowledgeRepository,
            KnowledgeIndexService knowledgeIndexService,
            LargeModelRouter largeModelRouter,
            OllamaClient ollamaClient,
            QdrantKnowledgeClient qdrantKnowledgeClient,
            KnowledgeSearchBackendRouter backendRouter,
            AiProperties aiProperties
    ) {
        this.knowledgeRepository = knowledgeRepository;
        this.knowledgeIndexService = knowledgeIndexService;
        this.largeModelRouter = largeModelRouter;
        this.ollamaClient = ollamaClient;
        this.qdrantKnowledgeClient = qdrantKnowledgeClient;
        this.backendRouter = backendRouter;
        this.aiProperties = aiProperties;
    }

    @Transactional(readOnly = true)
    /**
     * 执行智能检索页面查询。
     *
     * <p>实现步骤：
     * 1. 规范化关键词和检索模式；
     * 2. 检查当前向量库索引是否可访问；
     * 3. 生成 query 改写，按当前向量库执行 H2 或 Qdrant 检索；
     * 4. 过滤掉与用户意图明显不匹配的基础资料类结果并返回前端 DTO。</p>
     */
    public KnowledgeSearchResponse search(String keyword, String mode, int limit) {
        // 去除用户输入首尾空白，避免空白查询触发索引访问。
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        // 将前端传入模式限制到 keyword、semantic、hybrid 三种。
        String normalizedMode = normalizeMode(mode);
        if (normalizedKeyword.isBlank()) {
            return new KnowledgeSearchResponse(keyword, normalizedMode, aiAvailable(), 0, List.of(), List.of());
        }
        if (normalizedKeyword.length() > MAX_SEARCH_KEYWORD_LENGTH) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ResponseCode.ILLEGAL_PARAM,
                    "检索关键词长度不能超过500个字符。");
        }
        ensureIndexed();
        // query 改写用于扩展同义词、业务简称和可能的菜单/单据表达。
        List<String> queries = rewriteQueries(normalizedKeyword);
        int resultLimit = safeLimit(limit);
        int candidateLimit = KnowledgeResultIntentFilter.hasStructuredIntent(normalizedKeyword)
                ? Math.min(MAX_QDRANT_KEYWORD_SCAN_POINTS, Math.max(80, resultLimit * 5))
                : resultLimit;
        List<KnowledgeSearchResult> results = searchInternal(queries, normalizedMode, candidateLimit).stream()
                .filter(item -> KnowledgeResultIntentFilter.matches(normalizedKeyword, item))
                .filter(item -> visibleInSmartSearch(normalizedKeyword, item))
                .limit(resultLimit)
                .toList();
        return new KnowledgeSearchResponse(keyword, normalizedMode, aiAvailable(), results.size(), rewrittenOnly(normalizedKeyword, queries), results);
    }

    @Transactional(readOnly = true)
    /**
     * 为 ratel助手检索可放入 prompt 的上下文。
     *
     * <p>实现步骤：
     * 1. 使用 hybrid 模式召回本地知识；
     * 2. 按权限、账套和智能检索可见性过滤；
     * 3. 只保留达到相关度阈值或精确单号命中的结果，避免低相关内容进入模型上下文。</p>
     */
    public List<KnowledgeSearchResult> searchForContext(String question) {
        ensureIndexed();
        // 助手上下文检索允许空问题返回空结果，但不允许跨账套越权。
        String normalizedQuestion = question == null ? "" : question.trim();
        // 助手和搜索页面共用 query 改写逻辑，保持召回口径一致。
        List<String> queries = rewriteQueries(normalizedQuestion);
        int contextLimit = Math.max(1, aiProperties.getKnowledge().getMaxContextDocuments());
        int candidateLimit = Math.min(MAX_QDRANT_KEYWORD_SCAN_POINTS,
                Math.max(MIN_ASSISTANT_CONTEXT_CANDIDATES, contextLimit * 12));
        return searchInternal(queries, "hybrid", candidateLimit).stream()
                .filter(item -> KnowledgeResultIntentFilter.matches(normalizedQuestion, item))
                .filter(item -> visibleInSmartSearch(normalizedQuestion, item) || supplementalDirectContextAllowed(item, normalizedQuestion))
                .filter(item -> item.score() >= CONTEXT_SCORE_THRESHOLD
                        || exactTokenMatch(question, item)
                        || supplementalDirectContextAllowed(item, normalizedQuestion))
                .sorted(assistantContextComparator(normalizedQuestion))
                .limit(contextLimit)
                .toList();
    }

    /**
     * 使用单个关键词执行内部检索。
     *
     * <p>实现步骤：包装为单元素 query 列表后复用多 query 检索逻辑，避免两套评分代码。</p>
     */
    private List<KnowledgeSearchResult> searchInternal(String keyword, String mode, int limit) {
        return searchInternal(List.of(keyword), mode, limit);
    }

    /**
     * 按当前向量库执行核心检索。
     *
     * <p>实现步骤：
     * 1. 读取当前用户权限和所属公司，所有召回都必须在该范围内过滤；
     * 2. 根据检索模式和向量库生成查询 embedding；
     * 3. Qdrant 模式只访问 Qdrant payload，不回退 H2；
     * 4. H2 模式读取 H2 知识表并在 Java 侧进行关键词/向量混合评分。</p>
     */
    private List<KnowledgeSearchResult> searchInternal(List<String> keywords, String mode, int limit) {
        // 检索权限必须来自当前登录用户，避免助手通过上下文看到未授权菜单或业务数据。
        CurrentUser currentUser = SecurityUtils.currentUser();
        // 空权限集合只允许命中无需权限码的公共知识分片。
        Set<PermissionCode> permissions = currentUser.permissions() == null ? Set.of() : currentUser.permissions();
        // 去重和裁剪后的 query 列表，防止模型改写生成过多变体。
        List<String> effectiveKeywords = normalizeQueries(keywords);
        if (effectiveKeywords.isEmpty()) {
            return List.of();
        }
        List<QueryProfile> queryProfiles = new ArrayList<>();
        boolean needsEmbedding = shouldUseEmbedding(mode);
        boolean qdrantMode = useQdrantVectorStore();
        boolean keywordAvailable = shouldUseKeyword(mode);
        for (String keyword : effectiveKeywords) {
            List<Double> embedding = List.of();
            if (needsEmbedding) {
                if (!ollamaClient.embeddingAvailable()) {
                    if (qdrantMode && !keywordAvailable) {
                        throw qdrantEmbeddingUnavailable(null);
                    }
                } else {
                    embedding = qdrantMode ? qdrantEmbedding(keyword, keywordAvailable) : safeEmbedding(keyword);
                }
            }
            queryProfiles.add(new QueryProfile(keyword, embedding));
        }
        if (useQdrantVectorStore()) {
            return searchWithQdrant(permissions, CompanyScope.currentCompanyCode(), queryProfiles, mode, limit);
        }
        // H2 模式根据 embedding 是否可用决定候选读取方式：纯关键词走数据库预筛选，向量评分才读取全量可见分片。
        List<KnowledgeDocument> documents = h2CandidateDocuments(permissions, CompanyScope.currentCompanyCode(), queryProfiles, mode, limit);
        // 同一分片可能被多个 query 命中，只保留最高分。
        Map<Long, ScoredDocument> bestByDocumentId = new LinkedHashMap<>();
        for (KnowledgeDocument document : documents) {
            for (QueryProfile query : queryProfiles) {
                // 计算关键词、向量和单号意图加权后的最终分数。
                ScoredDocument scored = score(document, query.keyword(), mode, query.embedding());
                if (scored.score() <= 0) {
                    continue;
                }
                bestByDocumentId.merge(document.getId(), scored,
                        (left, right) -> right.score() > left.score() ? right : left);
            }
        }
        List<ScoredDocument> scoredDocuments = bestByDocumentId.values().stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed()
                        .thenComparing(item -> item.document().getModifyTime(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
        return scoredDocuments.stream()
                .map(item -> toResult(item.document(), item.score()))
                .toList();
    }

    /**
     * 读取 H2 检索候选分片。
     *
     * <p>实现步骤：如果本次有查询向量，则保留全量可见分片用于余弦评分；否则先用核心词在数据库侧筛选候选，
     * 避免全国行政区划等大字典写入后，每次检索都把全部知识分片加载到 JVM。</p>
     */
    private List<KnowledgeDocument> h2CandidateDocuments(
            Set<PermissionCode> permissions,
            String organizationCode,
            List<QueryProfile> queryProfiles,
            String mode,
            int limit
    ) {
        boolean hasQueryEmbedding = shouldUseEmbedding(mode)
                && queryProfiles.stream().anyMatch(item -> !item.embedding().isEmpty());
        if (hasQueryEmbedding) {
            return knowledgeRepository.findVisibleInCompany(permissions, organizationCode);
        }
        LinkedHashMap<Long, KnowledgeDocument> documents = new LinkedHashMap<>();
        int pageSize = Math.min(MAX_H2_KEYWORD_CANDIDATES_PER_TERM, Math.max(200, limit * 20));
        for (QueryProfile query : queryProfiles) {
            for (String term : h2CandidateTerms(query.keyword())) {
                for (KnowledgeDocument document : knowledgeRepository.findVisibleInCompanyMatchingTerm(
                        permissions,
                        organizationCode,
                        term,
                        PageRequest.of(0, pageSize)
                )) {
                    documents.putIfAbsent(document.getId(), document);
                }
            }
        }
        return documents.values().stream().toList();
    }

    /**
     * 使用 Qdrant 执行向量召回，并在 Qdrant payload 中完成权限过滤、账套隔离和结果转换。
     */
    private List<KnowledgeSearchResult> searchWithQdrant(Set<PermissionCode> permissions, String organizationCode, List<QueryProfile> queryProfiles, String mode, int limit) {
        boolean useVector = shouldUseEmbedding(mode);
        boolean useKeyword = shouldUseKeyword(mode);
        if (!useVector && !useKeyword) {
            return List.of();
        }
        if (useVector && !useKeyword && queryProfiles.stream().allMatch(item -> item.embedding().isEmpty())) {
            throw qdrantEmbeddingUnavailable(null);
        }
        Map<String, ScoredPointResult> bestByPointId = new LinkedHashMap<>();
        // Qdrant 会把 payload 一起返回，召回数量过大会放大响应体，先取较小候选集再用关键词补召回兜住精确词。
        int vectorLimit = Math.min(80, Math.max(limit * 3, limit + 10));
        if (useVector) {
            for (QueryProfile query : queryProfiles) {
                if (query.embedding().isEmpty()) {
                    continue;
                }
                for (QdrantKnowledgeClient.ScoredPoint point : qdrantKnowledgeClient.search(query.embedding(), vectorLimit)) {
                    if (!visiblePoint(point, permissions, organizationCode)) {
                        continue;
                    }
                    KnowledgeSearchResult result = toResult(point, query.keyword(), mode);
                    if (result.score() <= 0) {
                        continue;
                    }
                    bestByPointId.merge(point.id(), new ScoredPointResult(result, point.score()),
                            (left, right) -> right.result().score() > left.result().score() ? right : left);
                }
            }
        }
        if (useKeyword) {
            boolean hasVectorQuery = queryProfiles.stream().anyMatch(item -> !item.embedding().isEmpty());
            // 关键词补召回仍只走 Qdrant，但根据向量是否可用动态控制扫描预算，避免自然语言检索拖到前端超时。
            int keywordScanLimit = qdrantKeywordScanLimit(mode, limit, hasVectorQuery, bestByPointId.isEmpty());
            for (QdrantKnowledgeClient.ScoredPoint point : qdrantKnowledgeClient.scrollPayloads(keywordScanLimit)) {
                if (!visiblePoint(point, permissions, organizationCode)) {
                    continue;
                }
                for (QueryProfile query : queryProfiles) {
                    KnowledgeSearchResult result = toResult(point, query.keyword(), "keyword");
                    if (result.score() <= 0) {
                        continue;
                    }
                    bestByPointId.merge(point.id(), new ScoredPointResult(result, point.score()),
                            (left, right) -> right.result().score() > left.result().score() ? right : left);
                }
            }
        }
        return bestByPointId.values().stream()
                .sorted(Comparator.comparingDouble((ScoredPointResult item) -> item.result().score()).reversed())
                .limit(limit)
                .map(ScoredPointResult::result)
                .toList();
    }

    /**
     * 计算 Qdrant 关键词补召回扫描预算。
     *
     * <p>实现步骤：纯关键词或 embedding 不可用时给更高扫描预算；混合检索已拿到向量候选时只做小范围精确词补充。</p>
     */
    private int qdrantKeywordScanLimit(String mode, int limit, boolean hasVectorQuery, boolean noVectorCandidates) {
        int requestedLimit = Math.max(1, limit);
        int scanLimit;
        if ("keyword".equals(mode) || !hasVectorQuery) {
            scanLimit = Math.max(requestedLimit * 12, 240);
        } else if (noVectorCandidates) {
            scanLimit = Math.max(requestedLimit * 8, 160);
        } else {
            scanLimit = Math.max(requestedLimit * 4, 80);
        }
        return Math.min(MAX_QDRANT_KEYWORD_SCAN_POINTS, scanLimit);
    }

    /**
     * 将 Qdrant payload 转换为检索结果。
     */
    private KnowledgeSearchResult toResult(QdrantKnowledgeClient.ScoredPoint point, String keyword, String mode) {
        com.alibaba.fastjson2.JSONObject payload = point.payload();
        KnowledgeSourceType sourceType = parseSourceType(payload.getString("sourceType"));
        double keywordScore = shouldUseKeyword(mode) ? keywordScore(payload, keyword) : 0;
        double vectorScore = Math.max(0, Math.min(1.0, point.score()));
        double score = switch (mode) {
            case "semantic" -> vectorScore;
            case "keyword" -> keywordScore;
            default -> keywordScore * 0.35 + vectorScore * 0.65;
        };
        if (exactSourceNoMatch(payload.getString("sourceNo"), keyword)) {
            score = 1.0;
        } else if (sourceNoContainsBusinessToken(payload.getString("sourceNo"), keyword)) {
            score = Math.max(score, 0.85);
        }
        if (score > 0) {
            score = Math.min(1.0, score + sourceIntentBoost(sourceType, keyword));
        }
        return new KnowledgeSearchResult(
                null,
                sourceType == null ? payload.getString("sourceType") : sourceType.name(),
                payload.getLong("sourceId"),
                payload.getString("sourceNo"),
                payload.getString("title"),
                payload.getString("category"),
                payload.getString("summary"),
                payload.getString("content"),
                Math.round(score * 10000D) / 10000D,
                routePath(payload.getString("metadata"))
        );
    }

    /**
     * 计算 H2 模式下单个知识分片的最终相关度。
     *
     * <p>实现步骤：
     * 1. 按检索模式计算关键词分和可选向量余弦分；
     * 2. hybrid 模式对关键词和向量加权，保障自然语言与精确业务编号都能命中；
     * 3. 精确单号和业务类型意图会额外提升分数。</p>
     */
    private ScoredDocument score(KnowledgeDocument document, String keyword, String mode, List<Double> queryEmbedding) {
        // 关键词分用于保障单号、标题、菜单名和业务术语的确定性命中。
        double keywordScore = shouldUseKeyword(mode) ? keywordScore(document, keyword) : 0;
        // 向量分用于覆盖自然语言相近但不完全同词的查询。
        double vectorScore = 0;
        boolean vectorAvailable = shouldUseEmbedding(mode) && !queryEmbedding.isEmpty() && document.getEmbeddingJson() != null;
        if (vectorAvailable) {
            vectorScore = cosine(queryEmbedding, parseEmbedding(document.getEmbeddingJson()));
        }
        double score = switch (mode) {
            case "semantic" -> vectorAvailable ? vectorScore : keywordScore;
            case "keyword" -> keywordScore;
            default -> vectorAvailable
                    ? Math.max(keywordScore, keywordScore * 0.45 + vectorScore * 0.55)
                    : keywordScore;
        };
        if (exactSourceNoMatch(document, keyword)) {
            score = 1.0;
        } else if (sourceNoContainsBusinessToken(document, keyword)) {
            score = Math.max(score, 0.85);
        }
        if (score > 0) {
            score = Math.min(1.0, score + sourceIntentBoost(document, keyword));
        }
        return new ScoredDocument(document, score);
    }

    /**
     * 计算 H2 知识分片的关键词相关度。
     *
     * <p>实现步骤：
     * 1. 将标题、业务编号、分类、摘要和正文合并为搜索文本；
     * 2. 按拆分词命中正文、标题和业务编号累加权重；
     * 3. 精确单号或业务编号 token 命中时额外加分。</p>
     */
    private double keywordScore(KnowledgeDocument document, String keyword) {
        String haystack = (document.getTitle() + "\n" + document.getSourceNo() + "\n" + document.getCategory()
                + "\n" + document.getSummary() + "\n" + document.getContent()).toLowerCase(Locale.ROOT);
        // 查询拆词兼容中文短语、英文编号和数字单号。
        List<String> terms = splitTerms(keyword);
        if (terms.isEmpty()) {
            return 0;
        }
        // 得分上限最终裁剪到 1，避免关键词堆叠压过精确匹配规则。
        double score = 0;
        for (String term : terms) {
            if (term.isBlank()) {
                continue;
            }
            // 统一小写后匹配英文编码，中文内容不受大小写处理影响。
            String lowerTerm = term.toLowerCase(Locale.ROOT);
            if (haystack.contains(lowerTerm)) {
                score += 0.35;
            }
            if (document.getTitle() != null && document.getTitle().toLowerCase(Locale.ROOT).contains(lowerTerm)) {
                score += 0.35;
            }
            if (document.getSourceNo() != null && document.getSourceNo().toLowerCase(Locale.ROOT).contains(lowerTerm)) {
                score += 0.25;
            }
        }
        if (exactSourceNoMatch(document, keyword)) {
            score += 1.2;
        }
        if (sourceNoContainsBusinessToken(document, keyword)) {
            score += 0.6;
        }
        return Math.min(1.0, score);
    }

    /**
     * 对 Qdrant payload 执行关键词评分。
     */
    private double keywordScore(JSONObject payload, String keyword) {
        String haystack = (value(payload.getString("title")) + "\n" + value(payload.getString("sourceNo")) + "\n"
                + value(payload.getString("category")) + "\n" + value(payload.getString("summary")) + "\n"
                + value(payload.getString("content"))).toLowerCase(Locale.ROOT);
        List<String> terms = splitTerms(keyword);
        if (terms.isEmpty()) {
            return 0;
        }
        double score = 0;
        String title = value(payload.getString("title")).toLowerCase(Locale.ROOT);
        String sourceNo = value(payload.getString("sourceNo")).toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (term.isBlank()) {
                continue;
            }
            String lowerTerm = term.toLowerCase(Locale.ROOT);
            if (haystack.contains(lowerTerm)) {
                score += 0.35;
            }
            if (title.contains(lowerTerm)) {
                score += 0.35;
            }
            if (sourceNo.contains(lowerTerm)) {
                score += 0.25;
            }
        }
        if (exactSourceNoMatch(payload.getString("sourceNo"), keyword)) {
            score += 1.2;
        }
        if (sourceNoContainsBusinessToken(payload.getString("sourceNo"), keyword)) {
            score += 0.6;
        }
        return Math.min(1.0, score);
    }

    /**
     * 生成智能检索 query 变体。
     *
     * <p>实现步骤：
     * 1. 保留用户原始关键词作为第一优先查询；
     * 2. 若启用模型改写且当前配置的大模型 provider 可用，则请求模型生成同义表达；
     * 3. 始终追加规则改写，保障无 token 或模型失败时仍有基础召回。</p>
     */
    private List<String> rewriteQueries(String keyword) {
        // 规范化后的原始 query 必须保留，避免模型改写偏离用户真实意图。
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        // LinkedHashSet 同时保证去重和优先级顺序。
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        queries.add(normalized);
        if (!aiProperties.getKnowledge().isQueryRewriteModelEnabled() || !largeModelRouter.available()) {
            addRuleBasedQueries(normalized, queries);
            return normalizeQueries(queries.stream().toList());
        }
        try {
            // 模型返回必须再次解析和裁剪，防止异常文本污染查询列表。
            String answer = largeModelRouter.chat(
                    AiModelUseCase.QUERY_REWRITE,
                    queryRewriteSystemPrompt(),
                    queryRewriteUserPrompt(normalized),
                    false
            );
            parseRewrittenQueries(answer).forEach(queries::add);
        } catch (RuntimeException ex) {
            addRuleBasedQueries(normalized, queries);
        }
        addRuleBasedQueries(normalized, queries);
        return normalizeQueries(queries.stream().toList());
    }

    /**
     * 追加规则生成的检索变体。
     *
     * <p>实现步骤：
     * 1. 拆分用户关键词并保留原始业务单号；
     * 2. 按物流、库存、采购、应收应付、凭证、基础资料和审批意图追加系统字段同义词；
     * 3. 结果仍交给 normalizeQueries 去重和限量，避免一次检索生成过多 query。</p>
     */
    private void addRuleBasedQueries(String keyword, Set<String> queries) {
        List<String> terms = splitTerms(keyword);
        if (terms.size() > 1) {
            queries.add(String.join(" ", terms));
        }
        if (containsAny(keyword, "发货", "发运", "运输", "物流")) {
            queries.add(keyword + " 物流单 运输单 实际发运日期 计划发运日期 实际送达日期 承运商 运单号");
        }
        if (containsAny(keyword, "库存", "物料", "入库", "出库", "调拨")) {
            queries.add(keyword + " 库存流水 物料库存 入库数量 出库数量 调拨数量 库存数量");
        }
        if (containsAny(keyword, "采购", "供应商", "订单")) {
            queries.add(keyword + " 采购订单 供应商 物料 明细 订单日期 状态");
        }
        if (containsAny(keyword, "应收", "应付", "付款", "收款", "逾期")) {
            queries.add(keyword + " 应收应付 往来单位 金额 已收已付 未结清 到期日");
        }
        if (containsAny(keyword, "凭证", "分录", "借方", "贷方")) {
            queries.add(keyword + " 财务凭证 凭证号 摘要 借方 贷方 过账");
        }
        if (containsAny(keyword, "基础", "字典", "项目", "部门", "岗位", "所属公司", "公司", "账套", "物料", "供应商", "客户", "仓库", "币种", "汇率", "结算", "付款条件", "交货条件", "运输方式", "承运商", "区划", "单据类型", "业务类型", "取消类型")) {
            queries.add(keyword + " 基础字典 基础资料 层级路径 启用状态 字典名称 字典编码");
        }
        if (containsAny(keyword, "审批", "流程", "待办", "已办", "发起", "节点", "审批人")) {
            queries.add(keyword + " 审批中心 流程管理 流程定义 当前节点 审批人 审批意见 流程状态");
        }
    }

    /**
     * 解析模型返回的 query 改写结果。
     *
     * <p>实现步骤：
     * 1. 优先从文本中截取 JSON 数组，兼容模型前后夹杂少量说明的情况；
     * 2. JSON 解析失败时按行解析，保证 query 改写异常不会中断检索；
     * 3. 只返回非空字符串，后续再统一裁剪数量和长度。</p>
     */
    private List<String> parseRewrittenQueries(String answer) {
        if (answer == null || answer.isBlank()) {
            return List.of();
        }
        String text = answer.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            try {
                JSONArray array = JSON.parseArray(text.substring(start, end + 1));
                List<String> values = new ArrayList<>();
                for (int i = 0; i < array.size(); i++) {
                    String value = array.getString(i);
                    if (value != null && !value.isBlank()) {
                        values.add(value.trim());
                    }
                }
                return values;
            } catch (RuntimeException ignored) {
                // Fall through to line parsing.
            }
        }
        List<String> values = new ArrayList<>();
        for (String line : text.split("[\\r\\n]+")) {
            String value = line.replaceFirst("^\\s*[-*\\d.、]+\\s*", "").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 规范化检索 query 列表。
     *
     * <p>实现步骤：按顺序去重、过滤空字符串、限制单条 query 长度，并最多保留 MAX_QUERY_VARIANTS 条。</p>
     */
    private List<String> normalizeQueries(List<String> queries) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String query : queries) {
            if (query == null) {
                continue;
            }
            String value = query.trim();
            if (!value.isBlank() && value.length() <= 160) {
                normalized.add(value);
            }
            if (normalized.size() >= MAX_QUERY_VARIANTS) {
                break;
            }
        }
        return normalized.stream().toList();
    }

    /**
     * 过滤掉原始 query，只保留改写产生的补充 query。
     */
    private List<String> rewrittenOnly(String original, List<String> queries) {
        return queries.stream()
                .filter(query -> !query.equals(original))
                .toList();
    }

    /**
     * 构造 query 改写的系统提示词。
     *
     * <p>实现步骤：约束模型只输出 JSON 数组，并强调不得回答问题或编造业务数据。</p>
     */
    private String queryRewriteSystemPrompt() {
        return """
                你是企业财务、采购、物流、库存系统的检索 query 改写器。
                只输出 JSON 字符串数组，不要解释。
                目标是提高本地知识库检索召回，不回答用户问题，不编造业务数据。
                改写要求：
                1. 保留用户输入中的单号、编号、日期、物料名、供应商、客户等关键实体；
                2. 补充系统常用字段名和同义词，例如物流单、运输单、实际发运日期、采购订单、库存流水、应收应付、凭证号、审批流程、基础字典、项目、部门、岗位、物料、供应商、客户；
                3. 输出 2 到 5 个中文检索 query，每个不超过 80 字；
                4. 不要输出 Markdown，不要输出对象，只输出数组。
                """;
    }

    /**
     * 构造 query 改写的用户提示词。
     */
    private String queryRewriteUserPrompt(String keyword) {
        return "用户原始检索词：" + keyword;
    }

    /**
     * 拆分并扩展检索关键词。
     *
     * <p>实现步骤：
     * 1. 按中英文标点和空白拆词；
     * 2. 抽取业务单号、编号等连续 token；
     * 3. 根据业务关键词追加系统字段同义词，提高 H2 和 Qdrant 混合检索召回。</p>
     */
    private List<String> splitTerms(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (String item : normalized.split("[\\s,，。；;]+")) {
            if (!item.isBlank()) {
                terms.add(item.trim());
            }
        }
        terms.addAll(extractBusinessTokens(normalized));
        terms.addAll(extractNaturalChineseTerms(normalized));
        if (terms.isEmpty()) {
            terms.add(normalized);
        }
        if (normalized.length() <= 20 && !terms.contains(normalized)) {
            terms.add(normalized);
        }
        addTermIfContains(normalized, terms, "这个月", "本月", "当月");
        addTermIfContains(normalized, terms, "本月", "当月", "这个月");
        addTermIfContains(normalized, terms, "物流", "物流单", "运输", "运单", "发运", "送达", "承运商");
        addTermIfContains(normalized, terms, "运输", "物流", "物流单", "发运", "送达", "承运商");
        addTermIfContains(normalized, terms, "发运", "物流", "运输", "实际发运", "计划发运");
        addTermIfContains(normalized, terms, "送达", "物流", "运输", "已送达");
        addTermIfContains(normalized, terms, "采购", "采购单", "供应商", "采购订单");
        addTermIfContains(normalized, terms, "库存", "库存流水", "入库", "出库", "调拨", "物料");
        addTermIfContains(normalized, terms, "应收", "应收应付", "未结", "到期", "逾期", "客户");
        addTermIfContains(normalized, terms, "应付", "应收应付", "未结", "到期", "逾期", "供应商");
        addTermIfContains(normalized, terms, "出纳", "出纳流水", "收款", "付款", "银行账户", "资金流水");
        addTermIfContains(normalized, terms, "回款", "收款", "出纳流水", "客户回款", "银行账户");
        addTermIfContains(normalized, terms, "付款", "出纳流水", "供应商付款", "银行账户", "资金流水");
        addTermIfContains(normalized, terms, "账户流水", "出纳流水", "银行账户", "收款", "付款");
        addTermIfContains(normalized, terms, "凭证", "财务凭证", "过账", "借方", "贷方");
        addTermIfContains(normalized, terms, "附件", "业务附件", "文件", "上传");
        addTermIfContains(normalized, terms, "基础", "基础资料", "基础字典", "字典", "层级路径");
        addTermIfContains(normalized, terms, "字典", "基础字典", "基础资料", "字典名称", "字典编码");
        addTermIfContains(normalized, terms, "项目", "基础字典", "项目字典", "项目编码", "项目名称");
        addTermIfContains(normalized, terms, "部门", "基础字典", "部门字典", "组织部门", "部门岗位");
        addTermIfContains(normalized, terms, "岗位", "基础字典", "岗位字典", "部门岗位");
        addTermIfContains(normalized, terms, "所属公司", "基础字典", "公司", "账套", "组织");
        addTermIfContains(normalized, terms, "账套", "所属公司", "公司", "基础字典");
        addTermIfContains(normalized, terms, "物料", "基础字典", "物料字典", "规格型号", "单位");
        addTermIfContains(normalized, terms, "供应商", "基础字典", "往来单位", "采购方");
        addTermIfContains(normalized, terms, "客户", "基础字典", "往来单位", "应收");
        addTermIfContains(normalized, terms, "仓库", "基础字典", "来源仓库", "目标仓库");
        addTermIfContains(normalized, terms, "币种", "基础字典", "汇率", "人民币");
        addTermIfContains(normalized, terms, "审批", "审批中心", "流程", "待办", "已办", "发起事宜");
        addTermIfContains(normalized, terms, "流程", "审批流程", "流程定义", "流程管理", "当前节点");
        return terms;
    }

    /**
     * 提取适合 H2 数据库侧预筛选的核心词。
     *
     * <p>实现步骤：复用检索拆词结果，过滤过短、过泛的词；同时保留原始短关键词，
     * 保障“青岗”这类行政区划或业务地址关键字可直接命中。</p>
     */
    private List<String> h2CandidateTerms(String keyword) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String term : splitTerms(keyword)) {
            String normalized = value(term).trim();
            if (normalized.length() < 2 || genericAssistantMatchTerm(normalized)) {
                continue;
            }
            terms.add(normalized);
            if (terms.size() >= 8) {
                break;
            }
        }
        String original = value(keyword).trim();
        if (original.length() >= 2 && original.length() <= 20 && !genericAssistantMatchTerm(original)) {
            terms.add(original);
        }
        return terms.stream().toList();
    }

    /**
     * 从自然语言短问句中提取核心中文词。
     *
     * <p>实现步骤：
     * 1. 移除“有没有、查询、内容、记录”等问句虚词；
     * 2. 按剩余空白重新拆分候选词；
     * 3. 保留 2 到 20 个字符的中文片段，用于“有没有青岗的内容”这类问法召回业务数据。</p>
     */
    private List<String> extractNaturalChineseTerms(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String cleaned = value;
        for (String stopWord : List.of(
                "有没有", "是否", "查询", "检索", "搜索", "查看", "查找", "帮我", "帮", "请", "当前", "系统",
                "里面", "现在", "这个", "一下", "相关", "记录", "数据", "内容", "情况", "信息", "哪些", "有什么",
                "物流", "运输", "发货地", "目的地", "发货", "送达", "承运商",
                "采购", "采购单", "采购订单", "供应商", "库存", "库存流水", "物料", "仓库", "入库", "出库", "调拨",
                "应收", "应付", "应收应付", "往来单位", "付款", "收款", "核销", "凭证", "财务凭证", "分录", "科目",
                "附件", "文件", "基础", "字典", "基础资料", "业务", "订单", "流水", "单据", "编号", "单号", "单",
                "有", "没", "的", "了", "吗", "呢", "那", "啊"
        )) {
            cleaned = cleaned.replace(stopWord, " ");
        }
        List<String> terms = new ArrayList<>();
        for (String item : cleaned.split("[\\s,，。；;]+")) {
            String term = item.trim();
            if (term.length() >= 2 && term.length() <= 20 && term.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)) {
                terms.add(term);
            }
        }
        return terms;
    }

    /**
     * 抽取关键词中的业务编号 token。
     *
     * <p>实现步骤：使用 BUSINESS_TOKEN_PATTERN 识别单号、编码和包含数字的业务标识，保留顺序并去重。</p>
     */
    private Set<String> extractBusinessTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Matcher matcher = BUSINESS_TOKEN_PATTERN.matcher(value);
        Set<String> tokens = new LinkedHashSet<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /**
     * 判断检索结果是否精确匹配用户输入的业务编号。
     *
     * <p>实现步骤：只比较结果 sourceNo 和用户输入 token，命中后用于提升排序，保证单号查询优先返回原单据。</p>
     */
    private boolean exactTokenMatch(String keyword, KnowledgeSearchResult item) {
        String sourceNo = item.sourceNo();
        if (sourceNo == null || sourceNo.isBlank()) {
            return false;
        }
        return extractBusinessTokens(keyword).stream()
                .anyMatch(token -> sourceNo.equalsIgnoreCase(token));
    }

    /**
     * 判断检索结果正文是否直接包含用户问题中的核心词。
     *
     * <p>实现步骤：
     * 1. 从问题中抽取业务编号和非泛化中文词；
     * 2. 合并标题、编号、摘要和正文作为可匹配文本；
     * 3. 命中后允许 ratel助手把该结果放入候选上下文，解决自然词被向量排序挤掉的问题。</p>
     */
    private boolean directTextMatch(String keyword, KnowledgeSearchResult item) {
        if (item == null) {
            return false;
        }
        List<String> terms = directMatchTerms(keyword);
        if (terms.isEmpty()) {
            return false;
        }
        String searchableText = (value(item.sourceNo()) + "\n"
                + value(item.title()) + "\n"
                + value(item.category()) + "\n"
                + value(item.summary()) + "\n"
                + value(item.content())).toLowerCase(Locale.ROOT);
        return terms.stream()
                .map(term -> term.toLowerCase(Locale.ROOT))
                .anyMatch(searchableText::contains);
    }

    /**
     * 判断直接文本命中是否可作为助手上下文补充。
     *
     * <p>实现步骤：基础字典和会计科目仍要求显式意图；业务单据和附件才允许通过核心词命中放宽阈值。</p>
     */
    private boolean supplementalDirectContextAllowed(KnowledgeSearchResult item, String keyword) {
        if (item == null) {
            return false;
        }
        if (KnowledgeSourceType.BASIC_DICTIONARY.name().equals(item.type()) || KnowledgeSourceType.SUBJECT.name().equals(item.type())) {
            return visibleInSmartSearch(keyword, item) && directTextMatch(keyword, item);
        }
        return directTextMatch(keyword, item);
    }

    /**
     * ratel助手上下文排序规则。
     *
     * <p>实现步骤：精确业务编号优先，其次是直接文本命中的业务数据，最后再按混合检索评分排序。</p>
     */
    private Comparator<KnowledgeSearchResult> assistantContextComparator(String keyword) {
        return Comparator
                .comparingInt((KnowledgeSearchResult item) -> exactTokenMatch(keyword, item) ? 1 : 0)
                .reversed()
                .thenComparing(Comparator.comparingInt((KnowledgeSearchResult item) -> supplementalDirectContextAllowed(item, keyword) ? 1 : 0)
                        .reversed())
                .thenComparing(Comparator.comparingDouble(KnowledgeSearchResult::score).reversed());
    }

    /**
     * 抽取用于放宽助手上下文阈值的确定性匹配词。
     *
     * <p>实现步骤：保留单号类 token 和自然语言核心中文词，过滤掉“物流、内容、系统”等泛词，避免低相关结果进入 prompt。</p>
     */
    private List<String> directMatchTerms(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        extractBusinessTokens(keyword).forEach(terms::add);
        extractNaturalChineseTerms(keyword).stream()
                .filter(term -> !genericAssistantMatchTerm(term))
                .forEach(terms::add);
        return terms.stream()
                .filter(term -> term != null && !term.isBlank())
                .toList();
    }

    /**
     * 判断是否为不能单独用于上下文放宽的泛化查询词。
     */
    private boolean genericAssistantMatchTerm(String term) {
        return Set.of(
                "系统", "当前", "现在", "这个", "那个", "内容", "记录", "数据", "信息", "情况", "相关",
                "物流", "运输", "采购", "库存", "应收", "应付", "凭证", "附件", "字典", "基础", "项目",
                "部门", "岗位", "物料", "供应商", "客户", "仓库", "币种", "区划", "单据", "业务"
        ).contains(term);
    }

    /**
     * 判断 H2 文档来源编号是否精确匹配用户输入 token。
     */
    private boolean exactSourceNoMatch(KnowledgeDocument document, String keyword) {
        return exactSourceNoMatch(document.getSourceNo(), keyword);
    }

    private boolean exactSourceNoMatch(String sourceNo, String keyword) {
        if (sourceNo == null || sourceNo.isBlank()) {
            return false;
        }
        return extractBusinessTokens(keyword).stream()
                .anyMatch(token -> sourceNo.equalsIgnoreCase(token));
    }

    /**
     * 判断来源编号是否包含用户输入的业务 token。
     *
     * <p>实现步骤：在精确匹配之外提供包含匹配，用于处理用户只说单号片段的场景。</p>
     */
    private boolean sourceNoContainsBusinessToken(KnowledgeDocument document, String keyword) {
        return sourceNoContainsBusinessToken(document.getSourceNo(), keyword);
    }

    private boolean sourceNoContainsBusinessToken(String sourceNoValue, String keyword) {
        if (sourceNoValue == null || sourceNoValue.isBlank()) {
            return false;
        }
        String sourceNo = sourceNoValue.toLowerCase(Locale.ROOT);
        return extractBusinessTokens(keyword).stream()
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(sourceNo::contains);
    }

    /**
     * 命中触发词时追加同义检索词。
     *
     * <p>实现步骤：保持追加顺序并避免重复，便于关键词评分按更完整的业务词表匹配。</p>
     */
    private void addTermIfContains(String source, List<String> terms, String trigger, String... additions) {
        if (!source.contains(trigger)) {
            return;
        }
        for (String addition : additions) {
            if (!terms.contains(addition)) {
                terms.add(addition);
            }
        }
    }

    /**
     * 按用户意图给业务来源类型加权。
     *
     * <p>实现步骤：先读取文档来源类型，再根据关键词命中的业务域返回固定权重，减少跨模块噪声结果。</p>
     */
    private double sourceIntentBoost(KnowledgeDocument document, String keyword) {
        return sourceIntentBoost(document.getSourceType(), keyword);
    }

    private double sourceIntentBoost(KnowledgeSourceType sourceType, String keyword) {
        String normalized = keyword == null ? "" : keyword;
        if (sourceType == null || normalized.isBlank()) {
            return 0;
        }
        if (sourceType == KnowledgeSourceType.SHIPMENT && containsAny(normalized, "物流", "运输", "运单", "承运", "发运", "送达")) {
            return 0.35;
        }
        if (sourceType == KnowledgeSourceType.PURCHASE_ORDER && containsAny(normalized, "采购", "供应商", "采购单", "采购订单")) {
            return 0.3;
        }
        if (sourceType == KnowledgeSourceType.INVENTORY_LEDGER && containsAny(normalized, "库存", "入库", "出库", "调拨", "盘点", "物料")) {
            return 0.3;
        }
        if (sourceType == KnowledgeSourceType.AR_AP_BILL && containsAny(normalized, "应收", "应付", "往来", "到期", "逾期", "未结", "付款", "收款")) {
            return 0.3;
        }
        if (sourceType == KnowledgeSourceType.CASHIER_TRANSACTION && containsAny(normalized,
                "出纳", "收款", "付款", "回款", "支付", "转账", "退款", "银行", "现金", "账户", "流水")) {
            return 0.3;
        }
        if (sourceType == KnowledgeSourceType.VOUCHER && containsAny(normalized, "凭证", "分录", "过账", "借方", "贷方", "报表")) {
            return 0.25;
        }
        if (sourceType == KnowledgeSourceType.ATTACHMENT && containsAny(normalized, "附件", "文件", "合同", "发票", "单据")) {
            return 0.25;
        }
        if (sourceType == KnowledgeSourceType.SUBJECT && containsAny(normalized, "科目", "会计科目", "科目编码")) {
            return 0.25;
        }
        if (sourceType == KnowledgeSourceType.BASIC_DICTIONARY && basicDataIntent(normalized)) {
            return 0.35;
        }
        return 0;
    }

    /**
     * 判断文本是否包含任一候选词。
     */
    private boolean containsAny(String source, String... items) {
        for (String item : items) {
            if (source.contains(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * H2 语义检索下安全生成查询向量。
     *
     * <p>实现步骤：调用本地 Ollama embedding；失败时返回空向量，由 H2 模式继续使用关键词召回。</p>
     */
    private List<Double> safeEmbedding(String keyword) {
        try {
            return ollamaClient.embedding(keyword);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    /**
     * Qdrant 检索必须有查询向量，不能降级到 H2 或空向量。
     */
    private List<Double> requiredEmbedding(String keyword) {
        try {
            List<Double> embedding = ollamaClient.embedding(keyword);
            if (!embedding.isEmpty()) {
                return embedding;
            }
        } catch (RuntimeException ex) {
            throw qdrantEmbeddingUnavailable(ex);
        }
        throw qdrantEmbeddingUnavailable(null);
    }

    /**
     * Qdrant 混合/关键词可用时允许只使用 payload 关键词召回；纯语义模式仍要求 embedding。
     */
    private List<Double> qdrantEmbedding(String keyword, boolean allowKeywordOnly) {
        try {
            return requiredEmbedding(keyword);
        } catch (BusinessException ex) {
            if (allowKeywordOnly) {
                return List.of();
            }
            throw ex;
        }
    }

    /**
     * 解析 H2 中保存的 embedding JSON。
     *
     * <p>实现步骤：将 JSON 数组转换为 Double 列表；历史脏数据或空值解析失败时返回空向量。</p>
     */
    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            return JSON.parseArray(embeddingJson, Double.class);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    /**
     * 计算两个向量的余弦相似度。
     *
     * <p>实现步骤：维度不一致或零向量返回 0；正常结果限制为非负值，便于和关键词分数混合排序。</p>
     */
    private double cosine(List<Double> left, List<Double> right) {
        if (left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.size(); i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return Math.max(0, dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)));
    }

    /**
     * 将知识文档转换为前端检索结果。
     *
     * <p>实现步骤：保留来源类型、业务 ID、编号、标题、摘要、正文、评分和路由路径，评分统一保留 4 位小数。</p>
     */
    private KnowledgeSearchResult toResult(KnowledgeDocument document, double score) {
        return new KnowledgeSearchResult(
                document.getId(),
                document.getSourceType().name(),
                document.getSourceId(),
                document.getSourceNo(),
                document.getTitle(),
                document.getCategory(),
                document.getSummary(),
                document.getContent(),
                Math.round(score * 10000D) / 10000D,
                routePath(document)
        );
    }

    /**
     * 从知识文档元数据读取前端路由。
     */
    private String routePath(KnowledgeDocument document) {
        return routePath(document.getMetadata());
    }

    private String routePath(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(metadata).getString("route");
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 判断 Qdrant point 是否满足当前用户权限和账套。
     */
    private boolean visiblePoint(QdrantKnowledgeClient.ScoredPoint point, Set<PermissionCode> permissions, String organizationCode) {
        if (point == null || point.payload() == null) {
            return false;
        }
        JSONObject payload = point.payload();
        String permissionCode = value(payload.getString("permissionCode"));
        if (!permissionCode.isBlank()) {
            PermissionCode parsed = parsePermissionCode(permissionCode);
            if (parsed == null || permissions == null || !permissions.contains(parsed)) {
                return false;
            }
        }
        String pointOrganization = value(payload.getString("organizationCode"));
        return pointOrganization.isBlank() || pointOrganization.equals(organizationCode);
    }

    private PermissionCode parsePermissionCode(String permissionCode) {
        try {
            return permissionCode == null || permissionCode.isBlank() ? null : PermissionCode.valueOf(permissionCode);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private KnowledgeSourceType parseSourceType(String sourceType) {
        try {
            return sourceType == null || sourceType.isBlank() ? null : KnowledgeSourceType.valueOf(sourceType);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 判断结果是否适合展示在智能检索列表。
     *
     * <p>实现步骤：基础资料和会计科目只有在用户有明确意图时展示，避免“无命中推荐”把后台字典能力暴露得过多。</p>
     */
    private boolean visibleInSmartSearch(String keyword, KnowledgeSearchResult result) {
        if (result == null) {
            return false;
        }
        if (KnowledgeSourceType.SUBJECT.name().equals(result.type())) {
            return subjectIntent(keyword) || directTextMatch(keyword, result);
        }
        if (KnowledgeSourceType.BASIC_DICTIONARY.name().equals(result.type())) {
            return basicDataIntent(keyword) || directTextMatch(keyword, result);
        }
        if (containsAny(
                (value(result.title()) + "\n" + value(result.category()) + "\n" + value(result.summary())).toLowerCase(Locale.ROOT),
                "基础信息",
                "基础字典",
                "字典管理",
                "会计科目",
                "科目模块"
        )) {
            return basicDataIntent(keyword) || subjectIntent(keyword) || directTextMatch(keyword, result);
        }
        return true;
    }

    /**
     * 判断用户检索是否明确涉及会计科目。
     *
     * <p>实现步骤：通过科目、会计科目、科目编码等关键词识别意图，只有命中时才开放科目知识结果，避免普通业务检索被基础科目定义干扰。</p>
     */
    private boolean subjectIntent(String keyword) {
        return containsAny(value(keyword), "科目", "会计科目", "科目编码", "科目名称", "借方科目", "贷方科目");
    }

    /**
     * 判断用户检索是否明确涉及基础资料。
     *
     * <p>实现步骤：识别字典、项目、部门、岗位、物料、供应商、客户、仓库、币种、所属公司等基础资料关键词，命中后开放基础字典知识结果。</p>
     */
    private boolean basicDataIntent(String keyword) {
        String normalized = value(keyword);
        if (containsAny(normalized,
                "基础", "基础资料", "字典", "字典项", "编码", "层级", "层级路径", "选项", "下拉", "启用状态",
                "是否存在", "有哪些", "列表", "维护", "配置", "所属公司", "账套")) {
            return containsAny(normalized,
                    "基础", "基础资料", "字典", "项目", "部门", "岗位", "所属公司", "公司", "账套", "组织",
                    "物料", "供应商", "客户", "往来单位", "仓库", "币种", "汇率", "结算方式", "付款条件",
                    "交货条件", "运输方式", "承运商", "区划", "单据类型", "业务类型", "取消类型");
        }
        return false;
    }

    /**
     * 将可选字符串转换为空安全文本。
     */
    private String value(String value) {
        return value == null ? "" : value;
    }

    /**
     * 检查知识索引是否可用。
     * 
     * <p>实现步骤：
     * 1. 读取当前向量库的知识分片总数；
     * 2. 分片为 0 时明确中止检索，避免助手脱离业务依据回答“无需重建”；
     * 3. 当前请求不触发全量重建，后台初始化或管理员手工重建负责写入；
     * 4. Qdrant 模式下服务不可用会显式抛错，不回退到 H2 知识表。</p>
     */
    private void ensureIndexed() {
        long documentCount = knowledgeIndexService.count();
        if (documentCount <= 0) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ResponseCode.LOAD_CLIENT_ERROR,
                    "知识索引当前没有分片，系统可能正在后台初始化；请稍后重试，或由管理员执行重建索引。");
        }
    }

    /**
     * 判断当前检索模式是否需要关键词评分。
     *
     * <p>实现步骤：H2 未启用 embedding 时强制使用关键词；keyword 和 hybrid 模式也保留关键词分，保障单号和菜单名命中。</p>
     */
    private boolean shouldUseKeyword(String mode) {
        return backendRouter.useKeyword(mode);
    }

    /**
     * 判断当前检索是否需要查询向量。
     *
     * <p>实现步骤：只有 semantic/hybrid 需要查询向量；keyword 模式在 Qdrant 下直接走 payload 关键词召回。</p>
     */
    private boolean shouldUseEmbedding(String mode) {
        return backendRouter.useEmbedding(mode);
    }

    /**
     * 判断向量评分是否已启用。
     *
     * <p>实现步骤：H2 由 embedding-enabled 控制，Qdrant 模式天然需要向量评分。</p>
     */
    private boolean embeddingScoringEnabled() {
        return backendRouter.embeddingScoringEnabled();
    }

    /**
     * 判断当前向量库是否选择 Qdrant。
     */
    private boolean useQdrantVectorStore() {
        return backendRouter.useQdrant();
    }

    /**
     * 判断任一 AI 能力是否可用。
     *
     * <p>实现步骤：当前配置的大模型 provider 或本地 embedding 任一可用，即认为前端可展示 AI 能力状态。</p>
     */
    private boolean aiAvailable() {
        return largeModelRouter.available() || ollamaClient.embeddingAvailable();
    }

    /**
     * 创建 Qdrant 查询向量不可用的业务异常。
     *
     * <p>实现步骤：返回 BAD_GATEWAY 业务异常；不返回空结果或改走 H2，避免用户误以为 Qdrant 检索成功。</p>
     */
    private BusinessException qdrantEmbeddingUnavailable(Throwable cause) {
        BusinessException exception = new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                "Qdrant 模式需要本地 embedding 模型，请先启动 Ollama 并下载 " + ollamaClient.embeddingModel() + "。");
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    /**
     * 规范化检索模式。
     *
     * <p>实现步骤：仅接受 keyword、semantic、hybrid 三种模式；空值和非法值统一回退 hybrid。</p>
     */
    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "hybrid";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (List.of("keyword", "semantic", "hybrid").contains(normalized)) {
            return normalized;
        }
        return "hybrid";
    }

    /**
     * 规范化检索数量上限。
     *
     * <p>实现步骤：非法值回退 50，最大不超过 100，避免单次检索返回过多内容。</p>
     */
    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }

    /**
     * 单个检索 query 及其可选向量。
     */
    private record QueryProfile(String keyword, List<Double> embedding) {
    }

    /**
     * Qdrant point 转换后的检索结果及原始向量分。
     */
    private record ScoredPointResult(KnowledgeSearchResult result, double vectorScore) {
    }

    /**
     * H2 文档和混合评分。
     */
    private record ScoredDocument(KnowledgeDocument document, double score) {
    }
}
