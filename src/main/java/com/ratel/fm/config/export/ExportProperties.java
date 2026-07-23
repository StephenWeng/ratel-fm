package com.ratel.fm.config.export;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 导出配置。
 *
 * <p>集中管理列表导出的最大行数，避免大数据量导出占用过多内存。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
public class ExportProperties {

    /**
     * 字段 maxRows：保存 maxRows 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final int maxRows;

    /**
     * 构造 ExportProperties 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public ExportProperties(@Value("${app.export.max-rows:5000}") int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * 读取导出最大行数。
     *
     * <p>实现步骤：如果配置值小于 1，则回退到 5000，防止错误配置导致导出为空或无限制。</p>
     */
    public int maxRows() {
        return maxRows < 1 ? 5000 : maxRows;
    }
}
