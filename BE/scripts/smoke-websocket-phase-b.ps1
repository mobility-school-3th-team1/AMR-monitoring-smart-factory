# Phase B WebSocket Docker smoke (B-4)
# Usage: from BE/: powershell -File scripts/smoke-websocket-phase-b.ps1

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080/api/v1"
$RecoveryWaitSeconds = 65

function Write-Step([string]$Message) {
    Write-Host "[smoke] $Message"
}

function Receive-WebSocketMessages {
    param(
        [System.Net.WebSockets.ClientWebSocket]$Socket,
        [int]$TimeoutMs
    )
    $buffer = New-Object byte[] 16384
    $messages = [System.Collections.Generic.List[string]]::new()
    $deadline = [DateTime]::UtcNow.AddMilliseconds($TimeoutMs)

    while ([DateTime]::UtcNow -lt $deadline -and $Socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        $segment = [ArraySegment[byte]]::new($buffer)
        $cts = New-Object System.Threading.CancellationTokenSource
        $remainingMs = [Math]::Max(1, ($deadline - [DateTime]::UtcNow).TotalMilliseconds)
        $cts.CancelAfter([int][Math]::Min(500, $remainingMs))

        try {
            $result = $Socket.ReceiveAsync($segment, $cts.Token).GetAwaiter().GetResult()
        } catch [System.OperationCanceledException] {
            continue
        } catch [System.AggregateException] {
            $inner = $_.Exception.InnerException
            if ($inner -is [System.OperationCanceledException]) {
                continue
            }
            throw
        }

        if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) {
            break
        }
        $text = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count)
        $messages.Add($text)
    }

    return @($messages | ForEach-Object { "$_" })
}

function Assert-EventPresent {
    param(
        [object]$Messages,
        [string]$EventName,
        [string]$Pattern,
        [string]$FailureLabel
    )
    $match = @($Messages) | Where-Object { $_ -is [string] } | Where-Object {
        $_ -match ('"event"\s*:\s*"' + [regex]::Escape($EventName) + '"')
    }
    if ($Pattern) {
        $match = $match | Where-Object { $_ -match $Pattern }
    }
    if (-not $match) {
        $joined = ($Messages | ForEach-Object { $_ }) -join " | "
        throw "Missing $FailureLabel. Got: $joined"
    }
}

Write-Step "Health check"
$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get
if ($health.status -ne "UP") {
    throw "Health not UP: $($health | ConvertTo-Json -Compress)"
}

Write-Step "Login"
$loginBody = @{ username = "admin"; password = "demo123" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $login.accessToken
if (-not $token) {
    throw "No accessToken in login response"
}

Write-Step "WebSocket reject without token"
$badSocket = [System.Net.WebSockets.ClientWebSocket]::new()
$badUri = [Uri]::new("ws://localhost:8080/api/v1/stream")
$rejected = $false
try {
    $badSocket.ConnectAsync($badUri, [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
    if ($badSocket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        throw "Expected connection without token to fail"
    }
} catch {
    $rejected = $true
    Write-Step "OK: connection without token rejected"
} finally {
    if ($badSocket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        $badSocket.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "", [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
    }
    $badSocket.Dispose()
}
if (-not $rejected -and $badSocket.State -ne [System.Net.WebSockets.WebSocketState]::Open) {
    Write-Step "OK: connection without token not open"
}

Write-Step "WebSocket connect with token"
$socket = [System.Net.WebSockets.ClientWebSocket]::new()
$wsUri = [Uri]::new("ws://localhost:8080/api/v1/stream?token=$([uri]::EscapeDataString($token))")
$socket.ConnectAsync($wsUri, [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
if ($socket.State -ne [System.Net.WebSockets.WebSocketState]::Open) {
    throw "WebSocket did not open"
}

$initialMessages = Receive-WebSocketMessages -Socket $socket -TimeoutMs 4000
Assert-EventPresent -Messages $initialMessages -EventName "stream.connected" -FailureLabel "stream.connected"
Write-Step "OK: stream.connected received"

$headers = @{ Authorization = "Bearer $token" }

Write-Step "POST emergencyStop on amr-02"
$commandBody = @{ command = "emergencyStop"; params = @{} } | ConvertTo-Json
$command = Invoke-RestMethod -Uri "$BaseUrl/amrs/amr-02/commands" -Method Post -ContentType "application/json" -Headers $headers -Body $commandBody
if (-not $command.accepted) {
    throw "emergencyStop not accepted"
}

$stopMessages = Receive-WebSocketMessages -Socket $socket -TimeoutMs 6000
Assert-EventPresent -Messages $stopMessages -EventName "amrs.status.updated" -Pattern "EMERGENCY_STOP" -FailureLabel "amrs.status.updated (EMERGENCY_STOP)"
Assert-EventPresent -Messages $stopMessages -EventName "dashboard.summary.updated" -FailureLabel "dashboard.summary.updated after emergencyStop"
Write-Step "OK: emergencyStop WS events received"

$summaryAfterStop = Invoke-RestMethod -Uri "$BaseUrl/dashboard/summary" -Method Get -Headers $headers
Write-Step "Summary after stop: amrError=$($summaryAfterStop.amrError), amrErrorUnresolved=$($summaryAfterStop.amrErrorUnresolved)"

Write-Step "Waiting ${RecoveryWaitSeconds}s for auto-recovery"
Start-Sleep -Seconds $RecoveryWaitSeconds

$recoveryMessages = Receive-WebSocketMessages -Socket $socket -TimeoutMs 10000
Assert-EventPresent -Messages $recoveryMessages -EventName "amrs.status.updated" -Pattern '"status"\s*:\s*"IDLE"' -FailureLabel "amrs.status.updated (IDLE) after recovery"
Assert-EventPresent -Messages $recoveryMessages -EventName "dashboard.summary.updated" -FailureLabel "dashboard.summary.updated after recovery"
Write-Step "OK: auto-recovery WS events received"

$summaryAfterRecovery = Invoke-RestMethod -Uri "$BaseUrl/dashboard/summary" -Method Get -Headers $headers
Write-Step "Summary after recovery: amrError=$($summaryAfterRecovery.amrError), amrErrorUnresolved=$($summaryAfterRecovery.amrErrorUnresolved)"

$amrAfter = Invoke-RestMethod -Uri "$BaseUrl/amrs/amr-02" -Method Get -Headers $headers
if ($amrAfter.status -ne "IDLE") {
    throw "Expected amr-02 status IDLE after recovery, got $($amrAfter.status)"
}
Write-Step "OK: amr-02 status IDLE"

$socket.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, "smoke-done", [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
$socket.Dispose()

Write-Host ""
Write-Host "Phase B WebSocket smoke: ALL PASSED" -ForegroundColor Green
