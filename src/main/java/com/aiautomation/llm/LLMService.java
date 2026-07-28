package com.aiautomation.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LLMService {

    private final OllamaClient ollamaClient;

    public String generateResponse(String model, List<OllamaClient.Message> messages) {
        if (model == null || model.isBlank()) {
            model = "qwen2.5-coder:14b";
        }
        return ollamaClient.generateChatCompletion(model, messages);
    }

    public List<String> getAvailableModels() {
        return ollamaClient.getAvailableModels();
    }
}
