package com.aiautomation.service;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.AutomationAction;
import com.aiautomation.entity.AutomationExecution;
import com.aiautomation.entity.Task;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.AutomationActionRepository;
import com.aiautomation.repository.AutomationExecutionRepository;
import com.aiautomation.repository.TaskRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final TaskRepository taskRepository;
    private final AutomationActionRepository automationActionRepository;
    private final AutomationExecutionRepository automationExecutionRepository;
    private final ActivityLogRepository activityLogRepository;

    @Data
    @Builder
    public static class DayActivitiesDto {
        private String dateFormatted;
        private List<Task> tasks;
        private List<AutomationAction> automations;
        private List<AutomationExecution> executions;
        private List<ActivityLog> activities;
    }

    @Data
    @Builder
    public static class TaskSummaryDto {
        private String id;
        private String title;
        private String category;
        private String timeStr;
    }

    @Data
    @Builder
    public static class MonthDateSummaryDto {
        private String date;
        private boolean hasTask;       // Cat 1: General Automation (Blue)
        private boolean hasEmail;      // Cat 2: Email Management (Purple)
        private boolean hasReminder;   // Cat 3: Schedule & Reminders (Green)
        private boolean hasDatabase;   // Cat 4: Database Management (Orange)
        private boolean hasAutomation;
        private boolean hasFailed;     // Failed (Red)
        @Builder.Default
        private List<TaskSummaryDto> eventItems = new java.util.ArrayList<>();
    }

    public DayActivitiesDto getActivitiesForDay(User user, LocalDate date) {
        LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);

        List<Task> dayTasks = taskRepository.findByUserAndDueDateBetweenOrderByDueDateAsc(user, startOfDay, endOfDay);
        List<AutomationExecution> dayAutomations = automationExecutionRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, startOfDay, endOfDay);

        List<ActivityLog> dayLogs = activityLogRepository.findByUserOrderByTimestampDesc(user).stream()
                .filter(l -> l.getTimestamp() != null && !l.getTimestamp().isBefore(startOfDay) && !l.getTimestamp().isAfter(endOfDay))
                .toList();

        return DayActivitiesDto.builder()
                .dateFormatted(date.toString())
                .tasks(dayTasks)
                .executions(dayAutomations)
                .activities(dayLogs)
                .build();
    }

    public List<String> getTaskDatesForMonth(User user, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, month, start.toLocalDate().lengthOfMonth(), 23, 59, 59);

        List<Task> tasks = taskRepository.findByUserAndDueDateBetweenOrderByDueDateAsc(user, start, end);
        return tasks.stream()
                .filter(t -> t.getDueDate() != null)
                .map(t -> t.getDueDate().toLocalDate().toString())
                .distinct()
                .toList();
    }

    public java.util.Map<String, MonthDateSummaryDto> getMonthActivitySummary(User user, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, month, start.toLocalDate().lengthOfMonth(), 23, 59, 59);

        java.util.Map<String, MonthDateSummaryDto> map = new java.util.HashMap<>();

        // Tasks
        List<Task> tasks = taskRepository.findByUserAndDueDateBetweenOrderByDueDateAsc(user, start, end);
        for (Task t : tasks) {
            if (t.getDueDate() == null) continue;
            String d = t.getDueDate().toLocalDate().toString();
            MonthDateSummaryDto dto = map.computeIfAbsent(d, k -> MonthDateSummaryDto.builder().date(k).eventItems(new java.util.ArrayList<>()).build());
            if (dto.getEventItems() == null) dto.setEventItems(new java.util.ArrayList<>());

            String cat = t.getCategory() != null ? t.getCategory().trim().toLowerCase() : "1";
            if (cat.equals("2") || cat.contains("email")) {
                dto.setHasEmail(true);
            } else if (cat.equals("3") || cat.contains("schedule") || cat.contains("reminder")) {
                dto.setHasReminder(true);
            } else if (cat.equals("4") || cat.contains("database")) {
                dto.setHasDatabase(true);
            } else {
                dto.setHasTask(true);
            }

            String timeStr = t.getDueDate().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            dto.getEventItems().add(TaskSummaryDto.builder()
                    .id(t.getId() != null ? t.getId().toString() : "")
                    .title(t.getTitle())
                    .category(cat)
                    .timeStr(timeStr)
                    .build());
        }

        // Automations
        List<AutomationExecution> executions = automationExecutionRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtAsc(user, start, end);
        for (AutomationExecution e : executions) {
            if (e.getCreatedAt() == null) continue;
            String d = e.getCreatedAt().toLocalDate().toString();
            MonthDateSummaryDto dto = map.computeIfAbsent(d, k -> MonthDateSummaryDto.builder().date(k).build());
            if (e.getStatus() == AutomationExecution.Status.FAILED) {
                dto.setHasFailed(true);
            } else {
                dto.setHasAutomation(true);
            }
        }

        return map;
    }
}
