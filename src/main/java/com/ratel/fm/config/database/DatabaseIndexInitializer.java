package com.ratel.fm.config.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * PostgreSQL 数据库索引初始化器。
 *
 * <p>该初始化器集中维护业务查询、关联查询和排序查询需要的普通索引。唯一索引由表约束维护，
 * 这里补齐的是外键、状态、日期、层级、审计等高频查询路径。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
@ConditionalOnProperty(prefix = "app.database.initializer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseIndexInitializer {

    /** 数据库索引初始化日志对象，用于记录兼容清理中可忽略的历史索引差异。 */
    private static final Logger log = LoggerFactory.getLogger(DatabaseIndexInitializer.class);

    /**
     * 应用启动后补齐业务索引。
     *
     * <p>实现步骤：
     * 1. 等待 Hibernate 完成表结构更新；
     * 2. 清理库存流水历史单列唯一索引，避免多公司账套或重新入库时被旧约束阻断；
     * 3. 按索引清单逐条执行 create index if not exists；
     * 4. 所有索引限定在当前连接的 fm 库中，不跨库操作。</p>
     */
    @Bean
    @Order(3)
    public CommandLineRunner applyDatabaseIndexes(JdbcTemplate jdbcTemplate) {
        return args -> {
            // 变量说明：vendor 保存当前步骤计算、查询或转换得到的中间结果。
            DatabaseVendor vendor = databaseVendor(jdbcTemplate);
            repairInventoryLedgerUniqueIndexes(jdbcTemplate, vendor);
            indexDefinitions(vendor).forEach(jdbcTemplate::execute);
        };
    }

    /**
     * 返回系统当前需要维护的索引 DDL。
     */
    private List<String> indexDefinitions(DatabaseVendor vendor) {
        List<String> indexes = new java.util.ArrayList<>(List.of(
                "create index if not exists idx_fm_users_enabled on fm_users (enabled)",
                "create index if not exists idx_fm_users_department on fm_users (department)",
                "create index if not exists idx_fm_users_org on fm_users (organization_code)",
                "create index if not exists idx_fm_users_company_enabled on fm_users (organization_code, enabled)",

                "create index if not exists idx_fm_menus_parent on fm_menus (parent_id)",
                "create index if not exists idx_fm_menus_enabled_sort on fm_menus (enabled, sort_order, id)",
                "create index if not exists idx_fm_menus_type on fm_menus (type)",

                "create index if not exists idx_fm_user_login_sessions_user_id on fm_user_login_sessions (user_id)",
                "create index if not exists idx_fm_user_login_sessions_identity_terminal_status on fm_user_login_sessions (identity_no, terminal_type, status)",
                "create index if not exists idx_fm_user_login_sessions_company_identity_terminal on fm_user_login_sessions (organization_code, identity_no, terminal_type, status)",
                "create index if not exists idx_fm_user_login_sessions_status_expires on fm_user_login_sessions (status, expires_at)",
                "create index if not exists idx_fm_user_menu_usages_user_count on fm_user_menu_usages (organization_code, user_id, use_count desc, last_used_at desc)",

                "create index if not exists idx_fm_user_operation_logs_operator_time on fm_user_operation_logs (operator_id, operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_time on fm_user_operation_logs (operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_username_time on fm_user_operation_logs (operator_username, operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_identity_time on fm_user_operation_logs (identity_no, operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_phone_time on fm_user_operation_logs (contact_phone, operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_department_time on fm_user_operation_logs (department, operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_action_time on fm_user_operation_logs (action, operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_terminal_time on fm_user_operation_logs (terminal_type, terminal_identifier, operation_time desc)",
                "create index if not exists idx_fm_user_operation_logs_company_time on fm_user_operation_logs (organization_code, operation_time desc)",
                "create index if not exists idx_fm_business_operation_logs_biz_time_desc on fm_business_operation_logs (business_type, business_id, operation_time desc, id desc)",
                "create index if not exists idx_fm_business_operation_logs_company_biz_time on fm_business_operation_logs (organization_code, business_type, business_id, operation_time desc)",
                "create index if not exists idx_fm_business_operation_logs_operator_time on fm_business_operation_logs (operator_username, operation_time desc)",

                "create unique index if not exists uk_fm_basic_dictionaries_code on fm_basic_dictionaries (code)",
                "create index if not exists idx_fm_basic_dictionaries_parent_sort on fm_basic_dictionaries (parent_id, sort_order, id)",
                "create index if not exists idx_fm_basic_dictionaries_enabled_parent on fm_basic_dictionaries (enabled, parent_id, sort_order, id)",

                "create index if not exists idx_fm_accounting_subjects_enabled_code on fm_accounting_subjects (enabled, code)",
                "create index if not exists idx_fm_accounting_subjects_company_enabled_code on fm_accounting_subjects (organization_code, enabled, code)",
                "create index if not exists idx_fm_accounting_subjects_parent on fm_accounting_subjects (parent_id)",
                "create index if not exists idx_fm_accounting_subjects_category on fm_accounting_subjects (category)",

                "create index if not exists idx_fm_vouchers_date on fm_vouchers (voucher_date desc)",
                "create index if not exists idx_fm_vouchers_company_date on fm_vouchers (organization_code, voucher_date desc)",
                "create index if not exists idx_fm_vouchers_belong_month on fm_vouchers (belong_month)",
                "create index if not exists idx_fm_vouchers_status_date on fm_vouchers (status, voucher_date desc)",
                "create index if not exists idx_fm_vouchers_created_by on fm_vouchers (created_by)",
                "create index if not exists idx_fm_vouchers_source_biz_no on fm_vouchers (source_biz_no)",
                "create index if not exists idx_fm_vouchers_source_type_id on fm_vouchers (organization_code, source_type, source_id)",
                "create index if not exists idx_fm_vouchers_currency on fm_vouchers (currency_code)",
                "create index if not exists idx_fm_vouchers_project on fm_vouchers (project_code)",

                "create index if not exists idx_fm_voucher_lines_voucher_line_no on fm_voucher_lines (voucher_id, line_no)",
                "create index if not exists idx_fm_voucher_lines_subject on fm_voucher_lines (subject_id)",

                "create unique index if not exists uk_fm_accounting_periods_company_period on fm_accounting_periods (organization_code, period_code)",
                "create index if not exists idx_fm_accounting_periods_company_status on fm_accounting_periods (organization_code, status)",

                "create index if not exists idx_fm_purchase_orders_date on fm_purchase_orders (order_date desc)",
                "create index if not exists idx_fm_purchase_orders_company_date on fm_purchase_orders (organization_code, order_date desc)",
                "create index if not exists idx_fm_purchase_orders_status_date on fm_purchase_orders (status, order_date desc)",
                "create index if not exists idx_fm_purchase_orders_supplier on fm_purchase_orders (supplier_name)",
                "create index if not exists idx_fm_purchase_orders_created_by on fm_purchase_orders (created_by)",
                "create index if not exists idx_fm_purchase_orders_currency on fm_purchase_orders (currency_code)",
                "create index if not exists idx_fm_purchase_orders_project on fm_purchase_orders (project_code)",
                "create index if not exists idx_fm_purchase_orders_voucher on fm_purchase_orders (organization_code, voucher_id)",

                "create index if not exists idx_fm_purchase_order_lines_order_line_no on fm_purchase_order_lines (purchase_order_id, line_no)",
                "create index if not exists idx_fm_purchase_order_lines_item_code on fm_purchase_order_lines (item_code)",

                "create index if not exists idx_fm_shipment_orders_planned_date on fm_shipment_orders (planned_ship_date desc)",
                "create index if not exists idx_fm_shipment_orders_company_planned_date on fm_shipment_orders (organization_code, planned_ship_date desc)",
                "create index if not exists idx_fm_shipment_orders_status_date on fm_shipment_orders (status, planned_ship_date desc)",
                "create index if not exists idx_fm_shipment_orders_related_order on fm_shipment_orders (related_order_no)",
                "create index if not exists idx_fm_shipment_orders_tracking on fm_shipment_orders (tracking_no)",
                "create index if not exists idx_fm_shipment_orders_project on fm_shipment_orders (project_code)",
                "create index if not exists idx_fm_shipment_orders_origin_division on fm_shipment_orders (origin_division_code)",
                "create index if not exists idx_fm_shipment_orders_destination_division on fm_shipment_orders (destination_division_code)",
                "create index if not exists idx_fm_shipment_operation_logs_shipment_time_desc on fm_shipment_operation_logs (shipment_order_id, operation_time desc, id desc)",
                "create index if not exists idx_fm_shipment_operation_logs_status_time on fm_shipment_operation_logs (to_status, operation_time desc)",

                "create index if not exists idx_fm_inventory_ledgers_date on fm_inventory_ledgers (movement_date desc)",
                "create index if not exists idx_fm_inventory_ledgers_company_date on fm_inventory_ledgers (organization_code, movement_date desc)",
                "create index if not exists idx_fm_inventory_ledgers_item_date on fm_inventory_ledgers (item_code, movement_date desc)",
                "create index if not exists idx_fm_inventory_ledgers_item_name on fm_inventory_ledgers (item_name)",
                "create index if not exists idx_fm_inventory_ledgers_from_warehouse on fm_inventory_ledgers (from_warehouse)",
                "create index if not exists idx_fm_inventory_ledgers_to_warehouse on fm_inventory_ledgers (to_warehouse)",
                "create index if not exists idx_fm_inventory_ledgers_type_date on fm_inventory_ledgers (movement_type, movement_date desc)",
                "create index if not exists idx_fm_inventory_ledgers_related_biz on fm_inventory_ledgers (related_biz_no)",
                "create index if not exists idx_fm_inventory_ledgers_org on fm_inventory_ledgers (organization_code)",
                "create index if not exists idx_fm_inventory_ledgers_project on fm_inventory_ledgers (project_code)",
                "create index if not exists idx_fm_inventory_ledgers_voucher on fm_inventory_ledgers (organization_code, voucher_id)",
                "create unique index if not exists uk_fm_inventory_company_no on fm_inventory_ledgers (organization_code, movement_no)",

                "create index if not exists idx_fm_ar_ap_bills_due_date on fm_ar_ap_bills (due_date asc)",
                "create index if not exists idx_fm_ar_ap_bills_company_due_date on fm_ar_ap_bills (organization_code, due_date asc)",
                "create index if not exists idx_fm_ar_ap_bills_type_status_due on fm_ar_ap_bills (bill_type, status, due_date)",
                "create index if not exists idx_fm_ar_ap_bills_partner on fm_ar_ap_bills (partner_name)",
                "create index if not exists idx_fm_ar_ap_bills_org on fm_ar_ap_bills (organization_code)",
                "create index if not exists idx_fm_ar_ap_bills_currency on fm_ar_ap_bills (currency_code)",
                "create index if not exists idx_fm_ar_ap_bills_project_partner on fm_ar_ap_bills (project_code, partner_name)",
                "create index if not exists idx_fm_ar_ap_bills_voucher on fm_ar_ap_bills (organization_code, voucher_id)",

                "create index if not exists idx_fm_attachments_uploader_time on fm_attachments (uploader_id, created_time desc)",
                "create index if not exists idx_fm_business_attachments_biz on fm_business_attachments (business_type, business_id, sort_order, id)",
                "create index if not exists idx_fm_business_attachments_attachment on fm_business_attachments (attachment_id)",
                "create index if not exists idx_fm_workflow_instances_project on fm_workflow_instances (organization_code, project_code)"
        ));
        if (vendor == DatabaseVendor.POSTGRESQL) {
            indexes.add("create unique index if not exists uk_fm_basic_dictionaries_parent_name on fm_basic_dictionaries (coalesce(parent_id, 0), name)");
        }
        return indexes;
    }

    /**
     * 判断当前连接数据库类型。
     *
     * <p>实现步骤：读取 JDBC metadata 的数据库产品名，PostgreSQL 执行表达式唯一索引，H2 只执行普通索引。</p>
     */
    private DatabaseVendor databaseVendor(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.execute((ConnectionCallback<DatabaseVendor>) connection -> {
            // 变量说明：productName 保存当前步骤计算、查询或转换得到的中间结果。
            String productName = connection.getMetaData().getDatabaseProductName();
            if (productName != null && productName.toLowerCase(java.util.Locale.ROOT).contains("postgresql")) {
                return DatabaseVendor.POSTGRESQL;
            }
            return DatabaseVendor.H2;
        });
    }

    /**
     * 清理库存流水历史单列唯一索引。
     *
     * <p>实现步骤：
     * 1. 按数据库类型查找 fm_inventory_ledgers(movement_no) 上的单列唯一约束或索引；
     * 2. 先尝试按约束删除，再尝试按索引删除；
     * 3. 清理失败只记录日志并继续，让应用仍可启动，后续复核具体历史库差异。</p>
     */
    private void repairInventoryLedgerUniqueIndexes(JdbcTemplate jdbcTemplate, DatabaseVendor vendor) {
        try {
            if (vendor == DatabaseVendor.POSTGRESQL) {
                dropPostgresqlInventoryMovementNoUniques(jdbcTemplate);
                return;
            }
            dropH2InventoryMovementNoUniques(jdbcTemplate);
        } catch (Exception ex) {
            log.warn("repair inventory ledger movement_no unique index skipped", ex);
        }
    }

    /**
     * 清理 PostgreSQL 历史库中库存流水号的全局唯一约束或索引。
     *
     * <p>实现步骤：分别从 pg_constraint 和 pg_index 中定位仅包含 movement_no 的唯一对象，并用安全引用名删除。</p>
     */
    private void dropPostgresqlInventoryMovementNoUniques(JdbcTemplate jdbcTemplate) {
        List<String> constraints = jdbcTemplate.queryForList("""
                select c.conname
                from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                join pg_namespace n on n.oid = t.relnamespace
                join pg_index ix on ix.indexrelid = c.conindid
                where n.nspname = current_schema()
                  and t.relname = 'fm_inventory_ledgers'
                  and c.contype = 'u'
                  and array(
                      select a.attname
                      from unnest(ix.indkey) with ordinality as k(attnum, ord)
                      join pg_attribute a on a.attrelid = t.oid and a.attnum = k.attnum
                      order by k.ord
                  ) = array['movement_no']
                """, String.class);
        constraints.forEach(name -> jdbcTemplate.execute("alter table fm_inventory_ledgers drop constraint if exists " + quoteIdentifier(name)));

        List<String> indexes = jdbcTemplate.queryForList("""
                select i.relname
                from pg_index ix
                join pg_class i on i.oid = ix.indexrelid
                join pg_class t on t.oid = ix.indrelid
                join pg_namespace n on n.oid = t.relnamespace
                left join pg_constraint c on c.conindid = ix.indexrelid
                where n.nspname = current_schema()
                  and t.relname = 'fm_inventory_ledgers'
                  and ix.indisunique
                  and not ix.indisprimary
                  and c.oid is null
                  and array(
                      select a.attname
                      from unnest(ix.indkey) with ordinality as k(attnum, ord)
                      join pg_attribute a on a.attrelid = t.oid and a.attnum = k.attnum
                      order by k.ord
                  ) = array['movement_no']
                """, String.class);
        indexes.forEach(name -> jdbcTemplate.execute("drop index if exists " + quoteIdentifier(name)));
    }

    /**
     * 清理 H2 历史库中 Hibernate 由 unique=true 自动生成的库存流水号单列唯一索引。
     *
     * <p>实现步骤：从 information_schema.index_columns 查询仅包含 movement_no 的索引名，并按约束、索引两种方式兜底删除。</p>
     */
    private void dropH2InventoryMovementNoUniques(JdbcTemplate jdbcTemplate) {
        List<String> constraints = jdbcTemplate.queryForList("""
                select tc.constraint_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on kcu.constraint_schema = tc.constraint_schema
                 and kcu.constraint_name = tc.constraint_name
                 and kcu.table_name = tc.table_name
                where lower(tc.table_name) = 'fm_inventory_ledgers'
                  and upper(tc.constraint_type) = 'UNIQUE'
                group by tc.constraint_name
                having count(*) = 1 and max(lower(kcu.column_name)) = 'movement_no'
                """, String.class);
        for (String constraintName : constraints) {
            try {
                jdbcTemplate.execute("alter table fm_inventory_ledgers drop constraint if exists " + quoteIdentifier(constraintName));
            } catch (Exception ex) {
                log.debug("drop h2 inventory movement_no constraint skipped: {}", constraintName, ex);
            }
        }

        List<String> indexes = jdbcTemplate.queryForList("""
                select index_name
                from information_schema.index_columns
                where lower(table_name) = 'fm_inventory_ledgers'
                group by index_name
                having count(*) = 1 and max(lower(column_name)) = 'movement_no'
                """, String.class);
        for (String indexName : indexes) {
            String safeName = quoteIdentifier(indexName);
            try {
                jdbcTemplate.execute("alter table fm_inventory_ledgers drop constraint if exists " + safeName);
            } catch (Exception ex) {
                log.debug("drop h2 inventory movement_no constraint skipped: {}", indexName, ex);
            }
            try {
                jdbcTemplate.execute("drop index if exists " + safeName);
            } catch (Exception ex) {
                log.debug("drop h2 inventory movement_no index skipped: {}", indexName, ex);
            }
        }
    }

    /**
     * 对数据库对象名做最小安全引用。
     *
     * <p>实现步骤：只允许字母、数字、下划线和连字符，校验通过后加双引号，避免拼接系统表返回值时出现 SQL 注入风险。</p>
     */
    private String quoteIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("非法数据库对象名: " + value);
        }
        return "\"" + value + "\"";
    }

    /**
     * DatabaseVendor 枚举。
     * 
     * <p>用于承载 DatabaseVendor 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
     */
    private enum DatabaseVendor {
        /**
         * 枚举值 POSTGRESQL：表示 POSTGRESQL 对应的业务状态或类型。
         */
        POSTGRESQL,
        H2
    }
}
