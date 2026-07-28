package com.aiautomation.controller;

import com.aiautomation.entity.ChatMessage;
import com.aiautomation.entity.ChatSession;
import com.aiautomation.entity.User;
import com.aiautomation.llm.AIService;
import com.aiautomation.llm.LLMService;
import com.aiautomation.repository.ChatMessageRepository;
import com.aiautomation.repository.ChatSessionRepository;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.automation.EmailService;
import com.aiautomation.service.TaskService;
import com.aiautomation.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import com.aiautomation.entity.AutomationExecution;
import com.aiautomation.repository.AutomationExecutionRepository;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final AIService aiService;
    private final LLMService llmService;
    private final UserService userService;
    private final TaskService taskService;
    private final EmailService emailService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AutomationExecutionRepository automationExecutionRepository;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @GetMapping("/chat")
    public String chatPage(@RequestParam(value = "sessionId", required = false) UUID sessionId,
                           @AuthenticationPrincipal CustomUserDetails customUserDetails,
                           Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        List<ChatSession> sessions = chatSessionRepository.findByUserOrderByUpdatedAtDesc(user);
        ChatSession activeSession = aiService.getOrCreateActiveSession(user, sessionId);

        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByTimestampAsc(activeSession);
        List<String> availableModels = llmService.getAvailableModels();

        model.addAttribute("user", user);
        model.addAttribute("sessions", sessions);
        model.addAttribute("activeSession", activeSession);
        model.addAttribute("messages", messages);
        model.addAttribute("availableModels", availableModels);

        return "chat";
    }

    @PostMapping("/api/chat/session/new")
    public String createSession(@RequestParam(value = "title", required = false) String title,
                                @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        ChatSession newSession = aiService.createNewSession(user, title);
        return "redirect:/chat?sessionId=" + newSession.getId();
    }

    @PostMapping("/api/chat/session/delete/{id}")
    public String deleteSession(@PathVariable("id") UUID id,
                                @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        try {
            ChatSession session = chatSessionRepository.findById(id).orElse(null);
            if (session != null && session.getUser().getId().equals(user.getId())) {
                chatSessionRepository.delete(session);
            }
        } catch (Exception ignored) {}
        return "redirect:/chat";
    }

    @PostMapping("/api/chat/session/model")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSessionModel(@RequestParam("sessionId") String sessionId,
                                                                 @RequestParam("model") String model,
                                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        try {
            ChatSession session = chatSessionRepository.findById(UUID.fromString(sessionId)).orElse(null);
            if (session != null && session.getUser().getId().equals(user.getId())) {
                session.setSelectedModel(model);
                chatSessionRepository.save(session);
                user.setAiModel(model);
                userService.saveUser(user);
            }
        } catch (Exception ignored) {}
        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/api/chat/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadChatFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                              @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Map<String, Object> result = new HashMap<>();
        if (file == null || file.isEmpty()) {
            result.put("status", "error");
            result.put("message", "Uploaded file is empty.");
            return ResponseEntity.badRequest().body(result);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "document.txt";
        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        Set<String> allowedExtensions = Set.of("txt", "csv", "json", "pdf", "doc", "docx", "md", "log", "xml", "html");
        if (!allowedExtensions.contains(ext)) {
            result.put("status", "error");
            result.put("message", "Unsupported file format (." + ext + "). Supported formats: TXT, PDF, CSV, JSON, DOC, DOCX, MD, LOG.");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            String content;
            if (ext.equals("pdf")) {
                content = extractTextFromPdf(file.getBytes(), originalFilename);
            } else if (ext.equals("doc") || ext.equals("docx")) {
                content = extractTextFromDoc(file.getBytes(), originalFilename);
            } else {
                content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }

            if (content != null && content.length() > 8000) {
                content = content.substring(0, 8000) + "\n... [Document Content Truncated at 8000 characters]";
            }

            result.put("status", "success");
            result.put("fileName", originalFilename);
            result.put("fileSize", file.getSize());
            result.put("fileType", ext);
            result.put("content", content);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Failed to process file: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    private String extractTextFromPdf(byte[] bytes, String filename) {
        StringBuilder textResult = new StringBuilder();
        try {
            String pdfString = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            int index = 0;
            while ((index = pdfString.indexOf("stream", index)) != -1) {
                int streamStart = index + 6;
                if (streamStart < pdfString.length() && pdfString.charAt(streamStart) == '\r') streamStart++;
                if (streamStart < pdfString.length() && pdfString.charAt(streamStart) == '\n') streamStart++;

                int streamEnd = pdfString.indexOf("endstream", streamStart);
                if (streamEnd == -1) break;

                byte[] streamBytes = java.util.Arrays.copyOfRange(bytes, streamStart, streamEnd);
                byte[] decompressed = null;
                try {
                    java.util.zip.Inflater inflater = new java.util.zip.Inflater();
                    inflater.setInput(streamBytes);
                    byte[] buffer = new byte[8192];
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    while (!inflater.finished()) {
                        int count = inflater.inflate(buffer);
                        if (count == 0 && inflater.needsInput()) break;
                        baos.write(buffer, 0, count);
                    }
                    inflater.end();
                    decompressed = baos.toByteArray();
                } catch (Exception e) {
                    decompressed = streamBytes;
                }

                if (decompressed != null && decompressed.length > 0) {
                    String streamText = new String(decompressed, java.nio.charset.StandardCharsets.ISO_8859_1);
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\(([^\\)\\\\]*(?:\\\\.[^\\)\\\\]*)*)\\)\\s*T[jJ]|\\[([^\\]]+)\\]\\s*TJ");
                    java.util.regex.Matcher m = p.matcher(streamText);
                    while (m.find()) {
                        String group = m.group(1) != null ? m.group(1) : m.group(2);
                        if (group != null) {
                            String clean = group.replaceAll("\\\\\\)", ")").replaceAll("\\\\\\(", "(").replaceAll("\\\\\\\\", "\\\\").replaceAll("<[^>]+>", " ");
                            clean = clean.replaceAll("[^\\x20-\\x7E\\s]", "");
                            if (clean.trim().length() > 0) {
                                textResult.append(clean.trim()).append(" ");
                            }
                        }
                    }
                }
                index = streamEnd + 9;
            }

            if (textResult.length() > 20) {
                return textResult.toString().replaceAll("\\s+", " ").trim();
            }
        } catch (Exception e) {}

        return extractTextFromRaw(bytes, filename);
    }

    private String extractTextFromDoc(byte[] bytes, String filename) {
        // Unzip docx (word/document.xml)
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    String xml = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    StringBuilder text = new StringBuilder();
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("<w:t[^>]*>(.*?)</w:t>");
                    java.util.regex.Matcher m = p.matcher(xml);
                    while (m.find()) {
                        text.append(m.group(1)).append(" ");
                    }
                    if (text.length() > 0) {
                        return text.toString().replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&").replaceAll("\\s+", " ").trim();
                    }
                }
            }
        } catch (Exception e) {}

        return extractTextFromRaw(bytes, filename);
    }

    private String extractTextFromRaw(byte[] bytes, String filename) {
        try {
            String raw = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            StringBuilder sb = new StringBuilder();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("[A-Za-z0-9\\s,\\.?!'\":;\\-\\/]{8,}");
            java.util.regex.Matcher m = p.matcher(raw);
            while (m.find()) {
                String cand = m.group().trim();
                if (!cand.contains("FlateDecode") && !cand.contains("StructTree") && !cand.contains("ViewerPreferences") 
                    && !cand.contains("Metadata") && !cand.contains("ExtGState") && !cand.contains("Font") 
                    && !cand.contains("MediaBox") && !cand.contains("xref") && !cand.contains("trailer")
                    && !cand.contains("startxref") && !cand.contains("obj") && !cand.contains("endobj")) {
                    sb.append(cand).append("\n");
                }
            }
            if (sb.length() > 20) {
                return sb.toString().trim();
            }
        } catch (Exception e) {}
        return "[Attached Document: " + filename + "]";
    }

    @Data
    public static class ChatSendMessageDto {
        private String sessionId;
        private String prompt;
        private String displayPrompt;
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody ChatSendMessageDto dto,
                                                           @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        UUID sId = null;
        if (dto.getSessionId() != null && !dto.getSessionId().isBlank()) {
            try {
                sId = UUID.fromString(dto.getSessionId());
            } catch (IllegalArgumentException ignored) {}
        }

        ChatSession session = aiService.getOrCreateActiveSession(user, sId);
        String displayPrompt = (dto.getDisplayPrompt() != null && !dto.getDisplayPrompt().isBlank()) 
                                ? dto.getDisplayPrompt() 
                                : dto.getPrompt();

        String aiResponse = aiService.processUserMessageWithDisplay(user, session, dto.getPrompt(), displayPrompt);

        String messageId = null;
        try {
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(aiResponse);
            if (node.has("messageId")) {
                messageId = node.get("messageId").asText();
            }
        } catch (Exception ignored) {}

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("sessionId", session.getId().toString());
        result.put("userPrompt", displayPrompt);
        result.put("aiResponse", aiResponse);
        result.put("messageId", messageId);

        return ResponseEntity.ok(result);
    }

    @Data
    public static class ActionConfirmDto {
        private String messageId;
        private String action; // create_tasks, send_email, create_reminder
        private String message;
        private Map<String, Object> data;
    }

    private void updateChatMessageActionStatus(User user, String messageIdStr, String newStatus, String resultMessage) {
        try {
            ChatMessage msg = null;
            if (messageIdStr != null && !messageIdStr.isBlank()) {
                try {
                    msg = chatMessageRepository.findById(UUID.fromString(messageIdStr)).orElse(null);
                } catch (Exception ignored) {}
            }

            if (msg == null) {
                List<AutomationExecution> pendingList = automationExecutionRepository.findByUserAndStatusOrderByCreatedAtDesc(user, AutomationExecution.Status.PENDING_CONFIRMATION);
                if (!pendingList.isEmpty()) {
                    AutomationExecution exec = pendingList.get(0);
                    try {
                        msg = chatMessageRepository.findById(UUID.fromString(exec.getMessageId())).orElse(null);
                    } catch (Exception ignored) {}
                }
            }

            if (msg != null && msg.getContent() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(msg.getContent());
                if (node.isObject()) {
                    com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
                    obj.put("actionStatus", newStatus);
                    if (resultMessage != null) {
                        obj.put("actionResultMessage", resultMessage);
                    }
                    msg.setContent(mapper.writeValueAsString(obj));
                    chatMessageRepository.save(msg);
                }

                // Sync AutomationExecution database entity
                try {
                    AutomationExecution exec = automationExecutionRepository.findByMessageId(msg.getId().toString()).orElse(null);
                    if (exec != null) {
                        if ("executed".equalsIgnoreCase(newStatus)) {
                            exec.setStatus(AutomationExecution.Status.EXECUTED);
                            exec.setExecutedAt(java.time.LocalDateTime.now());
                        } else if ("cancelled".equalsIgnoreCase(newStatus)) {
                            exec.setStatus(AutomationExecution.Status.CANCELLED);
                        }
                        if (resultMessage != null) exec.setExecutionResult(resultMessage);
                        automationExecutionRepository.save(exec);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Non-critical logging fallback
        }
    }

    private java.time.LocalDateTime parseDueDate(Object dueDateObj) {
        if (dueDateObj == null) return null;
        String str = dueDateObj.toString().trim();
        if (str.isEmpty()) return null;
        try {
            return java.time.LocalDateTime.parse(str);
        } catch (Exception e) {
            try {
                return java.time.LocalDate.parse(str).atTime(10, 0);
            } catch (Exception e2) {
                try {
                    String lower = str.toLowerCase();

                    // Parse relative minute expressions: e.g. "in 1 minute", "after 5 minutes", "10 minutes"
                    java.util.regex.Matcher mMin = java.util.regex.Pattern.compile("(?:in|after)?\\s*(\\d+)\\s*(?:min|minute|minutes|m)\\b").matcher(lower);
                    if (mMin.find()) {
                        int mins = Integer.parseInt(mMin.group(1));
                        return java.time.LocalDateTime.now().plusMinutes(mins);
                    }

                    // Parse relative hour expressions: e.g. "in 2 hours", "after 1 hour"
                    java.util.regex.Matcher mHour = java.util.regex.Pattern.compile("(?:in|after)?\\s*(\\d+)\\s*(?:hour|hours|h)\\b").matcher(lower);
                    if (mHour.find()) {
                        int hrs = Integer.parseInt(mHour.group(1));
                        return java.time.LocalDateTime.now().plusHours(hrs);
                    }

                    // Parse relative second expressions: e.g. "in 30 seconds"
                    java.util.regex.Matcher mSec = java.util.regex.Pattern.compile("(?:in|after)?\\s*(\\d+)\\s*(?:sec|second|seconds|s)\\b").matcher(lower);
                    if (mSec.find()) {
                        int secs = Integer.parseInt(mSec.group(1));
                        return java.time.LocalDateTime.now().plusSeconds(secs);
                    }

                    java.time.LocalDateTime baseDate = java.time.LocalDateTime.now();

                    // Determine target date
                    if (lower.contains("tomorrow")) {
                        baseDate = baseDate.plusDays(1);
                    } else if (!lower.contains("today")) {
                        // Check for days of week
                        boolean matchedDay = false;
                        for (int d = 1; d <= 7; d++) {
                            String dayName = baseDate.plusDays(d).getDayOfWeek().name().toLowerCase();
                            if (lower.contains(dayName)) {
                                baseDate = baseDate.plusDays(d);
                                matchedDay = true;
                                break;
                            }
                        }
                        if (!matchedDay && !lower.contains("am") && !lower.contains("pm") && !lower.contains(":")) {
                            return null;
                        }
                    }

                    // Extract hour & minute
                    int hour = 10;
                    int minute = 0;

                    java.util.regex.Matcher mTime = java.util.regex.Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").matcher(lower);
                    if (mTime.find()) {
                        int parsedHour = Integer.parseInt(mTime.group(1));
                        if (mTime.group(2) != null) {
                            minute = Integer.parseInt(mTime.group(2));
                        }
                        String ampm = mTime.group(3);
                        if ("pm".equals(ampm) && parsedHour < 12) {
                            parsedHour += 12;
                        } else if ("am".equals(ampm) && parsedHour == 12) {
                            parsedHour = 0;
                        } else if (ampm == null && parsedHour <= 12 && lower.contains("pm")) {
                            parsedHour += 12;
                        }
                        hour = parsedHour;
                    }

                    return baseDate.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @PostMapping("/api/chat/action/confirm")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmAction(@RequestBody ActionConfirmDto dto,
                                                             @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        Map<String, Object> result = new HashMap<>();

        try {
            if ("create_tasks".equalsIgnoreCase(dto.getAction())) {
                List<Map<String, Object>> tasks = (List<Map<String, Object>>) dto.getData().get("tasks");
                int createdCount = 0;
                if (tasks != null) {
                    for (Map<String, Object> t : tasks) {
                        String title = (String) t.getOrDefault("title", "New Task");
                        String category = (String) t.getOrDefault("category", "General Automation");
                        String desc = (String) t.getOrDefault("description", "");
                        String recurrence = (String) t.getOrDefault("recurrence", "ONCE");
                        String reminderOffset = (String) t.getOrDefault("reminderOffset", "NONE");

                        java.time.LocalDateTime due = parseDueDate(t.get("dueDate"));
                        if (due == null) due = parseDueDate(t.get("date"));
                        if (due == null) due = parseDueDate(t.get("time"));

                        taskService.createTask(user, title, desc, category, due, recurrence, reminderOffset);
                        createdCount++;
                    }
                }
                String msgText = "Executed successfully: Created " + createdCount + " task(s).";
                result.put("status", "success");
                result.put("message", msgText);

                updateChatMessageActionStatus(user, dto.getMessageId(), "executed", msgText);

            } else if ("send_email".equalsIgnoreCase(dto.getAction())) {
                String recipient = (String) dto.getData().get("recipient");
                String subject = (String) dto.getData().get("subject");
                String body = (String) dto.getData().get("body");

                String userEmail = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getGmailUsername();
                String userName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : user.getEmail();

                if (recipient == null || recipient.isBlank() || recipient.contains("{{") || recipient.contains("[") || "me".equalsIgnoreCase(recipient.trim()) || "myself".equalsIgnoreCase(recipient.trim())) {
                    recipient = userEmail;
                }

                if (body != null) {
                    body = body.replace("{{userEmail}}", userEmail)
                               .replace("{{userName}}", userName)
                               .replace("[Your Name]", userName)
                               .replace("[User Name]", userName)
                               .replace("[Your Email]", userEmail)
                               .replace("{{user_email}}", userEmail)
                               .replace("{{user_name}}", userName);
                }

                if (subject != null) {
                    subject = subject.replace("{{userEmail}}", userEmail)
                                     .replace("{{userName}}", userName)
                                     .replace("[Your Name]", userName)
                                     .replace("[User Name]", userName);
                }

                java.time.LocalDateTime due = parseDueDate(dto.getData().get("dueDate"));
                if (due == null) due = parseDueDate(dto.getData().get("date"));
                if (due == null) due = parseDueDate(dto.getData().get("time"));

                boolean isFutureScheduled = (due != null && due.isAfter(java.time.LocalDateTime.now().plusSeconds(10)));

                if (isFutureScheduled) {
                    String taskDesc = "Recipient: " + recipient + "\nSubject: " + (subject != null ? subject : "") + "\nBody: " + (body != null ? body : "");
                    taskService.createTask(user, "Scheduled Email: " + (subject != null ? subject : "Notification"), taskDesc, "Email Management", due);
                    
                    String msgText = "Executed successfully: Scheduled email to " + recipient + " for " + due.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                    result.put("status", "success");
                    result.put("message", msgText);
                    updateChatMessageActionStatus(user, dto.getMessageId(), "executed", msgText);
                } else {
                    emailService.sendEmail(user, recipient, subject, body);
                    String msgText = "Executed successfully: Email sent immediately to " + recipient;
                    result.put("status", "success");
                    result.put("message", msgText);
                    updateChatMessageActionStatus(user, dto.getMessageId(), "executed", msgText);
                }

            } else if ("create_reminder".equalsIgnoreCase(dto.getAction())) {
                String title = (String) dto.getData().getOrDefault("title", "Reminder");
                String desc = (String) dto.getData().getOrDefault("description", "Reminder created via AI Agent");
                String recurrence = (String) dto.getData().getOrDefault("recurrence", "ONCE");
                String reminderOffset = (String) dto.getData().getOrDefault("reminderOffset", "NONE");

                java.time.LocalDateTime due = parseDueDate(dto.getData().get("dueDate"));
                if (due == null) due = parseDueDate(dto.getData().get("date"));
                if (due == null) due = parseDueDate(dto.getData().get("time"));
                if (due == null) due = java.time.LocalDateTime.now().plusMinutes(1);

                String userEmail = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getGmailUsername();
                String category = "Schedule & Reminders";

                boolean isEmailReminder = title.toLowerCase().contains("email") || desc.toLowerCase().contains("email") ||
                        title.toLowerCase().contains("send") || desc.toLowerCase().contains("send") ||
                        title.toLowerCase().contains("mail") || desc.toLowerCase().contains("mail");

                if (isEmailReminder) {
                    category = "Email Management";
                    if (!desc.contains("Recipient: ")) {
                        desc = "Recipient: " + userEmail + "\nSubject: Reminder - " + title + "\nBody: " + desc;
                    }
                }

                taskService.createTask(user, title, desc, category, due, recurrence, reminderOffset);

                String msgText = "Executed successfully: Scheduled " + (isEmailReminder ? "email reminder" : "reminder") + " for " + due.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
                result.put("status", "success");
                result.put("message", msgText);

                updateChatMessageActionStatus(user, dto.getMessageId(), "executed", msgText);

            } else {
                result.put("status", "error");
                result.put("message", "Unknown action type: " + dto.getAction());
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            result.put("status", "error");
            result.put("message", "Failed to execute action: " + ex.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/api/chat/action/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelAction(@RequestBody ActionConfirmDto dto,
                                                            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        updateChatMessageActionStatus(user, dto.getMessageId(), "cancelled", "Action Cancelled by User");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Action cancelled");
        return ResponseEntity.ok(result);
    }
}
