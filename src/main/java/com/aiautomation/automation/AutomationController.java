package com.aiautomation.automation;

import com.aiautomation.entity.AutomationAction;
import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationEngine automationEngine;
    private final UserService userService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @Data
    public static class PrepareActionDto {
        private String intent;
        private String actionType; // EMAIL, TASK, REMINDER, DOC_GEN, SUMMARIZE
        private String recipient;
        private String subject;
        private String body;
    }

    @PostMapping("/api/automation/prepare")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> prepareAction(@RequestBody PrepareActionDto dto,
                                                            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);

        Map<String, Object> params = new HashMap<>();
        if (dto.getRecipient() != null) params.put("recipient", dto.getRecipient());
        if (dto.getSubject() != null) params.put("subject", dto.getSubject());
        if (dto.getBody() != null) params.put("body", dto.getBody());

        AutomationAction action = automationEngine.prepareAction(
                user,
                dto.getIntent(),
                dto.getActionType(),
                params,
                dto.getBody() != null ? dto.getBody() : dto.getIntent()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("actionId", action.getId().toString());
        response.put("previewSummary", action.getPreviewSummary());
        response.put("requiresConfirmation", true);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/automation/confirm/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmAction(@PathVariable("id") UUID id,
                                                            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        Map<String, Object> result = new HashMap<>();

        try {
            AutomationAction executedAction = automationEngine.executeConfirmedAction(user, id);
            result.put("status", "success");
            result.put("message", "Automation executed successfully.");
            result.put("executionLog", executedAction.getExecutionLog());
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            result.put("status", "error");
            result.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/api/automation/cancel/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelAction(@PathVariable("id") UUID id,
                                                           @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        AutomationAction action = automationEngine.cancelAction(user, id);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Automation cancelled.");
        result.put("actionId", action.getId().toString());

        return ResponseEntity.ok(result);
    }
}
