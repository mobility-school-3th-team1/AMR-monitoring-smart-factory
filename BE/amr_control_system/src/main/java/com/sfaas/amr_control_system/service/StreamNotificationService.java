package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.websocket.StreamEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class StreamNotificationService {

    private final StreamEventPublisher streamEventPublisher;
    private final DashboardService dashboardService;

    public void publishAmrStatusChangeAfterCommit(String amrId, String status, String faultCode) {
        runAfterCommit(() -> {
            streamEventPublisher.publishAmrStatusUpdated(amrId, status, faultCode);
            streamEventPublisher.publishDashboardSummaryUpdated(dashboardService.getSummary());
        });
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}
