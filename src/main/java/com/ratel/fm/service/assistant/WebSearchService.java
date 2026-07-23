package com.ratel.fm.service.assistant;

import com.ratel.fm.common.concurrent.NamedDaemonThreadFactory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.WebSearchResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ratel助手互联网检索服务。
 */
@Service
public class WebSearchService {

    /**
     * DuckDuckGo HTML 结果提取正则，用于在没有 Tavily/Bing 配置时解析兜底搜索结果。
     */
    private static final Pattern DUCK_RESULT_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>.*?<a[^>]+class=\"result__snippet\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    /**
     * HTML 标签清理正则，用于把搜索标题、摘要和页面正文转成纯文本。
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    /**
     * 脚本、样式和不可读标签清理正则，避免网页噪声进入 AI 上下文。
     */
    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile(
            "<(script|style|noscript|svg|canvas|iframe)[^>]*>.*?</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    /**
     * 网页主体提取正则，优先截取 body 内容以提高互联网检索摘要质量。
     */
    private static final Pattern BODY_PATTERN = Pattern.compile("<body[^>]*>(.*?)</body>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /**
     * 常量 MAX_PAGE_TEXT_CHARS：保存 MAX_PAGE_TEXT_CHARS 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final int MAX_PAGE_TEXT_CHARS = 12_000;
    /**
     * 常量 MAX_SNIPPET_CHARS：保存 MAX_SNIPPET_CHARS 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final int MAX_SNIPPET_CHARS = 900;

    /**
     * 字段 aiProperties：保存 aiProperties 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AiProperties aiProperties;
    /**
     * 字段 httpClient：保存 httpClient 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final HttpClient httpClient;
    /**
     * 字段 executor：互联网检索 HTTP 客户端专用线程池，避免默认线程池在外部接口超时后增长不可控。
     */
    private final ExecutorService executor;
    /**
     * 字段 searchSemaphore：互联网检索并发闸门，防止多次搜索同时抓取网页造成堆内存上涨。
     */
    private final Semaphore searchSemaphore;

    /**
     * 构造 WebSearchService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public WebSearchService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.executor = Executors.newFixedThreadPool(
                Math.max(1, aiProperties.getWebSearch().getExecutorThreads()),
                new NamedDaemonThreadFactory("web-search-http-")
        );
        this.searchSemaphore = new Semaphore(Math.max(1, aiProperties.getWebSearch().getMaxConcurrentRequests()));
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(Math.max(3, aiProperties.getWebSearch().getRequestTimeoutSeconds())))
                .executor(executor)
                .build();
    }

    /**
     * 执行 enabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean enabled() {
        return aiProperties.getWebSearch().isEnabled();
    }

    /**
     * 执行 search 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public List<WebSearchResult> search(String query) {
        if (!enabled() || query == null || query.isBlank()) {
            return List.of();
        }
        boolean acquired = false;
        try {
            acquired = searchSemaphore.tryAcquire(Math.max(1, aiProperties.getWebSearch().getRequestTimeoutSeconds()), TimeUnit.SECONDS);
            if (!acquired) {
                return List.of();
            }
            List<WebSearchResult> results;
            if (useTavily()) {
                results = searchTavily(query);
            } else if (useBing()) {
                results = searchBing(query);
            } else {
                results = searchDuckDuckGo(query);
            }
            if (results.isEmpty() && !useDuckDuckGo()) {
                results = searchDuckDuckGo(query);
            }
            return enrichWithPageContent(query, results);
        } catch (RuntimeException ex) {
            return List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        } finally {
            if (acquired) {
                searchSemaphore.release();
            }
        }
    }

    /**
     * 执行 useTavily 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean useTavily() {
        return "tavily".equalsIgnoreCase(aiProperties.getWebSearch().getProvider())
                && aiProperties.getWebSearch().getTavilyApiKey() != null
                && !aiProperties.getWebSearch().getTavilyApiKey().isBlank();
    }

    /**
     * 执行 useBing 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean useBing() {
        return "bing".equalsIgnoreCase(aiProperties.getWebSearch().getProvider())
                && aiProperties.getWebSearch().getBingApiKey() != null
                && !aiProperties.getWebSearch().getBingApiKey().isBlank();
    }

    /**
     * 执行 useDuckDuckGo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean useDuckDuckGo() {
        return "duckduckgo".equalsIgnoreCase(aiProperties.getWebSearch().getProvider());
    }

    /**
     * 执行 searchTavily 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<WebSearchResult> searchTavily(String query) {
        // 变量说明：endpoint 保存当前步骤计算、查询或转换得到的中间结果。
        String endpoint = aiProperties.getWebSearch().getTavilyEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://api.tavily.com/search";
        }
        // 变量说明：payload 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject payload = new JSONObject();
        payload.put("query", query);
        payload.put("topic", "general");
        payload.put("search_depth", aiProperties.getWebSearch().isIncludeRawContent() ? "advanced" : "basic");
        payload.put("chunks_per_source", aiProperties.getWebSearch().isIncludeRawContent() ? 2 : 1);
        payload.put("max_results", maxResults());
        payload.put("include_answer", false);
        payload.put("include_raw_content", aiProperties.getWebSearch().isIncludeRawContent());
        payload.put("include_images", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout())
                .header("Authorization", "Bearer " + aiProperties.getWebSearch().getTavilyApiKey().trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString(), StandardCharsets.UTF_8))
                .build();
        // 变量说明：body 保存当前步骤计算、查询或转换得到的中间结果。
        String body = send(request);
        if (body.isBlank()) {
            return List.of();
        }
        // 变量说明：json 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject json = JSON.parseObject(body);
        // 变量说明：values 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray values = json.getJSONArray("results");
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        // 变量说明：results 保存当前步骤计算、查询或转换得到的中间结果。
        List<WebSearchResult> results = new ArrayList<>();
        for (int i = 0; i < values.size() && results.size() < maxResults(); i++) {
            // 变量说明：item 保存当前步骤计算、查询或转换得到的中间结果。
            JSONObject item = values.getJSONObject(i);
            // 变量说明：title 保存当前步骤计算、查询或转换得到的中间结果。
            String title = clean(item.getString("title"));
            // 变量说明：url 保存当前步骤计算、查询或转换得到的中间结果。
            String url = cleanUrl(item.getString("url"));
            // 变量说明：content 保存当前步骤计算、查询或转换得到的中间结果。
            String content = clean(item.getString("content"));
            // 变量说明：rawContent 保存当前步骤计算、查询或转换得到的中间结果。
            String rawContent = aiProperties.getWebSearch().isIncludeRawContent()
                    ? clean(item.getString("raw_content"))
                    : "";
            // 变量说明：summary 保存当前步骤计算、查询或转换得到的中间结果。
            String summary = mergeSummary(content, selectRelevantSnippet(query, rawContent));
            if (!title.isBlank() && !url.isBlank()) {
                results.add(new WebSearchResult(
                        title,
                        url,
                        summary,
                        "Tavily",
                        normalizeScore(item.getDoubleValue("score"), results.size())
                ));
            }
        }
        return deduplicate(results);
    }

    /**
     * 执行 searchBing 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<WebSearchResult> searchBing(String query) {
        // 变量说明：endpoint 保存当前步骤计算、查询或转换得到的中间结果。
        String endpoint = aiProperties.getWebSearch().getBingEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://api.bing.microsoft.com/v7.0/search";
        }
        // 变量说明：url 保存当前步骤计算、查询或转换得到的中间结果。
        String url = endpoint + "?q=" + encode(query) + "&mkt=zh-CN&count=" + maxResults();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout())
                .header("Ocp-Apim-Subscription-Key", aiProperties.getWebSearch().getBingApiKey().trim())
                .header("Accept", "application/json")
                .GET()
                .build();
        // 变量说明：body 保存当前步骤计算、查询或转换得到的中间结果。
        String body = send(request);
        // 变量说明：json 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject json = JSON.parseObject(body);
        // 变量说明：webPages 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject webPages = json.getJSONObject("webPages");
        // 变量说明：values 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray values = webPages == null ? null : webPages.getJSONArray("value");
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        // 变量说明：results 保存当前步骤计算、查询或转换得到的中间结果。
        List<WebSearchResult> results = new ArrayList<>();
        for (int i = 0; i < values.size() && results.size() < maxResults(); i++) {
            // 变量说明：item 保存当前步骤计算、查询或转换得到的中间结果。
            JSONObject item = values.getJSONObject(i);
            results.add(new WebSearchResult(
                    clean(item.getString("name")),
                    cleanUrl(item.getString("url")),
                    clean(item.getString("snippet")),
                    "Bing",
                    score(results.size())
            ));
        }
        return deduplicate(results);
    }

    /**
     * 执行 enrichWithPageContent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<WebSearchResult> enrichWithPageContent(String query, List<WebSearchResult> results) {
        if (results.isEmpty()) {
            return results;
        }
        // 变量说明：enriched 保存当前步骤计算、查询或转换得到的中间结果。
        List<WebSearchResult> enriched = new ArrayList<>();
        // 变量说明：fetched 保存当前步骤计算、查询或转换得到的中间结果。
        int fetched = 0;
        for (WebSearchResult result : results) {
            if (fetched >= maxFetchPages() || !isFetchableHttpUrl(result.url())) {
                enriched.add(result);
                continue;
            }
            // 变量说明：pageText 保存当前步骤计算、查询或转换得到的中间结果。
            String pageText = fetchPageText(result.url());
            fetched++;
            // 变量说明：snippet 保存当前步骤计算、查询或转换得到的中间结果。
            String snippet = selectRelevantSnippet(query, pageText);
            if (snippet.isBlank()) {
                enriched.add(result);
                continue;
            }
            enriched.add(new WebSearchResult(
                    result.title(),
                    result.url(),
                    mergeSummary(result.summary(), snippet),
                    result.source() + "+正文",
                    Math.min(1.0, result.score() + 0.05)
            ));
        }
        return deduplicate(enriched);
    }

    /**
     * 执行 fetchPageText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String fetchPageText(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout())
                    .header("User-Agent", "Mozilla/5.0 RatelFM/1.0")
                    .header("Accept", "text/html,application/xhtml+xml,text/plain")
                    .GET()
                    .build();
            // 变量说明：response 保存当前步骤计算、查询或转换得到的中间结果。
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            // 变量说明：contentType 保存当前步骤计算、查询或转换得到的中间结果。
            String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
            if (!contentType.isBlank()
                    && !contentType.contains("text/html")
                    && !contentType.contains("text/plain")
                    && !contentType.contains("application/xhtml")) {
                return "";
            }
            try (InputStream body = response.body()) {
                return htmlToText(readLimited(body, maxPageBytes()));
            }
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * 执行 htmlToText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // 变量说明：source 保存当前步骤计算、查询或转换得到的中间结果。
        String source = html.length() > 500_000 ? html.substring(0, 500_000) : html;
        // 变量说明：bodyMatcher 保存当前步骤计算、查询或转换得到的中间结果。
        Matcher bodyMatcher = BODY_PATTERN.matcher(source);
        if (bodyMatcher.find()) {
            source = bodyMatcher.group(1);
        }
        source = SCRIPT_STYLE_PATTERN.matcher(source).replaceAll(" ");
        source = source
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>|</div>|</li>|</h[1-6]>", "\n");
        String text = clean(source)
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n")
                .trim();
        if (text.length() > MAX_PAGE_TEXT_CHARS) {
            return text.substring(0, MAX_PAGE_TEXT_CHARS);
        }
        return text;
    }

    /**
     * 执行 selectRelevantSnippet 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String selectRelevantSnippet(String query, String pageText) {
        if (pageText == null || pageText.isBlank()) {
            return "";
        }
        // 变量说明：paragraphs 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> paragraphs = splitParagraphs(pageText);
        if (paragraphs.isEmpty()) {
            return pageText.length() > MAX_SNIPPET_CHARS ? pageText.substring(0, MAX_SNIPPET_CHARS) : pageText;
        }
        // 变量说明：terms 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> terms = queryTerms(query);
        List<ScoredParagraph> scored = paragraphs.stream()
                .map(paragraph -> new ScoredParagraph(paragraph, paragraphScore(paragraph, terms)))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredParagraph::score).reversed()
                        .thenComparingInt(item -> item.text().length()))
                .limit(3)
                .toList();
        List<String> selected = scored.isEmpty()
                ? paragraphs.stream().limit(2).toList()
                : scored.stream().map(ScoredParagraph::text).toList();
        // 变量说明：snippet 保存当前步骤计算、查询或转换得到的中间结果。
        String snippet = String.join("\n", selected).trim();
        if (snippet.length() > MAX_SNIPPET_CHARS) {
            return snippet.substring(0, MAX_SNIPPET_CHARS).trim();
        }
        return snippet;
    }

    /**
     * 执行 splitParagraphs 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<String> splitParagraphs(String text) {
        // 变量说明：paragraphs 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> paragraphs = new ArrayList<>();
        for (String item : text.split("[\\n。！？!?]+")) {
            // 变量说明：paragraph 保存当前步骤计算、查询或转换得到的中间结果。
            String paragraph = item.replaceAll("\\s+", " ").trim();
            if (paragraph.length() >= 20) {
                paragraphs.add(paragraph);
            }
            if (paragraphs.size() >= 120) {
                break;
            }
        }
        return paragraphs;
    }

    /**
     * 执行 queryTerms 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Set<String> queryTerms(String query) {
        // 变量说明：terms 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> terms = new LinkedHashSet<>();
        if (query == null || query.isBlank()) {
            return terms;
        }
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = query.toLowerCase(Locale.ROOT);
        for (String item : normalized.split("[\\s,，。；;:：?？!！/\\\\|()（）\\[\\]{}]+")) {
            if (item.length() >= 2) {
                terms.add(item);
            }
        }
        for (int i = 0; i + 2 <= normalized.length(); i++) {
            // 变量说明：token 保存当前步骤计算、查询或转换得到的中间结果。
            String token = normalized.substring(i, Math.min(i + 4, normalized.length())).trim();
            if (token.length() >= 2 && token.codePoints().anyMatch(code -> Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN)) {
                terms.add(token);
            }
        }
        return terms;
    }

    /**
     * 执行 paragraphScore 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int paragraphScore(String paragraph, Set<String> terms) {
        if (terms.isEmpty()) {
            return 1;
        }
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = paragraph.toLowerCase(Locale.ROOT);
        // 变量说明：score 保存当前步骤计算、查询或转换得到的中间结果。
        int score = 0;
        for (String term : terms) {
            if (normalized.contains(term)) {
                score += Math.min(8, term.length());
            }
        }
        return score;
    }

    /**
     * 执行 mergeSummary 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String mergeSummary(String searchSummary, String pageSnippet) {
        // 变量说明：summary 保存当前步骤计算、查询或转换得到的中间结果。
        String summary = searchSummary == null ? "" : searchSummary.trim();
        // 变量说明：snippet 保存当前步骤计算、查询或转换得到的中间结果。
        String snippet = pageSnippet == null ? "" : pageSnippet.trim();
        if (summary.isBlank()) {
            return snippet;
        }
        if (snippet.isBlank() || summary.contains(snippet)) {
            return summary;
        }
        return summary + "\n网页正文片段：" + snippet;
    }

    /**
     * 执行 normalizeScore 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private double normalizeScore(double value, int index) {
        if (value > 0 && value <= 1) {
            return Math.max(0.1, Math.min(1.0, value));
        }
        return score(index);
    }

    /**
     * 执行 isFetchableHttpUrl 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean isFetchableHttpUrl(String value) {
        try {
            // 变量说明：uri 保存当前步骤计算、查询或转换得到的中间结果。
            URI uri = URI.create(value == null ? "" : value.trim());
            // 变量说明：scheme 保存当前步骤计算、查询或转换得到的中间结果。
            String scheme = uri.getScheme();
            // 变量说明：host 保存当前步骤计算、查询或转换得到的中间结果。
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            if (!List.of("http", "https").contains(scheme.toLowerCase(Locale.ROOT))) {
                return false;
            }
            // 变量说明：lower 保存当前步骤计算、查询或转换得到的中间结果。
            String lower = uri.toString().toLowerCase(Locale.ROOT);
            return !lower.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|7z|jpg|jpeg|png|gif|mp4|mp3)(\\?.*)?$");
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 执行 searchDuckDuckGo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<WebSearchResult> searchDuckDuckGo(String query) {
        // 变量说明：url 保存当前步骤计算、查询或转换得到的中间结果。
        String url = "https://html.duckduckgo.com/html/?q=" + encode(query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout())
                .header("User-Agent", "Mozilla/5.0 RatelFM/1.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();
        // 变量说明：body 保存当前步骤计算、查询或转换得到的中间结果。
        String body = send(request);
        // 变量说明：matcher 保存当前步骤计算、查询或转换得到的中间结果。
        Matcher matcher = DUCK_RESULT_PATTERN.matcher(body);
        // 变量说明：results 保存当前步骤计算、查询或转换得到的中间结果。
        List<WebSearchResult> results = new ArrayList<>();
        while (matcher.find() && results.size() < maxResults()) {
            // 变量说明：resultUrl 保存当前步骤计算、查询或转换得到的中间结果。
            String resultUrl = cleanDuckDuckGoUrl(matcher.group(1));
            // 变量说明：title 保存当前步骤计算、查询或转换得到的中间结果。
            String title = clean(matcher.group(2));
            // 变量说明：summary 保存当前步骤计算、查询或转换得到的中间结果。
            String summary = clean(matcher.group(3));
            if (!title.isBlank() && !resultUrl.isBlank()) {
                results.add(new WebSearchResult(title, resultUrl, summary, "DuckDuckGo", score(results.size())));
            }
        }
        return deduplicate(results);
    }

    /**
     * 执行 send 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String send(HttpRequest request) {
        try {
            // 变量说明：response 保存当前步骤计算、查询或转换得到的中间结果。
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            try (InputStream body = response.body()) {
                return readLimited(body, maxResponseBytes());
            }
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * 按最大字节数读取 HTTP 响应体。
     *
     * <p>实现步骤：
     * 1. 循环读取 InputStream 到固定大小缓冲区；
     * 2. 一旦超过上限立即返回空字符串，拒绝继续解析异常大响应；
     * 3. 未超过上限时按 UTF-8 转成文本供搜索结果解析。</p>
     */
    private String readLimited(InputStream inputStream, int maxBytes) throws IOException {
        if (inputStream == null) {
            return "";
        }
        int limit = Math.max(8192, maxBytes);
        byte[] buffer = new byte[8192];
        byte[] bytes = new byte[0];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            total += read;
            if (total > limit) {
                return "";
            }
            byte[] merged = new byte[total];
            System.arraycopy(bytes, 0, merged, 0, bytes.length);
            System.arraycopy(buffer, 0, merged, bytes.length, read);
            bytes = merged;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 执行 deduplicate 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<WebSearchResult> deduplicate(List<WebSearchResult> results) {
        // 变量说明：unique 保存当前步骤计算、查询或转换得到的中间结果。
        Map<String, WebSearchResult> unique = new LinkedHashMap<>();
        for (WebSearchResult result : results) {
            if (result.url() != null && !result.url().isBlank()) {
                unique.putIfAbsent(result.url(), result);
            }
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * 执行 cleanDuckDuckGoUrl 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String cleanDuckDuckGoUrl(String value) {
        // 变量说明：cleaned 保存当前步骤计算、查询或转换得到的中间结果。
        String cleaned = unescapeHtml(value == null ? "" : value);
        // 变量说明：targetIndex 保存当前步骤计算、查询或转换得到的中间结果。
        int targetIndex = cleaned.indexOf("uddg=");
        if (targetIndex >= 0) {
            // 变量说明：target 保存当前步骤计算、查询或转换得到的中间结果。
            String target = cleaned.substring(targetIndex + 5);
            // 变量说明：end 保存当前步骤计算、查询或转换得到的中间结果。
            int end = target.indexOf('&');
            if (end >= 0) {
                target = target.substring(0, end);
            }
            cleaned = java.net.URLDecoder.decode(target, StandardCharsets.UTF_8);
        }
        return cleanUrl(cleaned);
    }

    /**
     * 执行 cleanUrl 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String cleanUrl(String value) {
        // 变量说明：cleaned 保存当前步骤计算、查询或转换得到的中间结果。
        String cleaned = unescapeHtml(value == null ? "" : value).trim();
        if (cleaned.startsWith("//")) {
            cleaned = "https:" + cleaned;
        }
        return cleaned;
    }

    /**
     * 执行 clean 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        // 变量说明：withoutTags 保存当前步骤计算、查询或转换得到的中间结果。
        String withoutTags = HTML_TAG_PATTERN.matcher(value).replaceAll(" ");
        return unescapeHtml(withoutTags)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 执行 unescapeHtml 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String unescapeHtml(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");
    }

    /**
     * 执行 encode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * 执行 timeout 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Duration timeout() {
        return Duration.ofSeconds(Math.max(3, aiProperties.getWebSearch().getRequestTimeoutSeconds()));
    }

    /**
     * 执行 maxResults 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int maxResults() {
        return Math.max(1, Math.min(aiProperties.getWebSearch().getMaxResults(), 10));
    }

    /**
     * 获取搜索接口响应体最大字节数。
     *
     * <p>实现步骤：读取配置并设置最小值，避免配置过小导致正常搜索 JSON 被截断。</p>
     */
    private int maxResponseBytes() {
        return Math.max(64 * 1024, aiProperties.getWebSearch().getMaxResponseBytes());
    }

    /**
     * 获取网页正文抓取最大字节数。
     *
     * <p>实现步骤：读取配置并设置最小值，控制单页 HTML 进入内存的规模。</p>
     */
    private int maxPageBytes() {
        return Math.max(32 * 1024, aiProperties.getWebSearch().getMaxPageBytes());
    }

    /**
     * 获取单次检索最多正文补抓页数。
     *
     * <p>实现步骤：读取配置并限制在 0 到 5 页之间，避免互联网搜索一次抓取过多网页。</p>
     */
    private int maxFetchPages() {
        return Math.max(0, Math.min(aiProperties.getWebSearch().getMaxFetchPages(), 5));
    }

    /**
     * 执行 score 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private double score(int index) {
        return Math.max(0.1, 1.0 - (index * 0.08));
    }

    /**
     * ScoredParagraph 数据传输记录。
     * 
     * <p>用于承载 ScoredParagraph 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    private record ScoredParagraph(String text, int score) {
    }
}
