package com.ratel.fm.config.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * FastJson2 HTTP 消息转换器。
 *
 * <p>实现目的：后端 REST 接口统一使用阿里 FastJson2 进行 JSON 请求解析和响应输出。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
public class FastJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    /**
     * JSON 读取特性。
     *
     * <p>SupportSmartMatch 用于兼容前端字段大小写或命名轻微差异，TrimString 用于减少输入空格造成的参数误差。</p>
     */
    private static final JSONReader.Feature[] READER_FEATURES = {
            JSONReader.Feature.SupportSmartMatch,
            JSONReader.Feature.TrimString
    };

    /**
     * JSON 输出特性。
     *
     * <p>WriteMapNullValue 保持响应字段完整，WriteBigDecimalAsPlain 避免金额出现科学计数法。</p>
     */
    private static final JSONWriter.Feature[] WRITER_FEATURES = {
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteBigDecimalAsPlain
    };

    /**
     * 构造 FastJsonHttpMessageConverter 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public FastJsonHttpMessageConverter() {
        super(StandardCharsets.UTF_8, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    }

    /**
     * 读取 JSON 请求体并转换为控制器入参对象。
     *
     * <p>实现步骤：
     * 1. 读取 HTTP 请求体字节；
     * 2. 使用 FastJson2 按目标 Java 类型反序列化；
     * 3. 解析失败时包装为 Spring MVC 标准参数读取异常。</p>
     */
    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException {
        try {
            // 变量说明：body 保存当前步骤计算、查询或转换得到的中间结果。
            byte[] body = inputMessage.getBody().readAllBytes();
            return JSON.parseObject(body, type, READER_FEATURES);
        } catch (RuntimeException ex) {
            throw new HttpMessageNotReadableException("FastJson 解析请求体失败", ex, inputMessage);
        }
    }

    /**
     * 兼容非泛型读取入口。
     *
     * <p>实现步骤：把 Class 类型转交给泛型读取方法，保证普通 DTO 和泛型 DTO 使用同一套 FastJson 逻辑。</p>
     */
    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        return read(clazz, clazz, inputMessage);
    }

    /**
     * 写出控制器返回值。
     *
     * <p>实现步骤：
     * 1. 按 FastJson2 规则把对象序列化为 UTF-8 JSON 字节；
     * 2. 写入 HTTP 响应流；
     * 3. 序列化失败时包装为 Spring MVC 标准响应写出异常。</p>
     */
    @Override
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage) throws IOException {
        try {
            // 变量说明：body 保存当前步骤计算、查询或转换得到的中间结果。
            byte[] body = JSON.toJSONBytes(object, WRITER_FEATURES);
            // 变量说明：outputStream 保存当前步骤计算、查询或转换得到的中间结果。
            OutputStream outputStream = outputMessage.getBody();
            outputStream.write(body);
            outputStream.flush();
        } catch (RuntimeException ex) {
            throw new HttpMessageNotWritableException("FastJson 写出响应体失败", ex);
        }
    }
}
