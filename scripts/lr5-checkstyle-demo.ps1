# lr5-checkstyle-demo.ps1
# ЛР №5, Часть 3 — «Эксперимент с ошибкой Static Analysis».
#
# Сценарий демонстрации:
#   1) в исходный код вносится намеренное нарушение правила LineLength
#      (безопасное для бизнес-логики: добавляется только строка комментария);
#   2) запускается статический анализ  ->  FAIL, в выводе видно файл, строку и правило;
#   3) нарушение исправляется;
#   4) статический анализ запускается повторно  ->  SUCCESS.
#
# Запуск:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\lr5-checkstyle-demo.ps1
#
# Ключ -KeepViolation оставляет нарушение в коде (нужно для Части 6/7 —
# чтобы закоммитить нарушение и отдать его Jenkins). По умолчанию файл
# восстанавливается автоматически, даже если что-то пошло не так.

param(
    [switch]$KeepViolation,
    [string]$ProjectDir = 'D:\bbgg\hotel',
    [string]$OutDir = ''
)

$ErrorActionPreference = 'Stop'

if (-not $OutDir) { $OutDir = Join-Path $ProjectDir '_tools\demo-checkstyle' }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$target      = Join-Path $ProjectDir 'src\main\java\hotel\model\Room.java'
$backup      = Join-Path $OutDir 'Room.java.backup'
$violation   = '    // НАРУШЕНИЕ ПРАВИЛА LineLength (демонстрация статического анализа): эта строка намеренно длиннее 120 символов, чтобы Checkstyle сообщил "Line is longer than allowed".'
$mvnScript   = Join-Path $ProjectDir 'scripts\run-mvn.ps1'

function Write-Step([string]$Text) {
    Write-Host ''
    Write-Host ('=' * 70)
    Write-Host $Text
    Write-Host ('=' * 70)
}

function Invoke-Checkstyle([string]$LogName) {
    $log = Join-Path $OutDir $LogName
    & powershell -ExecutionPolicy Bypass -File $mvnScript -Args 'checkstyle:check' -Log $log -WorkDir $ProjectDir | Out-Null
    return @{ ExitCode = $LASTEXITCODE; Log = $log }
}

Write-Step 'ЛР №5, Часть 3. Эксперимент: статический анализ реагирует на изменение кода'

# --- Шаг 0. Исходное состояние: анализ без нарушений -------------------------
Write-Step 'Шаг 0. Исходное состояние — запускаем mvn checkstyle:check'
$r0 = Invoke-Checkstyle '00-исходное-состояние.log'
Write-Host "Результат: exit code = $($r0.ExitCode) (0 = нарушений нет)"
Write-Host "Лог: $($r0.Log)"

# --- Шаг 1. Вносим нарушение --------------------------------------------------
Write-Step 'Шаг 1. Вносим намеренное нарушение правила LineLength'
Copy-Item -LiteralPath $target -Destination $backup -Force

$content = Get-Content -LiteralPath $target -Raw -Encoding UTF8
$marker  = '    public int getNumber() {'
if (-not $content.Contains($marker)) {
    throw "Не найден маркер '$marker' в файле $target"
}
$content = $content.Replace($marker, "$violation`n`n$marker")
[System.IO.File]::WriteAllText($target, $content, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Нарушение внесено в: $target"

try {
    # --- Шаг 2. Анализ должен завершиться ошибкой ----------------------------
    Write-Step 'Шаг 2. Запускаем mvn checkstyle:check — ожидаем FAIL'
    $r1 = Invoke-Checkstyle '01-нарушение-FAIL.log'
    Write-Host "Результат: exit code = $($r1.ExitCode) (не 0 = нарушение обнаружено)"
    Write-Host "Лог: $($r1.Log)"

    if ($r1.ExitCode -eq 0) {
        Write-Warning 'ВНИМАНИЕ: статический анализ не заметил нарушение — проверьте checkstyle.xml'
    }

    # Показываем найденные нарушения прямо в консоли.
    Write-Host ''
    Write-Host 'Обнаруженные нарушения (из отчёта Checkstyle):'
    $xml = Join-Path $ProjectDir 'target\checkstyle-result.xml'
    if (Test-Path $xml) {
        [xml]$report = Get-Content -LiteralPath $xml -Raw -Encoding UTF8
        foreach ($file in $report.checkstyle.file) {
            foreach ($err in $file.error) {
                Write-Host ("  {0}:{1}  [{2}]  {3}" -f $file.name, $err.line, $err.source, $err.message)
            }
        }
    }

    if ($KeepViolation) {
        Write-Step 'Режим -KeepViolation: нарушение ОСТАЁТСЯ в коде (для коммита в Git)'
        Write-Host "Резервная копия оригинала: $backup"
        exit 0
    }

    # --- Шаг 3. Исправляем нарушение -----------------------------------------
    Write-Step 'Шаг 3. Исправляем нарушение — возвращаем исходный файл'
    Copy-Item -LiteralPath $backup -Destination $target -Force
    Write-Host 'Нарушение устранено.'

    # --- Шаг 4. Повторный анализ — ожидаем SUCCESS ---------------------------
    Write-Step 'Шаг 4. Повторно запускаем mvn checkstyle:check — ожидаем SUCCESS'
    $r2 = Invoke-Checkstyle '02-исправление-SUCCESS.log'
    Write-Host "Результат: exit code = $($r2.ExitCode) (0 = нарушений нет)"
    Write-Host "Лог: $($r2.Log)"

    if ($r2.ExitCode -ne 0) {
        throw 'После исправления статический анализ всё ещё сообщает об ошибках.'
    }

    Write-Step 'Эксперимент завершён успешно: НАРУШЕНИЕ -> FAIL -> ИСПРАВЛЕНИЕ -> SUCCESS'
}
finally {
    if (-not $KeepViolation -and (Test-Path $backup)) {
        Copy-Item -LiteralPath $backup -Destination $target -Force
    }
}
