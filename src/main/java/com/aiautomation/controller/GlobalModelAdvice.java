package com.aiautomation.controller;

import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.NotificationService;
import com.aiautomation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final NotificationService notificationService;
    private final UserService userService;

    @ModelAttribute("unreadNotificationsCount")
    public long getUnreadNotificationsCount(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            return 0;
        }
        User user = customUserDetails.getUser();
        if (user == null) return 0;
        try {
            return notificationService.getUnreadCount(user);
        } catch (Exception e) {
            return 0;
        }
    }
}
