package com.ratel.fm.config.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JJWT 使用的 FastJson2 序列化和反序列化适配器。
 *
 * <p>实现目的：JWT Header 和 Claims 的 JSON 编解码显式使用阿里 FastJson2。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
public class FastJsonJwtCodec implements Serializer<Map<String, ?>>, Deserializer<Map<String, ?>> {

    /**
     * JWT JSON Map 类型。
     *
     * <p>LinkedHashMap 用于保留写入顺序，便于排查令牌内容和生成稳定的签名输入。</p>
     */
    private static final Type JWT_MAP_TYPE = new TypeReference<LinkedHashMap<String, Object>>() {
    }.getType();

    /**
     * JWT 序列化特性。
     *
     * <p>MapSortField 让 Header 和 Claims 输出稳定，WriteBigDecimalAsPlain 避免数值字段出现科学计数法。</p>
     */
    private static final JSONWriter.Feature[] WRITER_FEATURES = {
            JSONWriter.Feature.MapSortField,
            JSONWriter.Feature.WriteBigDecimalAsPlain
    };

    /**
     * JWT 反序列化特性。
     *
     * <p>UseBigDecimalForDoubles 保持小数读取精度，TrimString 清理字符串首尾空白。</p>
     */
    private static final JSONReader.Feature[] READER_FEATURES = {
            JSONReader.Feature.UseBigDecimalForDoubles,
            JSONReader.Feature.TrimString
    };

    /**
     * 把 JWT Header 或 Claims 序列化为 JSON 字节。
     *
     * <p>实现步骤：
     * 1. 接收 JJWT 传入的 Header 或 Claims Map；
     * 2. 使用 FastJson2 生成 UTF-8 JSON 字节；
     * 3. 序列化异常转为 JJWT 标准 SerializationException。</p>
     */
    @Override
    public byte[] serialize(Map<String, ?> map) {
        try {
            return JSON.toJSONBytes(map, WRITER_FEATURES);
        } catch (RuntimeException ex) {
            throw new SerializationException("FastJson 序列化 JWT 数据失败", ex);
        }
    }

    /**
     * 把 JWT Header 或 Claims 写入输出流。
     *
     * <p>实现步骤：先复用字节序列化逻辑生成 JSON，再写入 JJWT 提供的输出流。</p>
     */
    @Override
    public void serialize(Map<String, ?> map, OutputStream out) {
        try {
            out.write(serialize(map));
        } catch (IOException ex) {
            throw new SerializationException("FastJson 写入 JWT 数据失败", ex);
        }
    }

    /**
     * 从 JWT JSON 字节读取 Map。
     *
     * <p>实现步骤：
     * 1. 按 UTF-8 解析 JWT Header 或 Claims；
     * 2. 返回 LinkedHashMap；
     * 3. 解析异常转为 JJWT 标准 DeserializationException。</p>
     */
    @Override
    public Map<String, ?> deserialize(byte[] bytes) {
        try {
            return JSON.parseObject(bytes, JWT_MAP_TYPE, READER_FEATURES);
        } catch (RuntimeException ex) {
            throw new DeserializationException("FastJson 解析 JWT 数据失败", ex);
        }
    }

    /**
     * 从 Reader 中读取 JWT JSON。
     *
     * <p>实现步骤：读取 Reader 中的全部字符，转为 UTF-8 字节后复用统一的 JWT 反序列化逻辑。</p>
     */
    @Override
    public Map<String, ?> deserialize(Reader reader) {
        try {
            // 变量说明：builder 保存当前步骤计算、查询或转换得到的中间结果。
            StringBuilder builder = new StringBuilder();
            // 变量说明：buffer 保存当前步骤计算、查询或转换得到的中间结果。
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }
            return deserialize(builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new DeserializationException("FastJson 读取 JWT 数据失败", ex);
        }
    }
}
