# Restart test for Windows: start server, sync a plan, kill the server, start it again, check the plan is still there.
# Run after: mvn package
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
$base = "http://localhost:8080"
$plan = [guid]::NewGuid().ToString()
$item = [guid]::NewGuid().ToString()
$now  = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()

function Wait-ForServer {
    for ($i = 0; $i -lt 20; $i++) {
        try { Invoke-RestMethod "$base/health" | Out-Null; return } catch { Start-Sleep -Milliseconds 500 }
    }
    throw "server did not start"
}

Write-Host "[1] start server"
$p = Start-Process java -ArgumentList "-jar","target/stemmate-server.jar" -PassThru -NoNewWindow
Wait-ForServer

Write-Host "[2] sync plan $plan"
Invoke-RestMethod "$base/sync?deviceId=restart-test" -Method Post `
    -Body "id=$item&action=SAVE_PLAN&planId=$plan&title=Restart%20test&safety=Teacher%20checks%20wiring&updatedAt=$now"

Write-Host "[3] kill server"
Stop-Process -Id $p.Id -Force; Start-Sleep 1

Write-Host "[4] start again"
$p = Start-Process java -ArgumentList "-jar","target/stemmate-server.jar" -PassThru -NoNewWindow
Wait-ForServer

Write-Host "[5] read it back"
$plans = Invoke-RestMethod "$base/plans?deviceId=restart-test"
Stop-Process -Id $p.Id -Force
if ($plans -match $plan) { Write-Host "PASS: plan survived a hard restart" }
else { Write-Host "FAIL: plan was lost after a hard restart"; exit 1 }
