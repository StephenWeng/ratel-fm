package com.ratel.fm.config.system;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 系统状态展示配置。
 *
 * <p>用于配置右上角服务器时间、天气展示所需的天气接口、位置和缓存参数。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
@ConfigurationProperties(prefix = "app.system.weather")
public class SystemStatusProperties {

    /** 是否启用天气查询；关闭后前端只展示服务器时间。 */
    private boolean enabled = true;

    /** Open-Meteo 天气预报接口地址，默认无需 API Key。 */
    private String endpoint = "https://api.open-meteo.com/v1/forecast";

    /** 天气展示城市名称，前端悬浮面板会直接展示该名称。 */
    private String locationName = "成都";

    /** 天气查询纬度，默认成都。 */
    private BigDecimal latitude = new BigDecimal("30.5728");

    /** 天气查询经度，默认成都。 */
    private BigDecimal longitude = new BigDecimal("104.0668");

    /** 未来小时天气返回数量，过小或过大时服务层会做边界保护。 */
    private int forecastHours = 12;

    /** 天气接口请求超时时间，避免外部接口异常拖慢系统页面。 */
    private int requestTimeoutSeconds = 5;

    /** 天气结果内存缓存秒数，少量用户场景下避免频繁访问外部接口。 */
    private int cacheSeconds = 600;

    /**
     * 执行 isEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 执行 setEnabled 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 执行 getEndpoint 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * 执行 setEndpoint 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * 执行 getLocationName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getLocationName() {
        return locationName;
    }

    /**
     * 执行 setLocationName 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    /**
     * 执行 getLatitude 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getLatitude() {
        return latitude;
    }

    /**
     * 执行 setLatitude 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    /**
     * 执行 getLongitude 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public BigDecimal getLongitude() {
        return longitude;
    }

    /**
     * 执行 setLongitude 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    /**
     * 执行 getForecastHours 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public int getForecastHours() {
        return forecastHours;
    }

    /**
     * 执行 setForecastHours 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setForecastHours(int forecastHours) {
        this.forecastHours = forecastHours;
    }

    /**
     * 执行 getRequestTimeoutSeconds 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    /**
     * 执行 setRequestTimeoutSeconds 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    /**
     * 执行 getCacheSeconds 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public int getCacheSeconds() {
        return cacheSeconds;
    }

    /**
     * 执行 setCacheSeconds 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setCacheSeconds(int cacheSeconds) {
        this.cacheSeconds = cacheSeconds;
    }
}
