# lr6-metrics-demo.ps1
# ЛР №6, Части 3-5 — демонстрация мониторинга приложения "Отель":
#   сборка -> запуск в режиме --serve -> endpoint /metrics -> Target в Prometheus.
#
# Скрипт:
#   1. останавливает ранее запущенный экземпляр приложения
#      (иначе mvn clean не удалит JAR-библиотеки, занятые работающим процессом);
#   2. собирает проект (mvn clean package);
#   3. запускает приложение в режиме --serve (HTTP-сервер метрик на порту 8081);
#   4. показывает ответ endpoint'а /metrics;
#   5. показывает состояние Target в Prometheus (ожидается health=up);
#   6. выполняет несколько PromQL-запросов.
#
# Запуск:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\lr6-metrics-demo.ps1
#
# Ключи:
#   -ProjectDir   каталог проекта (по умолчанию D:\bbgg\hotel)
#   -AppUrl       адрес endpoint'а метрик (по умолчанию http://localhost:8081/metrics)
#   -PromUrl      адрес Prometheus (по умолчанию http://localhost:9090)
#   -KeepRunning  не останавливать приложение после демонстрации
#   -SkipBuild    не пересобирать проект

param(
    [string]$ProjectDir = 'D:\bbgg\hotel',
    [string]$AppUrl = 'http://localhost:8081/metrics',
    [string]$PromUrl = 'http://localhost:9090',
    [switch]$KeepRunning,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$Maven = 'C:\Program Files\Apache\Maven\apache-maven-3.9.16\bin\mvn.cmd'
$Jar = Join-Path $ProjectDir 'target\hotel-system-1.0-SNAPSHOT.jar'

function Write-Step([string]$Text) {
    Write-Host ''
    Write-Host ('=' * 72)
    Write-Host $Text
    Write-Host ('=' * 72)
}

function Stop-RunningApp {
    # Работающее приложение держит открытыми файлы в target\lib, поэтому
    # перед "mvn clean" его нужно остановить. В процессах Java на этой машине
    # один и тот же экземпляр приложения может числиться дважды
    # (обёртка javapath и сам JDK) — останавливаем оба.
    $targets = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -like '*hotel-system*' })
    foreach ($item in $targets) {
        Write-Host "Останавливаю экземпляр приложения, PID $($item.ProcessId)"
        Stop-Process -Id $item.ProcessId -Force -ErrorAction SilentlyContinue
    }
    if ($targets.Count -gt 0) { Start-Sleep -Seconds 3 }
    return $targets.Count
}

Write-Step 'ЛР №6. Части 3-5: метрики приложения -> /metrics -> Prometheus'

$stopped = Stop-RunningApp
if ($stopped -eq 0) {
    Write-Host 'Шаг 1. Запущенных экземпляров приложения не найдено.'
} else {
    Write-Host "Шаг 1. Остановлено экземпляров приложения: $stopped"
}

if (-not $SkipBuild) {
    Write-Host 'Шаг 2. Сборка проекта (mvn clean package)'
    & $Maven -B -f (Join-Path $ProjectDir 'pom.xml') clean package | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Сборка не удалась, код возврата $LASTEXITCODE" }
    Write-Host 'Сборка успешна.'
} else {
    Write-Host 'Шаг 2. Сборка пропущена (-SkipBuild).'
}

Write-Host ''
Write-Host 'Шаг 3. Запуск приложения в режиме --serve'
$process = Start-Process -FilePath 'java' `
    -ArgumentList '-jar', $Jar, '--serve' `
    -WorkingDirectory $ProjectDir `
    -RedirectStandardOutput (Join-Path $ProjectDir 'logs\lr6-app.out.log') `
    -RedirectStandardError (Join-Path $ProjectDir 'logs\lr6-app.err.log') `
    -WindowStyle Hidden -PassThru

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $null = Invoke-WebRequest -Uri $AppUrl -UseBasicParsing -TimeoutSec 3
        $ready = $true
        break
    } catch {
        $ready = $false
    }
}
if (-not $ready) { throw "Endpoint метрик $AppUrl не поднялся за 30 секунд." }
Write-Host "Приложение запущено, PID=$($process.Id), endpoint $AppUrl отвечает."

Write-Host ''
Write-Host 'Шаг 4. Ответ endpoint /metrics (метрики приложения)'
$response = Invoke-WebRequest -Uri $AppUrl -UseBasicParsing -TimeoutSec 15
($response.Content -split "`n") |
    Where-Object { ($_ -match '^(application_|hotel_)') -and ($_ -notmatch '_bucket') } |
    ForEach-Object { Write-Host "  $_" }

Write-Host ''
Write-Host 'Шаг 5. Состояние Target в Prometheus'
$targets = Invoke-RestMethod -Uri "$PromUrl/api/v1/targets?state=active" -TimeoutSec 15
$targets.data.activeTargets | ForEach-Object {
    Write-Host ("  job={0}  instance={1}  health={2}  lastError='{3}'" -f `
            $_.labels.job, $_.labels.instance, $_.health, $_.lastError)
}

Write-Host ''
Write-Host 'Шаг 6. PromQL-запросы'
foreach ($query in @('application_operations_total', 'application_operations_success_total',
        'application_operations_error_total', 'hotel_bookings_total',
        'hotel_bookings_conflict_total', 'hotel_bookings_error_total')) {
    $encoded = [uri]::EscapeDataString($query)
    $result = Invoke-RestMethod -Uri "$PromUrl/api/v1/query?query=$encoded" -TimeoutSec 15
    $value = $result.data.result[0].value[1]
    Write-Host ("  {0,-38} = {1}" -f $query, $value)
}

Write-Host ''
Write-Host 'Приложение оставлено запущенным: Prometheus и Grafana продолжают собирать метрики.'
Write-Host 'Остановить вручную: Get-CimInstance Win32_Process -Filter "Name=''java.exe''" |'
Write-Host '  Where-Object { $_.CommandLine -like ''*hotel-system*'' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }'

Write-Step 'Готово. Метрики приложения доступны, Target в состоянии UP.'
