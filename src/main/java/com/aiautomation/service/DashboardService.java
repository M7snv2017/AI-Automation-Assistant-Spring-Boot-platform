package com.aiautomation.service;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.AutomationExecution;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.AutomationExecutionRepository;
import com.aiautomation.repository.NotificationRepository;
import com.aiautomation.repository.TaskRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final AutomationExecutionRepository automationExecutionRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;

    @Value("${ollama.host:http://localhost:11434}")
    private String ollamaHost;

    @Data
    @Builder
    public static class DashboardSummary {
        private long activeTasks;
        private long upcomingReminders;
        private long pendingConfirmations;
        private long automationsExecutedToday;
        private long completedTasks;
        private long unreadNotificationsCount;
        private boolean ollamaOnline;
        private boolean gmailConfigured;
        private List<ActivityLog> recentActivities;
        private List<AutomationExecution> pendingActions;
    }

    public DashboardSummary getDashboardSummary(User user) {
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();

        long activeTasksCount = taskRepository.countByUserAndCompleted(user, false);
        long upcomingRemindersCount = taskRepository.countByUserAndCompletedFalseAndDueDateGreaterThanEqual(user, now);
        long pendingConfirms = automationExecutionRepository.countByUserAndStatus(user, AutomationExecution.Status.PENDING_CONFIRMATION);
        long executedToday = automationExecutionRepository.countByUserAndStatusAndExecutedAtGreaterThanEqual(user, AutomationExecution.Status.EXECUTED, startOfToday);
        long completedTasksCount = taskRepository.countByUserAndCompleted(user, true);
        long unreadNotifs = notificationRepository.countByUserAndRead(user, false);

        List<ActivityLog> activities = activityLogRepository.findByUserOrderByTimestampDesc(user, PageRequest.of(0, 5));
        List<AutomationExecution> pendingList = automationExecutionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(e -> e.getStatus() == AutomationExecution.Status.PENDING_CONFIRMATION)
                .toList();

        boolean isOllamaUp = checkOllamaStatus();
        boolean isGmailSet = user.getGmailUsername() != null && !user.getGmailUsername().isBlank();

        return DashboardSummary.builder()
                .activeTasks(activeTasksCount)
                .upcomingReminders(upcomingRemindersCount)
                .pendingConfirmations(pendingConfirms)
                .automationsExecutedToday(executedToday)
                .completedTasks(completedTasksCount)
                .unreadNotificationsCount(unreadNotifs)
                .ollamaOnline(isOllamaUp)
                .gmailConfigured(isGmailSet)
                .recentActivities(activities)
                .pendingActions(pendingList)
                .build();
    }

    private boolean checkOllamaStatus() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(ollamaHost + "/api/tags", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
