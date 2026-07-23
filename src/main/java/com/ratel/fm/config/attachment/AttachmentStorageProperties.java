package com.ratel.fm.config.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 附件磁盘存储配置。
 *
 * <p>默认使用程序运行目录下的 files 文件夹；打包部署时随启动目录迁移，便于单机笔记本部署。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
@ConfigurationProperties(prefix = "app.attachments")
public class AttachmentStorageProperties {

    /** 附件根目录，默认相对程序运行目录的 files 文件夹。 */
    private String baseDir = "./files";

    /**
     * 执行 getBaseDir 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public String getBaseDir() {
        return baseDir;
    }

    /**
     * 执行 setBaseDir 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
}
