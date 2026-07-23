package com.ratel.fm.web.dto.cashier;

import com.ratel.fm.domain.cashier.CashierTransactionStatus;
import com.ratel.fm.domain.cashier.CashierTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 出纳管理接口 DTO。
 *
 * <p>实现目的：承载资金流水新增、确认、取消、查询和导出数据，保证出纳台账和会计平台使用同一字段口径。</p>
 */
public final class CashierDtos {

    private CashierDtos() {
    }

    /** 出纳资金流水保存请求。 */
    public record CashierTransactionRequest(
            @Schema(description = "出纳交易日期。")
            @NotNull LocalDate transactionDate,
            @Schema(description = "出纳流水类型。")
            @NotNull CashierTransactionType transactionType,
            @Schema(description = "项目字典编码。")
            @NotBlank(message = "项目不能为空")
            @Size(max = 80, message = "项目编码长度不能超过80个字符")
            String projectCode,
            @Schema(description = "项目名称快照。")
            @Size(max = 160, message = "项目名称长度不能超过160个字符")
            String projectName,
            @Schema(description = "往来单位名称。")
            @Size(max = 160, message = "往来单位名称长度不能超过160个字符")
            String partnerName,
            @Schema(description = "银行或现金账户。")
            @Size(max = 160, message = "银行或现金账户长度不能超过160个字符")
            String bankAccount,
            @Schema(description = "结算方式。")
            @Size(max = 120, message = "结算方式长度不能超过120个字符")
            String settlementMethod,
            @Schema(description = "原币金额。")
            @Positive(message = "金额必须大于0")
            BigDecimal amount,
            @Schema(description = "币种编码。")
            @Size(max = 20, message = "币种编码长度不能超过20个字符")
            String currencyCode,
            @Schema(description = "币种名称快照。")
            @Size(max = 80, message = "币种名称长度不能超过80个字符")
            String currencyName,
            @Schema(description = "折人民币汇率。")
            @Positive(message = "汇率必须大于0")
            BigDecimal exchangeRateToCny,
            @Schema(description = "关联业务单号。")
            @Size(max = 300, message = "关联业务单号长度不能超过300个字符")
            String relatedBizNo,
            @Schema(description = "摘要。")
            @NotBlank(message = "摘要不能为空")
            @Size(max = 200, message = "摘要长度不能超过200个字符")
            String summary,
            @Schema(description = "备注。")
            @Size(max = 2000, message = "备注长度不能超过2000个字符")
            String remark
    ) {
    }

    /** 出纳资金流水视图。 */
    public record CashierTransactionView(
            Long id,
            String organizationCode,
            String transactionNo,
            LocalDate transactionDate,
            CashierTransactionType transactionType,
            CashierTransactionStatus status,
            String projectCode,
            String projectName,
            String partnerName,
            String bankAccount,
            String settlementMethod,
            BigDecimal amount,
            String currencyCode,
            String currencyName,
            BigDecimal exchangeRateToCny,
            BigDecimal amountCny,
            String relatedBizNo,
            String summary,
            String remark,
            String createdBy,
            String confirmedBy,
            OffsetDateTime confirmedTime,
            Long voucherId,
            String voucherNo
    ) {
    }

    /** 出纳流水导出请求。 */
    public record CashierExportRequest(
            List<Long> ids,
            LocalDate startDate,
            LocalDate endDate,
            CashierTransactionType transactionType,
            CashierTransactionStatus status,
            String projectCode,
            String partnerName,
            String relatedBizNo
    ) {
    }
}
