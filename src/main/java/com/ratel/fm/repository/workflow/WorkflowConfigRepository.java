package com.ratel.fm.repository.workflow;

import com.ratel.fm.domain.workflow.WorkflowConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 流程配置数据访问接口。
 *
 * <p>实现目的：根据所属公司和功能模块编码定位当前生效流程模板，业务模块发起审批时只依赖该配置。</p>
 */
public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfig, Long>, JpaSpecificationExecutor<WorkflowConfig> {

    /** 按所属公司和配置 ID 查询流程配置。 */
    Optional<WorkflowConfig> findByOrganizationCodeAndId(String organizationCode, Long id);

    /** 按所属公司和功能模块编码查询流程配置。 */
    Optional<WorkflowConfig> findByOrganizationCodeAndFunctionModuleCode(String organizationCode, String functionModuleCode);

    /** 判断同一所属公司同一功能模块是否已配置流程。 */
    boolean existsByOrganizationCodeAndFunctionModuleCode(String organizationCode, String functionModuleCode);
}
