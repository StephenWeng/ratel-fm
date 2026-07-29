package com.ratel.fm.web.dto.finance;

import com.ratel.fm.domain.finance.SubjectCategory;
import com.ratel.fm.domain.finance.AccountingSourceType;
import com.ratel.fm.domain.finance.VoucherStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 财务科目、凭证和报表接口 DTO。
 *
 * <p>实现目的：
 * 1. 对凭证摘要、来源单号、分录金额等关键入参做统一校验；
 * 2. 将凭证主表和分录明细完整返回给列表、编辑和查看流水页面；
 * 3. 保持多币种金额快照字段在接口层可见，便于后续报表和追溯。</p>
 */
public final class FinanceDtos {

    private FinanceDtos() {
    }

    @Schema(description = "会计科目请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * SubjectRequest 数据传输记录。
     * 
     * <p>用于承载 SubjectRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record SubjectRequest(
            @Schema(description = "科目编码，全系统唯一。")
            @NotBlank(message = "科目编码不能为空")
            @Size(max = 40, message = "科目编码长度不能超过40个字符")
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            @Schema(description = "科目名称。")
            @NotBlank(message = "科目名称不能为空")
            @Size(max = 120, message = "科目名称长度不能超过120个字符")
            /**
             * 记录组件 name：表示接口入参或出参中的 name 字段。
             */
            String name,
            @Schema(description = "科目类别：资产、负债、共同、权益、收入、成本、费用。")
            @NotNull SubjectCategory category,
            @Schema(description = "父级科目 ID；为空表示一级科目。")
            /**
             * 记录组件 parentId：表示接口入参或出参中的 parentId 字段。
             */
            Long parentId,
            @Schema(description = "是否启用；为空时按启用处理。")
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            Boolean enabled,
            @Schema(description = "停用时如存在启用下级科目，是否已经完成二次确认。")
            /**
             * 记录组件 confirmDisableWithEnabledChildren：表示接口入参或出参中的 confirmDisableWithEnabledChildren 字段。
             */
            Boolean confirmDisableWithEnabledChildren,
            @Schema(description = "科目说明。")
            @Size(max = 2000, message = "科目说明长度不能超过2000个中文字符")
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description
    ) {
    }

    /**
     * SubjectView 数据传输记录。
     * 
     * <p>用于承载 SubjectView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record SubjectView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 code：表示接口入参或出参中的 code 字段。
             */
            String code,
            /**
             * 记录组件 name：表示接口入参或出参中的 name 字段。
             */
            String name,
            /**
             * 记录组件 category：表示接口入参或出参中的 category 字段。
             */
            SubjectCategory category,
            /**
             * 记录组件 parentId：表示接口入参或出参中的 parentId 字段。
             */
            Long parentId,
            /**
             * 记录组件 parentName：表示接口入参或出参中的 parentName 字段。
             */
            String parentName,
            /**
             * 记录组件 subjectLevel：表示接口入参或出参中的 subjectLevel 字段。
             */
            int subjectLevel,
            /**
             * 记录组件 enabled：表示接口入参或出参中的 enabled 字段。
             */
            boolean enabled,
            /**
             * 记录组件 description：表示接口入参或出参中的 description 字段。
             */
            String description
    ) {
    }

    /**
     * VoucherLineRequest 数据传输记录。
     * 
     * <p>用于承载 VoucherLineRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record VoucherLineRequest(
            @Schema(description = "会计科目 ID，必须是启用科目。")
            @NotNull Long subjectId,
            @Schema(description = "分录摘要。")
            @NotBlank(message = "分录摘要不能为空")
            @Size(max = 200, message = "分录摘要长度不能超过200个字符")
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            @Schema(description = "借方金额；同一分录不能同时填写借方和贷方金额。")
            @PositiveOrZero BigDecimal debitAmount,
            @Schema(description = "贷方金额；同一分录不能同时填写借方和贷方金额。")
            @PositiveOrZero BigDecimal creditAmount,
            @Schema(description = "分录币种编码，来自基础信息币种字典；为空时默认人民币 CNY。")
            @Size(max = 20, message = "币种编码长度不能超过20个字符")
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            @Schema(description = "分录币种名称快照；服务端会优先按字典编码解析。")
            @Size(max = 80, message = "币种名称长度不能超过80个字符")
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            @Schema(description = "分录业务发生时该币种折人民币汇率；人民币固定为 1，非人民币必填。")
            @Positive(message = "汇率必须大于0")
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            @Schema(description = "辅助核算信息，预留客户、供应商、项目等。")
            @Size(max = 300, message = "辅助核算信息长度不能超过300个字符")
            /**
             * 记录组件 auxiliary：表示接口入参或出参中的 auxiliary 字段。
             */
            String auxiliary
    ) {
    }

    @Schema(description = "凭证请求，要求借贷平衡。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * VoucherRequest 数据传输记录。
     * 
     * <p>用于承载 VoucherRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record VoucherRequest(
            @Schema(description = "凭证日期。")
            @NotNull LocalDate voucherDate,
            @Schema(description = "所属年月，格式 yyyy-MM；为空时服务端按凭证日期自动取年月。")
            @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "所属年月格式必须为yyyy-MM")
            /**
             * 记录组件 belongMonth：表示接口入参或出参中的 belongMonth 字段。
             */
            String belongMonth,
            @Schema(description = "项目字典编码，来自基础信息项目字典。")
            @Size(max = 80, message = "项目编码长度不能超过80个字符")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "项目名称快照，随项目编码一并提交。")
            @Size(max = 160, message = "项目名称长度不能超过160个字符")
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            @Schema(description = "客户名称。")
            @Size(max = 160, message = "客户名称长度不能超过160个字符")
            String customerName,
            @Schema(description = "部门名称。")
            @Size(max = 120, message = "部门名称长度不能超过120个字符")
            String departmentName,
            @Schema(description = "业务员名称。")
            @Size(max = 120, message = "业务员名称长度不能超过120个字符")
            String businessUser,
            @Schema(description = "记账人名称。")
            @Size(max = 120, message = "记账人名称长度不能超过120个字符")
            String bookkeeper,
            @Schema(description = "制单人名称。")
            @Size(max = 120, message = "制单人名称长度不能超过120个字符")
            String maker,
            @Schema(description = "凭证摘要，由分录摘要汇总形成，手工录入时可为空。")
            @Size(max = 200, message = "凭证摘要长度不能超过200个字符")
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            @Schema(description = "来源业务单号，如采购单号、物流单号。")
            @Size(max = 300, message = "来源业务单号长度不能超过300个中文字符")
            /**
             * 记录组件 sourceBizNo：表示接口入参或出参中的 sourceBizNo 字段。
             */
            String sourceBizNo,
            @Schema(description = "来源业务类型，用于凭证反向追溯来源模块。")
            /**
             * 记录组件 sourceType：表示凭证来源业务模块类型，手工凭证为空，自动凭证由会计平台写入。
             */
            AccountingSourceType sourceType,
            @Schema(description = "来源业务主键，用于凭证反向追溯来源单据详情。")
            /**
             * 记录组件 sourceId：表示来源业务单据主键，配合 sourceType 精确定位原始单据。
             */
            Long sourceId,
            @Schema(description = "来源业务标题，用于凭证列表和查看来源弹窗展示。")
            @Size(max = 160, message = "来源业务标题长度不能超过160个字符")
            /**
             * 记录组件 sourceTitle：表示来源业务的可读标题，便于用户识别凭证来源。
             */
            String sourceTitle,
            @Schema(description = "币种编码，来自基础信息币种字典；为空时默认人民币 CNY。")
            @Size(max = 20, message = "币种编码长度不能超过20个字符")
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            @Schema(description = "币种名称快照；服务端会优先按字典编码解析。")
            @Size(max = 80, message = "币种名称长度不能超过80个字符")
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            @Schema(description = "业务发生时该币种折人民币汇率；人民币固定为 1，非人民币必填。")
            @Positive(message = "汇率必须大于0")
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            @Schema(description = "凭证分录集合，整张凭证必须借贷平衡。")
            @Valid @NotEmpty List<VoucherLineRequest> lines
    ) {
    }

    /**
     * VoucherLineView 数据传输记录。
     * 
     * <p>用于承载 VoucherLineView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record VoucherLineView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 lineNo：表示接口入参或出参中的 lineNo 字段。
             */
            int lineNo,
            /**
             * 记录组件 subjectId：表示接口入参或出参中的 subjectId 字段。
             */
            Long subjectId,
            /**
             * 记录组件 subjectCode：表示接口入参或出参中的 subjectCode 字段。
             */
            String subjectCode,
            /**
             * 记录组件 subjectName：表示接口入参或出参中的 subjectName 字段。
             */
            String subjectName,
            /**
             * 记录组件 subjectFullName：表示科目从一级到末级的完整名称路径，供在线凭证和流水详情展示级联科目。
             */
            String subjectFullName,
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            /**
             * 记录组件 debitAmount：表示接口入参或出参中的 debitAmount 字段。
             */
            BigDecimal debitAmount,
            /**
             * 记录组件 creditAmount：表示接口入参或出参中的 creditAmount 字段。
             */
            BigDecimal creditAmount,
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            /**
             * 记录组件 debitAmountCny：表示接口入参或出参中的 debitAmountCny 字段。
             */
            BigDecimal debitAmountCny,
            /**
             * 记录组件 creditAmountCny：表示接口入参或出参中的 creditAmountCny 字段。
             */
            BigDecimal creditAmountCny,
            /**
             * 记录组件 auxiliary：表示接口入参或出参中的 auxiliary 字段。
             */
            String auxiliary
    ) {
    }

    /**
     * VoucherView 数据传输记录。
     * 
     * <p>用于承载 VoucherView 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record VoucherView(
            /**
             * 记录组件 id：表示接口入参或出参中的 id 字段。
             */
            Long id,
            /**
             * 记录组件 voucherNo：表示接口入参或出参中的 voucherNo 字段。
             */
            String voucherNo,
            /**
             * 记录组件 voucherDate：表示接口入参或出参中的 voucherDate 字段。
             */
            LocalDate voucherDate,
            /**
             * 记录组件 belongMonth：表示接口入参或出参中的 belongMonth 字段。
             */
            String belongMonth,
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示接口入参或出参中的 projectName 字段。
             */
            String projectName,
            /**
             * 记录组件 customerName：表示客户名称快照。
             */
            String customerName,
            /**
             * 记录组件 departmentName：表示部门名称快照。
             */
            String departmentName,
            /**
             * 记录组件 businessUser：表示业务员名称。
             */
            String businessUser,
            /**
             * 记录组件 bookkeeper：表示记账人名称。
             */
            String bookkeeper,
            /**
             * 记录组件 maker：表示制单人名称。
             */
            String maker,
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            VoucherStatus status,
            /**
             * 记录组件 totalDebit：表示接口入参或出参中的 totalDebit 字段。
             */
            BigDecimal totalDebit,
            /**
             * 记录组件 totalCredit：表示接口入参或出参中的 totalCredit 字段。
             */
            BigDecimal totalCredit,
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            /**
             * 记录组件 totalDebitCny：表示接口入参或出参中的 totalDebitCny 字段。
             */
            BigDecimal totalDebitCny,
            /**
             * 记录组件 totalCreditCny：表示接口入参或出参中的 totalCreditCny 字段。
             */
            BigDecimal totalCreditCny,
            /**
             * 记录组件 createdBy：表示接口入参或出参中的 createdBy 字段。
             */
            String createdBy,
            /**
             * 记录组件 postedBy：表示接口入参或出参中的 postedBy 字段。
             */
            String postedBy,
            /**
             * 记录组件 sourceBizNo：表示接口入参或出参中的 sourceBizNo 字段。
             */
            String sourceBizNo,
            /**
             * 记录组件 sourceType：表示凭证来源业务类型，手工凭证为空。
             */
            AccountingSourceType sourceType,
            /**
             * 记录组件 sourceId：表示凭证来源业务主键，配合 sourceType 反向查看原始单据。
             */
            Long sourceId,
            /**
             * 记录组件 sourceTitle：表示凭证来源业务标题。
             */
            String sourceTitle,
            /**
             * 记录组件 lines：表示接口入参或出参中的 lines 字段。
             */
            List<VoucherLineView> lines
    ) {
    }

    @Schema(description = "凭证列表导出请求。作者：ratel；开发人员：WenZhang；联系方式：18782945613。")
    /**
     * VoucherExportRequest 数据传输记录。
     * 
     * <p>用于承载 VoucherExportRequest 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record VoucherExportRequest(
            @Schema(description = "选中的凭证 ID。为空时按筛选条件导出。")
            /**
             * 记录组件 ids：表示接口入参或出参中的 ids 字段。
             */
            List<Long> ids,
            @Schema(description = "凭证开始日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 startDate：表示接口入参或出参中的 startDate 字段。
             */
            LocalDate startDate,
            @Schema(description = "凭证结束日期，格式 yyyy-MM-dd。")
            /**
             * 记录组件 endDate：表示接口入参或出参中的 endDate 字段。
             */
            LocalDate endDate,
            @Schema(description = "所属年月，格式 yyyy-MM，等值匹配。")
            /**
             * 记录组件 belongMonth：表示接口入参或出参中的 belongMonth 字段。
             */
            String belongMonth,
            @Schema(description = "项目字典编码，等值匹配。")
            /**
             * 记录组件 projectCode：表示接口入参或出参中的 projectCode 字段。
             */
            String projectCode,
            @Schema(description = "凭证号，包含匹配。")
            /**
             * 记录组件 voucherNo：表示接口入参或出参中的 voucherNo 字段。
             */
            String voucherNo,
            @Schema(description = "摘要，包含匹配。")
            /**
             * 记录组件 summary：表示接口入参或出参中的 summary 字段。
             */
            String summary,
            @Schema(description = "来源业务单号，包含匹配。")
            /**
             * 记录组件 sourceBizNo：表示接口入参或出参中的 sourceBizNo 字段。
             */
            String sourceBizNo,
            @Schema(description = "凭证状态，等值匹配。")
            /**
             * 记录组件 status：表示接口入参或出参中的 status 字段。
             */
            VoucherStatus status,
            @Schema(description = "制单人，包含匹配。")
            /**
             * 记录组件 createdBy：表示接口入参或出参中的 createdBy 字段。
             */
            String createdBy
    ) {
    }

    /**
     * 自动凭证业务来源行。
     *
     * <p>用于会计平台页面展示可制证业务单据，包含来源单号、往来单位、项目、金额和是否已存在未作废凭证。</p>
     */
    public record AccountingSourceView(
            /**
             * 记录组件 sourceType：表示业务来源类型。
             */
            AccountingSourceType sourceType,
            /**
             * 记录组件 sourceId：表示来源业务单据主键。
             */
            Long sourceId,
            /**
             * 记录组件 sourceNo：表示来源业务单号。
             */
            String sourceNo,
            /**
             * 记录组件 sourceTitle：表示来源业务单据标题，供页面和流水展示。
             */
            String sourceTitle,
            /**
             * 记录组件 projectCode：表示来源业务单据项目编码。
             */
            String projectCode,
            /**
             * 记录组件 projectName：表示来源业务单据项目名称快照。
             */
            String projectName,
            /**
             * 记录组件 businessDate：表示来源业务发生日期。
             */
            LocalDate businessDate,
            /**
             * 记录组件 partnerName：表示供应商、客户或其他往来单位名称。
             */
            String partnerName,
            /**
             * 记录组件 amount：表示来源业务原币金额。
             */
            BigDecimal amount,
            /**
             * 记录组件 amountCny：表示来源业务折人民币金额。
             */
            BigDecimal amountCny,
            /**
             * 记录组件 currencyCode：表示来源业务币种编码。
             */
            String currencyCode,
            /**
             * 记录组件 currencyName：表示来源业务币种名称快照。
             */
            String currencyName,
            /**
             * 记录组件 exchangeRateToCny：表示来源业务发生时折人民币汇率。
             */
            BigDecimal exchangeRateToCny,
            /**
             * 记录组件 statusText：表示来源业务状态中文描述。
             */
            String statusText,
            /**
             * 记录组件 voucherGenerated：表示该来源单据是否已经存在未作废凭证。
             */
            boolean voucherGenerated
    ) {
    }

    /**
     * 自动生成凭证请求。
     *
     * <p>用于会计平台按业务来源和用户选择的借贷科目生成凭证草稿。</p>
     */
    public record AutoVoucherRequest(
            @Schema(description = "业务来源类型。")
            @NotNull AccountingSourceType sourceType,
            @Schema(description = "业务来源单据主键。")
            @NotNull Long sourceId,
            @Schema(description = "借方科目 ID。")
            @NotNull Long debitSubjectId,
            @Schema(description = "贷方科目 ID。")
            @NotNull Long creditSubjectId,
            @Schema(description = "凭证摘要；为空时按来源单据自动生成。")
            @Size(max = 200, message = "凭证摘要长度不能超过200个字符")
            String summary,
            @Schema(description = "是否允许重复生成凭证；默认不允许。")
            Boolean allowDuplicate
    ) {
    }

    /**
     * 自动生成凭证结果。
     *
     * <p>用于返回生成后的凭证草稿和来源单据说明，前端可直接引导用户进入凭证记账页面继续审核。</p>
     */
    public record AutoVoucherResult(
            /**
             * 记录组件 voucher：表示自动生成的凭证草稿。
             */
            VoucherView voucher,
            /**
             * 记录组件 source：表示本次制证使用的业务来源。
             */
            AccountingSourceView source,
            /**
             * 记录组件 message：表示生成结果说明。
             */
            String message
    ) {
    }

    /**
     * 凭证来源详情字段。
     *
     * <p>用于查看来源弹窗以通用键值方式展示不同业务模块的核心单据信息。</p>
     */
    public record VoucherSourceField(
            /** 字段中文名称。 */
            String label,
            /** 字段展示值，后端统一转换为用户可读文本。 */
            String value
    ) {
    }

    /**
     * 凭证来源详情。
     *
     * <p>用于凭证记账模块从凭证反向查看采购、应收应付、库存或出纳来源单据。</p>
     */
    public record VoucherSourceDetail(
            /** 来源业务类型。 */
            AccountingSourceType sourceType,
            /** 来源业务主键。 */
            Long sourceId,
            /** 来源业务单号。 */
            String sourceNo,
            /** 来源业务标题。 */
            String sourceTitle,
            /** 来源模块中文名称。 */
            String sourceModule,
            /** 通用字段明细。 */
            List<VoucherSourceField> fields
    ) {
    }

    /**
     * 凭证导入识别结果。
     *
     * <p>用于图片或 PDF 上传识别后返回前端，前端只回填草稿分录，由用户继续确认保存。</p>
     */
    public record VoucherImportResult(
            /** 模型识别出的凭证日期，无法判断时为空，前端保留当前表单日期。 */
            LocalDate voucherDate,
            /** 模型识别出的整张凭证摘要，无法判断时为空。 */
            String summary,
            /** 模型识别出的来源单号或原始单据编号，无法判断时为空。 */
            String sourceBizNo,
            /** 识别出的凭证分录草稿。 */
            List<VoucherImportLine> lines,
            /** 无法自动处理但需要用户关注的提示。 */
            List<String> warnings
    ) {
    }

    /**
     * 凭证导入分录草稿。
     *
     * <p>字段中的 subjectId 只有在当前账套启用末级科目能明确命中时才返回，否则为空，前端要求用户手工选择。</p>
     */
    public record VoucherImportLine(
            /** 匹配成功的会计科目 ID，未匹配成功时为空。 */
            Long subjectId,
            /** 模型识别或匹配到的科目名称。 */
            String subjectName,
            /** 模型识别或匹配到的完整级联科目名称。 */
            String subjectFullName,
            /** 分录摘要。 */
            String summary,
            /** 借方金额，未识别时为 0。 */
            BigDecimal debitAmount,
            /** 贷方金额，未识别时为 0。 */
            BigDecimal creditAmount,
            /** 币种编码，默认 CNY。 */
            String currencyCode,
            /** 币种名称，默认人民币。 */
            String currencyName,
            /** 折人民币汇率，默认 1。 */
            BigDecimal exchangeRateToCny,
            /** 辅助核算信息。 */
            String auxiliary,
            /** 该行识别可信度，0 到 1。 */
            BigDecimal confidence,
            /** 该行需要用户关注的提示。 */
            String warning
    ) {
    }

    /**
     * TrialBalanceRow 数据传输记录。
     * 
     * <p>用于承载 TrialBalanceRow 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    public record TrialBalanceRow(
            /**
             * 记录组件 subjectId：表示接口入参或出参中的 subjectId 字段。
             */
            Long subjectId,
            /**
             * 记录组件 subjectCode：表示接口入参或出参中的 subjectCode 字段。
             */
            String subjectCode,
            /**
             * 记录组件 subjectName：表示接口入参或出参中的 subjectName 字段。
             */
            String subjectName,
            /**
             * 记录组件 debitAmount：表示接口入参或出参中的 debitAmount 字段。
             */
            BigDecimal debitAmount,
            /**
             * 记录组件 creditAmount：表示接口入参或出参中的 creditAmount 字段。
             */
            BigDecimal creditAmount,
            /**
             * 记录组件 balance：表示接口入参或出参中的 balance 字段。
             */
            BigDecimal balance
    ) {
    }
}
