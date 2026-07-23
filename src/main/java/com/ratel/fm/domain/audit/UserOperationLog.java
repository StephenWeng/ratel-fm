package com.ratel.fm.domain.audit;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 用户关键操作日志，落库保存业务关键操作的人员、终端、模块、功能、参数、结果和影响。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_user_operation_logs")
@Comment("用户关键操作日志表，记录业务关键操作的人员、终端、模块、功能、参数、结果和影响")
public class UserOperationLog extends BaseEntity {

    /** 操作人员主键。系统自动任务没有真实人员时为系统用户上下文。 */
    @Comment("操作人员主键")
    private Long operatorId;

    /** 操作人员登录账号。 */
    @Column(length = 80)
    @Comment("操作人员登录账号")
    private String operatorUsername;

    /** 操作人员姓名，用于审计报表直接展示。 */
    @Column(length = 120)
    @Comment("操作人员姓名")
    private String operatorName;

    /** 操作人员身份证号，用于审计查询中按人员唯一身份追踪。 */
    @Column(length = 40)
    @Comment("操作人员身份证号")
    private String identityNo;

    /** 所属公司字典编码，操作日志按登录账套隔离查询。 */
    @Column(length = 80)
    @Comment("所属公司字典编码，作为操作日志账套隔离标识")
    private String organizationCode;

    /** 操作人员部门，来自当前 JWT 人员信息。 */
    @Column(length = 80)
    @Comment("操作人员部门")
    private String department;

    /** 操作人员联系方式，来自当前 JWT 人员信息。 */
    @Column(length = 40)
    @Comment("操作人员联系方式")
    private String contactPhone;

    /** 操作终端类型，取值 PC 或 APP。 */
    @Column(length = 20)
    @Comment("操作终端类型")
    private String terminalType;

    /** 操作终端标识。PC 为 IP，APP 为手机号。 */
    @Column(length = 120)
    @Comment("操作终端标识")
    private String terminalIdentifier;

    /** 操作模块，如系统管理、财务管理、业务管理。 */
    @Column(length = 80)
    @Comment("操作模块")
    private String operationModule;

    /** 操作功能，如人员维护、凭证记账、角色授权。 */
    @Column(length = 120)
    @Comment("操作功能")
    private String operationFunction;

    /** 业务操作发生时间。 */
    @Column(nullable = false)
    @Comment("业务操作发生时间")
    private OffsetDateTime operationTime;

    /** 操作动作编码，如 CREATE_VOUCHER、POST_VOUCHER、SAVE_ROLE。 */
    @Column(nullable = false, length = 120)
    @Comment("操作动作编码")
    private String action;

    /** 操作请求参数或关键业务参数，保存前会截断到字段长度。 */
    @Column(length = 2000)
    @Comment("操作请求参数或关键业务参数")
    private String operationParameters;

    /** 操作是否成功，便于日志查询页面按成功或失败聚合过滤。 */
    @Comment("操作是否成功")
    private Boolean success;

    /** 操作结果，如 SUCCESS、FAILED。当前关键写操作成功后记录 SUCCESS。 */
    @Column(length = 80)
    @Comment("操作结果")
    private String operationResult;

    /** 操作响应值，记录业务接口返回给调用方的关键结果或数据库日志异常说明。 */
    @Column(length = 2000)
    @Comment("操作响应值")
    private String responseValue;

    /** 操作造成的业务影响说明，用于后续审计人员快速判断影响范围。 */
    @Column(length = 1000)
    @Comment("操作造成的业务影响说明")
    private String impact;

    /**
     * 执行 getOperatorId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getOperatorId() { return operatorId; }
    /**
     * 执行 getOperatorUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperatorUsername() { return operatorUsername; }
    /**
     * 执行 getOperatorName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperatorName() { return operatorName; }
    /**
     * 执行 getIdentityNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getIdentityNo() { return identityNo; }
    /**
     * 获取操作日志所属公司编码。
     *
     * <p>实现步骤：直接返回操作发生时当前登录人的公司编码，日志列表按该字段隔离。</p>
     */
    public String getOrganizationCode() { return organizationCode; }
    /**
     * 执行 getDepartment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDepartment() { return department; }
    /**
     * 执行 getContactPhone 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getContactPhone() { return contactPhone; }
    /**
     * 执行 getTerminalType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getTerminalType() { return terminalType; }
    /**
     * 执行 getTerminalIdentifier 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getTerminalIdentifier() { return terminalIdentifier; }
    /**
     * 执行 getOperationModule 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperationModule() { return operationModule; }
    /**
     * 执行 getOperationFunction 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperationFunction() { return operationFunction; }
    /**
     * 执行 getOperationTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getOperationTime() { return operationTime; }
    /**
     * 执行 getAction 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getAction() { return action; }
    /**
     * 执行 getOperationParameters 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperationParameters() { return operationParameters; }
    /**
     * 执行 getSuccess 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Boolean getSuccess() { return success; }
    /**
     * 执行 getOperationResult 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getOperationResult() { return operationResult; }
    /**
     * 执行 getResponseValue 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getResponseValue() { return responseValue; }
    /**
     * 执行 getImpact 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getImpact() { return impact; }

    /**
     * 执行 setOperatorId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    /**
     * 执行 setOperatorUsername 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }
    /**
     * 执行 setOperatorName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    /**
     * 执行 setIdentityNo 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
    /**
     * 设置操作日志所属公司编码。
     *
     * <p>实现步骤：记录日志时写入当前登录人的公司编码，保证审计查询只展示当前公司操作。</p>
     */
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }
    /**
     * 执行 setDepartment 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDepartment(String department) { this.department = department; }
    /**
     * 执行 setContactPhone 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    /**
     * 执行 setTerminalType 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTerminalType(String terminalType) { this.terminalType = terminalType; }
    /**
     * 执行 setTerminalIdentifier 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setTerminalIdentifier(String terminalIdentifier) { this.terminalIdentifier = terminalIdentifier; }
    /**
     * 执行 setOperationModule 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationModule(String operationModule) { this.operationModule = operationModule; }
    /**
     * 执行 setOperationFunction 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationFunction(String operationFunction) { this.operationFunction = operationFunction; }
    /**
     * 执行 setOperationTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationTime(OffsetDateTime operationTime) { this.operationTime = operationTime; }
    /**
     * 执行 setAction 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setAction(String action) { this.action = action; }
    /**
     * 执行 setOperationParameters 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationParameters(String operationParameters) { this.operationParameters = operationParameters; }
    /**
     * 执行 setSuccess 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSuccess(Boolean success) { this.success = success; }
    /**
     * 执行 setOperationResult 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setOperationResult(String operationResult) { this.operationResult = operationResult; }
    /**
     * 执行 setResponseValue 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setResponseValue(String responseValue) { this.responseValue = responseValue; }
    /**
     * 执行 setImpact 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setImpact(String impact) { this.impact = impact; }
}
