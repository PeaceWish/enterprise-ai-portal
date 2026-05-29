package com.enterprise.aiportal.controller;

import com.enterprise.aiportal.dto.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    private static final String QUICK_PROMPTS_KEY = "quick_prompts";
    private static final String CHAT_FOOTER_HINT_KEY = "chat_footer_hint";
    private static final List<Map<String, String>> DEFAULT_PROMPTS = List.of(
            Map.of("text", "帮我写一份周报"),
            Map.of("text", "解释这段代码"),
            Map.of("text", "翻译这段文字"),
            Map.of("text", "帮我制定计划")
    );
    private static final String DEFAULT_FOOTER_HINT = "AI 可能犯错，请核实重要信息。";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AppConfigController(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/ui")
    public Result<Map<String, Object>> getUiConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("quickPrompts", getQuickPrompts());
        result.put("chatFooterHint", getStringValue(CHAT_FOOTER_HINT_KEY, DEFAULT_FOOTER_HINT));
        return Result.ok(result);
    }

    @PostMapping("/ui")
    @PreAuthorize("hasAuthority('admin:prompts')")
    public Result<String> saveUiConfig(@RequestBody Map<String, Object> req) {
        Object prompts = req.get("quickPrompts");
        Object hint = req.get("chatFooterHint");
        try {
            if (prompts != null) {
                upsert(QUICK_PROMPTS_KEY, objectMapper.writeValueAsString(prompts));
            }
            if (hint != null) {
                upsert(CHAT_FOOTER_HINT_KEY, String.valueOf(hint));
            }
            return Result.ok("保存成功");
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    private List<Map<String, String>> getQuickPrompts() {
        String raw = getStringValue(QUICK_PROMPTS_KEY, null);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PROMPTS;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return DEFAULT_PROMPTS;
        }
    }

    private String getStringValue(String key, String defaultValue) {
        List<String> values = jdbcTemplate.query(
                "SELECT config_value FROM app_config WHERE config_key = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("config_value"),
                key
        );
        return values.isEmpty() ? defaultValue : values.get(0);
    }

    private void upsert(String key, String value) {
        int updated = jdbcTemplate.update(
                "UPDATE app_config SET config_value = ?, updated_at = CURRENT_TIMESTAMP WHERE config_key = ?",
                value, key
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO app_config (config_key, config_value) VALUES (?, ?)",
                    key, value
            );
        }
    }
}
