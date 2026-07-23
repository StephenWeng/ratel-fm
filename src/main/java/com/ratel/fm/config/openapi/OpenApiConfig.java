package com.ratel.fm.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 与 Knife4j 文档配置。
 *
 * <p>集中维护接口文档标题、版本、开发信息和 JWT 认证说明。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Configuration
public class OpenApiConfig {

    /**
     * 构建财务管理 ERP 的 OpenAPI 文档对象。
     *
     * <p>实现步骤：
     * 1. 设置文档标题、版本和开发组织信息；
     * 2. 声明 JWT Bearer 安全模式，便于 Knife4j 页面调试接口；
     * 3. 把安全模式加入全局安全要求。</p>
     */
    @Bean
    public OpenAPI fmOpenApi() {
        // 变量说明：schemeName 保存当前步骤计算、查询或转换得到的中间结果。
        String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Ratel FM 财务管理 ERP API")
                        .version("0.0.1")
                        .description("开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。覆盖人员授权、科目字典、凭证记账、采购、物流、库存、应收应付、统计分析、ratel助手与智能检索。"))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("HMAC"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
