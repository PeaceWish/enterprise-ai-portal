package com.enterprise.aiportal.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuVO> menus;
}
