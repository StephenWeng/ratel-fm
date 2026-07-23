package com.ratel.fm.web.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量操作 ID 请求。
 *
 * <p>用于列表批量删除等只需要传递主键集合的接口。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Schema(description = "批量操作 ID 请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
public record BatchIdsRequest(
        @Schema(description = "待操作的数据主键集合。")
        @NotEmpty(message = "请选择需要操作的数据")
        /**
         * 记录组件 ids：表示接口入参或出参中的 ids 字段。
         */
        List<@NotNull(message = "数据主键不能为空") Long> ids
) {
}
