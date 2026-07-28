package com.aiautomation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "automation_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationExecution {

    public enum Status {
        PENDING_CONFIRMATION,
        CONFIRMED,
        EXECUTED,
        CANCELLED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "message_id")
    private String messageId;

    @Column(nullable = false)
    private String actionType; // CREATE_TASKS, SEND_EMAIL, CREATE_REMINDER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(columnDefinition = "TEXT")
    private String executionResult;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime executedAt;
}
