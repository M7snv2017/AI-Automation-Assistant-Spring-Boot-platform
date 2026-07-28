package com.aiautomation.controller;

import com.aiautomation.entity.Notification;
import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.NotificationService;
import com.aiautomation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @GetMapping("/notifications")
    public String notificationsPage(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        List<Notification> notifications = notificationService.getUserNotifications(user);
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("user", user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        return "notifications";
    }

    @PostMapping("/api/notifications/read/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("id") UUID id,
                                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        notificationService.markAsRead(user, id);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/notifications/read-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        notificationService.markAllAsRead(user);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        return ResponseEntity.ok(result);
    }
}
