param (
    [string]$Scenario = "menu",
    [string]$BaseUrl = "http://localhost:8080"
)

$K6_PATH = (Get-Command k6 -ErrorAction SilentlyContinue).Source
if (-not $K6_PATH) {
    if (Test-Path "C:\Program Files\k6\k6.exe") {
        $K6_PATH = "C:\Program Files\k6\k6.exe"
    } else {
        Write-Error "k6 is not found in PATH or C:\Program Files\k6\k6.exe"
        exit 1
    }
}

function Show-Header {
    Clear-Host
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "   AegisLedger - Enterprise Concurrency & Stress Suite    " -ForegroundColor Green
    Write-Host "   Tool: Grafana k6 + JUnit 5 Virtual Threads Engine      " -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Test-1000Payments {
    Write-Host "[1/3] Running 1,000 Concurrent Payments Load Test..." -ForegroundColor Green
    Write-Host "Target: $BaseUrl" -ForegroundColor Yellow
    & "$K6_PATH" run -e "BASE_URL=$BaseUrl" "benchmark\k6\test_1000_payments.js"
}

function Test-RaceCondition {
    Write-Host "[2/3] Running Race Condition / Double Spending Test..." -ForegroundColor Green
    Write-Host "Target: $BaseUrl" -ForegroundColor Yellow
    & "$K6_PATH" run -e "BASE_URL=$BaseUrl" "benchmark\k6\test_race_condition.js"
}

function Run-JUnitConcurrency {
    Write-Host "[3/3] Running JUnit In-Process Concurrency & Invariant Test..." -ForegroundColor Green
    & ".\mvnw.cmd" test -Dtest=ConcurrencyRaceConditionTest
}

if ($Scenario -eq "1000") {
    Test-1000Payments
} elseif ($Scenario -eq "race") {
    Test-RaceCondition
} elseif ($Scenario -eq "junit") {
    Run-JUnitConcurrency
} else {
    Show-Header
    Write-Host "Please select testing scenario:" -ForegroundColor White
    Write-Host "  [1] Test 1,000 Concurrent Payments (Stress & P99 Latency)" -ForegroundColor Cyan
    Write-Host "  [2] Test 20 Users Draining 1 Account (Race Condition / Double Spending)" -ForegroundColor Yellow
    Write-Host "  [3] Run JUnit In-Process Concurrency Test (No HTTP server required)" -ForegroundColor Magenta
    Write-Host "  [Q] Exit" -ForegroundColor Gray
    Write-Host ""
    $choice = Read-Host "Enter your choice [1-3, Q]"

    switch ($choice) {
        "1" { Test-1000Payments }
        "2" { Test-RaceCondition }
        "3" { Run-JUnitConcurrency }
        Default { Write-Host "Exited." }
    }
}
