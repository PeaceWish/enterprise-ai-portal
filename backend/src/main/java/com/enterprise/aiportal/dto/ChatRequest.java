package com.enterprise.aiportal.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    /**
     * 用户输入的聊天内容
     */
    private String query;
    
    /**
     * 会话ID，首次对话为空，后续对话需携带以维持上下文
     */
    private String conversationId; 
    
    /**
     * 前端传来的目标助手ID（用于后期在数据库查出对应的 Dify API Key）
     */
    private String agentId;

    /**
     * 本地历史上下文兜底。
     * 正常情况下 Dify 通过 conversationId 自行维持上下文；当早期历史记录缺少
     * difyConversationId 时，前端会附带最近几轮本地消息，后端将其拼进 query，
     * 避免“历史续聊像新对话一样从 0 开始”。
     */
    private List<HistoryMessage> historyMessages;

    @Data
    public static class HistoryMessage {
        private String role;
        private String content;
    }
}
