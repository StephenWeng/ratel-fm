package com.ratel.fm.service.knowledge;

import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResult;

import java.time.YearMonth;
import java.util.Locale;

/** Applies deterministic business-field filters after semantic recall. */
final class KnowledgeResultIntentFilter {

    private KnowledgeResultIntentFilter() {
    }

    static boolean hasStructuredIntent(String query) {
        String text = value(query);
        return text.contains("非草稿") || text.contains("草稿") || text.contains("采购中")
                || text.contains("审批中") || text.contains("审批同意") || text.contains("审批不同意")
                || text.contains("已完成") || text.contains("已收货") || text.contains("已取消")
                || text.contains("本月") || text.contains("默认项目");
    }

    static boolean matches(String query, KnowledgeSearchResult result) {
        return matches(query, result, YearMonth.now());
    }

    static boolean matches(String query, KnowledgeSearchResult result, YearMonth currentMonth) {
        if (result == null || !KnowledgeSourceType.PURCHASE_ORDER.name().equals(result.type())) {
            return true;
        }
        String text = value(query);
        String content = value(result.content()).toUpperCase(Locale.ROOT);
        if (text.contains("非草稿") && hasStatus(content, "DRAFT")) {
            return false;
        }
        if (!text.contains("非草稿") && text.contains("草稿") && !hasStatus(content, "DRAFT")) {
            return false;
        }
        if (text.contains("采购中") && !hasStatus(content, "PURCHASING")) {
            return false;
        }
        if (text.contains("审批中") && !hasStatus(content, "IN_APPROVAL")) {
            return false;
        }
        if (text.contains("审批不同意") && !hasStatus(content, "APPROVAL_REJECTED")) {
            return false;
        }
        if (text.contains("审批同意") && !hasStatus(content, "APPROVED")) {
            return false;
        }
        if ((text.contains("已完成") || text.contains("已收货"))
                && !(hasStatus(content, "PURCHASE_COMPLETED") || hasStatus(content, "RECEIVED") || hasStatus(content, "CLOSED"))) {
            return false;
        }
        if (text.contains("已取消") && !hasStatus(content, "CANCELLED")) {
            return false;
        }
        if (text.contains("本月") && !content.contains("采购日期: " + currentMonth + "-")) {
            return false;
        }
        return !text.contains("默认项目") || content.contains("项目: 默认项目(");
    }

    private static boolean hasStatus(String content, String status) {
        return content.contains("状态: " + status + "\n") || content.endsWith("状态: " + status);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
