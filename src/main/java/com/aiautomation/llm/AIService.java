package com.aiautomation.llm;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.ChatMessage;
import com.aiautomation.entity.ChatSession;
import com.aiautomation.entity.User;
import com.aiautomation.entity.AutomationExecution;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.AutomationExecutionRepository;
import com.aiautomation.repository.ChatMessageRepository;
import com.aiautomation.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final LLMService llmService;
    private final ConversationMemory conversationMemory;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AutomationExecutionRepository automationExecutionRepository;
    private final LLMAgentResponseParser responseParser;

    public ChatSession getOrCreateActiveSession(User user, UUID sessionId) {
        if (sessionId != null) {
            return chatSessionRepository.findById(sessionId).orElseGet(() -> createNewSession(user, "New Chat"));
        }
        List<ChatSession> sessions = chatSessionRepository.findByUserOrderByUpdatedAtDesc(user);
        if (!sessions.isEmpty()) {
            return sessions.get(0);
        }
        return createNewSession(user, "General Conversation");
    }

    public ChatSession createNewSession(User user, String title) {
        ChatSession session = ChatSession.builder()
                .user(user)
                .title((title != null && !title.isBlank()) ? title : "New Chat")
                .selectedModel(user.getAiModel() != null ? user.getAiModel() : "qwen2.5-coder:14b")
                .build();
        return chatSessionRepository.save(session);
    }

    public String processUserMessage(User user, ChatSession session, String userPrompt) {
        return processUserMessageWithDisplay(user, session, userPrompt, userPrompt);
    }

    public String processUserMessageWithDisplay(User user, ChatSession session, String llmPrompt, String displayContent) {
        // Save clean display content to user message DB
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .sender("USER")
                .content((displayContent != null && !displayContent.isBlank()) ? displayContent : llmPrompt)
                .build();
        chatMessageRepository.save(userMsg);

        // Build context memory
        List<OllamaClient.Message> messages = conversationMemory.buildMessageContext(session);
        if (!messages.isEmpty() && "user".equals(messages.get(messages.size() - 1).getRole())) {
            messages.get(messages.size() - 1).setContent(llmPrompt);
        }

        // Get AI response
        String rawResponse = llmService.generateResponse(session.getSelectedModel(), messages);
        String jsonResponse = responseParser.sanitizeAndValidateJsonResponse(rawResponse);

        // Save AI message
        ChatMessage aiMsg = ChatMessage.builder()
                .session(session)
                .sender("AI")
                .content(jsonResponse)
                .build();
        chatMessageRepository.save(aiMsg);

        // Record AutomationExecution if proposal requires confirmation
        try {
            if (jsonResponse.contains("\"confirmation\"") && jsonResponse.contains("\"action\"")) {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonResponse);
                if (root.has("action")) {
                    String actionType = root.get("action").asText();
                    String payloadJson = root.has("data") ? root.get("data").toString() : "{}";

                    automationExecutionRepository.save(AutomationExecution.builder()
                            .user(user)
                            .messageId(aiMsg.getId().toString())
                            .actionType(actionType.toUpperCase())
                            .status(AutomationExecution.Status.PENDING_CONFIRMATION)
                            .payloadJson(payloadJson)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to create AutomationExecution tracking record", e);
        }

        // Save activity log
        activityLogRepository.save(ActivityLog.builder()
                .user(user)
                .actionType("CHAT")
                .description("Prompted AI Agent with model: " + session.getSelectedModel())
                .build());

        return jsonResponse;
    }
}
