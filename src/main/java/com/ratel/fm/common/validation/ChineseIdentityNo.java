package com.ratel.fm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 中国大陆居民身份证号格式校验注解。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Documented
@Constraint(validatedBy = ChineseIdentityNoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChineseIdentityNo {

    /**
     * 校验失败时返回给接口调用方的提示语。
     */
    String message() default "身份证号格式不正确";

    /**
     * Bean Validation 分组。
     */
    Class<?>[] groups() default {};

    /**
     * Bean Validation 负载信息。
     */
    Class<? extends Payload>[] payload() default {};
}
