package com.ratel.fm.config.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * H2 模板数据库生成后自动退出配置。
 *
 * <p>打包阶段使用 h2-template profile 启动一次应用上下文，只执行 JPA 建表、索引初始化和基础数据初始化，
 * 完成后关闭上下文退出 Maven 执行，避免真正启动 Web 服务。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
@ConditionalOnProperty(prefix = "app.h2-template", name = "exit-after-startup", havingValue = "true")
public class H2TemplateDatabaseExitRunner {

    /**
     * 在所有基础数据初始化完成后关闭应用上下文。
     *
     * <p>实现步骤：
     * 1. 该 Runner 使用最低优先级，等待菜单、角色、默认管理员、科目和基础字典写入；
     * 2. 调用 SpringApplication.exit 关闭 EntityManager、Hikari 连接池和 H2 文件句柄；
     * 3. Maven 后续 assembly 阶段即可安全复制生成的 ratel-fm.mv.db 文件。</p>
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public CommandLineRunner exitAfterTemplateDatabaseReady(ConfigurableApplicationContext context) {
        return args -> SpringApplication.exit(context, () -> 0);
    }
}
