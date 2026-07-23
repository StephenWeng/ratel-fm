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

import java.util.Locale;

/**
 * 知识库表结构兼容初始化器。
 *
 * <p>AI 知识来源类型会随着业务模块扩展持续增加，历史 H2 文件库可能由 Hibernate
 * 自动建成 ENUM 列；启动时统一放宽为 varchar，避免新增来源类型后索引写入失败。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "app.database.initializer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeDatabaseCompatibilityInitializer {

    /** 初始化日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(KnowledgeDatabaseCompatibilityInitializer.class);

    /**
     * 应用启动后修正知识文档表的来源类型列。
     *
     * <p>实现步骤：
     * 1. 识别当前数据库产品；
     * 2. H2 使用 alter column varchar，PostgreSQL 使用 alter column type varchar；
     * 3. 调整失败时记录告警并继续启动，具体索引写入问题由业务日志继续暴露。</p>
     */
    @Bean
    @Order(2)
    public CommandLineRunner normalizeKnowledgeSourceTypeColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            DatabaseVendor vendor = databaseVendor(jdbcTemplate);
            if (vendor == DatabaseVendor.H2) {
                executeSafely(jdbcTemplate, "alter table fm_knowledge_documents alter column source_type varchar(40)");
                return;
            }
            if (vendor == DatabaseVendor.POSTGRESQL) {
                executeSafely(jdbcTemplate, "alter table fm_knowledge_documents alter column source_type type varchar(40)");
            }
        };
    }

    /**
     * 识别当前数据库类型。
     */
    private DatabaseVendor databaseVendor(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.execute((ConnectionCallback<DatabaseVendor>) connection -> {
            String productName = connection.getMetaData().getDatabaseProductName();
            String normalized = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
            if (normalized.contains("postgresql")) {
                return DatabaseVendor.POSTGRESQL;
            }
            if (normalized.contains("h2")) {
                return DatabaseVendor.H2;
            }
            return DatabaseVendor.OTHER;
        });
    }

    /**
     * 执行单条兼容 DDL。
     */
    private void executeSafely(JdbcTemplate jdbcTemplate, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
        } catch (Exception ex) {
            log.warn("normalize knowledge source_type column skipped", ex);
        }
    }

    /**
     * 支持的数据库类型。
     */
    private enum DatabaseVendor {
        H2,
        POSTGRESQL,
        OTHER
    }
}
