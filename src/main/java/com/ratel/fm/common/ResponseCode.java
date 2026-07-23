package com.ratel.fm.common;

/**
 * 统一响应码。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public enum ResponseCode {
    /**
     * 枚举值 SUCCESS：表示 SUCCESS 对应的业务状态或类型。
     */
    SUCCESS("200", "操作成功"),
    /**
     * 枚举值 WARN：表示 WARN 对应的业务状态或类型。
     */
    WARN("201", "警告"),
    /**
     * 枚举值 FAILED：表示 FAILED 对应的业务状态或类型。
     */
    FAILED("999999", "操作失败"),
    /**
     * 枚举值 ILLEGAL_PARAM：表示 ILLEGAL_PARAM 对应的业务状态或类型。
     */
    ILLEGAL_PARAM("000001", "非法的参数"),
    /**
     * 枚举值 NO_MAPPING_RESULT：表示 NO_MAPPING_RESULT 对应的业务状态或类型。
     */
    NO_MAPPING_RESULT("000002", "没有对应的结果数据"),
    /**
     * 枚举值 REF_OBJ_NOT_EXISIT：表示 REF_OBJ_NOT_EXISIT 对应的业务状态或类型。
     */
    REF_OBJ_NOT_EXISIT("000003", "查询的数据不存在"),
    /**
     * 枚举值 OBJ_BEEN_USED：表示 OBJ_BEEN_USED 对应的业务状态或类型。
     */
    OBJ_BEEN_USED("000004", "已经被使用"),
    /**
     * 枚举值 DELETE_FORBIDDEN：表示 DELETE_FORBIDDEN 对应的业务状态或类型。
     */
    DELETE_FORBIDDEN("000005", "不允许被删除"),
    /**
     * 枚举值 MODIFY_FORBIDDEN：表示 MODIFY_FORBIDDEN 对应的业务状态或类型。
     */
    MODIFY_FORBIDDEN("000006", "不允许被修改"),
    /**
     * 枚举值 CLASS_CAST_ERROR：表示 CLASS_CAST_ERROR 对应的业务状态或类型。
     */
    CLASS_CAST_ERROR("000007", "参数类型转换错误"),
    /**
     * 枚举值 DATABASE_OPERATION_ERROR：表示 DATABASE_OPERATION_ERROR 对应的业务状态或类型。
     */
    DATABASE_OPERATION_ERROR("000008", "数据库操作错误"),
    /**
     * 枚举值 USER_INFO_NULL：表示 USER_INFO_NULL 对应的业务状态或类型。
     */
    USER_INFO_NULL("000009", "用户信息为空"),
    /**
     * 枚举值 LOAD_CLIENT_ERROR：表示 LOAD_CLIENT_ERROR 对应的业务状态或类型。
     */
    LOAD_CLIENT_ERROR("000010", "访问服务错误"),
    /**
     * 枚举值 LOGOUT_ERROR：表示 LOGOUT_ERROR 对应的业务状态或类型。
     */
    LOGOUT_ERROR("401", "退出失败"),
    /**
     * 枚举值 NO_TOKEN_ERROR：表示 NO_TOKEN_ERROR 对应的业务状态或类型。
     */
    NO_TOKEN_ERROR("200001", "无认证信息"),
    /**
     * 枚举值 JWT_OVERTIME：表示 JWT_OVERTIME 对应的业务状态或类型。
     */
    JWT_OVERTIME("200002", "认证信息过期"),
    /**
     * 枚举值 FORCE_LOGOUT：表示 FORCE_LOGOUT 对应的业务状态或类型。
     */
    FORCE_LOGOUT("200003", "强制下线"),
    /**
     * 枚举值 NO_AUTH：表示 NO_AUTH 对应的业务状态或类型。
     */
    NO_AUTH("300001", "无权限访问"),
    /**
     * 枚举值 OUTER_REQ_NO_AUTH：表示 OUTER_REQ_NO_AUTH 对应的业务状态或类型。
     */
    OUTER_REQ_NO_AUTH("300002", "无权限访问"),
    /**
     * 枚举值 PASSWORD_ERROR：表示 PASSWORD_ERROR 对应的业务状态或类型。
     */
    PASSWORD_ERROR("400001", "密码错误"),
    /**
     * 枚举值 REPEAT_ERROR：表示 REPEAT_ERROR 对应的业务状态或类型。
     */
    REPEAT_ERROR("400002", "重复登录");

    /**
     * 字段 code：保存 code 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final String code;
    /**
     * 字段 message：保存 message 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 执行 code 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String code() {
        return code;
    }

    /**
     * 执行 message 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String message() {
        return message;
    }
}
