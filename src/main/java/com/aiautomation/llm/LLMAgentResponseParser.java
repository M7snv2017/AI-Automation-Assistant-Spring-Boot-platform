package com.aiautomation.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LLMAgentResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String sanitizeAndValidateJsonResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return fallbackMessageJson("No response received from AI model.");
        }

        String cleaned = rawResponse.trim();
        if (cleaned.contains("```")) {
            cleaned = cleaned.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "").trim();
        }

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1).trim();
        }

        try {
            JsonNode node = objectMapper.readTree(cleaned);
            if (node.isObject()) {
                ObjectNode obj = (ObjectNode) node;
                if (obj.has("action") || obj.has("tasks") || obj.has("recipient")) {
                    obj.put("type", "confirmation");
                    if (!obj.has("message")) {
                        obj.put("message", "I have prepared the action for your approval.");
                    }
                    return objectMapper.writeValueAsString(obj);
                }

                String textContent = null;
                if (obj.has("message")) textContent = obj.get("message").asText();
                else if (obj.has("content")) textContent = obj.get("content").asText();
                else if (obj.has("response")) textContent = obj.get("response").asText();
                else if (obj.has("text")) textContent = obj.get("text").asText();
                else if (obj.has("answer")) textContent = obj.get("answer").asText();

                if (textContent != null) {
                    String parsedEmailAction = attemptParsePlainTextEmail(textContent);
                    if (parsedEmailAction != null) return parsedEmailAction;

                    ObjectNode msgObj = objectMapper.createObjectNode();
                    msgObj.put("type", "message");
                    msgObj.put("message", textContent);
                    return objectMapper.writeValueAsString(msgObj);
                }

                if (obj.has("type")) {
                    return objectMapper.writeValueAsString(obj);
                }
            }
        } catch (Exception e) {
            log.warn("AI model did not return pure JSON. Raw response: {}", rawResponse);
        }

        String parsedEmailAction = attemptParsePlainTextEmail(rawResponse);
        if (parsedEmailAction != null) return parsedEmailAction;

        return fallbackMessageJson(rawResponse);
    }

    private String attemptParsePlainTextEmail(String raw) {
        if (raw == null || raw.isBlank()) return null;

        // Check if raw text looks like an email draft (contains Subject: or Dear or Job Recommendations)
        boolean hasSubject = raw.contains("Subject:");
        boolean hasSalutation = raw.toLowerCase().contains("dear ") || raw.toLowerCase().contains("hello ") || raw.toLowerCase().contains("hi ");
        boolean hasSignature = raw.toLowerCase().contains("best regards") || raw.toLowerCase().contains("sincerely") || raw.toLowerCase().contains("regards,");

        if (hasSubject || (hasSalutation && hasSignature)) {
            String subject = "Job Recommendations & Career Match";
            String body = raw.trim();

            if (hasSubject) {
                int subjIndex = raw.indexOf("Subject:");
                int lineEnd = raw.indexOf("\n", subjIndex);
                if (lineEnd > subjIndex) {
                    subject = raw.substring(subjIndex + 8, lineEnd).replace("---", "").trim();
                    body = raw.substring(lineEnd + 1).replace("---", "").trim();
                }
            }

            // Remove any trailing tutorial advice like "Feel free to customize..."
            int feelFreeIdx = body.toLowerCase().indexOf("feel free to customize");
            if (feelFreeIdx > 0) {
                body = body.substring(0, feelFreeIdx).trim();
            }

            try {
                ObjectNode root = objectMapper.createObjectNode();
                root.put("type", "confirmation");
                root.put("action", "send_email");
                root.put("message", "I analyzed the CV and prepared the email draft with job recommendations. Please review and confirm to send.");

                ObjectNode data = objectMapper.createObjectNode();
                data.put("recipient", "me");
                data.put("subject", subject);
                data.put("body", body);
                root.set("data", data);

                return objectMapper.writeValueAsString(root);
            } catch (Exception e) {
                log.warn("Failed to build fallback send_email action card", e);
            }
        }
        return null;
    }

    private String fallbackMessageJson(String messageText) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "message");
            root.put("message", messageText);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"type\":\"message\",\"message\":\"" + messageText.replace("\"", "\\\"") + "\"}";
        }
    }
}
