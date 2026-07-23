package com.ratel.fm.service.common;

import com.ratel.fm.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * 业务单号序号服务。
 *
 * <p>实现目的：为采购、物流、库存、应收应付和出纳等按日期生成的单号提供 JVM 内原子序号，
 * 避免多用户并发新增时同时读取到相同最大号而撞唯一索引。</p>
 */
@Service
public class BusinessNumberSequenceService {

    /** 按业务域、所属公司和日期前缀缓存下一个可尝试序号。 */
    private final ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * 生成下一个业务单号。
     *
     * <p>实现步骤：
     * 1. 使用业务域、所属公司和单号前缀定位独立号段；
     * 2. 首次使用时由调用方读取数据库最大号并初始化下一个序号；
     * 3. 后续并发请求通过 AtomicInteger 原子递增拿号；
     * 4. 对极少数外部导入或重启边界产生的已存在号码继续递增跳过。</p>
     */
    public String next(
            String domain,
            String companyCode,
            String prefix,
            IntSupplier initialNextSequenceSupplier,
            Predicate<String> exists
    ) {
        String key = domain + "|" + companyCode + "|" + prefix;
        AtomicInteger counter = counters.computeIfAbsent(key, ignored -> new AtomicInteger(Math.max(1, initialNextSequenceSupplier.getAsInt())));
        for (int attempt = 0; attempt < 10000; attempt++) {
            String number = prefix + String.format("%04d", counter.getAndIncrement());
            if (!exists.test(number)) {
                return number;
            }
        }
        throw new BusinessException("业务单号生成失败，请稍后重试");
    }
}
