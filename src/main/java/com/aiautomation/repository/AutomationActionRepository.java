package com.aiautomation.repository;

import com.aiautomation.entity.AutomationAction;
import com.aiautomation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationActionRepository extends JpaRepository<AutomationAction, UUID> {
    List<AutomationAction> findByUserOrderByCreatedAtDesc(User user);
    List<AutomationAction> findByUserAndStatusOrderByCreatedAtDesc(User user, String status);
    long countByUserAndStatus(User user, String status);
    long countByUserAndActionTypeAndStatus(User user, String actionType, String status);
    long countByUserAndCreatedAtAfter(User user, LocalDateTime timestamp);
}
