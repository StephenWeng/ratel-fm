package com.ratel.fm.domain.period;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 会计期间。
 *
 * <p>实现目的：按所属公司维护月度会计期间，承接用友/金蝶常见的“期间开启、月末检查、结账、反结账”流程。</p>
 */
@Entity
@Table(name = "fm_accounting_periods")
@Comment("会计期间表，按所属公司维护每个月度期间的开启和关闭状态")
public class AccountingPeriod extends BaseEntity {

    /** 所属公司字典编码，即账套编码，会计期间按该字段隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为会计期间账套隔离标识")
    private String organizationCode;

    /** 期间编码，格式 yyyy-MM。 */
    @Column(nullable = false, length = 20)
    @Comment("会计期间编码，格式yyyy-MM")
    private String periodCode;

    /** 期间开始日期。 */
    @Column(nullable = false)
    @Comment("会计期间开始日期")
    private LocalDate startDate;

    /** 期间结束日期。 */
    @Column(nullable = false)
    @Comment("会计期间结束日期")
    private LocalDate endDate;

    /** 期间状态：开启或关闭。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("会计期间状态")
    private AccountingPeriodStatus status = AccountingPeriodStatus.OPEN;

    /** 关闭期间的操作人账号。 */
    @Column(length = 80)
    @Comment("关闭期间操作人账号")
    private String closedBy;

    /** 关闭期间的操作时间。 */
    @Comment("关闭期间操作时间")
    private OffsetDateTime closedTime;

    /** 期间备注，记录月结说明或反结账原因。 */
    @Column(length = 2000)
    @Comment("会计期间备注")
    private String remark;

    /** 获取所属公司字典编码。 */
    public String getOrganizationCode() { return organizationCode; }

    /** 设置所属公司字典编码。 */
    public void setOrganizationCode(String organizationCode) { this.organizationCode = organizationCode; }

    /** 获取期间编码。 */
    public String getPeriodCode() { return periodCode; }

    /** 设置期间编码。 */
    public void setPeriodCode(String periodCode) { this.periodCode = periodCode; }

    /** 获取期间开始日期。 */
    public LocalDate getStartDate() { return startDate; }

    /** 设置期间开始日期。 */
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    /** 获取期间结束日期。 */
    public LocalDate getEndDate() { return endDate; }

    /** 设置期间结束日期。 */
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    /** 获取期间状态。 */
    public AccountingPeriodStatus getStatus() { return status; }

    /** 设置期间状态。 */
    public void setStatus(AccountingPeriodStatus status) { this.status = status; }

    /** 获取关闭操作人账号。 */
    public String getClosedBy() { return closedBy; }

    /** 设置关闭操作人账号。 */
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    /** 获取关闭操作时间。 */
    public OffsetDateTime getClosedTime() { return closedTime; }

    /** 设置关闭操作时间。 */
    public void setClosedTime(OffsetDateTime closedTime) { this.closedTime = closedTime; }

    /** 获取期间备注。 */
    public String getRemark() { return remark; }

    /** 设置期间备注。 */
    public void setRemark(String remark) { this.remark = remark; }
}
