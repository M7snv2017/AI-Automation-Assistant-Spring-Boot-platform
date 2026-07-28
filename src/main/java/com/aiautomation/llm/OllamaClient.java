package com.aiautomation.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OllamaClient {

    @Value("${ollama.host:http://localhost:11434}")
    private String ollamaHost;

    private final RestTemplate restTemplate;

    public OllamaClient() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);  // 30 seconds
        factory.setReadTimeout(180000);    // 3 minutes (180 seconds)
        this.restTemplate = new RestTemplate(factory);
    }

    @Data
    @Builder
    public static class Message {
        private String role; // system, user, assistant
        private String content;
    }

    @Data
    @Builder
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private boolean stream;
        private Map<String, Object> options;
    }

    @Data
    public static class ChatResponse {
        private String model;
        private Message message;

        @JsonProperty("done")
        private boolean done;
    }

    public String generateChatCompletion(String model, List<Message> messages) {
        String url = ollamaHost + "/api/chat";

        Map<String, Object> options = Map.of("num_ctx", 16384);

        ChatRequest requestPayload = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .stream(false)
                .options(options)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(requestPayload, headers);

        try {
            ResponseEntity<ChatResponse> response = restTemplate.postForEntity(url, entity, ChatResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getMessage() != null) {
                return response.getBody().getMessage().getContent();
            }
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            log.error("Ollama API HTTP error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getResponseBodyAsString().contains("context length")) {
                return "The prompt is longer than the context length available to the local AI model. Please start a new chat session or provide a shorter document excerpt.";
            }
            return "Ollama AI Engine returned HTTP Error " + ex.getStatusCode() + ". Please verify model availability.";
        } catch (Exception ex) {
            log.error("Error communicating with Ollama API at {}: {}", url, ex.getMessage());
            return "I am currently unable to connect to the local Ollama AI engine (" + ollamaHost + "). Please verify that Ollama is running.";
        }

        return "No response received from local AI model.";
    }

    @SuppressWarnings("unchecked")
    public List<String> getAvailableModels() {
        String url = ollamaHost + "/api/tags";
        List<String> models = new ArrayList<>();
        models.add("qwen2.5-coder:14b");
        models.add("qwen3:8b");
        models.add("deepseek-coder-v2");

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("models")) {
                List<Map<String, Object>> modelList = (List<Map<String, Object>>) response.getBody().get("models");
                if (modelList != null && !modelList.isEmpty()) {
                    List<String> fetched = new ArrayList<>();
                    for (Map<String, Object> m : modelList) {
                        if (m.containsKey("name")) {
                            fetched.add((String) m.get("name"));
                        }
                    }
                    if (!fetched.isEmpty()) return fetched;
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch live model list from Ollama: {}", ex.getMessage());
        }

        return models;
    }
}
