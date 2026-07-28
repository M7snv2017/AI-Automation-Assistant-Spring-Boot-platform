package com.aiautomation.service;

import com.aiautomation.entity.ActivityLog;
import com.aiautomation.entity.AutomationAction;
import com.aiautomation.entity.User;
import com.aiautomation.repository.ActivityLogRepository;
import com.aiautomation.repository.AutomationActionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ActivityLogRepository activityLogRepository;
    private final AutomationActionRepository automationActionRepository;

    @Data
    @Builder
    public static class CombinedHistoryDto {
        private List<ActivityLog> activityLogs;
        private List<AutomationAction> automationHistory;
    }

    public CombinedHistoryDto getHistory(User user, String query, String filterType) {
        List<ActivityLog> logs = activityLogRepository.findByUserOrderByTimestampDesc(user);
        List<AutomationAction> automations = automationActionRepository.findByUserOrderByCreatedAtDesc(user);

        if (filterType != null && !filterType.isBlank() && !"ALL".equalsIgnoreCase(filterType)) {
            logs = logs.stream()
                    .filter(l -> filterType.equalsIgnoreCase(l.getActionType()))
                    .toList();

            automations = automations.stream()
                    .filter(a -> filterType.equalsIgnoreCase(a.getActionType()))
                    .toList();
        }

        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            logs = logs.stream()
                    .filter(l -> l.getDescription() != null && l.getDescription().toLowerCase().contains(q))
                    .toList();

            automations = automations.stream()
                    .filter(a -> (a.getIntent() != null && a.getIntent().toLowerCase().contains(q)) ||
                                 (a.getPreviewSummary() != null && a.getPreviewSummary().toLowerCase().contains(q)))
                    .toList();
        }

        return CombinedHistoryDto.builder()
                .activityLogs(logs)
                .automationHistory(automations)
                .build();
    }
}
