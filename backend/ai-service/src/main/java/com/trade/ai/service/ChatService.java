package com.trade.ai.service;

import com.trade.ai.dto.ChatRequest;
import com.trade.ai.dto.ChatResponse;
import com.trade.ai.dto.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 对话服务
 * <p>
 * 使用 Spring AI + OpenRouter 接入多模型 LLM
 * 支持多轮对话（通过 Redis 存储会话历史）
 * </p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ChatSessionService sessionService;

    public ChatService(ChatClient.Builder chatClientBuilder, ChatSessionService sessionService) {
        this.chatClient = chatClientBuilder.build();
        this.sessionService = sessionService;
    }

    /**
     * 处理用户消息，支持多轮对话
     */
    public ChatResponse chat(ChatRequest request) {
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : java.util.UUID.randomUUID().toString();

        log.info("[AI] sessionId={}, message={}", sessionId, request.message());

        try {
            // 1. 获取或创建会话（含历史消息）
            ChatSession session = sessionService.getOrCreateSession(sessionId);

            // 2. 添加用户新消息
            session.addMessage(ChatSession.Role.USER, request.message());

            // 3. 将 ChatSession.Message 转为 Spring AI Message
            List<Message> aiMessages = session.getMessages().stream()
                    .map(msg -> switch (msg.role()) {
                        case SYSTEM -> (Message) new SystemMessage(msg.content());
                        case USER -> (Message) new UserMessage(msg.content());
                        case ASSISTANT -> (Message) new AssistantMessage(msg.content());
                    })
                    .collect(Collectors.toList());

            // 4. 调用 LLM
            String content = chatClient.prompt()
                    .messages(aiMessages)
                    .call()
                    .content();

            // 5. 将 AI 回复加入会话历史
            session.addMessage(ChatSession.Role.ASSISTANT, content);

            // 6. 保存会话到 Redis
            sessionService.saveSession(session);

            log.info("[AI] sessionId={}, 响应成功, 历史消息数={}", sessionId, session.size());

            return new ChatResponse(content, "tencent/hy3-preview:free", null, sessionId);

        } catch (Exception e) {
            log.error("[AI] sessionId={}, 调用失败: {}", sessionId, e.getMessage(), e);
            return new ChatResponse(
                    "抱歉，AI 服务暂时不可用，请稍后重试。错误信息：" + e.getMessage(),
                    "unknown",
                    null,
                    sessionId
            );
        }
    }

    /**
     * 清空指定会话的历史
     */
    public void clearSession(String sessionId) {
        sessionService.deleteSession(sessionId);
    }
}
