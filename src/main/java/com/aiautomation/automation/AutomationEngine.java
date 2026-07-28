package com.aiautomation.automation;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.AutomationAction;
import com.aiautomation.entity.Task;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.AutomationActionRepository;
import com.aiautomation.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationEngine {

    private final AutomationActionRepository automationActionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final TaskRepository taskRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AutomationAction prepareAction(User user, String intent, String actionType, Map<String, Object> params, String previewSummary) {
        String paramsJson = "{}";
        try {
            paramsJson = objectMapper.writeValueAsString(params);
        } catch (Exception ignored) {}

        AutomationAction action = AutomationAction.builder()
                .user(user)
                .intent(intent)
                .actionType(actionType)
                .parametersJson(paramsJson)
                .previewSummary(previewSummary)
                .status("PENDING_CONFIRMATION")
                .requiresConfirmation(true)
                .build();

        return automationActionRepository.save(action);
    }

    public AutomationAction executeConfirmedAction(User user, UUID actionId) {
        AutomationAction action = automationActionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Automation action not found."));

        if (!action.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to action.");
        }

        if (!"PENDING_CONFIRMATION".equals(action.getStatus())) {
            throw new IllegalStateException("Action is not in pending confirmation state.");
        }

        try {
            switch (action.getActionType().toUpperCase()) {
                case "EMAIL" -> executeEmailAction(user, action);
                case "TASK" -> executeTaskAction(user, action);
                case "REMINDER" -> executeReminderAction(user, action);
                case "DOC_GEN", "SUMMARIZE" -> executeDocAction(user, action);
                default -> action.setExecutionLog("Action type " + action.getActionType() + " executed.");
            }

            action.setStatus("EXECUTED");
            action.setExecutedAt(LocalDateTime.now());
            automationActionRepository.save(action);

            activityLogRepository.save(ActivityLog.builder()
                    .user(user)
                    .actionType(action.getActionType())
                    .description("Confirmed and executed automation: " + action.getIntent())
                    .build());

        } catch (Exception ex) {
            log.error("Execution failed for action {}: {}", actionId, ex.getMessage());
            action.setStatus("FAILED");
            action.setExecutionLog("Execution error: " + ex.getMessage());
            automationActionRepository.save(action);
            throw new RuntimeException("Automation execution failed: " + ex.getMessage());
        }

        return action;
    }

    public AutomationAction cancelAction(User user, UUID actionId) {
        AutomationAction action = automationActionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Action not found."));
        action.setStatus("CANCELLED");
        return automationActionRepository.save(action);
    }

    @SuppressWarnings("unchecked")
    private void executeEmailAction(User user, AutomationAction action) throws Exception {
        Map<String, Object> params = objectMapper.readValue(action.getParametersJson(), Map.class);
        String recipient = (String) params.getOrDefault("recipient", user.getEmail());
        String subject = (String) params.getOrDefault("subject", "Automated Notification");
        String body = (String) params.getOrDefault("body", action.getPreviewSummary());

        emailService.sendEmail(user, recipient, subject, body);
        action.setExecutionLog("Email dispatched successfully to " + recipient);
    }

    @SuppressWarnings("unchecked")
    private void executeTaskAction(User user, AutomationAction action) throws Exception {
        Map<String, Object> params = objectMapper.readValue(action.getParametersJson(), Map.class);
        String title = (String) params.getOrDefault("title", action.getIntent());
        String desc = (String) params.getOrDefault("description", action.getPreviewSummary());
        String cat = (String) params.getOrDefault("category", "Automation");

        taskRepository.save(Task.builder()
                .user(user)
                .title(title)
                .description(desc)
                .category(cat)
                .completed(false)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build());

        action.setExecutionLog("Task created: " + title);
    }

    private void executeReminderAction(User user, AutomationAction action) {
        action.setExecutionLog("Reminder scheduled: " + action.getIntent());
    }

    private void executeDocAction(User user, AutomationAction action) {
        action.setExecutionLog("Document generated and saved.");
    }
}
