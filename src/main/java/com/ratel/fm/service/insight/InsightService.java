package com.ratel.fm.service.insight;

import com.ratel.fm.domain.finance.Voucher;
import com.ratel.fm.domain.finance.VoucherStatus;
import com.ratel.fm.domain.inventory.InventoryLedger;
import com.ratel.fm.domain.inventory.InventoryMovementType;
import com.ratel.fm.domain.logistics.ShipmentOrder;
import com.ratel.fm.domain.logistics.ShipmentStatus;
import com.ratel.fm.domain.purchase.PurchaseOrder;
import com.ratel.fm.domain.purchase.PurchaseStatus;
import com.ratel.fm.domain.receivable.ArApBill;
import com.ratel.fm.domain.receivable.ArApStatus;
import com.ratel.fm.domain.receivable.ArApType;
import com.ratel.fm.repository.auth.UserAccountRepository;
import com.ratel.fm.repository.finance.AccountingSubjectRepository;
import com.ratel.fm.repository.finance.VoucherRepository;
import com.ratel.fm.repository.inventory.InventoryLedgerRepository;
import com.ratel.fm.repository.logistics.ShipmentOrderRepository;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.repository.receivable.ArApBillRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.service.knowledge.KnowledgeSearchService;
import com.ratel.fm.web.dto.insight.InsightDtos.AccountingSuggestion;
import com.ratel.fm.web.dto.insight.InsightDtos.DashboardOverview;
import com.ratel.fm.web.dto.insight.InsightDtos.MonthCloseCheck;
import com.ratel.fm.web.dto.insight.InsightDtos.RiskAlert;
import com.ratel.fm.web.dto.insight.InsightDtos.WorkbenchTodo;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeSearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Service
/**
 * InsightService 类。
 * 
 * <p>用于承载 InsightService 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class InsightService {

    /**
     * 字段 userRepository：保存 userRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserAccountRepository userRepository;
    /**
     * 字段 subjectRepository：保存 subjectRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AccountingSubjectRepository subjectRepository;
    /**
     * 字段 voucherRepository：保存 voucherRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final VoucherRepository voucherRepository;
    /**
     * 字段 purchaseOrderRepository：保存 purchaseOrderRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final PurchaseOrderRepository purchaseOrderRepository;
    /**
     * 字段 shipmentOrderRepository：保存 shipmentOrderRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ShipmentOrderRepository shipmentOrderRepository;
    /**
     * 字段 inventoryLedgerRepository：保存 inventoryLedgerRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final InventoryLedgerRepository inventoryLedgerRepository;
    /**
     * 字段 arApBillRepository：保存 arApBillRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ArApBillRepository arApBillRepository;
    /**
     * 字段 knowledgeSearchService：保存 knowledgeSearchService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final KnowledgeSearchService knowledgeSearchService;

    /**
     * 构造 InsightService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public InsightService(
            UserAccountRepository userRepository,
            AccountingSubjectRepository subjectRepository,
            VoucherRepository voucherRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            ShipmentOrderRepository shipmentOrderRepository,
            InventoryLedgerRepository inventoryLedgerRepository,
            ArApBillRepository arApBillRepository,
            KnowledgeSearchService knowledgeSearchService
    ) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.voucherRepository = voucherRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.arApBillRepository = arApBillRepository;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @Transactional(readOnly = true)
    /**
     * 执行 overview 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public DashboardOverview overview() {
        String organizationCode = CompanyScope.currentCompanyCode();
        List<Voucher> vouchers = voucherRepository.findByOrganizationCodeAndVoucherDateBetweenOrderByVoucherDateDesc(
                organizationCode, LocalDate.now().minusYears(20), LocalDate.now().plusYears(1));
        // 变量说明：purchaseOrders 保存当前步骤计算、查询或转换得到的中间结果。
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll(CompanyScope.<PurchaseOrder>currentCompanySpec());
        // 变量说明：shipmentOrders 保存当前步骤计算、查询或转换得到的中间结果。
        List<ShipmentOrder> shipmentOrders = shipmentOrderRepository.findAll(CompanyScope.<ShipmentOrder>currentCompanySpec());
        // 变量说明：inventoryLedgers 保存当前步骤计算、查询或转换得到的中间结果。
        List<InventoryLedger> inventoryLedgers = inventoryLedgerRepository.findAll(CompanyScope.<InventoryLedger>currentCompanySpec());
        // 变量说明：arApBills 保存当前步骤计算、查询或转换得到的中间结果。
        List<ArApBill> arApBills = arApBillRepository.findAll(CompanyScope.<ArApBill>currentCompanySpec());

        BigDecimal postedDebitTotal = vouchers.stream()
                .filter(voucher -> voucher.getStatus() == VoucherStatus.POSTED)
                .map(voucher -> voucher.getTotalDebitCny() == null ? safeMoney(voucher.getTotalDebit()) : voucher.getTotalDebitCny())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal purchaseTotal = purchaseOrders.stream()
                .map(order -> order.getTotalAmountCny() == null ? safeMoney(order.getTotalAmount()) : order.getTotalAmountCny())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 变量说明：draftVoucherCount 保存当前步骤计算、查询或转换得到的中间结果。
        long draftVoucherCount = vouchers.stream().filter(voucher -> voucher.getStatus() == VoucherStatus.DRAFT).count();
        // 变量说明：pendingPurchaseCount 保存当前步骤计算、查询或转换得到的中间结果。
        long pendingPurchaseCount = purchaseOrders.stream().filter(this::isPendingPurchase).count();
        // 变量说明：inTransitShipmentCount 保存当前步骤计算、查询或转换得到的中间结果。
        long inTransitShipmentCount = shipmentOrders.stream().filter(this::isActiveShipment).count();
        // 变量说明：overdueArApCount 保存当前步骤计算、查询或转换得到的中间结果。
        long overdueArApCount = arApBills.stream().filter(this::isOverdueArAp).count();

        // 变量说明：receivableOpenAmount 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal receivableOpenAmount = openAmount(arApBills, bill -> bill.getBillType() == ArApType.RECEIVABLE);
        // 变量说明：payableOpenAmount 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal payableOpenAmount = openAmount(arApBills, bill -> bill.getBillType() == ArApType.PAYABLE);
        // 变量说明：stockAmountMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<String, MaterialStockAmount> stockAmountMap = materialStockAmountMap(inventoryLedgers);
        // 变量说明：accountingSuggestions 保存当前步骤计算、查询或转换得到的中间结果。
        List<AccountingSuggestion> accountingSuggestions = accountingSuggestions(purchaseOrders, inventoryLedgers, arApBills, vouchers);
        // 变量说明：risks 保存当前步骤计算、查询或转换得到的中间结果。
        List<RiskAlert> risks = riskAlerts(vouchers, shipmentOrders, arApBills, stockAmountMap);

        return new DashboardOverview(
                userRepository.count(CompanyScope.<com.ratel.fm.domain.auth.UserAccount>currentCompanySpec()),
                subjectRepository.count(CompanyScope.<com.ratel.fm.domain.finance.AccountingSubject>currentCompanySpec()),
                vouchers.size(),
                purchaseOrders.size(),
                shipmentOrders.size(),
                draftVoucherCount,
                pendingPurchaseCount,
                inTransitShipmentCount,
                overdueArApCount,
                postedDebitTotal,
                purchaseTotal,
                receivableOpenAmount,
                payableOpenAmount,
                workbenchTodos(draftVoucherCount, pendingPurchaseCount, inTransitShipmentCount, overdueArApCount, stockAmountMap),
                risks,
                accountingSuggestions,
                monthCloseChecks(draftVoucherCount, overdueArApCount, stockAmountMap, accountingSuggestions, vouchers)
        );
    }

    @Transactional
    /**
     * 执行 search 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public KnowledgeSearchResponse search(String keyword, String mode, int limit) {
        return knowledgeSearchService.search(keyword, mode, limit);
    }

    /**
     * 空金额按 0 处理，用于兼容升级前未保存人民币金额快照的历史数据。
     */
    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 执行 openAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BigDecimal openAmount(List<ArApBill> arApBills, Predicate<ArApBill> typeFilter) {
        return arApBills.stream()
                .filter(typeFilter)
                .filter(bill -> bill.getStatus() != ArApStatus.CLOSED)
                .map(bill -> safeMoney(bill.getAmountCny()).subtract(safeMoney(bill.getPaidAmountCny())))
                .map(amount -> amount.max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 执行 isPendingPurchase 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean isPendingPurchase(PurchaseOrder order) {
        return order.getStatus() == PurchaseStatus.DRAFT
                || order.getStatus() == PurchaseStatus.IN_APPROVAL
                || order.getStatus() == PurchaseStatus.APPROVAL_REJECTED
                || order.getStatus() == PurchaseStatus.SUBMITTED
                || order.getStatus() == PurchaseStatus.APPROVED
                || order.getStatus() == PurchaseStatus.PURCHASING;
    }

    /**
     * 执行 isActiveShipment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean isActiveShipment(ShipmentOrder order) {
        return order.getStatus() == ShipmentStatus.CREATED
                || order.getStatus() == ShipmentStatus.DISPATCHED
                || order.getStatus() == ShipmentStatus.IN_TRANSIT;
    }

    /**
     * 执行 isOverdueArAp 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean isOverdueArAp(ArApBill bill) {
        return bill.getStatus() == ArApStatus.OVERDUE
                || (bill.getStatus() != ArApStatus.CLOSED
                && bill.getDueDate() != null
                && bill.getDueDate().isBefore(LocalDate.now()));
    }

    /**
     * 执行 workbenchTodos 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<WorkbenchTodo> workbenchTodos(
            long draftVoucherCount,
            long pendingPurchaseCount,
            long inTransitShipmentCount,
            long overdueArApCount,
            Map<String, MaterialStockAmount> stockAmountMap
    ) {
        // 变量说明：todos 保存当前步骤计算、查询或转换得到的中间结果。
        List<WorkbenchTodo> todos = new ArrayList<>();
        if (draftVoucherCount > 0) {
            todos.add(new WorkbenchTodo("VOUCHER_POST", "待过账凭证", "有 " + draftVoucherCount + " 张草稿凭证需要复核并过账。", "warning",
                    "/vouchers", "status", VoucherStatus.DRAFT.name()));
        }
        if (pendingPurchaseCount > 0) {
            todos.add(new WorkbenchTodo("PURCHASE_FOLLOW", "采购履约跟进", "有 " + pendingPurchaseCount + " 张采购单仍处于草稿、已提交或已审核状态。", "primary",
                    "/purchase-orders", null, null));
        }
        if (inTransitShipmentCount > 0) {
            todos.add(new WorkbenchTodo("SHIPMENT_FOLLOW", "物流在途跟进", "有 " + inTransitShipmentCount + " 张物流单尚未送达。", "primary",
                    "/shipments", null, null));
        }
        if (overdueArApCount > 0) {
            todos.add(new WorkbenchTodo("AR_AP_OVERDUE", "逾期应收应付处理", "有 " + overdueArApCount + " 张应收应付单已逾期或标记逾期。", "danger",
                    "/ar-ap", "status", ArApStatus.OVERDUE.name()));
        }
        // 变量说明：negativeStockCount 保存当前步骤计算、查询或转换得到的中间结果。
        long negativeStockCount = stockAmountMap.values().stream().filter(MaterialStockAmount::negative).count();
        if (negativeStockCount > 0) {
            todos.add(new WorkbenchTodo("INVENTORY_NEGATIVE", "负库存复核", "有 " + negativeStockCount + " 个物料库存数量小于 0，请检查入库、出库和调拨流水。", "danger",
                    "/inventory", null, null));
        }
        return todos;
    }

    /**
     * 执行 riskAlerts 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<RiskAlert> riskAlerts(
            List<Voucher> vouchers,
            List<ShipmentOrder> shipmentOrders,
            List<ArApBill> arApBills,
            Map<String, MaterialStockAmount> stockAmountMap
    ) {
        // 变量说明：risks 保存当前步骤计算、查询或转换得到的中间结果。
        List<RiskAlert> risks = new ArrayList<>();
        vouchers.stream()
                .filter(voucher -> safeMoney(voucher.getTotalDebit()).compareTo(safeMoney(voucher.getTotalCredit())) != 0)
                .limit(3)
                .forEach(voucher -> risks.add(new RiskAlert("danger", "凭证借贷不平衡",
                        voucher.getVoucherNo() + " 借方合计与贷方合计不一致，请复核分录。", "/vouchers", "voucherNo", voucher.getVoucherNo())));

        shipmentOrders.stream()
                .filter(this::isActiveShipment)
                .filter(order -> order.getPlannedShipDate() != null && order.getPlannedShipDate().isBefore(LocalDate.now()))
                .limit(3)
                .forEach(order -> risks.add(new RiskAlert("warning", "物流计划发运已超期",
                        order.getShipmentNo() + " 计划发运日期为 " + order.getPlannedShipDate() + "，当前仍未完成。", "/shipments", "shipmentNo", order.getShipmentNo())));

        arApBills.stream()
                .filter(this::isOverdueArAp)
                .limit(3)
                .forEach(bill -> risks.add(new RiskAlert("warning", "往来款项逾期",
                        bill.getBillNo() + " 到期日为 " + bill.getDueDate() + "，未结金额 " + openAmountText(bill) + "。", "/ar-ap", "billNo", bill.getBillNo())));

        stockAmountMap.entrySet().stream()
                .filter(entry -> entry.getValue().negative())
                .limit(3)
                .forEach(entry -> risks.add(new RiskAlert("danger", "物料库存为负",
                        entry.getKey() + " 当前库存数量 " + entry.getValue().stockQuantity().toPlainString() + "，请复核库存流水。", "/inventory", "itemName", entry.getKey())));

        if (risks.isEmpty()) {
            risks.add(new RiskAlert("success", "暂无高优先级风险", "当前未发现借贷不平、负库存、物流超期或逾期往来款。", "/dashboard", null, null));
        }
        return risks;
    }

    /**
     * 执行 accountingSuggestions 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<AccountingSuggestion> accountingSuggestions(
            List<PurchaseOrder> purchaseOrders,
            List<InventoryLedger> inventoryLedgers,
            List<ArApBill> arApBills,
            List<Voucher> vouchers
    ) {
        // 变量说明：suggestions 保存当前步骤计算、查询或转换得到的中间结果。
        List<AccountingSuggestion> suggestions = new ArrayList<>();

        purchaseOrders.stream()
                .filter(order -> order.getStatus() == PurchaseStatus.RECEIVED
                        || order.getStatus() == PurchaseStatus.PURCHASE_COMPLETED
                        || order.getStatus() == PurchaseStatus.CLOSED)
                .filter(order -> !hasVoucherForSource(order.getOrderNo(), vouchers))
                .limit(4)
                .forEach(order -> suggestions.add(new AccountingSuggestion("PURCHASE_ORDER", order.getOrderNo(), "采购入库应付凭证",
                        "采购单已收货或关闭，但未找到来源业务单号对应凭证。", moneyOf(order), "库存商品/材料采购", "应付账款",
                        "/purchase-orders", "orderNo", order.getOrderNo())));

        inventoryLedgers.stream()
                .filter(ledger -> ledger.getMovementType() != InventoryMovementType.TRANSFER)
                .filter(ledger -> !hasVoucherForSource(ledger.getMovementNo(), vouchers))
                .limit(4)
                .forEach(ledger -> suggestions.add(inventorySuggestion(ledger)));

        arApBills.stream()
                .filter(bill -> bill.getStatus() != ArApStatus.CLOSED)
                .filter(bill -> !hasVoucherForSource(bill.getBillNo(), vouchers))
                .limit(4)
                .forEach(bill -> suggestions.add(arApSuggestion(bill)));

        return suggestions.stream().limit(10).toList();
    }

    /**
     * 执行 inventorySuggestion 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private AccountingSuggestion inventorySuggestion(InventoryLedger ledger) {
        String title;
        String debitSubject;
        String creditSubject;
        if (ledger.getMovementType() == InventoryMovementType.INBOUND) {
            title = "库存入库暂估凭证";
            debitSubject = "库存商品";
            creditSubject = "暂估应付/材料采购";
        } else if (ledger.getMovementType() == InventoryMovementType.OUTBOUND) {
            title = "库存出库成本结转";
            debitSubject = "主营业务成本";
            creditSubject = "库存商品";
        } else {
            title = "库存盘点差异处理";
            debitSubject = "待处理财产损溢";
            creditSubject = "库存商品";
        }
        return new AccountingSuggestion("INVENTORY_LEDGER", ledger.getMovementNo(), title,
                "库存流水尚未找到来源业务单号对应凭证，可作为月末核算候选事项。",
                safeMoney(ledger.getQuantity()), debitSubject, creditSubject, "/inventory", "movementNo", ledger.getMovementNo());
    }

    /**
     * 执行 arApSuggestion 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private AccountingSuggestion arApSuggestion(ArApBill bill) {
        // 变量说明：receivable 保存当前步骤计算、查询或转换得到的中间结果。
        boolean receivable = bill.getBillType() == ArApType.RECEIVABLE;
        return new AccountingSuggestion("AR_AP_BILL", bill.getBillNo(), receivable ? "应收确认凭证" : "应付确认凭证",
                "应收应付单未结清，且未找到来源业务单号对应凭证。",
                openAmount(bill), receivable ? "应收账款" : "管理费用/采购成本", receivable ? "主营业务收入" : "应付账款",
                "/ar-ap", "billNo", bill.getBillNo());
    }

    /**
     * 执行 monthCloseChecks 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private List<MonthCloseCheck> monthCloseChecks(
            long draftVoucherCount,
            long overdueArApCount,
            Map<String, MaterialStockAmount> stockAmountMap,
            List<AccountingSuggestion> accountingSuggestions,
            List<Voucher> vouchers
    ) {
        // 变量说明：checks 保存当前步骤计算、查询或转换得到的中间结果。
        List<MonthCloseCheck> checks = new ArrayList<>();
        checks.add(new MonthCloseCheck("VOUCHER_POSTED", "凭证过账检查", draftVoucherCount == 0 ? "success" : "warning",
                draftVoucherCount == 0 ? "当前没有草稿凭证。" : "仍有 " + draftVoucherCount + " 张草稿凭证未过账。", "/vouchers"));

        BigDecimal debit = vouchers.stream()
                .filter(voucher -> voucher.getStatus() == VoucherStatus.POSTED)
                .map(voucher -> voucher.getTotalDebitCny() == null ? safeMoney(voucher.getTotalDebit()) : voucher.getTotalDebitCny())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = vouchers.stream()
                .filter(voucher -> voucher.getStatus() == VoucherStatus.POSTED)
                .map(voucher -> voucher.getTotalCreditCny() == null ? safeMoney(voucher.getTotalCredit()) : voucher.getTotalCreditCny())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        checks.add(new MonthCloseCheck("TRIAL_BALANCE", "试算平衡检查", debit.compareTo(credit) == 0 ? "success" : "danger",
                debit.compareTo(credit) == 0 ? "已过账凭证借贷合计一致。" : "已过账凭证借方与贷方发生额不一致。", "/reports"));

        // 变量说明：negativeStockCount 保存当前步骤计算、查询或转换得到的中间结果。
        long negativeStockCount = stockAmountMap.values().stream().filter(MaterialStockAmount::negative).count();
        checks.add(new MonthCloseCheck("INVENTORY_STOCK", "库存余额检查", negativeStockCount == 0 ? "success" : "danger",
                negativeStockCount == 0 ? "未发现物料库存为负。" : "有 " + negativeStockCount + " 个物料库存为负。", "/inventory"));

        checks.add(new MonthCloseCheck("AR_AP_AGING", "往来账龄检查", overdueArApCount == 0 ? "success" : "warning",
                overdueArApCount == 0 ? "未发现逾期应收应付。" : "有 " + overdueArApCount + " 张应收应付单逾期。", "/ar-ap"));

        checks.add(new MonthCloseCheck("ACCOUNTING_SUGGESTION", "智能核算检查", accountingSuggestions.isEmpty() ? "success" : "warning",
                accountingSuggestions.isEmpty() ? "未发现需要补充凭证的业务事项。" : "有 " + accountingSuggestions.size() + " 条业务事项建议生成或复核凭证。", "/vouchers"));
        return checks;
    }

    /**
     * 执行 materialStockAmountMap 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Map<String, MaterialStockAmount> materialStockAmountMap(List<InventoryLedger> inventoryLedgers) {
        // 变量说明：amountMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<String, MaterialStockAmount> amountMap = new LinkedHashMap<>();
        for (InventoryLedger ledger : inventoryLedgers) {
            // 变量说明：itemName 保存当前步骤计算、查询或转换得到的中间结果。
            String itemName = blankToNull(ledger.getItemName());
            if (itemName == null || ledger.getMovementType() == null) {
                continue;
            }
            amountMap.computeIfAbsent(itemName, ignored -> new MaterialStockAmount()).add(ledger.getMovementType(), safeMoney(ledger.getQuantity()));
        }
        return amountMap;
    }

    /**
     * 执行 hasVoucherForSource 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean hasVoucherForSource(String sourceNo, List<Voucher> vouchers) {
        // 变量说明：normalizedSourceNo 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedSourceNo = blankToNull(sourceNo);
        if (normalizedSourceNo == null) {
            return false;
        }
        return vouchers.stream()
                .map(Voucher::getSourceBizNo)
                .map(this::blankToNull)
                .anyMatch(sourceBizNo -> sourceBizNo != null && sourceBizNo.contains(normalizedSourceNo));
    }

    /**
     * 执行 moneyOf 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BigDecimal moneyOf(PurchaseOrder order) {
        return order.getTotalAmountCny() == null ? safeMoney(order.getTotalAmount()) : order.getTotalAmountCny();
    }

    /**
     * 执行 openAmount 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BigDecimal openAmount(ArApBill bill) {
        return safeMoney(bill.getAmountCny()).subtract(safeMoney(bill.getPaidAmountCny())).max(BigDecimal.ZERO);
    }

    /**
     * 执行 openAmountText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String openAmountText(ArApBill bill) {
        return openAmount(bill).stripTrailingZeros().toPlainString();
    }

    /**
     * 执行 blankToNull 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * MaterialStockAmount 类。
     * 
     * <p>用于承载 MaterialStockAmount 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    private static final class MaterialStockAmount {

        /**
         * 字段 inboundQuantity：保存 inboundQuantity 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
         */
        private BigDecimal inboundQuantity = BigDecimal.ZERO;
        /**
         * 字段 outboundQuantity：保存 outboundQuantity 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
         */
        private BigDecimal outboundQuantity = BigDecimal.ZERO;
        /**
         * 字段 transferQuantity：保存 transferQuantity 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
         */
        private BigDecimal transferQuantity = BigDecimal.ZERO;

        /**
         * 执行 add 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private void add(InventoryMovementType movementType, BigDecimal quantity) {
            switch (movementType) {
                case INBOUND -> inboundQuantity = inboundQuantity.add(quantity);
                case OUTBOUND -> outboundQuantity = outboundQuantity.add(quantity);
                case TRANSFER -> transferQuantity = transferQuantity.add(quantity);
                case CHECK -> {
                }
            }
        }

        /**
         * 执行 stockQuantity 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private BigDecimal stockQuantity() {
            return inboundQuantity.subtract(outboundQuantity).subtract(transferQuantity);
        }

        /**
         * 执行 negative 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private boolean negative() {
            return stockQuantity().compareTo(BigDecimal.ZERO) < 0;
        }
    }
}
