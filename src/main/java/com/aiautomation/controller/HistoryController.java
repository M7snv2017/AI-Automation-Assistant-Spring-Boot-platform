package com.aiautomation.controller;

import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.HistoryService;
import com.aiautomation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final UserService userService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @GetMapping("/history")
    public String historyPage(@RequestParam(value = "q", required = false) String query,
                              @RequestParam(value = "filter", required = false) String filter,
                              @AuthenticationPrincipal CustomUserDetails customUserDetails,
                              Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        HistoryService.CombinedHistoryDto historyData = historyService.getHistory(user, query, filter);

        model.addAttribute("user", user);
        model.addAttribute("history", historyData);
        model.addAttribute("query", query);
        model.addAttribute("filter", filter);

        return "history";
    }
}
