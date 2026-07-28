package com.aiautomation.controller;

import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final UserService userService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) {
            return customUserDetails.getUser();
        }
        return userService.getOrCreateDemoUser();
    }











    @GetMapping("/email-preview")
    public String emailPreviewPage(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        model.addAttribute("user", user);
        return "email-preview";
    }
}
