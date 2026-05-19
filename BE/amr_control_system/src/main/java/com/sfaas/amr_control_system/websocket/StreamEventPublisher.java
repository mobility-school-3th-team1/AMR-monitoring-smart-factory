package com.sfaas.amr_control_system.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfaas.amr_control_system.dto.AmrStatusUpdatedEventDataDto;
import com.sfaas.amr_control_system.dto.DashboardSummaryDto;
import com.sfaas.amr_control_system.dto.StreamEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreamEventPublisher {

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, Object data) {
        TextMessage message = createMessage(eventType, data);
        if (message == null) {
            return;
        }
        for (WebSocketSession session : sessionRegistry.getOpenSessions()) {
            sendMessageToSession(eventType, message, session);
        }
    }

    public void publishToSession(WebSocketSession session, String eventType, Object data) {
        TextMessage message = createMessage(eventType, data);
        if (message == null) {
            return;
        }
        sendMessageToSession(eventType, message, session);
    }

    public void publishAmrStatusUpdated(String amrId, String status, String faultCode) {
        publish(
                WebSocketConstants.EVENT_AMRS_STATUS_UPDATED,
                new AmrStatusUpdatedEventDataDto(amrId, status, faultCode)
        );
    }

    public void publishDashboardSummaryUpdated(DashboardSummaryDto summary) {
        publish(WebSocketConstants.EVENT_DASHBOARD_SUMMARY_UPDATED, summary);
    }

    private TextMessage createMessage(String eventType, Object data) {
        StreamEventDto envelope = new StreamEventDto(eventType, Instant.now().toString(), data);
        try {
            return new TextMessage(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize stream event {}", eventType, exception);
            return null;
        }
    }

    private void sendMessageToSession(String eventType, TextMessage message, WebSocketSession session) {
        if (!session.isOpen()) {
            sessionRegistry.unregister(session);
            return;
        }
        try {
            session.sendMessage(message);
        } catch (IOException exception) {
            log.warn("Failed to send stream event {} to session {}", eventType, session.getId(), exception);
            sessionRegistry.unregister(session);
        }
    }
}
