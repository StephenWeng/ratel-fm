package com.ratel.fm.common;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * JPA 字段级搜索工具。
 *
 * <p>实现目的：统一管理列表页的“输入框 like、下拉/字典等值、日期范围”查询规则，避免不同模块重复手写条件。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public final class SearchSpecs {

    private SearchSpecs() {
    }

    /**
     * 字符串包含匹配。
     *
     * <p>实现步骤：空文本不参与查询；非空文本转换为小写后拼接 `%`，数据库字段也 lower 后执行 like。</p>
     */
    public static <T> Specification<T> like(String field, String value) {
        // 变量说明：text 保存当前步骤计算、查询或转换得到的中间结果。
        String text = normalize(value);
        if (text == null) {
            return unrestricted();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(path(root, field)), "%" + text + "%");
    }

    /**
     * 枚举或确定值等值匹配。
     *
     * <p>实现步骤：值为空时不参与查询；有值时按字段真实类型执行 equal，供状态、类别、启用状态等确定性条件使用。</p>
     */
    public static <T, V> Specification<T> equal(String field, V value) {
        if (value == null) {
            return unrestricted();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(path(root, field), value);
    }

    /**
     * 日期字段起止范围。
     *
     * <p>实现步骤：开始日期按大于等于过滤；结束日期按小于等于过滤；两端都为空时不参与查询。</p>
     */
    public static <T> Specification<T> dateBetween(String field, LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return unrestricted();
        }
        return (root, query, criteriaBuilder) -> {
            // 变量说明：datePath 保存当前步骤计算、查询或转换得到的中间结果。
            Path<LocalDate> datePath = path(root, field);
            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(datePath, startDate, endDate);
            }
            return startDate != null
                    ? criteriaBuilder.greaterThanOrEqualTo(datePath, startDate)
                    : criteriaBuilder.lessThanOrEqualTo(datePath, endDate);
        };
    }

    /**
     * 空条件。
     */
    public static <T> Specification<T> unrestricted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    /**
     * 执行 normalize 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    @SuppressWarnings("unchecked")
    /**
     * 执行 path 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private static <T, V> Path<V> path(Root<T> root, String field) {
        // 变量说明：segments 保存当前步骤计算、查询或转换得到的中间结果。
        String[] segments = field.split("\\.");
        // 变量说明：current 保存当前步骤计算、查询或转换得到的中间结果。
        Path<?> current = root;
        for (String segment : segments) {
            current = current instanceof Root<?> rootPath
                    ? rootPath.get(segment)
                    : current.get(segment);
        }
        return (Path<V>) current;
    }
}
