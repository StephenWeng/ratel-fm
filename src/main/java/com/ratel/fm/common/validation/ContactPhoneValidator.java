package com.ratel.fm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 联系方式校验器。
 *
 * <p>实现步骤：
 * 1. 空值放行，由 {@code @NotBlank} 控制必填；
 * 2. 手机号匹配中国大陆 11 位手机号；
 * 3. 座机号匹配区号加 7 到 8 位号码，可选分机号。</p>
 */
public class ContactPhoneValidator implements ConstraintValidator<ContactPhone, String> {

    /**
     * 联系方式格式正则，同时支持中国大陆手机号和带区号的座机号。
     */
    private static final Pattern CONTACT_PHONE_PATTERN = Pattern.compile(
            "^(?:1[3-9]\\d{9}|0\\d{2,3}-?\\d{7,8}(?:-\\d{1,6})?)$"
    );

    @Override
    /**
     * 执行 isValid 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || CONTACT_PHONE_PATTERN.matcher(value.trim()).matches();
    }
}
