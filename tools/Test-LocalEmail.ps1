param([string]$MavenRepository = '')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    & docker compose -f compose.test.yaml --profile email up -d mailpit-test
    if ($LASTEXITCODE -ne 0) { throw 'Could not start isolated Mailpit. Check Docker Desktop and ports 11025/18025.' }
    $taskReady = $false
    for ($taskAttempt = 0; $taskAttempt -lt 20; $taskAttempt++) {
        try {
            $null = Invoke-RestMethod 'http://127.0.0.1:18025/api/v1/info' -TimeoutSec 2
            $taskReady = $true
            break
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $taskReady) { throw 'Mailpit did not become ready.' }
    Push-Location (Join-Path $taskRoot 'projects/quanlydetai')
    try {
        $taskArgs = @('-B','-ntp','-Dtest=MailpitDeliveryIT','test')
        if ($MavenRepository) { $taskArgs += "-Dmaven.repo.local=$MavenRepository" }
        & .\mvnw.cmd @taskArgs
        if ($LASTEXITCODE -ne 0) { throw 'Local SMTP verification failed. Inspect the Maven report above.' }
    } finally { Pop-Location }
    Write-Host 'SMTP checks passed. Synthetic test messages: http://localhost:18025'
    Write-Host 'The application database, .env and delivery setting were not changed.'
    Write-Host 'Stop the preview when finished: docker compose -f compose.test.yaml stop mailpit-test'
} finally { Pop-Location }
