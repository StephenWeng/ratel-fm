package com.ratel.fm.config.database;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * PostgreSQL 数据库注释初始化器。
 *
 * <p>实体上的 Hibernate {@code @Comment} 主要服务于新建或更新 DDL；该初始化器负责在应用启动后，
 * 对已经存在的表和字段补写数据库 comment，确保数据库工具中也能直接看到表说明和字段说明。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
@ConditionalOnProperty(prefix = "app.database.postgresql-initializer", name = "enabled", havingValue = "true")
public class DatabaseCommentInitializer {

    /**
     * 应用启动后补齐数据库表和字段注释。
     *
     * <p>实现步骤：
     * 1. 等待 Hibernate 完成 ddl-auto 更新表结构；
     * 2. 遍历系统内维护的表注释清单，表存在时执行 comment on table；
     * 3. 遍历系统内维护的字段注释清单，字段存在时执行 comment on column；
     * 4. 对不存在的表或字段直接跳过，避免历史库结构不完整导致应用启动失败。</p>
     */
    @Bean
    @Order(2)
    public CommandLineRunner applyDatabaseComments(JdbcTemplate jdbcTemplate) {
        return args -> {
            // 步骤一：补齐所有业务表、关联表的表级描述。
            for (TableComment tableComment : tableComments()) {
                applyTableCommentIfExists(jdbcTemplate, tableComment);
            }

            // 步骤二：补齐所有业务表、关联表的字段级描述。
            for (ColumnComment columnComment : columnComments()) {
                applyColumnCommentIfExists(jdbcTemplate, columnComment);
            }
        };
    }

    /**
     * 系统内所有需要维护描述的数据库表。
     *
     * <p>这里把 JPA 实体表和 JPA 自动生成的关系表放在同一清单中，便于后续新增模块时统一维护。</p>
     */
    private List<TableComment> tableComments() {
        return List.of(
                new TableComment("fm_users", "系统人员账号表，保存登录账号、人员基础信息和角色授权关系"),
                new TableComment("fm_roles", "系统角色表，保存角色编码、名称和模块权限集合"),
                new TableComment("fm_menus", "系统菜单资源表，保存模块、页面、按钮等可授权资源"),
                new TableComment("fm_user_roles", "人员角色关联表，维护人员与角色的多对多关系"),
                new TableComment("fm_role_permissions", "角色权限关联表，维护角色拥有的模块权限码"),
                new TableComment("fm_role_menus", "角色菜单关联表，维护角色拥有的模块、页面和按钮资源"),
                new TableComment("fm_user_login_sessions", "用户登录会话表，支持同一身份证同一终端类型唯一在线登录"),
                new TableComment("fm_user_operation_logs", "用户关键操作日志表，记录业务关键操作的人员、终端、模块、功能、参数、结果和影响"),
                new TableComment("fm_basic_dictionaries", "基础信息字典表，维护采购方、物流方等层级基础资料"),
                new TableComment("fm_accounting_subjects", "会计科目字典表，作为凭证分录、统计分析和财务报表的基础维度"),
                new TableComment("fm_vouchers", "财务凭证主表，记录复式记账业务的凭证头、状态和借贷合计"),
                new TableComment("fm_voucher_lines", "财务凭证明细表，记录每条会计科目的借方或贷方发生额"),
                new TableComment("fm_purchase_orders", "采购订单主表，为应付、库存入库和财务凭证提供业务来源"),
                new TableComment("fm_purchase_order_lines", "采购订单明细表，记录物料采购数量、单价和金额"),
                new TableComment("fm_shipment_orders", "物流运输单表，跟踪采购或其他业务单据的运输履约状态"),
                new TableComment("fm_shipment_operation_logs", "物流管理操作流水表，记录每次状态确认后的物流信息快照"),
                new TableComment("fm_inventory_ledgers", "库存台账流水表，记录入库、出库、调拨和盘点等库存数量变化"),
                new TableComment("fm_ar_ap_bills", "应收应付单表，记录客户应收或供应商应付并支持账龄和付款计划分析"),
                new TableComment("fm_business_operation_logs", "统一业务操作流水表，记录凭证、采购、库存、应收应付等业务记录的时间轴"),
                new TableComment("fm_attachments", "统一附件文件表，保存附件名称、后缀、大小、类型和磁盘存储路径"),
                new TableComment("fm_business_attachments", "业务附件关联表，记录业务类型、业务记录ID和附件ID的多附件关系"),
                new TableComment("fm_local_knowledge_documents", "本地知识库上传资料表，保存用户上传资料的元数据、入库状态和失败原因")
        );
    }

    /**
     * 系统内所有需要维护描述的数据库字段。
     *
     * <p>字段名使用 Spring Boot 默认物理命名策略生成的 snake_case 名称，和 PostgreSQL 实际字段保持一致。</p>
     */
    private List<ColumnComment> columnComments() {
        return List.of(
                new ColumnComment("fm_users", "id", "主键ID"),
                new ColumnComment("fm_users", "created_time", "记录创建时间"),
                new ColumnComment("fm_users", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_users", "username", "登录账号，同一所属公司内唯一，可用于账号密码登录"),
                new ColumnComment("fm_users", "real_name", "人员真实姓名"),
                new ColumnComment("fm_users", "password_hash", "BCrypt加密后的登录密码"),
                new ColumnComment("fm_users", "department", "人员所属部门"),
                new ColumnComment("fm_users", "organization_code", "人员所属公司字典编码，作为账套隔离标识"),
                new ColumnComment("fm_users", "position", "人员岗位名称"),
                new ColumnComment("fm_users", "identity_no", "身份证号，同一所属公司内唯一，是登录和人员管理唯一性校验字段"),
                new ColumnComment("fm_users", "phone", "人员联系电话"),
                new ColumnComment("fm_users", "email", "人员邮箱地址"),
                new ColumnComment("fm_users", "avatar_url", "兼容历史头像照片访问地址"),
                new ColumnComment("fm_users", "avatar_base64", "头像图片Base64数据"),
                new ColumnComment("fm_users", "enabled", "是否启用账号"),

                new ColumnComment("fm_roles", "id", "主键ID"),
                new ColumnComment("fm_roles", "created_time", "记录创建时间"),
                new ColumnComment("fm_roles", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_roles", "code", "角色编码，全系统唯一"),
                new ColumnComment("fm_roles", "name", "角色名称"),
                new ColumnComment("fm_roles", "description", "角色说明"),

                new ColumnComment("fm_menus", "id", "主键ID"),
                new ColumnComment("fm_menus", "created_time", "记录创建时间"),
                new ColumnComment("fm_menus", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_menus", "code", "菜单编码，全系统唯一"),
                new ColumnComment("fm_menus", "name", "菜单名称"),
                new ColumnComment("fm_menus", "type", "菜单类型：模块、页面或按钮"),
                new ColumnComment("fm_menus", "parent_id", "父级菜单ID"),
                new ColumnComment("fm_menus", "route_path", "前端路由路径"),
                new ColumnComment("fm_menus", "sort_order", "菜单排序号"),
                new ColumnComment("fm_menus", "enabled", "是否启用菜单"),
                new ColumnComment("fm_menus", "permission_code", "绑定的后端权限码"),

                new ColumnComment("fm_user_roles", "user_id", "人员主键"),
                new ColumnComment("fm_user_roles", "role_id", "角色主键"),
                new ColumnComment("fm_role_permissions", "role_id", "角色主键"),
                new ColumnComment("fm_role_permissions", "permission_code", "角色拥有的权限码"),
                new ColumnComment("fm_role_menus", "role_id", "角色主键"),
                new ColumnComment("fm_role_menus", "menu_id", "菜单资源主键"),

                new ColumnComment("fm_user_login_sessions", "id", "主键ID"),
                new ColumnComment("fm_user_login_sessions", "created_time", "记录创建时间"),
                new ColumnComment("fm_user_login_sessions", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_user_login_sessions", "session_id", "登录会话唯一ID"),
                new ColumnComment("fm_user_login_sessions", "user_id", "登录人员主键"),
                new ColumnComment("fm_user_login_sessions", "username", "登录用户名"),
                new ColumnComment("fm_user_login_sessions", "organization_code", "登录所属公司字典编码，用于会话和唯一在线隔离"),
                new ColumnComment("fm_user_login_sessions", "real_name", "登录人员姓名"),
                new ColumnComment("fm_user_login_sessions", "identity_no", "登录人员身份证号"),
                new ColumnComment("fm_user_login_sessions", "terminal_type", "终端类型：PC或APP"),
                new ColumnComment("fm_user_login_sessions", "terminal_identifier", "终端唯一标识，PC为IP，APP为手机号"),
                new ColumnComment("fm_user_login_sessions", "status", "会话状态"),
                new ColumnComment("fm_user_login_sessions", "login_time", "登录成功时间"),
                new ColumnComment("fm_user_login_sessions", "expires_at", "会话过期时间"),
                new ColumnComment("fm_user_login_sessions", "logout_time", "会话结束时间"),

                new ColumnComment("fm_user_operation_logs", "id", "主键ID"),
                new ColumnComment("fm_user_operation_logs", "created_time", "记录创建时间"),
                new ColumnComment("fm_user_operation_logs", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_user_operation_logs", "operator_id", "操作人员主键"),
                new ColumnComment("fm_user_operation_logs", "operator_username", "操作人员登录账号"),
                new ColumnComment("fm_user_operation_logs", "operator_name", "操作人员姓名"),
                new ColumnComment("fm_user_operation_logs", "identity_no", "操作人员身份证号"),
                new ColumnComment("fm_user_operation_logs", "organization_code", "操作发生时所属公司字典编码，用于操作日志账套隔离"),
                new ColumnComment("fm_user_operation_logs", "department", "操作人员部门"),
                new ColumnComment("fm_user_operation_logs", "contact_phone", "操作人员联系方式"),
                new ColumnComment("fm_user_operation_logs", "terminal_type", "操作终端类型"),
                new ColumnComment("fm_user_operation_logs", "terminal_identifier", "操作终端标识"),
                new ColumnComment("fm_user_operation_logs", "operation_module", "操作模块"),
                new ColumnComment("fm_user_operation_logs", "operation_function", "操作功能"),
                new ColumnComment("fm_user_operation_logs", "operation_time", "业务操作发生时间"),
                new ColumnComment("fm_user_operation_logs", "action", "操作动作编码"),
                new ColumnComment("fm_user_operation_logs", "operation_parameters", "操作请求参数或关键业务参数"),
                new ColumnComment("fm_user_operation_logs", "success", "操作是否成功"),
                new ColumnComment("fm_user_operation_logs", "operation_result", "操作结果"),
                new ColumnComment("fm_user_operation_logs", "response_value", "操作响应值"),
                new ColumnComment("fm_user_operation_logs", "impact", "操作造成的业务影响说明"),

                new ColumnComment("fm_basic_dictionaries", "id", "主键ID"),
                new ColumnComment("fm_basic_dictionaries", "created_time", "记录创建时间"),
                new ColumnComment("fm_basic_dictionaries", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_basic_dictionaries", "code", "字典编码，全系统唯一；未填写时由服务端随机生成"),
                new ColumnComment("fm_basic_dictionaries", "name", "字典名称，同一父级下唯一"),
                new ColumnComment("fm_basic_dictionaries", "parent_id", "父级字典ID"),
                new ColumnComment("fm_basic_dictionaries", "sort_order", "排序号，越小越靠前"),
                new ColumnComment("fm_basic_dictionaries", "enabled", "是否启用"),
                new ColumnComment("fm_basic_dictionaries", "description", "字典说明"),

                new ColumnComment("fm_accounting_subjects", "id", "主键ID"),
                new ColumnComment("fm_accounting_subjects", "created_time", "记录创建时间"),
                new ColumnComment("fm_accounting_subjects", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_accounting_subjects", "organization_code", "所属公司字典编码，作为会计科目账套隔离标识"),
                new ColumnComment("fm_accounting_subjects", "code", "科目编码，同一所属公司内唯一"),
                new ColumnComment("fm_accounting_subjects", "name", "科目名称"),
                new ColumnComment("fm_accounting_subjects", "category", "科目类别"),
                new ColumnComment("fm_accounting_subjects", "parent_id", "父级科目ID"),
                new ColumnComment("fm_accounting_subjects", "subject_level", "科目层级"),
                new ColumnComment("fm_accounting_subjects", "enabled", "是否启用科目"),
                new ColumnComment("fm_accounting_subjects", "description", "科目说明"),

                new ColumnComment("fm_vouchers", "id", "主键ID"),
                new ColumnComment("fm_vouchers", "created_time", "记录创建时间"),
                new ColumnComment("fm_vouchers", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_vouchers", "organization_code", "所属公司字典编码，作为凭证账套隔离标识"),
                new ColumnComment("fm_vouchers", "voucher_no", "凭证编号，同一所属公司内唯一"),
                new ColumnComment("fm_vouchers", "voucher_date", "凭证日期"),
                new ColumnComment("fm_vouchers", "belong_month", "所属年月，格式 yyyy-MM，用于标记账目发生月份"),
                new ColumnComment("fm_vouchers", "project_code", "项目字典编码，来自基础信息项目字典"),
                new ColumnComment("fm_vouchers", "project_name", "项目名称快照"),
                new ColumnComment("fm_vouchers", "summary", "凭证摘要"),
                new ColumnComment("fm_vouchers", "status", "凭证状态"),
                new ColumnComment("fm_vouchers", "total_debit", "借方合计金额，保留8位小数"),
                new ColumnComment("fm_vouchers", "total_credit", "贷方合计金额，保留8位小数"),
                new ColumnComment("fm_vouchers", "currency_code", "币种编码，来自基础信息币种字典"),
                new ColumnComment("fm_vouchers", "currency_name", "币种名称快照"),
                new ColumnComment("fm_vouchers", "exchange_rate_to_cny", "业务发生时该币种折人民币汇率"),
                new ColumnComment("fm_vouchers", "total_debit_cny", "借方合计折人民币金额，保留8位小数"),
                new ColumnComment("fm_vouchers", "total_credit_cny", "贷方合计折人民币金额，保留8位小数"),
                new ColumnComment("fm_vouchers", "created_by", "创建凭证的登录账号"),
                new ColumnComment("fm_vouchers", "posted_by", "过账人员账号"),
                new ColumnComment("fm_vouchers", "source_biz_no", "来源业务单号"),
                new ColumnComment("fm_vouchers", "source_type", "来源业务类型"),
                new ColumnComment("fm_vouchers", "source_id", "来源业务主键"),
                new ColumnComment("fm_vouchers", "source_title", "来源业务标题快照"),

                new ColumnComment("fm_voucher_lines", "id", "主键ID"),
                new ColumnComment("fm_voucher_lines", "created_time", "记录创建时间"),
                new ColumnComment("fm_voucher_lines", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_voucher_lines", "voucher_id", "所属凭证主表ID"),
                new ColumnComment("fm_voucher_lines", "line_no", "分录行号"),
                new ColumnComment("fm_voucher_lines", "subject_id", "记账科目ID"),
                new ColumnComment("fm_voucher_lines", "summary", "分录摘要"),
                new ColumnComment("fm_voucher_lines", "debit_amount", "借方金额，保留8位小数"),
                new ColumnComment("fm_voucher_lines", "credit_amount", "贷方金额，保留8位小数"),
                new ColumnComment("fm_voucher_lines", "currency_code", "分录币种编码，来自基础信息币种字典"),
                new ColumnComment("fm_voucher_lines", "currency_name", "分录币种名称快照"),
                new ColumnComment("fm_voucher_lines", "exchange_rate_to_cny", "分录业务发生时该币种折人民币汇率"),
                new ColumnComment("fm_voucher_lines", "debit_amount_cny", "借方折人民币金额，保留8位小数"),
                new ColumnComment("fm_voucher_lines", "credit_amount_cny", "贷方折人民币金额，保留8位小数"),
                new ColumnComment("fm_voucher_lines", "auxiliary", "辅助核算信息"),

                new ColumnComment("fm_purchase_orders", "id", "主键ID"),
                new ColumnComment("fm_purchase_orders", "created_time", "记录创建时间"),
                new ColumnComment("fm_purchase_orders", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_purchase_orders", "organization_code", "所属公司字典编码，作为采购单账套隔离标识"),
                new ColumnComment("fm_purchase_orders", "order_no", "采购单号，同一所属公司内唯一"),
                new ColumnComment("fm_purchase_orders", "supplier_name", "供应商名称"),
                new ColumnComment("fm_purchase_orders", "project_code", "项目字典编码，来自基础信息项目字典"),
                new ColumnComment("fm_purchase_orders", "project_name", "项目名称快照"),
                new ColumnComment("fm_purchase_orders", "order_date", "采购日期"),
                new ColumnComment("fm_purchase_orders", "status", "采购状态"),
                new ColumnComment("fm_purchase_orders", "total_amount", "采购总金额，保留8位小数"),
                new ColumnComment("fm_purchase_orders", "currency_code", "币种编码，来自基础信息币种字典"),
                new ColumnComment("fm_purchase_orders", "currency_name", "币种名称快照"),
                new ColumnComment("fm_purchase_orders", "exchange_rate_to_cny", "采购业务发生时该币种折人民币汇率"),
                new ColumnComment("fm_purchase_orders", "total_amount_cny", "采购总金额折人民币金额，保留8位小数"),
                new ColumnComment("fm_purchase_orders", "created_by", "创建采购单的登录账号"),
                new ColumnComment("fm_purchase_orders", "remark", "采购备注"),
                new ColumnComment("fm_purchase_orders", "voucher_id", "关联凭证主键"),
                new ColumnComment("fm_purchase_orders", "voucher_no", "关联凭证号"),

                new ColumnComment("fm_purchase_order_lines", "id", "主键ID"),
                new ColumnComment("fm_purchase_order_lines", "created_time", "记录创建时间"),
                new ColumnComment("fm_purchase_order_lines", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_purchase_order_lines", "purchase_order_id", "所属采购订单主表ID"),
                new ColumnComment("fm_purchase_order_lines", "line_no", "明细行号"),
                new ColumnComment("fm_purchase_order_lines", "item_code", "物料编码"),
                new ColumnComment("fm_purchase_order_lines", "item_name", "物料名称"),
                new ColumnComment("fm_purchase_order_lines", "quantity", "采购数量"),
                new ColumnComment("fm_purchase_order_lines", "unit_price", "采购单价，保留8位小数"),
                new ColumnComment("fm_purchase_order_lines", "amount", "采购明细金额，保留8位小数"),
                new ColumnComment("fm_purchase_order_lines", "currency_code", "采购明细币种编码，来自基础信息币种字典"),
                new ColumnComment("fm_purchase_order_lines", "currency_name", "采购明细币种名称快照"),
                new ColumnComment("fm_purchase_order_lines", "exchange_rate_to_cny", "采购明细发生时该币种折人民币汇率"),
                new ColumnComment("fm_purchase_order_lines", "unit_price_cny", "采购单价折人民币金额，保留8位小数"),
                new ColumnComment("fm_purchase_order_lines", "amount_cny", "采购明细折人民币金额，保留8位小数"),

                new ColumnComment("fm_shipment_orders", "id", "主键ID"),
                new ColumnComment("fm_shipment_orders", "created_time", "记录创建时间"),
                new ColumnComment("fm_shipment_orders", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_shipment_orders", "organization_code", "所属公司字典编码，作为物流单账套隔离标识"),
                new ColumnComment("fm_shipment_orders", "shipment_no", "物流单号，同一所属公司内唯一"),
                new ColumnComment("fm_shipment_orders", "related_order_no", "关联业务单号"),
                new ColumnComment("fm_shipment_orders", "project_code", "项目字典编码，来自基础信息项目字典"),
                new ColumnComment("fm_shipment_orders", "project_name", "项目名称快照"),
                new ColumnComment("fm_shipment_orders", "carrier_name", "承运商名称"),
                new ColumnComment("fm_shipment_orders", "tracking_no", "承运商运单号或跟踪号"),
                new ColumnComment("fm_shipment_orders", "origin_division_code", "发货地行政区划编码级联路径，来自全国行政区划字典"),
                new ColumnComment("fm_shipment_orders", "origin_division_name", "发货地行政区划名称级联快照"),
                new ColumnComment("fm_shipment_orders", "destination_division_code", "目的地行政区划编码级联路径，来自全国行政区划字典"),
                new ColumnComment("fm_shipment_orders", "destination_division_name", "目的地行政区划名称级联快照"),
                new ColumnComment("fm_shipment_orders", "origin", "发货地详细地址"),
                new ColumnComment("fm_shipment_orders", "destination", "目的地详细地址"),
                new ColumnComment("fm_shipment_orders", "planned_ship_date", "计划发运日期"),
                new ColumnComment("fm_shipment_orders", "actual_ship_date", "实际发运日期"),
                new ColumnComment("fm_shipment_orders", "delivered_date", "实际送达日期"),
                new ColumnComment("fm_shipment_orders", "status", "物流状态"),
                new ColumnComment("fm_shipment_orders", "remark", "物流备注"),

                new ColumnComment("fm_shipment_operation_logs", "id", "主键ID"),
                new ColumnComment("fm_shipment_operation_logs", "created_time", "记录创建时间"),
                new ColumnComment("fm_shipment_operation_logs", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_shipment_operation_logs", "shipment_order_id", "所属物流单ID"),
                new ColumnComment("fm_shipment_operation_logs", "shipment_no", "物流单号快照"),
                new ColumnComment("fm_shipment_operation_logs", "from_status", "原物流状态"),
                new ColumnComment("fm_shipment_operation_logs", "to_status", "目标物流状态"),
                new ColumnComment("fm_shipment_operation_logs", "related_order_no", "关联业务单号快照"),
                new ColumnComment("fm_shipment_operation_logs", "document_type", "物流单据类型快照"),
                new ColumnComment("fm_shipment_operation_logs", "project_code", "项目字典编码快照"),
                new ColumnComment("fm_shipment_operation_logs", "project_name", "项目名称快照"),
                new ColumnComment("fm_shipment_operation_logs", "transport_mode", "运输方式快照"),
                new ColumnComment("fm_shipment_operation_logs", "shipping_organization", "发运组织快照"),
                new ColumnComment("fm_shipment_operation_logs", "receiving_organization", "收货组织快照"),
                new ColumnComment("fm_shipment_operation_logs", "carrier_name", "承运商名称快照"),
                new ColumnComment("fm_shipment_operation_logs", "tracking_no", "承运商运单号或跟踪号快照"),
                new ColumnComment("fm_shipment_operation_logs", "driver_name", "司机姓名快照"),
                new ColumnComment("fm_shipment_operation_logs", "driver_phone", "司机电话快照"),
                new ColumnComment("fm_shipment_operation_logs", "vehicle_no", "车牌号快照"),
                new ColumnComment("fm_shipment_operation_logs", "origin_division_code", "发货地行政区划编码级联路径快照"),
                new ColumnComment("fm_shipment_operation_logs", "origin_division_name", "发货地行政区划名称级联快照"),
                new ColumnComment("fm_shipment_operation_logs", "destination_division_code", "目的地行政区划编码级联路径快照"),
                new ColumnComment("fm_shipment_operation_logs", "destination_division_name", "目的地行政区划名称级联快照"),
                new ColumnComment("fm_shipment_operation_logs", "origin", "发货地详细地址快照"),
                new ColumnComment("fm_shipment_operation_logs", "destination", "目的地详细地址快照"),
                new ColumnComment("fm_shipment_operation_logs", "planned_ship_date", "计划发运日期快照"),
                new ColumnComment("fm_shipment_operation_logs", "actual_ship_date", "实际发运日期快照"),
                new ColumnComment("fm_shipment_operation_logs", "delivered_date", "实际送达日期快照"),
                new ColumnComment("fm_shipment_operation_logs", "remark", "物流备注快照"),
                new ColumnComment("fm_shipment_operation_logs", "operation_remark", "本次状态确认说明"),
                new ColumnComment("fm_shipment_operation_logs", "operator_id", "操作人员主键"),
                new ColumnComment("fm_shipment_operation_logs", "operator_username", "操作人员账号"),
                new ColumnComment("fm_shipment_operation_logs", "operator_name", "操作人员姓名"),
                new ColumnComment("fm_shipment_operation_logs", "operation_time", "操作发生时间"),

                new ColumnComment("fm_inventory_ledgers", "id", "主键ID"),
                new ColumnComment("fm_inventory_ledgers", "created_time", "记录创建时间"),
                new ColumnComment("fm_inventory_ledgers", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_inventory_ledgers", "movement_no", "库存流水号，同一所属公司内唯一"),
                new ColumnComment("fm_inventory_ledgers", "movement_type", "库存变动类型"),
                new ColumnComment("fm_inventory_ledgers", "movement_date", "库存变动日期"),
                new ColumnComment("fm_inventory_ledgers", "item_code", "物料编码"),
                new ColumnComment("fm_inventory_ledgers", "item_name", "物料名称"),
                new ColumnComment("fm_inventory_ledgers", "project_code", "项目字典编码，来自基础信息项目字典"),
                new ColumnComment("fm_inventory_ledgers", "project_name", "项目名称快照"),
                new ColumnComment("fm_inventory_ledgers", "quantity", "库存变动数量"),
                new ColumnComment("fm_inventory_ledgers", "from_warehouse", "来源仓库"),
                new ColumnComment("fm_inventory_ledgers", "to_warehouse", "目标仓库"),
                new ColumnComment("fm_inventory_ledgers", "related_biz_no", "关联业务单号"),
                new ColumnComment("fm_inventory_ledgers", "remark", "库存流水备注"),
                new ColumnComment("fm_inventory_ledgers", "organization_code", "所属公司字典编码，作为库存台账账套隔离标识"),
                new ColumnComment("fm_inventory_ledgers", "voucher_id", "关联凭证主键"),
                new ColumnComment("fm_inventory_ledgers", "voucher_no", "关联凭证号"),

                new ColumnComment("fm_ar_ap_bills", "id", "主键ID"),
                new ColumnComment("fm_ar_ap_bills", "created_time", "记录创建时间"),
                new ColumnComment("fm_ar_ap_bills", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_ar_ap_bills", "bill_no", "应收应付单据编号，同一所属公司内唯一"),
                new ColumnComment("fm_ar_ap_bills", "bill_type", "单据类型：应收或应付"),
                new ColumnComment("fm_ar_ap_bills", "partner_name", "往来单位名称"),
                new ColumnComment("fm_ar_ap_bills", "project_code", "项目字典编码，来自基础信息项目字典"),
                new ColumnComment("fm_ar_ap_bills", "project_name", "项目名称快照"),
                new ColumnComment("fm_ar_ap_bills", "bill_date", "单据发生日期"),
                new ColumnComment("fm_ar_ap_bills", "due_date", "到期日期"),
                new ColumnComment("fm_ar_ap_bills", "amount", "应收或应付总金额，保留8位小数"),
                new ColumnComment("fm_ar_ap_bills", "paid_amount", "已收或已付金额，保留8位小数"),
                new ColumnComment("fm_ar_ap_bills", "currency_code", "币种编码，来自基础信息币种字典"),
                new ColumnComment("fm_ar_ap_bills", "currency_name", "币种名称快照"),
                new ColumnComment("fm_ar_ap_bills", "exchange_rate_to_cny", "应收应付发生时该币种折人民币汇率"),
                new ColumnComment("fm_ar_ap_bills", "amount_cny", "应收或应付总金额折人民币金额，保留8位小数"),
                new ColumnComment("fm_ar_ap_bills", "paid_amount_cny", "已收或已付金额折人民币金额，保留8位小数"),
                new ColumnComment("fm_ar_ap_bills", "status", "应收应付单据状态"),
                new ColumnComment("fm_ar_ap_bills", "payment_plan", "付款或收款计划说明"),
                new ColumnComment("fm_ar_ap_bills", "organization_code", "所属公司字典编码，作为应收应付账套隔离标识"),
                new ColumnComment("fm_ar_ap_bills", "voucher_id", "关联凭证主键"),
                new ColumnComment("fm_ar_ap_bills", "voucher_no", "关联凭证号"),

                new ColumnComment("fm_business_operation_logs", "id", "主键ID"),
                new ColumnComment("fm_business_operation_logs", "created_time", "记录创建时间"),
                new ColumnComment("fm_business_operation_logs", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_business_operation_logs", "organization_code", "所属公司字典编码，作为业务流水账套隔离标识"),
                new ColumnComment("fm_business_operation_logs", "business_type", "业务类型"),
                new ColumnComment("fm_business_operation_logs", "business_id", "业务记录主键ID"),
                new ColumnComment("fm_business_operation_logs", "business_no", "业务编号快照"),
                new ColumnComment("fm_business_operation_logs", "business_title", "业务标题快照"),
                new ColumnComment("fm_business_operation_logs", "action", "操作动作"),
                new ColumnComment("fm_business_operation_logs", "action_name", "操作动作中文名称"),
                new ColumnComment("fm_business_operation_logs", "detail", "操作详情"),
                new ColumnComment("fm_business_operation_logs", "from_state", "操作前状态或关键值快照"),
                new ColumnComment("fm_business_operation_logs", "to_state", "操作后状态或关键值快照"),
                new ColumnComment("fm_business_operation_logs", "snapshot", "操作参数或业务快照"),
                new ColumnComment("fm_business_operation_logs", "operator_id", "操作人员主键"),
                new ColumnComment("fm_business_operation_logs", "operator_username", "操作人员账号"),
                new ColumnComment("fm_business_operation_logs", "operator_name", "操作人员姓名"),
                new ColumnComment("fm_business_operation_logs", "operation_time", "操作发生时间"),

                new ColumnComment("fm_attachments", "id", "主键ID"),
                new ColumnComment("fm_attachments", "created_time", "记录创建时间"),
                new ColumnComment("fm_attachments", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_attachments", "original_name", "上传时的原始文件名"),
                new ColumnComment("fm_attachments", "display_name", "附件展示名称，支持后续改名"),
                new ColumnComment("fm_attachments", "suffix", "文件后缀，不包含英文句点"),
                new ColumnComment("fm_attachments", "file_size", "文件大小，单位字节"),
                new ColumnComment("fm_attachments", "content_type", "文件内容类型"),
                new ColumnComment("fm_attachments", "storage_path", "相对 files 目录的磁盘存储路径"),
                new ColumnComment("fm_attachments", "uploader_id", "上传人员主键"),
                new ColumnComment("fm_attachments", "uploader_username", "上传人员账号快照"),

                new ColumnComment("fm_local_knowledge_documents", "id", "主键ID"),
                new ColumnComment("fm_local_knowledge_documents", "created_time", "记录创建时间"),
                new ColumnComment("fm_local_knowledge_documents", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_local_knowledge_documents", "title", "资料标题，未填写时取原始文件名"),
                new ColumnComment("fm_local_knowledge_documents", "description", "资料说明，用于辅助检索和列表展示"),
                new ColumnComment("fm_local_knowledge_documents", "original_name", "上传时的原始文件名"),
                new ColumnComment("fm_local_knowledge_documents", "storage_path", "相对 files 目录的文件保存路径"),
                new ColumnComment("fm_local_knowledge_documents", "suffix", "文件后缀，不包含英文句点"),
                new ColumnComment("fm_local_knowledge_documents", "content_type", "文件 MIME 类型"),
                new ColumnComment("fm_local_knowledge_documents", "file_size", "文件大小，单位字节"),
                new ColumnComment("fm_local_knowledge_documents", "status", "入库状态：PENDING、INDEXING、INDEXED、FAILED"),
                new ColumnComment("fm_local_knowledge_documents", "chunk_count", "写入知识索引或向量库的分片数量"),
                new ColumnComment("fm_local_knowledge_documents", "ocr_used", "是否使用图片 OCR 或本地视觉模型识别"),
                new ColumnComment("fm_local_knowledge_documents", "organization_code", "所属公司字典编码，作为本地知识库账套隔离标识"),
                new ColumnComment("fm_local_knowledge_documents", "uploaded_by", "上传人用户名快照"),
                new ColumnComment("fm_local_knowledge_documents", "error_message", "入库失败时的用户可读原因"),

                new ColumnComment("fm_business_attachments", "id", "主键ID"),
                new ColumnComment("fm_business_attachments", "created_time", "记录创建时间"),
                new ColumnComment("fm_business_attachments", "modify_time", "记录最近更新时间"),
                new ColumnComment("fm_business_attachments", "business_type", "业务模块类型"),
                new ColumnComment("fm_business_attachments", "business_id", "业务记录主键ID"),
                new ColumnComment("fm_business_attachments", "attachment_id", "附件文件主键ID"),
                new ColumnComment("fm_business_attachments", "sort_order", "附件显示顺序")
        );
    }

    /**
     * 表存在时写入表注释。
     *
     * <p>实现步骤：先通过 information_schema 判断当前 schema 下是否存在该表，再执行 PostgreSQL comment 语句。</p>
     */
    private void applyTableCommentIfExists(JdbcTemplate jdbcTemplate, TableComment tableComment) {
        if (!tableExists(jdbcTemplate, tableComment.tableName())) {
            return;
        }
        jdbcTemplate.execute("comment on table " + identifier(tableComment.tableName()) + " is " + literal(tableComment.comment()));
    }

    /**
     * 字段存在时写入字段注释。
     *
     * <p>实现步骤：先判断当前 schema 下的表字段是否存在，再执行 PostgreSQL comment on column 语句。</p>
     */
    private void applyColumnCommentIfExists(JdbcTemplate jdbcTemplate, ColumnComment columnComment) {
        if (!columnExists(jdbcTemplate, columnComment.tableName(), columnComment.columnName())) {
            return;
        }
        jdbcTemplate.execute("comment on column "
                + identifier(columnComment.tableName())
                + "."
                + identifier(columnComment.columnName())
                + " is "
                + literal(columnComment.comment()));
    }

    /**
     * 判断当前 schema 下是否存在指定表。
     */
    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * 判断当前 schema 下是否存在指定字段。
     */
    private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    /**
     * 校验并返回数据库对象名。
     *
     * <p>对象名全部来自代码内置清单，仍保留白名单校验，避免后续维护时把非法字符拼进 DDL。</p>
     */
    private String identifier(String value) {
        if (!value.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Illegal database identifier: " + value);
        }
        if (value.matches("[a-z0-9_]+")) {
            return value;
        }
        return "\"" + value + "\"";
    }

    /**
     * 把 Java 字符串转换为 PostgreSQL 字面量。
     *
     * <p>实现步骤：使用单引号包裹文本，并把文本内部单引号转义为两个单引号。</p>
     */
    private String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * 表注释清单项。
     */
    private record TableComment(String tableName, String comment) {
    }

    /**
     * 字段注释清单项。
     */
    private record ColumnComment(String tableName, String columnName, String comment) {
    }
}
