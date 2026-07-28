package com.aiautomation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private String category = "General";

    @Builder.Default
    private boolean completed = false;

    private LocalDateTime dueDate;

    @Builder.Default
    private String recurrence = "ONCE"; // ONCE, DAILY, WEEKLY, MONTHLY, WEEKDAYS

    @Builder.Default
    private String reminderOffset = "NONE"; // NONE, MINUTES_30, HOURS_1, DAYS_1, WEEKS_1

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getFormattedDueDate() {
        if (dueDate == null) return null;
        return dueDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }
}
