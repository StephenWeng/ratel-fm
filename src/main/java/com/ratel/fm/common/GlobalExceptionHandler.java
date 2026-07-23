package com.ratel.fm.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.stream.Collectors;

@RestControllerAdvice
/**
 * GlobalExceptionHandler 类。
 * 
 * <p>用于承载 GlobalExceptionHandler 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
public class GlobalExceptionHandler {

    /** 服务端异常日志对象，用于记录真实技术异常，前端只接收业务化提示。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    /**
     * 执行 handleBusiness 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /**
     * 执行 handleValidation 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(ResponseCode.ILLEGAL_PARAM, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    /**
     * 执行 handleConstraint 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ResponseCode.ILLEGAL_PARAM, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    /**
     * 处理 URL 查询参数或路径参数类型错误。
     *
     * <p>实现步骤：
     * 1. 记录真实参数名和值，方便服务端排查；
     * 2. 返回用户能理解的参数格式提示，不暴露 Java 类型名称。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        LOGGER.warn("请求参数类型不正确。name={}, value={}, requiredType={}", ex.getName(), ex.getValue(), ex.getRequiredType(), ex);
        return ResponseEntity.badRequest().body(ApiResponse.fail(ResponseCode.ILLEGAL_PARAM, "请求参数格式不正确，请检查筛选条件或录入内容后重试。"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    /**
     * 处理请求体无法解析的错误。
     *
     * <p>实现步骤：请求 JSON 格式、日期格式或枚举值无法解析时，统一提示用户检查表单内容。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        LOGGER.warn("请求体解析失败。", ex);
        return ResponseEntity.badRequest().body(ApiResponse.fail(ResponseCode.ILLEGAL_PARAM, "提交的数据格式不正确，请检查表单内容后重新提交。"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    /**
     * 执行 handleBadCredentials 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ResponseCode.PASSWORD_ERROR, "用户名或密码错误"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    /**
     * 执行 handleAccessDenied 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(ResponseCode.NO_AUTH, "无访问权限"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    /**
     * 处理请求方法不支持的错误。
     *
     * <p>实现步骤：当页面使用了错误的 GET、POST、PUT 或 DELETE 方法时，提示刷新页面或联系管理员。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        LOGGER.warn("接口请求方法不支持。method={}, supported={}", ex.getMethod(), ex.getSupportedHttpMethods(), ex);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ResponseCode.ILLEGAL_PARAM, "当前功能请求方式不正确，请刷新页面后重试。"));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    /**
     * 处理接口或静态资源不存在。
     *
     * <p>实现步骤：HTTP 404 不向用户展示 Not Found 或资源路径，统一提示功能不存在或页面版本已过期。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        LOGGER.warn("请求的接口或资源不存在。", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ResponseCode.REF_OBJ_NOT_EXISIT, "当前访问的功能或资源不存在，请刷新页面后重试。"));
    }

    @ExceptionHandler({FileNotFoundException.class, NoSuchFileException.class})
    /**
     * 处理文件不存在。
     *
     * <p>实现步骤：文件系统路径、文件名等技术信息只写日志，前端统一提示附件或文件不存在。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleFileNotFound(Exception ex) {
        LOGGER.warn("文件不存在或已被移动。", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ResponseCode.REF_OBJ_NOT_EXISIT, "文件不存在或已被删除，请刷新页面后重试。"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    /**
     * 处理数据库唯一性、外键或非空约束错误。
     *
     * <p>实现步骤：将约束名、SQL 和字段细节保留在日志中，前端只展示可操作的业务提示。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        LOGGER.error("数据库约束校验失败。", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ResponseCode.DATABASE_OPERATION_ERROR, "数据保存失败，可能存在重复数据、必填项缺失或关联数据已被删除，请检查后重试。"));
    }

    @ExceptionHandler(DataAccessException.class)
    /**
     * 处理数据库访问异常。
     *
     * <p>实现步骤：捕获 SQL 执行、连接、锁等待等数据库异常，避免把 SQL 语句或驱动错误直接展示给用户。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
        LOGGER.error("数据库访问异常。", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ResponseCode.DATABASE_OPERATION_ERROR, "数据库暂时无法完成本次操作，请稍后重试或联系管理员。"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    /**
     * 执行 handleResponseStatus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        // 变量说明：status 保存当前步骤计算、查询或转换得到的中间结果。
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        // 变量说明：code 保存当前步骤计算、查询或转换得到的中间结果。
        ResponseCode code = status.is4xxClientError() ? ResponseCode.ILLEGAL_PARAM : ResponseCode.FAILED;
        // 变量说明：message 保存按 HTTP 状态转换后的用户可读提示，不直接返回英文状态或技术异常。
        String message = httpStatusMessage(status);
        LOGGER.warn("接口状态异常。status={}, reason={}", status.value(), ex.getReason(), ex);
        return ResponseEntity.status(status).body(ApiResponse.fail(code, message));
    }

    @ExceptionHandler(Exception.class)
    /**
     * 执行 handleException 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        LOGGER.error("系统未预期异常。", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ResponseCode.FAILED, "系统暂时无法完成本次操作，请稍后重试或联系管理员。"));
    }

    /**
     * 将 HTTP 状态转换为用户可理解的中文提示。
     *
     * <p>实现步骤：
     * 1. 常见 400、404、405、409、413、415、429 和 5xx 状态逐一转换；
     * 2. 未覆盖状态按 4xx 或 5xx 归类兜底；
     * 3. 不返回英文状态短语，避免用户看到技术细节。</p>
     */
    private String httpStatusMessage(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "请求内容不正确，请检查输入后重试。";
            case 403 -> "您没有权限执行当前操作。";
            case 404 -> "当前访问的功能或数据不存在，请刷新页面后重试。";
            case 405 -> "当前功能请求方式不正确，请刷新页面后重试。";
            case 409 -> "当前数据状态已变化，请刷新页面后重试。";
            case 413 -> "上传或提交的内容过大，请减少内容后重试。";
            case 415 -> "上传或提交的文件格式不支持，请更换文件后重试。";
            case 429 -> "当前操作过于频繁，请稍后再试。";
            case 500 -> "系统暂时无法完成本次操作，请稍后重试或联系管理员。";
            case 502, 503, 504 -> "外部服务或网络暂时不可用，请稍后重试。";
            default -> status.is4xxClientError()
                    ? "请求无法处理，请检查当前页面内容后重试。"
                    : "系统暂时无法完成本次操作，请稍后重试或联系管理员。";
        };
    }
}
