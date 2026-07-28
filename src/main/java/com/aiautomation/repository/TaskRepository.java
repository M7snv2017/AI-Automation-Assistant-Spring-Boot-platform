package com.aiautomation.repository;

import com.aiautomation.entity.Task;
import com.aiautomation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByUserOrderByCreatedAtDesc(User user);
    List<Task> findByUserAndCompletedOrderByCreatedAtDesc(User user, boolean completed);
    long countByUserAndCompleted(User user, boolean completed);
    long countByUser(User user);
    List<Task> findByUserAndDueDateBetweenOrderByDueDateAsc(User user, LocalDateTime start, LocalDateTime end);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.user WHERE t.completed = false AND t.dueDate <= :now")
    List<Task> findByCompletedFalseAndDueDateLessThanEqual(@Param("now") LocalDateTime now);

    long countByUserAndCompletedFalseAndDueDateGreaterThanEqual(User user, LocalDateTime now);
}
