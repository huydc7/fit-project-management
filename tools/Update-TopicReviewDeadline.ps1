param()
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    $taskContainer = ((& docker compose ps -q mysql) -join '').Trim()
    if ($LASTEXITCODE -ne 0 -or -not $taskContainer) { throw 'Start this project MySQL container first.' }
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot quanlydetai -e "SELECT COUNT(*) FROM DUYET_BO_MON;"'
    if ($LASTEXITCODE -ne 0) { throw 'Install SQL 01-07 before migration 08.' }
    & docker compose --profile app stop app
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop application writes.' }
    $taskBackupDir = Join-Path $taskRoot 'backups'
    [IO.Directory]::CreateDirectory($taskBackupDir) | Out-Null
    $taskBackupPath = Join-Path $taskBackupDir ('before-topic-review-deadline-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --events --set-gtid-purged=OFF --no-tablespaces --databases quanlydetai --result-file=/tmp/before-topic-review-deadline.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed; no migration applied.' }
    & docker cp "${taskContainer}:/tmp/before-topic-review-deadline.sql" $taskBackupPath
    if ($LASTEXITCODE -ne 0 -or (Get-Item -LiteralPath $taskBackupPath).Length -eq 0) { throw 'Backup copy failed; no migration applied.' }
    Write-Host "Backup saved: $taskBackupPath"
    & docker compose cp ./projects/Database/08_topic_review_deadline.sql mysql:/tmp/review08.sql
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy migration.' }
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot quanlydetai -e "source /tmp/review08.sql; SELECT COUNT(*) AS department_heads FROM TRUONG_BO_MON; SELECT COUNT(*) AS department_reviews FROM DUYET_BO_MON;"'
    if ($LASTEXITCODE -ne 0) { throw 'Migration failed. Keep app stopped and inspect the error.' }
    Write-Host 'Migration 08 completed. Rebuild the application; topic approvals must finish before Begin_SV.'
} finally { Pop-Location }
