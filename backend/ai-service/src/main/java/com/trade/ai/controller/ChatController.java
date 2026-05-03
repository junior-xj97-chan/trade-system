package com.trade.ai.controller;

import com.trade.ai.dto.ChatRequest;
import com.trade.ai.dto.ChatResponse;
import com.trade.ai.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 对话接口
 * <p>
 * RESTful 接口，供前端或其他微服务调用 LLM 对话能力
 * </p>
 */
@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * POST /ai/chat
     * 发送消息给 AI，返回回复
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.debug("收到对话请求: {}", request.message());
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /ai/models
     * 查询当前支持的模型列表（可扩展为从 OpenRouter API 拉取）
     */
    @GetMapping("/models")
    public ResponseEntity<Object> listModels() {
        return ResponseEntity.ok(new Object() {
            public final String defaultModel = "google/gemini-2.0-flash-exp:free";
            public final String[] available = {
                    "google/gemini-2.0-flash-exp:free",
                    "anthropic/claude-3-haiku:free",
                    "mistralai/mistral-7b-instruct:free",
                    "openai/chatgpt-4o-latest"
            };
        });
    }

    /**
     * GET /ai/health
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
