package com.enterprise.aiportal.controller;

import com.enterprise.aiportal.dto.Result;
import com.enterprise.aiportal.entity.Agent;
import com.enterprise.aiportal.entity.User;
import com.enterprise.aiportal.mapper.AgentMapper;
import com.enterprise.aiportal.mapper.UserMapper;
import com.enterprise.aiportal.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentMapper agentMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;

    public AgentController(AgentMapper agentMapper, UserMapper userMapper, PermissionService permissionService) {
        this.agentMapper = agentMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
    }

    /**
     * 获取智能体列表（全部，含离线）- 管理员用
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('admin:agents', 'agent:create', 'agent:edit', 'agent:delete')")
    public Result<List<Agent>> listAgents() {
        List<Agent> list = agentMapper.selectList(
            new LambdaQueryWrapper<Agent>().orderByDesc(Agent::getCreatedAt)
        );
        return Result.ok(list);
    }

    /**
     * 获取已激活的智能体列表（用于广场展示）- 带权限过滤
     */
    @GetMapping("/active")
    public Result<List<Agent>> listActiveAgents(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 未登录返回空（理论上已被 Security 拦截，但做兜底）
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        // 管理员返回全部
        if (permissionService.hasRole(userId, "super_admin") || permissionService.hasRole(userId, "admin")) {
            List<Agent> list = agentMapper.selectList(
                new LambdaQueryWrapper<Agent>()
                    .eq(Agent::getStatus, 1)
                    .orderByAsc(Agent::getSortOrder)
                    .orderByDesc(Agent::getCreatedAt)
            );
            return Result.ok(list);
        }

        User user = userMapper.selectById(userId);
        List<Long> roleIds = permissionService.getUserRoleIds(userId);

        String roleIdsJson = cn.hutool.json.JSONUtil.toJsonStr(roleIds);

        List<Agent> list = agentMapper.selectVisibleAgents(
            userId,
            user != null ? user.getDeptId() : null,
            roleIds,
            roleIdsJson
        );
        return Result.ok(list);
    }

    @GetMapping("/test-encoding")
    public Result<String> testEncoding() {
        return Result.ok("后端硬编码中文测试：连接成功！");
    }

    /**
     * 保存或更新智能体配置
     */
    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('agent:create', 'agent:edit')")
    public Result<String> saveAgent(@RequestBody Agent agent, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (agent.getId() == null) {
            agent.setCreatorId(userId);
            if (agent.getVisibility() == null) {
                agent.setVisibility("public");
            }
            if (agent.getSortOrder() == null) {
                agent.setSortOrder(0);
            }
            agentMapper.insert(agent);
            return Result.ok("创建成功");
        } else {
            agentMapper.updateById(agent);
            return Result.ok("更新成功");
        }
    }

    /**
     * 删除智能体
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('agent:delete')")
    public Result<String> deleteAgent(@PathVariable Long id) {
        agentMapper.deleteById(id);
        return Result.ok("删除成功");
    }
}
