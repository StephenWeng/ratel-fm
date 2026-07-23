package com.ratel.fm.security;

import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import org.springframework.data.jpa.domain.Specification;

import java.util.Objects;

/**
 * 当前登录所属公司的数据隔离工具。
 *
 * <p>实现目的：
 * 1. 统一从 JWT Cookie 还原的 CurrentUser 中读取所属公司编码；
 * 2. 为各业务模块提供同一套 company/account-set 查询条件；
 * 3. 在修改、删除、查看单条数据前校验记录所属公司，避免跨公司操作。</p>
 */
public final class CompanyScope {

    /** 系统预置所属公司字典编码，兼容初始化管理员和首次启动种子数据。 */
    public static final String DEFAULT_COMPANY_CODE = "ORGANIZATION_RATEL";

    private CompanyScope() {
    }

    /**
     * 读取当前登录人的所属公司编码。
     *
     * <p>实现步骤：
     * 1. 从 SecurityUtils 读取 JWT 中的 CurrentUser；
     * 2. 已登录人员返回 JWT 中的 organizationCode；
     * 3. 系统初始化或历史空值场景回退到预置公司编码，避免启动任务无法写入公司字段。</p>
     */
    public static String currentCompanyCode() {
        String organizationCode = SecurityUtils.currentUser().organizationCode();
        return hasText(organizationCode) ? organizationCode.trim() : DEFAULT_COMPANY_CODE;
    }

    /**
     * 判断当前登录人是否为系统超级管理员。
     *
     * <p>实现步骤：默认管理员账号 admin 可以跨公司维护人员和所属公司字典；业务数据仍按其登录公司隔离。</p>
     */
    public static boolean isSuperAdmin() {
        CurrentUser user = SecurityUtils.currentUser();
        return user != null && "admin".equalsIgnoreCase(user.username());
    }

    /**
     * 构建当前所属公司的 JPA 查询条件。
     *
     * <p>实现步骤：按实体 organizationCode 字段等值过滤，所有列表、导出和统计查询复用该条件。</p>
     */
    public static <T> Specification<T> currentCompanySpec() {
        String companyCode = currentCompanyCode();
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("organizationCode"), companyCode);
    }

    /**
     * 校验记录是否属于当前登录公司。
     *
     * <p>实现步骤：记录公司为空或与当前公司不同均拒绝访问，使接口表现为“数据不存在或无权访问”。</p>
     */
    public static void requireCurrentCompany(String recordCompanyCode, String objectName) {
        if (!Objects.equals(currentCompanyCode(), recordCompanyCode)) {
            throw new BusinessException(ResponseCode.NO_AUTH, objectName + "不属于当前所属公司，不能访问或操作");
        }
    }

    /**
     * 规范化公司编码。
     *
     * <p>实现步骤：去除首尾空格；空值回退预置公司编码；非空值原样返回，保证字典编码稳定保存。</p>
     */
    public static String normalizeCompanyCode(String companyCode) {
        return hasText(companyCode) ? companyCode.trim() : DEFAULT_COMPANY_CODE;
    }

    /**
     * 判断文本是否包含非空白字符。
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
