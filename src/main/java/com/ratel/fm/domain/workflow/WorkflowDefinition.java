package com.ratel.fm.domain.workflow;

import com.ratel.fm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 流程定义实体。
 *
 * <p>实现目的：
 * 1. 保存流程模板的基本信息；
 * 2. 使用 JSON 保存节点定义，降低初期流程设计器落地成本；
 * 3. 运行流程时按定义快照生成实例和任务，后续模板修改不影响历史流程。</p>
 */
@Entity
@Table(name = "fm_workflow_definitions")
@Comment("流程定义表，保存可复用的审批模板和节点设计")
public class WorkflowDefinition extends BaseEntity {

    /** 所属公司字典编码，流程模板按账套隔离。 */
    @Column(nullable = false, length = 80)
    @Comment("所属公司字典编码，作为流程定义账套隔离标识")
    private String organizationCode;

    /** 流程模板名称，例如采购审批流程。 */
    @Column(nullable = false, length = 160)
    @Comment("流程模板名称")
    private String name;

    /** 流程模板编码，同一所属公司内唯一。 */
    @Column(nullable = false, length = 120)
    @Comment("流程模板编码")
    private String code;

    /** 流程说明。 */
    @Column(length = 500)
    @Comment("流程说明")
    private String description;

    /** 流程节点 JSON，按节点顺序保存审批人来源、部门、岗位和人员信息。 */
    @Column(nullable = false, columnDefinition = "text")
    @Comment("流程节点JSON")
    private String nodesJson;

    /** 是否启用流程模板。 */
    @Column(nullable = false)
    @Comment("是否启用流程模板")
    private boolean enabled = true;

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNodesJson() {
        return nodesJson;
    }

    public void setNodesJson(String nodesJson) {
        this.nodesJson = nodesJson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
