package com.ratel.fm.common.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * 客户端 IP 解析工具。
 *
 * <p>优先读取代理转发头；直接本机访问时如果只能拿到回环地址，则回退到本机局域网 IPv4，避免 PC 终端标识长期记录为 127.0.0.1。</p>
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
                return normalizeLoopback(candidate);
            }
        }
        return normalizeLoopback(request.getRemoteAddr());
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

    /**
     * 回环地址回退为本机局域网 IPv4。
     */
    private static String normalizeLoopback(String ip) {
        if (!isLoopback(ip)) {
            return ip;
        }
        // 变量说明：localAddress 保存当前步骤计算、查询或转换得到的中间结果。
        String localAddress = firstLocalIpv4();
        return localAddress == null ? ip : localAddress;
    }

    /**
     * 判断是否是本机回环地址。
     */
    private static boolean isLoopback(String ip) {
        return ip == null
                || ip.isBlank()
                || "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip)
                || "localhost".equalsIgnoreCase(ip);
    }

    /**
     * 获取本机第一个可用局域网 IPv4。
     */
    private static String firstLocalIpv4() {
        try {
            // 变量说明：interfaces 保存当前步骤计算、查询或转换得到的中间结果。
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                // 变量说明：networkInterface 保存当前步骤计算、查询或转换得到的中间结果。
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                // 变量说明：addresses 保存当前步骤计算、查询或转换得到的中间结果。
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    // 变量说明：address 保存当前步骤计算、查询或转换得到的中间结果。
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
