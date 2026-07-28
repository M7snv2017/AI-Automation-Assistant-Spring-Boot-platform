package com.aiautomation.controller;

import com.aiautomation.automation.EmailService;
import com.aiautomation.entity.User;
import com.aiautomation.llm.LLMService;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;
    private final LLMService llmService;
    private final EmailService emailService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @GetMapping("/settings")
    public String settingsPage(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        List<String> availableModels = llmService.getAvailableModels();

        model.addAttribute("user", user);
        model.addAttribute("availableModels", availableModels);
        return "settings";
    }

    @Data
    public static class UpdateSettingsDto {
        private String fullName;
        private String theme;
        private String language;
        private String aiModel;
        private boolean notifyEmail;
        private boolean notifyBrowser;
        private String gmailUsername;
        private String appPassword;
    }

    @PostMapping("/settings/update")
    public String updateSettings(@ModelAttribute UpdateSettingsDto dto,
                                 @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(customUserDetails);
        userService.updateUserPreferences(
                user,
                dto.getFullName(),
                dto.getTheme(),
                dto.getLanguage(),
                dto.getAiModel(),
                dto.isNotifyEmail(),
                dto.isNotifyBrowser(),
                dto.getGmailUsername(),
                dto.getAppPassword()
        );

        if (dto.getGmailUsername() != null && !dto.getGmailUsername().isBlank()) {
            redirectAttributes.addFlashAttribute("gmailSuccessMessage", "Gmail credentials updated successfully.");
        }
        redirectAttributes.addFlashAttribute("settingsSuccessMessage", "AI Configuration and preferences updated successfully.");

        return "redirect:/settings?updated=true";
    }

    @PostMapping("/settings/gmail/test")
    public String sendTestEmail(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(customUserDetails);
        try {
            String recipient = emailService.sendTestEmail(user);
            redirectAttributes.addFlashAttribute("testEmailSuccess", "Test email sent successfully to " + recipient + ". Gmail connected successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("testEmailError", "Failed to send test email: " + ex.getMessage());
        }
        return "redirect:/settings?tested=true";
    }

    @PostMapping("/api/settings/gmail/test")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> apiSendTestEmail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        try {
            String recipient = emailService.sendTestEmail(user);
            res.put("status", "success");
            res.put("message", "Test email sent successfully to " + recipient + ". Gmail connected successfully!");
            return org.springframework.http.ResponseEntity.ok(res);
        } catch (Exception ex) {
            res.put("status", "error");
            res.put("message", "Failed to send test email: " + ex.getMessage());
            return org.springframework.http.ResponseEntity.badRequest().body(res);
        }
    }
}
