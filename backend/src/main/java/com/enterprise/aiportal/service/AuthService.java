package com.enterprise.aiportal.service;

import com.enterprise.aiportal.dto.LoginDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.aiportal.entity.Department;
import com.enterprise.aiportal.entity.SysPermission;
import com.enterprise.aiportal.entity.SysRole;
import com.enterprise.aiportal.entity.SysUserRole;
import com.enterprise.aiportal.entity.User;
import com.enterprise.aiportal.mapper.DepartmentMapper;
import com.enterprise.aiportal.mapper.SysPermissionMapper;
import com.enterprise.aiportal.mapper.SysRoleMapper;
import com.enterprise.aiportal.mapper.SysUserRoleMapper;
import com.enterprise.aiportal.mapper.UserMapper;
import com.enterprise.aiportal.util.JwtUtil;
import com.enterprise.aiportal.vo.LoginVO;
import com.enterprise.aiportal.vo.MenuVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserMapper userMapper, SysRoleMapper sysRoleMapper,
                       SysPermissionMapper sysPermissionMapper, SysUserRoleMapper sysUserRoleMapper,
                       DepartmentMapper departmentMapper, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.departmentMapper = departmentMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("账户已被禁用");
        }

        ensureDefaultUserRole(user.getId());

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 查询角色
        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());

        // 查询菜单
        List<SysPermission> menus = sysPermissionMapper.selectMenusByUserId(user.getId());
        List<MenuVO> menuTree = buildMenuTree(menus);

        // 查询所有权限编码（菜单 + 按钮）
        List<String> permCodes = sysPermissionMapper.selectPermCodesByUserId(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        fillDepartmentInfo(vo, user);
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roleCodes);
        vo.setPermissions(permCodes);
        vo.setMenus(menuTree);

        return vo;
    }

    public LoginVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        ensureDefaultUserRole(userId);

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(userId);
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());

        List<SysPermission> menus = sysPermissionMapper.selectMenusByUserId(userId);
        List<MenuVO> menuTree = buildMenuTree(menus);

        // 查询所有权限编码（菜单 + 按钮）
        List<String> permCodes = sysPermissionMapper.selectPermCodesByUserId(userId);

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        fillDepartmentInfo(vo, user);
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roleCodes);
        vo.setPermissions(permCodes);
        vo.setMenus(menuTree);

        return vo;
    }

    private void fillDepartmentInfo(LoginVO vo, User user) {
        vo.setDeptId(user.getDeptId());
        if (user.getDeptId() == null) {
            return;
        }
        Department department = departmentMapper.selectById(user.getDeptId());
        if (department != null) {
            vo.setDeptName(department.getDeptName());
        }
    }

    private void ensureDefaultUserRole(Long userId) {
        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(userId);
        if (!roles.isEmpty()) {
            return;
        }

        SysRole userRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, "user")
                        .eq(SysRole::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (userRole == null) {
            return;
        }

        SysUserRole userRoleLink = new SysUserRole();
        userRoleLink.setUserId(userId);
        userRoleLink.setRoleId(userRole.getId());
        sysUserRoleMapper.insert(userRoleLink);
    }

    private List<MenuVO> buildMenuTree(List<SysPermission> menus) {
        List<MenuVO> all = menus.stream().map(this::convert).collect(Collectors.toList());
        return buildTree(all, 0L);
    }

    private List<MenuVO> buildTree(List<MenuVO> nodes, Long parentId) {
        List<MenuVO> result = new ArrayList<>();
        for (MenuVO node : nodes) {
            if (node.getParentId() != null && node.getParentId().equals(parentId)) {
                node.setChildren(buildTree(nodes, node.getId()));
                result.add(node);
            }
        }
        return result;
    }

    private MenuVO convert(SysPermission p) {
        MenuVO vo = new MenuVO();
        vo.setId(p.getId());
        vo.setPermCode(p.getPermCode());
        vo.setPermName(p.getPermName());
        vo.setPermType(p.getPermType());
        vo.setParentId(p.getParentId());
        vo.setPath(p.getPath());
        vo.setIcon(p.getIcon());
        vo.setComponent(p.getComponent());
        vo.setSortOrder(p.getSortOrder());
        return vo;
    }
}
