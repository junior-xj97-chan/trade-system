package com.trade.ai.service;

import com.trade.ai.dto.ChatRequest;
import com.trade.ai.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * AI 对话服务
 * <p>
 * 使用 Spring AI + OpenRouter 接入多模型 LLM
 * </p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        // 构建 ChatClient，添加日志拦截器便于调试
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 处理用户消息，返回 AI 回复
     *
     * @param request 对话请求
     * @return AI 回复
     */
    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        String model = request.model() != null ? request.model() : "default";

        log.info("[AI] sessionId={}, model={}, message={}", sessionId, model, request.message());

        try {
            // 通过 OpenRouter 调用 LLM
            String content = chatClient.prompt()
                    .user(request.message())
                    .call()
                    .content();

            log.info("[AI] sessionId={}, 响应成功, content长度={}", sessionId, content.length());

            return new ChatResponse(content, model, null, sessionId);

        } catch (Exception e) {
            log.error("[AI] sessionId={}, 调用失败: {}", sessionId, e.getMessage(), e);
            return new ChatResponse(
                    "抱歉，AI 服务暂时不可用，请稍后重试。错误信息：" + e.getMessage(),
                    model,
                    null,
                    sessionId
            );
        }
    }
}
