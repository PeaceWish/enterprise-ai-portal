package com.enterprise.aiportal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_conversations")
public class ChatConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String title;
    private String difyConversationId;
    private Long agentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}