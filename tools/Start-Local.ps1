param([switch]$Docker, [string]$EnvFile = '.env')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskEnvPath = if ([IO.Path]::IsPathRooted($EnvFile)) { $EnvFile } else { Join-Path $taskRoot $EnvFile }
if ($Docker) {
    Push-Location $taskRoot
    try {
        if (-not (Test-Path -LiteralPath $taskEnvPath)) { throw 'Create the selected environment file and set local passwords first.' }
        & docker compose --env-file $taskEnvPath --profile app up --build -d
        if ($LASTEXITCODE -ne 0) { throw 'Docker failed. Make sure Docker Desktop is running, then check compose logs.' }
        Write-Host 'Open http://localhost:8080 (or PORT from .env).'
    } finally { Pop-Location }
    exit
}
# Native Maven does not automatically load .env. Only recognized project variables are imported.
$allowedKeys = @('DB_URL','DB_USERNAME','DB_PASSWORD','PORT','STORAGE_BACKEND','LOCAL_STORAGE_PATH',
    'SUPABASE_URL','SUPABASE_KEY','SUPABASE_BUCKET',
    'EMAIL_ENABLED','EMAIL_BACKEND','EMAIL_FROM','EMAIL_API_KEY','PUBLIC_BASE_URL',
    'MAIL_HOST','MAIL_PORT','MAIL_USERNAME','MAIL_PASSWORD','MAIL_AUTH','MAIL_STARTTLS')
if (Test-Path -LiteralPath $taskEnvPath) {
    foreach ($entryLine in Get-Content -LiteralPath $taskEnvPath -Encoding UTF8) {
        if ($entryLine -match '^\s*([A-Z_]+)\s*=(.*)$') {
            $entryKey = $Matches[1]; $entryValue = $Matches[2].Trim()
            if ($allowedKeys -contains $entryKey) {
                if (($entryValue.StartsWith('"') -and $entryValue.EndsWith('"')) -or
                    ($entryValue.StartsWith("'") -and $entryValue.EndsWith("'"))) {
                    $entryValue = $entryValue.Substring(1,$entryValue.Length-2)
                }
                [Environment]::SetEnvironmentVariable($entryKey,$entryValue,'Process')
            }
        }
    }
} elseif ($EnvFile -ne '.env') { throw 'The selected environment file does not exist.' }
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { throw 'Set DB_PASSWORD in the selected environment file or the current environment.' }
if ([string]::IsNullOrWhiteSpace($env:LOCAL_STORAGE_PATH)) { $env:LOCAL_STORAGE_PATH = Join-Path $taskRoot 'uploads' }
$env:SPRING_PROFILES_ACTIVE = 'dev'
Push-Location (Join-Path $taskRoot 'projects/quanlydetai')
try {
    & .\mvnw.cmd -ntp spring-boot:run '-Dspring-boot.run.jvmArguments=-Duser.timezone=Asia/Ho_Chi_Minh'
    if ($LASTEXITCODE -ne 0) { throw 'Application failed. Check the startup error above (database/schema/JDK/password).' }
} finally { Pop-Location }
