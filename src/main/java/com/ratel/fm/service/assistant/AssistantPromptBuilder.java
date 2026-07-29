package com.ratel.fm.service.assistant;

import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResult;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.WebSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ratel 助手 Prompt 构造器。
 *
 * <p>集中维护系统提示词、用户提示词和上下文压缩规则，降低业务编排服务的 prompt 维护成本。</p>
 */
@Component
public class AssistantPromptBuilder {

    private static final int MAX_LOCAL_CONTEXTS_FOR_MODEL = 5;
    private static final int MAX_WEB_CONTEXTS_FOR_MODEL = 3;
    private static final int MAX_LOCAL_CONTENT_CHARS = 900;
    private static final int MAX_WEB_SUMMARY_CHARS = 600;
    private static final int MAX_SYSTEM_CONTEXT_CHARS = 5000;

    public String systemPrompt() {
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
                回答使用中文，结构清晰，先给结论，再列依据。结论或关键依据有多条时，使用“1、2、3”编号，不使用短横线作为行首。
                不得输出内部思考过程、推理草稿、复盘语句、<think> 标签或“重新读一下用户的问题”这类过程性文字。
                """;
    }

    public String userPrompt(
            String question,
            String modeLabel,
            String conversationContext,
            String systemContext,
            List<KnowledgeSearchResult> contexts,
            List<WebSearchResult> webResults
    ) {
        String contextText = compactLocalContexts(contexts).stream()
                .map(item -> """
                        [来源ID:%s][类型:%s][单号:%s][标题:%s][相关度:%.4f]
                        摘要: %s
                        关键内容: %s
                        """.formatted(item.id(), item.category(), item.sourceNo(), item.title(), item.score(), item.summary(), item.content()))
                .collect(Collectors.joining("\n---\n"));
        String webContextText = compactWebResults(webResults).stream()
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
                7. 结论或关键依据有多条时，使用“1、2、3”编号，不使用短横线作为行首；
                8. 涉及经营统计、财务报表、往来账款、资金现金流、采购库存、对账、制证、账龄、逾期、利润、毛利等财务专业问题时，优先基于“实时系统上下文”做统计、归纳和风险判断，不要改为知识库文件问答；
                9. 不要输出内部思考过程、推理草稿、复盘语句、<think> 标签或“重新读一下用户的问题”这类过程性文字；
                10. 不要使用常识或猜测补齐系统内缺失字段，也不要把未在上下文中出现的网页内容当作依据。
                11. 实时系统上下文或本地知识上下文已给出非零记录时，不得回答“没有数据”“暂无记录”或“无法分析”；只能说明具体缺失的字段，并先回答已有的确定性事实。
                """.formatted(question, modeLabel, blank(conversationContext), blank(compactSystemContext(systemContext)), blank(contextText), blank(webContextText));
    }

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

    private String compactSystemContext(String systemContext) {
        return truncate(value(systemContext), MAX_SYSTEM_CONTEXT_CHARS);
    }

    private String preferContent(KnowledgeSearchResult item) {
        String content = value(item.content());
        return content.isBlank() ? value(item.summary()) : content;
    }

    private String blank(String text) {
        return value(text).isBlank() ? "无" : text;
    }

    private String truncate(String value, int maxChars) {
        String text = value(value).trim();
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars)) + "...";
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
