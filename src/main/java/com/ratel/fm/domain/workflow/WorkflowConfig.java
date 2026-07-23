package com.ratel.fm.domain.workflow;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 流程配置实体。
 *
 * <p>实现目的：按照所属公司、业务模块和功能模块绑定流程定义。业务单据只依赖功能模块编码，
 * 模板后续替换时无需修改采购、库存等业务代码。</p>
 */
@Entity
@Table(name = "fm_workflow_configs")
@Comment("流程配置表，按所属公司和功能模块绑定流程定义")
public class WorkflowConfig extends BaseEntity {

    /** 所属公司字典编码，流程配置按账套隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为流程配置账套隔离标识")
    private String organizationCode;

    /** 业务模块编码，例如 OPERATION。 */
    @Column(nullable = false, length = 80)
    @Comment("业务模块编码")
    private String businessModuleCode;

    /** 业务模块名称，例如业务管理。 */
    @Column(nullable = false, length = 120)
    @Comment("业务模块名称")
    private String businessModuleName;

    /** 功能模块编码，例如 PURCHASE_APPROVAL。 */
    @Column(nullable = false, length = 120)
    @Comment("功能模块编码")
    private String functionModuleCode;

    /** 功能模块名称，例如采购审批。 */
    @Column(nullable = false, length = 160)
    @Comment("功能模块名称")
    private String functionModuleName;

    /** 关联流程定义 ID。 */
    @Column(nullable = false)
    @Comment("流程定义ID")
    private Long definitionId;

    /** 关联流程定义名称快照。 */
    @Column(nullable = false, length = 160)
    @Comment("流程定义名称快照")
    private String definitionName;

    /** 是否启用该模块流程配置。 */
    @Column(nullable = false)
    @Comment("是否启用流程配置")
    private boolean enabled = true;

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public String getBusinessModuleCode() {
        return businessModuleCode;
    }

    public void setBusinessModuleCode(String businessModuleCode) {
        this.businessModuleCode = businessModuleCode;
    }

    public String getBusinessModuleName() {
        return businessModuleName;
    }

    public void setBusinessModuleName(String businessModuleName) {
        this.businessModuleName = businessModuleName;
    }

    public String getFunctionModuleCode() {
        return functionModuleCode;
    }

    public void setFunctionModuleCode(String functionModuleCode) {
        this.functionModuleCode = functionModuleCode;
    }

    public String getFunctionModuleName() {
        return functionModuleName;
    }

    public void setFunctionModuleName(String functionModuleName) {
        this.functionModuleName = functionModuleName;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public void setDefinitionName(String definitionName) {
        this.definitionName = definitionName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
