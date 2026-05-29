package com.enterprise.aiportal.service;

import com.enterprise.aiportal.mapper.SysPermissionMapper;
import com.enterprise.aiportal.mapper.SysRoleMapper;
import com.enterprise.aiportal.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public PermissionService(SysPermissionMapper sysPermissionMapper, SysRoleMapper sysRoleMapper,
                             SysUserRoleMapper sysUserRoleMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    public List<String> getUserPermissions(Long userId) {
        return sysPermissionMapper.selectPermCodesByUserId(userId);
    }

    public boolean hasPermission(Long userId, String permCode) {
        List<String> perms = getUserPermissions(userId);
        return perms.contains(permCode);
    }

    public boolean hasRole(Long userId, String roleCode) {
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(userId);
        return roles.contains(roleCode);
    }

    public List<Long> getUserRoleIds(Long userId) {
        return sysUserRoleMapper.selectRoleIdsByUserId(userId);
    }
}
