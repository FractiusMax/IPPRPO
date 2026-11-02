# run-mvn.ps1 — обёртка для запуска Maven с корректным UTF-8 выводом в файл.
#
# Использование:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\run-mvn.ps1 `
#       -Args 'checkstyle:check' -Log 'D:\bbgg\hotel\_tools\cs.log'
#
# Параметры:
#   -Args      — аргументы Maven (обязательный)
#   -Log       — путь к файлу лога (по умолчанию — во временный каталог)
#   -WorkDir   — каталог проекта (по умолчанию D:\bbgg\hotel)
param(
    [Parameter(Mandatory = $true)][string]$Args,
    [string]$Log = '',
    [string]$WorkDir = 'D:\bbgg\hotel'
)

$ErrorActionPreference = 'Stop'

if (-not $Log) {
    $Log = Join-Path $env:TEMP ("mvn-" + ($Args -replace '[^A-Za-z0-9]+', '_') + ".log")
}

# Java должна писать UTF-8, иначе русские сообщения Checkstyle превращаются в «кракозябры».
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'

$rawLog = "$Log.raw"

# cmd.exe /c + перенаправление в файл: так вывод Maven не теряется и кодировка не искажается.
& cmd.exe /c "mvn -B $Args > `"$rawLog`" 2>&1"
$exitCode = $LASTEXITCODE

$header = "### CMD: mvn -B $Args`n### WORKDIR: $WorkDir`n### EXITCODE: $exitCode`n`n"
if (Test-Path $rawLog) {
    $body = [System.IO.File]::ReadAllText($rawLog, [System.Text.Encoding]::UTF8)
    [System.IO.File]::WriteAllText($Log, $header + $body, (New-Object System.Text.UTF8Encoding($false)))
    Remove-Item -LiteralPath $rawLog -Force
} else {
    [System.IO.File]::WriteAllText($Log, $header + '(вывод Maven пуст)', (New-Object System.Text.UTF8Encoding($false)))
}

Write-Host "EXITCODE=$exitCode  LOG=$Log"
exit $exitCode
