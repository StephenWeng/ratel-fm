package com.ratel.fm.service.operation;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.logistics.ShipmentOrder;
import com.ratel.fm.domain.logistics.ShipmentOperationLog;
import com.ratel.fm.domain.logistics.ShipmentStatus;
import com.ratel.fm.domain.purchase.PurchaseOrder;
import com.ratel.fm.domain.purchase.PurchaseOrderLine;
import com.ratel.fm.domain.purchase.PurchaseStatus;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.repository.logistics.ShipmentOperationLogRepository;
import com.ratel.fm.repository.logistics.ShipmentOrderRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.CurrentUser;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.export.ExportProperties;
import com.ratel.fm.service.attachment.AttachmentService;
import com.ratel.fm.service.basic.CurrencyService;
import com.ratel.fm.service.basic.CurrencyService.CurrencySnapshot;
import com.ratel.fm.service.common.BusinessNumberSequenceService;
import com.ratel.fm.service.export.ExcelExportService;
import com.ratel.fm.service.export.ExcelExportService.ExcelColumn;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.service.operationlog.BusinessOperationLogService;
import com.ratel.fm.service.workflow.WorkflowService;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderExportRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseApprovalSubmitRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseCancelRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseLineView;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.PurchaseOrderView;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentExportRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentOperationLogPage;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentOperationLogView;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentStatusConfirmRequest;
import com.ratel.fm.web.dto.operation.OperationDtos.ShipmentView;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogView;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowStartRequest;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 采购与物流业务服务。
 *
 * <p>负责采购单、采购明细、物流单的创建、修改、查询和状态流转。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class OperationService {

    /** 采购审批绑定的流程功能模块编码，流程管理通过该编码替换或更新模板。 */
    public static final String PURCHASE_APPROVAL_FUNCTION_CODE = "PURCHASE_APPROVAL";

    /** 采购单流程业务类型，审批实例用该值回调采购状态机。 */
    private static final String PURCHASE_WORKFLOW_BUSINESS_TYPE = "PURCHASE_ORDER";

    /** 全国行政区划六位编码格式，仅用于兼容旧版只存叶子编码的区划搜索。 */
    private static final Pattern ADMIN_DIVISION_CODE_PATTERN = Pattern.compile("\\d{6}");

    /**
     * 字段 purchaseOrderRepository：保存 purchaseOrderRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final PurchaseOrderRepository purchaseOrderRepository;
    /**
     * 字段 shipmentOrderRepository：保存 shipmentOrderRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ShipmentOrderRepository shipmentOrderRepository;
    /**
     * 字段 shipmentOperationLogRepository：保存 shipmentOperationLogRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ShipmentOperationLogRepository shipmentOperationLogRepository;
    /**
     * 字段 auditLogService：保存 auditLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AuditLogService auditLogService;
    /**
     * 字段 exportProperties：保存 exportProperties 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ExportProperties exportProperties;
    /**
     * 字段 excelExportService：保存 excelExportService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ExcelExportService excelExportService;
    /**
     * 字段 currencyService：保存 currencyService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final CurrencyService currencyService;
    /**
     * 字段 attachmentService：保存 attachmentService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final AttachmentService attachmentService;
    /**
     * 字段 businessOperationLogService：保存 businessOperationLogService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BusinessOperationLogService businessOperationLogService;
    /**
     * 字段 knowledgeIndexService：保存 knowledgeIndexService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final KnowledgeIndexService knowledgeIndexService;
    /** 流程服务，用于采购单提交审批和读取最近审批详情。 */
    private final WorkflowService workflowService;
    /** 业务单号序号服务，用于并发安全生成采购单号和物流单号。 */
    private final BusinessNumberSequenceService numberSequenceService;

    /**
     * 构造 OperationService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public OperationService(
            PurchaseOrderRepository purchaseOrderRepository,
            ShipmentOrderRepository shipmentOrderRepository,
            ShipmentOperationLogRepository shipmentOperationLogRepository,
            AuditLogService auditLogService,
            ExportProperties exportProperties,
            ExcelExportService excelExportService,
            CurrencyService currencyService,
            AttachmentService attachmentService,
            BusinessOperationLogService businessOperationLogService,
            KnowledgeIndexService knowledgeIndexService,
            WorkflowService workflowService,
            BusinessNumberSequenceService numberSequenceService
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.shipmentOperationLogRepository = shipmentOperationLogRepository;
        this.auditLogService = auditLogService;
        this.exportProperties = exportProperties;
        this.excelExportService = excelExportService;
        this.currencyService = currencyService;
        this.attachmentService = attachmentService;
        this.businessOperationLogService = businessOperationLogService;
        this.knowledgeIndexService = knowledgeIndexService;
        this.workflowService = workflowService;
        this.numberSequenceService = numberSequenceService;
    }

    /**
     * 新增采购单。
     *
     * <p>实现步骤：
     * 1. 按采购日期生成唯一采购单号；
     * 2. 写入供应商、日期、创建人和备注；
     * 3. 重建采购明细并计算总金额；
     * 4. 保存采购单；
     * 5. 记录关键操作日志。</p>
     */
    @Transactional
    public PurchaseOrderView createPurchaseOrder(PurchaseOrderRequest request) {
        // 步骤1-2：采购单初始为草稿，创建人来自当前登录上下文。
        PurchaseOrder order = new PurchaseOrder();
        order.setOrganizationCode(CompanyScope.currentCompanyCode());
        order.setOrderNo(nextPurchaseOrderNo(request.orderDate()));
        order.setSupplierName(request.supplierName());
        applyPurchaseHeader(order, request);
        order.setOrderDate(request.orderDate());
        order.setCreatedBy(SecurityUtils.currentUser().username());
        order.setRemark(request.remark());
        // 步骤3：根据请求明细重建实体明细，每条明细独立保存币种、汇率和折人民币金额。
        replacePurchaseLines(order, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        PurchaseOrderView view = toPurchaseOrderView(purchaseOrderRepository.save(order));
        businessOperationLogService.record("PURCHASE_ORDER", view.id(), view.orderNo(), purchaseTitle(view), "CREATE", "新增采购单",
                "新增采购单号" + view.orderNo() + "，供应商为" + view.supplierName() + "。", null, purchaseStatusText(view.status()), view);
        knowledgeIndexService.rebuildPurchaseOrder(order);
        // 步骤4：采购单会成为库存、应付和凭证来源，需要记录审计日志。
        auditLogService.record("CREATE_PURCHASE_ORDER", request, "SUCCESS",
                "采购管理新增了采购单号" + view.orderNo() + "，供应商为" + view.supplierName() + "。");
        return view;
    }

    /**
     * 修改采购单。
     *
     * <p>实现步骤：
     * 1. 带明细读取采购单；
     * 2. 仅草稿采购单允许修改；
     * 3. 更新主表字段；
     * 4. 替换采购明细并重算金额；
     * 5. 记录审计日志。</p>
     */
    @Transactional
    public PurchaseOrderView updatePurchaseOrder(Long id, PurchaseOrderRequest request) {
        // 步骤1：修改采购明细需要加载旧明细集合，便于 orphanRemoval 删除旧行。
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        // 步骤2：非草稿采购单已经进入业务流转，不允许随意改明细。
        if (order.getStatus() != PurchaseStatus.DRAFT && order.getStatus() != PurchaseStatus.APPROVAL_REJECTED) {
            throw new BusinessException("仅草稿或审批不同意的采购单允许修改");
        }
        // 步骤3-4：更新主表字段后整体替换明细，保证请求即最终状态。
        order.setSupplierName(request.supplierName());
        applyPurchaseHeader(order, request);
        order.setOrderDate(request.orderDate());
        order.setRemark(request.remark());
        replacePurchaseLines(order, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        PurchaseOrderView view = toPurchaseOrderView(order);
        businessOperationLogService.record("PURCHASE_ORDER", view.id(), view.orderNo(), purchaseTitle(view), "UPDATE", "修改采购单",
                "修改采购单号" + view.orderNo() + "，供应商为" + view.supplierName() + "。", purchaseStatusText(view.status()), purchaseStatusText(view.status()), view);
        knowledgeIndexService.rebuildPurchaseOrder(order);
        // 步骤5：采购变更影响后续库存和应付，必须记录。
        auditLogService.record("UPDATE_PURCHASE_ORDER", "purchaseOrderId=" + id + ", " + request, "SUCCESS",
                "采购管理修改了采购单号" + view.orderNo() + "。");
        return view;
    }

    /**
     * 变更采购单状态。
     *
     * <p>实现步骤：
     * 1. 读取采购单；
     * 2. 记录原状态；
     * 3. 写入目标状态；
     * 4. 记录状态流转审计日志。</p>
     */
    @Transactional
    public PurchaseOrderView changePurchaseStatus(Long id, PurchaseStatus status) {
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        // 变量说明：oldStatus 保存当前步骤计算、查询或转换得到的中间结果。
        PurchaseStatus oldStatus = order.getStatus();
        order.setStatus(status);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        PurchaseOrderView view = toPurchaseOrderView(order);
        businessOperationLogService.record("PURCHASE_ORDER", view.id(), view.orderNo(), purchaseTitle(view), "STATUS_CHANGE", "变更采购状态",
                "将采购单号" + view.orderNo() + "的状态从" + purchaseStatusText(oldStatus) + "改为" + purchaseStatusText(status) + "。",
                purchaseStatusText(oldStatus), purchaseStatusText(status), view);
        knowledgeIndexService.rebuildPurchaseOrder(order);
        // 状态变化是采购业务闭环的重要节点，需要记录 from/to。
        auditLogService.record("CHANGE_PURCHASE_STATUS", "purchaseOrderId=" + id + ", from=" + oldStatus + ", to=" + status,
                "SUCCESS", "采购管理将采购单号" + view.orderNo() + "的状态从" + purchaseStatusText(oldStatus) + "改为" + purchaseStatusText(status) + "。");
        return view;
    }

    /**
     * 提交采购审批。
     *
     * <p>实现步骤：
     * 1. 读取采购单并校验当前状态为草稿或审批不同意；
     * 2. 复用采购明细完整性校验，避免不完整单据进入审批；
     * 3. 调用流程服务按采购审批功能模块编码发起流程；
     * 4. 流程发起成功后把采购状态改为审批中；
     * 5. 记录采购业务流水和审计日志。</p>
     */
    @Transactional
    public PurchaseOrderView submitPurchaseApproval(Long id, PurchaseApprovalSubmitRequest request) {
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        if (order.getStatus() != PurchaseStatus.DRAFT && order.getStatus() != PurchaseStatus.APPROVAL_REJECTED) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "仅草稿或审批不同意的采购单允许提交审批");
        }
        validatePurchaseReadyForApproval(order);
        PurchaseStatus oldStatus = order.getStatus();
        workflowService.startWorkflow(new WorkflowStartRequest(
                PURCHASE_APPROVAL_FUNCTION_CODE,
                PURCHASE_WORKFLOW_BUSINESS_TYPE,
                order.getId(),
                order.getOrderNo(),
                order.getProjectCode(),
                order.getProjectName(),
                "采购审批：" + order.getOrderNo(),
                request == null ? null : request.applyReason()
        ));
        order.setStatus(PurchaseStatus.IN_APPROVAL);
        PurchaseOrderView view = toPurchaseOrderView(order);
        businessOperationLogService.record("PURCHASE_ORDER", view.id(), view.orderNo(), purchaseTitle(view), "SUBMIT_APPROVAL", "提交采购审批",
                "采购单号" + view.orderNo() + "提交审批。", purchaseStatusText(oldStatus), purchaseStatusText(view.status()), view);
        knowledgeIndexService.rebuildPurchaseOrder(order);
        auditLogService.record("SUBMIT_PURCHASE_APPROVAL", "purchaseOrderId=" + id + ", " + request, "SUCCESS",
                "采购管理提交采购单号" + view.orderNo() + "审批。");
        return view;
    }

    /**
     * 发起采购履约。
     *
     * <p>实现步骤：只有审批同意的采购单允许进入采购中状态，并记录业务流水。</p>
     */
    @Transactional
    public PurchaseOrderView startPurchase(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        if (order.getStatus() != PurchaseStatus.APPROVED) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "仅审批同意的采购单允许发起采购");
        }
        PurchaseStatus oldStatus = order.getStatus();
        order.setStatus(PurchaseStatus.PURCHASING);
        PurchaseOrderView view = toPurchaseOrderView(order);
        businessOperationLogService.record("PURCHASE_ORDER", view.id(), view.orderNo(), purchaseTitle(view), "START_PURCHASE", "发起采购",
                "采购单号" + view.orderNo() + "已发起采购。", purchaseStatusText(oldStatus), purchaseStatusText(view.status()), view);
        knowledgeIndexService.rebuildPurchaseOrder(order);
        auditLogService.record("START_PURCHASE", "purchaseOrderId=" + id, "SUCCESS",
                "采购管理发起采购单号" + view.orderNo() + "。");
        return view;
    }

    /**
     * 确认采购已收货。
     *
     * <p>实现步骤：只有采购中状态允许确认收货，确认后进入采购完成并记录业务流水。</p>
     */
    @Transactional
    public PurchaseOrderView receivePurchase(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        if (order.getStatus() != PurchaseStatus.PURCHASING) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "仅采购中的采购单允许确认已收货");
        }
        PurchaseStatus oldStatus = order.getStatus();
        order.setStatus(PurchaseStatus.PURCHASE_COMPLETED);
        PurchaseOrderView view = toPurchaseOrderView(order);
        businessOperationLogService.record("PURCHASE_ORDER", view.id(), view.orderNo(), purchaseTitle(view), "RECEIVE_PURCHASE", "采购已收货",
                "采购单号" + view.orderNo() + "已确认收货。", purchaseStatusText(oldStatus), purchaseStatusText(view.status()), view);
        knowledgeIndexService.rebuildPurchaseOrder(order);
        auditLogService.record("RECEIVE_PURCHASE", "purchaseOrderId=" + id, "SUCCESS",
                "采购管理确认采购单号" + view.orderNo() + "已收货。");
        return view;
    }

    /**
     * 取消采购。
     *
     * <p>实现步骤：采购未完成且未取消时均允许取消；保存取消类型和原因后进入取消状态，并记录业务流水。</p>
     */
    @Transactional
    public PurchaseOrderView cancelPurchase(Long id, PurchaseCancelRequest request) {
        PurchaseOrder order = purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        if (order.getStatus() == PurchaseStatus.PURCHASE_COMPLETED || order.getStatus() == PurchaseStatus.RECEIVED
                || order.getStatus() == PurchaseStatus.CANCELLED || order.getStatus() == PurchaseStatus.CLOSED) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "当前采购单状态不允许取消采购");
        }
        PurchaseStatus oldStatus = order.getStatus();
        order.setCancelType(request.cancelType());
        order.setCancelReason(request.cancelReason());
        order.setStatus(PurchaseStatus.CANCELLED);
        workflowService.cancelRunningWorkflow(PURCHASE_WORKFLOW_BUSINESS_TYPE, order.getId(), "采购单取消：" + request.cancelReason());
        PurchaseOrderView view = toPurchaseOrderView(order);
        businessOperationLogService.record("PURCHASE_ORDER", view.id(), view.orderNo(), purchaseTitle(view), "CANCEL_PURCHASE", "取消采购",
                "采购单号" + view.orderNo() + "已取消，取消类型为" + request.cancelType() + "，原因为" + request.cancelReason() + "。",
                purchaseStatusText(oldStatus), purchaseStatusText(view.status()), view);
        knowledgeIndexService.rebuildPurchaseOrder(order);
        auditLogService.record("CANCEL_PURCHASE", "purchaseOrderId=" + id + ", " + request, "SUCCESS",
                "采购管理取消采购单号" + view.orderNo() + "。");
        return view;
    }

    /**
     * 批量删除采购单。
     *
     * <p>实现步骤：
     * 1. 清理并去重前端传入的采购单 ID；
     * 2. 逐个读取采购单，任一 ID 不存在则整体失败；
     * 3. 删除采购单主表，采购明细通过 orphanRemoval 级联删除；
     * 4. 记录关键操作日志。</p>
     */
    @Transactional
    public void deletePurchaseOrders(List<Long> ids) {
        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> deleteIds = normalizedBatchIds(ids);
        // 变量说明：orderNos 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> orderNos = new java.util.ArrayList<>();
        for (Long id : deleteIds) {
            PurchaseOrder order = purchaseOrderRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在: " + id));
            CompanyScope.requireCurrentCompany(order.getOrganizationCode(), "采购单");
            orderNos.add(order.getOrderNo());
            businessOperationLogService.record("PURCHASE_ORDER", order.getId(), order.getOrderNo(), order.getSupplierName(), "DELETE", "删除采购单",
                    "删除采购单号" + order.getOrderNo() + "。", purchaseStatusText(order.getStatus()), "已删除", order.getOrderNo());
            attachmentService.deleteAllForBusiness(AttachmentBusinessType.PURCHASE_ORDER, id);
            knowledgeIndexService.deletePurchaseOrder(id);
            purchaseOrderRepository.delete(order);
        }
        auditLogService.record("BATCH_DELETE_PURCHASE_ORDERS", "purchaseOrderIds=" + deleteIds + ", orderNos=" + orderNos,
                "SUCCESS", "采购管理删除了采购单号: " + String.join("、", orderNos) + "。");
    }

    /**
     * 查询最近采购单列表。
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderView> listPurchaseOrders() {
        return listPurchaseOrders(null, null, null, null, null, null, null, null);
    }

    /**
     * 按字段查询采购单列表。
     *
     * <p>实现步骤：
     * 1. 未指定日期时默认查最近 50 条；
     * 2. 采购单号、供应商、创建人、备注使用包含匹配；
     * 3. 采购状态使用等值匹配，采购日期按起止日期范围过滤。</p>
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderView> listPurchaseOrders(
            LocalDate startDate,
            LocalDate endDate,
            String orderNo,
            String supplierName,
            String projectCode,
            PurchaseStatus status,
            String createdBy,
            String remark
    ) {
        /**
         * 采购单列表查询条件，先限定当前账套，再叠加日期、单号、供应商、项目、状态和创建人筛选。
         */
        var spec = CompanyScope.<PurchaseOrder>currentCompanySpec()
                .and(SearchSpecs.dateBetween("orderDate", startDate, endDate))
                .and(SearchSpecs.like("orderNo", orderNo))
                .and(SearchSpecs.like("supplierName", supplierName))
                .and(SearchSpecs.equal("projectCode", firstText(projectCode, null)))
                .and(SearchSpecs.equal("status", status))
                .and(SearchSpecs.like("createdBy", createdBy))
                .and(SearchSpecs.like("remark", remark));
        return purchaseOrderRepository.findAll(
                        spec,
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "modifyTime", "id"))
                ).stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 导出采购单列表。
     *
     * <p>实现步骤：
     * 1. 选中 ID 不为空时按选中采购单导出；
     * 2. 未选中时按当前搜索条件查询；
     * 3. 使用配置的最大导出行数截断结果；
     * 4. 只导出列表可见字段。</p>
     */
    @Transactional(readOnly = true)
    public byte[] exportPurchaseOrders(PurchaseOrderExportRequest request) {
        PurchaseOrderExportRequest exportRequest = request == null
                ? new PurchaseOrderExportRequest(null, null, null, null, null, null, null, null, null)
                : request;
        List<PurchaseOrderView> rows = hasSelectedIds(exportRequest.ids())
                ? selectedPurchaseRows(exportRequest.ids())
                : searchPurchaseRowsForExport(exportRequest);
        return excelExportService.export("采购管理", List.of(
                new ExcelColumn<>("采购单号", PurchaseOrderView::orderNo),
                new ExcelColumn<>("供应商", PurchaseOrderView::supplierName),
                new ExcelColumn<>("单据类型", PurchaseOrderView::documentType),
                new ExcelColumn<>("业务类型", PurchaseOrderView::businessType),
                new ExcelColumn<>("项目", PurchaseOrderView::projectName),
                new ExcelColumn<>("采购组织", PurchaseOrderView::purchaseOrganization),
                new ExcelColumn<>("采购部门", PurchaseOrderView::purchaseDepartment),
                new ExcelColumn<>("采购员", PurchaseOrderView::purchaserName),
                new ExcelColumn<>("结算组织", PurchaseOrderView::settlementOrganization),
                new ExcelColumn<>("采购日期", PurchaseOrderView::orderDate),
                new ExcelColumn<>("状态", row -> purchaseStatusText(row.status())),
                new ExcelColumn<>("总金额", PurchaseOrderView::totalAmount),
                new ExcelColumn<>("币种", row -> currencyDisplay(row.currencyCode(), row.currencyName())),
                new ExcelColumn<>("汇率", PurchaseOrderView::exchangeRateToCny),
                new ExcelColumn<>("总金额人民币", PurchaseOrderView::totalAmountCny),
                new ExcelColumn<>("创建人", PurchaseOrderView::createdBy)
        ), rows);
    }

    /**
     * 按选中 ID 查询采购单导出数据。
     */
    private List<PurchaseOrderView> selectedPurchaseRows(List<Long> ids) {
        // 变量说明：exportIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> exportIds = normalizedExportIds(ids);
        // 变量说明：orderMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<Long, Integer> orderMap = orderMap(exportIds);
        return purchaseOrderRepository.findAllById(exportIds).stream()
                .filter(row -> Objects.equals(CompanyScope.currentCompanyCode(), row.getOrganizationCode()))
                .sorted(Comparator.comparingInt(row -> orderMap.getOrDefault(row.getId(), Integer.MAX_VALUE)))
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 按搜索条件查询采购单导出数据。
     */
    private List<PurchaseOrderView> searchPurchaseRowsForExport(PurchaseOrderExportRequest request) {
        /**
         * 采购单导出查询条件，与列表筛选口径一致并限制当前所属公司。
         */
        var spec = CompanyScope.<PurchaseOrder>currentCompanySpec()
                .and(SearchSpecs.dateBetween("orderDate", request.startDate(), request.endDate()))
                .and(SearchSpecs.like("orderNo", request.orderNo()))
                .and(SearchSpecs.like("supplierName", request.supplierName()))
                .and(SearchSpecs.equal("projectCode", firstText(request.projectCode(), null)))
                .and(SearchSpecs.equal("status", request.status()))
                .and(SearchSpecs.like("createdBy", request.createdBy()))
                .and(SearchSpecs.like("remark", request.remark()));
        return purchaseOrderRepository.findAll(
                        spec,
                        PageRequest.of(0, exportProperties.maxRows(), Sort.by(Sort.Direction.DESC, "modifyTime", "id"))
                ).stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 查询采购单明细。
     */
    @Transactional(readOnly = true)
    public PurchaseOrderView getPurchaseOrder(Long id) {
        return purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .map(this::toPurchaseOrderView)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
    }

    /**
     * 查询采购单操作流水。
     */
    @Transactional(readOnly = true)
    public List<BusinessOperationLogView> listPurchaseOperationLogs(Long id) {
        purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        return businessOperationLogService.list("PURCHASE_ORDER", id);
    }

    /**
     * 分页查询采购单操作流水。
     *
     * <p>实现步骤：先确认采购单存在，再按操作时间范围和分页条件查询，前端右侧抽屉滚动加载。</p>
     */
    @Transactional(readOnly = true)
    public BusinessOperationLogPage pagePurchaseOperationLogs(Long id, OffsetDateTime startTime, OffsetDateTime endTime, int page, int size) {
        purchaseOrderRepository.findWithLinesByOrganizationCodeAndId(CompanyScope.currentCompanyCode(), id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "采购单不存在"));
        return businessOperationLogService.page("PURCHASE_ORDER", id, startTime, endTime, page, size);
    }

    /**
     * 新增物流单。
     *
     * <p>实现步骤：
     * 1. 按计划发运日期生成物流单号；
     * 2. 写入承运商、运单号、发货地行政区划和详址、目的地行政区划和详址等字段；
     * 3. 保存物流单；
     * 4. 记录关键操作日志。</p>
     */
    @Transactional
    public ShipmentView createShipment(ShipmentRequest request) {
        // 变量说明：shipment 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentOrder shipment = new ShipmentOrder();
        shipment.setOrganizationCode(CompanyScope.currentCompanyCode());
        shipment.setShipmentNo(nextShipmentNo(request.plannedShipDate()));
        applyShipment(shipment, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentView view = toShipmentView(shipmentOrderRepository.save(shipment));
        shipmentOperationLogRepository.save(createShipmentOperationLog(shipment, null, ShipmentStatus.CREATED, "新增物流单"));
        knowledgeIndexService.rebuildShipment(shipment);
        auditLogService.record("CREATE_SHIPMENT", request, "SUCCESS",
                "物流管理新增了物流单号" + view.shipmentNo() + "，承运商为" + view.carrierName() + "。");
        return view;
    }

    /**
     * 修改物流单。
     *
     * <p>实现步骤：
     * 1. 读取物流单；
     * 2. 只有草稿物流单允许修改；
     * 3. 更新物流基础字段；
     * 4. 记录审计日志。</p>
     */
    @Transactional
    public ShipmentView updateShipment(Long id, ShipmentRequest request) {
        ShipmentOrder shipment = shipmentOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "物流单不存在"));
        CompanyScope.requireCurrentCompany(shipment.getOrganizationCode(), "物流单");
        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "只有草稿状态的物流单允许编辑");
        }
        // 步骤3：状态不在此方法修改，状态流转统一走 confirmShipmentStatus。
        applyShipment(shipment, request);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentView view = toShipmentView(shipment);
        shipmentOperationLogRepository.save(createShipmentOperationLog(shipment, shipment.getStatus(), shipment.getStatus(), "修改物流基础信息"));
        knowledgeIndexService.rebuildShipment(shipment);
        auditLogService.record("UPDATE_SHIPMENT", "shipmentId=" + id + ", " + request, "SUCCESS",
                "物流管理修改了物流单号" + view.shipmentNo() + "。");
        return view;
    }

    /**
     * 确认物流状态并同步最新物流信息。
     *
     * <p>实现步骤：
     * 1. 读取物流单并记录原状态；
     * 2. 校验目标状态只能向后流转，已取消/已送达不再允许变更；
     * 3. 用用户确认后的实际物流信息覆盖主表，保证列表展示最新物流信息；
     * 4. 保存本次确认后的完整物流快照到操作流水；
     * 5. 记录关键操作日志。</p>
     */
    @Transactional
    public ShipmentView confirmShipmentStatus(Long id, ShipmentStatusConfirmRequest request) {
        ShipmentOrder shipment = shipmentOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "物流单不存在"));
        CompanyScope.requireCurrentCompany(shipment.getOrganizationCode(), "物流单");
        // 变量说明：oldStatus 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentStatus oldStatus = shipment.getStatus();
        // 变量说明：targetStatus 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentStatus targetStatus = request.status();
        validateShipmentStatusFlow(oldStatus, targetStatus);
        applyShipmentConfirmation(shipment, request);
        shipment.setStatus(targetStatus);
        if (targetStatus == ShipmentStatus.DISPATCHED && shipment.getActualShipDate() == null) {
            shipment.setActualShipDate(LocalDate.now());
        }
        if (targetStatus == ShipmentStatus.DELIVERED && shipment.getDeliveredDate() == null) {
            shipment.setDeliveredDate(LocalDate.now());
        }
        // 变量说明：operationLog 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentOperationLog operationLog = createShipmentOperationLog(shipment, oldStatus, targetStatus, request.operationRemark());
        shipmentOperationLogRepository.save(operationLog);
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentView view = toShipmentView(shipment);
        knowledgeIndexService.rebuildShipment(shipment);
        auditLogService.record("CHANGE_SHIPMENT_STATUS", "shipmentId=" + id + ", from=" + oldStatus + ", to=" + targetStatus + ", " + request,
                "SUCCESS", "物流管理确认物流单号" + view.shipmentNo() + "，状态从" + shipmentStatusText(oldStatus) + "更新为" + shipmentStatusText(targetStatus) + "。");
        return view;
    }

    /**
     * 查询物流操作流水。
     *
     * <p>实现步骤：先确认物流单存在，再按操作时间正序返回流水，前端按时间轴展示。</p>
     */
    @Transactional(readOnly = true)
    public List<ShipmentOperationLogView> listShipmentOperationLogs(Long shipmentId) {
        ShipmentOrder shipment = shipmentOrderRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "物流单不存在"));
        CompanyScope.requireCurrentCompany(shipment.getOrganizationCode(), "物流单");
        return shipmentOperationLogRepository.findByShipmentOrder_IdOrderByOperationTimeAscIdAsc(shipmentId)
                .stream()
                .map(this::toShipmentOperationLogView)
                .toList();
    }

    /**
     * 分页查询物流操作流水。
     *
     * <p>实现步骤：
     * 1. 校验物流单存在；
     * 2. 按操作时间范围过滤；
     * 3. 按操作时间倒序分页返回，前端右侧抽屉滚动加载。</p>
     */
    @Transactional(readOnly = true)
    public ShipmentOperationLogPage pageShipmentOperationLogs(Long shipmentId, OffsetDateTime startTime, OffsetDateTime endTime, int page, int size) {
        ShipmentOrder shipment = shipmentOrderRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "物流单不存在"));
        CompanyScope.requireCurrentCompany(shipment.getOrganizationCode(), "物流单");
        Specification<ShipmentOperationLog> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("shipmentOrder").get("id"), shipmentId);
        if (startTime != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("operationTime"), startTime));
        }
        if (endTime != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("operationTime"), endTime));
        }
        /**
         * 物流状态流水分页结果，按当前物流单和时间范围查询后倒序返回。
         */
        var result = shipmentOperationLogRepository.findAll(
                spec,
                PageRequest.of(safePage(page), safeSize(size), Sort.by(Sort.Direction.DESC, "operationTime", "id"))
        );
        return new ShipmentOperationLogPage(result.getContent().stream().map(this::toShipmentOperationLogView).toList(), result.getTotalElements());
    }

    /**
     * 批量删除物流单。
     *
     * <p>实现步骤：
     * 1. 清理并去重前端传入的物流单 ID；
     * 2. 逐个读取物流单，任一 ID 不存在则整体失败；
     * 3. 删除物流单主表；
     * 4. 记录关键操作日志。</p>
     */
    @Transactional
    public void deleteShipments(List<Long> ids) {
        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> deleteIds = normalizedBatchIds(ids);
        // 变量说明：shipmentNos 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> shipmentNos = new java.util.ArrayList<>();
        for (Long id : deleteIds) {
            ShipmentOrder shipment = shipmentOrderRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "物流单不存在: " + id));
            CompanyScope.requireCurrentCompany(shipment.getOrganizationCode(), "物流单");
            shipmentNos.add(shipment.getShipmentNo());
            attachmentService.deleteAllForBusiness(AttachmentBusinessType.SHIPMENT, id);
            shipmentOperationLogRepository.deleteByShipmentOrder_Id(id);
            knowledgeIndexService.deleteShipment(id);
            shipmentOrderRepository.delete(shipment);
        }
        auditLogService.record("BATCH_DELETE_SHIPMENTS", "shipmentIds=" + deleteIds + ", shipmentNos=" + shipmentNos,
                "SUCCESS", "物流管理删除了物流单号: " + String.join("、", shipmentNos) + "。");
    }

    /**
     * 查询最近物流单列表。
     */
    @Transactional(readOnly = true)
    public List<ShipmentView> listShipments() {
        return listShipments(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 按字段查询物流单列表。
     *
     * <p>实现步骤：
     * 1. 未指定日期时默认查最近 50 条；
     * 2. 物流单号、关联单号、承运商、运单号、发货详址、目的详址使用包含匹配；
     * 3. 行政区划支持多选，并按编码前缀右 like 匹配下级区划；
     * 4. 物流状态使用等值匹配，计划发运日期按起止日期范围过滤。</p>
     */
    @Transactional(readOnly = true)
    public List<ShipmentView> listShipments(
            LocalDate startDate,
            LocalDate endDate,
            String shipmentNo,
            String relatedOrderNo,
            String projectCode,
            String carrierName,
            String trackingNo,
            String originDivisionCodes,
            String destinationDivisionCodes,
            String origin,
            String destination,
            ShipmentStatus status
    ) {
        /**
         * 物流单列表查询条件，先限定当前账套，再叠加日期、单号、项目、承运、区划、地址和状态筛选。
         */
        var spec = CompanyScope.<ShipmentOrder>currentCompanySpec()
                .and(SearchSpecs.dateBetween("plannedShipDate", startDate, endDate))
                .and(SearchSpecs.like("shipmentNo", shipmentNo))
                .and(SearchSpecs.like("relatedOrderNo", relatedOrderNo))
                .and(SearchSpecs.equal("projectCode", firstText(projectCode, null)))
                .and(SearchSpecs.like("carrierName", carrierName))
                .and(SearchSpecs.like("trackingNo", trackingNo))
                .and(divisionPrefixIn("originDivisionCode", originDivisionCodes))
                .and(divisionPrefixIn("destinationDivisionCode", destinationDivisionCodes))
                .and(SearchSpecs.like("origin", origin))
                .and(SearchSpecs.like("destination", destination))
                .and(SearchSpecs.equal("status", status));
        return shipmentOrderRepository.findAll(
                        spec,
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "modifyTime", "id"))
                ).stream()
                .map(this::toShipmentView)
                .toList();
    }

    /**
     * 导出物流单列表。
     *
     * <p>实现步骤：
     * 1. 选中 ID 不为空时按选中物流单导出；
     * 2. 未选中时按当前搜索条件查询；
     * 3. 使用配置的最大导出行数截断结果；
     * 4. 只导出列表可见字段。</p>
     */
    @Transactional(readOnly = true)
    public byte[] exportShipments(ShipmentExportRequest request) {
        ShipmentExportRequest exportRequest = request == null
                ? new ShipmentExportRequest(null, null, null, null, null, null, null, null, null, null, null, null, null)
                : request;
        List<ShipmentView> rows = hasSelectedIds(exportRequest.ids())
                ? selectedShipmentRows(exportRequest.ids())
                : searchShipmentRowsForExport(exportRequest);
        return excelExportService.export("物流管理", List.of(
                new ExcelColumn<>("物流单号", ShipmentView::shipmentNo),
                new ExcelColumn<>("关联单号", ShipmentView::relatedOrderNo),
                new ExcelColumn<>("单据类型", ShipmentView::documentType),
                new ExcelColumn<>("项目", ShipmentView::projectName),
                new ExcelColumn<>("运输方式", ShipmentView::transportMode),
                new ExcelColumn<>("发运组织", ShipmentView::shippingOrganization),
                new ExcelColumn<>("收货组织", ShipmentView::receivingOrganization),
                new ExcelColumn<>("承运商", ShipmentView::carrierName),
                new ExcelColumn<>("运单号", ShipmentView::trackingNo),
                new ExcelColumn<>("司机", ShipmentView::driverName),
                new ExcelColumn<>("司机电话", ShipmentView::driverPhone),
                new ExcelColumn<>("车牌号", ShipmentView::vehicleNo),
                new ExcelColumn<>("发货地行政区划", ShipmentView::originDivisionName),
                new ExcelColumn<>("发货地详址", ShipmentView::origin),
                new ExcelColumn<>("目的地行政区划", ShipmentView::destinationDivisionName),
                new ExcelColumn<>("目的地详址", ShipmentView::destination),
                new ExcelColumn<>("状态", row -> shipmentStatusText(row.status()))
        ), rows);
    }

    /**
     * 按选中 ID 查询物流单导出数据。
     */
    private List<ShipmentView> selectedShipmentRows(List<Long> ids) {
        // 变量说明：exportIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> exportIds = normalizedExportIds(ids);
        // 变量说明：orderMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<Long, Integer> orderMap = orderMap(exportIds);
        return shipmentOrderRepository.findAllById(exportIds).stream()
                .filter(row -> Objects.equals(CompanyScope.currentCompanyCode(), row.getOrganizationCode()))
                .sorted(Comparator.comparingInt(row -> orderMap.getOrDefault(row.getId(), Integer.MAX_VALUE)))
                .map(this::toShipmentView)
                .toList();
    }

    /**
     * 按搜索条件查询物流单导出数据。
     */
    private List<ShipmentView> searchShipmentRowsForExport(ShipmentExportRequest request) {
        /**
         * 物流单导出查询条件，与列表筛选口径一致并限制当前所属公司。
         */
        var spec = CompanyScope.<ShipmentOrder>currentCompanySpec()
                .and(SearchSpecs.dateBetween("plannedShipDate", request.startDate(), request.endDate()))
                .and(SearchSpecs.like("shipmentNo", request.shipmentNo()))
                .and(SearchSpecs.like("relatedOrderNo", request.relatedOrderNo()))
                .and(SearchSpecs.equal("projectCode", firstText(request.projectCode(), null)))
                .and(SearchSpecs.like("carrierName", request.carrierName()))
                .and(SearchSpecs.like("trackingNo", request.trackingNo()))
                .and(divisionPrefixIn("originDivisionCode", request.originDivisionCodes()))
                .and(divisionPrefixIn("destinationDivisionCode", request.destinationDivisionCodes()))
                .and(SearchSpecs.like("origin", request.origin()))
                .and(SearchSpecs.like("destination", request.destination()))
                .and(SearchSpecs.equal("status", request.status()));
        return shipmentOrderRepository.findAll(
                        spec,
                        PageRequest.of(0, exportProperties.maxRows(), Sort.by(Sort.Direction.DESC, "modifyTime", "id"))
                ).stream()
                .map(this::toShipmentView)
                .toList();
    }

    /**
     * 替换采购明细并重新计算采购总金额。
     *
     * <p>实现步骤：
     * 1. 清空旧明细；
     * 2. 逐行统一数量和单价格式；
     * 3. 解析每条明细自己的币种和汇率快照；
     * 4. 计算行原币金额和折人民币金额；
     * 5. 创建新明细并加入采购单；
     * 6. 汇总总金额并写回主表。</p>
     */
    private void replacePurchaseLines(PurchaseOrder order, PurchaseOrderRequest request) {
        // 步骤1：旧明细通过 orphanRemoval 删除，避免残留。
        order.getLines().clear();
        // 变量说明：total 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal total = BigDecimal.ZERO;
        // 变量说明：totalCny 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal totalCny = BigDecimal.ZERO;
        // 变量说明：firstCurrency 保存当前步骤计算、查询或转换得到的中间结果。
        CurrencySnapshot firstCurrency = null;
        // 变量说明：multiCurrency 保存当前步骤计算、查询或转换得到的中间结果。
        boolean multiCurrency = false;
        // 变量说明：lineNo 保存当前步骤计算、查询或转换得到的中间结果。
        int lineNo = 1;
        for (var lineRequest : request.lines()) {
            // 步骤2：数量保留 4 位，单价和金额保留 8 位，满足多币种和高精度采购核算。
            BigDecimal quantity = lineRequest.quantity().setScale(4, RoundingMode.HALF_UP);
            // 变量说明：unitPrice 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal unitPrice = lineRequest.unitPrice().setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
            // 变量说明：amount 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal amount = quantity.multiply(unitPrice).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
            // 变量说明：taxRate 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal taxRate = defaultMoney(lineRequest.taxRate(), BigDecimal.ZERO);
            // 变量说明：taxAmount 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal taxAmount = amount.multiply(taxRate).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
            // 变量说明：amountWithTax 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal amountWithTax = amount.add(taxAmount).setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP);
            // 步骤3：每条采购明细独立保存币种和汇率，避免明细币种不一致时丢失原始币种单位。
            CurrencySnapshot lineCurrency = resolveLineCurrency(
                    lineRequest.currencyCode(),
                    lineRequest.currencyName(),
                    lineRequest.exchangeRateToCny(),
                    request.currencyCode(),
                    request.currencyName(),
                    request.exchangeRateToCny()
            );
            if (firstCurrency == null) {
                firstCurrency = lineCurrency;
            } else if (isDifferentCurrencySnapshot(firstCurrency, lineCurrency)) {
                multiCurrency = true;
            }
            // 步骤4：人民币金额按明细汇率快照计算并保留 8 位小数。
            BigDecimal unitPriceCny = currencyService.toCnyAmount(unitPrice, lineCurrency);
            // 变量说明：amountCny 保存当前步骤计算、查询或转换得到的中间结果。
            BigDecimal amountCny = currencyService.toCnyAmount(amount, lineCurrency);
            // 步骤5：明细行号按请求顺序生成。
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrder(order);
            line.setLineNo(lineNo++);
            line.setItemCode(lineRequest.itemCode());
            line.setItemName(lineRequest.itemName());
            line.setSpecification(lineRequest.specification());
            line.setUnitName(lineRequest.unitName());
            line.setQuantity(quantity);
            line.setUnitPrice(unitPrice);
            line.setAmount(amount);
            line.setTaxRate(taxRate);
            line.setTaxAmount(taxAmount);
            line.setAmountWithTax(amountWithTax);
            line.setPlannedArrivalDate(lineRequest.plannedArrivalDate());
            line.setReceiveWarehouse(lineRequest.receiveWarehouse());
            line.setCurrencyCode(lineCurrency.currencyCode());
            line.setCurrencyName(lineCurrency.currencyName());
            line.setExchangeRateToCny(lineCurrency.exchangeRateToCny());
            line.setUnitPriceCny(unitPriceCny);
            line.setAmountCny(amountCny);
            order.getLines().add(line);
            total = total.add(amount);
            totalCny = totalCny.add(amountCny);
        }
        // 步骤6：主表总金额用于列表展示和经营指标统计，币种摘要用于提示是否为多币种采购单。
        applyPurchaseCurrencySummary(order, firstCurrency, multiCurrency);
        order.setTotalAmount(total);
        order.setTotalAmountCny(totalCny);
    }

    /**
     * 将采购单头金蝶式采集字段写入采购主表。
     *
     * <p>实现步骤：
     * 1. 单据类型和业务类型为空时使用标准采购默认值；
     * 2. 写入采购组织、采购部门、采购员等组织维度字段；
     * 3. 写入结算组织、付款条件、结算方式和交货条件；
     * 4. 写入来源单据类型和来源单据编号，支持后续业务追溯。</p>
     */
    private void applyPurchaseHeader(PurchaseOrder order, PurchaseOrderRequest request) {
        order.setDocumentType(defaultText(request.documentType(), "标准采购订单"));
        order.setBusinessType(defaultText(request.businessType(), "标准采购"));
        order.setProjectCode(firstText(request.projectCode(), null));
        order.setProjectName(firstText(request.projectName(), null));
        order.setPurchaseOrganization(request.purchaseOrganization());
        order.setPurchaseDepartment(request.purchaseDepartment());
        order.setPurchaserName(request.purchaserName());
        order.setSettlementOrganization(request.settlementOrganization());
        order.setPaymentTerms(request.paymentTerms());
        order.setSettlementMethod(request.settlementMethod());
        order.setDeliveryTerms(request.deliveryTerms());
        order.setSourceBillType(request.sourceBillType());
        order.setSourceBillNo(request.sourceBillNo());
    }

    /**
     * 解析采购明细币种快照。
     *
     * <p>实现步骤：优先使用明细自己的币种和汇率；如果历史调用没有传明细币种，则回退到采购单主表币种。</p>
     */
    private CurrencySnapshot resolveLineCurrency(
            String lineCurrencyCode,
            String lineCurrencyName,
            BigDecimal lineExchangeRate,
            String fallbackCurrencyCode,
            String fallbackCurrencyName,
            BigDecimal fallbackExchangeRate
    ) {
        // 变量说明：currencyCode 保存当前步骤计算、查询或转换得到的中间结果。
        String currencyCode = firstText(lineCurrencyCode, fallbackCurrencyCode);
        // 变量说明：currencyName 保存当前步骤计算、查询或转换得到的中间结果。
        String currencyName = firstText(lineCurrencyName, fallbackCurrencyName);
        // 变量说明：exchangeRate 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal exchangeRate = lineExchangeRate == null ? fallbackExchangeRate : lineExchangeRate;
        return currencyService.snapshot(currencyCode, currencyName, exchangeRate);
    }

    /**
     * 将采购明细币种摘要写入采购单主表。
     *
     * <p>实现步骤：单币种采购单保存该币种快照；多币种采购单保存 MULTI/多币种，提醒用户查看明细币种。</p>
     */
    private void applyPurchaseCurrencySummary(PurchaseOrder order, CurrencySnapshot firstCurrency, boolean multiCurrency) {
        if (firstCurrency == null) {
            // 变量说明：defaultCurrency 保存当前步骤计算、查询或转换得到的中间结果。
            CurrencySnapshot defaultCurrency = currencyService.snapshot(null, null, null);
            order.setCurrencyCode(defaultCurrency.currencyCode());
            order.setCurrencyName(defaultCurrency.currencyName());
            order.setExchangeRateToCny(defaultCurrency.exchangeRateToCny());
            return;
        }
        if (multiCurrency) {
            order.setCurrencyCode("MULTI");
            order.setCurrencyName("多币种/多汇率");
            order.setExchangeRateToCny(BigDecimal.ONE.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP));
            return;
        }
        order.setCurrencyCode(firstCurrency.currencyCode());
        order.setCurrencyName(firstCurrency.currencyName());
        order.setExchangeRateToCny(firstCurrency.exchangeRateToCny());
    }

    /**
     * 判断两个币种快照是否存在币种或汇率差异。
     *
     * <p>实现目的：同一采购单可能使用同一外币但不同明细采用不同汇率，此时主表不应展示成单一汇率。</p>
     */
    private boolean isDifferentCurrencySnapshot(CurrencySnapshot left, CurrencySnapshot right) {
        return !Objects.equals(left.currencyCode(), right.currencyCode())
                || left.exchangeRateToCny().compareTo(right.exchangeRateToCny()) != 0;
    }

    /**
     * 将物流请求字段写入物流实体。
     *
     * <p>状态字段不在这里处理，避免普通修改绕过状态流转审计。</p>
     */
    private void applyShipment(ShipmentOrder shipment, ShipmentRequest request) {
        shipment.setRelatedOrderNo(request.relatedOrderNo());
        shipment.setDocumentType(defaultText(request.documentType(), "采购发运"));
        shipment.setProjectCode(firstText(request.projectCode(), null));
        shipment.setProjectName(firstText(request.projectName(), null));
        shipment.setTransportMode(request.transportMode());
        shipment.setShippingOrganization(request.shippingOrganization());
        shipment.setReceivingOrganization(request.receivingOrganization());
        shipment.setCarrierName(request.carrierName());
        shipment.setTrackingNo(request.trackingNo());
        shipment.setDriverName(request.driverName());
        shipment.setDriverPhone(request.driverPhone());
        shipment.setVehicleNo(request.vehicleNo());
        shipment.setOriginDivisionCode(normalizeCascadePath(request.originDivisionCode()));
        shipment.setOriginDivisionName(normalizeCascadePath(request.originDivisionName()));
        shipment.setDestinationDivisionCode(normalizeCascadePath(request.destinationDivisionCode()));
        shipment.setDestinationDivisionName(normalizeCascadePath(request.destinationDivisionName()));
        shipment.setOrigin(request.origin());
        shipment.setDestination(request.destination());
        shipment.setPlannedShipDate(request.plannedShipDate());
        shipment.setRemark(request.remark());
    }

    /**
     * 将物流状态确认请求写入物流主表。
     *
     * <p>实现步骤：覆盖计划和实际可能不一致的关键物流字段，主表始终保存最新确认内容。</p>
     */
    private void applyShipmentConfirmation(ShipmentOrder shipment, ShipmentStatusConfirmRequest request) {
        shipment.setRelatedOrderNo(request.relatedOrderNo());
        shipment.setDocumentType(defaultText(request.documentType(), "采购发运"));
        shipment.setProjectCode(firstText(request.projectCode(), null));
        shipment.setProjectName(firstText(request.projectName(), null));
        shipment.setTransportMode(request.transportMode());
        shipment.setShippingOrganization(request.shippingOrganization());
        shipment.setReceivingOrganization(request.receivingOrganization());
        shipment.setCarrierName(request.carrierName());
        shipment.setTrackingNo(request.trackingNo());
        shipment.setDriverName(request.driverName());
        shipment.setDriverPhone(request.driverPhone());
        shipment.setVehicleNo(request.vehicleNo());
        shipment.setOriginDivisionCode(normalizeCascadePath(request.originDivisionCode()));
        shipment.setOriginDivisionName(normalizeCascadePath(request.originDivisionName()));
        shipment.setDestinationDivisionCode(normalizeCascadePath(request.destinationDivisionCode()));
        shipment.setDestinationDivisionName(normalizeCascadePath(request.destinationDivisionName()));
        shipment.setOrigin(request.origin());
        shipment.setDestination(request.destination());
        shipment.setPlannedShipDate(request.plannedShipDate());
        shipment.setActualShipDate(request.actualShipDate());
        shipment.setDeliveredDate(request.deliveredDate());
        shipment.setRemark(request.remark());
    }

    /**
     * 校验物流状态流转顺序。
     *
     * <p>CREATED、DISPATCHED、IN_TRANSIT、DELIVERED 按顺序前进；CANCELLED 作为终止状态，只允许从非终止状态进入。</p>
     */
    private void validateShipmentStatusFlow(ShipmentStatus currentStatus, ShipmentStatus targetStatus) {
        if (targetStatus == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "物流状态不能为空");
        }
        if (currentStatus == ShipmentStatus.DELIVERED || currentStatus == ShipmentStatus.CANCELLED) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "已送达或已取消物流单不允许再次确认状态");
        }
        if (shipmentStatusOrder(targetStatus) <= shipmentStatusOrder(currentStatus)) {
            throw new BusinessException(ResponseCode.MODIFY_FORBIDDEN, "物流状态只能向后流转，不能改回之前状态");
        }
    }

    /**
     * 返回物流状态顺序号。
     */
    private int shipmentStatusOrder(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> 10;
            case DISPATCHED -> 20;
            case IN_TRANSIT -> 30;
            case DELIVERED -> 40;
            case CANCELLED -> 50;
        };
    }

    /**
     * 创建物流操作流水实体。
     *
     * <p>实现步骤：
     * 1. 读取当前登录人，作为本次流水的操作人；
     * 2. 复制物流主表的完整业务字段，形成状态确认时点的快照；
     * 3. 写入状态变更前后、操作说明和操作时间；
     * 4. 返回待保存的流水实体，调用方负责落库。</p>
     */
    private ShipmentOperationLog createShipmentOperationLog(
            ShipmentOrder shipment,
            ShipmentStatus oldStatus,
            ShipmentStatus targetStatus,
            String operationRemark
    ) {
        // 步骤1：当前登录人用于流水时间轴展示，不能从前端传入以免被篡改。
        CurrentUser currentUser = SecurityUtils.currentUser();
        // 变量说明：operationLog 保存当前步骤计算、查询或转换得到的中间结果。
        ShipmentOperationLog operationLog = new ShipmentOperationLog();
        // 步骤2：复制物流主表字段，确保查看流水能看到当时的单据类型、组织、司机、车牌和地址信息。
        operationLog.setShipmentOrder(shipment);
        operationLog.setShipmentNo(shipment.getShipmentNo());
        operationLog.setFromStatus(oldStatus);
        operationLog.setToStatus(targetStatus);
        operationLog.setRelatedOrderNo(shipment.getRelatedOrderNo());
        operationLog.setDocumentType(shipment.getDocumentType());
        operationLog.setProjectCode(shipment.getProjectCode());
        operationLog.setProjectName(shipment.getProjectName());
        operationLog.setTransportMode(shipment.getTransportMode());
        operationLog.setShippingOrganization(shipment.getShippingOrganization());
        operationLog.setReceivingOrganization(shipment.getReceivingOrganization());
        operationLog.setCarrierName(shipment.getCarrierName());
        operationLog.setTrackingNo(shipment.getTrackingNo());
        operationLog.setDriverName(shipment.getDriverName());
        operationLog.setDriverPhone(shipment.getDriverPhone());
        operationLog.setVehicleNo(shipment.getVehicleNo());
        operationLog.setOriginDivisionCode(shipment.getOriginDivisionCode());
        operationLog.setOriginDivisionName(shipment.getOriginDivisionName());
        operationLog.setDestinationDivisionCode(shipment.getDestinationDivisionCode());
        operationLog.setDestinationDivisionName(shipment.getDestinationDivisionName());
        operationLog.setOrigin(shipment.getOrigin());
        operationLog.setDestination(shipment.getDestination());
        operationLog.setPlannedShipDate(shipment.getPlannedShipDate());
        operationLog.setActualShipDate(shipment.getActualShipDate());
        operationLog.setDeliveredDate(shipment.getDeliveredDate());
        operationLog.setRemark(shipment.getRemark());
        operationLog.setOperationRemark(operationRemark);
        // 步骤3：状态确认说明和操作人信息独立保存，后续即使人员资料变化也不影响历史流水。
        operationLog.setOperatorId(currentUser.id());
        operationLog.setOperatorUsername(currentUser.username());
        operationLog.setOperatorName(currentUser.realName());
        operationLog.setOperationTime(OffsetDateTime.now());
        return operationLog;
    }

    /**
     * 按采购日期生成采购单号。
     *
     * <p>格式为 POyyyyMMdd0001，若当天已有同号则递增序号。</p>
     */
    private String nextPurchaseOrderNo(LocalDate date) {
        String prefix = "PO" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String companyCode = CompanyScope.currentCompanyCode();
        return numberSequenceService.next(
                "PURCHASE_ORDER",
                companyCode,
                prefix,
                () -> purchaseOrderRepository.findFirstByOrganizationCodeAndOrderNoStartingWithOrderByOrderNoDesc(companyCode, prefix)
                        .map(order -> documentNoSequence(order.getOrderNo(), prefix) + 1)
                        .orElse(1),
                orderNo -> purchaseOrderRepository.existsByOrganizationCodeAndOrderNo(companyCode, orderNo)
        );
    }

    /**
     * 按计划发运日期生成物流单号。
     *
     * <p>格式为 SOyyyyMMdd0001，若当天已有同号则递增序号。</p>
     */
    private String nextShipmentNo(LocalDate date) {
        String prefix = "SO" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String companyCode = CompanyScope.currentCompanyCode();
        return numberSequenceService.next(
                "SHIPMENT_ORDER",
                companyCode,
                prefix,
                () -> shipmentOrderRepository.findFirstByOrganizationCodeAndShipmentNoStartingWithOrderByShipmentNoDesc(companyCode, prefix)
                        .map(shipment -> documentNoSequence(shipment.getShipmentNo(), prefix) + 1)
                        .orElse(1),
                shipmentNo -> shipmentOrderRepository.existsByOrganizationCodeAndShipmentNo(companyCode, shipmentNo)
        );
    }

    /**
     * 提取业务单号末尾序号。
     */
    private int documentNoSequence(String documentNo, String prefix) {
        if (documentNo == null || !documentNo.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(documentNo.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * 判断是否存在有效选中 ID。
     */
    private boolean hasSelectedIds(List<Long> ids) {
        return ids != null && ids.stream().anyMatch(Objects::nonNull);
    }

    /**
     * 清理导出 ID，并按配置最大行数截断。
     */
    private List<Long> normalizedExportIds(List<Long> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(exportProperties.maxRows())
                .toList();
    }

    /**
     * 清理批量删除 ID。
     */
    private List<Long> normalizedBatchIds(List<Long> ids) {
        if (ids == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        List<Long> normalizedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "请选择需要删除的数据");
        }
        return normalizedIds;
    }

    /**
     * 记录选中 ID 的顺序，保证导出顺序与前端选择顺序一致。
     */
    private Map<Long, Integer> orderMap(List<Long> ids) {
        // 变量说明：orderMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<Long, Integer> orderMap = new LinkedHashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            orderMap.put(ids.get(index), index);
        }
        return orderMap;
    }

    /**
     * 采购状态中文化，用于 Excel 导出。
     */
    private String purchaseStatusText(PurchaseStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case IN_APPROVAL -> "审批中";
            case APPROVAL_REJECTED -> "已审批【不同意】";
            case SUBMITTED -> "已提交";
            case APPROVED -> "已审批【同意】";
            case PURCHASING -> "采购中";
            case PURCHASE_COMPLETED -> "采购完成";
            case RECEIVED -> "已收货";
            case CLOSED -> "已关闭";
            case CANCELLED -> "取消采购";
        };
    }

    /**
     * 校验采购单是否满足提交审批条件。
     *
     * <p>实现步骤：
     * 1. 校验供应商、项目、采购日期和明细集合；
     * 2. 遍历采购明细，确认每行物料、数量、单价和汇率完整；
     * 3. 任一字段不满足时抛出业务异常，事务回滚，采购状态不变化。</p>
     */
    private void validatePurchaseReadyForApproval(PurchaseOrder order) {
        if (order.getSupplierName() == null || order.getSupplierName().isBlank()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "供应商不能为空");
        }
        if (order.getProjectCode() == null || order.getProjectCode().isBlank()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "项目不能为空");
        }
        if (order.getOrderDate() == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "采购日期不能为空");
        }
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "采购明细不能为空");
        }
        for (PurchaseOrderLine line : order.getLines()) {
            if (line.getItemCode() == null || line.getItemCode().isBlank() || line.getItemName() == null || line.getItemName().isBlank()) {
                throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "采购明细物料不能为空");
            }
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "采购明细数量必须大于0");
            }
            if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "采购明细单价必须大于0");
            }
            if (line.getExchangeRateToCny() == null || line.getExchangeRateToCny().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "采购明细汇率必须大于0");
            }
        }
    }

    /**
     * 组装币种显示文本，用于 Excel 导出。
     */
    private String currencyDisplay(String currencyCode, String currencyName) {
        if (currencyName == null || currencyName.isBlank()) {
            return currencyCode == null ? "" : currencyCode;
        }
        return currencyName + "(" + currencyCode + ")";
    }

    /**
     * 生成采购流水标题。
     */
    private String purchaseTitle(PurchaseOrderView view) {
        return view.orderNo() + " " + view.supplierName();
    }

    /**
     * 物流状态中文化，用于 Excel 导出。
     */
    private String shipmentStatusText(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> "草稿";
            case DISPATCHED -> "已发送";
            case IN_TRANSIT -> "运输中";
            case DELIVERED -> "已送达";
            case CANCELLED -> "已取消";
        };
    }

    /**
     * 执行 toPurchaseOrderView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private PurchaseOrderView toPurchaseOrderView(PurchaseOrder order) {
        return new PurchaseOrderView(
                order.getId(),
                order.getOrderNo(),
                order.getSupplierName(),
                order.getDocumentType(),
                order.getBusinessType(),
                order.getProjectCode(),
                order.getProjectName(),
                order.getPurchaseOrganization(),
                order.getPurchaseDepartment(),
                order.getPurchaserName(),
                order.getSettlementOrganization(),
                order.getPaymentTerms(),
                order.getSettlementMethod(),
                order.getDeliveryTerms(),
                order.getSourceBillType(),
                order.getSourceBillNo(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotalAmount(),
                defaultString(order.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                defaultString(order.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                defaultRate(order.getExchangeRateToCny()),
                defaultMoney(order.getTotalAmountCny(), order.getTotalAmount()),
                order.getCreatedBy(),
                order.getRemark(),
                order.getCancelType(),
                order.getCancelReason(),
                order.getVoucherId(),
                order.getVoucherNo(),
                workflowService.latestBusinessWorkflow(PURCHASE_WORKFLOW_BUSINESS_TYPE, order.getId()),
                order.getLines().stream().map(this::toPurchaseLineView).toList()
        );
    }

    /**
     * 执行 toPurchaseLineView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private PurchaseLineView toPurchaseLineView(PurchaseOrderLine line) {
        return new PurchaseLineView(
                line.getId(),
                line.getLineNo(),
                line.getItemCode(),
                line.getItemName(),
                line.getSpecification(),
                line.getUnitName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getAmount(),
                defaultMoney(line.getTaxRate(), BigDecimal.ZERO),
                defaultMoney(line.getTaxAmount(), BigDecimal.ZERO),
                defaultMoney(line.getAmountWithTax(), line.getAmount()),
                line.getPlannedArrivalDate(),
                line.getReceiveWarehouse(),
                defaultString(line.getCurrencyCode(), CurrencyService.DEFAULT_CURRENCY_CODE),
                defaultString(line.getCurrencyName(), CurrencyService.DEFAULT_CURRENCY_NAME),
                defaultRate(line.getExchangeRateToCny()),
                defaultMoney(line.getUnitPriceCny(), line.getUnitPrice()),
                defaultMoney(line.getAmountCny(), line.getAmount())
        );
    }

    /**
     * 空字符串时返回默认文本。
     */
    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 执行 defaultText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 将前端下拉框清空后的空字符串转为空值。
     *
     * <p>实现步骤：空文本返回 null；非空文本去除首尾空格后参与等值查询。</p>
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 返回第一个非空白文本。
     *
     * <p>实现目的：明细币种为空时兼容旧版主表币种字段，避免历史请求升级后无法保存。</p>
     */
    private String firstText(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback;
    }

    /**
     * 构建行政区划多选右 like 查询条件。
     *
     * <p>实现步骤：
     * 1. 前端把多选区划路径用逗号拼接传入，例如 110000/110100；
     * 2. 服务端去重、去空后使用路径前缀右 like 查询；
     * 3. 同时兼容旧版只保存末级六位编码的数据；
     * 4. 多个区划之间使用 OR，满足任一上级或本级区划即可命中。</p>
     */
    private org.springframework.data.jpa.domain.Specification<ShipmentOrder> divisionPrefixIn(String field, String codes) {
        List<String> values = Arrays.stream((codes == null ? "" : codes).split(","))
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
        if (values.isEmpty()) {
            return SearchSpecs.unrestricted();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(values.stream()
                .flatMap(value -> divisionQueryPatterns(value).stream())
                .distinct()
                .map(pattern -> criteriaBuilder.like(root.get(field), pattern))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    /**
     * 把行政区划编码或路径转换为“包含下级”的右 like 模式。
     *
     * <p>新数据保存完整路径，乡镇街道使用 9 位统计区划代码并通过完整路径命中下级；旧数据保存六位叶子编码，
     * 额外生成按省市截断的兼容模式，避免升级后旧单据查询不到。</p>
     */
    private List<String> divisionQueryPatterns(String codePath) {
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = normalizeCascadePath(codePath);
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }
        // 变量说明：leafCode 保存当前步骤计算、查询或转换得到的中间结果。
        String leafCode = cascadeLeaf(normalized);
        // 变量说明：patterns 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> patterns = new java.util.ArrayList<>();
        patterns.add(normalized);
        patterns.add(normalized + "/%");
        if (ADMIN_DIVISION_CODE_PATTERN.matcher(leafCode).matches()) {
            patterns.add(divisionLegacyPrefix(leafCode) + "%");
        }
        return patterns;
    }

    /**
     * 兼容旧版单级行政区划编码的右 like 前缀。
     */
    private String divisionLegacyPrefix(String code) {
        if (code.endsWith("0000")) {
            return code.substring(0, 2);
        }
        if (code.endsWith("00")) {
            return code.substring(0, 4);
        }
        return code;
    }

    /**
     * 标准化级联路径。
     *
     * <p>实现步骤：把前端级联数组拼接得到的 `110000/110100/110102` 去除多余斜杠和空白，
     * 后端后续存储、搜索、展示都使用同一种路径格式。</p>
     */
    private String normalizeCascadePath(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(value.split("/"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }

    /**
     * 获取级联路径最后一级，用于兼容旧行政区划编码搜索。
     */
    private String cascadeLeaf(String value) {
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = normalizeCascadePath(value);
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        // 变量说明：index 保存当前步骤计算、查询或转换得到的中间结果。
        int index = normalized.lastIndexOf('/');
        return index < 0 ? normalized : normalized.substring(index + 1);
    }

    /**
     * 空汇率按人民币 1 处理，兼容升级前历史数据。
     */
    private BigDecimal defaultRate(BigDecimal value) {
        return value == null ? BigDecimal.ONE.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP) : value;
    }

    /**
     * 空人民币快照按原币金额回退，兼容升级前历史数据。
     */
    private BigDecimal defaultMoney(BigDecimal value, BigDecimal fallback) {
        return value == null ? (fallback == null ? BigDecimal.ZERO : fallback.setScale(CurrencyService.MONEY_SCALE, RoundingMode.HALF_UP)) : value;
    }

    /**
     * 执行 safePage 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int safePage(int page) {
        return Math.max(page, 0);
    }

    /**
     * 执行 safeSize 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int safeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    /**
     * 执行 toShipmentView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private ShipmentView toShipmentView(ShipmentOrder shipment) {
        return new ShipmentView(
                shipment.getId(),
                shipment.getShipmentNo(),
                shipment.getRelatedOrderNo(),
                shipment.getDocumentType(),
                shipment.getProjectCode(),
                shipment.getProjectName(),
                shipment.getTransportMode(),
                shipment.getShippingOrganization(),
                shipment.getReceivingOrganization(),
                shipment.getCarrierName(),
                shipment.getTrackingNo(),
                shipment.getDriverName(),
                shipment.getDriverPhone(),
                shipment.getVehicleNo(),
                shipment.getOriginDivisionCode(),
                shipment.getOriginDivisionName(),
                shipment.getDestinationDivisionCode(),
                shipment.getDestinationDivisionName(),
                shipment.getOrigin(),
                shipment.getDestination(),
                shipment.getPlannedShipDate(),
                shipment.getActualShipDate(),
                shipment.getDeliveredDate(),
                shipment.getStatus(),
                shipment.getRemark()
        );
    }

    /**
     * 执行 toShipmentOperationLogView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private ShipmentOperationLogView toShipmentOperationLogView(ShipmentOperationLog operationLog) {
        return new ShipmentOperationLogView(
                operationLog.getId(),
                operationLog.getShipmentNo(),
                operationLog.getFromStatus(),
                operationLog.getToStatus(),
                operationLog.getRelatedOrderNo(),
                operationLog.getDocumentType(),
                operationLog.getProjectCode(),
                operationLog.getProjectName(),
                operationLog.getTransportMode(),
                operationLog.getShippingOrganization(),
                operationLog.getReceivingOrganization(),
                operationLog.getCarrierName(),
                operationLog.getTrackingNo(),
                operationLog.getDriverName(),
                operationLog.getDriverPhone(),
                operationLog.getVehicleNo(),
                operationLog.getOriginDivisionCode(),
                operationLog.getOriginDivisionName(),
                operationLog.getDestinationDivisionCode(),
                operationLog.getDestinationDivisionName(),
                operationLog.getOrigin(),
                operationLog.getDestination(),
                operationLog.getPlannedShipDate(),
                operationLog.getActualShipDate(),
                operationLog.getDeliveredDate(),
                operationLog.getRemark(),
                operationLog.getOperationRemark(),
                operationLog.getOperatorId(),
                operationLog.getOperatorUsername(),
                operationLog.getOperatorName(),
                operationLog.getOperationTime() == null ? null : operationLog.getOperationTime().toString()
        );
    }
}
