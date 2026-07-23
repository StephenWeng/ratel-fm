package com.ratel.fm.service.knowledge;

import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.config.ai.AiProperties;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.attachment.BusinessAttachment;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.domain.cashier.CashierTransaction;
import com.ratel.fm.domain.finance.AccountingSubject;
import com.ratel.fm.domain.finance.Voucher;
import com.ratel.fm.domain.finance.VoucherLine;
import com.ratel.fm.domain.inventory.InventoryLedger;
import com.ratel.fm.domain.knowledge.KnowledgeDocument;
import com.ratel.fm.domain.knowledge.KnowledgeSourceType;
import com.ratel.fm.domain.logistics.ShipmentOrder;
import com.ratel.fm.domain.purchase.PurchaseOrder;
import com.ratel.fm.domain.purchase.PurchaseOrderLine;
import com.ratel.fm.domain.receivable.ArApBill;
import com.ratel.fm.repository.attachment.BusinessAttachmentRepository;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.repository.cashier.CashierTransactionRepository;
import com.ratel.fm.repository.finance.AccountingSubjectRepository;
import com.ratel.fm.repository.finance.VoucherRepository;
import com.ratel.fm.repository.inventory.InventoryLedgerRepository;
import com.ratel.fm.repository.logistics.ShipmentOrderRepository;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.repository.receivable.ArApBillRepository;
import com.ratel.fm.service.attachment.BusinessRecordValidator;
import com.ratel.fm.service.ai.OllamaClient;
import com.ratel.fm.web.dto.knowledge.KnowledgeDtos.KnowledgeRebuildResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * AI 知识索引构建服务。
 */
@Service
public class KnowledgeIndexService {

    /** 知识索引后台任务日志对象，用于记录启动自动重建和外部向量库异常。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeIndexService.class);

    /**
     * 字段 basicDictionaryRepository：读取基础字典、项目、组织、部门、岗位等基础资料并写入知识索引。
     */
    private final BasicDictionaryRepository basicDictionaryRepository;
    /**
     * 字段 subjectRepository：读取会计科目主数据并生成科目检索分片。
     */
    private final AccountingSubjectRepository subjectRepository;
    /**
     * 字段 voucherRepository：读取凭证和分录数据并生成凭证检索分片。
     */
    private final VoucherRepository voucherRepository;
    /**
     * 字段 purchaseOrderRepository：读取采购订单及明细，用于采购问答和单据检索。
     */
    private final PurchaseOrderRepository purchaseOrderRepository;
    /**
     * 字段 shipmentOrderRepository：读取物流运输单，用于物流状态、路线和承运信息检索。
     */
    private final ShipmentOrderRepository shipmentOrderRepository;
    /**
     * 字段 inventoryLedgerRepository：读取库存台账流水，用于出入库、调拨和库存相关问答。
     */
    private final InventoryLedgerRepository inventoryLedgerRepository;
    /**
     * 字段 arApBillRepository：读取应收应付单据，用于往来、账龄、到期和逾期检索。
     */
    private final ArApBillRepository arApBillRepository;
    /**
     * 字段 cashierTransactionRepository：读取出纳资金流水，用于收付款、账户和关联业务单号检索。
     */
    private final CashierTransactionRepository cashierTransactionRepository;
    /**
     * 字段 businessAttachmentRepository：读取业务附件关联关系，用于把附件文本索引到对应业务权限和账套。
     */
    private final BusinessAttachmentRepository businessAttachmentRepository;
    /**
     * 字段 attachmentTextExtractor：从 txt、pdf、docx、xlsx 等附件中抽取可检索文本。
     */
    private final AttachmentTextExtractor attachmentTextExtractor;
    /**
     * 字段 localKnowledgeDocumentService：读取用户上传的本地知识库资料，避免全量重建清空用户资料索引。
     */
    private final LocalKnowledgeDocumentService localKnowledgeDocumentService;
    /** 附件关联业务记录校验器，用于索引附件时解析附件所属公司。 */
    private final BusinessRecordValidator businessRecordValidator;
    /**
     * 字段 ollamaClient：本地 Ollama 客户端，用于生成知识索引 embedding。
     */
    private final OllamaClient ollamaClient;
    /**
     * 字段 vectorStoreRouter：按配置选择 H2 或 Qdrant 向量库写入实现。
     */
    private final KnowledgeVectorStoreRouter vectorStoreRouter;
    /**
     * 字段 aiProperties：读取知识索引分片、embedding、H2/Qdrant 向量库等配置。
     */
    private final AiProperties aiProperties;
    /**
     * 字段 lastRebuildAt：记录最近一次全量索引成功重建时间，供健康检查和问题排查使用。
     */
    private volatile OffsetDateTime lastRebuildAt;
    /**
     * 字段 lastRebuildError：记录最近一次全量索引失败原因，避免索引失败只能查看日志。
     */
    private volatile String lastRebuildError;

    /**
     * 构造 KnowledgeIndexService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public KnowledgeIndexService(
            BasicDictionaryRepository basicDictionaryRepository,
            AccountingSubjectRepository subjectRepository,
            VoucherRepository voucherRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            ShipmentOrderRepository shipmentOrderRepository,
            InventoryLedgerRepository inventoryLedgerRepository,
            ArApBillRepository arApBillRepository,
            CashierTransactionRepository cashierTransactionRepository,
            BusinessAttachmentRepository businessAttachmentRepository,
            AttachmentTextExtractor attachmentTextExtractor,
            LocalKnowledgeDocumentService localKnowledgeDocumentService,
            BusinessRecordValidator businessRecordValidator,
            OllamaClient ollamaClient,
            KnowledgeVectorStoreRouter vectorStoreRouter,
            AiProperties aiProperties
    ) {
        this.basicDictionaryRepository = basicDictionaryRepository;
        this.subjectRepository = subjectRepository;
        this.voucherRepository = voucherRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.arApBillRepository = arApBillRepository;
        this.cashierTransactionRepository = cashierTransactionRepository;
        this.businessAttachmentRepository = businessAttachmentRepository;
        this.attachmentTextExtractor = attachmentTextExtractor;
        this.localKnowledgeDocumentService = localKnowledgeDocumentService;
        this.businessRecordValidator = businessRecordValidator;
        this.ollamaClient = ollamaClient;
        this.vectorStoreRouter = vectorStoreRouter;
        this.aiProperties = aiProperties;
    }

    @Transactional
    /**
     * 全量重建智能检索知识索引。
     *
     * <p>实现步骤：
     * 1. 先索引业务单据和附件，再索引基础资料，避免大量行政区划把业务数据挤出索引上限；
     * 2. 通过当前向量库 provider 全量替换旧索引；
     * 3. provider 内部负责 H2 或 Qdrant 的实际写入和历史数据清理。</p>
     */
    public synchronized KnowledgeRebuildResponse rebuildAll() {
        try {
            // 收集本次全量索引的全部知识分片，最后按向量库模式一次性替换旧索引。
            List<KnowledgeDocument> documents = new ArrayList<>();
            indexVouchers(documents);
            indexPurchaseOrders(documents);
            indexShipments(documents);
            indexInventoryLedgers(documents);
            indexArApBills(documents);
            indexCashierTransactions(documents);
            indexAttachments(documents);
            documents.addAll(localKnowledgeDocumentService.indexedDocumentsForRebuild());
            indexSystemModules(documents);
            indexSubjects(documents);
            indexBasicDictionaries(documents);
            vectorStore().replaceAll(documents);
            lastRebuildAt = OffsetDateTime.now();
            lastRebuildError = null;
            return new KnowledgeRebuildResponse(documents.size(), lastRebuildAt);
        } catch (RuntimeException ex) {
            lastRebuildError = ex.getMessage();
            throw ex;
        }
    }

    /**
     * 返回最近一次全量索引成功重建时间。
     */
    public OffsetDateTime lastRebuildAt() {
        return lastRebuildAt;
    }

    /**
     * 返回最近一次全量索引失败原因。
     */
    public String lastRebuildError() {
        return lastRebuildError;
    }

    @Transactional
    /**
     * 重建全部附件知识索引。
     *
     * <p>实现步骤：
     * 1. 重新读取业务附件和附件正文；
     * 2. 通过当前向量库 provider 替换附件来源分片；
     * 3. provider 内部保证只保留当前配置的索引数据。</p>
     */
    public void rebuildAttachments() {
        // 仅收集附件来源的知识分片，避免影响其他业务索引。
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexAttachments(documents);
        vectorStore().replaceSourceType(KnowledgeSourceType.ATTACHMENT, documents);
    }

    @Transactional
    /**
     * 重建指定附件的知识索引。
     *
     * <p>实现步骤：
     * 1. 按附件 ID 读取该附件当前绑定的所有业务关系；
     * 2. 重新解析附件正文并按业务权限生成知识分片；
     * 3. 替换当前向量库中该附件的旧分片，确保上传、改名和业务关系变化后可立即检索。</p>
     */
    public void rebuildAttachment(Long attachmentId) {
        if (attachmentId == null) {
            return;
        }
        List<KnowledgeDocument> documents = new ArrayList<>();
        for (BusinessAttachment relation : businessAttachmentRepository.findByAttachment_IdOrderBySortOrderAscIdAsc(attachmentId)) {
            indexAttachment(documents, relation);
        }
        vectorStore().replaceSource(KnowledgeSourceType.ATTACHMENT, attachmentId, documents);
    }

    @Transactional
    /**
     * 删除指定附件的知识索引。
     *
     * <p>实现步骤：按附件来源类型和附件 ID 删除当前向量库分片，避免附件删除后仍被智能检索命中。</p>
     */
    public void deleteAttachment(Long attachmentId) {
        if (attachmentId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.ATTACHMENT, attachmentId);
    }

    @Transactional
    /**
     * 重建单个基础字典的知识索引。
     *
     * <p>实现步骤：
     * 1. 校验字典记录是否存在；
     * 2. 删除该字典旧知识分片，避免修改名称或层级后命中旧内容；
     * 3. 重新生成包含层级路径、根分类、启停状态和说明的知识文本；
     * 4. 保存新的知识分片，供智能检索和 ratel 助手使用。</p>
     */
    public void rebuildBasicDictionary(BasicDictionary dictionary) {
        if (dictionary == null || dictionary.getId() == null) {
            return;
        }
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexBasicDictionary(documents, dictionary);
        vectorStore().replaceSource(KnowledgeSourceType.BASIC_DICTIONARY, dictionary.getId(), documents);
    }

    @Transactional
    /**
     * 删除单个基础字典的知识索引。
     *
     * <p>实现步骤：按基础字典来源类型和业务 ID 删除知识分片，确保字典删除后不会继续被智能检索命中。</p>
     */
    public void deleteBasicDictionary(Long dictionaryId) {
        if (dictionaryId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.BASIC_DICTIONARY, dictionaryId);
    }

    @Transactional
    /**
     * 重建单个会计科目的知识索引。
     *
     * <p>实现步骤：
     * 1. 校验科目已保存并拥有主键；
     * 2. 生成科目编码、名称、类别、层级和启停状态知识文本；
     * 3. 按当前向量库 provider 替换该科目旧分片。</p>
     */
    public void rebuildSubject(AccountingSubject subject) {
        if (subject == null || subject.getId() == null) {
            return;
        }
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexSubject(documents, subject);
        vectorStore().replaceSource(KnowledgeSourceType.SUBJECT, subject.getId(), documents);
    }

    @Transactional
    /**
     * 删除单个会计科目的知识索引。
     *
     * <p>实现步骤：按会计科目来源类型和业务 ID 删除当前向量库分片，防止科目删除后继续被检索命中。</p>
     */
    public void deleteSubject(Long subjectId) {
        if (subjectId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.SUBJECT, subjectId);
    }

    @Transactional
    /**
     * 重建单张财务凭证知识索引。
     *
     * <p>实现步骤：
     * 1. 校验凭证已保存并拥有主键；
     * 2. 生成凭证号、日期、项目、状态、来源单号和分录正文；
     * 3. 按当前向量库 provider 替换该凭证旧分片。</p>
     */
    public void rebuildVoucher(Voucher voucher) {
        if (voucher == null || voucher.getId() == null) {
            return;
        }
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexVoucher(documents, voucher);
        vectorStore().replaceSource(KnowledgeSourceType.VOUCHER, voucher.getId(), documents);
    }

    @Transactional
    /**
     * 删除单张财务凭证知识索引。
     *
     * <p>实现步骤：按凭证来源类型和业务 ID 删除当前向量库分片。</p>
     */
    public void deleteVoucher(Long voucherId) {
        if (voucherId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.VOUCHER, voucherId);
    }

    @Transactional
    /**
     * 重建单个采购单知识索引。
     *
     * <p>实现步骤：
     * 1. 校验采购单存在且有主键；
     * 2. 生成供应商、项目、金额、状态、明细物料和来源单号知识文本；
     * 3. 按当前向量库模式替换对应业务 ID 的旧分片。</p>
     */
    public void rebuildPurchaseOrder(PurchaseOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexPurchaseOrder(documents, order);
        vectorStore().replaceSource(KnowledgeSourceType.PURCHASE_ORDER, order.getId(), documents);
    }

    @Transactional
    /**
     * 删除单个采购单知识索引。
     *
     * <p>实现步骤：按采购单来源类型和业务 ID 删除当前向量库分片，避免采购单删除后仍被智能检索命中。</p>
     */
    public void deletePurchaseOrder(Long orderId) {
        if (orderId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.PURCHASE_ORDER, orderId);
    }

    @Transactional
    /**
     * 重建单个物流单知识索引。
     *
     * <p>实现步骤：
     * 1. 校验物流单存在且有主键；
     * 2. 生成物流单标题、路线、承运商、状态和备注分片；
     * 3. 按当前向量库 provider 替换对应业务 ID 的旧分片。</p>
     */
    public void rebuildShipment(ShipmentOrder shipment) {
        if (shipment == null || shipment.getId() == null) {
            return;
        }
        // 单据保存后只重建当前物流单，避免频繁全量索引。
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexShipment(documents, shipment);
        vectorStore().replaceSource(KnowledgeSourceType.SHIPMENT, shipment.getId(), documents);
    }

    @Transactional
    /**
     * 删除单个物流单知识索引。
     *
     * <p>实现步骤：按物流单来源类型和业务 ID 删除当前向量库分片。</p>
     */
    public void deleteShipment(Long shipmentId) {
        if (shipmentId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.SHIPMENT, shipmentId);
    }

    @Transactional
    /**
     * 重建单条库存台账知识索引。
     *
     * <p>实现步骤：
     * 1. 校验库存台账流水存在且有主键；
     * 2. 生成物料、仓库、数量、单价、关联业务和项目分片；
     * 3. 按当前向量库 provider 替换对应业务 ID 的旧分片。</p>
     */
    public void rebuildInventoryLedger(InventoryLedger ledger) {
        if (ledger == null || ledger.getId() == null) {
            return;
        }
        // 单条库存流水保存后只重建当前流水，降低索引更新开销。
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexInventoryLedger(documents, ledger);
        vectorStore().replaceSource(KnowledgeSourceType.INVENTORY_LEDGER, ledger.getId(), documents);
    }

    @Transactional
    /**
     * 删除单条库存台账知识索引。
     *
     * <p>实现步骤：按库存台账来源类型和业务 ID 删除当前向量库分片。</p>
     */
    public void deleteInventoryLedger(Long ledgerId) {
        if (ledgerId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.INVENTORY_LEDGER, ledgerId);
    }

    @Transactional
    /**
     * 重建单个应收应付单知识索引。
     *
     * <p>实现步骤：
     * 1. 校验应收应付单存在且有主键；
     * 2. 生成往来单位、项目、金额、未结余额、到期日和付款计划知识文本；
     * 3. 按当前向量库模式替换对应业务 ID 的旧分片。</p>
     */
    public void rebuildArApBill(ArApBill bill) {
        if (bill == null || bill.getId() == null) {
            return;
        }
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexArApBill(documents, bill);
        vectorStore().replaceSource(KnowledgeSourceType.AR_AP_BILL, bill.getId(), documents);
    }

    @Transactional
    /**
     * 删除单个应收应付单知识索引。
     *
     * <p>实现步骤：按应收应付来源类型和业务 ID 删除当前向量库分片，避免删除后的债权债务记录继续被检索命中。</p>
     */
    public void deleteArApBill(Long billId) {
        if (billId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.AR_AP_BILL, billId);
    }

    @Transactional
    /**
     * 重建单条出纳流水知识索引。
     *
     * <p>实现步骤：把出纳流水的账户、收付款方向、关联业务单号、金额和状态写入当前向量库，使新增或状态变化后可立即检索。</p>
     */
    public void rebuildCashierTransaction(CashierTransaction transaction) {
        if (transaction == null || transaction.getId() == null) {
            return;
        }
        List<KnowledgeDocument> documents = new ArrayList<>();
        indexCashierTransaction(documents, transaction);
        vectorStore().replaceSource(KnowledgeSourceType.CASHIER_TRANSACTION, transaction.getId(), documents);
    }

    @Transactional
    /**
     * 删除单条出纳流水知识索引。
     *
     * <p>实现步骤：出纳流水被删除时同步清理该流水在 H2 或 Qdrant 中的分片，避免智能检索返回已删除数据。</p>
     */
    public void deleteCashierTransaction(Long transactionId) {
        if (transactionId == null) {
            return;
        }
        vectorStore().deleteSource(KnowledgeSourceType.CASHIER_TRANSACTION, transactionId);
    }

    @Transactional(readOnly = true)
    /**
     * 统计当前向量库中的知识分片数量。
     *
     * <p>实现步骤：通过当前向量库 provider 统计分片数量；该方法不做跨存储降级。</p>
     */
    public long count() {
        return vectorStore().count();
    }

    /**
     * 启动后按配置自动重建知识索引。
     *
     * <p>实现步骤：
     * 1. force=true 时无条件全量重建；
     * 2. rebuildWhenEmpty=true 时只在当前向量库分片数为 0 时重建；
     * 3. 任何 Qdrant、Ollama 或数据库异常只写日志，不影响主应用继续运行。</p>
     */
    public void rebuildOnStartupIfNeeded(
            boolean force,
            boolean rebuildWhenEmpty,
            int maxAttempts,
            int retryDelaySeconds
    ) {
        int attempts = force ? 1 : Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                if (force) {
                    LOGGER.info("AI knowledge startup rebuild triggered by app.ai.knowledge.auto-rebuild-on-startup=true.");
                    KnowledgeRebuildResponse response = rebuildAll();
                    LOGGER.info("AI knowledge startup rebuild finished, documentCount={}.", response.documentCount());
                    return;
                }
                if (!rebuildWhenEmpty) {
                    return;
                }
                long currentCount = count();
                if (currentCount > 0) {
                    LOGGER.info("AI knowledge startup rebuild skipped, current index count={}.", currentCount);
                    return;
                }
                LOGGER.info("AI knowledge index is empty, startup rebuild attempt {}/{} will create initial vector index.",
                        attempt, attempts);
                KnowledgeRebuildResponse response = rebuildAll();
                LOGGER.info("AI knowledge empty-index rebuild finished, documentCount={}.", response.documentCount());
                return;
            } catch (RuntimeException ex) {
                lastRebuildError = ex.getMessage();
                if (attempt >= attempts) {
                    LOGGER.warn("AI knowledge startup rebuild exhausted {} attempts because Qdrant or Ollama was not ready.",
                            attempts, ex);
                    return;
                }
                LOGGER.warn("AI knowledge startup rebuild attempt {}/{} failed; retrying in {} seconds. reason={}",
                        attempt, attempts, Math.max(1, retryDelaySeconds), ex.getMessage());
                if (!sleepBeforeStartupRebuild(Math.max(1, retryDelaySeconds))) {
                    return;
                }
            }
        }
    }

    /**
     * 等待下一次启动索引初始化尝试。
     *
     * <p>实现步骤：按配置休眠后台守护线程；应用关闭打断线程时恢复中断标记并停止重试。</p>
     */
    private boolean sleepBeforeStartupRebuild(int delaySeconds) {
        try {
            Thread.sleep(Math.max(1, delaySeconds) * 1000L);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.info("AI knowledge startup rebuild retry stopped because the application is shutting down.");
            return false;
        }
    }

    /**
     * 写入系统模块能力说明索引。
     *
     * <p>实现步骤：
     * 1. 为基础字典、会计科目、凭证、采购、物流、库存、应收应付和附件分别生成模块说明；
     * 2. 按模块绑定对应权限码，智能检索无命中时也只能展示用户有权访问的模块；
     * 3. 写入路由元数据，便于前端从检索结果跳转到对应菜单。</p>
     */
    private void indexSystemModules(List<KnowledgeDocument> documents) {
        addDocuments(documents, KnowledgeSourceType.BASIC_DICTIONARY, 0L, "MODULE_BASIC_DICTIONARY", "基础字典模块能力说明", "系统模块",
                lines(
                        "基础字典模块能力说明",
                        "基础字典模块维护所属公司、项目、部门、岗位、物料、供应商、客户、仓库、币种、结算方式、运输方式、承运商、区划等基础资料。",
                        "基础资料以层级字典形式维护，业务表单展示名称和层级路径，后端保存字典编码、名称和级联层级关系。",
                        "适合回答项目、部门、岗位、所属公司、物料、供应商、客户、仓库、币种、运输方式等基础资料是否存在、启用状态和层级位置问题。"
                ), null, null, meta("route", "/basic-dictionaries"));
        addDocuments(documents, KnowledgeSourceType.SUBJECT, 0L, "MODULE_SUBJECT", "会计科目模块能力说明", "系统模块",
                lines(
                        "会计科目模块能力说明",
                        "会计科目模块维护科目编码、科目名称、科目类别、层级、启用状态和说明。",
                        "适合回答科目定义、科目启用状态、科目编码和财务基础资料问题。"
                ), PermissionCode.FINANCE_SUBJECT_MANAGE, null, meta("route", "/subjects"));
        addDocuments(documents, KnowledgeSourceType.VOUCHER, 0L, "MODULE_VOUCHER", "凭证记账模块能力说明", "系统模块",
                lines(
                        "凭证记账模块能力说明",
                        "凭证记账模块维护财务凭证、项目、凭证分录、借方金额、贷方金额、币种、汇率、过账状态、作废状态和来源业务单号。",
                        "适合回答按项目区分的凭证数量、凭证状态、过账情况、借贷合计和财务报表基础数据问题。"
                ), PermissionCode.FINANCE_VOUCHER_MANAGE, null, meta("route", "/vouchers"));
        addDocuments(documents, KnowledgeSourceType.PURCHASE_ORDER, 0L, "MODULE_PURCHASE", "采购管理模块能力说明", "系统模块",
                lines(
                        "采购管理模块能力说明",
                        "采购管理模块维护采购单、项目、供应商、采购日期、采购状态、采购明细、物料、数量、单价、金额、币种和备注。",
                        "适合回答按项目区分的采购单数量、供应商采购、采购金额、采购状态和采购明细问题。"
                ), PermissionCode.PURCHASE_MANAGE, null, meta("route", "/purchase-orders"));
        addDocuments(documents, KnowledgeSourceType.SHIPMENT, 0L, "MODULE_SHIPMENT", "物流运输模块能力说明", "系统模块",
                lines(
                        "物流运输模块能力说明",
                        "物流运输模块维护物流单、项目、关联业务单号、承运商、运单号、发货地、目的地、计划发运日期、实际发运日期、送达日期、物流状态和状态确认流水。",
                        "适合回答按项目区分的物流运输数量、本月发运数量、本月送达数量、承运商、运输状态和物流单明细问题。"
                ), PermissionCode.LOGISTICS_MANAGE, null, meta("route", "/shipments"));
        addDocuments(documents, KnowledgeSourceType.INVENTORY_LEDGER, 0L, "MODULE_INVENTORY", "库存管理模块能力说明", "系统模块",
                lines(
                        "库存管理模块能力说明",
                        "库存管理模块维护库存流水、项目、入库、出库、调拨、盘点、物料编码、物料名称、数量、来源仓库、目标仓库、关联业务单号和组织。",
                        "适合回答按项目区分的库存流水数量、库存变动类型、物料出入库、仓库流转和库存明细问题。"
                ), PermissionCode.INVENTORY_MANAGE, null, meta("route", "/inventory"));
        addDocuments(documents, KnowledgeSourceType.AR_AP_BILL, 0L, "MODULE_AR_AP", "应收应付模块能力说明", "系统模块",
                lines(
                        "应收应付模块能力说明",
                        "应收应付模块维护应收单、应付单、项目、往来单位、单据日期、到期日期、金额、已收付金额、未结余额、币种、状态、账龄和付款计划。",
                        "适合回答按项目区分的应收应付余额、收付统计、逾期单据、到期单据、客户供应商往来和收付款计划问题。"
                ), PermissionCode.AR_AP_MANAGE, null, meta("route", "/ar-ap"));
        addDocuments(documents, KnowledgeSourceType.CASHIER_TRANSACTION, 0L, "MODULE_CASHIER", "出纳管理模块能力说明", "系统模块",
                lines(
                        "出纳管理模块能力说明",
                        "出纳管理模块维护收款、付款、转账、退款流水，记录交易日期、账户、往来单位、结算方式、金额、币种、关联业务单号和确认状态。",
                        "适合回答资金收付、银行账户流水、关联应收应付单号、客户回款、供应商付款和出纳状态问题。"
                ), PermissionCode.FINANCE_VOUCHER_MANAGE, null, meta("route", "/cashier"));
        addDocuments(documents, KnowledgeSourceType.ATTACHMENT, 0L, "MODULE_ATTACHMENT", "业务附件模块能力说明", "系统模块",
                lines(
                        "业务附件模块能力说明",
                        "附件模块维护业务附件、原始文件名、展示名称、上传人、附件正文解析和业务单据关联。",
                        "适合回答合同、发票、单据文件、附件内容和业务资料检索问题。"
                ), null, null, meta("route", "/search"));
    }

    /**
     * 构建全部基础字典知识索引。
     *
     * <p>实现步骤：
     * 1. 按排序字段读取基础字典，保持知识结果和字典页面展示顺序一致；
     * 2. 逐条提取编码、名称、根分类、父级、完整层级路径、启停状态和说明；
     * 3. 写入 BASIC_DICTIONARY 类型知识分片，供智能检索在基础资料意图下召回。</p>
     */
    private void indexBasicDictionaries(List<KnowledgeDocument> documents) {
        for (BasicDictionary dictionary : basicDictionaryRepository.findAllByOrderBySortOrderAscIdAsc()) {
            indexBasicDictionary(documents, dictionary);
        }
    }

    /**
     * 构建单个基础字典知识文档。
     *
     * <p>实现步骤：
     * 1. 计算字典完整层级路径，用于用户按“项目/部门/岗位/物料”等自然名称检索；
     * 2. 识别根字典名称和编码，便于区分不同基础资料类型；
     * 3. 组合为结构化文本并写入知识分片。</p>
     */
    private void indexBasicDictionary(List<KnowledgeDocument> documents, BasicDictionary dictionary) {
        String path = dictionaryPath(dictionary);
        BasicDictionary root = rootDictionary(dictionary);
        String parentName = dictionary.getParent() == null ? "" : dictionary.getParent().getName();
        String rootName = root == null ? "" : root.getName();
        String rootCode = root == null ? "" : root.getCode();
        // 全国行政区划超过四万条，默认不逐条生成向量，避免笔记本初始化长期占满 CPU、内存和机械盘。
        if ("ADMINISTRATIVE_DIVISION".equals(rootCode)
                && !aiProperties.getKnowledge().isIncludeAdministrativeDivisions()) {
            return;
        }
        String title = "基础字典 " + dictionary.getName() + " " + path;
        String content = lines(
                title,
                "字典编码: " + value(dictionary.getCode()),
                "字典名称: " + value(dictionary.getName()),
                "基础资料类型: " + value(rootName),
                "基础资料类型编码: " + value(rootCode),
                "父级字典: " + value(parentName),
                "层级路径: " + value(path),
                "排序号: " + dictionary.getSortOrder(),
                "启用状态: " + (dictionary.isEnabled() ? "启用" : "停用"),
                "说明: " + value(dictionary.getDescription()),
                "可用于智能检索的基础资料关键词: 所属公司 账套 项目 部门 岗位 人员 物料 供应商 客户 仓库 币种 汇率 结算方式 付款条件 交货条件 运输方式 承运商 区划 单据类型 业务类型 取消类型"
        );
        addDocuments(documents, KnowledgeSourceType.BASIC_DICTIONARY, dictionary.getId(), dictionary.getCode(), title,
                "基础字典", content, dictionaryKnowledgePermission(rootCode), null, meta("route", "/basic-dictionaries"));
    }

    /**
     * 解析基础字典知识访问权限。
     *
     * <p>实现步骤：
     * 1. 所属公司根字典及其下级仍绑定 BASIC_DICT_MANAGE，延续“只有 admin 可看所属公司字典”的业务边界；
     * 2. 其他项目、物料、供应商、客户等基础资料作为业务检索公共上下文开放给有智能检索权限的用户；
     * 3. 返回 null 表示知识检索不再额外要求字典管理权限。</p>
     */
    private PermissionCode dictionaryKnowledgePermission(String rootCode) {
        if ("ORGANIZATION".equals(rootCode)) {
            return PermissionCode.BASIC_DICT_MANAGE;
        }
        return null;
    }

    /**
     * 读取基础字典所在层级的根节点。
     *
     * <p>实现步骤：从当前字典沿父级链路向上追溯，直到 parent 为空，返回该根节点作为基础资料类型。</p>
     */
    private BasicDictionary rootDictionary(BasicDictionary dictionary) {
        BasicDictionary cursor = dictionary;
        BasicDictionary root = dictionary;
        while (cursor != null) {
            root = cursor;
            cursor = cursor.getParent();
        }
        return root;
    }

    /**
     * 生成基础字典完整层级路径。
     *
     * <p>实现步骤：
     * 1. 从当前节点向上收集所有父级名称；
     * 2. 反转为根到叶子的顺序；
     * 3. 使用“ / ”拼接，方便用户按展示层级进行自然语言检索。</p>
     */
    private String dictionaryPath(BasicDictionary dictionary) {
        List<String> names = new ArrayList<>();
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (cursor.getName() != null && !cursor.getName().isBlank()) {
                names.add(0, cursor.getName());
            }
            cursor = cursor.getParent();
        }
        return String.join(" / ", names);
    }

    /**
     * 构建会计科目知识索引。
     *
     * <p>实现步骤：读取全部会计科目，提取编码、名称、类别、层级、启停状态和说明，并绑定科目管理权限。</p>
     */
    private void indexSubjects(List<KnowledgeDocument> documents) {
        for (AccountingSubject subject : subjectRepository.findAll()) {
            indexSubject(documents, subject);
        }
    }

    /**
     * 构建单个会计科目知识分片。
     *
     * <p>实现步骤：将科目编码、名称、类别、层级和说明拼成统一正文，供全量重建和单科目增量刷新复用。</p>
     */
    private void indexSubject(List<KnowledgeDocument> documents, AccountingSubject subject) {
        if (subject == null || subject.getId() == null) {
            return;
        }
        String title = "会计科目 " + subject.getCode() + " " + subject.getName();
        String content = lines(
                title,
                "科目编码: " + subject.getCode(),
                "科目名称: " + subject.getName(),
                "科目类别: " + subject.getCategory(),
                "科目层级: " + subject.getSubjectLevel(),
                "启用状态: " + (subject.isEnabled() ? "启用" : "停用"),
                "说明: " + value(subject.getDescription())
        );
        addDocuments(documents, KnowledgeSourceType.SUBJECT, subject.getId(), subject.getCode(), title,
                "会计科目", content, PermissionCode.FINANCE_SUBJECT_MANAGE, subject.getOrganizationCode(), meta("route", "/subjects"));
    }

    /**
     * 构建财务凭证知识索引。
     *
     * <p>实现步骤：
     * 1. 读取覆盖历史和未来期间的凭证，支持按凭证号、日期、项目和来源业务单号检索；
     * 2. 将凭证分录摘要、科目、借贷金额和辅助核算合并到正文；
     * 3. 绑定凭证管理权限和所属公司，确保检索结果不会越权。</p>
     */
    private void indexVouchers(List<KnowledgeDocument> documents) {
        for (Voucher voucher : voucherRepository.findByVoucherDateBetweenOrderByVoucherDateDesc(
                java.time.LocalDate.now().minusYears(20), java.time.LocalDate.now().plusYears(2))) {
            indexVoucher(documents, voucher);
        }
    }

    /**
     * 构建单张财务凭证知识分片。
     *
     * <p>实现步骤：将凭证主表和分录行拼成统一正文，供全量重建和单张凭证增量刷新复用。</p>
     */
    private void indexVoucher(List<KnowledgeDocument> documents, Voucher voucher) {
        if (voucher == null || voucher.getId() == null) {
            return;
        }
        // 分录文本直接进入知识正文，保证用户按科目、摘要或金额提问时可以召回凭证。
        StringBuilder lineText = new StringBuilder();
        for (VoucherLine line : voucher.getLines()) {
            lineText.append("分录").append(line.getLineNo())
                    .append(": 科目").append(line.getSubject().getCode()).append(" ").append(line.getSubject().getName())
                    .append("，摘要").append(value(line.getSummary()))
                    .append("，借方").append(money(line.getDebitAmount()))
                    .append("，贷方").append(money(line.getCreditAmount()))
                    .append("，币种").append(value(line.getCurrencyCode()))
                    .append("，辅助核算").append(value(line.getAuxiliary()))
                    .append("\n");
        }
        String title = "财务凭证 " + voucher.getVoucherNo() + " " + voucher.getSummary();
        String content = lines(
                title,
                "凭证号: " + voucher.getVoucherNo(),
                "凭证日期: " + voucher.getVoucherDate(),
                "所属年月: " + value(voucher.getBelongMonth()),
                "项目: " + value(voucher.getProjectName()) + "(" + value(voucher.getProjectCode()) + ")",
                "摘要: " + voucher.getSummary(),
                "状态: " + voucher.getStatus(),
                "借方合计: " + money(voucher.getTotalDebit()) + "，贷方合计: " + money(voucher.getTotalCredit()),
                "人民币借方合计: " + money(voucher.getTotalDebitCny()) + "，人民币贷方合计: " + money(voucher.getTotalCreditCny()),
                "币种: " + value(voucher.getCurrencyName()) + "(" + value(voucher.getCurrencyCode()) + ")",
                "制单人: " + value(voucher.getCreatedBy()) + "，过账人: " + value(voucher.getPostedBy()),
                "来源业务单号: " + value(voucher.getSourceBizNo()),
                lineText.toString()
        );
        addDocuments(documents, KnowledgeSourceType.VOUCHER, voucher.getId(), voucher.getVoucherNo(), title,
                "财务凭证", content, PermissionCode.FINANCE_VOUCHER_MANAGE, voucher.getOrganizationCode(), meta("route", "/vouchers"));
    }

    /**
     * 构建采购单知识索引。
     *
     * <p>实现步骤：读取采购单主表和明细行，合并供应商、项目、状态、币种、金额、物料和数量信息，并绑定采购管理权限。</p>
     */
    private void indexPurchaseOrders(List<KnowledgeDocument> documents) {
        for (PurchaseOrder order : purchaseOrderRepository.findAll()) {
            indexPurchaseOrder(documents, order);
        }
    }

    /**
     * 构建单个采购单知识分片。
     *
     * <p>实现步骤：将采购主表字段和明细物料行合并为统一正文，供全量重建和单据增量刷新复用。</p>
     */
    private void indexPurchaseOrder(List<KnowledgeDocument> documents, PurchaseOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        // 明细文本用于支持“某物料采购了多少”“某供应商有哪些订单”等检索场景。
        StringBuilder lineText = new StringBuilder();
        for (PurchaseOrderLine line : order.getLines()) {
            lineText.append("明细").append(line.getLineNo())
                    .append(": 物料").append(line.getItemCode()).append(" ").append(line.getItemName())
                    .append("，数量").append(money(line.getQuantity()))
                    .append("，单价").append(money(line.getUnitPrice()))
                    .append("，金额").append(money(line.getAmount()))
                    .append("，币种").append(value(line.getCurrencyCode()))
                    .append("\n");
        }
        String title = "采购单 " + order.getOrderNo() + " " + order.getSupplierName();
        String content = lines(
                title,
                "采购单号: " + order.getOrderNo(),
                "项目: " + value(order.getProjectName()) + "(" + value(order.getProjectCode()) + ")",
                "供应商: " + order.getSupplierName(),
                "单据类型: " + value(order.getDocumentType()),
                "业务类型: " + value(order.getBusinessType()),
                "采购组织: " + value(order.getPurchaseOrganization()),
                "采购部门: " + value(order.getPurchaseDepartment()),
                "采购员: " + value(order.getPurchaserName()),
                "结算组织: " + value(order.getSettlementOrganization()),
                "付款条件: " + value(order.getPaymentTerms()),
                "结算方式: " + value(order.getSettlementMethod()),
                "交货条件: " + value(order.getDeliveryTerms()),
                "来源单据类型: " + value(order.getSourceBillType()),
                "来源单据编号: " + value(order.getSourceBillNo()),
                "采购日期: " + order.getOrderDate(),
                "状态: " + order.getStatus(),
                "总金额: " + money(order.getTotalAmount()) + "，人民币金额: " + money(order.getTotalAmountCny()),
                "币种: " + value(order.getCurrencyName()) + "(" + value(order.getCurrencyCode()) + ")",
                "创建人: " + value(order.getCreatedBy()),
                "备注: " + value(order.getRemark()),
                lineText.toString()
        );
        addDocuments(documents, KnowledgeSourceType.PURCHASE_ORDER, order.getId(), order.getOrderNo(), title,
                "采购单", content, PermissionCode.PURCHASE_MANAGE, order.getOrganizationCode(), meta("route", "/purchase-orders"));
    }

    /**
     * 构建全部物流单知识索引。
     *
     * <p>实现步骤：读取物流单列表，并逐单生成可按运单号、承运商、发货地、目的地和状态召回的知识分片。</p>
     */
    private void indexShipments(List<KnowledgeDocument> documents) {
        for (ShipmentOrder shipment : shipmentOrderRepository.findAll()) {
            indexShipment(documents, shipment);
        }
    }

    /**
     * 构建单个物流单知识文档。
     *
     * <p>实现步骤：提取物流单号、项目、关联业务单号、承运商、发到货地、计划和实际日期、状态与备注，并绑定物流管理权限。</p>
     */
    private void indexShipment(List<KnowledgeDocument> documents, ShipmentOrder shipment) {
        String title = "物流单 " + shipment.getShipmentNo() + " " + shipment.getCarrierName();
        String content = lines(
                title,
                "物流单号: " + shipment.getShipmentNo(),
                "项目: " + value(shipment.getProjectName()) + "(" + value(shipment.getProjectCode()) + ")",
                "关联业务单号: " + value(shipment.getRelatedOrderNo()),
                "承运商: " + shipment.getCarrierName(),
                "运单号: " + value(shipment.getTrackingNo()),
                "发货地: " + value(shipment.getOriginDivisionName()) + " " + value(shipment.getOrigin()),
                "发货区划: " + value(shipment.getOriginDivisionName()),
                "发货详细地址: " + value(shipment.getOrigin()),
                "目的地: " + value(shipment.getDestinationDivisionName()) + " " + value(shipment.getDestination()),
                "目的地区划: " + value(shipment.getDestinationDivisionName()),
                "目的地详细地址: " + value(shipment.getDestination()),
                "计划发运日期: " + shipment.getPlannedShipDate(),
                "实际发运日期: " + value(shipment.getActualShipDate()),
                "实际发货时间: " + value(shipment.getActualShipDate()),
                "送达日期: " + value(shipment.getDeliveredDate()),
                "状态: " + shipment.getStatus(),
                "备注: " + value(shipment.getRemark())
        );
        addDocuments(documents, KnowledgeSourceType.SHIPMENT, shipment.getId(), shipment.getShipmentNo(), title,
                "物流单", content, PermissionCode.LOGISTICS_MANAGE, shipment.getOrganizationCode(), meta("route", "/shipments"));
    }

    /**
     * 构建全部库存流水知识索引。
     *
     * <p>实现步骤：读取库存流水列表，并逐条生成支持按物料、仓库、变动类型和关联业务单号召回的知识分片。</p>
     */
    private void indexInventoryLedgers(List<KnowledgeDocument> documents) {
        for (InventoryLedger ledger : inventoryLedgerRepository.findAll()) {
            indexInventoryLedger(documents, ledger);
        }
    }

    /**
     * 构建单条库存流水知识文档。
     *
     * <p>实现步骤：提取流水号、项目、物料、数量、来源和目标仓库、关联业务单号以及组织信息，并绑定库存管理权限。</p>
     */
    private void indexInventoryLedger(List<KnowledgeDocument> documents, InventoryLedger ledger) {
        String title = "库存流水 " + ledger.getMovementNo() + " " + ledger.getItemName();
        String content = lines(
                title,
                "库存流水号: " + ledger.getMovementNo(),
                "项目: " + value(ledger.getProjectName()) + "(" + value(ledger.getProjectCode()) + ")",
                "变动类型: " + ledger.getMovementType(),
                "变动日期: " + ledger.getMovementDate(),
                "物料: " + ledger.getItemCode() + " " + ledger.getItemName(),
                "数量: " + money(ledger.getQuantity()),
                "库存数量: " + money(ledger.getQuantity()),
                "来源仓库: " + value(ledger.getFromWarehouse()),
                "目标仓库: " + value(ledger.getToWarehouse()),
                "仓库: " + value(ledger.getFromWarehouse()) + " " + value(ledger.getToWarehouse()),
                "关联业务单号: " + value(ledger.getRelatedBizNo()),
                "组织: " + value(ledger.getOrganizationCode()),
                "备注: " + value(ledger.getRemark())
        );
        addDocuments(documents, KnowledgeSourceType.INVENTORY_LEDGER, ledger.getId(), ledger.getMovementNo(), title,
                "库存流水", content, PermissionCode.INVENTORY_MANAGE, ledger.getOrganizationCode(), meta("route", "/inventory"));
    }

    /**
     * 构建应收应付知识索引。
     *
     * <p>实现步骤：读取应收应付单据，计算未结余额，合并往来单位、到期日、金额、币种、状态和付款计划，并绑定应收应付权限。</p>
     */
    private void indexArApBills(List<KnowledgeDocument> documents) {
        for (ArApBill bill : arApBillRepository.findAll()) {
            indexArApBill(documents, bill);
        }
    }

    /**
     * 构建单个应收应付单知识分片。
     *
     * <p>实现步骤：将往来单位、组织、来源单据、金额、状态和付款计划合并为统一正文，供全量重建和单据增量刷新复用。</p>
     */
    private void indexArApBill(List<KnowledgeDocument> documents, ArApBill bill) {
        if (bill == null || bill.getId() == null) {
            return;
        }
        // 未结余额进入知识正文，支持“逾期未收/未付多少”等自然语言问题。
        BigDecimal remaining = safe(bill.getAmount()).subtract(safe(bill.getPaidAmount()));
        String title = "应收应付 " + bill.getBillNo() + " " + bill.getPartnerName();
        String content = lines(
                title,
                "单据编号: " + bill.getBillNo(),
                "单据类型: " + bill.getBillType(),
                "业务单据类型: " + value(bill.getDocumentType()),
                "项目: " + value(bill.getProjectName()) + "(" + value(bill.getProjectCode()) + ")",
                "往来单位: " + bill.getPartnerName(),
                "业务组织: " + value(bill.getBusinessOrganization()),
                "结算组织: " + value(bill.getSettlementOrganization()),
                "收付款组织: " + value(bill.getPaymentOrganization()),
                "收付款条件: " + value(bill.getPaymentTerms()),
                "结算方式: " + value(bill.getSettlementMethod()),
                "来源单据类型: " + value(bill.getSourceBillType()),
                "来源单据编号: " + value(bill.getSourceBillNo()),
                "单据日期: " + bill.getBillDate(),
                "到期日期: " + bill.getDueDate(),
                "金额: " + money(bill.getAmount()) + "，已收付: " + money(bill.getPaidAmount()) + "，未结: " + money(remaining),
                "人民币金额: " + money(bill.getAmountCny()) + "，人民币已收付: " + money(bill.getPaidAmountCny()),
                "币种: " + value(bill.getCurrencyName()) + "(" + value(bill.getCurrencyCode()) + ")",
                "状态: " + bill.getStatus(),
                "付款计划: " + value(bill.getPaymentPlan()),
                "组织: " + value(bill.getOrganizationCode())
        );
        addDocuments(documents, KnowledgeSourceType.AR_AP_BILL, bill.getId(), bill.getBillNo(), title,
                "应收应付", content, PermissionCode.AR_AP_MANAGE, bill.getOrganizationCode(), meta("route", "/ar-ap"));
    }

    /**
     * 构建出纳流水知识索引。
     *
     * <p>实现步骤：读取所有出纳资金流水，合并收付方向、账户、金额、状态、关联单号和摘要备注，供智能检索与 ratel助手引用。</p>
     */
    private void indexCashierTransactions(List<KnowledgeDocument> documents) {
        for (CashierTransaction transaction : cashierTransactionRepository.findAll()) {
            indexCashierTransaction(documents, transaction);
        }
    }

    /**
     * 构建单条出纳流水知识分片。
     *
     * <p>实现步骤：把资金流水正文绑定财务凭证权限和账套，防止无出纳权限用户看到资金流水。</p>
     */
    private void indexCashierTransaction(List<KnowledgeDocument> documents, CashierTransaction transaction) {
        if (transaction == null || transaction.getId() == null) {
            return;
        }
        String title = "出纳流水 " + transaction.getTransactionNo() + " " + value(transaction.getSummary());
        String content = lines(
                title,
                "流水号: " + transaction.getTransactionNo(),
                "交易日期: " + transaction.getTransactionDate(),
                "流水类型: " + transaction.getTransactionType(),
                "状态: " + transaction.getStatus(),
                "项目: " + value(transaction.getProjectName()) + "(" + value(transaction.getProjectCode()) + ")",
                "往来单位: " + value(transaction.getPartnerName()),
                "银行或现金账户: " + value(transaction.getBankAccount()),
                "结算方式: " + value(transaction.getSettlementMethod()),
                "金额: " + money(transaction.getAmount()),
                "人民币金额: " + money(transaction.getAmountCny()),
                "币种: " + value(transaction.getCurrencyName()) + "(" + value(transaction.getCurrencyCode()) + ")",
                "关联业务单号: " + value(transaction.getRelatedBizNo()),
                "摘要: " + value(transaction.getSummary()),
                "备注: " + value(transaction.getRemark()),
                "确认人: " + value(transaction.getConfirmedBy()),
                "确认时间: " + value(transaction.getConfirmedTime()),
                "凭证号: " + value(transaction.getVoucherNo()),
                "组织: " + value(transaction.getOrganizationCode())
        );
        addDocuments(documents, KnowledgeSourceType.CASHIER_TRANSACTION, transaction.getId(), transaction.getTransactionNo(), title,
                "出纳流水", content, PermissionCode.FINANCE_VOUCHER_MANAGE, transaction.getOrganizationCode(), meta("route", "/cashier"));
    }

    /**
     * 构建业务附件知识索引。
     *
     * <p>实现步骤：
     * 1. 遍历附件与业务单据关系，解析文本、PDF、Word 和 Excel 正文；
     * 2. 根据附件业务类型继承原业务菜单权限；
     * 3. 原业务记录可解析时写入所属公司，保证附件检索同样受账套过滤。</p>
     */
    private void indexAttachments(List<KnowledgeDocument> documents) {
        for (BusinessAttachment relation : businessAttachmentRepository.findAll()) {
            indexAttachment(documents, relation);
        }
    }

    /**
     * 构建单条业务附件知识分片。
     *
     * <p>实现步骤：解析附件正文、继承业务权限和所属公司，供全量重建及单附件增量刷新复用。</p>
     */
    private void indexAttachment(List<KnowledgeDocument> documents, BusinessAttachment relation) {
        if (relation == null || relation.getAttachment() == null || relation.getAttachment().getId() == null) {
            return;
        }
        String attachmentText = attachmentTextExtractor.extract(relation.getAttachment());
        String title = "业务附件 " + relation.getAttachment().getDisplayName();
        String content = lines(
                title,
                "附件名称: " + relation.getAttachment().getDisplayName(),
                "原始文件名: " + relation.getAttachment().getOriginalName(),
                "业务类型: " + relation.getBusinessType(),
                "业务ID: " + relation.getBusinessId(),
                "上传人: " + value(relation.getAttachment().getUploaderUsername()),
                "附件文本: " + value(attachmentText)
        );
        if (attachmentText.isBlank()) {
            content = lines(title,
                    "附件名称: " + relation.getAttachment().getDisplayName(),
                    "原始文件名: " + relation.getAttachment().getOriginalName(),
                    "业务类型: " + relation.getBusinessType(),
                    "业务ID: " + relation.getBusinessId(),
                    "该附件暂未解析出可检索正文。");
        }
        String organizationCode = attachmentCompanyCode(relation);
        String businessType = relation.getBusinessType() == null ? "" : relation.getBusinessType().name();
        addDocuments(documents, KnowledgeSourceType.ATTACHMENT, relation.getAttachment().getId(),
                relation.getAttachment().getDisplayName(), title, "业务附件", content,
                permissionFromAttachment(relation.getBusinessType()), organizationCode,
                meta("businessType", businessType, "businessId", String.valueOf(relation.getBusinessId())));
    }

    /**
     * 解析附件索引所属公司。
     *
     * <p>实现步骤：
     * 1. 通过附件关系里的业务类型和业务 ID 定位原业务记录；
     * 2. 读取原业务记录 organizationCode；
     * 3. 原业务记录已删除或公司为空时返回 null，让索引作为公共历史附件处理。</p>
     */
    private String attachmentCompanyCode(BusinessAttachment relation) {
        try {
            return businessRecordValidator.resolveCompanyCode(relation.getBusinessType(), relation.getBusinessId());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 根据附件业务类型解析检索权限。
     *
     * <p>实现步骤：附件跟随原业务单据的管理权限；业务类型为空时返回 null，视为公共附件知识。</p>
     */
    private PermissionCode permissionFromAttachment(AttachmentBusinessType businessType) {
        if (businessType == null) {
            return null;
        }
        return businessType.managePermission();
    }

    /**
     * 将一段业务文本转换为一个或多个知识分片。
     *
     * <p>实现步骤：
     * 1. 按配置分片长度和重叠长度切分正文；
     * 2. 写入来源类型、业务 ID、权限码、账套、摘要、路由元数据和内容哈希；
     * 3. 在 H2 embedding 开启或当前 provider 强制要求 embedding 时生成本地 Ollama embedding。</p>
     */
    private void addDocuments(
            List<KnowledgeDocument> documents,
            KnowledgeSourceType sourceType,
            Long sourceId,
            String sourceNo,
            String title,
            String category,
            String content,
            PermissionCode permissionCode,
            String organizationCode,
            String metadata
    ) {
        // 将长业务文本切分为稳定分片，便于向量召回和引用跳转。
        List<String> chunks = chunks(normalize(content));
        // 分片序号参与内容哈希和 Qdrant point ID，保证同一来源多分片可稳定覆盖。
        int index = 0;
        for (String chunk : chunks) {
            if (documents.size() >= Math.max(1, aiProperties.getKnowledge().getMaxDocuments())) {
                return;
            }
            // 每个 KnowledgeDocument 都是一个可独立检索、可权限过滤的知识分片。
            KnowledgeDocument document = new KnowledgeDocument();
            document.setSourceType(sourceType);
            document.setSourceId(sourceId);
            document.setSourceNo(truncate(sourceNo, 120));
            document.setTitle(truncate(title, 300));
            document.setCategory(category);
            document.setContent(truncate(chunk, 4000));
            document.setSummary(truncate(chunk, 500));
            document.setMetadata(metadata);
            document.setPermissionCode(permissionCode);
            document.setOrganizationCode(organizationCode);
            document.setContentHash(sha256(sourceType + ":" + sourceId + ":" + index + ":" + chunk));
            document.setChunkIndex(index++);
            fillEmbedding(document, chunk);
            documents.add(document);
        }
    }

    /**
     * 为知识分片生成本地 embedding。
     *
     * <p>实现步骤：
     * 1. 非 embedding 模式直接返回；
     * 2. 强制 embedding 的向量库必须依赖本地 Ollama embedding，模型不可用或返回空向量时直接抛错；
     * 3. 非强制 embedding 模式下向量只是增强能力，失败时保留关键词检索。</p>
     */
    private void fillEmbedding(KnowledgeDocument document, String chunk) {
        if (!shouldBuildEmbedding()) {
            return;
        }
        if (!ollamaClient.embeddingAvailable()) {
            if (embeddingRequired()) {
                throw embeddingUnavailable(null);
            }
            return;
        }
        try {
            // 本地向量会写入 H2 embedding_json，或作为外部向量库 point vector。
            List<Double> embedding = ollamaClient.embedding(chunk);
            if (!embedding.isEmpty()) {
                document.setEmbeddingJson(JSONObject.toJSONString(embedding));
                document.setEmbeddingModel(ollamaClient.embeddingModel());
                return;
            }
        } catch (RuntimeException ex) {
            if (embeddingRequired()) {
                throw embeddingUnavailable(ex);
            }
        }
        document.setEmbeddingJson(null);
        document.setEmbeddingModel(null);
        if (embeddingRequired()) {
            throw embeddingUnavailable(null);
        }
    }

    /**
     * 判断本次索引是否需要生成向量。
     *
     * <p>实现步骤：H2 语义检索开启时可生成 embedding；外部向量库要求 embedding 时必须生成，失败直接中止索引构建。</p>
     */
    private boolean shouldBuildEmbedding() {
        return aiProperties.getKnowledge().isEmbeddingEnabled() || embeddingRequired();
    }

    /**
     * 判断当前向量库是否强制要求 embedding。
     */
    private boolean embeddingRequired() {
        return vectorStoreRouter.requiresEmbedding();
    }

    /**
     * 获取当前配置的向量库实现。
     */
    private KnowledgeVectorStore vectorStore() {
        return vectorStoreRouter.active();
    }

    /**
     * 创建强制 embedding 模式下 embedding 不可用的业务异常。
     *
     * <p>实现步骤：返回 BAD_GATEWAY 业务异常，并保留底层异常 cause 便于日志排查 Ollama 连接或模型问题。</p>
     */
    private BusinessException embeddingUnavailable(Throwable cause) {
        BusinessException exception = new BusinessException(HttpStatus.BAD_GATEWAY, ResponseCode.LOAD_CLIENT_ERROR,
                "当前向量库模式需要本地 embedding 模型，请先启动 Ollama 并下载 " + ollamaClient.embeddingModel() + "。");
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    /**
     * 将业务知识正文切分为固定长度分片。
     *
     * <p>实现步骤：
     * 1. 空正文生成占位分片，确保业务记录仍可按标题和编号被检索；
     * 2. 按配置 chunk-size 和 chunk-overlap 滑动切分；
     * 3. 限制 overlap 不超过分片一半，避免长文本切分时无限循环。</p>
     */
    private List<String> chunks(String content) {
        if (content == null || content.isBlank()) {
            return List.of("无正文内容");
        }
        // 分片最小长度兜底到 300，避免配置过小导致 embedding 调用数量暴涨。
        int chunkSize = Math.max(300, aiProperties.getKnowledge().getChunkSize());
        // 重叠区最大为分片一半，兼顾语义连续性和索引体积。
        int overlap = Math.max(0, Math.min(aiProperties.getKnowledge().getChunkOverlap(), chunkSize / 2));
        // 按原文顺序保存分片，chunkIndex 会参与稳定 point ID。
        List<String> chunks = new ArrayList<>();
        // start 每轮至少前进 1，避免异常配置下卡住。
        int start = 0;
        while (start < content.length()) {
            // end 不超过正文长度，最后一个分片允许短于 chunkSize。
            int end = Math.min(content.length(), start + chunkSize);
            chunks.add(content.substring(start, end));
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    /**
     * 构造单键元数据 JSON。
     *
     * <p>实现步骤：将路由、业务类型等轻量元数据序列化，供检索结果跳转和 Qdrant payload 使用。</p>
     */
    private String meta(String key, String value) {
        // 元数据保持 JSON 格式，避免后续扩展时解析字符串片段。
        JSONObject object = new JSONObject();
        object.put(key, value);
        return object.toJSONString();
    }

    /**
     * 构造双键元数据 JSON。
     *
     * <p>实现步骤：把路由和业务附件类型等成对信息写入同一 JSON，减少调用处重复创建对象。</p>
     */
    private String meta(String key1, String value1, String key2, String value2) {
        // 元数据会随 H2 分片或 Qdrant payload 一起保存。
        JSONObject object = new JSONObject();
        object.put(key1, value1);
        object.put(key2, value2);
        return object.toJSONString();
    }

    /**
     * 拼接知识正文的多行文本。
     *
     * <p>实现步骤：忽略空值和空白值，再用换行符拼接，保持知识分片可读且便于关键词命中。</p>
     */
    private String lines(Object... values) {
        // 保留业务字段的行结构，便于后续摘要和 prompt 引用。
        List<String> lines = new ArrayList<>();
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) {
                lines.add(String.valueOf(value));
            }
        }
        return String.join("\n", lines);
    }

    /**
     * 将可选业务值转换为知识文本。
     *
     * <p>实现步骤：null 转空字符串，非空值使用 `String.valueOf`，避免知识正文出现 `null` 字样。</p>
     */
    private String value(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    /**
     * 将金额写入知识正文。
     *
     * <p>实现步骤：null 金额按 0 处理，并移除多余小数零，提升检索结果可读性。</p>
     */
    private String money(BigDecimal value) {
        return safe(value).stripTrailingZeros().toPlainString();
    }

    /**
     * 金额空值兜底。
     *
     * <p>实现步骤：null 转为 BigDecimal.ZERO，避免索引文本拼接时出现空指针。</p>
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 规范化知识正文文本。
     *
     * <p>实现步骤：清理不可见空字符和连续空白，保留换行结构，减少 embedding 和关键词评分噪声。</p>
     */
    private String normalize(String value) {
        return value == null ? "" : value.replace('\u0000', ' ').replaceAll("[\\t\\x0B\\f\\r ]+", " ").trim();
    }

    /**
     * 按字段上限截断知识文本。
     *
     * <p>实现步骤：先规范化文本，再按最大长度截断，避免标题、摘要和正文超过数据库或 payload 预期。</p>
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        // 截断前先清理空白，避免有效内容被无意义空格挤掉。
        String normalized = normalize(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    /**
     * 生成知识分片内容哈希。
     *
     * <p>实现步骤：优先使用 SHA-256；极端环境算法不可用时回退对象 hash，保证索引流程不中断。</p>
     */
    private String sha256(String value) {
        try {
            // SHA-256 用于稳定识别同一来源、同一分片和同一正文内容。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(Objects.hashCode(value)).toLowerCase(Locale.ROOT);
        }
    }

    @Configuration
    /**
     * KnowledgeIndexStartupConfiguration 类。
     * 
     * <p>用于承载 KnowledgeIndexStartupConfiguration 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    static class KnowledgeIndexStartupConfiguration {

        @Bean
        @Order(20)
        CommandLineRunner rebuildKnowledgeOnStartup(KnowledgeIndexService service, AiProperties aiProperties) {
            return args -> {
                AiProperties.Knowledge config = aiProperties.getKnowledge() == null
                        ? new AiProperties.Knowledge()
                        : aiProperties.getKnowledge();
                if (!config.isAutoRebuildOnStartup() && !config.isAutoRebuildWhenEmpty()) {
                    return;
                }
                Thread thread = new Thread(
                        () -> {
                            if (!service.sleepBeforeStartupRebuild(
                                    Math.max(1, config.getStartupRebuildInitialDelaySeconds()))) {
                                return;
                            }
                            service.rebuildOnStartupIfNeeded(
                                    config.isAutoRebuildOnStartup(),
                                    config.isAutoRebuildWhenEmpty(),
                                    config.getStartupRebuildMaxAttempts(),
                                    config.getStartupRebuildRetryDelaySeconds()
                            );
                        },
                        "knowledge-index-startup"
                );
                thread.setDaemon(true);
                thread.start();
            };
        }
    }
}
