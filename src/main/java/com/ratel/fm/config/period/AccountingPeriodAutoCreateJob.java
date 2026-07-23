package com.ratel.fm.config.period;

import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.service.period.AccountingPeriodService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 会计期间自动生成任务。
 *
 * <p>实现目的：系统启动后和每月初自动检查账套缺失的会计期间，避免财务人员进入会计期间页面后还需要手工创建当月期间。</p>
 */
@Component
public class AccountingPeriodAutoCreateJob {

    /** 日志对象，用于记录自动补齐期间的执行结果和可忽略异常。 */
    private static final Logger log = LoggerFactory.getLogger(AccountingPeriodAutoCreateJob.class);

    /** 所属公司根字典编码，所有账套公司均维护在该根节点下。 */
    private static final String ORGANIZATION_ROOT_CODE = "ORGANIZATION";

    /** 基础字典仓库，用于读取启用的所属公司账套。 */
    private final BasicDictionaryRepository dictionaryRepository;

    /** 会计期间服务，用于执行实际的缺失期间补齐逻辑。 */
    private final AccountingPeriodService accountingPeriodService;

    /**
     * 构造会计期间自动生成任务。
     *
     * <p>实现步骤：接收字典仓库和会计期间服务，任务触发时先解析公司账套，再委托服务层创建缺失期间。</p>
     */
    public AccountingPeriodAutoCreateJob(
            BasicDictionaryRepository dictionaryRepository,
            AccountingPeriodService accountingPeriodService
    ) {
        this.dictionaryRepository = dictionaryRepository;
        this.accountingPeriodService = accountingPeriodService;
    }

    /**
     * 应用启动完成后自动检查并补齐会计期间。
     *
     * <p>实现步骤：
     * 1. 等待 Spring Boot 应用和基础数据初始化完成；
     * 2. 读取启用的所属公司账套编码；
     * 3. 为每个账套补齐当前年度到当前月的缺失期间。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensurePeriodsOnStartup() {
        run("启动检查");
    }

    /**
     * 每月初自动检查并补齐会计期间。
     *
     * <p>实现步骤：
     * 1. 每月 1 日 00:05 触发，避开 0 点整的其他初始化任务；
     * 2. 读取当前启用账套；
     * 3. 补齐上次最新期间之后到当前月之间缺失的期间。</p>
     */
    @Scheduled(cron = "0 5 0 1 * ?", zone = "Asia/Shanghai")
    public void ensurePeriodsMonthly() {
        run("每月定时");
    }

    /**
     * 执行会计期间自动补齐。
     *
     * <p>实现步骤：
     * 1. 解析启用所属公司编码；
     * 2. 调用服务层幂等创建缺失期间；
     * 3. 记录新增数量，异常时只记录日志，不阻断应用启动或其他定时任务。</p>
     */
    private void run(String triggerName) {
        try {
            // 步骤1：启用公司账套决定自动生成范围，默认公司作为兜底账套始终包含。
            Set<String> companyCodes = enabledCompanyCodes();
            // 步骤2：服务层负责按账套查最新期间并补齐缺失年月。
            int createdCount = accountingPeriodService.ensurePeriodsForCompanies(companyCodes);
            log.info("会计期间{}完成，账套数={}，新增期间数={}", triggerName, companyCodes.size(), createdCount);
        } catch (Exception ex) {
            log.warn("会计期间{}失败，已跳过本次自动补齐", triggerName, ex);
        }
    }

    /**
     * 读取启用的所属公司账套编码。
     *
     * <p>实现步骤：
     * 1. 读取全部字典并定位 ORGANIZATION 根节点；
     * 2. 从每个字典节点向上追溯，保留位于所属公司根节点下且自身及上级均启用的节点；
     * 3. 返回公司字典编码集合，最后加入默认公司编码作为兜底。</p>
     */
    private Set<String> enabledCompanyCodes() {
        // 步骤1：全部字典按排序读取，EntityGraph 已加载父级，便于向上判断层级归属。
        List<BasicDictionary> dictionaries = dictionaryRepository.findAllByOrderBySortOrderAscIdAsc();
        BasicDictionary root = dictionaries.stream()
                .filter(item -> ORGANIZATION_ROOT_CODE.equals(item.getCode()))
                .findFirst()
                .orElse(null);

        // 步骤2：根节点不存在或停用时仍返回默认公司，保证系统首次启动可用。
        Set<String> codes = new LinkedHashSet<>();
        if (root != null && root.isEnabled()) {
            for (BasicDictionary dictionary : dictionaries) {
                if (!ORGANIZATION_ROOT_CODE.equals(dictionary.getCode()) && isEnabledDescendantOf(dictionary, root)) {
                    codes.add(dictionary.getCode());
                }
            }
        }
        codes.add(CompanyScope.DEFAULT_COMPANY_CODE);
        return codes;
    }

    /**
     * 判断字典节点是否属于启用的所属公司树。
     *
     * <p>实现步骤：从当前节点向父级追溯，任一节点停用则排除；追溯到目标根节点则判定为有效公司账套。</p>
     */
    private boolean isEnabledDescendantOf(BasicDictionary dictionary, BasicDictionary root) {
        // 步骤1：visited 防止异常循环父级导致启动任务死循环。
        Set<Long> visited = new HashSet<>();
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (!cursor.isEnabled()) {
                return false;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            if (root.getId() != null && root.getId().equals(cursor.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }
}
