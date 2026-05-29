package com.enterprise.aiportal.controller;

import com.enterprise.aiportal.dto.Result;
import com.enterprise.aiportal.dto.sync.OrgSyncDTO;
import com.enterprise.aiportal.dto.sync.UserSyncDTO;
import com.enterprise.aiportal.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * 接收 OA 组织机构同步推送
     */
    @PostMapping("/org")
    public Result<String> syncOrganizations(@RequestBody OrgSyncDTO dto) {
        log.info("收到组织同步请求，数据条数: {}", dto.getList() != null ? dto.getList().size() : 0);
        syncService.syncOrganizations(dto);
        return Result.ok("同步成功");
    }

    /**
     * 接收 OA 人员档案同步推送
     */
    @PostMapping("/user")
    public Result<String> syncUsers(@RequestBody UserSyncDTO dto) {
        int totalAccounts = 0;
        if (dto.getList() != null) {
            for (UserSyncDTO.UserItem item : dto.getList()) {
                if (item.getRyJc() != null && item.getRyJc().getChildModels() != null) {
                    totalAccounts += item.getRyJc().getChildModels().size();
                }
            }
        }
        log.info("收到人员同步请求，人员条数: {}, 账号总数: {}",
                dto.getList() != null ? dto.getList().size() : 0, totalAccounts);

        syncService.syncUsers(dto);
        return Result.ok("同步成功");
    }
}
