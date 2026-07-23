package com.ratel.fm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 一般中文姓名校验注解。
 *
 * <p>实现步骤：
 * 1. 由 {@link ChineseNameValidator} 判断空值和格式；
 * 2. 非空时只允许 1 到 20 个中文字符；
 * 3. 具体字段是否必填由 DTO 上的 {@code @NotBlank} 控制。</p>
 */
@Documented
@Constraint(validatedBy = ChineseNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChineseName {

    /**
     * 校验失败时返回给接口调用方的提示语。
     */
    String message() default "姓名必须为1到20个中文字符";

    /**
     * Bean Validation 分组。
     */
    Class<?>[] groups() default {};

    /**
     * Bean Validation 负载信息。
     */
    Class<? extends Payload>[] payload() default {};
}
