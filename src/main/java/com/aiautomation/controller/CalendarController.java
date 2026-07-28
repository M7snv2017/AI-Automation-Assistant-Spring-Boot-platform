package com.aiautomation.controller;

import com.aiautomation.entity.User;
import com.aiautomation.security.CustomUserDetails;
import com.aiautomation.service.CalendarService;
import com.aiautomation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final UserService userService;

    private User getAuthenticatedUser(CustomUserDetails customUserDetails) {
        if (customUserDetails != null) return customUserDetails.getUser();
        return userService.getOrCreateDemoUser();
    }

    @GetMapping("/calendar")
    public String calendarPage(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        User user = getAuthenticatedUser(customUserDetails);
        model.addAttribute("user", user);
        return "calendar";
    }

    @GetMapping("/api/calendar/day")
    @ResponseBody
    public ResponseEntity<CalendarService.DayActivitiesDto> getDayActivities(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        if (date == null) date = LocalDate.now();

        CalendarService.DayActivitiesDto activities = calendarService.getActivitiesForDay(user, date);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/api/calendar/month-tasks")
    @ResponseBody
    public ResponseEntity<java.util.List<String>> getMonthTaskDates(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        java.util.List<String> dates = calendarService.getTaskDatesForMonth(user, year, month);
        return ResponseEntity.ok(dates);
    }

    @GetMapping("/api/calendar/month-summary")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, CalendarService.MonthDateSummaryDto>> getMonthSummary(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = getAuthenticatedUser(customUserDetails);
        return ResponseEntity.ok(calendarService.getMonthActivitySummary(user, year, month));
    }
}
