package com.ratel.fm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 联系方式校验注解。
 *
 * <p>实现步骤：
 * 1. 允许手机号；
 * 2. 允许带区号的座机号，可带横线和分机号；
 * 3. 空值是否允许由业务 DTO 的必填注解决定。</p>
 */
@Documented
@Constraint(validatedBy = ContactPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContactPhone {

    /**
     * 校验失败时返回给接口调用方的提示语。
     */
    String message() default "联系方式必须为手机号或座机号";

    /**
     * Bean Validation 分组。
     */
    Class<?>[] groups() default {};

    /**
     * Bean Validation 负载信息。
     */
    Class<? extends Payload>[] payload() default {};
}
