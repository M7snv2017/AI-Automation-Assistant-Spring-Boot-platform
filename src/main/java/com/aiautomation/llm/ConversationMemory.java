package com.aiautomation.llm;

import com.aiautomation.entity.ChatMessage;
import com.aiautomation.entity.ChatSession;
import com.aiautomation.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationMemory {

    private final ChatMessageRepository chatMessageRepository;
    private final PromptBuilder promptBuilder;

    public List<OllamaClient.Message> buildMessageContext(ChatSession session) {
        List<OllamaClient.Message> messages = new ArrayList<>();

        // Add System Prompt
        messages.add(OllamaClient.Message.builder()
                .role("system")
                .content(promptBuilder.buildSystemPrompt(session.getUser()))
                .build());

        // Fetch recent messages in ascending order
        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByTimestampAsc(session);

        // Sliding window: keep max last 10 messages to prevent context overflow
        if (history.size() > 10) {
            history = history.subList(history.size() - 10, history.size());
        }

        for (ChatMessage msg : history) {
            String role = switch (msg.getSender().toUpperCase()) {
                case "USER" -> "user";
                case "AI", "ASSISTANT" -> "assistant";
                default -> "system";
            };
            messages.add(OllamaClient.Message.builder()
                    .role(role)
                    .content(msg.getContent())
                    .build());
        }

        return messages;
    }
}
