# lr6-jenkins-demo.ps1
# ЛР №6, Части 11 и 12 — полный цикл: изменение кода -> git -> Jenkins -> метрики.
#
# Скрипт выполняет:
#   git add -> git commit -> git push -> ожидание автоматической сборки Jenkins
#   (Job настроен на "Pipeline script from SCM" с SCMTrigger, опрос раз в 2 минуты,
#   поэтому сборка запускается сама) -> проверка результата сборки.
#
# Запуск:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\lr6-jenkins-demo.ps1 `
#       -CommitMessage 'ЛР №6: мониторинг приложения'
#
# Ключи:
#   -CommitMessage   сообщение коммита (обязательный параметр)
#   -Expected        ожидаемый результат сборки: SUCCESS или FAILURE (по умолчанию SUCCESS)
#   -Job             имя Job в Jenkins (по умолчанию Lab4-Hotel)
#   -JenkinsHome     каталог JENKINS_HOME (по умолчанию C:\ProgramData\Jenkins\.jenkins)
#   -TimeoutMinutes  сколько ждать завершения сборки (по умолчанию 12)
#   -ProjectDir      каталог проекта (по умолчанию D:\bbgg\hotel)

param(
    [Parameter(Mandatory = $true)]
    [string]$CommitMessage,
    [ValidateSet('SUCCESS', 'FAILURE')]
    [string]$Expected = 'SUCCESS',
    [string]$Job = 'Lab4-Hotel',
    [string]$JenkinsHome = 'C:\ProgramData\Jenkins\.jenkins',
    [int]$TimeoutMinutes = 12,
    [string]$ProjectDir = 'D:\bbgg\hotel'
)

$ErrorActionPreference = 'Stop'

$buildsDir = Join-Path $JenkinsHome "jobs\$Job\builds"
if (-not (Test-Path $buildsDir)) {
    throw "Не найден каталог сборок Job '$Job': $buildsDir"
}

function Write-Step([string]$Text) {
    Write-Host ''
    Write-Host ('=' * 72)
    Write-Host $Text
    Write-Host ('=' * 72)
}

function Get-ExistingBuildNumbers {
    return @(Get-ChildItem $buildsDir -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^\d+$' } |
        Select-Object -ExpandProperty Name)
}

function Get-BuildResult([int]$Number) {
    $xml = Join-Path $buildsDir "$Number\build.xml"
    if (-not (Test-Path $xml)) { return $null }
    $content = Get-Content $xml -Raw -Encoding UTF8
    if ($content -match '<result>([^<]+)</result>') { return $Matches[1] }
    return $null
}

function Get-BuildLog([int]$Number) {
    $log = Join-Path $buildsDir "$Number\log"
    if (Test-Path $log) { return Get-Content $log -Raw -Encoding UTF8 }
    return ''
}

function Wait-ForBuild {
    param([string[]]$Known, [string]$Sha)

    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    Write-Host "Жду новую сборку (commit $Sha), максимум $TimeoutMinutes мин..."
    while ((Get-Date) -lt $deadline) {
        $current = Get-ExistingBuildNumbers
        $new = $current | Where-Object { $Known -notcontains $_ } | Sort-Object { [int]$_ }
        foreach ($number in $new) {
            $log = Get-BuildLog ([int]$number)
            if ($Sha -and $log -notmatch [regex]::Escape($Sha)) {
                continue
            }
            $result = Get-BuildResult ([int]$number)
            if ($result) {
                return @{ Number = [int]$number; Result = $result; Log = $log }
            }
        }
        Start-Sleep -Seconds 10
    }
    throw "Сборка для commit $Sha не завершилась за $TimeoutMinutes минут."
}

function Invoke-Git([string[]]$GitArgs) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $output = & git @GitArgs 2>&1
    $code = $LASTEXITCODE
    $ErrorActionPreference = $previous

    if ($code -ne 0) {
        throw "git $($GitArgs -join ' ') завершился с кодом $code`n$output"
    }
    return $output
}

Write-Step "ЛР №6. Части 11-12: git -> Jenkins (Job '$Job')"

$known = Get-ExistingBuildNumbers

Invoke-Git @('-C', $ProjectDir, 'add', '-A') | Out-Null
Invoke-Git @('-C', $ProjectDir, 'commit', '-m', $CommitMessage) | Out-Null
Invoke-Git @('-C', $ProjectDir, 'push', 'origin', 'main') | Out-Null

$sha = ((Invoke-Git @('-C', $ProjectDir, 'rev-parse', 'HEAD')) | Out-String).Trim()
Write-Host "Зафиксированный и отправленный commit: $sha"
Write-Host "Ожидаемый результат сборки: $Expected"

$build = Wait-ForBuild -Known $known -Sha $sha

Write-Host ''
Write-Host "Сборка #$($build.Number)  результат: $($build.Result)"
Write-Host '--- фрагменты Console Output ---'
($build.Log -split "`n") |
    Where-Object { $_ -match 'Checking out Revision|Stage: |^\[Pipeline\] \{ \(Stage|BUILD SUCCESS|BUILD FAILURE|Tests run:|You have \d+ Checkstyle violations|Finished:' } |
    Select-Object -First 30 |
    ForEach-Object { Write-Host "  $($_.Trim())" }

if ($build.Result -ne $Expected) {
    throw "Ожидался результат '$Expected', получен '$($build.Result)'."
}

Write-Step "Готово. Jenkins выполнил Pipeline: Checkout -> Build -> Static Analysis -> Test -> Package -> SUCCESS (сборка #$($build.Number))."
