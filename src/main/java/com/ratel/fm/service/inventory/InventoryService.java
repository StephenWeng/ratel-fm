package com.ratel.fm.service.inventory;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.common.SearchSpecs;
import com.ratel.fm.config.export.ExportProperties;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.domain.inventory.InventoryLedger;
import com.ratel.fm.domain.inventory.InventoryMovementType;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.repository.inventory.InventoryLedgerRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.security.SecurityUtils;
import com.ratel.fm.service.attachment.AttachmentService;
import com.ratel.fm.service.audit.AuditLogService;
import com.ratel.fm.service.export.ExcelExportService;
import com.ratel.fm.service.export.ExcelExportService.ExcelColumn;
import com.ratel.fm.service.common.BusinessNumberSequenceService;
import com.ratel.fm.service.knowledge.KnowledgeIndexService;
import com.ratel.fm.service.operationlog.BusinessOperationLogService;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryExportRequest;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryMaterialStockView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryRequest;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryStockView;
import com.ratel.fm.web.dto.phasetwo.PhaseTwoDtos.InventoryView;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogPage;
import com.ratel.fm.web.dto.operationlog.BusinessOperationLogDtos.BusinessOperationLogView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存台账服务。
 *
 * <p>当前提供库存流水新增和最近流水查询，后续可扩展库存余额、成本核算和盘点差异处理。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class InventoryService {

    /**
     * 常量 QUANTITY_SCALE：保存 QUANTITY_SCALE 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final int QUANTITY_SCALE = 4;

    /**
     * 字段 repository：保存 repository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final InventoryLedgerRepository repository;
    /**
     * 字段 dictionaryRepository：保存 dictionaryRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BasicDictionaryRepository dictionaryRepository;
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
    /** 业务单号序号服务，用于并发安全生成库存流水号。 */
    private final BusinessNumberSequenceService numberSequenceService;

    /**
     * 构造 InventoryService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public InventoryService(
            InventoryLedgerRepository repository,
            BasicDictionaryRepository dictionaryRepository,
            AuditLogService auditLogService,
            ExportProperties exportProperties,
            ExcelExportService excelExportService,
            AttachmentService attachmentService,
            BusinessOperationLogService businessOperationLogService,
            KnowledgeIndexService knowledgeIndexService,
            BusinessNumberSequenceService numberSequenceService
    ) {
        this.repository = repository;
        this.dictionaryRepository = dictionaryRepository;
        this.auditLogService = auditLogService;
        this.exportProperties = exportProperties;
        this.excelExportService = excelExportService;
        this.attachmentService = attachmentService;
        this.businessOperationLogService = businessOperationLogService;
        this.knowledgeIndexService = knowledgeIndexService;
        this.numberSequenceService = numberSequenceService;
    }

    /**
     * 查询最近 100 条库存流水。
     */
    @Transactional(readOnly = true)
    public List<InventoryView> list() {
        return list(null, null, null, null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    /**
     * 执行 stock 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public InventoryStockView stock(String itemCode, String warehouse, LocalDate asOfDate) {
        // 变量说明：normalizedItemCode 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedItemCode = requiredText(itemCode, "请选择物料");
        // 变量说明：normalizedWarehouse 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedWarehouse = requiredText(warehouse, "请选择仓库");
        // 变量说明：date 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        String itemName = repository.findAll(CompanyScope.<InventoryLedger>currentCompanySpec()).stream()
                .filter(item -> normalizedItemCode.equals(item.getItemCode()))
                .map(InventoryLedger::getItemName)
                .filter(item -> item != null && !item.isBlank())
                .findFirst()
                .orElse("");
        return new InventoryStockView(normalizedItemCode, itemName, normalizedWarehouse, date,
                quantity(currentStock(normalizedItemCode, normalizedWarehouse, date)));
    }

    /**
     * 按物料字典层级统计库存数量。
     */
    @Transactional(readOnly = true)
    public List<InventoryMaterialStockView> materialStock() {
        BasicDictionary root = dictionaryRepository.findByCode("MATERIAL")
                .filter(this::isDictionaryVisibleForBusiness)
                .orElse(null);
        if (root == null) {
            return List.of();
        }
        // 变量说明：dictionaries 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionary> dictionaries = dictionaryRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, List<BasicDictionary>> childrenMap = dictionaries.stream()
                .filter(item -> item.getParent() != null)
                .collect(Collectors.groupingBy(item -> item.getParent().getId()));
        // 变量说明：amountMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<String, MaterialStockAmount> amountMap = materialStockAmountMap();
        return childrenMap.getOrDefault(root.getId(), List.of()).stream()
                .filter(this::isDictionaryVisibleForBusiness)
                .map(item -> materialStockNode(item, childrenMap, amountMap))
                .toList();
    }

    /**
     * 按字段查询库存流水。
     *
     * <p>实现步骤：
     * 1. 未指定日期时默认查最近 100 条；
     * 2. 流水号、物料名称、关联单号使用包含匹配；
     * 3. 库存变动类型、来源仓和目标仓使用等值匹配，日期按范围过滤。</p>
     */
    @Transactional(readOnly = true)
    public List<InventoryView> list(
            LocalDate startDate,
            LocalDate endDate,
            String movementNo,
            InventoryMovementType movementType,
            String itemName,
            String projectCode,
            String fromWarehouse,
            String toWarehouse,
            String relatedBizNo
    ) {
        /**
         * 库存台账列表查询条件，先限定当前账套，再叠加日期、流水号、物料、项目和仓库筛选。
         */
        var spec = CompanyScope.<InventoryLedger>currentCompanySpec()
                .and(SearchSpecs.dateBetween("movementDate", startDate, endDate))
                .and(SearchSpecs.like("movementNo", movementNo))
                .and(SearchSpecs.equal("movementType", movementType))
                .and(SearchSpecs.like("itemName", itemName))
                .and(SearchSpecs.equal("projectCode", blankToNull(projectCode)))
                .and(SearchSpecs.equal("fromWarehouse", blankToNull(fromWarehouse)))
                .and(SearchSpecs.equal("toWarehouse", blankToNull(toWarehouse)))
                .and(SearchSpecs.like("relatedBizNo", relatedBizNo));
        return repository.findAll(spec, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "modifyTime", "id"))).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 导出库存台账列表。
     *
     * <p>实现步骤：
     * 1. 如果请求携带选中流水 ID，则按选中数据导出；
     * 2. 如果未选择数据，则按当前搜索条件查询；
     * 3. 查询结果按配置最大行数截断；
     * 4. 导出字段与库存台账列表可见字段保持一致。</p>
     */
    @Transactional(readOnly = true)
    public byte[] export(InventoryExportRequest request) {
        InventoryExportRequest exportRequest = request == null
                ? new InventoryExportRequest(null, null, null, null, null, null, null, null, null, null, null)
                : request;
        List<InventoryView> rows = hasSelectedIds(exportRequest.ids())
                ? selectedRows(exportRequest.ids())
                : searchRowsForExport(exportRequest);
        return excelExportService.export("库存台账", List.of(
                new ExcelColumn<>("流水号", InventoryView::movementNo),
                new ExcelColumn<>("类型", row -> movementTypeText(row.movementType())),
                new ExcelColumn<>("日期", InventoryView::movementDate),
                new ExcelColumn<>("物料编码", InventoryView::itemCode),
                new ExcelColumn<>("物料名称", InventoryView::itemName),
                new ExcelColumn<>("项目", InventoryView::projectName),
                new ExcelColumn<>("数量", InventoryView::quantity),
                new ExcelColumn<>("来源仓", InventoryView::fromWarehouse),
                new ExcelColumn<>("目标仓", InventoryView::toWarehouse)
        ), rows);
    }

    /**
     * 按选中 ID 查询库存导出数据。
     */
    private List<InventoryView> selectedRows(List<Long> ids) {
        List<Long> exportIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(exportProperties.maxRows())
                .toList();
        // 变量说明：orderMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<Long, Integer> orderMap = new LinkedHashMap<>();
        for (int index = 0; index < exportIds.size(); index++) {
            orderMap.put(exportIds.get(index), index);
        }
        return repository.findAllById(exportIds).stream()
                .filter(row -> Objects.equals(CompanyScope.currentCompanyCode(), row.getOrganizationCode()))
                .sorted(Comparator.comparingInt(row -> orderMap.getOrDefault(row.getId(), Integer.MAX_VALUE)))
                .map(this::toView)
                .toList();
    }

    /**
     * 按搜索条件查询库存导出数据。
     */
    private List<InventoryView> searchRowsForExport(InventoryExportRequest request) {
        /**
         * 库存导出查询条件，与列表筛选口径一致并限制当前所属公司。
         */
        var spec = CompanyScope.<InventoryLedger>currentCompanySpec()
                .and(SearchSpecs.dateBetween("movementDate", request.startDate(), request.endDate()))
                .and(SearchSpecs.like("movementNo", request.movementNo()))
                .and(SearchSpecs.equal("movementType", request.movementType()))
                .and(SearchSpecs.like("itemName", request.itemName()))
                .and(SearchSpecs.equal("projectCode", blankToNull(request.projectCode())))
                .and(SearchSpecs.equal("fromWarehouse", blankToNull(request.fromWarehouse())))
                .and(SearchSpecs.equal("toWarehouse", blankToNull(request.toWarehouse())))
                .and(SearchSpecs.like("relatedBizNo", request.relatedBizNo()));
        return repository.findAll(
                        spec,
                        PageRequest.of(0, exportProperties.maxRows(), Sort.by(Sort.Direction.DESC, "modifyTime", "id"))
                ).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 新增库存流水。
     *
     * <p>实现步骤：
     * 1. 按库存变动日期生成流水号；
     * 2. 写入变动类型、物料、数量、仓库和关联业务单号；
     * 3. 从当前登录人带入所属公司字典编码，保证库存流水按账套隔离；
     * 4. 保存流水；
     * 5. 记录关键操作日志。</p>
     */
    @Transactional
    public InventoryView create(InventoryRequest request) {
        // 步骤1-3：库存流水不直接计算余额，先完整记录每一次数量变动。
        validateInventoryRequest(request);
        // 变量说明：ledger 保存当前步骤计算、查询或转换得到的中间结果。
        InventoryLedger ledger = new InventoryLedger();
        ledger.setMovementNo(nextNo(request));
        ledger.setMovementType(request.movementType());
        ledger.setMovementDate(request.movementDate());
        ledger.setItemCode(request.itemCode());
        ledger.setItemName(request.itemName());
        ledger.setProjectCode(blankToNull(request.projectCode()));
        ledger.setProjectName(blankToNull(request.projectName()));
        ledger.setSpecification(request.specification());
        ledger.setStockOrganization(request.stockOrganization());
        ledger.setOwnerName(request.ownerName());
        ledger.setUnitName(request.unitName());
        ledger.setBatchNo(request.batchNo());
        ledger.setQuantity(quantity(request.quantity()));
        ledger.setFromWarehouse(blankToNull(request.fromWarehouse()));
        ledger.setToWarehouse(blankToNull(request.toWarehouse()));
        ledger.setRelatedBizNo(request.relatedBizNo());
        ledger.setSourceBillType(request.sourceBillType());
        ledger.setRemark(request.remark());
        ledger.setOrganizationCode(CompanyScope.currentCompanyCode());
        // 变量说明：view 保存当前步骤计算、查询或转换得到的中间结果。
        InventoryView view = toView(repository.save(ledger));
        knowledgeIndexService.rebuildInventoryLedger(ledger);
        businessOperationLogService.record("INVENTORY_LEDGER", view.id(), view.movementNo(), inventoryTitle(view), "CREATE", "新增库存流水",
                "新增库存流水号" + view.movementNo() + "，物料为" + view.itemName() + "，数量为" + view.quantity() + "。",
                null, movementTypeText(view.movementType()), view);
        // 步骤5：库存流水会影响库存台账和后续成本核算，必须审计。
        auditLogService.record("CREATE_INVENTORY_LEDGER", request, "SUCCESS",
                "库存台账新增了流水号" + view.movementNo() + "，物料为" + view.itemName() + "，数量为" + view.quantity() + "。");
        return view;
    }

    /**
     * 批量删除库存流水。
     *
     * <p>实现步骤：
     * 1. 清理并去重前端传入的库存流水 ID；
     * 2. 逐个读取库存流水，任一 ID 不存在则整体失败；
     * 3. 删除库存流水主表；
     * 4. 记录关键操作日志。</p>
     */
    @Transactional
    public void delete(List<Long> ids) {
        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        List<Long> deleteIds = normalizeBatchIds(ids);
        // 变量说明：movementNos 保存当前步骤计算、查询或转换得到的中间结果。
        List<String> movementNos = new java.util.ArrayList<>();
        for (Long id : deleteIds) {
            InventoryLedger ledger = repository.findById(id)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "库存流水不存在: " + id));
            CompanyScope.requireCurrentCompany(ledger.getOrganizationCode(), "库存流水");
            movementNos.add(ledger.getMovementNo());
            businessOperationLogService.record("INVENTORY_LEDGER", ledger.getId(), ledger.getMovementNo(), ledger.getItemName(), "DELETE", "删除库存流水",
                    "删除库存流水号" + ledger.getMovementNo() + "，物料为" + ledger.getItemName() + "。",
                    movementTypeText(ledger.getMovementType()), "已删除", ledger.getMovementNo());
            attachmentService.deleteAllForBusiness(AttachmentBusinessType.INVENTORY_LEDGER, id);
            knowledgeIndexService.deleteInventoryLedger(id);
            repository.delete(ledger);
        }
        auditLogService.record("BATCH_DELETE_INVENTORY_LEDGERS", "inventoryLedgerIds=" + deleteIds + ", movementNos=" + movementNos,
                "SUCCESS", "库存台账删除了流水号: " + String.join("、", movementNos) + "。");
    }

    /**
     * 按库存变动日期生成流水号。
     *
     * <p>实现步骤：
     * 1. 按库存变动日期生成 INVyyyyMMdd 前缀；
     * 2. 只在当前所属公司内查询相同日期前缀下的最大流水号；
     * 3. 从最大流水号后递增，并再次校验同公司内是否已存在；
     * 4. 返回同一所属公司内唯一的库存流水号。</p>
     */
    private String nextNo(InventoryRequest request) {
        // 步骤一：库存流水号仍按业务日期分段，方便人工识别和排序。
        String prefix = "INV" + request.movementDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 步骤二：库存流水号唯一范围限定在当前所属公司，避免多账套互相占用号段。
        String companyCode = CompanyScope.currentCompanyCode();
        return numberSequenceService.next(
                "INVENTORY_LEDGER",
                companyCode,
                prefix,
                () -> repository.findFirstByOrganizationCodeAndMovementNoStartingWithOrderByMovementNoDesc(companyCode, prefix)
                        .map(ledger -> movementNoSequence(ledger.getMovementNo(), prefix) + 1)
                        .orElse(1),
                no -> repository.existsByOrganizationCodeAndMovementNo(companyCode, no)
        );
    }

    /**
     * 提取库存流水号末尾序号。
     *
     * <p>实现步骤：
     * 1. 校验流水号是否属于当前日期前缀；
     * 2. 截取前缀后的数字段；
     * 3. 数字段解析失败时返回 0，由调用方从 1 开始重新递增。</p>
     */
    private int movementNoSequence(String movementNo, String prefix) {
        if (movementNo == null || !movementNo.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(movementNo.substring(prefix.length()));
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
     * 清理批量删除 ID。
     */
    private List<Long> normalizeBatchIds(List<Long> ids) {
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
     * 将下拉筛选清空后的空字符串转为空值。
     *
     * <p>实现步骤：空文本返回 null；非空文本去除首尾空格后用于字典类等值查询。</p>
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 执行 validateInventoryRequest 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private void validateInventoryRequest(InventoryRequest request) {
        if (request.movementType() == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "库存变动类型不能为空");
        }
        // 变量说明：requestQuantity 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal requestQuantity = quantity(request.quantity());
        if (requestQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "库存数量必须大于0");
        }
        switch (request.movementType()) {
            case INBOUND -> {
                if (blankToNull(request.toWarehouse()) == null) {
                    throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "入库必须选择目标仓");
                }
            }
            case OUTBOUND -> {
                // 变量说明：warehouse 保存当前步骤计算、查询或转换得到的中间结果。
                String warehouse = requiredText(request.fromWarehouse(), "出库必须选择来源仓");
                ensureEnoughStock(request.itemCode(), request.itemName(), warehouse, request.movementDate(), requestQuantity);
            }
            case TRANSFER -> {
                // 变量说明：fromWarehouse 保存当前步骤计算、查询或转换得到的中间结果。
                String fromWarehouse = requiredText(request.fromWarehouse(), "调拨必须选择来源仓");
                // 变量说明：toWarehouse 保存当前步骤计算、查询或转换得到的中间结果。
                String toWarehouse = requiredText(request.toWarehouse(), "调拨必须选择目标仓");
                if (fromWarehouse.equals(toWarehouse)) {
                    throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "调拨来源仓和目标仓不能相同");
                }
                ensureEnoughStock(request.itemCode(), request.itemName(), fromWarehouse, request.movementDate(), requestQuantity);
            }
            case CHECK -> {
                if (blankToNull(request.toWarehouse()) == null && blankToNull(request.fromWarehouse()) == null) {
                    throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "盘点必须选择盘点仓库");
                }
            }
        }
    }

    /**
     * 执行 ensureEnoughStock 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private void ensureEnoughStock(String itemCode, String itemName, String warehouse, LocalDate movementDate, BigDecimal requestQuantity) {
        // 变量说明：available 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal available = currentStock(itemCode, warehouse, movementDate == null ? LocalDate.now() : movementDate);
        if (requestQuantity.compareTo(available) > 0) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM,
                    "库存不足，物料" + itemName + "在仓库" + warehouse + "当前可用库存为"
                            + quantityText(available) + "，本次数量为" + quantityText(requestQuantity) + "，不能提交");
        }
    }

    /**
     * 执行 currentStock 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BigDecimal currentStock(String itemCode, String warehouse, LocalDate asOfDate) {
        // 变量说明：normalizedItemCode 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedItemCode = requiredText(itemCode, "请选择物料");
        // 变量说明：normalizedWarehouse 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedWarehouse = requiredText(warehouse, "请选择仓库");
        // 变量说明：date 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDate date = asOfDate == null ? LocalDate.now() : asOfDate;
        BigDecimal stock = repository.findAll(CompanyScope.<InventoryLedger>currentCompanySpec()).stream()
                .filter(item -> normalizedItemCode.equals(item.getItemCode()))
                .filter(item -> item.getMovementDate() != null && !item.getMovementDate().isAfter(date))
                .filter(item -> warehouseTouches(item, normalizedWarehouse))
                .map(item -> stockDelta(item, normalizedWarehouse))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return stock.max(BigDecimal.ZERO);
    }

    /**
     * 执行 warehouseTouches 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean warehouseTouches(InventoryLedger row, String warehouse) {
        return warehouse.equals(blankToNull(row.getFromWarehouse())) || warehouse.equals(blankToNull(row.getToWarehouse()));
    }

    /**
     * 执行 stockDelta 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BigDecimal stockDelta(InventoryLedger row, String warehouse) {
        // 变量说明：quantity 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal quantity = quantity(row.getQuantity());
        return switch (row.getMovementType()) {
            case INBOUND -> warehouse.equals(blankToNull(row.getToWarehouse())) ? quantity : BigDecimal.ZERO;
            case OUTBOUND -> warehouse.equals(blankToNull(row.getFromWarehouse())) ? quantity.negate() : BigDecimal.ZERO;
            case TRANSFER -> {
                if (warehouse.equals(blankToNull(row.getFromWarehouse()))) {
                    yield quantity.negate();
                }
                if (warehouse.equals(blankToNull(row.getToWarehouse()))) {
                    yield quantity;
                }
                yield BigDecimal.ZERO;
            }
            case CHECK -> BigDecimal.ZERO;
        };
    }

    /**
     * 执行 materialStockAmountMap 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Map<String, MaterialStockAmount> materialStockAmountMap() {
        // 变量说明：amountMap 保存当前步骤计算、查询或转换得到的中间结果。
        Map<String, MaterialStockAmount> amountMap = new LinkedHashMap<>();
        for (InventoryLedger row : repository.findAll(CompanyScope.<InventoryLedger>currentCompanySpec())) {
            // 变量说明：itemCode 保存当前步骤计算、查询或转换得到的中间结果。
            String itemCode = blankToNull(row.getItemCode());
            if (itemCode == null || row.getMovementType() == null) {
                continue;
            }
            amountMap.computeIfAbsent(itemCode, ignored -> new MaterialStockAmount())
                    .add(row.getMovementType(), quantity(row.getQuantity()));
        }
        return amountMap;
    }

    /**
     * 执行 materialStockNode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private InventoryMaterialStockView materialStockNode(
            BasicDictionary dictionary,
            Map<Long, List<BasicDictionary>> childrenMap,
            Map<String, MaterialStockAmount> amountMap
    ) {
        // 变量说明：ownAmount 保存当前步骤计算、查询或转换得到的中间结果。
        MaterialStockAmount ownAmount = amountMap.getOrDefault(dictionary.getCode(), new MaterialStockAmount());
        // 变量说明：inboundQuantity 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal inboundQuantity = ownAmount.inboundQuantity();
        // 变量说明：outboundQuantity 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal outboundQuantity = ownAmount.outboundQuantity();
        // 变量说明：transferQuantity 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal transferQuantity = ownAmount.transferQuantity();
        // 变量说明：children 保存当前步骤计算、查询或转换得到的中间结果。
        List<InventoryMaterialStockView> children = new ArrayList<>();
        for (BasicDictionary child : childrenMap.getOrDefault(dictionary.getId(), List.of())) {
            if (!isDictionaryVisibleForBusiness(child)) {
                continue;
            }
            // 变量说明：childView 保存当前步骤计算、查询或转换得到的中间结果。
            InventoryMaterialStockView childView = materialStockNode(child, childrenMap, amountMap);
            children.add(childView);
            inboundQuantity = inboundQuantity.add(childView.inboundQuantity());
            outboundQuantity = outboundQuantity.add(childView.outboundQuantity());
            transferQuantity = transferQuantity.add(childView.transferQuantity());
        }
        // 变量说明：stockQuantity 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal stockQuantity = inboundQuantity.subtract(outboundQuantity).subtract(transferQuantity);
        return new InventoryMaterialStockView(
                dictionary.getCode(),
                dictionary.getName(),
                quantity(inboundQuantity),
                quantity(outboundQuantity),
                quantity(transferQuantity),
                quantity(stockQuantity),
                List.copyOf(children)
        );
    }

    /**
     * 执行 isDictionaryVisibleForBusiness 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean isDictionaryVisibleForBusiness(BasicDictionary dictionary) {
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (!cursor.isEnabled()) {
                return false;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return true;
    }

    /**
     * 执行 quantity 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private BigDecimal quantity(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /**
     * 执行 quantityText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String quantityText(BigDecimal value) {
        return quantity(value).toPlainString();
    }

    /**
     * 执行 requiredText 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String requiredText(String value, String message) {
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, message);
        }
        return normalized;
    }

    /**
     * 库存变动类型中文化，用于 Excel 导出。
     */
    private String movementTypeText(InventoryMovementType movementType) {
        return switch (movementType) {
            case INBOUND -> "入库";
            case OUTBOUND -> "出库";
            case TRANSFER -> "调拨";
            case CHECK -> "盘点";
        };
    }

    /**
     * 查询库存流水操作流水。
     */
    @Transactional(readOnly = true)
    public List<BusinessOperationLogView> listOperationLogs(Long id) {
        InventoryLedger ledger = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "库存流水不存在"));
        CompanyScope.requireCurrentCompany(ledger.getOrganizationCode(), "库存流水");
        return businessOperationLogService.list("INVENTORY_LEDGER", id);
    }

    /**
     * 分页查询库存流水操作流水。
     *
     * <p>实现步骤：先确认库存流水存在，再按操作时间范围和分页条件查询，前端右侧抽屉滚动加载。</p>
     */
    @Transactional(readOnly = true)
    public BusinessOperationLogPage pageOperationLogs(Long id, OffsetDateTime startTime, OffsetDateTime endTime, int page, int size) {
        InventoryLedger ledger = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "库存流水不存在"));
        CompanyScope.requireCurrentCompany(ledger.getOrganizationCode(), "库存流水");
        return businessOperationLogService.page("INVENTORY_LEDGER", id, startTime, endTime, page, size);
    }

    /**
     * 生成库存流水标题。
     */
    private String inventoryTitle(InventoryView view) {
        return view.movementNo() + " " + view.itemName();
    }

    /**
     * 执行 toView 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private InventoryView toView(InventoryLedger ledger) {
        return new InventoryView(ledger.getId(), ledger.getMovementNo(), ledger.getMovementType(), ledger.getMovementDate(),
                ledger.getItemCode(), ledger.getItemName(), ledger.getProjectCode(), ledger.getProjectName(), ledger.getSpecification(), ledger.getStockOrganization(),
                ledger.getOwnerName(), ledger.getUnitName(), ledger.getBatchNo(), ledger.getQuantity(), ledger.getFromWarehouse(),
                ledger.getToWarehouse(), ledger.getRelatedBizNo(), ledger.getSourceBillType(), ledger.getRemark(), ledger.getOrganizationCode(),
                ledger.getVoucherId(), ledger.getVoucherNo(),
                attachmentService.count(AttachmentBusinessType.INVENTORY_LEDGER, ledger.getId()));
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
         * 执行 inboundQuantity 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private BigDecimal inboundQuantity() {
            return inboundQuantity;
        }

        /**
         * 执行 outboundQuantity 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private BigDecimal outboundQuantity() {
            return outboundQuantity;
        }

        /**
         * 执行 transferQuantity 方法。
         * 
         * <p>实现步骤：
         * 1. 接收并校验调用方传入的数据；
         * 2. 按当前方法职责执行业务查询、转换或持久化处理；
         * 3. 返回处理结果或更新对象状态。</p>
         */
        private BigDecimal transferQuantity() {
            return transferQuantity;
        }
    }
}
