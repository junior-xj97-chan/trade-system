package com.trade.ai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话会话，封装会话 ID + 消息历史
 */
public class ChatSession {

    /**
     * 消息角色
     */
    public enum Role {
        SYSTEM, USER, ASSISTANT
    }

    /**
     * 单条消息
     */
    public record Message(Role role, String content) {
    }

    private final String sessionId;
    private final List<Message> messages = new ArrayList<>();

    public ChatSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void addMessage(Role role, String content) {
        messages.add(new Message(role, content));
    }

    public void trimToMaxSize(int maxSize) {
        if (messages.size() > maxSize) {
            int from = messages.size() - maxSize;
            // 保留最近 maxSize 条
            for (int i = 0; i < from; i++) {
                messages.remove(0);
            }
        }
    }

    public int size() {
        return messages.size();
    }
}
