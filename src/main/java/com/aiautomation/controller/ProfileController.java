package com.aiautomation.controller;

import com.aiautomation.dto.ChangePasswordDto;
import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        model.addAttribute("user", user);
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                 Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "profile";
        }
        try {
            userService.changePassword(user, dto);
            return "redirect:/profile?passwordChanged=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", ex.getMessage());
            return "profile";
        }
    }
}
