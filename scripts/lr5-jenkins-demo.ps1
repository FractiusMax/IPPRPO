# lr5-jenkins-demo.ps1
# ЛР №5, Части 6 и 7 — полный цикл: изменение кода -> git -> Jenkins -> Static Analysis.
#
# Сценарий демонстрации (соответствует условию задания):
#
#   Фаза 1 (Part 6)  финальный код ЛР№5 -> commit -> push -> Jenkins -> SUCCESS
#   Фаза 2 (Part 7)  намеренное нарушение -> commit -> push -> Jenkins -> FAILURE
#                    (падает этап Static Analysis, Test и Package не выполняются)
#   Фаза 3 (Part 7)  исправление -> commit -> push -> Jenkins -> SUCCESS
#
# Jenkins запускается автоматически: у Job включён SCMTrigger (опрос раз в 2 минуты),
# поэтому скрипт НЕ запускает сборку вручную, а ждёт результата.
#
# Запуск:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\lr5-jenkins-demo.ps1
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\lr5-jenkins-demo.ps1 -Phase failure
#
# Ключи:
#   -Phase           all | success | failure | fix      (по умолчанию all)
#   -Job             имя Job в Jenkins (по умолчанию Lab4-Hotel)
#   -JenkinsHome     каталог JENKINS_HOME (по умолчанию C:\ProgramData\Jenkins\.jenkins)
#   -TimeoutMinutes  сколько ждать завершения сборки (по умолчанию 12)

param(
    [ValidateSet('all', 'success', 'failure', 'fix')][string]$Phase = 'all',
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
                # Сборка ещё не успела записать лог с нужным commit — подождём.
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
    # git пишет предупреждения (LF -> CRLF) в stderr, и при $ErrorActionPreference='Stop'
    # PowerShell считает их фатальной ошибкой. Поэтому временно переключаем режим
    # и опираемся на реальный код возврата git.
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

function Push-And-Wait([string]$CommitMessage, [string]$Expect) {
    Write-Step "git add -> git commit -> git push ($CommitMessage)"

    # Список уже существующих сборок снимаем ДО push — иначе Jenkins может
    # успеть начать сборку между push и этим снимком, и мы будем ждать вечно.
    $known = Get-ExistingBuildNumbers

    Invoke-Git @('-C', $ProjectDir, 'add', '-A') | Out-Null
    Invoke-Git @('-C', $ProjectDir, 'commit', '-m', $CommitMessage) | Out-Null
    Invoke-Git @('-C', $ProjectDir, 'push', 'origin', 'main') | Out-Null

    $sha = ((Invoke-Git @('-C', $ProjectDir, 'rev-parse', 'HEAD')) | Out-String).Trim()
    Write-Host "Зафиксированный и отправленный commit: $sha"
    Write-Host "Ожидаемый результат сборки: $Expect"

    $build = Wait-ForBuild -Known $known -Sha $sha

    Write-Host ''
    Write-Host "Сборка #$($build.Number)  результат: $($build.Result)"
    Write-Host '--- фрагменты Console Output ---'
    ($build.Log -split "`n") |
        Where-Object { $_ -match 'Checking out Revision|Stage: |^\[Pipeline\] \{ \(Stage|BUILD SUCCESS|BUILD FAILURE|There are \d+ errors|violation|ERROR\].*\.java|Finished:' } |
        Select-Object -First 30 |
        ForEach-Object { Write-Host "  $($_.Trim())" }

    $ok = ($build.Result -eq $Expect)
    if (-not $ok) {
        Write-Warning "Ожидался результат '$Expect', получен '$($build.Result)'."
    }
    return $build
}

# ---------------------------------------------------------------------------
Write-Step "ЛР №5, Части 6-7. Полный цикл: код -> git -> Jenkins (Job '$Job')"

if ($Phase -eq 'all' -or $Phase -eq 'success') {
    Write-Host ''
    Write-Host '>>> ФАЗА 1: финальный код ЛР№5 -> commit -> push -> Jenkins -> SUCCESS'
    Push-And-Wait 'ЛР №5: статический анализ (Checkstyle) и журналирование (SLF4J + Logback)' 'SUCCESS' | Out-Null
}

if ($Phase -eq 'all' -or $Phase -eq 'failure') {
    Write-Host ''
    Write-Host '>>> ФАЗА 2: намеренное нарушение -> commit -> push -> Jenkins -> FAILURE'
    Write-Host '    (нарушение вносится скриптом, бизнес-логика не затрагивается)'

    $demo = Join-Path $ProjectDir 'scripts\lr5-checkstyle-demo.ps1'
    & powershell -ExecutionPolicy Bypass -File $demo -KeepViolation | Out-Null

    Push-And-Wait 'ЛР №5 (Part 7): намеренное нарушение правила Checkstyle для проверки реакции Jenkins' 'FAILURE' | Out-Null
}

if ($Phase -eq 'all' -or $Phase -eq 'fix') {
    Write-Host ''
    Write-Host '>>> ФАЗА 3: исправление -> commit -> push -> Jenkins -> SUCCESS'
    Write-Host '    (нарушение устраняется: строка, нарушающая LineLength, удаляется)'

    # Исправление — удаляем строку с намеренно длинным комментарием.
    # Важно: восстанавливать файл через `git checkout` нельзя — нарушение уже
    # закоммичено, поэтому checkout вернул бы его обратно.
    $target = Join-Path $ProjectDir 'src\main\java\hotel\model\Room.java'
    $lines = [System.IO.File]::ReadAllLines($target, [System.Text.Encoding]::UTF8)
    $kept = @($lines | Where-Object { $_ -notmatch 'НАРУШЕНИЕ ПРАВИЛА LineLength' })
    if ($kept.Count -eq $lines.Count) {
        Write-Warning 'Строка с нарушением не найдена — возможно, нарушение уже исправлено.'
    }
    $text = ($kept -join "`n") -replace "(`n){3,}", "`n`n"
    [System.IO.File]::WriteAllText($target, $text.TrimEnd() + "`n", (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "Нарушение устранено: удалено $($lines.Count - $kept.Count) строк."

    Push-And-Wait 'ЛР №5 (Part 7): исправление нарушения правила Checkstyle' 'SUCCESS' | Out-Null
}

Write-Step 'Готово. Все фазы выполнены.'
