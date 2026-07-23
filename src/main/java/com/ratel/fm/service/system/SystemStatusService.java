package com.ratel.fm.service.system;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.config.system.SystemStatusProperties;
import com.ratel.fm.web.dto.system.SystemStatusDtos.SystemStatusView;
import com.ratel.fm.web.dto.system.SystemStatusDtos.WeatherHourlyView;
import com.ratel.fm.web.dto.system.SystemStatusDtos.WeatherView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统状态服务。
 *
 * <p>用于右上角统一展示服务器时间、当前天气和未来小时天气列表。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class SystemStatusService {

    /** 天气来源名称，前端悬浮面板展示给用户。 */
    private static final String WEATHER_SOURCE = "Open-Meteo";

    /** 浏览器定位来源名称，前端用于区分当前位置和配置兜底位置。 */
    private static final String LOCATION_SOURCE_BROWSER = "BROWSER";

    /** 公网 IP 粗定位来源名称，适用于 HTTP IP 访问时浏览器无法弹出精确定位授权的场景。 */
    private static final String LOCATION_SOURCE_IP = "IP";

    /** 配置兜底位置来源名称。 */
    private static final String LOCATION_SOURCE_CONFIG = "CONFIG";

    /** 经纬度反查地址接口，失败时只影响位置名称，不影响天气查询。 */
    private static final String REVERSE_GEOCODE_ENDPOINT = "https://nominatim.openstreetmap.org/reverse";

    /** JDK 内置 HTTP 客户端，避免为了一个轻量天气接口额外增加依赖。 */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 字段 properties：保存 properties 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final SystemStatusProperties properties;

    /** 内存天气缓存，少量用户场景下减少外部天气接口调用。 */
    private volatile CachedWeather cachedWeather;

    /**
     * 构造 SystemStatusService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public SystemStatusService(SystemStatusProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取系统状态。
     *
     * <p>实现步骤：
     * 1. 读取服务器当前时间和服务器时区；
     * 2. 从内存缓存或外部接口读取天气；
     * 3. 外部天气异常时返回不可用状态，不影响服务器时间展示和页面主流程。</p>
     */
    public SystemStatusView currentStatus(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy, String locationSource, String locationName) {
        // 变量说明：serverZone 保存当前步骤计算、查询或转换得到的中间结果。
        ZoneId serverZone = ZoneId.systemDefault();
        return new SystemStatusView(OffsetDateTime.now(serverZone), serverZone.getId(), loadWeatherSafely(resolveWeatherLocation(latitude, longitude, accuracy, locationSource, locationName)));
    }

    /**
     * 安全读取天气。
     *
     * <p>实现步骤：
     * 1. 天气配置关闭时直接返回不可用；
     * 2. 优先读取未过期的内存缓存；
     * 3. 缓存过期时访问天气接口并刷新缓存；
     * 4. 任意异常转为不可用天气视图，避免外部接口影响系统。</p>
     */
    private WeatherView loadWeatherSafely(WeatherLocation location) {
        if (!properties.isEnabled()) {
            return unavailableWeather(location, "天气查询未启用");
        }
        // 变量说明：snapshot 保存当前步骤计算、查询或转换得到的中间结果。
        CachedWeather snapshot = cachedWeather;
        if (snapshot != null && snapshot.cacheKey().equals(location.cacheKey()) && snapshot.expiresAt().isAfter(Instant.now())) {
            return snapshot.weather();
        }
        try {
            // 变量说明：weather 保存当前步骤计算、查询或转换得到的中间结果。
            WeatherView weather = fetchWeather(location);
            cachedWeather = new CachedWeather(location.cacheKey(), weather, Instant.now().plusSeconds(safeCacheSeconds()));
            return weather;
        } catch (Exception ex) {
            return unavailableWeather(location, "天气暂不可用");
        }
    }

    /**
     * 从 Open-Meteo 读取天气。
     *
     * <p>实现步骤：
     * 1. 组装经纬度、当前天气、小时天气和时区参数；
     * 2. 使用 JDK HttpClient 请求 JSON；
     * 3. 解析 current 中的当前气温和天气代码；
     * 4. 解析 hourly 中当前时刻之后的未来小时列表；
     * 5. 把 WMO 天气代码转换为中文描述和前端图标类型。</p>
     */
    private WeatherView fetchWeather(WeatherLocation location) throws Exception {
        // 变量说明：payload 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject payload = JSON.parseObject(sendWeatherRequest(buildWeatherUrl(location)));
        // 变量说明：current 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject current = payload.getJSONObject("current");
        // 变量说明：hourly 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject hourly = payload.getJSONObject("hourly");
        // 变量说明：daily 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject daily = payload.getJSONObject("daily");
        if (current == null || hourly == null) {
            throw new IllegalStateException("天气接口响应缺少 current 或 hourly");
        }

        // 变量说明：currentTime 保存当前步骤计算、查询或转换得到的中间结果。
        String currentTime = current.getString("time");
        // 变量说明：temperature 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal temperature = current.getBigDecimal("temperature_2m");
        // 变量说明：uvIndex 保存当前日期对应的紫外线指数。
        BigDecimal uvIndex = uvIndexForTime(daily, currentTime);
        // 变量说明：weatherCode 保存当前步骤计算、查询或转换得到的中间结果。
        Integer weatherCode = current.getInteger("weather_code");
        // 变量说明：descriptor 保存当前步骤计算、查询或转换得到的中间结果。
        WeatherDescriptor descriptor = describeWeather(weatherCode);
        // 变量说明：futureHours 保存当前步骤计算、查询或转换得到的中间结果。
        List<WeatherHourlyView> futureHours = parseFutureHours(hourly, currentTime, daily);

        return new WeatherView(
                true,
                WEATHER_SOURCE,
                location.source(),
                location.name(),
                location.latitude(),
                location.longitude(),
                currentTime,
                temperature,
                uvIndex,
                weatherCode,
                descriptor.text(),
                descriptor.iconType(),
                futureHours,
                null
        );
    }

    /**
     * 组装天气接口地址。
     *
     * <p>实现步骤：固定读取当前天气、小时天气和 daily.uv_index_max；
     * forecast_days 取 2 天，确保任何当前小时都能截取足够的未来小时和当天紫外线指数。</p>
     */
    private String buildWeatherUrl(WeatherLocation location) {
        return properties.getEndpoint()
                + "?latitude=" + encode(location.latitude())
                + "&longitude=" + encode(location.longitude())
                + "&current=temperature_2m,weather_code"
                + "&hourly=temperature_2m,weather_code"
                + "&daily=uv_index_max"
                + "&forecast_days=2"
                + "&timezone=auto";
    }

    /**
     * 发送天气接口请求。
     *
     * <p>实现步骤：设置 JSON Accept 头和配置化超时；非 2xx、超时或中断均向上抛出，由安全读取方法兜底。</p>
     */
    private String sendWeatherRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(safeTimeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            // 变量说明：response 保存当前步骤计算、查询或转换得到的中间结果。
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("天气接口响应状态异常: " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    /**
     * 解析未来小时天气。
     *
     * <p>实现步骤：
     * 1. 读取 hourly.time、hourly.temperature_2m、hourly.weather_code 三个数组；
     * 2. 从当前时刻之后开始筛选；
     * 3. 最多返回配置指定的未来小时数量；
     * 4. 每行天气代码都转换为中文描述和前端图标类型。</p>
     */
    private List<WeatherHourlyView> parseFutureHours(JSONObject hourly, String currentTime, JSONObject daily) {
        // 变量说明：times 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray times = hourly.getJSONArray("time");
        // 变量说明：temperatures 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray temperatures = hourly.getJSONArray("temperature_2m");
        // 变量说明：weatherCodes 保存当前步骤计算、查询或转换得到的中间结果。
        JSONArray weatherCodes = hourly.getJSONArray("weather_code");
        if (times == null || temperatures == null || weatherCodes == null) {
            return List.of();
        }
        // 变量说明：current 保存当前步骤计算、查询或转换得到的中间结果。
        LocalDateTime current = parseOpenMeteoTime(currentTime);
        // 变量说明：rows 保存当前步骤计算、查询或转换得到的中间结果。
        List<WeatherHourlyView> rows = new ArrayList<>();
        // 变量说明：maxRows 保存当前步骤计算、查询或转换得到的中间结果。
        int maxRows = Math.min(Math.min(times.size(), temperatures.size()), weatherCodes.size());
        for (int index = 0; index < maxRows && rows.size() < safeForecastHours(); index++) {
            // 变量说明：time 保存当前步骤计算、查询或转换得到的中间结果。
            String time = times.getString(index);
            // 变量说明：forecastTime 保存当前步骤计算、查询或转换得到的中间结果。
            LocalDateTime forecastTime = parseOpenMeteoTime(time);
            if (forecastTime == null || current != null && !forecastTime.isAfter(current)) {
                continue;
            }
            // 变量说明：weatherCode 保存当前步骤计算、查询或转换得到的中间结果。
            Integer weatherCode = weatherCodes.getInteger(index);
            // 变量说明：descriptor 保存当前步骤计算、查询或转换得到的中间结果。
            WeatherDescriptor descriptor = describeWeather(weatherCode);
            rows.add(new WeatherHourlyView(
                    time,
                    temperatures.getBigDecimal(index),
                    uvIndexForTime(daily, time),
                    weatherCode,
                    descriptor.text(),
                    descriptor.iconType()
            ));
        }
        return rows;
    }

    /**
     * 按 Open-Meteo 时间匹配当日紫外线指数。
     */
    private BigDecimal uvIndexForTime(JSONObject daily, String time) {
        LocalDateTime parsedTime = parseOpenMeteoTime(time);
        if (parsedTime == null) {
            return null;
        }
        return uvIndexForDate(daily, parsedTime.toLocalDate().toString());
    }

    /**
     * 从 daily.time 和 daily.uv_index_max 中读取指定日期的紫外线指数。
     */
    private BigDecimal uvIndexForDate(JSONObject daily, String date) {
        if (daily == null || date == null || date.isBlank()) {
            return null;
        }
        JSONArray dates = daily.getJSONArray("time");
        JSONArray uvIndexes = daily.getJSONArray("uv_index_max");
        if (dates == null || uvIndexes == null) {
            return null;
        }
        int maxRows = Math.min(dates.size(), uvIndexes.size());
        for (int index = 0; index < maxRows; index++) {
            if (date.equals(dates.getString(index))) {
                return uvIndexes.getBigDecimal(index);
            }
        }
        return null;
    }

    /**
     * 解析 Open-Meteo 本地时间字符串。
     *
     * <p>实现步骤：Open-Meteo 在 timezone=auto 时返回 yyyy-MM-dd'T'HH:mm 格式，本方法解析失败时返回 null，由调用方跳过该行。</p>
     */
    private LocalDateTime parseOpenMeteoTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 把 WMO 天气代码转换为业务展示信息。
     *
     * <p>实现步骤：按 Open-Meteo 返回的 WMO Weather interpretation codes 分组映射为中文描述和前端图标类型。</p>
     */
    private WeatherDescriptor describeWeather(Integer code) {
        if (code == null) {
            return new WeatherDescriptor("未知", "CLOUDY");
        }
        return switch (code) {
            case 0 -> new WeatherDescriptor("晴", "SUNNY");
            case 1 -> new WeatherDescriptor("大部晴朗", "PARTLY_CLOUDY");
            case 2 -> new WeatherDescriptor("多云", "PARTLY_CLOUDY");
            case 3 -> new WeatherDescriptor("阴", "CLOUDY");
            case 45, 48 -> new WeatherDescriptor("雾", "CLOUDY");
            case 51, 53, 55, 56, 57 -> new WeatherDescriptor("毛毛雨", "DRIZZLE");
            case 61, 63, 65, 66, 67, 80, 81, 82 -> new WeatherDescriptor("雨", "RAIN");
            case 71, 73, 75, 77, 85, 86 -> new WeatherDescriptor("雪", "SNOW");
            case 95, 96, 99 -> new WeatherDescriptor("雷雨", "THUNDER");
            default -> new WeatherDescriptor("未知", "CLOUDY");
        };
    }

    /**
     * 生成不可用天气视图。
     */
    private WeatherView unavailableWeather(WeatherLocation location, String message) {
        return new WeatherView(
                false,
                WEATHER_SOURCE,
                location.source(),
                location.name(),
                location.latitude(),
                location.longitude(),
                null,
                null,
                null,
                null,
                "天气暂不可用",
                "CLOUDY",
                List.of(),
                message
        );
    }

    /**
     * 解析本次天气查询使用的位置。
     *
     * <p>实现步骤：
     * 1. 校验浏览器传入的经纬度是否在合法范围内；
     * 2. 合法时优先使用浏览器定位坐标，并尝试反查行政区名称；
     * 3. 定位缺失、拒绝授权或坐标异常时，回退到配置文件中的默认地区。</p>
     */
    private WeatherLocation resolveWeatherLocation(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy, String locationSource, String locationName) {
        if (validCoordinate(latitude, longitude)) {
            BigDecimal normalizedLatitude = normalizeCoordinate(latitude);
            BigDecimal normalizedLongitude = normalizeCoordinate(longitude);
            String source = normalizeLocationSource(locationSource);
            String name = resolveLocationName(normalizedLatitude, normalizedLongitude, accuracy, source, locationName);
            return new WeatherLocation(
                    source,
                    name,
                    normalizedLatitude,
                    normalizedLongitude,
                    source.toLowerCase() + ":" + normalizedLatitude + "," + normalizedLongitude
            );
        }
        return new WeatherLocation(
                LOCATION_SOURCE_CONFIG,
                safeConfiguredLocationName(),
                properties.getLatitude(),
                properties.getLongitude(),
                "config:" + properties.getLatitude() + "," + properties.getLongitude()
        );
    }

    /**
     * 判断经纬度是否可用于天气查询。
     */
    private boolean validCoordinate(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude.compareTo(new BigDecimal("-90")) >= 0
                && latitude.compareTo(new BigDecimal("90")) <= 0
                && longitude.compareTo(new BigDecimal("-180")) >= 0
                && longitude.compareTo(new BigDecimal("180")) <= 0;
    }

    /**
     * 规范经纬度精度，避免同一位置因浏览器微小抖动不断击穿缓存。
     */
    private BigDecimal normalizeCoordinate(BigDecimal value) {
        return value.setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /**
     * 尝试把经纬度反查为用户可读的行政区划名称。
     */
    private String resolveLocationName(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy, String source, String locationName) {
        try {
            JSONObject address = JSON.parseObject(sendReverseGeocodeRequest(latitude, longitude)).getJSONObject("address");
            String name = joinDistinct(
                    addressText(address, "province", "state", "region"),
                    addressText(address, "city", "county", "state_district", "municipality"),
                    addressText(address, "district", "city_district", "borough", "suburb", "quarter", "town", "village")
            );
            if (!name.isBlank()) {
                return name;
            }
        } catch (Exception ex) {
            // 地址反查失败只影响展示名称，天气仍然按浏览器经纬度查询。
        }
        String clientLocationName = sanitizeClientLocationName(locationName);
        if (!clientLocationName.isBlank()) {
            return clientLocationName;
        }
        String knownDistrictName = knownDistrictName(latitude, longitude);
        if (!knownDistrictName.isBlank()) {
            return knownDistrictName;
        }
        String suffix = accuracy != null && accuracy.compareTo(BigDecimal.ZERO) > 0
                ? "，精度约" + accuracy.setScale(0, java.math.RoundingMode.HALF_UP) + "米"
                : "";
        String label = LOCATION_SOURCE_IP.equals(source) ? "公网IP粗定位" : "当前位置";
        return label + "（" + latitude + ", " + longitude + suffix + "）";
    }

    /**
     * 规范化前端传入的位置来源。
     */
    private String normalizeLocationSource(String source) {
        if (LOCATION_SOURCE_IP.equalsIgnoreCase(source == null ? "" : source.trim())) {
            return LOCATION_SOURCE_IP;
        }
        return LOCATION_SOURCE_BROWSER;
    }

    /**
     * 清理前端 IP 粗定位返回的位置名称，避免异常长文本进入天气缓存和页面。
     */
    private String sanitizeClientLocationName(String locationName) {
        if (locationName == null) {
            return "";
        }
        String normalized = locationName.trim();
        if (normalized.length() > 80) {
            return normalized.substring(0, 80);
        }
        return normalized;
    }

    private String knownDistrictName(BigDecimal latitude, BigDecimal longitude) {
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();
        if (lat >= 30.58D && lat <= 30.74D && lon >= 103.95D && lon <= 104.10D) {
            return "四川省成都市青羊区";
        }
        if (lat >= 30.45D && lat <= 30.85D && lon >= 103.80D && lon <= 104.30D) {
            return "四川省成都市";
        }
        return "";
    }

    /**
     * 调用经纬度反查接口。
     */
    private String sendReverseGeocodeRequest(BigDecimal latitude, BigDecimal longitude) throws Exception {
        String url = REVERSE_GEOCODE_ENDPOINT
                + "?format=jsonv2"
                + "&addressdetails=1"
                + "&accept-language=zh-CN"
                + "&lat=" + encode(latitude)
                + "&lon=" + encode(longitude);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(safeTimeoutSeconds()))
                .header("Accept", "application/json")
                .header("User-Agent", "ratel-fm-weather/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("地址反查接口响应状态异常: " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    /**
     * 从地址对象中按优先级读取第一个非空字段。
     */
    private String addressText(JSONObject address, String... keys) {
        if (address == null) {
            return "";
        }
        for (String key : keys) {
            String value = address.getString(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * 拼接行政区名称，连续重复名称只保留一次。
     */
    private String joinDistinct(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String text = value.trim();
            if (parts.isEmpty() || !parts.get(parts.size() - 1).equals(text)) {
                parts.add(text);
            }
        }
        return String.join("", parts);
    }

    /**
     * 读取配置中的默认天气位置名称。
     */
    private String safeConfiguredLocationName() {
        String name = properties.getLocationName();
        return name == null || name.isBlank() ? "默认地区" : name.trim();
    }

    /**
     * 执行 encode 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private String encode(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

    /**
     * 执行 safeForecastHours 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int safeForecastHours() {
        return Math.min(Math.max(properties.getForecastHours(), 1), 24);
    }

    /**
     * 执行 safeTimeoutSeconds 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int safeTimeoutSeconds() {
        return Math.min(Math.max(properties.getRequestTimeoutSeconds(), 1), 15);
    }

    /**
     * 执行 safeCacheSeconds 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private int safeCacheSeconds() {
        return Math.min(Math.max(properties.getCacheSeconds(), 30), 3600);
    }

    /**
     * CachedWeather 数据传输记录。
     * 
     * <p>用于承载 CachedWeather 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    private record CachedWeather(String cacheKey, WeatherView weather, Instant expiresAt) {
    }

    /**
     * WeatherLocation 数据传输记录。
     *
     * <p>封装一次天气查询使用的位置来源、名称、坐标和缓存键。</p>
     */
    private record WeatherLocation(String source, String name, BigDecimal latitude, BigDecimal longitude, String cacheKey) {
    }

    /**
     * WeatherDescriptor 数据传输记录。
     * 
     * <p>用于承载 WeatherDescriptor 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    private record WeatherDescriptor(String text, String iconType) {
    }
}
