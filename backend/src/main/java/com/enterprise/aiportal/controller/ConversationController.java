package com.enterprise.aiportal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.aiportal.entity.Agent;
import com.enterprise.aiportal.entity.ChatConversation;
import com.enterprise.aiportal.entity.ChatMessage;
import com.enterprise.aiportal.mapper.AgentMapper;
import com.enterprise.aiportal.mapper.ChatConversationMapper;
import com.enterprise.aiportal.mapper.ChatMessageMapper;
import com.enterprise.aiportal.mapper.SysRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ChatConversationMapper convMapper;
    private final ChatMessageMapper msgMapper;
    private final AgentMapper agentMapper;
    private final SysRoleMapper sysRoleMapper;

    public ConversationController(ChatConversationMapper convMapper, ChatMessageMapper msgMapper,
                                  AgentMapper agentMapper, SysRoleMapper sysRoleMapper) {
        this.convMapper = convMapper;
        this.msgMapper = msgMapper;
        this.agentMapper = agentMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listConversations(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return List.of();
        }

        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<ChatConversation>()
                .orderByDesc(ChatConversation::getUpdatedAt);
        if (!isSuperAdmin(currentUserId)) {
            wrapper.eq(ChatConversation::getUserId, String.valueOf(currentUserId));
        }
        List<ChatConversation> convs = convMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatConversation c : convs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("agentId", c.getAgentId());
            m.put("difyConversationId", c.getDifyConversationId());
            m.put("createdAt", c.getCreatedAt());
            m.put("updatedAt", c.getUpdatedAt());
            if (c.getAgentId() != null) {
                Agent agent = agentMapper.selectById(c.getAgentId());
                if (agent != null) {
                    m.put("agentName", agent.getAgentName());
                    m.put("agentIconUrl", agent.getIconUrl());
                }
            }
            result.add(m);
        }
        return result;
    }

    @GetMapping("/{convId}/messages")
    public Map<String, Object> getMessages(@PathVariable Long convId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        ChatConversation conv = convMapper.selectById(convId);
        if (conv == null || !canViewConversation(currentUserId, conv)) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("messages", List.of());
            resp.put("error", "无权查看该对话");
            return resp;
        }
        List<ChatMessage> msgs = msgMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, convId)
                .orderByAsc(ChatMessage::getSortOrder)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage m : msgs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("role", m.getRole());
            map.put("content", m.getContent());
            result.add(map);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("messages", result);
        if (conv != null) {
            resp.put("conversationId", conv.getId());
            resp.put("title", conv.getTitle());
            resp.put("difyConversationId", conv.getDifyConversationId());
            resp.put("agentId", conv.getAgentId());
            if (conv.getAgentId() != null) {
                Agent agent = agentMapper.selectById(conv.getAgentId());
                if (agent != null) {
                    Map<String, Object> agentMap = new HashMap<>();
                    agentMap.put("id", agent.getId());
                    agentMap.put("agentName", agent.getAgentName());
                    agentMap.put("description", agent.getDescription());
                    agentMap.put("iconUrl", agent.getIconUrl());
                    agentMap.put("status", agent.getStatus());
                    resp.put("agent", agentMap);
                }
            }
        }
        return resp;
    }

    @PostMapping("/save")
    public Map<String, Object> saveConversation(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        String userId = currentUserId != null ? String.valueOf(currentUserId) : "default_user";
        String title = (String) req.get("title");
        Long convId = req.get("conversationId") != null ? ((Number) req.get("conversationId")).longValue() : null;
        String difyConversationId = blankToNull((String) req.get("difyConversationId"));
        Long agentId = req.get("agentId") != null ? ((Number) req.get("agentId")).longValue() : null;

        ChatConversation conv;
        if (convId != null) {
            conv = convMapper.selectById(convId);
            if (conv != null && !String.valueOf(userId).equals(conv.getUserId())) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("message", "无权修改该对话");
                return resp;
            }
            if (conv != null) {
                conv.setTitle(title);
                if (difyConversationId != null) {
                    conv.setDifyConversationId(difyConversationId);
                }
                if (agentId != null) {
                    conv.setAgentId(agentId);
                }
                convMapper.updateById(conv);
            }
        } else {
            conv = new ChatConversation();
            conv.setUserId(userId);
            conv.setTitle(title);
            conv.setDifyConversationId(difyConversationId);
            conv.setAgentId(agentId);
            convMapper.insert(conv);
            convId = conv.getId();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> msgs = (List<Map<String, Object>>) req.get("messages");
        if (msgs != null && !msgs.isEmpty()) {
            msgMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, convId));
            int sort = 0;
            for (Map<String, Object> m : msgs) {
                ChatMessage msg = new ChatMessage();
                msg.setConversationId(convId);
                msg.setRole((String) m.get("role"));
                msg.setContent((String) m.get("content"));
                msg.setSortOrder(sort++);
                msgMapper.insert(msg);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("conversationId", convId);
        resp.put("success", true);
        return resp;
    }

    @DeleteMapping("/{convId}")
    public Map<String, Object> deleteConversation(@PathVariable Long convId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        Map<String, Object> resp = new HashMap<>();
        ChatConversation conv = convMapper.selectById(convId);
        if (conv == null) {
            resp.put("success", false);
            resp.put("message", "记录不存在");
            return resp;
        }
        // 超级管理员只能查看全部记录，不能删除任何记录
        if (isSuperAdmin(currentUserId)) {
            resp.put("success", false);
            resp.put("message", "超级管理员仅可查看，不能删除对话记录");
            return resp;
        }
        if (currentUserId == null || !String.valueOf(currentUserId).equals(conv.getUserId())) {
            resp.put("success", false);
            resp.put("message", "无权删除该对话");
            return resp;
        }
        msgMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getConversationId, convId));
        convMapper.deleteById(convId);
        resp.put("success", true);
        return resp;
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clearConversations(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        Map<String, Object> resp = new HashMap<>();
        if (currentUserId == null || isSuperAdmin(currentUserId)) {
            resp.put("success", false);
            resp.put("message", "无权清空对话记录");
            return resp;
        }
        String userId = String.valueOf(currentUserId);
        List<ChatConversation> convs = convMapper.selectList(
            new LambdaQueryWrapper<ChatConversation>().eq(ChatConversation::getUserId, userId));
        for (ChatConversation c : convs) {
            msgMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getConversationId, c.getId()));
        }
        convMapper.delete(new LambdaQueryWrapper<ChatConversation>().eq(ChatConversation::getUserId, userId));
        resp.put("success", true);
        return resp;
    }

    private boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return sysRoleMapper.selectRoleCodesByUserId(userId).contains("super_admin");
    }

    private boolean canViewConversation(Long currentUserId, ChatConversation conv) {
        if (currentUserId == null || conv == null) {
            return false;
        }
        return isSuperAdmin(currentUserId) || String.valueOf(currentUserId).equals(conv.getUserId());
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
