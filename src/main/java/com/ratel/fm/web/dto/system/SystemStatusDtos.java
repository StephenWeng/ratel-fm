package com.ratel.fm.web.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 系统状态 DTO。
 *
 * <p>承载右上角服务器时间、当前天气和未来小时天气列表。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public final class SystemStatusDtos {

    private SystemStatusDtos() {
    }

    @Schema(description = "系统状态视图")
    /**
     * SystemStatusView 数据传输记录。
     * 
     * <p>用于承载 SystemStatusView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record SystemStatusView(
            @Schema(description = "当前服务器时间") OffsetDateTime serverTime,
            @Schema(description = "服务器时区") String serverZone,
            @Schema(description = "天气信息") WeatherView weather
    ) {
    }

    @Schema(description = "天气视图")
    /**
     * WeatherView 数据传输记录。
     * 
     * <p>用于承载 WeatherView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record WeatherView(
            @Schema(description = "天气是否可用") boolean available,
            @Schema(description = "天气数据来源") String source,
            @Schema(description = "天气位置来源") String locationSource,
            @Schema(description = "天气位置名称") String locationName,
            @Schema(description = "纬度") BigDecimal latitude,
            @Schema(description = "经度") BigDecimal longitude,
            @Schema(description = "当前天气时刻") String currentTime,
            @Schema(description = "当前气温") BigDecimal temperature,
            @Schema(description = "当前紫外线指数") BigDecimal uvIndex,
            @Schema(description = "天气代码") Integer weatherCode,
            @Schema(description = "天气文字") String weatherText,
            @Schema(description = "前端图标类型") String iconType,
            @Schema(description = "未来小时天气列表") List<WeatherHourlyView> futureHours,
            @Schema(description = "不可用原因") String errorMessage
    ) {
    }

    @Schema(description = "未来小时天气视图")
    /**
     * WeatherHourlyView 数据传输记录。
     * 
     * <p>用于承载 WeatherHourlyView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record WeatherHourlyView(
            @Schema(description = "预报时刻") String time,
            @Schema(description = "气温") BigDecimal temperature,
            @Schema(description = "紫外线指数") BigDecimal uvIndex,
            @Schema(description = "天气代码") Integer weatherCode,
            @Schema(description = "天气文字") String weatherText,
            @Schema(description = "前端图标类型") String iconType
    ) {
    }
}
