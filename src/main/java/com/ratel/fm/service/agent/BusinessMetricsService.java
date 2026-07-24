package com.ratel.fm.service.agent;

import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.service.inventory.InventoryService;
import com.ratel.fm.service.operation.OperationService;
import com.ratel.fm.service.receivable.ArApService;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.ArApView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryMaterialStockView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 经营指标快照服务。
 *
 * <p>把经营分析需要的采购规模、往来未结、库存结构等指标先结构化，再交给 Agent 或大模型归纳，
 * 避免在 Agent 内部临时拼接统计口径。</p>
 */
@Service
public class BusinessMetricsService {

    private final OperationService operationService;
    private final ArApService arApService;
    private final InventoryService inventoryService;

    public BusinessMetricsService(OperationService operationService, ArApService arApService, InventoryService inventoryService) {
        this.operationService = operationService;
        this.arApService = arApService;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public BusinessMetricsSnapshot snapshot(String businessNo, String keyword, Set<PermissionCode> permissions) {
        List<PurchaseOrderView> purchases = permissions.contains(PermissionCode.REPORT_VIEW) || permissions.contains(PermissionCode.PURCHASE_MANAGE)
                ? operationService.listPurchaseOrders(null, null, businessNo, keyword, null, null, null, null)
                : List.of();
        List<ArApView> arAps = permissions.contains(PermissionCode.REPORT_VIEW) || permissions.contains(PermissionCode.AR_AP_MANAGE)
                ? arApService.list(null, null, businessNo, null, keyword, null, null, null)
                : List.of();
        List<InventoryMaterialStockView> stocks = permissions.contains(PermissionCode.REPORT_VIEW) || permissions.contains(PermissionCode.INVENTORY_MANAGE)
                ? flattenStocks(inventoryService.materialStock())
                : List.of();
        BigDecimal purchaseTotal = purchases.stream().map(PurchaseOrderView::totalAmountCny).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingTotal = arAps.stream().map(ArApView::remainingAmountCny).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long materialKinds = stocks.stream().filter(item -> amount(item.stockQuantity()).compareTo(BigDecimal.ZERO) != 0).count();
        long negativeStockKinds = stocks.stream().filter(item -> amount(item.stockQuantity()).compareTo(BigDecimal.ZERO) < 0).count();
        return new BusinessMetricsSnapshot(purchases, arAps, stocks, purchaseTotal, remainingTotal, materialKinds, negativeStockKinds);
    }

    private List<InventoryMaterialStockView> flattenStocks(List<InventoryMaterialStockView> rows) {
        List<InventoryMaterialStockView> result = new ArrayList<>();
        for (InventoryMaterialStockView row : rows) {
            result.add(row);
            result.addAll(flattenStocks(row.children()));
        }
        return result;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record BusinessMetricsSnapshot(
            List<PurchaseOrderView> purchases,
            List<ArApView> arAps,
            List<InventoryMaterialStockView> stocks,
            BigDecimal purchaseTotal,
            BigDecimal remainingTotal,
            long materialKinds,
            long negativeStockKinds
    ) {
    }
}
