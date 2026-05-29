package com.enterprise.aiportal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.aiportal.dto.Result;
import com.enterprise.aiportal.entity.SysPermission;
import com.enterprise.aiportal.entity.SysRole;
import com.enterprise.aiportal.entity.SysRolePermission;
import com.enterprise.aiportal.entity.SysUserRole;
import com.enterprise.aiportal.mapper.SysPermissionMapper;
import com.enterprise.aiportal.mapper.SysRoleMapper;
import com.enterprise.aiportal.mapper.SysRolePermissionMapper;
import com.enterprise.aiportal.mapper.SysUserRoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class RoleAdminController {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public RoleAdminController(SysRoleMapper sysRoleMapper, SysPermissionMapper sysPermissionMapper,
                               SysRolePermissionMapper sysRolePermissionMapper,
                               SysUserRoleMapper sysUserRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @GetMapping
    public Result<List<SysRole>> list() {
        List<SysRole> list = sysRoleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortOrder)
        );
        return Result.ok(list);
    }

    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<SysRolePermission> list = sysRolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id)
        );
        return Result.ok(list.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList()));
    }

    @GetMapping("/{id}/users")
    public Result<List<Long>> getRoleUsers(@PathVariable Long id) {
        List<SysUserRole> list = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id)
        );
        return Result.ok(list.stream().map(SysUserRole::getUserId).collect(Collectors.toList()));
    }

    @PutMapping("/{id}/users")
    @Transactional
    public Result<String> updateRoleUsers(@PathVariable Long id, @RequestBody(required = false) List<Long> userIds) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }

        sysUserRoleMapper.delete(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id)
        );

        if (userIds != null) {
            List<Long> distinctUserIds = new ArrayList<>(userIds.stream().distinct().collect(Collectors.toList()));
            if (id == 1L && !distinctUserIds.contains(1L)) {
                distinctUserIds.add(1L);
            }
            for (Long userId : distinctUserIds) {
                if (userId == null) continue;
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(id);
                sysUserRoleMapper.insert(ur);
            }
        } else if (id == 1L) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(1L);
            ur.setRoleId(id);
            sysUserRoleMapper.insert(ur);
        }

        return Result.ok("分配成功");
    }

    @PostMapping
    public Result<String> create(@RequestBody SysRole role) {
        role.setRoleType("custom");
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        sysRoleMapper.insert(role);
        return Result.ok("创建成功");
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<String> update(@PathVariable Long id, @RequestBody SysRole role,
                                  @RequestParam(required = false) List<Long> permissionIds) {
        if (id <= 3) {
            return Result.error("系统内置角色不可修改");
        }
        role.setId(id);
        role.setUpdatedAt(LocalDateTime.now());
        sysRoleMapper.updateById(role);

        if (permissionIds != null) {
            sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id)
            );
            for (Long permId : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }

        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        if (id <= 3) {
            return Result.error("系统内置角色不可删除");
        }
        sysRoleMapper.deleteById(id);
        sysRolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id)
        );
        return Result.ok("删除成功");
    }

    @GetMapping("/permissions/tree")
    public Result<List<SysPermission>> permissionTree() {
        List<SysPermission> all = sysPermissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder)
        );
        return Result.ok(all);
    }
}
