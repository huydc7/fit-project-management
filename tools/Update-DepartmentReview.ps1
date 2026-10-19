param()
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    $taskContainer = ((& docker compose ps -q mysql) -join '').Trim()
    if ($LASTEXITCODE -ne 0 -or -not $taskContainer) { throw 'Start this project MySQL container first.' }
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot quanlydetai -e "SELECT COUNT(*) FROM DANG_KY_DE_TAI;"'
    if ($LASTEXITCODE -ne 0) { throw 'Install SQL 01-06 before migration 07.' }
    & docker compose --profile app stop app
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop application writes.' }
    $taskBackupDir = Join-Path $taskRoot 'backups'
    [IO.Directory]::CreateDirectory($taskBackupDir) | Out-Null
    $taskBackupPath = Join-Path $taskBackupDir ('before-department-review-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --triggers --events --set-gtid-purged=OFF --no-tablespaces --databases quanlydetai --result-file=/tmp/before-department-review.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed; no migration applied.' }
    & docker cp "${taskContainer}:/tmp/before-department-review.sql" $taskBackupPath
    if ($LASTEXITCODE -ne 0 -or (Get-Item -LiteralPath $taskBackupPath).Length -eq 0) { throw 'Backup copy failed; no migration applied.' }
    Write-Host "Backup saved: $taskBackupPath"
    & docker compose cp ./projects/Database/07_department_review.sql mysql:/tmp/department07.sql
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy migration.' }
    & docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot quanlydetai -e "source /tmp/department07.sql; SELECT COUNT(*) AS department_heads FROM TRUONG_BO_MON; SELECT COUNT(*) AS department_reviews FROM DUYET_BO_MON;"'
    if ($LASTEXITCODE -ne 0) { throw 'Migration failed. Keep app stopped and inspect the error.' }
    Write-Host 'Migration 07 completed. Rebuild the app, then appoint department heads from Admin > Accounts.'
} finally { Pop-Location }
