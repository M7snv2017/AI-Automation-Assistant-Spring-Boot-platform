package com.aiautomation.repository;

import com.aiautomation.entity.AutomationExecution;
import com.aiautomation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutomationExecutionRepository extends JpaRepository<AutomationExecution, UUID> {

    Optional<AutomationExecution> findByMessageId(String messageId);

    List<AutomationExecution> findByUserOrderByCreatedAtDesc(User user);

    List<AutomationExecution> findByUserAndStatusOrderByCreatedAtDesc(User user, AutomationExecution.Status status);

    long countByUserAndStatus(User user, AutomationExecution.Status status);

    long countByUserAndStatusAndExecutedAtGreaterThanEqual(User user, AutomationExecution.Status status, LocalDateTime since);

    List<AutomationExecution> findByUserAndCreatedAtBetweenOrderByCreatedAtAsc(User user, LocalDateTime start, LocalDateTime end);
}
