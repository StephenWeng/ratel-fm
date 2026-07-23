package com.ratel.fm.web.basic;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.ratel.fm.common.ApiResponse;
import com.ratel.fm.service.basic.BasicDictionaryService;
import com.ratel.fm.service.basic.CurrencyService;
import com.ratel.fm.web.dto.basic.BasicDictionaryDtos.BasicDictionaryRequest;
import com.ratel.fm.web.dto.basic.BasicDictionaryDtos.BasicDictionaryView;
import com.ratel.fm.web.dto.basic.BasicDictionaryDtos.ExchangeRateView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基础信息接口。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Tag(name = "基础信息")
@ApiSupport(order = 15, author = "ratel / WenZhang / 18782945613")
@RestController
@RequestMapping("/api/basic/dictionaries")
public class BasicDictionaryController {

    /**
     * 字段 dictionaryService：保存 dictionaryService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BasicDictionaryService dictionaryService;
    /**
     * 字段 currencyService：保存 currencyService 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final CurrencyService currencyService;

    /**
     * 构造 BasicDictionaryController 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public BasicDictionaryController(BasicDictionaryService dictionaryService, CurrencyService currencyService) {
        this.dictionaryService = dictionaryService;
        this.currencyService = currencyService;
    }

    @ApiOperationSupport(order = 10, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询基础字典树", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。用于基础信息页面维护采购方、物流方等层级字典。")
    @PreAuthorize("hasAuthority('BASIC_DICT_MANAGE')")
    @GetMapping
    /**
     * 执行 listTree 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> listTree() {
        return ApiResponse.ok(dictionaryService.listTree());
    }

    @ApiOperationSupport(order = 12, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询根层级基础字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。字典管理页面首屏懒加载，只返回第一层字典。")
    @PreAuthorize("hasAuthority('BASIC_DICT_MANAGE')")
    @GetMapping("/roots")
    /**
     * 执行 roots 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> roots() {
        return ApiResponse.ok(dictionaryService.listRoots());
    }

    @ApiOperationSupport(order = 14, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询直接子级基础字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。字典管理页面展开节点时按需加载直接子级。")
    @PreAuthorize("hasAuthority('BASIC_DICT_MANAGE')")
    @GetMapping("/children")
    /**
     * 执行 children 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> children(@RequestParam(required = false) Long parentId) {
        return ApiResponse.ok(dictionaryService.listChildren(parentId));
    }

    @ApiOperationSupport(order = 16, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "搜索基础字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。编码、名称和说明输入框按包含匹配，父级和启用状态按等值匹配，只返回命中节点及祖先链。")
    @PreAuthorize("hasAuthority('BASIC_DICT_MANAGE')")
    @GetMapping("/search")
    /**
     * 执行 search 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> search(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Long parentId
    ) {
        return ApiResponse.ok(dictionaryService.searchTree(code, name, description, enabled, parentId));
    }

    @ApiOperationSupport(order = 20, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询启用子级字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。业务模块按根字典编码加载启用子级，禁用字典不会返回。")
    @GetMapping("/enabled-children")
    /**
     * 执行 enabledChildren 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> enabledChildren(@RequestParam String parentCode) {
        return ApiResponse.ok(dictionaryService.listEnabledChildren(parentCode));
    }

    @ApiOperationSupport(order = 22, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "按父级ID查询启用子级字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。用于行政区划等大字典的业务级联懒加载，展开到下一层时才读取子级。")
    @GetMapping("/enabled-children-by-parent")
    /**
     * 按父级 ID 查询启用的直接子级字典。
     *
     * <p>实现步骤：
     * 1. 接收前端级联组件传入的父级字典 ID；
     * 2. 校验父级字典自身及上级均处于启用状态；
     * 3. 只返回下一层启用子级，避免行政区划等大字典一次性加载导致页面卡顿。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> enabledChildrenByParent(@RequestParam Long parentId) {
        return ApiResponse.ok(dictionaryService.listEnabledChildrenByParentId(parentId));
    }

    @ApiOperationSupport(order = 25, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询启用字典树", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。业务模块按根字典编码加载启用树，父级停用时整棵下级不会返回。")
    @GetMapping("/enabled-tree")
    /**
     * 执行 enabledTree 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<List<BasicDictionaryView>> enabledTree(@RequestParam String rootCode) {
        return ApiResponse.ok(dictionaryService.listEnabledTree(rootCode));
    }

    @ApiOperationSupport(order = 27, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "查询币种最新参考汇率", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。用于新增凭证切换币种时自动填充最新公开参考汇率；该汇率不是秒级实时交易价，外部汇率失败时前端仍可手工填写。")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/exchange-rate")
    /**
     * 查询币种最新公开参考汇率。
     * 
     * <p>实现步骤：
     * 1. 接收前端传入的币种编码；
     * 2. 调用币种服务查询该币种折人民币的最新公开参考汇率；
     * 3. 返回币种、汇率、目标币种、汇率来源和汇率日期，便于前端提示用户该值可手工调整。</p>
     */
    public ApiResponse<ExchangeRateView> exchangeRate(@RequestParam String currencyCode) {
        // 变量说明：snapshot 保存当前步骤计算、查询或转换得到的中间结果。
        var snapshot = currencyService.currentExchangeRateToCny(currencyCode);
        return ApiResponse.ok(new ExchangeRateView(
                snapshot.currencyCode(),
                snapshot.currencyName(),
                snapshot.exchangeRateToCny(),
                snapshot.quoteCurrencyCode(),
                snapshot.source(),
                snapshot.rateDate()
        ));
    }

    @ApiOperationSupport(order = 30, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "新增基础字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。字典编码可不填，服务端自动生成；同一父级下名称唯一。")
    @PreAuthorize("hasAuthority('BASIC_DICT_MANAGE')")
    @PostMapping
    /**
     * 执行 create 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<BasicDictionaryView> create(@Valid @RequestBody BasicDictionaryRequest request) {
        return ApiResponse.ok("基础字典已创建", dictionaryService.create(request));
    }

    @ApiOperationSupport(order = 40, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "修改基础字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。同一父级下名称唯一；禁用后不会在业务下拉展示。")
    @PreAuthorize("hasAuthority('BASIC_DICT_MANAGE')")
    @PutMapping("/{id}")
    /**
     * 执行 update 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<BasicDictionaryView> update(@PathVariable Long id, @Valid @RequestBody BasicDictionaryRequest request) {
        return ApiResponse.ok("基础字典已更新", dictionaryService.update(id, request));
    }

    @ApiOperationSupport(order = 50, author = "ratel / WenZhang / 18782945613")
    @Operation(summary = "删除基础字典", description = "作者：ratel；开发人员：WenZhang；联系方式：18782945613。存在下级字典时不允许删除。")
    @PreAuthorize("hasAuthority('BASIC_DICT_MANAGE')")
    @DeleteMapping("/{id}")
    /**
     * 执行 delete 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dictionaryService.delete(id);
        return ApiResponse.ok("基础字典已删除", null);
    }
}
