package com.ratel.fm.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

/**
 * 所有业务实体的公共父类。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@MappedSuperclass
public abstract class BaseEntity {

    /** 数据库主键，使用 PostgreSQL identity/serial 风格自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    /** 记录新增时间，由 JPA 在首次持久化前自动写入，数据库字段名为 created_time。 */
    @Column(name = "created_time", nullable = false, updatable = false)
    @Comment("记录创建时间")
    private OffsetDateTime createdTime;

    /** 记录修改时间，由 JPA 在新增和更新前自动维护，数据库字段名为 modify_time。 */
    @Column(name = "modify_time", nullable = false)
    @Comment("记录最近更新时间")
    private OffsetDateTime modifyTime;

    /**
     * 新增实体前初始化审计时间。
     *
     * <p>实现步骤：
     * 1. 获取当前服务端时间；
     * 2. 同时写入创建时间和更新时间，确保新增记录两个时间一致。</p>
     */
    @PrePersist
    protected void onCreate() {
        // 变量说明：now 保存当前步骤计算、查询或转换得到的中间结果。
        OffsetDateTime now = OffsetDateTime.now();
        createdTime = now;
        modifyTime = now;
    }

    /**
     * 更新实体前刷新更新时间。
     *
     * <p>实现步骤：在 JPA flush 更新 SQL 前，把 modifyTime 改为当前服务端时间。</p>
     */
    @PreUpdate
    protected void onUpdate() {
        modifyTime = OffsetDateTime.now();
    }

    /**
     * 执行 getId 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public Long getId() {
        return id;
    }

    /**
     * 执行 getCreatedTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * 执行 getModifyTime 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public OffsetDateTime getModifyTime() {
        return modifyTime;
    }
}
