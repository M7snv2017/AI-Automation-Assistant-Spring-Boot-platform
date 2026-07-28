package com.aiautomation.service;

import com.aiautomation.dto.ChangePasswordDto;
import com.aiautomation.dto.RegisterDto;
import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.UserRepository;
import com.aiautomation.security.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;

    public User registerUser(RegisterDto registerDto) {
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new IllegalArgumentException("Email address is already in use.");
        }
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = User.builder()
                .fullName(registerDto.getFullName())
                .email(registerDto.getEmail())
                .passwordHash(passwordEncoder.encode(registerDto.getPassword()))
                .role("ROLE_USER")
                .theme("light")
                .language("en")
                .aiModel("qwen2.5-coder:14b")
                .notifyEmail(true)
                .notifyBrowser(true)
                .build();

        User savedUser = userRepository.save(user);

        activityLogRepository.save(ActivityLog.builder()
                .user(savedUser)
                .actionType("LOGIN")
                .description("Account created and registered successfully.")
                .build());

        return savedUser;
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void changePassword(User user, ChangePasswordDto dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        activityLogRepository.save(ActivityLog.builder()
                .user(user)
                .actionType("SETTINGS")
                .description("Security password changed successfully.")
                .build());
    }

    public User updateUserPreferences(User user, String fullName, String theme, String language, String aiModel, boolean notifyEmail, boolean notifyBrowser, String gmailUsername, String appPassword) {
        if (fullName != null && !fullName.isBlank()) user.setFullName(fullName);
        if (theme != null) user.setTheme(theme);
        if (language != null) user.setLanguage(language);
        if (aiModel != null) user.setAiModel(aiModel);
        user.setNotifyEmail(notifyEmail);
        user.setNotifyBrowser(notifyBrowser);
        if (gmailUsername != null) user.setGmailUsername(gmailUsername);
        if (appPassword != null && !appPassword.isBlank()) {
            user.setEncryptedAppPassword(encryptionUtil.encrypt(appPassword));
        }

        return userRepository.save(user);
    }

    public User getOrCreateDemoUser() {
        return userRepository.findByEmail("demo@example.com").orElseGet(() -> {
            User demo = User.builder()
                    .email("demo@example.com")
                    .fullName("Alex Morgan")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role("ROLE_USER")
                    .theme("light")
                    .language("en")
                    .aiModel("qwen2.5-coder:14b")
                    .notifyEmail(true)
                    .notifyBrowser(true)
                    .build();
            return userRepository.save(demo);
        });
    }
}
