package com.ratel.fm.common.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Locale;

/**
 * 客户端 IP 解析工具。
 *
 * <p>优先读取代理转发头；代理头无有效地址时使用连接的远端地址。服务端不能把自身局域网地址冒充客户端地址。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public final class ClientIpUtils {

    /**
     * 常见反向代理转发头名称，按可信度和常用度依次尝试解析客户端 IP。
     */
    private static final List<String> HEADER_NAMES = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "Forwarded"
    );

    /**
     * 私有构造方法，禁止实例化纯工具类。
     */
    private ClientIpUtils() {
    }

    /**
     * 解析客户端真实 IP。
     *
     * <p>实现步骤：
     * 1. 依次读取常见代理头；
     * 2. X-Forwarded-For 取第一个非 unknown 地址，Forwarded 解析 for= 片段；
     * 3. 代理头无效时使用 request remoteAddr；
     * 4. 如果结果是回环地址，尝试回退本机局域网 IPv4。</p>
     */
    public static String resolve(HttpServletRequest request) {
        for (String headerName : HEADER_NAMES) {
            // 变量说明：candidate 保存当前步骤计算、查询或转换得到的中间结果。
            String candidate = firstValidIp(request.getHeader(headerName));
            if (candidate != null) {
                return candidate;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 从请求头原始值中提取第一个有效 IP。
     */
    private static String firstValidIp(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        // 变量说明：normalized 保存当前步骤计算、查询或转换得到的中间结果。
        String normalized = rawValue;
        if (rawValue.toLowerCase(Locale.ROOT).contains("for=")) {
            normalized = rawValue.replace("for=", "")
                    .replace("\"", "")
                    .replace("[", "")
                    .replace("]", "");
        }
        for (String part : normalized.split(",")) {
            // 变量说明：ip 保存当前步骤计算、查询或转换得到的中间结果。
            String ip = part.trim();
            // 变量说明：forwardedParameter 保存当前步骤计算、查询或转换得到的中间结果。
            int forwardedParameter = ip.indexOf(';');
            if (forwardedParameter > -1) {
                ip = ip.substring(0, forwardedParameter).trim();
            }
            if (!ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // 变量说明：portSeparator 保存当前步骤计算、查询或转换得到的中间结果。
                int portSeparator = ip.lastIndexOf(':');
                if (portSeparator > -1 && ip.indexOf(':') == portSeparator) {
                    ip = ip.substring(0, portSeparator);
                }
                return ip;
            }
        }
        return null;
    }

}
