package com.enterprise.aiportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Service
public class DifyProxyService {

    @Value("${dify.api.base-url}")
    private String difyBaseUrl;

    private final WebClient webClient;

    public DifyProxyService(WebClient.Builder webClientBuilder) {
        // 配置 WebClient
        this.webClient = webClientBuilder.build();
    }

    /**
     * 向 Dify 发起流式对话请求，并返回 SSE 流
     */
    public Flux<ServerSentEvent<String>> streamChat(String query, String user, String conversationId, String apiKey) {
        return streamChat(query, user, conversationId, apiKey, null);
    }

    public Flux<ServerSentEvent<String>> streamChat(String query, String user, String conversationId, String apiKey, Map<String, Object> inputs) {
        System.out.println(">>> 进入 DifyProxyService.streamChat, query: " + query);
        System.out.println(">>> Dify Base URL: " + difyBaseUrl);
        
        // 组装 Dify 要求的标准请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", inputs != null ? inputs : new HashMap<>());
        requestBody.put("query", query);
        requestBody.put("response_mode", "streaming");
        requestBody.put("user", user); 
        
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            requestBody.put("conversation_id", conversationId);
        }

        ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<>() {};

        return webClient.post()
                .uri(difyBaseUrl + "/v1/chat-messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), response -> 
                    response.bodyToMono(String.class).flatMap(errorBody -> {
                        System.err.println("!!! Dify 详细报错回传: " + errorBody);
                        return reactor.core.publisher.Mono.error(new RuntimeException(errorBody));
                    })
                )
                .bodyToFlux(type)
                .doOnNext(event -> System.out.println("<<< 收到 Dify 事件: " + event.data()))
                .doOnError(e -> System.err.println("!!! Dify 流出错: " + e.getMessage()))
                .onErrorResume(e -> {
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("{\"event\": \"error\", \"message\": \"无法连接到底层 AI 服务: " + e.getMessage() + "\"}")
                            .build());
                });
    }
}
