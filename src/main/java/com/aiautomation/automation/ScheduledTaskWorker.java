package com.aiautomation.automation;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.Task;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.TaskRepository;
import com.aiautomation.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTaskWorker {

    private final TaskRepository taskRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ActivityLogRepository activityLogRepository;

    @Scheduled(fixedRate = 10000) // Runs every 10 seconds
    @Transactional
    public void processDueTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> dueTasks = taskRepository.findByCompletedFalseAndDueDateLessThanEqual(now);

        if (dueTasks.isEmpty()) {
            return;
        }

        log.info("Processing {} due scheduled tasks/reminders at {}...", dueTasks.size(), now);

        for (Task task : dueTasks) {
            try {
                User user = task.getUser();
                String title = task.getTitle() != null ? task.getTitle() : "Scheduled Automation";
                String desc = task.getDescription() != null ? task.getDescription() : "";
                String category = task.getCategory() != null ? task.getCategory().toLowerCase() : "";

                boolean isReportTask = title.toLowerCase().contains("report") || desc.toLowerCase().contains("report") ||
                        title.toLowerCase().contains("digest") || desc.toLowerCase().contains("digest") ||
                        title.toLowerCase().contains("summary") || desc.toLowerCase().contains("summary");

                boolean isEmailTask = isReportTask || category.contains("email") || category.contains("2") ||
                        title.toLowerCase().contains("email") || desc.toLowerCase().contains("email") ||
                        title.toLowerCase().contains("send") || desc.toLowerCase().contains("send");

                if (isEmailTask) {
                    // Extract recipient if encoded in description or default to user email
                    String recipient = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getGmailUsername();

                    if (desc.contains("Recipient: ")) {
                        try {
                            int start = desc.indexOf("Recipient: ") + 11;
                            int end = desc.indexOf("\n", start);
                            if (end > start) {
                                String parsedRecipient = desc.substring(start, end).trim();
                                if (parsedRecipient.contains("@")) {
                                    recipient = parsedRecipient;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    String subject = title.startsWith("Scheduled Email:") ? title.substring(16).trim() : "[Scheduled Automation] " + title;
                    String body = desc;

                    if (desc.contains("Subject: ")) {
                        try {
                            int sStart = desc.indexOf("Subject: ") + 9;
                            int sEnd = desc.indexOf("\n", sStart);
                            if (sEnd > sStart) {
                                String parsedSubject = desc.substring(sStart, sEnd).trim();
                                if (!parsedSubject.isBlank()) {
                                    subject = parsedSubject;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    if (desc.contains("Body: ")) {
                        body = desc.substring(desc.indexOf("Body: ") + 6).trim();
                    } else if (desc.contains("Recipient: ") && desc.contains("\n\n")) {
                        body = desc.substring(desc.indexOf("\n\n") + 2).trim();
                    }

                    if (body.isBlank()) body = "This is a scheduled automated reminder for: " + title;

                    if (isReportTask) {
                        String reportType = "Periodic";
                        if (title.toLowerCase().contains("weekly") || desc.toLowerCase().contains("weekly")) reportType = "Weekly";
                        else if (title.toLowerCase().contains("monthly") || desc.toLowerCase().contains("monthly")) reportType = "Monthly";
                        else if (title.toLowerCase().contains("yearly") || desc.toLowerCase().contains("yearly")) reportType = "Yearly";

                        String htmlContent = buildActivityReportHtml(user, reportType);
                        log.info("Executing scheduled HTML report email for task '{}' to recipient '{}'...", title, recipient);
                        emailService.sendHtmlEmail(user, recipient, reportType + " Activity & Performance Report", htmlContent);
                    } else {
                        log.info("Executing scheduled email for task '{}' (Subject: '{}') to recipient '{}'...", title, subject, recipient);
                        emailService.sendEmail(user, recipient, subject, body);
                    }
                }

                // Always send in-app notification for due tasks
                notificationService.createNotification(user,
                        "Scheduled Event Due: " + title,
                        desc.isBlank() ? "Your scheduled event is now due." : desc,
                        "REMINDER");

                // Log activity
                activityLogRepository.save(ActivityLog.builder()
                        .user(user)
                        .actionType("AUTOMATION")
                        .description("Executed scheduled task: " + title)
                        .build());

                // Update recurrence or mark completed
                String recurrence = task.getRecurrence() != null ? task.getRecurrence().toUpperCase() : "ONCE";
                switch (recurrence) {
                    case "DAILY" -> task.setDueDate(task.getDueDate().plusDays(1));
                    case "WEEKLY" -> task.setDueDate(task.getDueDate().plusWeeks(1));
                    case "MONTHLY" -> task.setDueDate(task.getDueDate().plusMonths(1));
                    case "YEARLY" -> task.setDueDate(task.getDueDate().plusYears(1));
                    case "WEEKDAYS" -> task.setDueDate(advanceWeekday(task.getDueDate()));
                    default -> task.setCompleted(true);
                }

                taskRepository.save(task);
                log.info("Task '{}' successfully processed. Recurrence: {}", title, recurrence);

            } catch (Exception ex) {
                log.error("Failed to process scheduled task '{}' (ID: {}): {}", task.getTitle(), task.getId(), ex.getMessage(), ex);
            }
        }
    }

    private LocalDateTime advanceWeekday(LocalDateTime current) {
        LocalDateTime next = current.plusDays(1);
        while (next.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || next.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            next = next.plusDays(1);
        }
        return next;
    }

    private String buildActivityReportHtml(User user, String reportType) {
        long totalTasks = taskRepository.countByUser(user);
        long completedTasks = taskRepository.countByUserAndCompleted(user, true);
        List<ActivityLog> logs = activityLogRepository.findByUserOrderByTimestampDesc(user);
        int activityCount = logs.size();

        StringBuilder rows = new StringBuilder();
        int max = Math.min(10, logs.size());
        if (max == 0) {
            rows.append("<tr><td colspan='3' style='text-align: center; color: #94a3b8;'>No recent activity logs recorded.</td></tr>");
        } else {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            for (int i = 0; i < max; i++) {
                ActivityLog logItem = logs.get(i);
                String action = logItem.getActionType() != null ? logItem.getActionType() : "LOG";
                String desc = logItem.getDescription() != null ? logItem.getDescription() : "";
                String time = logItem.getTimestamp() != null ? logItem.getTimestamp().format(fmt) : "";
                rows.append("<tr>")
                    .append("<td><span style='background: #312e81; color: #a5b4fc; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 600;'>").append(action).append("</span></td>")
                    .append("<td>").append(desc).append("</td>")
                    .append("<td>").append(time).append("</td>")
                    .append("</tr>");
            }
        }

        String userName = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : user.getEmail();

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
              body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
              .container { max-width: 650px; margin: 0 auto; background: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
              .header { background: linear-gradient(135deg, #6366f1 0%%, #4f46e5 100%%); padding: 24px; text-align: center; }
              .header h2 { margin: 0; color: #ffffff; font-size: 22px; font-weight: 700; }
              .header p { margin: 6px 0 0; color: #e0e7ff; font-size: 14px; }
              .content { padding: 24px; }
              .metrics-grid { display: flex; gap: 12px; margin-bottom: 24px; }
              .metric-card { flex: 1; background: #0f172a; border-radius: 8px; border: 1px solid #334155; padding: 16px; text-align: center; }
              .metric-val { font-size: 24px; font-weight: 700; color: #818cf8; }
              .metric-lbl { font-size: 12px; color: #94a3b8; margin-top: 4px; }
              .section-title { font-size: 15px; font-weight: 600; color: #f1f5f9; margin-bottom: 12px; border-bottom: 1px solid #334155; padding-bottom: 6px; }
              .table { width: 100%%; border-collapse: collapse; margin-top: 8px; }
              .table th { background: #0f172a; color: #94a3b8; text-align: left; padding: 10px; font-size: 12px; border-bottom: 1px solid #334155; }
              .table td { padding: 10px; font-size: 13px; color: #cbd5e1; border-bottom: 1px solid #334155; }
              .footer { background: #0f172a; padding: 16px; text-align: center; border-top: 1px solid #334155; font-size: 13px; color: #94a3b8; }
              .signature { color: #818cf8; font-weight: 600; margin: 0; }
            </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2>AI Automation Assistant</h2>
                  <p>%s Activity & Performance Report</p>
                </div>
                <div class="content">
                  <p style="color: #cbd5e1; font-size: 14px;">Hi %s,</p>
                  <p style="color: #94a3b8; font-size: 13px;">Here is your automated activity report summarizing everything executed in your workspace:</p>
                  
                  <div class="metrics-grid">
                    <div class="metric-card">
                      <div class="metric-val">%d</div>
                      <div class="metric-lbl">Total Tasks</div>
                    </div>
                    <div class="metric-card">
                      <div class="metric-val">%d</div>
                      <div class="metric-lbl">Completed</div>
                    </div>
                    <div class="metric-card">
                      <div class="metric-val">%d</div>
                      <div class="metric-lbl">Automations Executed</div>
                    </div>
                  </div>

                  <div class="section-title">Recent Activity Logs</div>
                  <table class="table">
                    <thead>
                      <tr>
                        <th>Action</th>
                        <th>Description</th>
                        <th>Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>
                </div>
                <div class="footer">
                  <p class="signature">AI Automated Assistant regards</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(reportType, userName, totalTasks, completedTasks, activityCount, rows.toString());
    }
}
