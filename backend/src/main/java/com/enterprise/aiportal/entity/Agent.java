package com.enterprise.aiportal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agents")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String agentName;

    private String difyAppId;

    private String difyApiKey;

    private String description;

    private String prompt;

    private String iconUrl;

    private Integer status;

    private String visibility;

    private String allowedRoles;

    private String allowedDepts;

    private String allowedUsers;

    private Long creatorId;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
