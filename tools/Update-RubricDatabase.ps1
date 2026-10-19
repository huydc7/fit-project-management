param()
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    $taskContainer = (& docker compose ps -q mysql).Trim()
    if ($LASTEXITCODE -ne 0 -or -not $taskContainer) { throw 'Start this project MySQL container first.' }
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot quanlydetai -e "SELECT COUNT(*) FROM DANG_KY_DE_TAI;"'
    if ($LASTEXITCODE -ne 0) { throw 'Scripts 01-05 must be installed before migration 06.' }
    # Stop application writes while taking the backup and changing triggers.
    & docker compose --profile app stop app
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop the application. No migration applied.' }
    $taskBackupDir = Join-Path $taskRoot 'backups'
    [IO.Directory]::CreateDirectory($taskBackupDir) | Out-Null
    $taskBackupPath = Join-Path $taskBackupDir ('before-rubric-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --events --set-gtid-purged=OFF --no-tablespaces --databases quanlydetai --result-file=/tmp/before-rubric.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed. No migration applied.' }
    & docker cp "${taskContainer}:/tmp/before-rubric.sql" $taskBackupPath
    if ($LASTEXITCODE -ne 0 -or (Get-Item -LiteralPath $taskBackupPath).Length -eq 0) { throw 'Backup copy failed. No migration applied.' }
    Write-Host "Backup saved: $taskBackupPath"
    & docker compose cp ./projects/Database/06_rubric_email_groups.sql mysql:/tmp/rubric06.sql
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy migration.' }
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot quanlydetai -e "source /tmp/rubric06.sql"'
    if ($LASTEXITCODE -ne 0) { throw 'Migration failed. Keep app stopped; inspect the error before retrying.' }
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot quanlydetai -e "SELECT COUNT(*) AS triggers_count FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE();"'
    if ($LASTEXITCODE -ne 0) { throw 'Migration verification failed.' }
    Write-Host 'Migration 06 completed. Rebuild/restart the new application: .\tools\Start-Local.ps1 -Docker'
} finally { Pop-Location }
