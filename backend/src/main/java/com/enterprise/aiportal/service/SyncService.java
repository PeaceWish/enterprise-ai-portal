package com.enterprise.aiportal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.aiportal.dto.sync.OrgSyncDTO;
import com.enterprise.aiportal.dto.sync.UserSyncDTO;
import com.enterprise.aiportal.entity.Department;
import com.enterprise.aiportal.entity.SysUserRole;
import com.enterprise.aiportal.entity.User;
import com.enterprise.aiportal.mapper.DepartmentMapper;
import com.enterprise.aiportal.mapper.SysUserRoleMapper;
import com.enterprise.aiportal.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SyncService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    // OA 同步用户的默认角色 ID（普通用户）
    private static final Long DEFAULT_ROLE_ID = 3L;

    public SyncService(DepartmentMapper departmentMapper, UserMapper userMapper,
                       SysUserRoleMapper sysUserRoleMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    /**
     * 同步组织机构（部门）
     */
    @Transactional
    public void syncOrganizations(OrgSyncDTO dto) {
        if (dto.getList() == null || dto.getList().isEmpty()) {
            log.info("组织同步：空数据，跳过");
            return;
        }

        int created = 0;
        int updated = 0;

        for (OrgSyncDTO.OrgItem item : dto.getList()) {
            if (item.getZzjgJc() == null) continue;

            OrgSyncDTO.OrgInfo org = item.getZzjgJc();
            String code = org.getOrganizationCode();
            String name = org.getOrganizationName();
            String status = org.getDataStatus();

            if (code == null || code.isBlank()) continue;

            // 根据部门编码查找是否已存在
            Department exist = departmentMapper.selectOne(
                new LambdaQueryWrapper<Department>()
                    .eq(Department::getDeptCode, code)
            );

            if (exist == null) {
                // 新增
                Department dept = new Department();
                dept.setDeptCode(code);
                dept.setDeptName(name);
                dept.setStatus("1".equals(status) ? 1 : 0);
                dept.setSource("oa_sync");
                dept.setCreatedAt(LocalDateTime.now());
                dept.setUpdatedAt(LocalDateTime.now());
                departmentMapper.insert(dept);
                created++;
            } else {
                // 更新
                exist.setDeptName(name);
                exist.setStatus("1".equals(status) ? 1 : 0);
                exist.setSource("oa_sync");
                exist.setUpdatedAt(LocalDateTime.now());
                departmentMapper.updateById(exist);
                updated++;
            }
        }

        log.info("组织同步完成：新增 {} 条，更新 {} 条", created, updated);
    }

    /**
     * 同步人员档案（用户）
     * 一个人员(REFFIELD)下可能有多个账号(ACCOUNT)，每个账号作为一个独立用户
     */
    @Transactional
    public void syncUsers(UserSyncDTO dto) {
        if (dto.getList() == null || dto.getList().isEmpty()) {
            log.info("人员同步：空数据，跳过");
            return;
        }

        // 预加载所有部门编码 -> ID 映射
        Map<String, Long> deptCodeMap = new HashMap<>();
        for (Department d : departmentMapper.selectList(null)) {
            if (d.getDeptCode() != null) {
                deptCodeMap.put(d.getDeptCode(), d.getId());
            }
        }

        int created = 0;
        int updated = 0;

        for (UserSyncDTO.UserItem item : dto.getList()) {
            if (item.getRyJc() == null) continue;

            UserSyncDTO.UserInfo userInfo = item.getRyJc();
            String refField = userInfo.getRefField();  // 人员编码（工号）

            if (userInfo.getChildModels() == null || userInfo.getChildModels().isEmpty()) {
                continue;
            }

            // 遍历该人员下的所有账号
            for (UserSyncDTO.AccountItem accItem : userInfo.getChildModels()) {
                if (accItem.getZh() == null) continue;

                UserSyncDTO.AccountInfo acc = accItem.getZh();
                String account = acc.getAccount();  // 登录账号（唯一标识）
                String userName = acc.getUserName();  // 姓名
                String accountStatus = acc.getAccountStatus();
                String orgCode = acc.getOrganizationCode();

                if (account == null || account.isBlank()) continue;

                // 查找是否已存在
                User exist = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getUserCode, account)
                );

                Long deptId = deptCodeMap.get(orgCode);

                if (exist == null) {
                    // 新增用户
                    User user = new User();
                    user.setUsername(account);  // 登录用户名 = account
                    user.setPassword("");  // OA 同步用户不设密码，用 SSO 登录
                    user.setRealName(userName);
                    user.setUserCode(account);
                    user.setDeptId(deptId);
                    user.setRole("user");  // 默认角色
                    user.setStatus("0".equals(accountStatus) ? 1 : 0);  // 0-启用->1, 1-停用->0
                    user.setSource("oa_sync");
                    user.setIdafeLoginId(account);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    userMapper.insert(user);

                    // 绑定默认角色（普通用户）
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(DEFAULT_ROLE_ID);
                    sysUserRoleMapper.insert(ur);

                    created++;
                } else {
                    // 更新用户
                    exist.setUsername(account);
                    exist.setRealName(userName);
                    exist.setUserCode(account);
                    exist.setDeptId(deptId);
                    exist.setStatus("0".equals(accountStatus) ? 1 : 0);
                    exist.setSource("oa_sync");
                    exist.setIdafeLoginId(account);
                    exist.setUpdatedAt(LocalDateTime.now());
                    userMapper.updateById(exist);
                    updated++;
                }
            }
        }

        log.info("人员同步完成：新增 {} 条，更新 {} 条", created, updated);
    }
}
