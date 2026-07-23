package com.ratel.fm.service.knowledge;

import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeResultIntentFilterTest {

    private static final String QUERY = "默认项目在本月的非草稿状态的采购单";

    @Test
    void nonDraftMonthlyProjectQueryShouldExcludeDraftPurchaseOrder() {
        assertThat(KnowledgeResultIntentFilter.matches(QUERY, purchase("DRAFT", "2026-07-14", "默认项目"), YearMonth.of(2026, 7)))
                .isFalse();
    }

    @Test
    void nonDraftMonthlyProjectQueryShouldKeepPurchasingOrder() {
        assertThat(KnowledgeResultIntentFilter.matches(QUERY, purchase("PURCHASING", "2026-07-14", "默认项目"), YearMonth.of(2026, 7)))
                .isTrue();
    }

    @Test
    void monthlyAndProjectConstraintsShouldBeHardFilters() {
        assertThat(KnowledgeResultIntentFilter.matches(QUERY, purchase("PURCHASING", "2026-06-30", "默认项目"), YearMonth.of(2026, 7)))
                .isFalse();
        assertThat(KnowledgeResultIntentFilter.matches(QUERY, purchase("PURCHASING", "2026-07-14", "其它项目"), YearMonth.of(2026, 7)))
                .isFalse();
    }

    private KnowledgeSearchResult purchase(String status, String date, String project) {
        return new KnowledgeSearchResult(
                1L,
                "PURCHASE_ORDER",
                1L,
                "PO202607140001",
                "采购单 PO202607140001",
                "采购单",
                "采购单",
                "项目: " + project + "(PROJECT_DEFAULT)\n采购日期: " + date + "\n状态: " + status + "\n",
                0.9,
                "/purchase-orders"
        );
    }
}
