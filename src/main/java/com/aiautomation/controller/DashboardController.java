package com.aiautomation.controller;

import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.DashboardService;
import com.aiautomation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        User user = (customUserDetails != null) ? customUserDetails.getUser() : userService.getOrCreateDemoUser();

        DashboardService.DashboardSummary summary = dashboardService.getDashboardSummary(user);

        model.addAttribute("user", user);
        model.addAttribute("summary", summary);
        return "dashboard";
    }
}
