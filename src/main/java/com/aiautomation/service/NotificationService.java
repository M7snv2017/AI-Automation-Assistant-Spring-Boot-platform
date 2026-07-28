package com.aiautomation.service;

import com.aiautomation.entity.Notification;
import com.aiautomation.entity.User;
import com.aiautomation.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndRead(user, false);
    }

    public void markAsRead(User user, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found."));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to notification.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type != null ? type : "INFO")
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }
}
