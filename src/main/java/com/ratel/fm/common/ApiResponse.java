package com.ratel.fm.common;

import java.time.OffsetDateTime;

/**
 * REST API 统一响应体，所有响应均携带业务 code 和 message。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public record ApiResponse<T>(
        /**
         * 记录组件 success：表示接口入参或出参中的 success 字段。
         */
        boolean success,
        /**
         * 记录组件 code：表示接口入参或出参中的 code 字段。
         */
        String code,
        /**
         * 记录组件 message：表示接口入参或出参中的 message 字段。
         */
        String message,
        T data,
        /**
         * 记录组件 timestamp：表示接口入参或出参中的 timestamp 字段。
         */
        OffsetDateTime timestamp
) {

    /**
     * 执行 ok 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static <T> ApiResponse<T> ok(T data) {
        return of(ResponseCode.SUCCESS, true, ResponseCode.SUCCESS.message(), data);
    }

    /**
     * 执行 ok 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(ResponseCode.SUCCESS, true, message, data);
    }

    /**
     * 执行 fail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static ApiResponse<Void> fail(String message) {
        return fail(ResponseCode.FAILED, message);
    }

    /**
     * 执行 fail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static ApiResponse<Void> fail(ResponseCode code) {
        return fail(code, code.message());
    }

    /**
     * 执行 fail 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static ApiResponse<Void> fail(ResponseCode code, String message) {
        return of(code, false, message, null);
    }

    /**
     * 执行 warn 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static <T> ApiResponse<T> warn(ResponseCode code, String message, T data) {
        return of(code, false, message, data);
    }

    /**
     * 执行 of 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static <T> ApiResponse<T> of(ResponseCode code, boolean success, String message, T data) {
        return new ApiResponse<>(success, code.code(), message, data, OffsetDateTime.now());
    }
}
