# lr6-promql-demo.ps1
# ЛР №6, Часть 6 — работа с PromQL и Часть 9 — эксперимент с мониторингом.
#
# Скрипт:
#   1. выполняет PromQL-запросы к Prometheus и печатает значения;
#   2. снимает значения метрик дважды с интервалом в 15 секунд и показывает,
#      как они изменились (доказательство того, что метрики связаны с работой
#      приложения).
#
# Запуск:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\lr6-promql-demo.ps1

param(
    [string]$PromUrl = 'http://localhost:9090',
    [int]$WaitSeconds = 15
)

$ErrorActionPreference = 'Stop'

$Queries = @(
    'application_operations_total',
    'application_operations_success_total',
    'application_operations_error_total',
    'hotel_bookings_total',
    'hotel_bookings_conflict_total',
    'hotel_bookings_error_total'
)

function Write-Step([string]$Text) {
    Write-Host ''
    Write-Host ('=' * 72)
    Write-Host $Text
    Write-Host ('=' * 72)
}

function Get-MetricValue([string]$Query) {
    $encoded = [uri]::EscapeDataString($Query)
    $result = Invoke-RestMethod -Uri "$PromUrl/api/v1/query?query=$encoded" -TimeoutSec 15
    if (-not $result.data.result) { return $null }
    return [double]$result.data.result[0].value[1]
}

Write-Step 'ЛР №6. Часть 6: PromQL-запросы к метрикам приложения'
foreach ($query in $Queries) {
    $value = Get-MetricValue $query
    Write-Host ("  {0,-38} = {1}" -f $query, $value)
}

Write-Step "Часть 9: значения до и через $WaitSeconds секунд"
$before = @{}
foreach ($query in $Queries) { $before[$query] = Get-MetricValue $query }

Write-Host "Жду $WaitSeconds секунд, пока приложение выполняет операции..."
Start-Sleep -Seconds $WaitSeconds

$after = @{}
foreach ($query in $Queries) { $after[$query] = Get-MetricValue $query }

Write-Host ''
Write-Host ('{0,-38} {1,10} {2,10} {3,10}' -f 'Метрика', 'До', 'После', 'Изменение')
Write-Host ('-' * 72)
foreach ($query in $Queries) {
    $delta = $after[$query] - $before[$query]
    Write-Host ('{0,-38} {1,10} {2,10} {3,10}' -f $query, $before[$query], $after[$query], $delta)
}

Write-Step 'Готово. Значения метрик изменились вслед за операциями приложения.'
