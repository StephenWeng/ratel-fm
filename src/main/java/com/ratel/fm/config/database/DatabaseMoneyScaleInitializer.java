package com.ratel.fm.config.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;

/**
 * 金额字段精度初始化器。
 *
 * <p>将凭证、采购、应收应付等涉金额字段统一扩展为 8 位小数，兼容已经生成的 H2 文件库和历史 PostgreSQL 库。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
@ConditionalOnProperty(prefix = "app.database.initializer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMoneyScaleInitializer {

    /**
     * 数据库精度初始化日志对象，用于记录启动时字段调整结果和兼容性异常。
     */
    private static final Logger log = LoggerFactory.getLogger(DatabaseMoneyScaleInitializer.class);

    /**
     * 应用启动后统一调整金额字段精度。
     *
     * <p>实现步骤：
     * 1. 识别当前数据库产品；
     * 2. 遍历金额字段清单；
     * 3. PostgreSQL 使用 alter column type，H2 使用 alter column；
     * 4. 单列调整失败时记录日志并继续，避免个别旧库差异阻塞应用启动。</p>
     */
    @Bean
    @Order(1)
    public CommandLineRunner normalizeMoneyScale(JdbcTemplate jdbcTemplate) {
        return args -> {
            // 变量说明：database 保存当前步骤计算、查询或转换得到的中间结果。
            String database = databaseProductName(jdbcTemplate);
            // 变量说明：postgresql 保存当前步骤计算、查询或转换得到的中间结果。
            boolean postgresql = database.contains("postgresql");
            // 变量说明：h2 保存当前步骤计算、查询或转换得到的中间结果。
            boolean h2 = database.contains("h2");
            if (!postgresql && !h2) {
                return;
            }
            for (MoneyColumn column : moneyColumns()) {
                String ddl = postgresql
                        ? "alter table " + column.tableName() + " alter column " + column.columnName() + " type numeric(26, 8)"
                        : "alter table " + column.tableName() + " alter column " + column.columnName() + " numeric(26, 8)";
                try {
                    jdbcTemplate.execute(ddl);
                } catch (Exception ex) {
                    log.warn("normalize money scale skipped: {}.{}", column.tableName(), column.columnName(), ex);
                }
            }
        };
    }

    /**
     * 获取数据库产品名称。
     *
     * <p>实现步骤：从当前连接元数据读取产品名，并统一转小写，便于后续分支判断。</p>
     */
    private String databaseProductName(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.execute((Connection connection) ->
                connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT));
    }

    /**
     * 系统内所有需要保留 8 位小数的金额字段。
     */
    private List<MoneyColumn> moneyColumns() {
        return List.of(
                new MoneyColumn("fm_vouchers", "total_debit"),
                new MoneyColumn("fm_vouchers", "total_credit"),
                new MoneyColumn("fm_vouchers", "total_debit_cny"),
                new MoneyColumn("fm_vouchers", "total_credit_cny"),
                new MoneyColumn("fm_voucher_lines", "debit_amount"),
                new MoneyColumn("fm_voucher_lines", "credit_amount"),
                new MoneyColumn("fm_voucher_lines", "debit_amount_cny"),
                new MoneyColumn("fm_voucher_lines", "credit_amount_cny"),
                new MoneyColumn("fm_purchase_orders", "total_amount"),
                new MoneyColumn("fm_purchase_orders", "total_amount_cny"),
                new MoneyColumn("fm_purchase_order_lines", "unit_price"),
                new MoneyColumn("fm_purchase_order_lines", "amount"),
                new MoneyColumn("fm_purchase_order_lines", "unit_price_cny"),
                new MoneyColumn("fm_purchase_order_lines", "amount_cny"),
                new MoneyColumn("fm_ar_ap_bills", "amount"),
                new MoneyColumn("fm_ar_ap_bills", "paid_amount"),
                new MoneyColumn("fm_ar_ap_bills", "amount_cny"),
                new MoneyColumn("fm_ar_ap_bills", "paid_amount_cny")
        );
    }

    /**
     * 金额字段描述。
     */
    private record MoneyColumn(String tableName, String columnName) {
    }
}
