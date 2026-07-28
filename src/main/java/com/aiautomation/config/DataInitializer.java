package com.aiautomation.config;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.Notification;
import com.aiautomation.entity.Task;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.NotificationRepository;
import com.aiautomation.repository.TaskRepository;
import com.aiautomation.repository.UserRepository;
import com.aiautomation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        User demoUser = userService.getOrCreateDemoUser();

        if (taskRepository.countByUser(demoUser) == 0) {
            taskRepository.save(Task.builder()
                    .user(demoUser)
                    .title("Review AI Email Automation Strategy")
                    .description("Prepare preview email templates for high priority client communications.")
                    .category("Automation")
                    .completed(false)
                    .dueDate(LocalDateTime.now().plusDays(1))
                    .build());

            taskRepository.save(Task.builder()
                    .user(demoUser)
                    .title("Configure Ollama Local Model Endpoint")
                    .description("Ensure qwen2.5-coder:14b model server is responding cleanly.")
                    .category("Setup")
                    .completed(true)
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .build());

            taskRepository.save(Task.builder()
                    .user(demoUser)
                    .title("Weekly Task Summary Report")
                    .description("Generate summarized report of completed automation workflows.")
                    .category("Reporting")
                    .completed(false)
                    .dueDate(LocalDateTime.now().plusDays(3))
                    .build());
        }

        if (notificationRepository.countByUserAndRead(demoUser, false) == 0 && notificationRepository.findByUserOrderByCreatedAtDesc(demoUser).isEmpty()) {
            notificationRepository.save(Notification.builder()
                    .user(demoUser)
                    .title("System Initialized")
                    .message("Welcome to AI Automation Assistant. Your privacy-first local engine is operational.")
                    .type("SUCCESS")
                    .read(false)
                    .build());

            notificationRepository.save(Notification.builder()
                    .user(demoUser)
                    .title("Ollama Connection Check")
                    .message("Model qwen2.5-coder:14b is configured as default LLM execution provider.")
                    .type("INFO")
                    .read(false)
                    .build());
        }

        if (activityLogRepository.findByUserOrderByTimestampDesc(demoUser).isEmpty()) {
            activityLogRepository.save(ActivityLog.builder()
                    .user(demoUser)
                    .actionType("LOGIN")
                    .description("Demo user initialized and logged into assistant session.")
                    .build());
        }
    }
}
