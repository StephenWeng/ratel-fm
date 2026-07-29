package com.ratel.fm.service.assistant;

import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.auth.Role;
import com.ratel.fm.domain.auth.SystemMenu;
import com.ratel.fm.domain.auth.UserAccount;
import com.ratel.fm.domain.audit.UserOperationLog;
import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.domain.finance.AccountingSubject;
import com.ratel.fm.domain.finance.Voucher;
import com.ratel.fm.domain.inventory.InventoryLedger;
import com.ratel.fm.domain.logistics.ShipmentOrder;
import com.ratel.fm.domain.purchase.PurchaseOrder;
import com.ratel.fm.domain.receivable.ArApBill;
import com.ratel.fm.repository.attachment.AttachmentFileRepository;
import com.ratel.fm.repository.attachment.BusinessAttachmentRepository;
import com.ratel.fm.repository.audit.UserOperationLogRepository;
import com.ratel.fm.repository.auth.RoleRepository;
import com.ratel.fm.repository.auth.SystemMenuRepository;
import com.ratel.fm.repository.auth.UserAccountRepository;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.repository.finance.AccountingSubjectRepository;
import com.ratel.fm.repository.finance.VoucherRepository;
import com.ratel.fm.repository.inventory.InventoryLedgerRepository;
import com.ratel.fm.repository.logistics.ShipmentOrderRepository;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.repository.receivable.ArApBillRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ratel助手实时系统上下文。
 */
@Service
public class SystemContextService {

    /**
     * 业务编号识别正则，用于从用户问题中提取采购单、物流单、凭证号等候选编号。
     */
    private static final Pattern BUSINESS_TOKEN_PATTERN = Pattern.compile("(?i)[a-z]{1,8}\\d{4,}|\\d{4,}");

    /**
     * 字段 userAccountRepository：保存 userAccountRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserAccountRepository userAccountRepository;
    /**
     * 字段 roleRepository：保存 roleRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final RoleRepository roleRepository;
    /**
     * 字段 systemMenuRepository：保存 systemMenuRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final SystemMenuRepository systemMenuRepository;
    /**
     * 字段 basicDictionaryRepository：保存 basicDictionaryRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BasicDictionaryRepository basicDictionaryRepository;
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
     * 字段 attachmentFileRepository：保存 attachmentFileRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AttachmentFileRepository attachmentFileRepository;
    /**
     * 字段 businessAttachmentRepository：保存 businessAttachmentRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BusinessAttachmentRepository businessAttachmentRepository;
    /**
     * 字段 operationLogRepository：保存 operationLogRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final UserOperationLogRepository operationLogRepository;

    /**
     * 构造 SystemContextService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public SystemContextService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            SystemMenuRepository systemMenuRepository,
            BasicDictionaryRepository basicDictionaryRepository,
            AccountingSubjectRepository subjectRepository,
            VoucherRepository voucherRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            ShipmentOrderRepository shipmentOrderRepository,
            InventoryLedgerRepository inventoryLedgerRepository,
            ArApBillRepository arApBillRepository,
            AttachmentFileRepository attachmentFileRepository,
            BusinessAttachmentRepository businessAttachmentRepository,
            UserOperationLogRepository operationLogRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.systemMenuRepository = systemMenuRepository;
        this.basicDictionaryRepository = basicDictionaryRepository;
        this.subjectRepository = subjectRepository;
        this.voucherRepository = voucherRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.arApBillRepository = arApBillRepository;
        this.attachmentFileRepository = attachmentFileRepository;
        this.businessAttachmentRepository = businessAttachmentRepository;
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional(readOnly = true)
    /**
     * 执行 buildContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String buildContext() {
        return buildContext(null);
    }

    @Transactional(readOnly = true)
    /**
     * 执行 buildContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String buildContext(String question) {
        // 变量说明：user 保存当前步骤计算、查询或转换得到的中间结果。
        CurrentUser user = SecurityUtils.currentUser();
        // 变量说明：permissions 保存当前步骤计算、查询或转换得到的中间结果。
        Set<PermissionCode> permissions = user.permissions() == null ? Set.of() : user.permissions();
        // 变量说明：menuCodes 保存当前登录人拥有的启用菜单编码，用于裁剪 ratel助手兜底上下文。
        Set<String> menuCodes = authorizedMenuCodes(user);
        // 变量说明：today 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate today = LocalDate.now();
        // 变量说明：monthStart 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate monthStart = YearMonth.from(today).atDay(1);
        // 变量说明：monthEnd 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate monthEnd = YearMonth.from(today).atEndOfMonth();
        // 变量说明：now 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime now = OffsetDateTime.now();
        // 变量说明：monthStartTime 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime monthStartTime = monthStart.atStartOfDay().atOffset(now.getOffset());
        // 变量说明：monthEndTime 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime monthEndTime = monthEnd.plusDays(1).atStartOfDay().atOffset(now.getOffset()).minusNanos(1);

        // 变量说明：sections 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> sections = new ArrayList<>();
        List<String> focusedModules = FinancialIntentTerms.selectedModules(question);
        boolean focusedBusinessQuestion = !focusedModules.isEmpty();
        sections.add(lines(
                "当前系统实时概览",
                "当前日期: " + today,
                "本月范围: " + monthStart + " 至 " + monthEnd,
                "当前用户: " + value(user.realName()) + "(" + value(user.username()) + ")",
                "当前所属公司: " + value(CompanyScope.currentCompanyCode())
        ));
        if (!focusedBusinessQuestion && canViewSystemContext(permissions, menuCodes)) {
            sections.add(systemManageContext(permissions, menuCodes));
        }
        if (!focusedBusinessQuestion && permissions.contains(PermissionCode.BASIC_DICT_MANAGE) && hasMenu(menuCodes, "PAGE_BASIC_DICTIONARIES")) {
            sections.add(basicContext());
        }
        if (focusedBusinessQuestion) {
            for (String module : focusedModules) {
                switch (module) {
                    case "finance" -> {
                        if (canViewFinanceContext(permissions, menuCodes)) sections.add(financeContext(permissions, menuCodes, monthStart, monthEnd, question));
                    }
                    case "arAp" -> {
                        if (permissions.contains(PermissionCode.AR_AP_MANAGE) && hasAnyMenu(menuCodes, "PAGE_AR_AP", "PAGE_AR_AP_STATS")) sections.add(arApContext(monthStart, monthEnd, today, question));
                    }
                    case "inventory" -> {
                        if (permissions.contains(PermissionCode.INVENTORY_MANAGE) && hasMenu(menuCodes, "PAGE_INVENTORY")) sections.add(inventoryContext(monthStart, monthEnd, question));
                    }
                    case "purchase" -> {
                        if (permissions.contains(PermissionCode.PURCHASE_MANAGE) && hasMenu(menuCodes, "PAGE_PURCHASE")) sections.add(purchaseContext(monthStart, monthEnd, question));
                    }
                    case "shipment" -> {
                        if (permissions.contains(PermissionCode.LOGISTICS_MANAGE) && hasMenu(menuCodes, "PAGE_SHIPMENTS")) sections.add(shipmentContext(monthStart, monthEnd, question));
                    }
                    default -> { }
                }
            }
        } else {
            if (canViewFinanceContext(permissions, menuCodes)) sections.add(financeContext(permissions, menuCodes, monthStart, monthEnd, question));
            if (permissions.contains(PermissionCode.PURCHASE_MANAGE) && hasMenu(menuCodes, "PAGE_PURCHASE")) sections.add(purchaseContext(monthStart, monthEnd, question));
            if (permissions.contains(PermissionCode.LOGISTICS_MANAGE) && hasMenu(menuCodes, "PAGE_SHIPMENTS")) sections.add(shipmentContext(monthStart, monthEnd, question));
            if (permissions.contains(PermissionCode.INVENTORY_MANAGE) && hasMenu(menuCodes, "PAGE_INVENTORY")) sections.add(inventoryContext(monthStart, monthEnd, question));
            if (permissions.contains(PermissionCode.AR_AP_MANAGE) && hasAnyMenu(menuCodes, "PAGE_AR_AP", "PAGE_AR_AP_STATS")) sections.add(arApContext(monthStart, monthEnd, today, question));
        }
        if (!focusedBusinessQuestion && canViewAttachmentContext(permissions, menuCodes)) {
            sections.add(attachmentContext());
        }
        if (!focusedBusinessQuestion && permissions.contains(PermissionCode.AUDIT_LOG_VIEW) && hasMenu(menuCodes, "PAGE_OPERATION_LOGS")) {
            sections.add(auditContext(monthStartTime, monthEndTime));
        }
        return sections.stream()
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 执行 systemManageContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String systemManageContext(Set<PermissionCode> permissions, Set<String> menuCodes) {
        // 变量说明：lines 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> lines = new ArrayList<>();
        lines.add("系统管理上下文");
        if (permissions.contains(PermissionCode.SYSTEM_USER_MANAGE) && hasMenu(menuCodes, "PAGE_USERS")) {
            // 变量说明：users 保存当前步骤计算、查询或转换得到的中间结果。
            List<UserAccount> users = userAccountRepository.findAll(CompanyScope.<UserAccount>currentCompanySpec());
            // 变量说明：enabledUsers 保存当前步骤计算、查询或转换得到的中间结果。
            long enabledUsers = users.stream().filter(UserAccount::isEnabled).count();
            lines.add("人员总数: " + users.size() + "，启用人员: " + enabledUsers + "，停用人员: " + (users.size() - enabledUsers));
        }
        if (permissions.contains(PermissionCode.SYSTEM_ROLE_MANAGE) && hasMenu(menuCodes, "PAGE_ROLES")) {
            // 变量说明：roles 保存当前步骤计算、查询或转换得到的中间结果。
            List<Role> roles = roleRepository.findAll();
            lines.add("角色总数: " + roles.size());
            lines.add("主要角色: " + joinTop(roles, Role::getName, 8));
        }
        if (permissions.contains(PermissionCode.SYSTEM_ROLE_MANAGE) && hasMenu(menuCodes, "PAGE_MENUS")) {
            // 变量说明：menus 保存当前用户已授权的启用菜单资源，避免展示无权访问的菜单入口。
            List<SystemMenu> menus = systemMenuRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                    .filter(SystemMenu::isEnabled)
                    .filter(menu -> menuCodes.contains(menu.getCode()))
                    .toList();
            lines.add("已授权菜单资源数: " + menus.size());
            lines.add("已授权主要菜单: " + joinTop(menus, SystemMenu::getName, 10));
        }
        return lines.size() <= 1 ? "" : String.join("\n", lines);
    }

    /**
     * 执行 basicContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String basicContext() {
        // 变量说明：dictionaries 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionary> dictionaries = basicDictionaryRepository.findAllByOrderBySortOrderAscIdAsc();
        // 变量说明：enabled 保存当前步骤计算、查询或转换得到的中间结果。
        long enabled = dictionaries.stream().filter(BasicDictionary::isEnabled).count();
        Map<String, Long> rootGroups = dictionaries.stream()
                .filter(item -> item.getParent() == null)
                .collect(Collectors.toMap(BasicDictionary::getName, parent -> dictionaries.stream()
                        .filter(item -> item.getParent() != null && Objects.equals(item.getParent().getId(), parent.getId()))
                        .count(), (left, right) -> left));
        return lines(
                "基础信息上下文",
                "字典总数: " + dictionaries.size() + "，启用字典: " + enabled + "，停用字典: " + (dictionaries.size() - enabled),
                "一级字典及子项数量: " + mapText(rootGroups)
        );
    }

    /**
     * 执行 financeContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String financeContext(Set<PermissionCode> permissions, Set<String> menuCodes, LocalDate monthStart, LocalDate monthEnd, String question) {
        // 变量说明：lines 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> lines = new ArrayList<>();
        lines.add("财务管理上下文");
        if (permissions.contains(PermissionCode.FINANCE_SUBJECT_MANAGE) && hasMenu(menuCodes, "PAGE_SUBJECTS")) {
            // 变量说明：subjectCount 保存当前步骤计算、查询或转换得到的中间结果。
            long subjectCount = subjectRepository.count(CompanyScope.<AccountingSubject>currentCompanySpec());
            // 变量说明：enabledSubjectCount 保存当前步骤计算、查询或转换得到的中间结果。
            long enabledSubjectCount = subjectRepository.findByOrganizationCodeAndEnabledTrueOrderByCodeAsc(CompanyScope.currentCompanyCode()).size();
            lines.add("会计科目总数: " + subjectCount + "，启用科目: " + enabledSubjectCount);
        }
        if ((permissions.contains(PermissionCode.FINANCE_VOUCHER_MANAGE) && hasMenu(menuCodes, "PAGE_VOUCHERS"))
                || (permissions.contains(PermissionCode.REPORT_VIEW) && hasMenu(menuCodes, "PAGE_REPORTS"))) {
            // 变量说明：vouchers 保存当前步骤计算、查询或转换得到的中间结果。
            List<Voucher> vouchers = voucherRepository.findByOrganizationCodeAndVoucherDateBetweenOrderByVoucherDateDesc(
                    CompanyScope.currentCompanyCode(), LocalDate.now().minusYears(20), LocalDate.now().plusYears(2));
            List<Voucher> monthVouchers = vouchers.stream()
                    .filter(item -> inRange(item.getVoucherDate(), monthStart, monthEnd))
                    .toList();
            lines.add("凭证总数: " + vouchers.size() + "，本月凭证数: " + monthVouchers.size());
            lines.add("凭证状态分布: " + enumCounts(vouchers, Voucher::getStatus));
            lines.add("本月过账凭证数: " + monthVouchers.stream().filter(item -> item.getStatus() != null && "POSTED".equals(item.getStatus().name())).count());
            lines.add("本月借方合计: " + money(sum(monthVouchers, Voucher::getTotalDebitCny)) + "，本月贷方合计: " + money(sum(monthVouchers, Voucher::getTotalCreditCny)));
            if (permissions.contains(PermissionCode.FINANCE_VOUCHER_MANAGE) && hasMenu(menuCodes, "PAGE_VOUCHERS")) {
                lines.add("最近凭证: " + recent(vouchers, item -> item.getVoucherNo() + "/" + item.getVoucherDate() + "/" + item.getSummary(), 5));
                String matchedDetail = matchedVoucherContext(question, vouchers);
                if (!matchedDetail.isBlank()) {
                    lines.add(matchedDetail);
                }
            }
        }
        return lines.size() <= 1 ? "" : String.join("\n", lines);
    }

    /**
     * 执行 purchaseContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String purchaseContext(LocalDate monthStart, LocalDate monthEnd, String question) {
        // 变量说明：orders 保存当前步骤计算、查询或转换得到的中间结果。
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll(CompanyScope.<PurchaseOrder>currentCompanySpec());
        List<PurchaseOrder> monthOrders = orders.stream()
                .filter(item -> inRange(item.getOrderDate(), monthStart, monthEnd))
                .toList();
        LocalDate halfYearStart = LocalDate.now().minusMonths(6);
        List<PurchaseOrder> halfYearOrders = orders.stream()
                .filter(item -> item.getOrderDate() != null && !item.getOrderDate().isBefore(halfYearStart))
                .toList();
        List<String> contextLines = new ArrayList<>();
        contextLines.add("采购管理上下文");
        contextLines.add("采购单总数: " + orders.size() + "，本月采购单数: " + monthOrders.size());
        contextLines.add("采购状态分布: " + enumCounts(orders, PurchaseOrder::getStatus));
        contextLines.add("本月采购总金额: " + money(sum(monthOrders, PurchaseOrder::getTotalAmountCny)));
        contextLines.add("近半年采购单数: " + halfYearOrders.size() + "，近半年采购人民币金额: "
                + money(sum(halfYearOrders, PurchaseOrder::getTotalAmountCny)));
        contextLines.add("最近采购单: " + recent(orders.stream().sorted(Comparator.comparing(PurchaseOrder::getOrderDate, Comparator.nullsLast(Comparator.reverseOrder()))).toList(),
                item -> item.getOrderNo() + "/" + item.getOrderDate() + "/" + item.getSupplierName() + "/" + money(item.getTotalAmountCny()), 5));
        String matchedDetail = matchedPurchaseContext(question, orders);
        if (!matchedDetail.isBlank()) {
            contextLines.add(matchedDetail);
        }
        return String.join("\n", contextLines);
    }

    /**
     * 追加问题命中的采购单明细。
     *
     * <p>实现步骤：从用户问题抽取核心词，在采购单头和明细行字段中匹配，命中后将最多 10 条具体采购单放入助手上下文。</p>
     */
    private String matchedPurchaseContext(String question, List<PurchaseOrder> orders) {
        Set<String> tokens = extractBusinessTokens(question);
        if (tokens.isEmpty()) {
            return "";
        }
        List<PurchaseOrder> matchedOrders = orders.stream()
                .filter(item -> tokens.stream().anyMatch(token -> matchesPurchaseToken(item, token)))
                .limit(10)
                .toList();
        if (matchedOrders.isEmpty()) {
            return "";
        }
        return "问题命中的采购单明细:\n" + matchedOrders.stream()
                .map(this::purchaseDetail)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 判断采购单是否包含指定问题 token。
     */
    private boolean matchesPurchaseToken(PurchaseOrder order, String token) {
        boolean headerMatched = containsIgnoreCase(order.getOrderNo(), token)
                || containsIgnoreCase(order.getSupplierName(), token)
                || containsIgnoreCase(order.getDocumentType(), token)
                || containsIgnoreCase(order.getBusinessType(), token)
                || containsIgnoreCase(order.getProjectName(), token)
                || containsIgnoreCase(order.getProjectCode(), token)
                || containsIgnoreCase(order.getPurchaseOrganization(), token)
                || containsIgnoreCase(order.getPurchaseDepartment(), token)
                || containsIgnoreCase(order.getPurchaserName(), token)
                || containsIgnoreCase(order.getSettlementOrganization(), token)
                || containsIgnoreCase(order.getPaymentTerms(), token)
                || containsIgnoreCase(order.getSettlementMethod(), token)
                || containsIgnoreCase(order.getDeliveryTerms(), token)
                || containsIgnoreCase(order.getSourceBillType(), token)
                || containsIgnoreCase(order.getSourceBillNo(), token)
                || containsIgnoreCase(order.getCreatedBy(), token)
                || containsIgnoreCase(order.getRemark(), token);
        if (headerMatched) {
            return true;
        }
        return order.getLines().stream()
                .anyMatch(line -> containsIgnoreCase(line.getItemCode(), token)
                        || containsIgnoreCase(line.getItemName(), token)
                        || containsIgnoreCase(line.getSpecification(), token)
                        || containsIgnoreCase(line.getUnitName(), token)
                        || containsIgnoreCase(line.getReceiveWarehouse(), token));
    }

    /**
     * 生成采购单上下文明细行。
     */
    private String purchaseDetail(PurchaseOrder order) {
        String lineSummary = order.getLines().stream()
                .limit(5)
                .map(line -> blankValue(line.getItemName()) + "/" + money(line.getQuantity()) + blankValue(line.getUnitName()))
                .collect(Collectors.joining("；"));
        return "- 采购单号: " + value(order.getOrderNo())
                + "，供应商: " + value(order.getSupplierName())
                + "，项目: " + blankValue(order.getProjectName())
                + "，采购日期: " + dateValue(order.getOrderDate())
                + "，状态: " + enumLabel(order.getStatus())
                + "，总金额人民币: " + money(order.getTotalAmountCny())
                + "，来源单据: " + blankValue(order.getSourceBillType()) + " " + blankValue(order.getSourceBillNo())
                + "，结算方式: " + blankValue(order.getSettlementMethod())
                + "，备注: " + blankValue(order.getRemark())
                + "，明细: " + blankValue(lineSummary);
    }

    /**
     * 执行 shipmentContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String shipmentContext(LocalDate monthStart, LocalDate monthEnd, String question) {
        // 变量说明：shipments 保存当前步骤计算、查询或转换得到的中间结果。
        List<ShipmentOrder> shipments = shipmentOrderRepository.findAll(CompanyScope.<ShipmentOrder>currentCompanySpec());
        List<ShipmentOrder> plannedThisMonth = shipments.stream()
                .filter(item -> inRange(item.getPlannedShipDate(), monthStart, monthEnd))
                .toList();
        List<ShipmentOrder> actualThisMonth = shipments.stream()
                .filter(item -> inRange(item.getActualShipDate(), monthStart, monthEnd))
                .toList();
        List<ShipmentOrder> deliveredThisMonth = shipments.stream()
                .filter(item -> inRange(item.getDeliveredDate(), monthStart, monthEnd))
                .toList();
        // 变量说明：contextLines 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> contextLines = new ArrayList<>();
        contextLines.add("物流运输上下文");
        contextLines.add("物流单总数: " + shipments.size());
        contextLines.add("本月计划发运物流单数: " + plannedThisMonth.size());
        contextLines.add("本月实际发运物流单数: " + actualThisMonth.size());
        contextLines.add("本月送达物流单数: " + deliveredThisMonth.size());
        contextLines.add("物流状态分布: " + enumCounts(shipments, ShipmentOrder::getStatus));
        contextLines.add("最近物流单: " + recent(shipments.stream()
                        .sorted(Comparator.comparing(ShipmentOrder::getPlannedShipDate, Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList(),
                item -> item.getShipmentNo()
                        + "/计划发运:" + dateValue(item.getPlannedShipDate())
                        + "/实际发运:" + dateValue(item.getActualShipDate())
                        + "/实际送达:" + dateValue(item.getDeliveredDate())
                        + "/" + item.getCarrierName()
                        + "/" + enumLabel(item.getStatus()), 5));
        // 变量说明：matchedDetail 保存当前步骤计算、查询或转换得到的中间结果。
        String matchedDetail = matchedShipmentContext(question, shipments);
        if (!matchedDetail.isBlank()) {
            contextLines.add(matchedDetail);
        }
        return String.join("\n", contextLines);
    }

    /**
     * 执行 matchedShipmentContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String matchedShipmentContext(String question, List<ShipmentOrder> shipments) {
        // 变量说明：tokens 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> tokens = extractBusinessTokens(question);
        if (tokens.isEmpty()) {
            return "";
        }
        List<ShipmentOrder> matchedShipments = shipments.stream()
                .filter(item -> tokens.stream().anyMatch(token -> matchesShipmentToken(item, token)))
                .limit(10)
                .toList();
        if (matchedShipments.isEmpty()) {
            return "";
        }
        return "问题命中的物流单明细:\n" + matchedShipments.stream()
                .map(this::shipmentDetail)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 执行 extractBusinessTokens 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Set<String> extractBusinessTokens(String question) {
        if (question == null || question.isBlank()) {
            return Set.of();
        }
        // 变量说明：matcher 保存当前步骤计算、查询或转换得到的中间结果。
        Matcher matcher = BUSINESS_TOKEN_PATTERN.matcher(question);
        // 变量说明：tokens 保存当前步骤计算、查询或转换得到的中间结果。
        Set<String> tokens = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        tokens.addAll(extractNaturalQuestionTokens(question));
        return tokens;
    }

    /**
     * 从自然语言问题中提取非单号类关键词。
     *
     * <p>实现步骤：去掉“有没有、查询、内容”等问句虚词，保留 2 到 20 字的中文片段，用于按地点、承运商、项目等字段匹配物流明细。</p>
     */
    private Set<String> extractNaturalQuestionTokens(String question) {
        if (question == null || question.isBlank()) {
            return Set.of();
        }
        String cleaned = question;
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
        Set<String> tokens = new java.util.LinkedHashSet<>();
        for (String item : cleaned.split("[\\s,，。；;]+")) {
            String token = item.trim();
            if (token.length() >= 2 && token.length() <= 20
                    && token.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * 执行 matchesShipmentToken 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean matchesShipmentToken(ShipmentOrder shipment, String token) {
        return containsIgnoreCase(shipment.getShipmentNo(), token)
                || containsIgnoreCase(shipment.getTrackingNo(), token)
                || containsIgnoreCase(shipment.getRelatedOrderNo(), token)
                || containsIgnoreCase(shipment.getProjectName(), token)
                || containsIgnoreCase(shipment.getProjectCode(), token)
                || containsIgnoreCase(shipment.getCarrierName(), token)
                || containsIgnoreCase(shipment.getOriginDivisionName(), token)
                || containsIgnoreCase(shipment.getDestinationDivisionName(), token)
                || containsIgnoreCase(shipment.getOrigin(), token)
                || containsIgnoreCase(shipment.getDestination(), token)
                || containsIgnoreCase(shipment.getRemark(), token);
    }

    /**
     * 执行 containsIgnoreCase 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean containsIgnoreCase(String value, String token) {
        return value != null && token != null
                && value.toLowerCase(java.util.Locale.ROOT).contains(token.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * 执行 shipmentDetail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String shipmentDetail(ShipmentOrder shipment) {
        return "- 物流单号: " + value(shipment.getShipmentNo())
                + "，关联单号: " + blankValue(shipment.getRelatedOrderNo())
                + "，承运商: " + value(shipment.getCarrierName())
                + "，运单号: " + blankValue(shipment.getTrackingNo())
                + "，状态: " + enumLabel(shipment.getStatus())
                + "，计划发运日期: " + dateValue(shipment.getPlannedShipDate())
                + "，实际发运日期: " + dateValue(shipment.getActualShipDate())
                + "，实际送达日期: " + dateValue(shipment.getDeliveredDate())
                + "，发货地: " + blankValue(shipment.getOriginDivisionName()) + " " + blankValue(shipment.getOrigin())
                + "，目的地: " + blankValue(shipment.getDestinationDivisionName()) + " " + blankValue(shipment.getDestination())
                + "，备注: " + blankValue(shipment.getRemark());
    }

    /**
     * 执行 inventoryContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String inventoryContext(LocalDate monthStart, LocalDate monthEnd, String question) {
        // 变量说明：ledgers 保存当前步骤计算、查询或转换得到的中间结果。
        List<InventoryLedger> ledgers = inventoryLedgerRepository.findAll(CompanyScope.<InventoryLedger>currentCompanySpec());
        List<InventoryLedger> monthLedgers = ledgers.stream()
                .filter(item -> inRange(item.getMovementDate(), monthStart, monthEnd))
                .toList();
        LocalDate halfYearStart = LocalDate.now().minusMonths(6);
        List<InventoryLedger> halfYearLedgers = ledgers.stream()
                .filter(item -> item.getMovementDate() != null && !item.getMovementDate().isBefore(halfYearStart))
                .toList();
        List<String> contextLines = new ArrayList<>();
        contextLines.add("库存管理上下文");
        contextLines.add("库存流水总数: " + ledgers.size() + "，本月库存流水数: " + monthLedgers.size());
        contextLines.add("库存变动类型分布: " + enumCounts(ledgers, InventoryLedger::getMovementType));
        contextLines.add("本月库存变动数量合计: " + money(sum(monthLedgers, InventoryLedger::getQuantity)));
        contextLines.add("近半年库存流水数: " + halfYearLedgers.size() + "，近半年变动数量合计: "
                + money(sum(halfYearLedgers, InventoryLedger::getQuantity)) + "，入库未制证数: "
                + halfYearLedgers.stream().filter(item -> item.getVoucherId() == null).count());
        contextLines.add("最近库存流水: " + recent(ledgers.stream().sorted(Comparator.comparing(InventoryLedger::getMovementDate, Comparator.nullsLast(Comparator.reverseOrder()))).toList(),
                item -> item.getMovementNo() + "/" + item.getMovementDate() + "/" + item.getMovementType() + "/" + item.getItemName() + "/" + money(item.getQuantity()), 5));
        String matchedDetail = matchedInventoryContext(question, ledgers);
        if (!matchedDetail.isBlank()) {
            contextLines.add(matchedDetail);
        }
        return String.join("\n", contextLines);
    }

    /**
     * 执行 arApContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String arApContext(LocalDate monthStart, LocalDate monthEnd, LocalDate today, String question) {
        // 变量说明：bills 保存当前步骤计算、查询或转换得到的中间结果。
        List<ArApBill> bills = arApBillRepository.findAll(CompanyScope.<ArApBill>currentCompanySpec());
        List<ArApBill> monthBills = bills.stream()
                .filter(item -> inRange(item.getBillDate(), monthStart, monthEnd))
                .toList();
        List<ArApBill> dueThisMonth = bills.stream()
                .filter(item -> inRange(item.getDueDate(), monthStart, monthEnd))
                .toList();
        List<ArApBill> overdue = bills.stream()
                .filter(item -> item.getDueDate() != null && item.getDueDate().isBefore(today))
                .filter(item -> item.getStatus() == null || !"CLOSED".equals(item.getStatus().name()))
                .toList();
        LocalDate halfYearStart = today.minusMonths(6);
        List<ArApBill> halfYearBills = bills.stream()
                .filter(item -> item.getBillDate() != null && !item.getBillDate().isBefore(halfYearStart))
                .toList();
        List<ArApBill> halfYearReceivables = halfYearBills.stream()
                .filter(item -> item.getBillType() != null && "RECEIVABLE".equals(item.getBillType().name()))
                .toList();
        List<ArApBill> halfYearPayables = halfYearBills.stream()
                .filter(item -> item.getBillType() != null && "PAYABLE".equals(item.getBillType().name()))
                .toList();
        List<String> contextLines = new ArrayList<>();
        contextLines.add("应收应付上下文");
        contextLines.add("应收应付单总数: " + bills.size() + "，本月新增单据数: " + monthBills.size() + "，本月到期单据数: " + dueThisMonth.size());
        contextLines.add("应收应付类型分布: " + enumCounts(bills, ArApBill::getBillType));
        contextLines.add("应收应付状态分布: " + enumCounts(bills, ArApBill::getStatus));
        contextLines.add("未结余额合计: " + money(bills.stream().map(item -> safe(item.getAmountCny()).subtract(safe(item.getPaidAmountCny()))).reduce(BigDecimal.ZERO, BigDecimal::add)));
        contextLines.add("逾期未结单据数: " + overdue.size());
        contextLines.add("近半年应收单数: " + halfYearReceivables.size() + "，待收人民币余额: " + money(remainingCny(halfYearReceivables))
                + "；近半年应付单数: " + halfYearPayables.size() + "，待付人民币余额: " + money(remainingCny(halfYearPayables)));
        contextLines.add("近半年往来单位未结余额排名: " + partnerBalanceSummary(halfYearBills));
        contextLines.add("最近到期单据: " + recent(bills.stream().sorted(Comparator.comparing(ArApBill::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))).toList(),
                item -> item.getBillNo() + "/" + item.getDueDate() + "/" + item.getPartnerName() + "/" + money(safe(item.getAmountCny()).subtract(safe(item.getPaidAmountCny()))), 5));
        String matchedDetail = matchedArApContext(question, bills);
        if (!matchedDetail.isBlank()) {
            contextLines.add(matchedDetail);
        }
        return String.join("\n", contextLines);
    }

    /** 汇总应收应付单的未结人民币余额。 */
    private BigDecimal remainingCny(List<ArApBill> bills) {
        return bills.stream()
                .map(item -> safe(item.getAmountCny()).subtract(safe(item.getPaidAmountCny())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 按往来单位汇总最近半年未结余额，供专业术语和日常问法复用同一事实口径。 */
    private String partnerBalanceSummary(List<ArApBill> bills) {
        return bills.stream()
                .collect(Collectors.groupingBy(item -> value(item.getPartnerName()),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> safe(item.getAmountCny()).subtract(safe(item.getPaidAmountCny())),
                                BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(8)
                .map(item -> item.getKey() + "/" + money(item.getValue()))
                .collect(Collectors.joining("；"));
    }

    /**
     * 追加问题命中的库存流水明细。
     *
     * <p>实现步骤：从用户问题抽取核心词，在物料、仓库、项目、批号、关联业务单号和备注中匹配，命中后返回具体库存流水。</p>
     */
    private String matchedInventoryContext(String question, List<InventoryLedger> ledgers) {
        Set<String> tokens = extractBusinessTokens(question);
        if (tokens.isEmpty()) {
            return "";
        }
        List<InventoryLedger> matchedLedgers = ledgers.stream()
                .filter(item -> tokens.stream().anyMatch(token -> matchesInventoryToken(item, token)))
                .limit(10)
                .toList();
        if (matchedLedgers.isEmpty()) {
            return "";
        }
        return "问题命中的库存流水明细:\n" + matchedLedgers.stream()
                .map(this::inventoryDetail)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 判断库存流水是否包含指定问题 token。
     */
    private boolean matchesInventoryToken(InventoryLedger ledger, String token) {
        return containsIgnoreCase(ledger.getMovementNo(), token)
                || containsIgnoreCase(ledger.getItemCode(), token)
                || containsIgnoreCase(ledger.getItemName(), token)
                || containsIgnoreCase(ledger.getProjectCode(), token)
                || containsIgnoreCase(ledger.getProjectName(), token)
                || containsIgnoreCase(ledger.getSpecification(), token)
                || containsIgnoreCase(ledger.getStockOrganization(), token)
                || containsIgnoreCase(ledger.getOwnerName(), token)
                || containsIgnoreCase(ledger.getUnitName(), token)
                || containsIgnoreCase(ledger.getBatchNo(), token)
                || containsIgnoreCase(ledger.getFromWarehouse(), token)
                || containsIgnoreCase(ledger.getToWarehouse(), token)
                || containsIgnoreCase(ledger.getRelatedBizNo(), token)
                || containsIgnoreCase(ledger.getSourceBillType(), token)
                || containsIgnoreCase(ledger.getRemark(), token);
    }

    /**
     * 生成库存流水上下文明细行。
     */
    private String inventoryDetail(InventoryLedger ledger) {
        return "- 库存流水号: " + value(ledger.getMovementNo())
                + "，变动类型: " + enumLabel(ledger.getMovementType())
                + "，变动日期: " + dateValue(ledger.getMovementDate())
                + "，物料: " + value(ledger.getItemName()) + "(" + value(ledger.getItemCode()) + ")"
                + "，规格: " + blankValue(ledger.getSpecification())
                + "，数量: " + money(ledger.getQuantity()) + blankValue(ledger.getUnitName())
                + "，来源仓库: " + blankValue(ledger.getFromWarehouse())
                + "，目标仓库: " + blankValue(ledger.getToWarehouse())
                + "，关联业务单号: " + blankValue(ledger.getRelatedBizNo())
                + "，项目: " + blankValue(ledger.getProjectName())
                + "，备注: " + blankValue(ledger.getRemark());
    }

    /**
     * 追加问题命中的应收应付单明细。
     *
     * <p>实现步骤：从用户问题抽取核心词，在往来单位、项目、组织、来源单号、付款计划和结算字段中匹配，命中后返回具体债权债务单。</p>
     */
    private String matchedArApContext(String question, List<ArApBill> bills) {
        Set<String> tokens = extractBusinessTokens(question);
        if (tokens.isEmpty()) {
            return "";
        }
        List<ArApBill> matchedBills = bills.stream()
                .filter(item -> tokens.stream().anyMatch(token -> matchesArApToken(item, token)))
                .limit(10)
                .toList();
        if (matchedBills.isEmpty()) {
            return "";
        }
        return "问题命中的应收应付单明细:\n" + matchedBills.stream()
                .map(this::arApDetail)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 判断应收应付单是否包含指定问题 token。
     */
    private boolean matchesArApToken(ArApBill bill, String token) {
        return containsIgnoreCase(bill.getBillNo(), token)
                || containsIgnoreCase(bill.getPartnerName(), token)
                || containsIgnoreCase(bill.getProjectCode(), token)
                || containsIgnoreCase(bill.getProjectName(), token)
                || containsIgnoreCase(bill.getDocumentType(), token)
                || containsIgnoreCase(bill.getBusinessOrganization(), token)
                || containsIgnoreCase(bill.getSettlementOrganization(), token)
                || containsIgnoreCase(bill.getPaymentOrganization(), token)
                || containsIgnoreCase(bill.getPaymentTerms(), token)
                || containsIgnoreCase(bill.getSettlementMethod(), token)
                || containsIgnoreCase(bill.getSourceBillType(), token)
                || containsIgnoreCase(bill.getSourceBillNo(), token)
                || containsIgnoreCase(bill.getPaymentPlan(), token);
    }

    /**
     * 生成应收应付上下文明细行。
     */
    private String arApDetail(ArApBill bill) {
        BigDecimal remaining = safe(bill.getAmount()).subtract(safe(bill.getPaidAmount()));
        BigDecimal remainingCny = safe(bill.getAmountCny()).subtract(safe(bill.getPaidAmountCny()));
        return "- 单据编号: " + value(bill.getBillNo())
                + "，类型: " + enumLabel(bill.getBillType())
                + "，往来单位: " + value(bill.getPartnerName())
                + "，项目: " + blankValue(bill.getProjectName())
                + "，单据日期: " + dateValue(bill.getBillDate())
                + "，到期日期: " + dateValue(bill.getDueDate())
                + "，状态: " + enumLabel(bill.getStatus())
                + "，金额: " + money(bill.getAmount())
                + "，未结: " + money(remaining)
                + "，未结人民币: " + money(remainingCny)
                + "，来源单据: " + blankValue(bill.getSourceBillType()) + " " + blankValue(bill.getSourceBillNo())
                + "，付款计划: " + blankValue(bill.getPaymentPlan());
    }

    /**
     * 追加问题命中的财务凭证明细。
     *
     * <p>实现步骤：从用户问题抽取核心词，在凭证号、摘要、项目、来源单号和分录科目中匹配，命中后返回具体凭证。</p>
     */
    private String matchedVoucherContext(String question, List<Voucher> vouchers) {
        Set<String> tokens = extractBusinessTokens(question);
        if (tokens.isEmpty()) {
            return "";
        }
        List<Voucher> matchedVouchers = vouchers.stream()
                .filter(item -> tokens.stream().anyMatch(token -> matchesVoucherToken(item, token)))
                .limit(10)
                .toList();
        if (matchedVouchers.isEmpty()) {
            return "";
        }
        return "问题命中的凭证明细:\n" + matchedVouchers.stream()
                .map(this::voucherDetail)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 判断凭证是否包含指定问题 token。
     */
    private boolean matchesVoucherToken(Voucher voucher, String token) {
        boolean headerMatched = containsIgnoreCase(voucher.getVoucherNo(), token)
                || containsIgnoreCase(voucher.getSummary(), token)
                || containsIgnoreCase(voucher.getProjectCode(), token)
                || containsIgnoreCase(voucher.getProjectName(), token)
                || containsIgnoreCase(voucher.getSourceBizNo(), token)
                || containsIgnoreCase(voucher.getSourceType() == null ? null : voucher.getSourceType().name(), token)
                || containsIgnoreCase(voucher.getSourceTitle(), token)
                || containsIgnoreCase(voucher.getCurrencyCode(), token)
                || containsIgnoreCase(voucher.getCurrencyName(), token)
                || containsIgnoreCase(voucher.getCreatedBy(), token)
                || containsIgnoreCase(voucher.getPostedBy(), token);
        if (headerMatched) {
            return true;
        }
        return voucher.getLines().stream()
                .anyMatch(line -> containsIgnoreCase(line.getSummary(), token)
                        || containsIgnoreCase(line.getAuxiliary(), token)
                        || containsIgnoreCase(line.getCurrencyCode(), token)
                        || (line.getSubject() != null
                        && (containsIgnoreCase(line.getSubject().getCode(), token)
                        || containsIgnoreCase(line.getSubject().getName(), token))));
    }

    /**
     * 生成凭证上下文明细行。
     */
    private String voucherDetail(Voucher voucher) {
        String lineSummary = voucher.getLines().stream()
                .limit(5)
                .map(line -> "分录" + line.getLineNo()
                        + "/" + (line.getSubject() == null ? "" : line.getSubject().getCode() + " " + line.getSubject().getName())
                        + "/" + blankValue(line.getSummary())
                        + "/借" + money(line.getDebitAmount())
                        + "/贷" + money(line.getCreditAmount()))
                .collect(Collectors.joining("；"));
        return "- 凭证号: " + value(voucher.getVoucherNo())
                + "，日期: " + dateValue(voucher.getVoucherDate())
                + "，摘要: " + value(voucher.getSummary())
                + "，项目: " + blankValue(voucher.getProjectName())
                + "，状态: " + enumLabel(voucher.getStatus())
                + "，来源业务单号: " + blankValue(voucher.getSourceBizNo())
                + "，借方合计: " + money(voucher.getTotalDebitCny())
                + "，贷方合计: " + money(voucher.getTotalCreditCny())
                + "，分录: " + blankValue(lineSummary);
    }

    /**
     * 执行 attachmentContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String attachmentContext() {
        return lines(
                "附件与知识索引上下文",
                "附件文件总数: " + attachmentFileRepository.count(),
                "业务附件关系数: " + businessAttachmentRepository.count()
        );
    }

    /**
     * 执行 auditContext 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String auditContext(OffsetDateTime monthStart, OffsetDateTime monthEnd) {
        // 变量说明：logs 保存当前账套的操作日志，避免审计上下文跨公司统计。
        List<UserOperationLog> logs = operationLogRepository.findAll(CompanyScope.<UserOperationLog>currentCompanySpec());
        long total = logs.size();
        long currentMonth = logs.stream()
                .filter(item -> item.getOperationTime() != null
                        && !item.getOperationTime().isBefore(monthStart)
                        && !item.getOperationTime().isAfter(monthEnd))
                .count();
        return lines(
                "审计日志上下文",
                "操作日志总数: " + total + "，本月操作日志数: " + currentMonth
        );
    }

    /**
     * 查询当前登录人授权菜单编码。
     *
     * <p>实现步骤：
     * 1. 根据当前登录人 ID 读取最新人员角色和菜单授权；
     * 2. 只收集启用菜单，并补齐启用父级菜单编码；
     * 3. 查询失败时返回空集合，避免降级上下文越权展示。</p>
     */
    private Set<String> authorizedMenuCodes(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.id() <= 0) {
            return Set.of();
        }
        return userAccountRepository.findById(currentUser.id())
                .map(user -> {
                    // 变量说明：menuCodes 保存当前人员角色展开后的授权菜单编码。
                    Set<String> menuCodes = new HashSet<>();
                    for (Role role : user.getRoles()) {
                        for (SystemMenu menu : role.getMenus()) {
                            addEnabledMenuWithAncestors(menu, menuCodes);
                        }
                    }
                    return menuCodes;
                })
                .orElse(Set.of());
    }

    /**
     * 收集启用菜单及其启用父级菜单编码。
     *
     * <p>实现步骤：从当前菜单向父级递归，遇到停用菜单立即停止，保持与前端授权菜单接口一致。</p>
     */
    private void addEnabledMenuWithAncestors(SystemMenu menu, Set<String> menuCodes) {
        if (menu == null || !menu.isEnabled()) {
            return;
        }
        menuCodes.add(menu.getCode());
        addEnabledMenuWithAncestors(menu.getParent(), menuCodes);
    }

    /**
     * 判断当前登录人是否可查看系统管理上下文。
     */
    private boolean canViewSystemContext(Set<PermissionCode> permissions, Set<String> menuCodes) {
        return (permissions.contains(PermissionCode.SYSTEM_USER_MANAGE) && hasMenu(menuCodes, "PAGE_USERS"))
                || (permissions.contains(PermissionCode.SYSTEM_ROLE_MANAGE) && hasAnyMenu(menuCodes, "PAGE_ROLES", "PAGE_MENUS"));
    }

    /**
     * 判断当前登录人是否可查看财务管理上下文。
     */
    private boolean canViewFinanceContext(Set<PermissionCode> permissions, Set<String> menuCodes) {
        return (permissions.contains(PermissionCode.FINANCE_SUBJECT_MANAGE) && hasMenu(menuCodes, "PAGE_SUBJECTS"))
                || (permissions.contains(PermissionCode.FINANCE_VOUCHER_MANAGE)
                && hasAnyMenu(menuCodes, "PAGE_VOUCHERS", "PAGE_ACCOUNTING_PERIODS", "PAGE_CASHIER", "PAGE_ACCOUNTING_PLATFORM"))
                || (permissions.contains(PermissionCode.REPORT_VIEW) && hasMenu(menuCodes, "PAGE_REPORTS"));
    }

    /**
     * 判断当前登录人是否可查看附件和知识索引上下文。
     */
    private boolean canViewAttachmentContext(Set<PermissionCode> permissions, Set<String> menuCodes) {
        return (permissions.contains(PermissionCode.SEARCH_VIEW) && hasMenu(menuCodes, "BTN_SEARCH_QUERY"))
                || hasAnyMenu(menuCodes,
                "BTN_VOUCHER_ATTACHMENT",
                "BTN_PURCHASE_ATTACHMENT",
                "BTN_SHIPMENT_ATTACHMENT",
                "BTN_INVENTORY_ATTACHMENT",
                "BTN_AR_AP_ATTACHMENT");
    }

    /**
     * 判断是否拥有指定菜单编码。
     */
    private boolean hasMenu(Set<String> menuCodes, String menuCode) {
        return menuCodes != null && menuCodes.contains(menuCode);
    }

    /**
     * 判断是否拥有任一指定菜单编码。
     */
    private boolean hasAnyMenu(Set<String> menuCodes, String... candidates) {
        if (menuCodes == null || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (menuCodes.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行 inRange 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean inRange(LocalDate value, LocalDate start, LocalDate end) {
        return value != null && !value.isBefore(start) && !value.isAfter(end);
    }

    /**
     * 统计枚举字段中文分布。
     *
     * <p>实现步骤：
     * 1. 从业务数据中提取非空枚举值；
     * 2. 使用 EnumMap 按枚举值分组计数；
     * 3. 转换为中文标签和值的摘要，供 AI 上下文直接引用。</p>
     */
    private <T, E extends Enum<E>> String enumCounts(List<T> values, Function<T, E> getter) {
        Map<E, Long> counts = values.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), () -> new EnumMap<>(enumClass(values, getter)), Collectors.counting()));
        return counts.isEmpty() ? "无" : counts.entrySet().stream()
                .map(item -> enumLabel(item.getKey()) + "=" + item.getValue())
                .collect(Collectors.joining("，"));
    }

    @SuppressWarnings("unchecked")
    /**
     * 推断枚举字段类型。
     *
     * <p>实现步骤：
     * 1. 查找第一条非空枚举值；
     * 2. 返回该值声明的枚举类用于创建 EnumMap；
     * 3. 没有样本时使用权限枚举兜底，避免空集合统计抛出异常。</p>
     */
    private <T, E extends Enum<E>> Class<E> enumClass(List<T> values, Function<T, E> getter) {
        return values.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .findFirst()
                .map(item -> (Class<E>) item.getDeclaringClass())
                .orElse((Class<E>) PermissionCode.class);
    }

    /**
     * 执行 enumLabel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String enumLabel(Enum<?> value) {
        if (value == null) {
            return "";
        }
        return switch (value.name()) {
            case "DRAFT" -> "草稿";
            case "POSTED" -> "已过账";
            case "VOIDED" -> "已作废";
            case "SUBMITTED" -> "已提交";
            case "APPROVED" -> "已审批";
            case "RECEIVED" -> "已收货";
            case "CLOSED" -> "已关闭/已结清";
            case "CANCELLED" -> "已取消";
            case "CREATED" -> "草稿";
            case "DISPATCHED" -> "已发送";
            case "IN_TRANSIT" -> "运输中";
            case "DELIVERED" -> "已送达";
            case "INBOUND" -> "入库";
            case "OUTBOUND" -> "出库";
            case "TRANSFER" -> "调拨";
            case "CHECK" -> "盘点";
            case "RECEIVABLE" -> "应收";
            case "PAYABLE" -> "应付";
            case "OPEN" -> "未结";
            case "PARTIAL" -> "部分结清";
            case "OVERDUE" -> "逾期";
            default -> value.name();
        };
    }

    /**
     * 精确汇总金额字段。
     *
     * <p>实现步骤：
     * 1. 使用 getter 读取每条记录的金额；
     * 2. null 金额按 0 处理；
     * 3. 使用 BigDecimal 累加，避免浮点误差影响财务统计。</p>
     */
    private <T> BigDecimal sum(Collection<T> values, Function<T, BigDecimal> getter) {
        return values.stream()
                .map(getter)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 执行 safe 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 执行 money 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String money(BigDecimal value) {
        return safe(value).stripTrailingZeros().toPlainString();
    }

    /**
     * 生成最近业务记录摘要。
     *
     * <p>实现步骤：
     * 1. 截取前 limit 条已按业务口径排序的记录；
     * 2. 使用 formatter 转换为可读文本；
     * 3. 没有内容时返回“无”，避免 AI 上下文出现空段落。</p>
     */
    private <T> String recent(List<T> values, Function<T, String> formatter, int limit) {
        String text = values.stream()
                .limit(limit)
                .map(formatter)
                .collect(Collectors.joining("；"));
        return text.isBlank() ? "无" : text;
    }

    /**
     * 拼接前 N 条非空摘要。
     *
     * <p>实现步骤：
     * 1. 截取前 limit 条业务数据；
     * 2. 使用 formatter 生成摘要并过滤空文本；
     * 3. 使用中文逗号拼接，空结果返回“无”。</p>
     */
    private <T> String joinTop(List<T> values, Function<T, String> formatter, int limit) {
        String text = values.stream()
                .limit(limit)
                .map(formatter)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.joining("，"));
        return text.isBlank() ? "无" : text;
    }

    /**
     * 执行 mapText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String mapText(Map<String, Long> values) {
        if (values.isEmpty()) {
            return "无";
        }
        return values.entrySet().stream()
                .map(item -> item.getKey() + "=" + item.getValue())
                .collect(Collectors.joining("，"));
    }

    /**
     * 执行 lines 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String lines(Object... values) {
        // 变量说明：lines 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> lines = new ArrayList<>();
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) {
                lines.add(String.valueOf(value));
            }
        }
        return String.join("\n", lines);
    }

    /**
     * 执行 value 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 执行 blankValue 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String blankValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "无";
        }
        return String.valueOf(value);
    }

    /**
     * 执行 dateValue 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String dateValue(LocalDate value) {
        return value == null ? "未记录" : value.toString();
    }
}
