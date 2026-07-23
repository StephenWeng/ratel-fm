package com.ratel.fm.config.bootstrap;

import com.alibaba.fastjson2.JSON;
import com.ratel.fm.domain.auth.PermissionCode;
import com.ratel.fm.domain.auth.Role;
import com.ratel.fm.domain.auth.MenuType;
import com.ratel.fm.domain.auth.SystemMenu;
import com.ratel.fm.domain.auth.UserAccount;
import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.domain.finance.AccountingSubject;
import com.ratel.fm.domain.finance.SubjectCategory;
import com.ratel.fm.domain.workflow.WorkflowApproverType;
import com.ratel.fm.domain.workflow.WorkflowConfig;
import com.ratel.fm.domain.workflow.WorkflowDefinition;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import com.ratel.fm.repository.finance.AccountingSubjectRepository;
import com.ratel.fm.repository.auth.RoleRepository;
import com.ratel.fm.repository.auth.SystemMenuRepository;
import com.ratel.fm.repository.auth.UserAccountRepository;
import com.ratel.fm.repository.workflow.WorkflowConfigRepository;
import com.ratel.fm.repository.workflow.WorkflowDefinitionRepository;
import com.ratel.fm.security.CompanyScope;
import com.ratel.fm.service.auth.PasswordHashService;
import com.ratel.fm.web.dto.workflow.WorkflowDtos.WorkflowNodeView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 系统基础数据初始化配置。
 *
 * <p>首次启动时初始化默认角色、管理员账号和企业会计准则标准科目。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
@ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer {

    /**
     * 常量 DEFAULT_ADMIN_IDENTITY_NO：保存 DEFAULT_ADMIN_IDENTITY_NO 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final String DEFAULT_ADMIN_IDENTITY_NO = "ADMIN_IDENTITY_0001";
    /**
     * 常量 DEFAULT_ADMIN_PHONE：保存 DEFAULT_ADMIN_PHONE 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final String DEFAULT_ADMIN_PHONE = "18782945613";
    /**
     * 常量 ADMINISTRATIVE_DIVISION_RESOURCE：保存 ADMINISTRATIVE_DIVISION_RESOURCE 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private static final String ADMINISTRATIVE_DIVISION_RESOURCE = "data/administrative-divisions.csv";
    /**
     * 常量 ACCOUNTING_SUBJECT_RESOURCE：企业会计准则标准科目初始化 CSV 路径。
     */
    private static final String ACCOUNTING_SUBJECT_RESOURCE = "data/accounting-subjects.csv";

    /**
     * 初始化系统基础数据。
     *
     * <p>实现步骤：
     * 1. 创建或更新管理员、财务人员、业务操作员三个基础角色；
     * 2. 首次启动时创建默认管理员账号，已存在时补齐身份证号和联系电话；
     * 3. 从 CSV 创建企业会计准则标准科目树，已存在的科目只补齐基础信息。</p>
     */
    @Bean
    @Order(1)
    @Transactional
    public CommandLineRunner bootstrapData(
            RoleRepository roleRepository,
            UserAccountRepository userRepository,
            SystemMenuRepository menuRepository,
            AccountingSubjectRepository subjectRepository,
            BasicDictionaryRepository dictionaryRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowConfigRepository workflowConfigRepository,
            PasswordHashService passwordHashService,
            @Value("${app.bootstrap.admin-username}") String adminUsername,
            @Value("${app.bootstrap.admin-password}") String adminPassword,
            @Value("${app.bootstrap.admin-identity-no:" + DEFAULT_ADMIN_IDENTITY_NO + "}") String adminIdentityNo
    ) {
        return args -> {
            // 步骤零：初始化菜单资源，角色授权以菜单编码为核心，菜单再映射后端权限码。
            seedMenus(menuRepository);

            // 步骤一：创建或更新拥有全部权限的管理员角色。
            Role adminRole = roleRepository.findByCode("ADMIN").orElseGet(Role::new);
            adminRole.setCode("ADMIN");
            adminRole.setName("系统管理员");
            adminRole.setDescription("拥有系统全部模块权限");
            adminRole.setMenus(new HashSet<>(menuRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()));
            adminRole.setPermissions(permissionsFromMenus(adminRole.getMenus()));
            roleRepository.save(adminRole);

            // 步骤二：创建或更新财务人员角色，授予财务和报表相关权限。
            Role financeRole = roleRepository.findByCode("FINANCE").orElseGet(Role::new);
            financeRole.setCode("FINANCE");
            financeRole.setName("财务人员");
            financeRole.setDescription("维护科目、凭证与财务报表");
            financeRole.setMenus(resolveMenus(menuRepository, Set.of(
                    "MODULE_PERSONAL", "PAGE_PROFILE", "BTN_PROFILE_EDIT", "BTN_PROFILE_PASSWORD", "BTN_PROFILE_AVATAR", "BTN_LOGOUT",
                    "MODULE_FINANCE", "PAGE_SUBJECTS", "BTN_SUBJECT_CREATE", "BTN_SUBJECT_EDIT", "BTN_SUBJECT_DELETE",
                    "BTN_SUBJECT_BATCH_DELETE",
                    "PAGE_VOUCHERS", "BTN_VOUCHER_CREATE", "BTN_VOUCHER_EDIT", "BTN_VOUCHER_POST", "BTN_VOUCHER_VOID",
                    "BTN_VOUCHER_QUERY", "BTN_VOUCHER_VIEW", "BTN_VOUCHER_EXPORT", "BTN_VOUCHER_BATCH_DELETE", "BTN_VOUCHER_LINE_DELETE", "BTN_VOUCHER_ATTACHMENT",
                    "PAGE_ACCOUNTING_PERIODS", "BTN_PERIOD_CREATE", "BTN_PERIOD_CLOSE_CHECK", "BTN_PERIOD_CLOSE", "BTN_PERIOD_REOPEN",
                    "PAGE_CASHIER", "BTN_CASHIER_CREATE", "BTN_CASHIER_CONFIRM", "BTN_CASHIER_CANCEL", "BTN_CASHIER_EXPORT", "BTN_CASHIER_BATCH_DELETE",
                    "PAGE_ACCOUNTING_PLATFORM", "BTN_ACCOUNTING_SOURCE_QUERY", "BTN_ACCOUNTING_AUTO_VOUCHER",
                    "MODULE_AR_AP", "PAGE_AR_AP", "BTN_AR_AP_CREATE", "BTN_AR_AP_EXPORT", "BTN_AR_AP_BATCH_DELETE", "BTN_AR_AP_ATTACHMENT", "BTN_AR_AP_SETTLE", "PAGE_AR_AP_STATS", "BTN_AR_AP_STATS_QUERY", "BTN_AR_AP_STATS_EXPORT",
                    "MODULE_WORKFLOW", "PAGE_WORKFLOW_CENTER", "BTN_WORKFLOW_APPROVE", "BTN_WORKFLOW_VIEW",
                    "MODULE_HOME", "PAGE_DASHBOARD", "BTN_DASHBOARD_REFRESH", "BTN_DASHBOARD_VOUCHER_LINK",
                    "BTN_DASHBOARD_REPORT_LINK", "BTN_DASHBOARD_SEARCH_LINK",
                    "MODULE_REPORT", "PAGE_REPORTS", "BTN_REPORT_QUERY",
                    "MODULE_SEARCH", "PAGE_SEARCH", "BTN_SEARCH_QUERY", "PAGE_AI_STATUS"
            )));
            financeRole.setPermissions(permissionsFromMenus(financeRole.getMenus()));
            roleRepository.save(financeRole);

            // 步骤三：创建或更新业务操作员角色，授予采购、物流、库存、应收应付等业务权限。
            Role operatorRole = roleRepository.findByCode("OPERATOR").orElseGet(Role::new);
            operatorRole.setCode("OPERATOR");
            operatorRole.setName("业务操作员");
            operatorRole.setDescription("维护采购、物流并查看检索");
            operatorRole.setMenus(resolveMenus(menuRepository, Set.of(
                    "MODULE_PERSONAL", "PAGE_PROFILE", "BTN_PROFILE_EDIT", "BTN_PROFILE_PASSWORD", "BTN_PROFILE_AVATAR", "BTN_LOGOUT",
                    "MODULE_OPERATION", "PAGE_PURCHASE", "BTN_PURCHASE_CREATE", "BTN_PURCHASE_EDIT", "BTN_PURCHASE_STATUS",
                    "BTN_PURCHASE_VIEW", "BTN_PURCHASE_EXPORT", "BTN_PURCHASE_BATCH_DELETE", "BTN_PURCHASE_LINE_DELETE", "BTN_PURCHASE_ATTACHMENT",
                    "PAGE_SHIPMENTS", "BTN_SHIPMENT_CREATE", "BTN_SHIPMENT_EDIT", "BTN_SHIPMENT_STATUS", "BTN_SHIPMENT_LOG", "BTN_SHIPMENT_EXPORT", "BTN_SHIPMENT_BATCH_DELETE", "BTN_SHIPMENT_ATTACHMENT",
                    "MODULE_INVENTORY", "PAGE_INVENTORY", "BTN_INVENTORY_CREATE", "BTN_INVENTORY_EXPORT", "BTN_INVENTORY_BATCH_DELETE", "BTN_INVENTORY_ATTACHMENT",
                    "MODULE_AR_AP", "PAGE_AR_AP", "BTN_AR_AP_CREATE", "BTN_AR_AP_EXPORT", "BTN_AR_AP_BATCH_DELETE", "BTN_AR_AP_ATTACHMENT", "BTN_AR_AP_SETTLE", "PAGE_AR_AP_STATS", "BTN_AR_AP_STATS_QUERY", "BTN_AR_AP_STATS_EXPORT",
                    "MODULE_WORKFLOW", "PAGE_WORKFLOW_CENTER", "BTN_WORKFLOW_APPROVE", "BTN_WORKFLOW_VIEW",
                    "MODULE_HOME", "PAGE_DASHBOARD", "BTN_DASHBOARD_REFRESH", "BTN_DASHBOARD_PURCHASE_LINK",
                    "BTN_DASHBOARD_REPORT_LINK", "BTN_DASHBOARD_SEARCH_LINK",
                    "MODULE_REPORT", "PAGE_REPORTS", "BTN_REPORT_QUERY",
                    "MODULE_SEARCH", "PAGE_SEARCH", "BTN_SEARCH_QUERY", "PAGE_AI_STATUS", "PAGE_ASSISTANT", "BTN_ASSISTANT_ASK", "BTN_ASSISTANT_VOICE"
            )));
            operatorRole.setPermissions(permissionsFromMenus(operatorRole.getMenus()));
            roleRepository.save(operatorRole);

            // 步骤四：首次启动创建默认管理员；默认管理员账号和身份证号都保持唯一。
            UserAccount defaultAdmin = userRepository.findByIdentityNo(adminIdentityNo)
                    .or(() -> userRepository.findByUsername(adminUsername))
                    .orElse(null);
            if (defaultAdmin == null) {
                // 变量说明：admin 保存当前步骤计算、查询或转换得到的中间结果。
                UserAccount admin = new UserAccount();
                admin.setUsername(adminUsername);
                admin.setRealName("系统管理员");
                admin.setPasswordHash(passwordHashService.hash(adminPassword));
                admin.setDepartment("管理部");
                admin.setOrganizationCode(CompanyScope.DEFAULT_COMPANY_CODE);
                admin.setPosition("Administrator");
                admin.setIdentityNo(adminIdentityNo);
                admin.setPhone(DEFAULT_ADMIN_PHONE);
                admin.setEnabled(true);
                admin.setRoles(Set.of(adminRole));
                userRepository.save(admin);
            } else {
                if (defaultAdmin.getIdentityNo() == null || defaultAdmin.getIdentityNo().isBlank()) {
                    defaultAdmin.setIdentityNo(adminIdentityNo);
                }
                if (defaultAdmin.getPhone() == null || defaultAdmin.getPhone().isBlank()) {
                    defaultAdmin.setPhone(DEFAULT_ADMIN_PHONE);
                }
                if (defaultAdmin.getOrganizationCode() == null
                        || defaultAdmin.getOrganizationCode().isBlank()
                        || "ratel".equals(defaultAdmin.getOrganizationCode())) {
                    defaultAdmin.setOrganizationCode(CompanyScope.DEFAULT_COMPANY_CODE);
                }
                if (passwordHashService.missing(defaultAdmin.getPasswordHash())
                        || (passwordHashService.requiresUpgrade(defaultAdmin.getPasswordHash())
                        && passwordHashService.matches(adminPassword, defaultAdmin.getPasswordHash()))) {
                    defaultAdmin.setPasswordHash(passwordHashService.hash(adminPassword));
                }
                defaultAdmin.setRoles(Set.of(adminRole));
                userRepository.save(defaultAdmin);
            }

            // 步骤五：初始化企业会计准则标准科目，保证系统首次启动后拥有完整科目树。
            seedStandardSubjects(subjectRepository);

            // 步骤六：初始化基础信息字典根节点和常用业务选项，采购、物流页面启动后可直接选择启用字典。
            seedDictionaries(dictionaryRepository);
            migrateAdministrativeDivisionCodes(dictionaryRepository);
            seedAdministrativeDivisions(dictionaryRepository);
            seedWorkflowDefaults(workflowDefinitionRepository, workflowConfigRepository);
        };
    }

    /**
     * 初始化企业会计准则标准科目树。
     *
     * <p>实现步骤：
     * 1. 从 resources/data/accounting-subjects.csv 读取标准科目；
     * 2. 按文件顺序先创建分类根节点，再按 parent_code 关联二级标准科目；
     * 3. 已存在科目只补齐缺失的账套、类别、父级、层级和说明，不覆盖用户维护过的说明；
     * 4. 保存编码到实体映射，供后续子科目建立父子关系。</p>
     */
    private void seedStandardSubjects(AccountingSubjectRepository subjectRepository) {
        // 变量说明：rows 保存 CSV 中的标准科目行，文件顺序保证父级在子级之前出现。
        List<SubjectSeedRow> rows = loadStandardSubjectRows();
        // 变量说明：subjectsByCode 保存本次启动已读取或创建的科目，供子科目快速查找父级。
        Map<String, AccountingSubject> subjectsByCode = new HashMap<>();
        for (SubjectSeedRow row : rows) {
            AccountingSubject parent = null;
            if (row.parentCode() != null && !row.parentCode().isBlank()) {
                parent = subjectsByCode.get(row.parentCode());
                if (parent == null) {
                    parent = subjectRepository.findByOrganizationCodeAndCode(CompanyScope.DEFAULT_COMPANY_CODE, row.parentCode())
                            .orElse(null);
                }
            }
            AccountingSubject subject = subjectRepository.findByOrganizationCodeAndCode(CompanyScope.DEFAULT_COMPANY_CODE, row.code())
                    .orElseGet(AccountingSubject::new);
            subject.setOrganizationCode(CompanyScope.DEFAULT_COMPANY_CODE);
            subject.setCode(row.code());
            subject.setName(row.name());
            subject.setCategory(row.category());
            subject.setParent(parent);
            subject.setSubjectLevel(parent == null ? 1 : parent.getSubjectLevel() + 1);
            subject.setEnabled(true);
            if (subject.getDescription() == null || subject.getDescription().isBlank()) {
                subject.setDescription(row.description());
            }
            subjectsByCode.put(row.code(), subjectRepository.save(subject));
        }
    }

    /**
     * 读取标准科目 CSV。
     *
     * <p>实现步骤：
     * 1. 打开 classpath 下的标准科目资源；
     * 2. 跳过表头和空行；
     * 3. 按逗号拆分为编码、名称、类别、父级编码和说明；
     * 4. 将文本类别转换为 SubjectCategory 枚举。</p>
     */
    private List<SubjectSeedRow> loadStandardSubjectRows() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(ACCOUNTING_SUBJECT_RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> line != null && !line.isBlank())
                    .map(this::parseSubjectSeedRow)
                    .toList();
        } catch (Exception ex) {
            throw new IllegalStateException("读取标准会计科目初始化数据失败: " + ACCOUNTING_SUBJECT_RESOURCE, ex);
        }
    }

    /**
     * 解析单行标准科目 CSV。
     *
     * <p>实现步骤：
     * 1. 采用最多五段切分，确保说明字段中即使后续出现逗号也不会破坏前四列；
     * 2. 清理字段两侧空白；
     * 3. 组装为不可变记录供初始化流程使用。</p>
     */
    private SubjectSeedRow parseSubjectSeedRow(String line) {
        String[] columns = line.split(",", 5);
        if (columns.length < 5) {
            throw new IllegalStateException("标准会计科目初始化数据列数不足: " + line);
        }
        String parentCode = columns[3].trim();
        return new SubjectSeedRow(
                columns[0].trim(),
                columns[1].trim(),
                SubjectCategory.valueOf(columns[2].trim()),
                parentCode.isBlank() ? null : parentCode,
                columns[4].trim()
        );
    }

    /**
     * 标准科目初始化行。
     *
     * <p>字段含义：code 为科目编码，name 为科目名称，category 为系统科目类别，parentCode 为父级科目编码，description 为初始化说明。</p>
     */
    private record SubjectSeedRow(String code, String name, SubjectCategory category, String parentCode, String description) {
    }

    /**
     * 初始化系统菜单。
     *
     * <p>实现步骤：先创建模块菜单，再创建页面菜单，最后创建按钮菜单。重复启动时按菜单编码更新名称、类型、路径和权限码。</p>
     */
    private void seedMenus(SystemMenuRepository menuRepository) {
        seedMenu(menuRepository, "MODULE_SYSTEM", "系统管理", MenuType.MODULE, null, null, 100, null);
        // 变量说明：basic 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu basic = seedMenu(menuRepository, "MODULE_BASIC", "基础信息", MenuType.MODULE, null, null, 160, null);
        seedMenu(menuRepository, "PAGE_BASIC_DICTIONARIES", "字典管理", MenuType.PAGE, basic, "/basic-dictionaries", 161, PermissionCode.BASIC_DICT_MANAGE);
        seedMenu(menuRepository, "BTN_DICT_CREATE", "新增字典", MenuType.BUTTON, menuRepository.findByCode("PAGE_BASIC_DICTIONARIES").orElseThrow(), null, 162, PermissionCode.BASIC_DICT_MANAGE);
        seedMenu(menuRepository, "BTN_DICT_EDIT", "编辑字典", MenuType.BUTTON, menuRepository.findByCode("PAGE_BASIC_DICTIONARIES").orElseThrow(), null, 163, PermissionCode.BASIC_DICT_MANAGE);
        seedMenu(menuRepository, "BTN_DICT_DELETE", "删除字典", MenuType.BUTTON, menuRepository.findByCode("PAGE_BASIC_DICTIONARIES").orElseThrow(), null, 164, PermissionCode.BASIC_DICT_MANAGE);
        seedMenu(menuRepository, "PAGE_USERS", "人员管理", MenuType.PAGE, basic, "/users", 170, PermissionCode.SYSTEM_USER_MANAGE);
        seedMenu(menuRepository, "BTN_USER_CREATE", "新增人员", MenuType.BUTTON, menuRepository.findByCode("PAGE_USERS").orElseThrow(), null, 111, PermissionCode.SYSTEM_USER_MANAGE);
        seedMenu(menuRepository, "BTN_USER_EDIT", "编辑人员", MenuType.BUTTON, menuRepository.findByCode("PAGE_USERS").orElseThrow(), null, 112, PermissionCode.SYSTEM_USER_MANAGE);
        seedMenu(menuRepository, "BTN_USER_DELETE", "删除人员", MenuType.BUTTON, menuRepository.findByCode("PAGE_USERS").orElseThrow(), null, 113, PermissionCode.SYSTEM_USER_MANAGE);
        seedMenu(menuRepository, "BTN_USER_PASSWORD", "修改人员密码", MenuType.BUTTON, menuRepository.findByCode("PAGE_USERS").orElseThrow(), null, 114, PermissionCode.SYSTEM_USER_MANAGE);
        seedMenu(menuRepository, "BTN_USER_AVATAR", "维护人员头像", MenuType.BUTTON, menuRepository.findByCode("PAGE_USERS").orElseThrow(), null, 115, PermissionCode.SYSTEM_USER_MANAGE);
        seedMenu(menuRepository, "BTN_USER_BATCH_DELETE", "批量删除人员", MenuType.BUTTON, menuRepository.findByCode("PAGE_USERS").orElseThrow(), null, 116, PermissionCode.SYSTEM_USER_MANAGE);
        seedMenu(menuRepository, "PAGE_ROLES", "角色管理", MenuType.PAGE, basic, "/roles", 180, PermissionCode.SYSTEM_ROLE_MANAGE);
        seedMenu(menuRepository, "BTN_ROLE_CREATE", "新增角色", MenuType.BUTTON, menuRepository.findByCode("PAGE_ROLES").orElseThrow(), null, 121, PermissionCode.SYSTEM_ROLE_MANAGE);
        seedMenu(menuRepository, "BTN_ROLE_EDIT", "编辑角色", MenuType.BUTTON, menuRepository.findByCode("PAGE_ROLES").orElseThrow(), null, 122, PermissionCode.SYSTEM_ROLE_MANAGE);
        seedMenu(menuRepository, "BTN_ROLE_DELETE", "删除角色", MenuType.BUTTON, menuRepository.findByCode("PAGE_ROLES").orElseThrow(), null, 123, PermissionCode.SYSTEM_ROLE_MANAGE);
        seedMenu(menuRepository, "PAGE_MENUS", "菜单管理", MenuType.PAGE, basic, "/menus", 190, PermissionCode.SYSTEM_ROLE_MANAGE);
        seedMenu(menuRepository, "BTN_MENU_CREATE", "新增菜单", MenuType.BUTTON, menuRepository.findByCode("PAGE_MENUS").orElseThrow(), null, 131, PermissionCode.SYSTEM_ROLE_MANAGE);
        seedMenu(menuRepository, "BTN_MENU_EDIT", "编辑菜单", MenuType.BUTTON, menuRepository.findByCode("PAGE_MENUS").orElseThrow(), null, 132, PermissionCode.SYSTEM_ROLE_MANAGE);
        seedMenu(menuRepository, "BTN_MENU_DELETE", "删除菜单", MenuType.BUTTON, menuRepository.findByCode("PAGE_MENUS").orElseThrow(), null, 133, PermissionCode.SYSTEM_ROLE_MANAGE);
        // 变量说明：personal 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu personal = seedMenu(menuRepository, "MODULE_PERSONAL", "个人中心", MenuType.MODULE, null, null, 150, null);
        seedMenu(menuRepository, "PAGE_PROFILE", "个人中心", MenuType.PAGE, personal, null, 151, null);
        seedMenu(menuRepository, "BTN_PROFILE_EDIT", "编辑个人信息", MenuType.BUTTON, menuRepository.findByCode("PAGE_PROFILE").orElseThrow(), null, 152, null);
        seedMenu(menuRepository, "BTN_PROFILE_PASSWORD", "修改个人密码", MenuType.BUTTON, menuRepository.findByCode("PAGE_PROFILE").orElseThrow(), null, 153, null);
        seedMenu(menuRepository, "BTN_PROFILE_AVATAR", "上传个人头像", MenuType.BUTTON, menuRepository.findByCode("PAGE_PROFILE").orElseThrow(), null, 154, null);
        seedMenu(menuRepository, "BTN_LOGOUT", "退出登录", MenuType.BUTTON, menuRepository.findByCode("PAGE_PROFILE").orElseThrow(), null, 155, null);
        disableMenuIfExists(menuRepository, "BTN_OPEN_DOC");
        disableMenuIfExists(menuRepository, "MODULE_SYSTEM");

        // 变量说明：home 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu home = seedMenu(menuRepository, "MODULE_HOME", "首页概览", MenuType.MODULE, null, null, 10, null);
        seedMenu(menuRepository, "PAGE_DASHBOARD", "首页概览", MenuType.PAGE, home, "/dashboard", 11, PermissionCode.REPORT_VIEW);
        seedMenu(menuRepository, "BTN_DASHBOARD_REFRESH", "刷新首页", MenuType.BUTTON, menuRepository.findByCode("PAGE_DASHBOARD").orElseThrow(), null, 12, PermissionCode.REPORT_VIEW);
        seedMenu(menuRepository, "BTN_DASHBOARD_VOUCHER_LINK", "首页新增凭证入口", MenuType.BUTTON, menuRepository.findByCode("PAGE_DASHBOARD").orElseThrow(), null, 13, PermissionCode.REPORT_VIEW);
        seedMenu(menuRepository, "BTN_DASHBOARD_PURCHASE_LINK", "首页采购入口", MenuType.BUTTON, menuRepository.findByCode("PAGE_DASHBOARD").orElseThrow(), null, 14, PermissionCode.REPORT_VIEW);
        seedMenu(menuRepository, "BTN_DASHBOARD_REPORT_LINK", "首页报表入口", MenuType.BUTTON, menuRepository.findByCode("PAGE_DASHBOARD").orElseThrow(), null, 15, PermissionCode.REPORT_VIEW);
        seedMenu(menuRepository, "BTN_DASHBOARD_SEARCH_LINK", "首页检索入口", MenuType.BUTTON, menuRepository.findByCode("PAGE_DASHBOARD").orElseThrow(), null, 16, PermissionCode.REPORT_VIEW);

        // 变量说明：finance 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu finance = seedMenu(menuRepository, "MODULE_FINANCE", "财务管理", MenuType.MODULE, null, null, 200, null);
        seedMenu(menuRepository, "PAGE_SUBJECTS", "会计科目", MenuType.PAGE, finance, "/subjects", 210, PermissionCode.FINANCE_SUBJECT_MANAGE);
        seedMenu(menuRepository, "BTN_SUBJECT_CREATE", "新增科目", MenuType.BUTTON, menuRepository.findByCode("PAGE_SUBJECTS").orElseThrow(), null, 211, PermissionCode.FINANCE_SUBJECT_MANAGE);
        seedMenu(menuRepository, "BTN_SUBJECT_EDIT", "编辑科目", MenuType.BUTTON, menuRepository.findByCode("PAGE_SUBJECTS").orElseThrow(), null, 212, PermissionCode.FINANCE_SUBJECT_MANAGE);
        seedMenu(menuRepository, "BTN_SUBJECT_DELETE", "删除科目", MenuType.BUTTON, menuRepository.findByCode("PAGE_SUBJECTS").orElseThrow(), null, 213, PermissionCode.FINANCE_SUBJECT_MANAGE);
        seedMenu(menuRepository, "BTN_SUBJECT_BATCH_DELETE", "批量删除科目", MenuType.BUTTON, menuRepository.findByCode("PAGE_SUBJECTS").orElseThrow(), null, 214, PermissionCode.FINANCE_SUBJECT_MANAGE);
        seedMenu(menuRepository, "PAGE_VOUCHERS", "凭证记账", MenuType.PAGE, finance, "/vouchers", 220, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_CREATE", "新增凭证", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 221, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_EDIT", "编辑凭证", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 222, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_POST", "凭证过账", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 223, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_VOID", "凭证作废", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 224, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_QUERY", "查询凭证", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 225, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_VIEW", "查看凭证明细", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 226, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_EXPORT", "导出凭证", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 227, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_BATCH_DELETE", "批量删除凭证", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 228, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_LINE_DELETE", "删除凭证分录", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 229, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_VOUCHER_ATTACHMENT", "维护凭证附件", MenuType.BUTTON, menuRepository.findByCode("PAGE_VOUCHERS").orElseThrow(), null, 230, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "PAGE_ACCOUNTING_PERIODS", "会计期间", MenuType.PAGE, finance, "/accounting-periods", 230, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_PERIOD_CREATE", "创建会计期间", MenuType.BUTTON, menuRepository.findByCode("PAGE_ACCOUNTING_PERIODS").orElseThrow(), null, 231, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_PERIOD_CLOSE_CHECK", "月结检查", MenuType.BUTTON, menuRepository.findByCode("PAGE_ACCOUNTING_PERIODS").orElseThrow(), null, 232, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_PERIOD_CLOSE", "关闭会计期间", MenuType.BUTTON, menuRepository.findByCode("PAGE_ACCOUNTING_PERIODS").orElseThrow(), null, 233, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_PERIOD_REOPEN", "反结账", MenuType.BUTTON, menuRepository.findByCode("PAGE_ACCOUNTING_PERIODS").orElseThrow(), null, 234, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "PAGE_CASHIER", "出纳管理", MenuType.PAGE, finance, "/cashier", 240, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_CASHIER_CREATE", "新增出纳流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_CASHIER").orElseThrow(), null, 241, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_CASHIER_CONFIRM", "确认出纳流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_CASHIER").orElseThrow(), null, 242, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_CASHIER_CANCEL", "取消出纳流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_CASHIER").orElseThrow(), null, 243, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_CASHIER_EXPORT", "导出出纳流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_CASHIER").orElseThrow(), null, 244, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_CASHIER_BATCH_DELETE", "批量删除出纳流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_CASHIER").orElseThrow(), null, 245, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "PAGE_ACCOUNTING_PLATFORM", "会计平台", MenuType.PAGE, finance, "/accounting-platform", 250, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_ACCOUNTING_SOURCE_QUERY", "查询制证来源", MenuType.BUTTON, menuRepository.findByCode("PAGE_ACCOUNTING_PLATFORM").orElseThrow(), null, 251, PermissionCode.FINANCE_VOUCHER_MANAGE);
        seedMenu(menuRepository, "BTN_ACCOUNTING_AUTO_VOUCHER", "自动生成凭证", MenuType.BUTTON, menuRepository.findByCode("PAGE_ACCOUNTING_PLATFORM").orElseThrow(), null, 252, PermissionCode.FINANCE_VOUCHER_MANAGE);

        // 变量说明：operation 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu operation = seedMenu(menuRepository, "MODULE_OPERATION", "业务管理", MenuType.MODULE, null, null, 300, null);
        seedMenu(menuRepository, "PAGE_PURCHASE", "采购管理", MenuType.PAGE, operation, "/purchase-orders", 310, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_CREATE", "新增采购单", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 311, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_EDIT", "编辑采购单", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 312, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_STATUS", "变更采购状态", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 313, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_VIEW", "查看采购单", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 314, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_EXPORT", "导出采购单", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 315, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_BATCH_DELETE", "批量删除采购单", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 316, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_LINE_DELETE", "删除采购明细", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 317, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "BTN_PURCHASE_ATTACHMENT", "维护采购附件", MenuType.BUTTON, menuRepository.findByCode("PAGE_PURCHASE").orElseThrow(), null, 318, PermissionCode.PURCHASE_MANAGE);
        seedMenu(menuRepository, "PAGE_SHIPMENTS", "物流管理", MenuType.PAGE, operation, "/shipments", 320, PermissionCode.LOGISTICS_MANAGE);
        seedMenu(menuRepository, "BTN_SHIPMENT_CREATE", "新增物流单", MenuType.BUTTON, menuRepository.findByCode("PAGE_SHIPMENTS").orElseThrow(), null, 321, PermissionCode.LOGISTICS_MANAGE);
        seedMenu(menuRepository, "BTN_SHIPMENT_EDIT", "编辑物流单", MenuType.BUTTON, menuRepository.findByCode("PAGE_SHIPMENTS").orElseThrow(), null, 322, PermissionCode.LOGISTICS_MANAGE);
        seedMenu(menuRepository, "BTN_SHIPMENT_STATUS", "确认物流状态", MenuType.BUTTON, menuRepository.findByCode("PAGE_SHIPMENTS").orElseThrow(), null, 323, PermissionCode.LOGISTICS_MANAGE);
        seedMenu(menuRepository, "BTN_SHIPMENT_EXPORT", "导出物流单", MenuType.BUTTON, menuRepository.findByCode("PAGE_SHIPMENTS").orElseThrow(), null, 324, PermissionCode.LOGISTICS_MANAGE);
        seedMenu(menuRepository, "BTN_SHIPMENT_BATCH_DELETE", "批量删除物流单", MenuType.BUTTON, menuRepository.findByCode("PAGE_SHIPMENTS").orElseThrow(), null, 325, PermissionCode.LOGISTICS_MANAGE);
        seedMenu(menuRepository, "BTN_SHIPMENT_ATTACHMENT", "维护物流附件", MenuType.BUTTON, menuRepository.findByCode("PAGE_SHIPMENTS").orElseThrow(), null, 326, PermissionCode.LOGISTICS_MANAGE);
        seedMenu(menuRepository, "BTN_SHIPMENT_LOG", "查看物流流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_SHIPMENTS").orElseThrow(), null, 327, PermissionCode.LOGISTICS_MANAGE);

        // 变量说明：inventory 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu inventory = seedMenu(menuRepository, "MODULE_INVENTORY", "库存管理", MenuType.MODULE, null, null, 400, null);
        seedMenu(menuRepository, "PAGE_INVENTORY", "库存台账", MenuType.PAGE, inventory, "/inventory", 410, PermissionCode.INVENTORY_MANAGE);
        seedMenu(menuRepository, "BTN_INVENTORY_CREATE", "新增库存流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_INVENTORY").orElseThrow(), null, 411, PermissionCode.INVENTORY_MANAGE);
        seedMenu(menuRepository, "BTN_INVENTORY_EXPORT", "导出库存台账", MenuType.BUTTON, menuRepository.findByCode("PAGE_INVENTORY").orElseThrow(), null, 412, PermissionCode.INVENTORY_MANAGE);
        seedMenu(menuRepository, "BTN_INVENTORY_BATCH_DELETE", "批量删除库存流水", MenuType.BUTTON, menuRepository.findByCode("PAGE_INVENTORY").orElseThrow(), null, 413, PermissionCode.INVENTORY_MANAGE);
        seedMenu(menuRepository, "BTN_INVENTORY_ATTACHMENT", "维护库存附件", MenuType.BUTTON, menuRepository.findByCode("PAGE_INVENTORY").orElseThrow(), null, 414, PermissionCode.INVENTORY_MANAGE);

        // 变量说明：arAp 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu arAp = seedMenu(menuRepository, "MODULE_AR_AP", "应收应付", MenuType.MODULE, null, null, 500, null);
        seedMenu(menuRepository, "PAGE_AR_AP", "应收应付", MenuType.PAGE, arAp, "/ar-ap", 510, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "BTN_AR_AP_CREATE", "新增应收应付单", MenuType.BUTTON, menuRepository.findByCode("PAGE_AR_AP").orElseThrow(), null, 511, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "BTN_AR_AP_EXPORT", "导出应收应付", MenuType.BUTTON, menuRepository.findByCode("PAGE_AR_AP").orElseThrow(), null, 512, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "BTN_AR_AP_BATCH_DELETE", "批量删除应收应付", MenuType.BUTTON, menuRepository.findByCode("PAGE_AR_AP").orElseThrow(), null, 513, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "BTN_AR_AP_ATTACHMENT", "维护应收应付附件", MenuType.BUTTON, menuRepository.findByCode("PAGE_AR_AP").orElseThrow(), null, 514, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "BTN_AR_AP_SETTLE", "收付核销", MenuType.BUTTON, menuRepository.findByCode("PAGE_AR_AP").orElseThrow(), null, 515, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "PAGE_AR_AP_STATS", "收付统计页签", MenuType.PAGE, arAp, null, 520, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "BTN_AR_AP_STATS_QUERY", "查询收付统计", MenuType.BUTTON, menuRepository.findByCode("PAGE_AR_AP_STATS").orElseThrow(), null, 521, PermissionCode.AR_AP_MANAGE);
        seedMenu(menuRepository, "BTN_AR_AP_STATS_EXPORT", "导出收付统计", MenuType.BUTTON, menuRepository.findByCode("PAGE_AR_AP_STATS").orElseThrow(), null, 522, PermissionCode.AR_AP_MANAGE);

        // 变量说明：workflow 保存审批中心模块菜单，流程审批和流程配置维护统一放在该模块下。
        SystemMenu workflow = seedMenu(menuRepository, "MODULE_WORKFLOW", "审批中心", MenuType.MODULE, null, null, 600, null);
        seedMenu(menuRepository, "PAGE_WORKFLOW_CENTER", "审批中心", MenuType.PAGE, workflow, "/workflow-center", 610, PermissionCode.WORKFLOW_USE);
        seedMenu(menuRepository, "BTN_WORKFLOW_APPROVE", "审批流程", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_CENTER").orElseThrow(), null, 611, PermissionCode.WORKFLOW_USE);
        seedMenu(menuRepository, "BTN_WORKFLOW_VIEW", "流程查看", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_CENTER").orElseThrow(), null, 612, PermissionCode.WORKFLOW_USE);
        seedMenu(menuRepository, "PAGE_WORKFLOW_CONFIGS", "流程管理", MenuType.PAGE, workflow, "/workflow-configs", 620, PermissionCode.WORKFLOW_MANAGE);
        seedMenu(menuRepository, "BTN_WORKFLOW_CONFIG_CREATE", "新增流程配置", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_CONFIGS").orElseThrow(), null, 621, PermissionCode.WORKFLOW_MANAGE);
        seedMenu(menuRepository, "BTN_WORKFLOW_CONFIG_EDIT", "编辑流程配置", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_CONFIGS").orElseThrow(), null, 622, PermissionCode.WORKFLOW_MANAGE);
        seedMenu(menuRepository, "BTN_WORKFLOW_CONFIG_DELETE", "删除流程配置", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_CONFIGS").orElseThrow(), null, 623, PermissionCode.WORKFLOW_MANAGE);
        seedMenu(menuRepository, "PAGE_WORKFLOW_DEFINITIONS", "流程定义", MenuType.PAGE, workflow, "/workflow-definitions", 630, PermissionCode.WORKFLOW_MANAGE);
        seedMenu(menuRepository, "BTN_WORKFLOW_DEFINITION_CREATE", "新增流程定义", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_DEFINITIONS").orElseThrow(), null, 631, PermissionCode.WORKFLOW_MANAGE);
        seedMenu(menuRepository, "BTN_WORKFLOW_DEFINITION_EDIT", "编辑流程定义", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_DEFINITIONS").orElseThrow(), null, 632, PermissionCode.WORKFLOW_MANAGE);
        seedMenu(menuRepository, "BTN_WORKFLOW_DEFINITION_DELETE", "删除流程定义", MenuType.BUTTON, menuRepository.findByCode("PAGE_WORKFLOW_DEFINITIONS").orElseThrow(), null, 633, PermissionCode.WORKFLOW_MANAGE);

        // 变量说明：search 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu search = seedMenu(menuRepository, "MODULE_SEARCH", "智能检索", MenuType.MODULE, null, null, 700, null);
        seedMenu(menuRepository, "PAGE_SEARCH", "智能检索", MenuType.PAGE, search, "/search", 810, PermissionCode.SEARCH_VIEW);
        seedMenu(menuRepository, "BTN_SEARCH_QUERY", "执行检索", MenuType.BUTTON, menuRepository.findByCode("PAGE_SEARCH").orElseThrow(), null, 811, PermissionCode.SEARCH_VIEW);
        seedMenu(menuRepository, "PAGE_ASSISTANT", "ratel助手", MenuType.PAGE, search, "/assistant", 820, PermissionCode.AI_ASSISTANT_USE);
        seedMenu(menuRepository, "BTN_ASSISTANT_ASK", "ratel提问", MenuType.BUTTON, menuRepository.findByCode("PAGE_ASSISTANT").orElseThrow(), null, 821, PermissionCode.AI_ASSISTANT_USE);
        seedMenu(menuRepository, "BTN_ASSISTANT_VOICE", "语音操作", MenuType.BUTTON, menuRepository.findByCode("PAGE_ASSISTANT").orElseThrow(), null, 822, PermissionCode.AI_ASSISTANT_USE);
        seedMenu(menuRepository, "PAGE_AI_STATUS", "AI 组件状态", MenuType.PAGE, search, "/ai-status", 830, PermissionCode.SEARCH_VIEW);

        // 变量说明：report 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu report = seedMenu(menuRepository, "MODULE_REPORT", "统计报表", MenuType.MODULE, null, null, 900, null);
        seedMenu(menuRepository, "PAGE_REPORTS", "统计报表", MenuType.PAGE, report, "/reports", 910, PermissionCode.REPORT_VIEW);
        seedMenu(menuRepository, "BTN_REPORT_QUERY", "查询报表", MenuType.BUTTON, menuRepository.findByCode("PAGE_REPORTS").orElseThrow(), null, 911, PermissionCode.REPORT_VIEW);

        // 变量说明：audit 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu audit = seedMenu(menuRepository, "MODULE_AUDIT", "日志管理", MenuType.MODULE, null, null, 990, null);
        seedMenu(menuRepository, "PAGE_OPERATION_LOGS", "日志管理", MenuType.PAGE, audit, "/operation-logs", 991, PermissionCode.AUDIT_LOG_VIEW);
        seedMenu(menuRepository, "BTN_OPERATION_LOG_QUERY", "查询操作日志", MenuType.BUTTON, menuRepository.findByCode("PAGE_OPERATION_LOGS").orElseThrow(), null, 992, PermissionCode.AUDIT_LOG_VIEW);
        disableMenuIfExists(menuRepository, "MODULE_ASSISTANT");
    }

    /**
     * 初始化基础信息字典。
     *
     * <p>实现步骤：
     * 1. 创建项目、采购方、物流方、物料、仓库、客户/供应商、部门、组织、岗位、币种、银行账户、行政区划和业务单据选项固定根字典；
     * 2. 创建默认项目、默认采购方、默认物流方、默认物料、默认仓库、默认客户/供应商、默认银行账户、人员基础资料、常用币种和业务表单常用选项；
     * 3. 重复启动时只更新名称、排序和启用状态，不删除用户后续维护的数据。</p>
     */
    private void seedDictionaries(BasicDictionaryRepository dictionaryRepository) {
        // 变量说明：projectRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary projectRoot = seedDictionary(dictionaryRepository, "PROJECT", "项目", null, 5, true, "凭证、采购、物流、库存和应收应付共用的项目字典");
        // 变量说明：supplierRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary supplierRoot = seedDictionary(dictionaryRepository, "SUPPLIER", "采购方", null, 10, true, "采购管理使用的供应商/采购方基础字典");
        // 变量说明：carrierRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary carrierRoot = seedDictionary(dictionaryRepository, "CARRIER", "物流方", null, 20, true, "物流管理使用的承运商/物流方基础字典");
        // 变量说明：materialRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary materialRoot = seedDictionary(dictionaryRepository, "MATERIAL", "物料", null, 30, true, "采购管理和库存台账共用的物料字典");
        // 变量说明：warehouseRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary warehouseRoot = seedDictionary(dictionaryRepository, "WAREHOUSE", "仓库", null, 40, true, "库存台账使用的仓库字典");
        // 变量说明：partnerRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary partnerRoot = seedDictionary(dictionaryRepository, "PARTNER", "客户/供应商", null, 50, true, "应收应付使用的客户和供应商往来单位字典");
        // 变量说明：departmentRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary departmentRoot = seedDictionary(dictionaryRepository, "DEPARTMENT", "部门", null, 60, true, "人员管理使用的部门字典");
        // 变量说明：organizationRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary organizationRoot = seedDictionary(dictionaryRepository, "ORGANIZATION", "所属公司", null, 70, true, "登录账套和数据隔离使用的所属公司字典");
        // 变量说明：positionRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary positionRoot = seedDictionary(dictionaryRepository, "POSITION", "岗位", null, 80, true, "人员管理使用的岗位字典");
        // 变量说明：currencyRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary currencyRoot = seedDictionary(dictionaryRepository, "CURRENCY", "币种", null, 90, true, "涉金额业务使用的币种字典");
        // 变量说明：bankAccountRoot 保存出纳流水和收付核销共用的银行账户字典根节点。
        BasicDictionary bankAccountRoot = seedDictionary(dictionaryRepository, "BANK_ACCOUNT", "银行账户", null, 95, true, "出纳管理和收付核销使用的银行账户字典");
        seedDictionary(dictionaryRepository, "ADMINISTRATIVE_DIVISION", "全国行政区划", null, 100, true, "全国省、市、区县、乡镇街道行政区划字典");
        seedDictionary(dictionaryRepository, "PROJECT_DEFAULT", "默认项目", projectRoot, 10, true, "系统预置项目，可按实际项目维护或停用");
        seedDictionary(dictionaryRepository, "PROJECT_INTERNAL", "内部管理项目", projectRoot, 20, true, "系统预置内部管理项目，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "SUPPLIER_DEFAULT", "默认采购方", supplierRoot, 10, true, "系统预置采购方，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "CARRIER_DEFAULT", "默认物流方", carrierRoot, 10, true, "系统预置物流方，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "MATERIAL_DEFAULT", "默认物料", materialRoot, 10, true, "系统预置物料，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "WAREHOUSE_MAIN", "主仓库", warehouseRoot, 10, true, "系统预置仓库，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "PARTNER_DEFAULT", "默认往来单位", partnerRoot, 10, true, "系统预置客户/供应商，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "DEPARTMENT_ADMIN", "管理部", departmentRoot, 10, true, "系统预置部门，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "ORGANIZATION_RATEL", "ratel", organizationRoot, 10, true, "系统预置所属公司，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "POSITION_ADMINISTRATOR", "Administrator", positionRoot, 10, true, "系统预置岗位，可按实际业务修改或停用");
        seedDictionary(dictionaryRepository, "CNY", "人民币", currencyRoot, 10, true, "系统默认币种，折人民币汇率固定为 1");
        seedDictionary(dictionaryRepository, "USD", "美元", currencyRoot, 20, true, "常用外币，业务录入时保存当时折人民币汇率");
        seedDictionary(dictionaryRepository, "EUR", "欧元", currencyRoot, 30, true, "常用外币，业务录入时保存当时折人民币汇率");
        seedDictionary(dictionaryRepository, "JPY", "日元", currencyRoot, 40, true, "常用外币，业务录入时保存当时折人民币汇率");
        seedDictionary(dictionaryRepository, "HKD", "港币", currencyRoot, 50, true, "常用外币，业务录入时保存当时折人民币汇率");
        seedDictionary(dictionaryRepository, "GBP", "英镑", currencyRoot, 60, true, "常用外币，业务录入时保存当时折人民币汇率");
        seedDictionary(dictionaryRepository, "BANK_ACCOUNT_DEFAULT", "默认银行账户", bankAccountRoot, 10, true, "系统预置银行账户，可按实际收付款账户维护或停用");
        seedBusinessFormDictionaries(dictionaryRepository);
    }

    /**
     * 初始化金蝶风格业务表单选项字典。
     *
     * <p>实现步骤：
     * 1. 创建采购、物流、应收应付、来源单据和结算相关根字典；
     * 2. 给每个根字典补齐常用业务选项，新增表单可以直接下拉选择；
     * 3. 只按编码创建或更新预置项，不删除用户在字典管理中扩展的选项。</p>
     */
    private void seedBusinessFormDictionaries(BasicDictionaryRepository dictionaryRepository) {
        // 变量说明：purchaseDocumentTypeRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary purchaseDocumentTypeRoot = seedDictionary(dictionaryRepository, "PURCHASE_DOCUMENT_TYPE", "采购单据类型", null, 110, true, "采购订单使用的单据类型字典");
        // 变量说明：purchaseBusinessTypeRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary purchaseBusinessTypeRoot = seedDictionary(dictionaryRepository, "PURCHASE_BUSINESS_TYPE", "采购业务类型", null, 120, true, "采购订单使用的业务类型字典");
        // 变量说明：logisticsDocumentTypeRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary logisticsDocumentTypeRoot = seedDictionary(dictionaryRepository, "LOGISTICS_DOCUMENT_TYPE", "物流单据类型", null, 130, true, "物流单使用的单据类型字典");
        // 变量说明：transportModeRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary transportModeRoot = seedDictionary(dictionaryRepository, "TRANSPORT_MODE", "运输方式", null, 140, true, "物流管理使用的运输方式字典");
        // 变量说明：arApDocumentTypeRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary arApDocumentTypeRoot = seedDictionary(dictionaryRepository, "AR_AP_DOCUMENT_TYPE", "应收应付单据类型", null, 150, true, "应收应付使用的单据类型字典");
        // 变量说明：sourceBillTypeRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary sourceBillTypeRoot = seedDictionary(dictionaryRepository, "SOURCE_BILL_TYPE", "来源单据类型", null, 160, true, "业务单据追溯使用的来源类型字典");
        // 变量说明：paymentTermsRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary paymentTermsRoot = seedDictionary(dictionaryRepository, "PAYMENT_TERMS", "收付款条件", null, 170, true, "应收应付和采购结算使用的账期字典");
        // 变量说明：settlementMethodRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary settlementMethodRoot = seedDictionary(dictionaryRepository, "SETTLEMENT_METHOD", "结算方式", null, 180, true, "资金结算使用的方式字典");
        // 变量说明：deliveryTermsRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary deliveryTermsRoot = seedDictionary(dictionaryRepository, "DELIVERY_TERMS", "交货条件", null, 190, true, "采购和物流使用的交货条件字典");
        // 变量说明：unitRoot 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary unitRoot = seedDictionary(dictionaryRepository, "UNIT", "计量单位", null, 200, true, "物料明细和库存流水使用的计量单位字典");
        // 变量说明：purchaseCancelTypeRoot 保存采购取消动作使用的取消类型字典根节点。
        BasicDictionary purchaseCancelTypeRoot = seedDictionary(dictionaryRepository, "PURCHASE_CANCEL_TYPE", "采购取消类型", null, 210, true, "取消采购动作使用的取消类型字典");

        seedDictionary(dictionaryRepository, "PURCHASE_DOC_STANDARD", "标准采购订单", purchaseDocumentTypeRoot, 10, true, "采购订单默认单据类型");
        seedDictionary(dictionaryRepository, "PURCHASE_DOC_EXPENSE", "费用采购订单", purchaseDocumentTypeRoot, 20, true, "费用类采购单据类型");
        seedDictionary(dictionaryRepository, "PURCHASE_DOC_ASSET", "资产采购订单", purchaseDocumentTypeRoot, 30, true, "资产类采购单据类型");
        seedDictionary(dictionaryRepository, "PURCHASE_BIZ_STANDARD", "标准采购", purchaseBusinessTypeRoot, 10, true, "常规采购业务");
        seedDictionary(dictionaryRepository, "PURCHASE_BIZ_OUTSOURCING", "委外采购", purchaseBusinessTypeRoot, 20, true, "委外加工采购业务");
        seedDictionary(dictionaryRepository, "PURCHASE_BIZ_ASSET", "资产采购", purchaseBusinessTypeRoot, 30, true, "固定资产采购业务");

        seedDictionary(dictionaryRepository, "LOGISTICS_DOC_PURCHASE", "采购发运", logisticsDocumentTypeRoot, 10, true, "采购履约发运单据");
        seedDictionary(dictionaryRepository, "LOGISTICS_DOC_SALES", "销售发运", logisticsDocumentTypeRoot, 20, true, "销售履约发运单据");
        seedDictionary(dictionaryRepository, "LOGISTICS_DOC_TRANSFER", "调拨发运", logisticsDocumentTypeRoot, 30, true, "库存调拨发运单据");
        seedDictionary(dictionaryRepository, "TRANSPORT_ROAD", "公路运输", transportModeRoot, 10, true, "默认运输方式");
        seedDictionary(dictionaryRepository, "TRANSPORT_RAIL", "铁路运输", transportModeRoot, 20, true, "铁路干线运输");
        seedDictionary(dictionaryRepository, "TRANSPORT_AIR", "航空运输", transportModeRoot, 30, true, "航空运输");
        seedDictionary(dictionaryRepository, "TRANSPORT_SEA", "海运", transportModeRoot, 40, true, "海运运输");
        seedDictionary(dictionaryRepository, "TRANSPORT_EXPRESS", "快递", transportModeRoot, 50, true, "快递配送");

        seedDictionary(dictionaryRepository, "AR_AP_DOC_AR_SALES", "销售应收", arApDocumentTypeRoot, 10, true, "销售业务形成的应收单");
        seedDictionary(dictionaryRepository, "AR_AP_DOC_AP_PURCHASE", "采购应付", arApDocumentTypeRoot, 20, true, "采购业务形成的应付单");
        seedDictionary(dictionaryRepository, "AR_AP_DOC_AR_OTHER", "其他应收", arApDocumentTypeRoot, 30, true, "非销售业务形成的应收单");
        seedDictionary(dictionaryRepository, "AR_AP_DOC_AP_OTHER", "其他应付", arApDocumentTypeRoot, 40, true, "非采购业务形成的应付单");

        seedDictionary(dictionaryRepository, "SOURCE_PURCHASE_REQUEST", "采购申请", sourceBillTypeRoot, 10, true, "采购源单类型");
        seedDictionary(dictionaryRepository, "SOURCE_PURCHASE_ORDER", "采购订单", sourceBillTypeRoot, 20, true, "采购订单源单类型");
        seedDictionary(dictionaryRepository, "SOURCE_PURCHASE_RECEIPT", "采购入库单", sourceBillTypeRoot, 30, true, "采购入库源单类型");
        seedDictionary(dictionaryRepository, "SOURCE_SALES_ORDER", "销售订单", sourceBillTypeRoot, 40, true, "销售订单源单类型");
        seedDictionary(dictionaryRepository, "SOURCE_SALES_OUTBOUND", "销售出库单", sourceBillTypeRoot, 50, true, "销售出库源单类型");
        seedDictionary(dictionaryRepository, "SOURCE_SHIPMENT", "物流单", sourceBillTypeRoot, 60, true, "物流履约源单类型");
        seedDictionary(dictionaryRepository, "SOURCE_INVENTORY_TRANSFER", "库存调拨单", sourceBillTypeRoot, 70, true, "库存调拨源单类型");
        seedDictionary(dictionaryRepository, "SOURCE_EXPENSE_REQUEST", "费用申请", sourceBillTypeRoot, 80, true, "费用业务源单类型");

        seedDictionary(dictionaryRepository, "PAYMENT_IMMEDIATE", "立即付款", paymentTermsRoot, 10, true, "即时收付款");
        seedDictionary(dictionaryRepository, "PAYMENT_MONTHLY_30", "月结30天", paymentTermsRoot, 20, true, "月结 30 天");
        seedDictionary(dictionaryRepository, "PAYMENT_MONTHLY_60", "月结60天", paymentTermsRoot, 30, true, "月结 60 天");
        seedDictionary(dictionaryRepository, "PAYMENT_AFTER_INVOICE", "票到付款", paymentTermsRoot, 40, true, "收到发票后付款");
        seedDictionary(dictionaryRepository, "SETTLEMENT_BANK_TRANSFER", "银行转账", settlementMethodRoot, 10, true, "默认结算方式");
        seedDictionary(dictionaryRepository, "SETTLEMENT_CASH", "现金", settlementMethodRoot, 20, true, "现金结算");
        seedDictionary(dictionaryRepository, "SETTLEMENT_ACCEPTANCE", "承兑汇票", settlementMethodRoot, 30, true, "票据结算");
        seedDictionary(dictionaryRepository, "SETTLEMENT_ONLINE", "线上支付", settlementMethodRoot, 40, true, "线上渠道结算");
        seedDictionary(dictionaryRepository, "DELIVERY_TO_DOOR", "送货上门", deliveryTermsRoot, 10, true, "供方送货到指定地点");
        seedDictionary(dictionaryRepository, "DELIVERY_SELF_PICKUP", "自提", deliveryTermsRoot, 20, true, "需方自行提货");
        seedDictionary(dictionaryRepository, "DELIVERY_LOGISTICS", "物流配送", deliveryTermsRoot, 30, true, "第三方物流配送");

        seedDictionary(dictionaryRepository, "UNIT_PIECE", "件", unitRoot, 10, true, "通用数量单位");
        seedDictionary(dictionaryRepository, "UNIT_EACH", "个", unitRoot, 20, true, "通用数量单位");
        seedDictionary(dictionaryRepository, "UNIT_BOX", "箱", unitRoot, 30, true, "包装数量单位");
        seedDictionary(dictionaryRepository, "UNIT_KG", "千克", unitRoot, 40, true, "重量单位");
        seedDictionary(dictionaryRepository, "UNIT_TON", "吨", unitRoot, 50, true, "重量单位");
        seedDictionary(dictionaryRepository, "UNIT_METER", "米", unitRoot, 60, true, "长度单位");
        seedDictionary(dictionaryRepository, "PURCHASE_CANCEL_BUSINESS", "业务取消", purchaseCancelTypeRoot, 10, true, "业务原因取消采购");
        seedDictionary(dictionaryRepository, "PURCHASE_CANCEL_SUPPLIER", "供应商原因", purchaseCancelTypeRoot, 20, true, "供应商原因取消采购");
        seedDictionary(dictionaryRepository, "PURCHASE_CANCEL_PRICE", "价格原因", purchaseCancelTypeRoot, 30, true, "价格或预算原因取消采购");
        seedDictionary(dictionaryRepository, "PURCHASE_CANCEL_OTHER", "其他原因", purchaseCancelTypeRoot, 90, true, "其他原因取消采购");
    }

    /**
     * 初始化默认流程模板和模块绑定。
     *
     * <p>实现步骤：
     * 1. 创建或更新默认采购审批流程定义，节点审批范围为管理部/Administrator；
     * 2. 创建或更新采购审批功能模块配置，业务模块发起审批时只依赖 PURCHASE_APPROVAL 编码；
     * 3. 后续用户可在流程定义和流程管理页面替换模板，不需要修改采购代码。</p>
     */
    private void seedWorkflowDefaults(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowConfigRepository workflowConfigRepository
    ) {
        WorkflowDefinition definition = workflowDefinitionRepository
                .findByOrganizationCodeAndCode(CompanyScope.DEFAULT_COMPANY_CODE, "WF_PURCHASE_APPROVAL")
                .orElseGet(WorkflowDefinition::new);
        definition.setOrganizationCode(CompanyScope.DEFAULT_COMPANY_CODE);
        definition.setName("采购审批流程");
        definition.setCode("WF_PURCHASE_APPROVAL");
        definition.setDescription("系统预置采购审批流程，可在流程定义中调整审批节点。");
        definition.setNodesJson(JSON.toJSONString(List.of(new WorkflowNodeView(
                1,
                "采购审批",
                WorkflowApproverType.DEPARTMENT_POSITION,
                null,
                null,
                null,
                "管理部",
                "Administrator",
                "管理部 / Administrator"
        ))));
        definition.setEnabled(true);
        WorkflowDefinition savedDefinition = workflowDefinitionRepository.save(definition);

        WorkflowConfig config = workflowConfigRepository
                .findByOrganizationCodeAndFunctionModuleCode(CompanyScope.DEFAULT_COMPANY_CODE, "PURCHASE_APPROVAL")
                .orElseGet(WorkflowConfig::new);
        config.setOrganizationCode(CompanyScope.DEFAULT_COMPANY_CODE);
        config.setBusinessModuleCode("OPERATION");
        config.setBusinessModuleName("业务管理");
        config.setFunctionModuleCode("PURCHASE_APPROVAL");
        config.setFunctionModuleName("采购审批");
        config.setDefinitionId(savedDefinition.getId());
        config.setDefinitionName(savedDefinition.getName());
        config.setEnabled(true);
        workflowConfigRepository.save(config);
    }

    /**
     * 创建或更新单个基础字典。
     */
    private BasicDictionary seedDictionary(
            BasicDictionaryRepository dictionaryRepository,
            String code,
            String name,
            BasicDictionary parent,
            int sortOrder,
            boolean enabled,
            String description
    ) {
        // 变量说明：dictionary 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary dictionary = dictionaryRepository.findByCode(code).orElseGet(BasicDictionary::new);
        dictionary.setCode(code);
        dictionary.setName(name);
        dictionary.setParent(parent);
        dictionary.setSortOrder(sortOrder);
        dictionary.setEnabled(enabled);
        dictionary.setDescription(description);
        return dictionaryRepository.save(dictionary);
    }

    /**
     * 初始化全国行政区划字典。
     *
     * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
     *
     * <p>实现步骤：
     * 1. 读取 resources/data/administrative-divisions.csv，当前数据为省、市、区县、乡镇街道四级；
     * 2. CSV 中每行包含编码、名称、父级编码、层级、排序、递归继承后的 6 位行政区划代码和数据来源；
     * 3. 按行创建或更新基础字典，父级尚未创建时跳过，避免影响主流程启动。</p>
     */
    private void seedAdministrativeDivisions(BasicDictionaryRepository dictionaryRepository) {
        // 变量说明：resource 保存当前步骤计算、查询或转换得到的中间结果。
        ClassPathResource resource = new ClassPathResource(ADMINISTRATIVE_DIVISION_RESOURCE);
        if (!resource.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            // 行政区划超过四万行，先缓存已有字典，避免逐行反复查询数据库。
            Map<String, BasicDictionary> dictionaryCache = new HashMap<>();
            dictionaryRepository.findAllByOrderBySortOrderAscIdAsc()
                    .forEach(dictionary -> dictionaryCache.put(dictionary.getCode(), dictionary));
            reader.lines()
                    .skip(1)
                    .map(this::parseCsvLine)
                    .filter(row -> row.length >= 5)
                    .forEach(row -> {
                        // 变量说明：code 保存当前步骤计算、查询或转换得到的中间结果。
                        String code = row[0];
                        // 变量说明：name 保存当前步骤计算、查询或转换得到的中间结果。
                        String name = row[1];
                        // 变量说明：parentCode 保存当前步骤计算、查询或转换得到的中间结果。
                        String parentCode = row[2];
                        // 变量说明：sortOrder 保存当前步骤计算、查询或转换得到的中间结果。
                        int sortOrder = parseInt(row[4]);
                        // 变量说明：administrativeCode 保存当前步骤计算、查询或转换得到的中间结果。
                        String administrativeCode = row.length > 5 ? row[5] : "";
                        // 变量说明：source 保存当前步骤计算、查询或转换得到的中间结果。
                        String source = row.length > 6 ? row[6] : "";
                        // 变量说明：parent 保存当前步骤计算、查询或转换得到的中间结果。
                        BasicDictionary parent = dictionaryCache.get(parentCode);
                        if (parent != null) {
                            BasicDictionary dictionary = dictionaryCache.getOrDefault(code, new BasicDictionary());
                            dictionary.setCode(code);
                            dictionary.setName(name);
                            dictionary.setParent(parent);
                            dictionary.setSortOrder(sortOrder);
                            dictionary.setEnabled(true);
                            dictionary.setDescription(administrativeDivisionDescription(code, administrativeCode, source));
                            dictionaryCache.put(code, dictionaryRepository.save(dictionary));
                        }
                    });
        } catch (Exception ex) {
            // 行政区划只是基础资料种子数据，读取失败不能影响系统启动。
        }
    }

    /**
     * 构造行政区划字典说明。
     */
    private String administrativeDivisionDescription(String code, String administrativeCode, String source) {
        // 变量说明：description 保存当前步骤计算、查询或转换得到的中间结果。
        StringBuilder description = new StringBuilder("全国行政区划初始化数据");
        if (administrativeCode != null && !administrativeCode.isBlank()) {
            description.append("；行政区划代码=").append(administrativeCode);
        }
        if (code != null && !code.isBlank() && !code.equals(administrativeCode)) {
            description.append("；统计区划代码=").append(code);
        }
        if (source != null && !source.isBlank()) {
            description.append("；数据来源=").append(source);
        }
        return description.toString();
    }

    /**
     * 迁移旧版全国行政区划编码。
     *
     * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
     *
     * <p>实现步骤：
     * 1. 扫描已经存在的基础字典；
     * 2. 将 `AREA_110000` 这类旧编码转换为 `110000`；
     * 3. 如果纯编码节点已经存在，则删除旧 `AREA_` 重复节点及其下级；
     * 4. 删除后由新的 CSV 初始化流程按纯行政区划代码补齐数据。</p>
     */
    private void migrateAdministrativeDivisionCodes(BasicDictionaryRepository dictionaryRepository) {
        // 变量说明：dictionaries 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionary> dictionaries = dictionaryRepository.findAllByOrderBySortOrderAscIdAsc();
        java.util.Map<String, BasicDictionary> byCode = new java.util.HashMap<>();
        java.util.Map<Long, List<BasicDictionary>> childrenByParentId = new java.util.HashMap<>();
        for (BasicDictionary dictionary : dictionaries) {
            byCode.put(dictionary.getCode(), dictionary);
            if (dictionary.getParent() != null && dictionary.getParent().getId() != null) {
                childrenByParentId
                        .computeIfAbsent(dictionary.getParent().getId(), key -> new java.util.ArrayList<>())
                        .add(dictionary);
            }
        }

        // 变量说明：deleteIds 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> deleteIds = new HashSet<>();
        for (BasicDictionary dictionary : dictionaries) {
            // 变量说明：code 保存当前步骤计算、查询或转换得到的中间结果。
            String code = dictionary.getCode();
            if (code == null || !code.startsWith("AREA_")) {
                continue;
            }
            // 变量说明：normalizedCode 保存当前步骤计算、查询或转换得到的中间结果。
            String normalizedCode = code.substring("AREA_".length());
            // 变量说明：existing 保存当前步骤计算、查询或转换得到的中间结果。
            BasicDictionary existing = byCode.get(normalizedCode);
            if (existing != null && !existing.getId().equals(dictionary.getId())) {
                collectDictionarySubtree(dictionary, childrenByParentId, deleteIds);
            }
        }

        // 变量说明：changedDictionaries 保存当前步骤计算、查询或转换得到的中间结果。
        List<BasicDictionary> changedDictionaries = new java.util.ArrayList<>();
        for (BasicDictionary dictionary : dictionaries) {
            // 变量说明：code 保存当前步骤计算、查询或转换得到的中间结果。
            String code = dictionary.getCode();
            if (dictionary.getId() != null && !deleteIds.contains(dictionary.getId())
                    && code != null && code.startsWith("AREA_")) {
                dictionary.setCode(code.substring("AREA_".length()));
                changedDictionaries.add(dictionary);
            }
        }
        dictionaryRepository.saveAll(changedDictionaries);

        List<BasicDictionary> deleteDictionaries = dictionaries.stream()
                .filter(dictionary -> dictionary.getId() != null && deleteIds.contains(dictionary.getId()))
                .sorted((left, right) -> Integer.compare(
                        dictionaryDepth(left, childrenByParentId),
                        dictionaryDepth(right, childrenByParentId)
                ))
                .toList();
        dictionaryRepository.deleteAll(deleteDictionaries);
    }

    /**
     * 收集指定字典及其全部下级 ID。
     */
    private void collectDictionarySubtree(
            BasicDictionary dictionary,
            java.util.Map<Long, List<BasicDictionary>> childrenByParentId,
            Set<Long> deleteIds
    ) {
        if (dictionary.getId() == null || !deleteIds.add(dictionary.getId())) {
            return;
        }
        for (BasicDictionary child : childrenByParentId.getOrDefault(dictionary.getId(), List.of())) {
            collectDictionarySubtree(child, childrenByParentId, deleteIds);
        }
    }

    /**
     * 计算字典下级深度，删除重复行政区划节点时保证先删叶子节点。
     */
    private int dictionaryDepth(BasicDictionary dictionary, java.util.Map<Long, List<BasicDictionary>> childrenByParentId) {
        if (dictionary.getId() == null) {
            return 0;
        }
        // 变量说明：depth 保存当前步骤计算、查询或转换得到的中间结果。
        int depth = 0;
        for (BasicDictionary child : childrenByParentId.getOrDefault(dictionary.getId(), List.of())) {
            depth = Math.max(depth, 1 + dictionaryDepth(child, childrenByParentId));
        }
        return depth;
    }

    /**
     * 解析简单 CSV 行。
     */
    private String[] parseCsvLine(String line) {
        return line.replace("\"", "").split(",", -1);
    }

    /**
     * 安全解析整数。
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * 执行 seedMenu 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private SystemMenu seedMenu(
            SystemMenuRepository menuRepository,
            String code,
            String name,
            MenuType type,
            SystemMenu parent,
            String routePath,
            int sortOrder,
            PermissionCode permissionCode
    ) {
        // 变量说明：menu 保存当前步骤计算、查询或转换得到的中间结果。
        SystemMenu menu = menuRepository.findByCode(code).orElseGet(SystemMenu::new);
        menu.setCode(code);
        menu.setName(name);
        menu.setType(type);
        menu.setParent(parent);
        menu.setRoutePath(routePath);
        menu.setSortOrder(sortOrder);
        menu.setPermissionCode(permissionCode);
        menu.setEnabled(true);
        return menuRepository.save(menu);
    }

    /**
     * 停用不再暴露的历史菜单。
     *
     * <p>实现步骤：如果历史数据库中存在该菜单编码，则仅改为停用，不物理删除，避免破坏历史角色关联数据。</p>
     */
    private void disableMenuIfExists(SystemMenuRepository menuRepository, String code) {
        menuRepository.findByCode(code).ifPresent(menu -> {
            menu.setEnabled(false);
            menuRepository.save(menu);
        });
    }

    /**
     * 执行 resolveMenus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Set<SystemMenu> resolveMenus(SystemMenuRepository menuRepository, Set<String> menuCodes) {
        // 变量说明：menus 保存当前步骤计算、查询或转换得到的中间结果。
        Set<SystemMenu> menus = new HashSet<>();
        for (String menuCode : menuCodes) {
            menuRepository.findByCode(menuCode).ifPresent(menus::add);
        }
        return menus;
    }

    /**
     * 执行 permissionsFromMenus 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private Set<PermissionCode> permissionsFromMenus(Set<SystemMenu> menus) {
        // 变量说明：permissions 保存当前步骤计算、查询或转换得到的中间结果。
        Set<PermissionCode> permissions = new HashSet<>();
        for (SystemMenu menu : menus) {
            if (menu.getPermissionCode() != null) {
                permissions.add(menu.getPermissionCode());
            }
        }
        return permissions;
    }
}
