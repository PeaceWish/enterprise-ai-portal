package com.enterprise.aiportal.dto.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OA 组织同步请求 DTO
 */
@Data
public class OrgSyncDTO {

    @JsonProperty("LIST")
    private List<OrgItem> list;

    @Data
    public static class OrgItem {
        @JsonProperty("ZZJG_JC")
        private OrgInfo zzjgJc;
    }

    @Data
    public static class OrgInfo {
        @JsonProperty("NORGANIZATION_CODE")
        private String organizationCode;

        @JsonProperty("ORGANIZATION_NAME")
        private String organizationName;

        @JsonProperty("DATASTATUS")
        private String dataStatus;
    }
}
