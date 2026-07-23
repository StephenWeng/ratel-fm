package com.ratel.fm.web.dto.insight;

import java.math.BigDecimal;
import java.util.List;

/**
 * InsightDtos 类。
 * 
 * <p>用于承载 InsightDtos 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public final class InsightDtos {

    private InsightDtos() {
    }

    /**
     * DashboardOverview 数据传输记录。
     * 
     * <p>用于承载 DashboardOverview 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record DashboardOverview(
            /**
             * 记录组件 userCount：表示接口入参或出参中的 userCount 字段。
             */
            long userCount,
            /**
             * 记录组件 subjectCount：表示接口入参或出参中的 subjectCount 字段。
             */
            long subjectCount,
            /**
             * 记录组件 voucherCount：表示接口入参或出参中的 voucherCount 字段。
             */
            long voucherCount,
            /**
             * 记录组件 purchaseOrderCount：表示接口入参或出参中的 purchaseOrderCount 字段。
             */
            long purchaseOrderCount,
            /**
             * 记录组件 shipmentOrderCount：表示接口入参或出参中的 shipmentOrderCount 字段。
             */
            long shipmentOrderCount,
            /**
             * 记录组件 draftVoucherCount：表示接口入参或出参中的 draftVoucherCount 字段。
             */
            long draftVoucherCount,
            /**
             * 记录组件 pendingPurchaseCount：表示接口入参或出参中的 pendingPurchaseCount 字段。
             */
            long pendingPurchaseCount,
            /**
             * 记录组件 inTransitShipmentCount：表示接口入参或出参中的 inTransitShipmentCount 字段。
             */
            long inTransitShipmentCount,
            /**
             * 记录组件 overdueArApCount：表示接口入参或出参中的 overdueArApCount 字段。
             */
            long overdueArApCount,
            /**
             * 记录组件 postedDebitTotal：表示接口入参或出参中的 postedDebitTotal 字段。
             */
            BigDecimal postedDebitTotal,
            /**
             * 记录组件 purchaseTotal：表示接口入参或出参中的 purchaseTotal 字段。
             */
            BigDecimal purchaseTotal,
            /**
             * 记录组件 receivableOpenAmount：表示接口入参或出参中的 receivableOpenAmount 字段。
             */
            BigDecimal receivableOpenAmount,
            /**
             * 记录组件 payableOpenAmount：表示接口入参或出参中的 payableOpenAmount 字段。
             */
            BigDecimal payableOpenAmount,
            /**
             * 记录组件 todos：表示接口入参或出参中的 todos 字段。
             */
            List<WorkbenchTodo> todos,
            /**
             * 记录组件 risks：表示接口入参或出参中的 risks 字段。
             */
            List<RiskAlert> risks,
            /**
             * 记录组件 accountingSuggestions：表示接口入参或出参中的 accountingSuggestions 字段。
             */
            List<AccountingSuggestion> accountingSuggestions,
            /**
             * 记录组件 monthCloseChecks：表示接口入参或出参中的 monthCloseChecks 字段。
             */
            List<MonthCloseCheck> monthCloseChecks
    ) {
    }

    /**
     * WorkbenchTodo 数据传输记录。
     * 
     * <p>用于承载 WorkbenchTodo 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record WorkbenchTodo(
            /**
             * 记录组件 type：表示接口入参或出参中的 type 字段。
             */
            String type,
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description,
            /**
             * 记录组件 severity：表示接口入参或出参中的 severity 字段。
             */
            String severity,
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath,
            /**
             * 记录组件 searchKey：表示接口入参或出参中的 searchKey 字段。
             */
            String searchKey,
            /**
             * 记录组件 searchValue：表示接口入参或出参中的 searchValue 字段。
             */
            String searchValue
    ) {
    }

    /**
     * RiskAlert 数据传输记录。
     * 
     * <p>用于承载 RiskAlert 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record RiskAlert(
            /**
             * 记录组件 level：表示接口入参或出参中的 level 字段。
             */
            String level,
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description,
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath,
            /**
             * 记录组件 searchKey：表示接口入参或出参中的 searchKey 字段。
             */
            String searchKey,
            /**
             * 记录组件 searchValue：表示接口入参或出参中的 searchValue 字段。
             */
            String searchValue
    ) {
    }

    /**
     * AccountingSuggestion 数据传输记录。
     * 
     * <p>用于承载 AccountingSuggestion 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record AccountingSuggestion(
            /**
             * 记录组件 sourceType：表示接口入参或出参中的 sourceType 字段。
             */
            String sourceType,
            /**
             * 记录组件 sourceNo：表示接口入参或出参中的 sourceNo 字段。
             */
            String sourceNo,
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 reason：表示接口入参或出参中的 reason 字段。
             */
            String reason,
            /**
             * 记录组件 amount：表示接口入参或出参中的 amount 字段。
             */
            BigDecimal amount,
            /**
             * 记录组件 debitSubject：表示接口入参或出参中的 debitSubject 字段。
             */
            String debitSubject,
            /**
             * 记录组件 creditSubject：表示接口入参或出参中的 creditSubject 字段。
             */
            String creditSubject,
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath,
            /**
             * 记录组件 searchKey：表示接口入参或出参中的 searchKey 字段。
             */
            String searchKey,
            /**
             * 记录组件 searchValue：表示接口入参或出参中的 searchValue 字段。
             */
            String searchValue
    ) {
    }

    /**
     * MonthCloseCheck 数据传输记录。
     * 
     * <p>用于承载 MonthCloseCheck 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record MonthCloseCheck(
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            String status,
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description,
            /**
             * 记录组件 routePath：表示接口入参或出参中的 routePath 字段。
             */
            String routePath
    ) {
    }

    /**
     * SearchResult 数据传输记录。
     * 
     * <p>用于承载 SearchResult 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record SearchResult(
            /**
             * 记录组件 type：表示接口入参或出参中的 type 字段。
             */
            String type,
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            /**
             * 记录组件 title：表示接口入参或出参中的 title 字段。
             */
            String title,
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description
    ) {
    }

    /**
     * SearchResponse 数据传输记录。
     * 
     * <p>用于承载 SearchResponse 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record SearchResponse(
            /**
             * 记录组件 keyword：表示接口入参或出参中的 keyword 字段。
             */
            String keyword,
            /**
             * 记录组件 results：表示接口入参或出参中的 results 字段。
             */
            List<SearchResult> results
    ) {
    }
}
