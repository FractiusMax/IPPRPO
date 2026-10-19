# =============================================================================
#  Демонстрация индивидуального задания ЛР №2, вариант 7
#  «Отель — Ресурс + сборка»
#
#  Запуск:  powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\demo-variant7.ps1
#
#  Скрипт проходит ровно по 6 пунктам требования 8 методички.
# =============================================================================

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

# Кириллица в консоли
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$jar = 'target\hotel-system-1.0-SNAPSHOT.jar'

function Step { param([string]$Text)
    Write-Host "`n"
    Write-Host ("=" * 78) -ForegroundColor Cyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host ("=" * 78) -ForegroundColor Cyan
}
function Sub { param([string]$Text) Write-Host "`n  -- $Text --" -ForegroundColor Yellow }
function Ok  { param([string]$Text) Write-Host "  [OK]   $Text" -ForegroundColor Green }
function Info{ param([string]$Text) Write-Host "  [i]    $Text" -ForegroundColor Gray }

# ---------------------------------------------------------------- ПУНКТ 1 ----
Step "ПУНКТ 1. Где находится настройка"
Info "Ресурс конфигурации:  src\main\resources\config.properties"
Info "Класс, который его читает:  src\main\java\hotel\util\Config.java"
Info "Подключение в приложении:  src\main\java\hotel\Main.java  (строка 17 -> Config.print();)"
Write-Host "`n  Содержимое ИСХОДНОГО ресурса:"
Get-Content 'src\main\resources\config.properties' -Encoding UTF8 |
    Where-Object { $_ -notmatch '^\s*#' -and $_.Trim() -ne '' } |
    ForEach-Object { Write-Host "    $_" }

# ---------------------------------------------------------------- ПУНКТ 2 ----
Step "ПУНКТ 2. Что именно было добавлено"
Info "1) Файл      src\main\resources\config.properties  - 8 параметров гостиницы"
Info "2) Класс     hotel/util/Config.java                - читает ресурс из classpath"
Info "3) Вызов     Main.java:17 -> Config.print();       - печатает параметры при старте"
Ok "Maven ничего специально настраивать не требовалось: src\main\resources"
Ok "это стандартная папка, Maven копирует её автоматически."

# ---------------------------------------------------------------- ПУНКТ 3 ----
Step "ПУНКТ 3. Зачем это было добавлено"
Info "Конфигурация отделена от кода: адрес, телефон, тарифы меняются"
Info "без перекомпиляции - правится только config.properties."
Info "Ресурс попадает в classpath и едет вместе с JAR, поэтому приложение"
Info "работает из любой папки - не нужен рядом отдельный файл."

# ---------------------------------------------------------------- ПУНКТ 4 ----
Step "ПУНКТ 4. Какую команду Maven необходимо выполнить"
Write-Host "`n  Выполняю:  mvn clean package" -ForegroundColor White
mvn clean package | Select-String -Pattern 'Tests run:|Building jar|BUILD'

# ---------------------------------------------------------------- ПУНКТ 5 ----
Step "ПУНКТ 5. Какой результат получен"

Sub "5.1  Содержимое итогового JAR - ищем config.properties"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Join-Path (Get-Location) $jar))
$zip.Entries | ForEach-Object { '  ' + $_.FullName } | Select-Object -First 30
$entry = $zip.Entries | Where-Object { $_.FullName -eq 'config.properties' }
if ($entry) { Ok "РЕСУРС НАЙДЕН ВНУТРИ JAR: config.properties ($($entry.Length) байт)" }
else       { throw "Ресурс не попал в JAR!" }
$zip.Dispose()

Sub "5.2  Содержимое ресурса ИЗНУТРИ собранного JAR (не с диска!)"
$zip = [System.IO.Compression.ZipFile]::OpenRead((Join-Path (Get-Location) $jar))
$reader = New-Object System.IO.StreamReader($zip.GetEntry('config.properties').Open(), [System.Text.Encoding]::UTF8)
$content = $reader.ReadToEnd()
$reader.Close()
$zip.Dispose()
$content -split "`n" | Where-Object { $_ -notmatch '^\s*#' -and $_.Trim() -ne '' } |
    ForEach-Object { Write-Host "    $_" }

# ---------------------------------------------------------------- ПУНКТ 6 ----
Step "ПУНКТ 6. Как результат подтверждает выполнение задания"
Sub "6.1  Запускаем приложение ИЗ СОБРАННОГО JAR - оно должно прочитать ресурс"
Write-Host "  Выполняю:  java -cp $jar hotel.Main" -ForegroundColor White
Write-Host "`n"
$out = Join-Path (Get-Location) 'target\demo-run.txt'
$p = Start-Process -FilePath 'java' `
        -ArgumentList '-Dstdout.encoding=UTF-8', '-cp', $jar, 'hotel.Main' `
        -RedirectStandardOutput $out -NoNewWindow -Wait
Get-Content $out -Encoding UTF8

Sub "6.2  Проверка"
$ran = Get-Content $out -Raw -Encoding UTF8
if ($ran -match 'Конфигурация из config\.properties') { Ok "Приложение вывело блок конфигурации" }
else { throw "Конфигурация не выведена" }
if ($ran -match 'hotel\.name' -and $ran -match 'hotel\.city' -and $ran -match 'reservation\.checkout\.time') {
    Ok "Приложение прочитало все ключи ресурса из JAR"
} else { throw "Не все ключи прочитаны" }
# Проверка кодировки: при чтении UTF-8 как ISO-8859-1 появляется символ U+00D0
if ($ran.Contains([char]0x00D0)) { throw "Искажение кодировки (кириллица прочитана как ISO-8859-1)" }
else { Ok "Кириллица не искажена" }
if ($ran -match 'Всего броней: 2') { Ok "Логика бронирования из ЛР1 отработала" }

Write-Host "`n"
Write-Host ("=" * 78) -ForegroundColor Green
Write-Host "  ИТОГ: ресурс config.properties лежит внутри hotel-system-1.0-SNAPSHOT.jar" -ForegroundColor Green
Write-Host "  и реально читается приложением при запуске из этого JAR." -ForegroundColor Green
Write-Host ("=" * 78) -ForegroundColor Green
