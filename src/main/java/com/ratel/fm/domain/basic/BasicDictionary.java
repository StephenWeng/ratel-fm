package com.ratel.fm.domain.basic;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 基础信息字典。
 *
 * <p>用于集中维护采购方、物流方等基础资料。业务模块只展示启用字典，停用字典保留历史数据但不再用于新业务选择。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Entity
@Table(name = "fm_basic_dictionaries")
@Comment("基础信息字典表，维护采购方、物流方等层级基础资料")
public class BasicDictionary extends BaseEntity {

    /** 字典编码。用户未填写时由服务端随机生成；系统预置根字典使用固定编码方便业务引用。 */
    @Column(nullable = false, unique = true, length = 80)
    @Comment("字典编码，全系统唯一；未填写时由服务端随机生成")
    private String code;

    /** 字典名称。同一父级下唯一，不同层级允许重复。 */
    @Column(nullable = false, length = 120)
    @Comment("字典名称，同一父级下唯一")
    private String name;

    /** 父级字典。为空表示一级字典；采购管理和物流管理引用固定一级字典下的启用子项。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Comment("父级字典ID")
    private BasicDictionary parent;

    /** 排序号，越小越靠前。 */
    @Column(nullable = false)
    @Comment("排序号，越小越靠前")
    private int sortOrder;

    /** 是否启用。禁用后不会出现在采购、物流等业务下拉选择中。 */
    @Column(nullable = false)
    @Comment("是否启用")
    private boolean enabled = true;

    /** 字典说明，用于记录供应商、承运商或其他基础资料的业务备注，支持长文本说明。 */
    @Column(length = 2000)
    @Comment("字典说明")
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
     * 执行 getParent 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BasicDictionary getParent() {
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
    public void setParent(BasicDictionary parent) {
        this.parent = parent;
    }

    /**
     * 执行 getSortOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * 执行 setSortOrder 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
