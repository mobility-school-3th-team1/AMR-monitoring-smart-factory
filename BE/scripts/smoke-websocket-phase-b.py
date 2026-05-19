#!/usr/bin/env python3
"""Phase B WebSocket Docker smoke (B-4). Run from BE/: python scripts/smoke-websocket-phase-b.py"""

from __future__ import annotations

import json
import sys
import threading
import time
import urllib.error
import urllib.request
from typing import List

try:
    from websocket import WebSocketBadStatusException, create_connection
except ImportError:
    print("Install dependency: pip install websocket-client", file=sys.stderr)
    sys.exit(1)

BASE_URL = "http://localhost:8080/api/v1"
RECOVERY_WAIT_SECONDS = 65


def step(message: str) -> None:
    print(f"[smoke] {message}")


def http_json(method: str, path: str, body: dict | None = None, token: str | None = None) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None if body is None else json.dumps(body).encode("utf-8")
    request = urllib.request.Request(f"{BASE_URL}{path}", data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=15) as response:
        raw = response.read().decode("utf-8")
        return json.loads(raw) if raw else {}


def assert_event(messages: List[str], event_name: str, pattern: str | None = None, label: str = "") -> None:
    for message in messages:
        payload = json.loads(message)
        if payload.get("event") != event_name:
            continue
        if pattern is None or pattern in message:
            return
    joined = " | ".join(messages)
    raise AssertionError(f"Missing {label or event_name}. Got: {joined}")


def main() -> None:
    step("Health check")
    health = http_json("GET", "/actuator/health")
    if health.get("status") != "UP":
        raise AssertionError(f"Health not UP: {health}")

    step("Login")
    login = http_json("POST", "/auth/login", {"username": "admin", "password": "demo123"})
    token = login.get("accessToken")
    if not token:
        raise AssertionError("No accessToken in login response")

    step("WebSocket reject without token")
    try:
        create_connection("ws://localhost:8080/api/v1/stream", timeout=5)
        raise AssertionError("Expected connection without token to fail")
    except (WebSocketBadStatusException, OSError):
        step("OK: connection without token rejected")

    step("WebSocket connect with token")
    messages: List[str] = []
    stop_event = threading.Event()

    def receive_loop(ws) -> None:
        ws.settimeout(0.5)
        while not stop_event.is_set():
            try:
                messages.append(ws.recv())
            except Exception:
                time.sleep(0.05)

    ws = create_connection(f"ws://localhost:8080/api/v1/stream?token={token}", timeout=10)
    receiver = threading.Thread(target=receive_loop, args=(ws,), daemon=True)
    receiver.start()
    time.sleep(1)

    assert_event(messages, "stream.connected", label="stream.connected")
    step("OK: stream.connected received")

    baseline_count = len(messages)

    step("POST emergencyStop on amr-02")
    command = http_json(
        "POST",
        "/amrs/amr-02/commands",
        {"command": "emergencyStop", "params": {}},
        token=token,
    )
    if not command.get("accepted"):
        raise AssertionError("emergencyStop not accepted")

    time.sleep(2)
    stop_messages = messages[baseline_count:]
    assert_event(stop_messages, "amrs.status.updated", "EMERGENCY_STOP", "amrs.status.updated (EMERGENCY_STOP)")
    assert_event(stop_messages, "dashboard.summary.updated", label="dashboard.summary.updated after emergencyStop")
    step("OK: emergencyStop WS events received")

    summary_after_stop = http_json("GET", "/dashboard/summary", token=token)
    step(
        f"Summary after stop: amrError={summary_after_stop.get('amrError')}, "
        f"amrErrorUnresolved={summary_after_stop.get('amrErrorUnresolved')}"
    )

    recovery_baseline = len(messages)
    step(f"Waiting {RECOVERY_WAIT_SECONDS}s for auto-recovery")
    time.sleep(RECOVERY_WAIT_SECONDS)

    time.sleep(2)
    recovery_messages = messages[recovery_baseline:]
    assert_event(recovery_messages, "amrs.status.updated", '"status":"IDLE"', "amrs.status.updated (IDLE) after recovery")
    assert_event(recovery_messages, "dashboard.summary.updated", label="dashboard.summary.updated after recovery")
    step("OK: auto-recovery WS events received")

    summary_after_recovery = http_json("GET", "/dashboard/summary", token=token)
    step(
        f"Summary after recovery: amrError={summary_after_recovery.get('amrError')}, "
        f"amrErrorUnresolved={summary_after_recovery.get('amrErrorUnresolved')}"
    )

    amr_after = http_json("GET", "/amrs/amr-02", token=token)
    if amr_after.get("status") != "IDLE":
        raise AssertionError(f"Expected amr-02 status IDLE, got {amr_after.get('status')}")
    step("OK: amr-02 status IDLE")

    stop_event.set()
    ws.close()
    print()
    print("Phase B WebSocket smoke: ALL PASSED")


if __name__ == "__main__":
    try:
        main()
    except urllib.error.URLError as error:
        print(f"[smoke] FAILED: {error}", file=sys.stderr)
        sys.exit(1)
    except AssertionError as error:
        print(f"[smoke] FAILED: {error}", file=sys.stderr)
        sys.exit(1)
