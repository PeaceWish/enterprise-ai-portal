package com.enterprise.aiportal.dto.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OA 人员档案同步请求 DTO
 */
@Data
public class UserSyncDTO {

    @JsonProperty("LIST")
    private List<UserItem> list;

    @Data
    public static class UserItem {
        @JsonProperty("RY_JC")
        private UserInfo ryJc;
    }

    @Data
    public static class UserInfo {
        @JsonProperty("REFFIELD")
        private String refField;  // 人员编码（工号）

        @JsonProperty("CHILDMODELS_1")
        private List<AccountItem> childModels;
    }

    @Data
    public static class AccountItem {
        @JsonProperty("ZH")
        private AccountInfo zh;
    }

    @Data
    public static class AccountInfo {
        @JsonProperty("USERNAME")
        private String userName;  // 姓名

        @JsonProperty("ACCOUNT")
        private String account;   // 登录账号（唯一标识）

        @JsonProperty("ACCOUNT_STATUS")
        private String accountStatus;  // 0-启用 1-停用

        @JsonProperty("ACC_NUM_SEC")
        private String accNumSec;  // 账号密级

        @JsonProperty("ORGANIZATION_CODE")
        private String organizationCode;  // 所属部门编码

        @JsonProperty("ORGANIZATION_NAME")
        private String organizationName;  // 所属部门名称
    }
}
