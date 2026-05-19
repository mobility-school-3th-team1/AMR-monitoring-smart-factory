package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.websocket.StreamEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
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
                    runBestEffort(action, true);
                }
            });
            return;
        }
        runBestEffort(action, false);
    }

    private void runBestEffort(Runnable action, boolean committedAfterTransaction) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (committedAfterTransaction) {
                log.warn("Stream notification failed after commit, but transaction commit is already finalized.", exception);
                return;
            }
            log.warn("Stream notification failed without active transaction context.", exception);
        }
    }
}
