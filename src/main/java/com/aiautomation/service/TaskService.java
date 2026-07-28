package com.aiautomation.service;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.Task;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;

    public List<Task> getUserTasks(User user) {
        return taskRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public static String normalizeCategory(String categoryInput) {
        if (categoryInput == null || categoryInput.isBlank()) return "General Automation";
        String str = categoryInput.trim();
        if ("1".equals(str) || "general".equalsIgnoreCase(str) || "general automation".equalsIgnoreCase(str)) {
            return "General Automation";
        } else if ("2".equals(str) || "email".equalsIgnoreCase(str) || "email management".equalsIgnoreCase(str)) {
            return "Email Management";
        } else if ("3".equals(str) || "calendar".equalsIgnoreCase(str) || "schedule".equalsIgnoreCase(str) || "schedule & reminders".equalsIgnoreCase(str)) {
            return "Schedule & Reminders";
        } else if ("4".equals(str) || "database".equalsIgnoreCase(str) || "database management".equalsIgnoreCase(str)) {
            return "Database Management";
        }
        return str;
    }

    public Task createTask(User user, String title, String description, String category, LocalDateTime dueDate) {
        return createTask(user, title, description, category, dueDate, "ONCE", "NONE");
    }

    public Task createTask(User user, String title, String description, String category, LocalDateTime dueDate, String recurrence, String reminderOffset) {
        String normalizedCategory = normalizeCategory(category);

        Task task = Task.builder()
                .user(user)
                .title(title)
                .description(description)
                .category(normalizedCategory)
                .completed(false)
                .dueDate(dueDate != null ? dueDate : LocalDateTime.now().plusDays(1))
                .recurrence(recurrence != null ? recurrence.toUpperCase() : "ONCE")
                .reminderOffset(reminderOffset != null ? reminderOffset.toUpperCase() : "NONE")
                .build();

        Task savedTask = taskRepository.save(task);

        activityLogRepository.save(ActivityLog.builder()
                .user(user)
                .actionType("TASK")
                .description("Created task: " + title)
                .build());

        try {
            notificationService.createNotification(user, "New Task Scheduled", "Task '" + title + "' scheduled under " + normalizedCategory, "TASK");
        } catch (Exception ignored) {}

        return savedTask;
    }

    public Task toggleTaskCompleted(User user, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized task access.");
        }

        task.setCompleted(!task.isCompleted());
        Task updated = taskRepository.save(task);

        activityLogRepository.save(ActivityLog.builder()
                .user(user)
                .actionType("TASK")
                .description((updated.isCompleted() ? "Completed" : "Reopened") + " task: " + task.getTitle())
                .build());

        try {
            String title = updated.isCompleted() ? "Task Completed" : "Task Reopened";
            notificationService.createNotification(user, title, "Task '" + task.getTitle() + "' marked as " + (updated.isCompleted() ? "completed" : "pending"), "TASK");
        } catch (Exception ignored) {}

        return updated;
    }

    public void deleteTask(User user, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized task access.");
        }

        taskRepository.delete(task);

        activityLogRepository.save(ActivityLog.builder()
                .user(user)
                .actionType("TASK")
                .description("Deleted task: " + task.getTitle())
                .build());
    }
}
