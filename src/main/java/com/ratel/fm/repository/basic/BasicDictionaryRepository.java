package com.ratel.fm.repository.basic;

import com.ratel.fm.domain.basic.BasicDictionary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 基础信息字典数据访问接口。
 *
 * <p>用于基础信息页面维护层级字典，也用于采购、物流、人员等业务模块加载启用字典选项。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
public interface BasicDictionaryRepository extends JpaRepository<BasicDictionary, Long> {

    /**
     * 根据字典编码查询字典。
     */
    @EntityGraph(attributePaths = "parent")
    Optional<BasicDictionary> findByCode(String code);

    /**
     * 判断字典编码是否已经存在。
     */
    boolean existsByCode(String code);

    /**
     * 查询全部字典并加载父级，用于前端构造层级树。
     */
    @EntityGraph(attributePaths = "parent")
    List<BasicDictionary> findAllByOrderBySortOrderAscIdAsc();

    /**
     * 查询指定父级下启用的直接子级字典，用于采购方、物流方等业务下拉。
     */
    @EntityGraph(attributePaths = "parent")
    List<BasicDictionary> findByParentIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long parentId);

    /**
     * 查询指定父级下的直接子级字典，用于人员部门、组织、岗位等字典化业务选择。
     */
    @EntityGraph(attributePaths = "parent")
    List<BasicDictionary> findByParentIdOrderBySortOrderAscIdAsc(Long parentId);

    /**
     * 查询根层级字典，用于字典管理懒加载表格首屏展示。
     */
    @EntityGraph(attributePaths = "parent")
    List<BasicDictionary> findByParentIsNullOrderBySortOrderAscIdAsc();

    /**
     * 查询指定父级下的直接子级字典，用于删除前检查层级完整性。
     */
    boolean existsByParentId(Long parentId);

    /**
     * 判断同一父级下是否存在同名字典。
     */
    boolean existsByParentIdAndName(Long parentId, String name);

    /**
     * 判断根层级下是否存在同名字典。
     */
    boolean existsByParentIsNullAndName(String name);
}
