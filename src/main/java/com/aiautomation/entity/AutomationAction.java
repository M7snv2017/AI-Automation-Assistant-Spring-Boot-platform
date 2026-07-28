package com.aiautomation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "automation_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String intent;

    @Column(nullable = false)
    private String actionType; // EMAIL, REMINDER, TASK, DOC_GEN, SUMMARIZE

    @Column(columnDefinition = "TEXT")
    private String parametersJson;

    @Column(columnDefinition = "TEXT")
    private String previewSummary;

    @Builder.Default
    private String status = "PENDING_CONFIRMATION"; // PENDING_QUESTIONS, PENDING_CONFIRMATION, EXECUTED, CANCELLED, FAILED

    @Builder.Default
    private boolean requiresConfirmation = true;

    @Column(columnDefinition = "TEXT")
    private String executionLog;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime executedAt;
}
