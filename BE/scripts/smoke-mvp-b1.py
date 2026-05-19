#!/usr/bin/env python3
"""FE 녹화 MVP B1 — §5.1 REST Docker smoke. Run from BE/: python scripts/smoke-mvp-b1.py"""

from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

BASE_URL = "http://localhost:8080/api/v1"

# docs/시연_MVP_합의.md §5.1 필수 REST
MVP_GET_ENDPOINTS = (
    "/dashboard/summary",
    "/dashboard/recent-alarms",
    "/dashboard/recent-logs",
    "/amrs",
    "/amrs/amr-01",
    "/charging/stations",
    "/charging/forecast",
    "/work-histories",
    "/analytics/workload",
)

SUMMARY_REQUIRED_FIELDS = (
    "amrOperating",
    "amrCharging",
    "amrWaiting",
    "amrError",
    "amrErrorUnresolved",
)


def step(message: str) -> None:
    print(f"[smoke] {message}")


def http_request(
    method: str,
    path: str,
    body: dict | None = None,
    token: str | None = None,
) -> tuple[int, dict | list | str]:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None if body is None else json.dumps(body).encode("utf-8")
    request = urllib.request.Request(
        f"{BASE_URL}{path}",
        data=data,
        headers=headers,
        method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            raw = response.read().decode("utf-8")
            payload: dict | list | str = json.loads(raw) if raw else {}
            return response.status, payload
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8")
        try:
            payload = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            payload = raw
        return error.code, payload


def assert_status(label: str, actual: int, expected: int) -> None:
    if actual != expected:
        raise AssertionError(f"{label}: expected HTTP {expected}, got {actual}")


def main() -> None:
    step("Health check")
    status, health = http_request("GET", "/actuator/health")
    assert_status("Health", status, 200)
    if not isinstance(health, dict) or health.get("status") != "UP":
        raise AssertionError(f"Health not UP: {health}")

    step("POST /auth/login")
    status, login = http_request(
        "POST",
        "/auth/login",
        body={"username": "admin", "password": "demo123"},
    )
    assert_status("Login", status, 200)
    if not isinstance(login, dict) or not login.get("accessToken"):
        raise AssertionError(f"No accessToken: {login}")
    token = login["accessToken"]
    step("OK: access token issued")

    step("MVP 5.1 GET endpoints (with Bearer)")
    for path in MVP_GET_ENDPOINTS:
        status, payload = http_request("GET", path, token=token)
        assert_status(f"GET {path}", status, 200)
        if not payload:
            raise AssertionError(f"GET {path}: empty response")
    step(f"OK: {len(MVP_GET_ENDPOINTS)} MVP GET paths return 200")

    step("GET /dashboard/summary - demo KPI fields")
    _, summary = http_request("GET", "/dashboard/summary", token=token)
    if not isinstance(summary, dict):
        raise AssertionError("summary is not an object")
    for field in SUMMARY_REQUIRED_FIELDS:
        if field not in summary:
            raise AssertionError(f"summary missing field: {field}")
    step("OK: amrError / amrErrorUnresolved present")

    step("GET /amrs?status=ERROR,EMERGENCY_STOP&sort=unresolvedFirst")
    status, filtered = http_request(
        "GET",
        "/amrs?status=ERROR,EMERGENCY_STOP&sort=unresolvedFirst",
        token=token,
    )
    assert_status("Filtered AMRs", status, 200)
    if not isinstance(filtered, dict) or "data" not in filtered:
        raise AssertionError("Filtered /amrs response missing data field")
    filtered_items = filtered["data"]
    if not isinstance(filtered_items, list):
        raise AssertionError("Filtered /amrs data is not an array")
    step(f"OK: error filter returned {len(filtered_items)} item(s)")

    step("POST /amrs/amr-02/commands emergencyStop")
    status, command = http_request(
        "POST",
        "/amrs/amr-02/commands",
        body={"command": "emergencyStop"},
        token=token,
    )
    assert_status("emergencyStop", status, 200)
    if not isinstance(command, dict) or command.get("accepted") is not True:
        raise AssertionError(f"emergencyStop not accepted: {command}")
    step("OK: accepted=true after DB commit")

    _, summary_after = http_request("GET", "/dashboard/summary", token=token)
    if not isinstance(summary_after, dict):
        raise AssertionError("summary after stop is not an object")
    if summary_after.get("amrError", 0) < 1:
        raise AssertionError(
            f"Expected amrError >= 1 after emergencyStop, got {summary_after.get('amrError')}"
        )
    step("OK: amrError reflects emergency stop")

    print("\nMVP B1 (section 5.1 REST) smoke: ALL PASSED")
    print("Endpoints verified:", ", ".join(MVP_GET_ENDPOINTS))
    print("Command: POST /amrs/amr-02/commands (emergencyStop)")


if __name__ == "__main__":
    try:
        main()
    except AssertionError as error:
        print(f"\nMVP B1 smoke: FAILED\n{error}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as error:
        print(f"\nMVP B1 smoke: FAILED\n{error}", file=sys.stderr)
        sys.exit(1)
