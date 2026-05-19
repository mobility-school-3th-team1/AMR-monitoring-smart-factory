package com.sfaas.amr_control_system.websocket;

public final class WebSocketConstants {

    public static final String STREAM_PATH = "/stream";
    public static final String QUERY_PARAM_TOKEN = "token";
    public static final String EVENT_STREAM_CONNECTED = "stream.connected";
    public static final String EVENT_AMRS_STATUS_UPDATED = "amrs.status.updated";
    public static final String EVENT_DASHBOARD_SUMMARY_UPDATED = "dashboard.summary.updated";

    private WebSocketConstants() {
    }
}
