param([switch]$DockerDatabase, [switch]$RefreshExisting, [string]$EnvFile = '.env')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskEnvPath = if ([IO.Path]::IsPathRooted($EnvFile)) { $EnvFile } else { Join-Path $taskRoot $EnvFile }
if (-not (Test-Path -LiteralPath $taskEnvPath)) { throw 'Environment file not found.' }
$taskValues = @{}
foreach ($taskLine in Get-Content -LiteralPath $taskEnvPath -Encoding UTF8) {
    if ($taskLine -match '^\s*([A-Z_]+)\s*=(.*)$') {
        $taskKey = $Matches[1]; $taskValue = $Matches[2].Trim()
        if (($taskValue.StartsWith('"') -and $taskValue.EndsWith('"')) -or
            ($taskValue.StartsWith("'") -and $taskValue.EndsWith("'"))) {
            $taskValue = $taskValue.Substring(1, $taskValue.Length - 2)
        }
        $taskValues[$taskKey] = $taskValue
    }
}
$taskVariables = @('DB_URL','DB_USERNAME','DB_PASSWORD','SAMPLE_DATA_PASSWORD','SAMPLE_DATA_PATH','SAMPLE_DATA_REFRESH','SPRING_PROFILES_ACTIVE')
$taskPrevious = @{}
foreach ($taskKey in $taskVariables) { $taskPrevious[$taskKey] = [Environment]::GetEnvironmentVariable($taskKey,'Process') }
try {
    foreach ($taskKey in @('DB_URL','DB_USERNAME','DB_PASSWORD','SAMPLE_DATA_PASSWORD','SAMPLE_DATA_PATH')) {
        if ($taskValues.ContainsKey($taskKey)) { [Environment]::SetEnvironmentVariable($taskKey,$taskValues[$taskKey],'Process') }
    }
    if ($DockerDatabase) {
        $taskPort = if ($taskValues['MYSQL_PORT']) { $taskValues['MYSQL_PORT'] } else { '3307' }
        if ($taskPort -notmatch '^\d{1,5}$') { throw 'Invalid MYSQL_PORT.' }
        $env:DB_URL = "jdbc:mysql://127.0.0.1:${taskPort}/quanlydetai?serverTimezone=Asia/Ho_Chi_Minh"
        if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'quanlydetai' }
    }
    if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD in the selected environment file.' }
    if ($env:SAMPLE_DATA_PASSWORD.Length -lt 12 -or $env:SAMPLE_DATA_PASSWORD.Length -gt 64 -or
            [Text.Encoding]::UTF8.GetByteCount($env:SAMPLE_DATA_PASSWORD) -gt 72) {
        throw 'Set SAMPLE_DATA_PASSWORD to 12-64 characters, at most 72 UTF-8 bytes. Do not share this password.'
    }
    if (-not $env:SAMPLE_DATA_PATH) { $env:SAMPLE_DATA_PATH = Join-Path $taskRoot 'projects/Database/sample-data/fit-hcmute.json' }
    $env:SAMPLE_DATA_REFRESH = if ($RefreshExisting) { 'true' } else { 'false' }
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    Push-Location (Join-Path $taskRoot 'projects/quanlydetai')
    try {
        & .\mvnw.cmd -B -ntp spring-boot:test-run '-Dspring-boot.run.main-class=com.example.quanlydetai.tools.sample.SampleDataImportApplication' '-Dspring-boot.run.jvmArguments=-Duser.timezone=Asia/Ho_Chi_Minh'
        if ($LASTEXITCODE -ne 0) { throw 'Sample import failed. Check the error above.' }
    } finally { Pop-Location }
} finally {
    foreach ($taskKey in $taskVariables) { [Environment]::SetEnvironmentVariable($taskKey,$taskPrevious[$taskKey],'Process') }
}
