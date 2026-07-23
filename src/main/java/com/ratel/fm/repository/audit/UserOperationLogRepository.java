package com.ratel.fm.repository.audit;

import com.ratel.fm.domain.audit.UserOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 用户关键操作日志数据访问。
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface UserOperationLogRepository extends JpaRepository<UserOperationLog, Long>, JpaSpecificationExecutor<UserOperationLog> {
}
