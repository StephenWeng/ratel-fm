package com.ratel.fm.service.attachment;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.repository.finance.VoucherRepository;
import com.ratel.fm.repository.inventory.InventoryLedgerRepository;
import com.ratel.fm.repository.logistics.ShipmentOrderRepository;
import com.ratel.fm.repository.purchase.PurchaseOrderRepository;
import com.ratel.fm.repository.receivable.ArApBillRepository;
import com.ratel.fm.security.CompanyScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 附件所属业务记录校验器。
 *
 * <p>上传、查询、下载和删除附件前先确认业务记录存在，避免产生无法追溯的孤立附件关系。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
public class BusinessRecordValidator {

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
     * 构造 BusinessRecordValidator 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BusinessRecordValidator(
            VoucherRepository voucherRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            ShipmentOrderRepository shipmentOrderRepository,
            InventoryLedgerRepository inventoryLedgerRepository,
            ArApBillRepository arApBillRepository
    ) {
        this.voucherRepository = voucherRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.shipmentOrderRepository = shipmentOrderRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.arApBillRepository = arApBillRepository;
    }

    /**
     * 校验附件所属业务记录真实存在。
     *
     * <p>实现步骤：
     * 1. 校验业务 ID 必须为正数；
     * 2. 根据业务类型读取业务记录所属公司；
     * 3. 业务记录不存在时抛出 404；
     * 4. 业务记录不属于当前登录公司时拒绝访问，阻止附件跨账套查询、上传或删除。</p>
     */
    public void ensureExists(AttachmentBusinessType businessType, Long businessId) {
        String organizationCode = resolveCompanyCode(businessType, businessId);
        CompanyScope.requireCurrentCompany(organizationCode, "业务附件");
    }

    /**
     * 解析业务记录所属公司。
     *
     * <p>实现步骤：
     * 1. 校验业务 ID 必须为正数；
     * 2. 按业务类型读取对应业务表记录；
     * 3. 返回记录的 organizationCode，供附件校验和知识索引复用。</p>
     */
    public String resolveCompanyCode(AttachmentBusinessType businessType, Long businessId) {
        if (businessId == null || businessId <= 0) {
            throw new BusinessException("业务记录ID不正确");
        }
        String organizationCode = switch (businessType) {
            case VOUCHER -> voucherRepository.findById(businessId)
                    .map(item -> item.getOrganizationCode())
                    .orElse(null);
            case PURCHASE_ORDER -> purchaseOrderRepository.findById(businessId)
                    .map(item -> item.getOrganizationCode())
                    .orElse(null);
            case SHIPMENT -> shipmentOrderRepository.findById(businessId)
                    .map(item -> item.getOrganizationCode())
                    .orElse(null);
            case INVENTORY_LEDGER -> inventoryLedgerRepository.findById(businessId)
                    .map(item -> item.getOrganizationCode())
                    .orElse(null);
            case AR_AP_BILL -> arApBillRepository.findById(businessId)
                    .map(item -> item.getOrganizationCode())
                    .orElse(null);
        };
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "业务记录不存在");
        }
        return organizationCode;
    }
}
