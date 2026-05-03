package com.trade.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.ai.dto.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 对话会话服务
 * <p>
 * 使用 Redis 存储会话历史，支持多轮对话上下文
 * Key 格式：ai:chat:session:{sessionId}
 * Value：JSON 序列化的消息列表
 * </p>
 */
@Service
public class ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private static final String SESSION_KEY_PREFIX = "ai:chat:session:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxHistorySize;
    private final long sessionTtlSeconds;

    public ChatSessionService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${ai.chat.max-history-size:20}") int maxHistorySize,
            @Value("${ai.chat.session-ttl-seconds:1800}") long sessionTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxHistorySize = maxHistorySize;
        this.sessionTtlSeconds = sessionTtlSeconds;
    }

    /**
     * 获取会话（如不存在则创建新会话，含 system prompt）
     */
    public ChatSession getOrCreateSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        Object cached = redisTemplate.opsForValue().get(key);

        ChatSession session;
        if (cached != null) {
            try {
                // 从 Redis 反序列化
                @SuppressWarnings("unchecked")
                List<ChatSession.Message> messages =
                        objectMapper.readValue(cached.toString(), new TypeReference<>() {});
                session = new ChatSession(sessionId);
                for (ChatSession.Message msg : messages) {
                    session.addMessage(msg.role(), msg.content());
                }
                log.debug("[Session] 从 Redis 加载会话 sessionId={}, 消息数={}", sessionId, session.size());
            } catch (Exception e) {
                log.warn("[Session] Redis 反序列化失败，新建会话 sessionId={}", sessionId, e);
                session = createNewSession(sessionId);
            }
        } else {
            session = createNewSession(sessionId);
        }

        // 刷新 TTL
        redisTemplate.expire(key, sessionTtlSeconds, TimeUnit.SECONDS);
        return session;
    }

    /**
     * 保存会话到 Redis
     */
    public void saveSession(ChatSession session) {
        String key = SESSION_KEY_PREFIX + session.getSessionId();
        try {
            // 截断过长的历史
            session.trimToMaxSize(maxHistorySize);

            // 序列化为 JSON
            String json = objectMapper.writeValueAsString(session.getMessages());
            redisTemplate.opsForValue().set(key, json, sessionTtlSeconds, TimeUnit.SECONDS);
            log.debug("[Session] 保存会话 sessionId={}, 消息数={}", session.getSessionId(), session.size());
        } catch (Exception e) {
            log.error("[Session] 保存会话失败 sessionId={}", session.getSessionId(), e);
        }
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.info("[Session] 删除会话 sessionId={}", sessionId);
    }

    /**
     * 创建新会话，加入 system prompt
     */
    private ChatSession createNewSession(String sessionId) {
        ChatSession session = new ChatSession(sessionId);
        // 加入系统提示词，定义 AI 角色和行为
        session.addMessage(
                ChatSession.Role.SYSTEM,
                "你是一个智能金融助手，专注于帮助用户了解金融交易系统。" +
                        "请用简洁、专业、友好的中文回答问题。" +
                        "如果问题超出金融/交易领域，请礼貌告知用户。"
        );
        log.info("[Session] 创建新会话 sessionId={}", sessionId);
        return session;
    }
}
