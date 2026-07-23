package com.ratel.fm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 中国车牌号校验注解。
 *
 * <p>实现步骤：
 * 1. 兼容普通燃油车 7 位车牌；
 * 2. 兼容新能源/电动车 8 位车牌；
 * 3. 支持常见特殊尾字，例如挂、学、警、港、澳、领、试、超。</p>
 */
@Documented
@Constraint(validatedBy = VehicleNoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface VehicleNo {

    /**
     * 校验失败时返回给接口调用方的提示语。
     */
    String message() default "车牌号格式不正确";

    /**
     * Bean Validation 分组。
     */
    Class<?>[] groups() default {};

    /**
     * Bean Validation 负载信息。
     */
    Class<? extends Payload>[] payload() default {};
}
