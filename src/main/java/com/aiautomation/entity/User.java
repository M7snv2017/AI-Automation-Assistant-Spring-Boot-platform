package com.aiautomation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;

    @Builder.Default
    private String role = "ROLE_USER";

    @Builder.Default
    private String theme = "light";

    @Builder.Default
    private String language = "en";

    @Builder.Default
    private String aiModel = "qwen2.5-coder:14b";

    @Builder.Default
    private boolean notifyEmail = true;

    @Builder.Default
    private boolean notifyBrowser = true;

    private String gmailUsername;

    private String encryptedAppPassword;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
