package com.enterprise.aiportal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.aiportal.dto.Result;
import com.enterprise.aiportal.entity.SysPermission;
import com.enterprise.aiportal.mapper.SysPermissionMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/menus")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class MenuAdminController {

    private final SysPermissionMapper sysPermissionMapper;

    public MenuAdminController(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @GetMapping
    public Result<List<SysPermission>> list() {
        // 返回所有菜单类型权限，前端自行构建树
        List<SysPermission> list = sysPermissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermType, "menu")
                .orderByAsc(SysPermission::getSortOrder)
        );
        return Result.ok(list);
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody SysPermission perm) {
        SysPermission exist = sysPermissionMapper.selectById(id);
        if (exist == null) {
            return Result.error("菜单不存在");
        }
        if (!"menu".equals(exist.getPermType())) {
            return Result.error("只能编辑菜单类型权限");
        }
        SysPermission update = new SysPermission();
        update.setId(id);
        update.setPermName(perm.getPermName());
        update.setSortOrder(perm.getSortOrder());
        sysPermissionMapper.updateById(update);
        return Result.ok("更新成功");
    }

    @PutMapping("/{id}/status")
    public Result<String> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysPermission exist = sysPermissionMapper.selectById(id);
        if (exist == null) {
            return Result.error("菜单不存在");
        }
        SysPermission update = new SysPermission();
        update.setId(id);
        update.setStatus(status);
        sysPermissionMapper.updateById(update);
        return Result.ok("更新成功");
    }
}
