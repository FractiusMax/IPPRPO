# lr5-logging-demo.ps1
# ЛР №5, Часть 5 — «Проверка Logging».
#
# Сценарий:
#   1) собираем проект (mvn clean package) — получаем исполняемый JAR;
#   2) запускаем приложение — в консоли видны сообщения уровней INFO, WARN и ERROR;
#   3) показываем содержимое файла журнала, который пишет Logback.
#
# Запуск:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\lr5-logging-demo.ps1

param(
    [string]$ProjectDir = 'D:\bbgg\hotel'
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$Text) {
    Write-Host ''
    Write-Host ('=' * 72)
    Write-Host $Text
    Write-Host ('=' * 72)
}

$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'

Write-Step 'Шаг 1. Сборка проекта: mvn clean package'
& powershell -ExecutionPolicy Bypass -File (Join-Path $ProjectDir 'scripts\run-mvn.ps1') `
    -Args 'clean package' -WorkDir $ProjectDir -Log (Join-Path $ProjectDir '_tools\demo-logging-build.log')
if ($LASTEXITCODE -ne 0) {
    throw 'Сборка не удалась — см. лог.'
}

Write-Step 'Шаг 2. Запуск приложения: java -jar target\hotel-system-1.0-SNAPSHOT.jar'
Write-Host 'Сценарий 1 (успех)   -> INFO'
Write-Host 'Сценарий 2 (занято)  -> WARN'
Write-Host 'Сценарий 3 (ошибка)  -> ERROR'
Write-Host ''
Write-Host '--- начало вывода приложения ---'

$raw = Join-Path $ProjectDir '_tools\logging-demo.raw'
& cmd.exe /c "cd /d `"$ProjectDir`" && java -jar target\hotel-system-1.0-SNAPSHOT.jar > `"$raw`" 2>&1"
$exitCode = $LASTEXITCODE

$body = [System.IO.File]::ReadAllText($raw, [System.Text.Encoding]::UTF8)
Write-Host $body
Write-Host '--- конец вывода приложения ---'
Write-Host "Код возврата приложения: $exitCode"

Write-Step 'Шаг 3. Файл журнала Logback (Console / Log file)'
$logFile = Join-Path $ProjectDir 'logs\hotel.log'
if (Test-Path $logFile) {
    Write-Host "Файл: $logFile"
    Write-Host ''
    Get-Content $logFile -Tail 12 -Encoding UTF8 | ForEach-Object { Write-Host "  $_" }
} else {
    Write-Warning "Файл журнала не найден: $logFile"
}
