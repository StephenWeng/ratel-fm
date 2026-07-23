package com.ratel.fm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 一般中文姓名校验器。
 *
 * <p>实现步骤：
 * 1. 空值直接放行，交给必填注解处理；
 * 2. 非空值必须全部为中文字符；
 * 3. 长度限制为 1 到 20 个中文字符，避免人员字段保存过长文本。</p>
 */
public class ChineseNameValidator implements ConstraintValidator<ChineseName, String> {

    /**
     * 中文姓名格式正则，限制为 1 到 20 个中文字符。
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]{1,20}$");

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
        return value == null || value.isBlank() || NAME_PATTERN.matcher(value.trim()).matches();
    }
}
