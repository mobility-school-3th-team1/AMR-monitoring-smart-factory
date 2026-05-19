#!/usr/bin/env python3
"""Phase S Swagger smoke (S-2 auth/OpenAPI, S-3 REST regression). Run from BE/: python scripts/smoke-swagger-phase-s.py"""

from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request

BASE_URL = "http://localhost:8080/api/v1"

EXPECTED_CONTROLLER_TAGS = (
    "auth-controller",
    "dashboard-controller",
    "amr-controller",
    "alarm-controller",
    "charging-controller",
    "work-history-controller",
    "analytics-controller",
)

PROTECTED_SAMPLES = (
    ("GET", "/dashboard/summary"),
    ("GET", "/amrs"),
    ("GET", "/alarms"),
    ("GET", "/charging/stations"),
    ("GET", "/work-histories"),
    ("GET", "/analytics/kpis"),
)


def step(message: str) -> None:
    print(f"[smoke] {message}")


def http_request(
    method: str,
    path: str,
    body: dict | None = None,
    token: str | None = None,
) -> tuple[int, dict | str]:
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
            payload: dict | str = json.loads(raw) if raw else {}
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


def fetch_openapi() -> dict:
    status, payload = http_request("GET", "/v3/api-docs")
    assert_status("OpenAPI docs", status, 200)
    if not isinstance(payload, dict):
        raise AssertionError("OpenAPI docs response is not JSON")
    return payload


def collect_operation_tags(openapi: dict) -> set[str]:
    tags: set[str] = set()
    for path_item in openapi.get("paths", {}).values():
        for operation in path_item.values():
            if isinstance(operation, dict) and "tags" in operation:
                tags.update(operation["tags"])
    return tags


def assert_public_auth_operations(openapi: dict) -> None:
    paths = openapi.get("paths", {})
    for path in ("/auth/login", "/auth/refresh"):
        post = paths.get(path, {}).get("post", {})
        security = post.get("security")
        if security != []:
            raise AssertionError(f"{path} should have empty security (public), got {security!r}")


def main() -> None:
    step("Health check")
    status, health = http_request("GET", "/actuator/health")
    assert_status("Health", status, 200)
    if not isinstance(health, dict) or health.get("status") != "UP":
        raise AssertionError(f"Health not UP: {health}")

    step("OpenAPI controller tags")
    openapi = fetch_openapi()
    document_tags = collect_operation_tags(openapi)
    for controller_tag in EXPECTED_CONTROLLER_TAGS:
        if controller_tag not in document_tags:
            raise AssertionError(f"Missing OpenAPI tag: {controller_tag}")
    step("OK: 7 REST controller groups exposed")

    step("Public auth operations in OpenAPI")
    assert_public_auth_operations(openapi)
    step("OK: /auth/login and /auth/refresh are public in spec")

    step("Login")
    status, login = http_request(
        "POST",
        "/auth/login",
        body={"username": "admin", "password": "demo123"},
    )
    assert_status("Login", status, 200)
    if not isinstance(login, dict) or not login.get("accessToken"):
        raise AssertionError(f"No accessToken in login response: {login}")
    token = login["accessToken"]
    step("OK: access token issued")

    step("Bad credentials")
    status, _ = http_request(
        "POST",
        "/auth/login",
        body={"username": "admin", "password": "wrong-password"},
    )
    assert_status("Login bad password", status, 401)

    step("Protected APIs without token")
    for method, path in PROTECTED_SAMPLES:
        status, _ = http_request(method, path)
        assert_status(f"{method} {path} (no auth)", status, 401)
    step("OK: protected APIs return 401 without Bearer token")

    step("Protected APIs with Bearer token")
    for method, path in PROTECTED_SAMPLES:
        status, _ = http_request(method, path, token=token)
        assert_status(f"{method} {path} (with auth)", status, 200)
    step("OK: representative protected APIs return 200 with token")

    print("\nPhase S Swagger smoke (S-2): ALL PASSED")
    print("Swagger UI: http://localhost:8080/api/v1/swagger-ui/index.html")
    print("  1) POST /auth/login (no Authorize) -> copy accessToken")
    print("  2) Authorize -> paste token only (no 'Bearer ' prefix)")
    print("  3) Try GET /dashboard/summary etc.")


if __name__ == "__main__":
    try:
        main()
    except AssertionError as error:
        print(f"\nPhase S Swagger smoke (S-2): FAILED\n{error}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as error:
        print(f"\nPhase S Swagger smoke (S-2): FAILED\n{error}", file=sys.stderr)
        sys.exit(1)
