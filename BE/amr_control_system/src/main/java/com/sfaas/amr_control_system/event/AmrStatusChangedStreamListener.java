package com.sfaas.amr_control_system.event;

import com.sfaas.amr_control_system.service.DashboardService;
import com.sfaas.amr_control_system.websocket.StreamEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AmrStatusChangedStreamListener {

    private final StreamEventPublisher streamEventPublisher;
    private final DashboardService dashboardService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAmrStatusChanged(AmrStatusChangedEvent event) {
        streamEventPublisher.publishAmrStatusUpdated(event.getAmrId(), event.getStatus(), event.getFaultCode());
        streamEventPublisher.publishDashboardSummaryUpdated(dashboardService.getSummary());
    }
}
