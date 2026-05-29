package com.enterprise.aiportal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.aiportal.dto.ChatRequest;
import com.enterprise.aiportal.entity.Agent;
import com.enterprise.aiportal.mapper.AgentMapper;
import com.enterprise.aiportal.service.DifyProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DifyProxyService difyProxyService;
    private final AgentMapper agentMapper;

    @Value("${dify.api.default-agent-id:1}")
    private Long defaultAgentId;

    public ChatController(DifyProxyService difyProxyService, AgentMapper agentMapper) {
        this.difyProxyService = difyProxyService;
        this.agentMapper = agentMapper;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.http.ResponseEntity<Flux<ServerSentEvent<String>>> streamChat(
            @RequestBody ChatRequest request,
            HttpServletRequest servletRequest) {

        Long userId = (Long) servletRequest.getAttribute("userId");
        // 如果 Security 配置正确，这里不会为 null，做兜底处理
        String currentUserId = userId != null ? String.valueOf(userId) : "anonymous";

        Agent targetAgent = resolveAgent(request.getAgentId());
        if (targetAgent == null) {
            return org.springframework.http.ResponseEntity.status(404).build();
        }

        String query = buildQueryWithLocalHistoryFallback(request);

        boolean explicitAgent = hasExplicitAgentId(request.getAgentId());
        System.out.println(explicitAgent
                ? ">>> 当前智能体: id=" + targetAgent.getId() + ", name=" + targetAgent.getAgentName() + ", difyConversationId=" + request.getConversationId()
                : ">>> 纯对话默认通道: agentId=" + targetAgent.getId() + ", name=" + targetAgent.getAgentName() + ", difyConversationId=" + request.getConversationId());

        // 组装 inputs：把智能体 prompt 作为上下文注入（Dify 应用可在 prompt 中引用 {{#start.system_prompt#}} 等变量）
        Map<String, Object> inputs = new HashMap<>();
        if (targetAgent.getPrompt() != null && !targetAgent.getPrompt().trim().isEmpty()) {
            inputs.put("system_prompt", targetAgent.getPrompt().trim());
        }

        Flux<ServerSentEvent<String>> stream = difyProxyService.streamChat(
                query,
                currentUserId,
                request.getConversationId(),
                targetAgent.getDifyApiKey(),
                inputs
        );

        return org.springframework.http.ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache")
                .body(stream);
    }

    private Agent resolveAgent(String agentId) {
        if (hasExplicitAgentId(agentId)) {
            Agent agent = agentMapper.selectById(agentId);
            if (agent != null) {
                return agent;
            }
        }

        Agent defaultAgent = agentMapper.selectById(defaultAgentId);
        if (defaultAgent != null) {
            return defaultAgent;
        }

        List<Agent> allAgents = agentMapper.selectList(
                new LambdaQueryWrapper<Agent>()
                        .orderByAsc(Agent::getCreatedAt)
                        .last("LIMIT 1")
        );
        return allAgents.isEmpty() ? null : allAgents.get(0);
    }

    private boolean hasExplicitAgentId(String agentId) {
        return agentId != null && !agentId.trim().isEmpty()
                && !"default-agent".equals(agentId)
                && !"test-agent-1".equals(agentId)
                && !"null".equalsIgnoreCase(agentId);
    }

    private String buildQueryWithLocalHistoryFallback(ChatRequest request) {
        String query = request.getQuery() == null ? "" : request.getQuery();
        boolean hasDifyConversationId = request.getConversationId() != null && !request.getConversationId().trim().isEmpty();
        if (hasDifyConversationId || request.getHistoryMessages() == null || request.getHistoryMessages().isEmpty()) {
            return query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是本地保存的历史对话上下文。请严格基于这些上下文继续回答，不要把本轮问题当成全新对话。\n\n");
        int count = 0;
        for (ChatRequest.HistoryMessage message : request.getHistoryMessages()) {
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            String role = "assistant".equalsIgnoreCase(message.getRole()) ? "助手" : "用户";
            sb.append(role).append("：").append(message.getContent().trim()).append("\n");
            count++;
            if (count >= 8) {
                break;
            }
        }
        sb.append("\n当前用户问题：").append(query);
        return sb.toString();
    }
}
