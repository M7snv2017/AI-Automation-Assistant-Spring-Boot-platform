package com.aiautomation.controller;

import com.aiautomation.entity.Task;
import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.TaskService;
import com.aiautomation.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @GetMapping("/tasks")
    public String tasksPage(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        List<Task> tasks = taskService.getUserTasks(user);

        model.addAttribute("user", user);
        model.addAttribute("tasks", tasks);
        return "tasks";
    }

    @Data
    public static class CreateTaskDto {
        private String title;
        private String description;
        private String category;
        private String dueDate;
        private String recurrence;
        private String reminderOffset;
    }

    @PostMapping("/api/tasks/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody CreateTaskDto dto,
                                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        java.time.LocalDateTime due = null;
        if (dto.getDueDate() != null && !dto.getDueDate().isBlank()) {
            try {
                due = java.time.LocalDateTime.parse(dto.getDueDate());
            } catch (Exception e) {
                try {
                    due = java.time.LocalDate.parse(dto.getDueDate()).atStartOfDay();
                } catch (Exception ignored) {}
            }
        }
        Task task = taskService.createTask(user, dto.getTitle(), dto.getDescription(), dto.getCategory(), due, dto.getRecurrence(), dto.getReminderOffset());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("taskId", task.getId().toString());
        response.put("title", task.getTitle());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/tasks/toggle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleTask(@PathVariable("id") UUID id,
                                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        Task task = taskService.toggleTaskCompleted(user, id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("completed", task.isCompleted());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/tasks/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable("id") UUID id,
                                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        taskService.deleteTask(user, id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}
