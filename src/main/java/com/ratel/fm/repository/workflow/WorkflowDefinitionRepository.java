package com.ratel.fm.repository.workflow;

import com.ratel.fm.domain.workflow.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 流程定义数据访问接口。
 *
 * <p>实现目的：为流程定义列表、模板保存和流程发起时读取启用模板提供统一数据访问能力。</p>
 */
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long>, JpaSpecificationExecutor<WorkflowDefinition> {

    /** 判断同一所属公司内流程定义编码是否已存在。 */
    boolean existsByOrganizationCodeAndCode(String organizationCode, String code);

    /** 按所属公司和流程定义 ID 查询模板，防止跨账套访问。 */
    Optional<WorkflowDefinition> findByOrganizationCodeAndId(String organizationCode, Long id);

    /** 按所属公司和模板编码查询流程定义。 */
    Optional<WorkflowDefinition> findByOrganizationCodeAndCode(String organizationCode, String code);

    /** 查询当前所属公司启用流程定义，供流程管理下拉选择。 */
    List<WorkflowDefinition> findByOrganizationCodeAndEnabledTrueOrderByNameAscIdAsc(String organizationCode);
}
