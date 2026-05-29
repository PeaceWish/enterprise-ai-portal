package com.enterprise.aiportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
public class OaClientService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${oa.sso.client-id}")
    private String clientId;

    @Value("${oa.sso.client-secret}")
    private String clientSecret;

    public OaClientService(@Value("${oa.base-url}") String oaBaseUrl, ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(oaBaseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 OA 接口解密 ticket，获取用户名
     * 非平台生成系统使用 /mdm/register/getDecrypt
     */
    public String decryptTicket(String ticket) {
        try {
            log.info("调用 OA ticket 解密接口，clientId={}, ticket={}", clientId, ticket);
            String response = webClient.get()
                    .uri("/mdm/register/getDecrypt")
                    .header("clientId", clientId)
                    .header("ticket", ticket)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, res ->
                            res.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("OA 解密失败: " + body)))
                    )
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode meta = root.path("meta");
            boolean success = meta.path("success").asBoolean(false);

            if (!success) {
                String message = meta.path("message").asText("未知错误");
                throw new RuntimeException("OA 解密失败: " + message);
            }

            String username = root.path("data").asText();
            if (username == null || username.isBlank()) {
                throw new RuntimeException("OA 返回用户名为空");
            }

            log.info("OA ticket 解密成功，用户名: {}", username);
            return username;

        } catch (Exception e) {
            log.error("OA ticket 解密异常: {}", e.getMessage());
            throw new RuntimeException("OA 单点登录验证失败: " + e.getMessage());
        }
    }

    /**
     * 构造 Basic Auth Authorization 头
     * 格式: Basic Base64(clientId:secret)
     */
    public String buildAuthorization() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
