package com.ratel.fm.web.system;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.service.system.SystemStatusService;
import com.ratel.fm.web.dto.system.SystemStatusDtos.SystemStatusView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统状态控制器。
 *
 * <p>提供右上角服务器时间和天气展示所需的轻量状态接口。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Tag(name = "系统状态")
@ApiSupport(order = 5, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    /**
     * 字段 systemStatusService：保存 systemStatusService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final SystemStatusService systemStatusService;

    /**
     * 构造 SystemStatusController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    /**
     * 查询系统状态。
     *
     * <p>实现步骤：调用系统状态服务获取服务器时间、服务器时区、当前天气和未来小时天气列表。</p>
     */
    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询系统状态", description = "查询服务器时间、服务器时区、当前天气和未来小时天气列表；传入浏览器定位或公网 IP 粗定位经纬度时优先查询当前位置天气。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    @GetMapping("/status")
    public ApiResponse<SystemStatusView> status(
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal accuracy,
            @RequestParam(required = false) String locationSource,
            @RequestParam(required = false) String locationName
    ) {
        return ApiResponse.ok(systemStatusService.currentStatus(latitude, longitude, accuracy, locationSource, locationName));
    }
}
