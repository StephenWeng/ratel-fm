package com.ratel.fm.web.dto.period;

import com.ratel.fm.domain.period.AccountingPeriodStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 会计期间接口 DTO。
 *
 * <p>实现目的：统一会计期间创建、关闭检查、结账和反结账接口的数据结构，前端据此展示月结控制点。</p>
 */
public final class AccountingPeriodDtos {

    private AccountingPeriodDtos() {
    }

    /** 会计期间保存请求。 */
    public record AccountingPeriodRequest(
            @Schema(description = "期间编码，格式 yyyy-MM。")
            @NotBlank(message = "期间编码不能为空")
            @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "期间编码格式必须为yyyy-MM")
            String periodCode,
            @Schema(description = "期间备注。")
            @Size(max = 2000, message = "期间备注长度不能超过2000个字符")
            String remark
    ) {
    }

    /** 会计期间关闭或反关闭请求。 */
    public record AccountingPeriodActionRequest(
            @Schema(description = "期间操作说明。")
            @Size(max = 2000, message = "期间操作说明长度不能超过2000个字符")
            String remark
    ) {
    }

    /** 会计期间列表视图。 */
    public record AccountingPeriodView(
            Long id,
            String organizationCode,
            String periodCode,
            LocalDate startDate,
            LocalDate endDate,
            AccountingPeriodStatus status,
            String closedBy,
            OffsetDateTime closedTime,
            String remark
    ) {
    }

    /** 月结检查结果。 */
    public record PeriodCloseCheckView(
            String periodCode,
            boolean closable,
            List<String> blockingItems,
            List<String> warningItems
    ) {
    }
}
