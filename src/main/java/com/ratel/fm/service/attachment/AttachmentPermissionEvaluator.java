package com.ratel.fm.service.attachment;

import com.ratel.fm.domain.attachment.AttachmentBusinessType;
import com.ratel.fm.domain.auth.PermissionCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 附件访问权限判断器。
 *
 * <p>附件接口通过业务类型动态映射到模块权限，保证统一附件接口不会绕开模块授权。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Component
public class AttachmentPermissionEvaluator {

    /**
     * 判断当前登录人是否可以查看业务附件。
     *
     * <p>实现步骤：具有对应模块管理权限可以查看；报表查看权限也可以查看业务证据，但不能改动附件。</p>
     */
    public boolean canView(AttachmentBusinessType businessType) {
        if (businessType == null) {
            return false;
        }
        return hasAuthority(businessType.managePermission()) || (businessType.reportVisible() && hasAuthority(PermissionCode.REPORT_VIEW));
    }

    /**
     * 判断当前登录人是否可以维护业务附件。
     *
     * <p>实现步骤：只有对应业务模块的管理权限可以上传、改名和删除附件。</p>
     */
    public boolean canManage(AttachmentBusinessType businessType) {
        return businessType != null && hasAuthority(businessType.managePermission());
    }

    /**
     * 执行 hasAuthority 方法。
     * 
     * <p>实现步骤：
     * 1. 接收并校验调用方传入的数据；
     * 2. 按当前方法职责执行业务查询、转换或持久化处理；
     * 3. 返回处理结果或更新对象状态。</p>
     */
    private boolean hasAuthority(PermissionCode permissionCode) {
        // 变量说明：authentication 保存当前步骤计算、查询或转换得到的中间结果。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> permissionCode.name().equals(authority.getAuthority()));
    }
}
