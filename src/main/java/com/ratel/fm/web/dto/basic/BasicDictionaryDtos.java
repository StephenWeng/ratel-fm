package com.ratel.fm.web.dto.basic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 基础字典接口 DTO。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
/**
 * 基础字典接口 DTO。
 *
 * <p>实现目的：
 * 1. 接收字典编码、名称、父级、排序和启停状态；
 * 2. 字典说明按说明类文本统一限制 2000 字符；
 * 3. 返回树节点时保留 hasChildren，支撑前端懒加载树表格。</p>
 */
public final class BasicDictionaryDtos {

    private BasicDictionaryDtos() {
    }

    @Schema(description = "基础字典保存请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * BasicDictionaryRequest 数据传输记录。
     * 
     * <p>用于承载 BasicDictionaryRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record BasicDictionaryRequest(
            @Schema(description = "字典编码；非必填，未填写时服务端随机生成。")
            @Size(max = 80, message = "字典编码长度不能超过80个字符")
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            @Schema(description = "字典名称；同一父级下唯一，不同层级允许重复。")
            @NotBlank(message = "字典名称不能为空")
            @Size(max = 120, message = "字典名称长度不能超过120个字符")
            /**
             * 记录组件 name：表示接口入参或出参中的 name 字段。
             */
            String name,
            @Schema(description = "父级字典 ID；为空表示一级字典。")
            /**
             * 记录组件 parentId：表示接口入参或出参中的 parentId 字段。
             */
            Long parentId,
            @Schema(description = "排序号，越小越靠前。")
            /**
             * 记录组件 sortOrder：表示接口入参或出参中的 sortOrder 字段。
             */
            Integer sortOrder,
            @Schema(description = "是否启用；为空时按启用处理。")
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            Boolean enabled,
            @Schema(description = "停用时如存在启用下级字典，是否已经完成二次确认。")
            /**
             * 记录组件 confirmDisableWithEnabledChildren：表示接口入参或出参中的 confirmDisableWithEnabledChildren 字段。
             */
            Boolean confirmDisableWithEnabledChildren,
            @Schema(description = "字典说明。")
            @Size(max = 2000, message = "字典说明长度不能超过2000个中文字符")
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description
    ) {
    }

    @Schema(description = "基础字典树节点。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * BasicDictionaryView 数据传输记录。
     * 
     * <p>用于承载 BasicDictionaryView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record BasicDictionaryView(
            @Schema(description = "字典主键 ID。") Long id,
            @Schema(description = "字典编码。") String code,
            @Schema(description = "字典名称。") String name,
            @Schema(description = "父级字典 ID。") Long parentId,
            @Schema(description = "排序号。") int sortOrder,
            @Schema(description = "是否启用。") boolean enabled,
            @Schema(description = "字典说明。") String description,
            @Schema(description = "是否存在下级字典；前端懒加载树表格据此显示展开入口。") boolean hasChildren,
            @Schema(description = "子级字典集合。") List<BasicDictionaryView> children
    ) {
    }

    @Schema(description = "最新公开参考汇率查询结果。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * ExchangeRateView 数据传输记录。
     * 
     * <p>用于承载币种折人民币的最新公开参考汇率、来源和汇率日期。该结果用于自动填充表单，不代表秒级实时交易价。</p>
     */
    public record ExchangeRateView(
            @Schema(description = "币种编码。") String currencyCode,
            @Schema(description = "币种名称。") String currencyName,
            @Schema(description = "该币种折人民币最新公开参考汇率，表示 1 单位当前币种可兑换多少人民币，保留 8 位小数。") BigDecimal exchangeRateToCny,
            @Schema(description = "目标币种编码，当前固定为 CNY。") String quoteCurrencyCode,
            @Schema(description = "汇率来源。") String source,
            @Schema(description = "汇率日期，由外部汇率服务返回；为空时表示系统固定汇率。") String rateDate
    ) {
    }
}
