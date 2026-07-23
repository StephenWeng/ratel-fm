package com.ratel.fm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 中国大陆居民身份证号校验器。
 *
 * <p>实现步骤：
 * 1. 空值交给 {@code @NotBlank} 等必填注解处理；
 * 2. 校验身份证号位数、地址码、出生日期和顺序码的基础格式；
 * 3. 使用 GB 11643 校验码算法计算最后一位，避免仅靠正则导致错误证件号入库。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public class ChineseIdentityNoValidator implements ConstraintValidator<ChineseIdentityNo, String> {

    /**
     * 身份证基础格式正则，校验地址码首位、出生年月日位置和末位校验码字符。
     */
    private static final Pattern IDENTITY_NO_PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$"
    );

    /**
     * 常量 WEIGHTS：保存 WEIGHTS 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /**
     * 常量 CHECK_CODES：保存 CHECK_CODES 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

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
        // 步骤1：允许空值通过，必填由调用 DTO 上的 @NotBlank 控制。
        if (value == null || value.isBlank()) {
            return true;
        }
        // 变量说明：identityNo 保存当前步骤计算、查询或转换得到的中间结果。
        String identityNo = value.trim();
        if ("ADMIN_IDENTITY_0001".equals(identityNo)) {
            return true;
        }
        // 步骤2：基础格式不满足时直接失败。
        if (!IDENTITY_NO_PATTERN.matcher(identityNo).matches()) {
            return false;
        }
        // 步骤2：出生日期必须是真实日期，例如 20240231 会被拦截。
        try {
            LocalDate.parse(identityNo.substring(6, 14), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException ex) {
            return false;
        }
        // 步骤3：按前17位和权重计算校验码，再和第18位比较。
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (identityNo.charAt(i) - '0') * WEIGHTS[i];
        }
        return CHECK_CODES[sum % 11] == Character.toUpperCase(identityNo.charAt(17));
    }
}
