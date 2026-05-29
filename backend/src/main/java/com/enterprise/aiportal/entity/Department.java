package com.enterprise.aiportal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("departments")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String deptName;

    private String deptCode;

    private Long parentId;

    private String deptLeader;

    private Integer sortOrder;

    private Integer status;

    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
