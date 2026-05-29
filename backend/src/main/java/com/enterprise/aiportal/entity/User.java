package com.enterprise.aiportal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String role;

    private Long deptId;

    private String userCode;

    private String phone;

    private String email;

    private String avatar;

    private Integer status;

    private String source;

    private String idafeLoginId;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 非持久化字段
    @TableField(exist = false)
    private List<SysRole> roles;

    @TableField(exist = false)
    private List<String> permissions;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private List<Long> roleIds;
}
