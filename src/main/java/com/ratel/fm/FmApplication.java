package com.ratel.fm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FmApplication 类。
 * 
 * <p>用于承载 FmApplication 相关的业务数据、接口契约或处理逻辑，便于维护人员快速定位模块职责。</p>
 */
@SpringBootApplication
@EnableScheduling
public class FmApplication {

    /**
     * 执行 main 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    public static void main(String[] args) {
        SpringApplication.run(FmApplication.class, args);
    }
}
