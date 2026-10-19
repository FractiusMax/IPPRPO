# break-test.ps1 / restore-test.ps1 — безопасная ломалка теста для ЛР №3.
#
# ВАЖНО: файл пишется БЕЗ BOM. В PowerShell 5.1 команда
#   Set-Content -Encoding UTF8
# добавляет BOM, и javac потом падает с
# "class, interface, enum, or record expected".
# Поэтому используем [System.IO.File]::WriteAllText с UTF8Encoding($false).
#
# Использование (Build #2 — демонстрация падения):
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\break-test.ps1
#   ... запустить Build Now в Jenkins, снять Console Output ...
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\restore-test.ps1
#   ... запустить Build Now снова (Build #3) ...

param(
    [switch]$Restore
)

$ErrorActionPreference = 'Stop'
$target = 'D:\bbgg\hotel\src\test\java\hotel\BookingServiceTest.java'
$backup = "$target.lab3.bak"

if (-not (Test-Path $target)) {
    throw "Test ne nayden: $target"
}

if ($Restore) {
        if (-not (Test-Path $backup)) {
                throw "Net rezervnoy kopii: $backup. Nechego vosstanavlivat."
            }
        Copy-Item -LiteralPath $backup -Destination $target -Force
        Write-Host "OK. Test vosstanovlen iz rezerva."
        Write-Host "Teper v Jenkins: Lab3-Hotel -> Build Now (budet Build #3, dolzhen byt SUCCESS)."
        exit 0
    }

# --- Lomayem ---
if (-not (Test-Path $backup)) {
        Copy-Item -LiteralPath $target -Destination $backup -Force
        Write-Host "Sdelan rezerv: $backup"
    }

$text = [System.IO.File]::ReadAllText($target, [System.Text.Encoding]::UTF8)
$from = 'assertEquals(1, repository.findAll().size());'
$to   = 'assertEquals(2, repository.findAll().size()); // LAB3 DEMO: namerenno slomano'

if ($text -notlike "*$from*") {
        throw "Ne nashli stroku '$from'. Test uzhe sloman ili izmenen ruku. Otkroy fail i prav vruchnuyu."
    }

$text = $text.Replace($from, $to)
# UTF8Encoding($false) = bez BOM, inache javac ne skompiliruet
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($target, $text, $utf8NoBom)

Write-Host "OK. Test sloman: $from  ->  $to"
Write-Host ''
Write-Host 'Teper: Jenkins -> Lab3-Hotel -> Build Now.'
Write-Host 'Ozhidaemy rezultat: Build = SUCCESS, Test = FAILURE,'
Write-Host 'Package i Verify JAR propuskany, v konsoli: expected: <2> but was: <1>.'
Write-Host ''
Write-Host 'POSLE snima Console Output:'
Write-Host '  powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\break-test.ps1 -Restore'
exit 0