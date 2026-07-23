package com.ratel.fm.domain.operation;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 统一业务操作流水。
 *
 * <p>用于凭证记账、采购管理、库存台账、应收应付等模块记录业务记录级别的时间轴，区别于系统级审计日志。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_business_operation_logs")
@Comment("统一业务操作流水表，记录凭证、采购、库存、应收应付等业务记录的时间轴")
public class BusinessOperationLog extends BaseEntity {

    /** 业务类型，例如 VOUCHER、PURCHASE_ORDER、INVENTORY_LEDGER、AR_AP_BILL。 */
    @Column(nullable = false, length = 40)
    @Comment("业务类型")
    private String businessType;

    /** 所属公司字典编码，业务记录流水按账套隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为业务操作流水账套隔离标识")
    private String organizationCode;

    /** 业务记录主键 ID。 */
    @Column(nullable = false)
    @Comment("业务记录主键ID")
    private Long businessId;

    /** 业务编号快照，例如凭证号、采购单号、库存流水号、应收应付单号。 */
    @Column(nullable = false, length = 120)
    @Comment("业务编号快照")
    private String businessNo;

    /** 业务标题快照，用于时间轴快速识别记录。 */
    @Column(nullable = false, length = 300)
    @Comment("业务标题快照")
    private String businessTitle;

    /** 操作动作，例如 CREATE、UPDATE、POST、VOID、STATUS_CHANGE、DELETE。 */
    @Column(nullable = false, length = 60)
    @Comment("操作动作")
    private String action;

    /** 操作动作中文名称。 */
    @Column(nullable = false, length = 120)
    @Comment("操作动作中文名称")
    private String actionName;

    /** 操作详情，直接面向用户描述做了什么。 */
    @Column(nullable = false, length = 1000)
    @Comment("操作详情")
    private String detail;

    /** 操作前状态或关键值快照。 */
    @Column(length = 120)
    @Comment("操作前状态或关键值快照")
    private String fromState;

    /** 操作后状态或关键值快照。 */
    @Column(length = 120)
    @Comment("操作后状态或关键值快照")
    private String toState;

    /** 操作参数或业务快照，使用 JSON 字符串保存；容量覆盖采购、库存、应收应付等模块新增字段。 */
    @Column(length = 10000)
    @Comment("操作参数或业务快照")
    private String snapshot;

    /** 操作人员主键。 */
    @Comment("操作人员主键")
    private Long operatorId;

    /** 操作人员账号。 */
    @Column(length = 80)
    @Comment("操作人员账号")
    private String operatorUsername;

    /** 操作人员姓名。 */
    @Column(length = 120)
    @Comment("操作人员姓名")
    private String operatorName;

    /** 操作发生时间。 */
    @Column(nullable = false)
    @Comment("操作发生时间")
    private OffsetDateTime operationTime;

    /**
     * 执行 getBusinessType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBusinessType() {
        return businessType;
    }

    /**
     * 执行 setBusinessType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    /**
     * 获取业务流水所属公司编码。
     *
     * <p>实现步骤：直接返回记录流水时写入的当前公司编码，查看流水和分页查询均按该字段隔离。</p>
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 设置业务流水所属公司编码。
     *
     * <p>实现步骤：记录业务操作时写入当前登录公司编码，保证不同公司业务时间轴互不可见。</p>
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 执行 getBusinessId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getBusinessId() {
        return businessId;
    }

    /**
     * 执行 setBusinessId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    /**
     * 执行 getBusinessNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBusinessNo() {
        return businessNo;
    }

    /**
     * 执行 setBusinessNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    /**
     * 执行 getBusinessTitle 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBusinessTitle() {
        return businessTitle;
    }

    /**
     * 执行 setBusinessTitle 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBusinessTitle(String businessTitle) {
        this.businessTitle = businessTitle;
    }

    /**
     * 执行 getAction 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getAction() {
        return action;
    }

    /**
     * 执行 setAction 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 执行 getActionName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * 执行 setActionName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    /**
     * 执行 getDetail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 执行 setDetail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * 执行 getFromState 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getFromState() {
        return fromState;
    }

    /**
     * 执行 setFromState 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setFromState(String fromState) {
        this.fromState = fromState;
    }

    /**
     * 执行 getToState 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getToState() {
        return toState;
    }

    /**
     * 执行 setToState 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setToState(String toState) {
        this.toState = toState;
    }

    /**
     * 执行 getSnapshot 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getSnapshot() {
        return snapshot;
    }

    /**
     * 执行 setSnapshot 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * 执行 getOperatorId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getOperatorId() {
        return operatorId;
    }

    /**
     * 执行 setOperatorId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    /**
     * 执行 getOperatorUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperatorUsername() {
        return operatorUsername;
    }

    /**
     * 执行 setOperatorUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    /**
     * 执行 getOperatorName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * 执行 setOperatorName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    /**
     * 执行 getOperationTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getOperationTime() {
        return operationTime;
    }

    /**
     * 执行 setOperationTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationTime(OffsetDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
