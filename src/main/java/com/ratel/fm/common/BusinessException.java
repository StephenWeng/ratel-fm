package com.ratel.fm.common;

import org.springframework.http.HttpStatus;

/**
 * BusinessException 类。
 * 
 * <p>用于承载 BusinessException 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class BusinessException extends RuntimeException {

    /**
     * 字段 status：保存 status 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final HttpStatus status;
    /**
     * 字段 code：保存 code 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final ResponseCode code;

    /**
     * 构造 BusinessException 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, ResponseCode.FAILED, message);
    }

    /**
     * 构造 BusinessException 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BusinessException(HttpStatus status, String message) {
        this(status, ResponseCode.FAILED, message);
    }

    /**
     * 构造 BusinessException 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BusinessException(ResponseCode code) {
        this(HttpStatus.BAD_REQUEST, code, code.message());
    }

    /**
     * 构造 BusinessException 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BusinessException(ResponseCode code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }

    /**
     * 构造 BusinessException 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BusinessException(HttpStatus status, ResponseCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * 执行 getStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 执行 getCode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseCode getCode() {
        return code;
    }
}
