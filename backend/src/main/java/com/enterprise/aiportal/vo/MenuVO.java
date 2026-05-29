package com.enterprise.aiportal.vo;

import lombok.Data;

import java.util.List;

@Data
public class MenuVO {
    private Long id;
    private String permCode;
    private String permName;
    private String permType;
    private Long parentId;
    private String path;
    private String icon;
    private String component;
    private Integer sortOrder;
    private List<MenuVO> children;
}
