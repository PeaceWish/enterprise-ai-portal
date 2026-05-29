package com.enterprise.aiportal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.aiportal.dto.Result;
import com.enterprise.aiportal.entity.Department;
import com.enterprise.aiportal.entity.SysRole;
import com.enterprise.aiportal.entity.SysUserRole;
import com.enterprise.aiportal.entity.User;
import com.enterprise.aiportal.mapper.DepartmentMapper;
import com.enterprise.aiportal.mapper.SysRoleMapper;
import com.enterprise.aiportal.mapper.SysUserRoleMapper;
import com.enterprise.aiportal.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class UserAdminController {

    private final UserMapper userMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAdminController(UserMapper userMapper, SysUserRoleMapper sysUserRoleMapper,
                               SysRoleMapper sysRoleMapper, DepartmentMapper departmentMapper,
                               PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.departmentMapper = departmentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getUserCode, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量填充部门名称
        List<Long> deptIds = result.getRecords().stream()
                .map(User::getDeptId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!deptIds.isEmpty()) {
            List<Department> depts = departmentMapper.selectList(
                    new LambdaQueryWrapper<Department>().in(Department::getId, deptIds)
            );
            Map<Long, String> deptMap = depts.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getDeptName));
            for (User user : result.getRecords()) {
                if (user.getDeptId() != null) {
                    user.setDeptName(deptMap.getOrDefault(user.getDeptId(), ""));
                }
            }
        }

        // 批量填充角色 ID，保证用户编辑弹窗能正确回显已有授权
        for (User user : result.getRecords()) {
            user.setPassword(null);
            user.setRoleIds(sysUserRoleMapper.selectRoleIdsByUserId(user.getId()));
        }

        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<User> detail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
            user.setRoleIds(sysUserRoleMapper.selectRoleIdsByUserId(id));
        }
        return Result.ok(user);
    }

    @PostMapping
    @Transactional
    public Result<String> create(@RequestBody User user, @RequestParam(required = false) List<Long> roleIds) {
        // 检查用户名唯一
        User exist = userMapper.selectByUsername(user.getUsername());
        if (exist != null) {
            return Result.error("用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setSource("local");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 绑定角色；未指定时默认绑定普通用户角色，避免登录后没有菜单权限被踢回登录页
        List<Long> targetRoleIds = roleIds;
        if (targetRoleIds == null || targetRoleIds.isEmpty()) {
            SysRole userRole = sysRoleMapper.selectOne(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleCode, "user")
                            .eq(SysRole::getStatus, 1)
                            .last("LIMIT 1")
            );
            if (userRole != null) {
                targetRoleIds = List.of(userRole.getId());
            }
        }
        if (targetRoleIds != null && !targetRoleIds.isEmpty()) {
            for (Long roleId : targetRoleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }

        return Result.ok("创建成功");
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<String> update(@PathVariable Long id, @RequestBody User user,
                                  @RequestParam(required = false) List<Long> roleIds) {
        if (id == 1L) {
            return Result.error("系统内置管理员不可修改");
        }

        User exist = userMapper.selectById(id);
        if (exist == null) {
            return Result.error("用户不存在");
        }

        user.setId(id);
        // 不修改密码（单独接口）
        user.setPassword(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 更新角色绑定
        if (roleIds != null) {
            sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
            );
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }

        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        if (id == 1L) {
            return Result.error("系统内置管理员不可删除");
        }
        userMapper.deleteById(id);
        sysUserRoleMapper.delete(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
        );
        return Result.ok("删除成功");
    }

    @PostMapping("/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        if (id == 1L) {
            return Result.error("系统内置管理员密码不可重置");
        }
        User user = new User();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.ok("密码重置成功");
    }
}
