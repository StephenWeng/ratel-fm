package com.ratel.fm.domain.finance;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 会计科目字典。
 *
 * <p>科目是凭证分录、试算平衡和三大报表汇总的基础维度。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_accounting_subjects")
@Comment("会计科目字典表，作为凭证分录、统计分析和财务报表的基础维度")
public class AccountingSubject extends BaseEntity {

    /** 科目编码，在同一所属公司内唯一，如 1001、6602。 */
    @Column(nullable = false, length = 40)
    @Comment("科目编码，同一所属公司内唯一")
    private String code;

    /** 所属公司字典编码，即账套编码，会计科目按该字段隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为会计科目账套隔离标识")
    private String organizationCode;

    /** 科目名称，如库存现金、管理费用。 */
    @Column(nullable = false, length = 120)
    @Comment("科目名称")
    private String name;

    /** 科目类别，用于报表汇总：资产、负债、权益、收入、成本、费用。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Comment("科目类别")
    private SubjectCategory category;

    /** 父级科目。为空表示一级科目，非空时组成科目树。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Comment("父级科目ID")
    private AccountingSubject parent;

    /** 科目层级。创建或修改时根据父级自动计算，便于前端展示。 */
    @Column(nullable = false)
    @Comment("科目层级")
    private int subjectLevel = 1;

    /** 是否启用。停用后不能作为新增凭证分录科目。 */
    @Column(nullable = false)
    @Comment("是否启用科目")
    private boolean enabled = true;

    /** 科目说明，记录企业自定义核算口径，支持长文本说明。 */
    @Column(length = 2000)
    @Comment("科目说明")
    private String description;

    /**
     * 执行 getCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getCode() {
        return code;
    }

    /**
     * 执行 setCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取科目所属公司编码。
     *
     * <p>实现步骤：直接返回科目创建时写入的账套编码，列表、凭证录入和报表查询均按该字段隔离。</p>
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 设置科目所属公司编码。
     *
     * <p>实现步骤：新增科目时写入当前登录公司的字典编码，保证不同公司可以维护各自的科目体系。</p>
     */
    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    /**
     * 执行 getName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getName() {
        return name;
    }

    /**
     * 执行 setName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 执行 getCategory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public SubjectCategory getCategory() {
        return category;
    }

    /**
     * 执行 setCategory 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCategory(SubjectCategory category) {
        this.category = category;
    }

    /**
     * 执行 getParent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public AccountingSubject getParent() {
        return parent;
    }

    /**
     * 执行 setParent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setParent(AccountingSubject parent) {
        this.parent = parent;
    }

    /**
     * 执行 getSubjectLevel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public int getSubjectLevel() {
        return subjectLevel;
    }

    /**
     * 执行 setSubjectLevel 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSubjectLevel(int subjectLevel) {
        this.subjectLevel = subjectLevel;
    }

    /**
     * 执行 isEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 执行 setEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 执行 getDescription 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getDescription() {
        return description;
    }

    /**
     * 执行 setDescription 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
