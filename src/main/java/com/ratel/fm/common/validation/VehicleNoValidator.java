package com.ratel.fm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 中国车牌号校验器。
 *
 * <p>实现步骤：
 * 1. 空值放行，便于物流单未录入车辆时先保存；
 * 2. 将英文字母统一转大写后校验；
 * 3. 正则同时覆盖 7 位普通车牌和 8 位新能源/电动车牌。</p>
 */
public class VehicleNoValidator implements ConstraintValidator<VehicleNo, String> {

    /**
     * 车牌号格式正则，覆盖普通燃油车牌、新能源车牌和电动车牌常用位数。
     */
    private static final Pattern VEHICLE_NO_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳领试超]$"
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
        return value == null || value.isBlank()
                || VEHICLE_NO_PATTERN.matcher(value.trim().toUpperCase(Locale.ROOT)).matches();
    }
}
