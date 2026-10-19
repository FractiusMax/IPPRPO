# deploy-to-jenkins-workspace.ps1
# Razkladyvaet proekt iz D:\bbgg\hotel v D:\10K\workspace\Lab3-Hotel,
# chtoby Jenkins Job Lab3-Hotel smog vypolnit Jenkinsfile.
#
# Zapusk:
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\deploy-to-jenkins-workspace.ps1
#
# Skript sam sebi podnimet «Run-as administrator» cherez Start-Process -Verb RunAs,
# potomu chto:
#   1) Jenkins zaregistrirovan kak LocalSystem-servis (HKLM\...\services\jenkins);
#   2) Ego JENKINS_HOME — C:\ProgramData\Jenkins\.jenkins — dostupen tolko iz elevated-processa;
#      iz-za etogo workspace lezit v C:\ProgramData\Jenkins\.jenkins\workspace,
#      a ne tam, gde lezhit sam jenkins.exe (D:\10K).
#   3) Zapusk samogo-dzhe servisa jenkins trebuet admin-prav (sm. oshibku
#      "Cannot install profile" pri obychnom zapuske jenkins.exe).
#
# Esli ty ne mozhesh podtverdit UAC, skript zavershitsya s podskazkoy.

$ErrorActionPreference = 'Stop'
$srcRoot = 'D:\bbgg\hotel'
$dstRoot = 'C:\ProgramData\Jenkins\.jenkins\workspace\Lab3-Hotel'
$jobName = 'Lab3-Hotel'

# --- 1. Self-elevate -------------------------------------------------------
$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
$isAdmin = $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "=" * 60
    Write-Host "Skript zapushchen bez prav admina. Podnimayu sebya cherez UAC..."
    Write-Host "=" * 60
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'powershell.exe'
    $psi.Arguments = "-ExecutionPolicy Bypass -NoProfile -File `"$PSCommandPath`""
    $psi.Verb = 'runas'
    try {
        $proc = [System.Diagnostics.Process]::Start($psi)
        $proc.WaitForExit()
        exit $proc.ExitCode
    } catch {
        Write-Error "Не удалось повысить права (UAC отклонён?). Запусти PowerShell от имени администратора вручную и повтори."
        exit 1
    }
}

# --- 2. Proverka istochnika ------------------------------------------------
if (-not (Test-Path $srcRoot)) {
    throw "Ne najden iskhodnyj proekt: $srcRoot"
}

# --- 3. Start Jenkins-servisa ---------------------------------------------
$svc = Get-Service jenkins -ErrorAction SilentlyContinue
if ($null -eq $svc) {
    Write-Warning "Servis 'jenkins' ne najden. Prover', chto Jenkins voobshe ustanovlen."
} elseif ($svc.Status -ne 'Running') {
    Write-Host "[1/5] Zapuskayu servis jenkins..."
    try {
        Start-Service -Name jenkins -ErrorAction Stop
        # Podozhdem, poka Jenkins polnisyaset.'
        $ready = $false
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 2
            try {
                $resp = Invoke-WebRequest -Uri 'http://localhost:8080' -UseBasicParsing -TimeoutSec 3
                if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
                    $ready = $true; break
                }
            } catch { }
        }
        if ($ready) {
            Write-Host "    Jenkins otvechaet na http://localhost:8080"
        } else {
            Write-Warning "    Jenkins ne otvechaet cherez 60 s. Esli uzhno, podozhdi i zapusti skript zanovo."
        }
    } catch {
        Write-Warning "Ne udalos zapustit servis jenkins: $($_.Exception.Message)"
        Write-Warning "Zapusti vruchnuyu: services.msc -> Jenkins -> Start, potom povtori etot skript."
    }
} else {
    Write-Host "[1/5] Jenkins uzhe zapushchen (Status=Running)."
}

# --- 4. Rabota s workspace -------------------------------------------------
# D:\10K mozhet ne imetevno papki workspace, poka Jenkins ne zapushchen.
$parent = Split-Path $dstRoot -Parent
if (-not (Test-Path $parent)) {
    throw "Net dostupa k $parent. Znachit JENKINS_HOME ne tam. Popravilsete $dstRoot v etom skripte."
}

if (Test-Path $dstRoot) {
    Write-Host "[2/5] Ochishchayu staruyu sborku: $dstRoot"
    Remove-Item -LiteralPath $dstRoot -Recurse -Force
}

Write-Host "[3/5] Sozdayu katalog: $dstRoot"
New-Item -ItemType Directory -Path $dstRoot -Force | Out-Null

Write-Host "[4/5] Kopiruyu pom.xml i Jenkinsfile..."
Copy-Item -LiteralPath (Join-Path $srcRoot 'pom.xml')     -Destination (Join-Path $dstRoot 'pom.xml')
Copy-Item -LiteralPath (Join-Path $srcRoot 'Jenkinsfile') -Destination (Join-Path $dstRoot 'Jenkinsfile')

Write-Host "[5/5] Kopiruyu src/ (main i test)..."
Copy-Item -LiteralPath (Join-Path $srcRoot 'src') -Destination (Join-Path $dstRoot 'src') -Recurse -Force

# --- 5. Kontrolnaya proverka ----------------------------------------------
Write-Host "Gotovo. Struktura workspace posle kopirovaniya:"
Get-ChildItem -LiteralPath $dstRoot -Recurse -File |
    Where-Object { $_.FullName -notmatch '\\target\\' } |
    Select-Object -First 25 FullName |
    ForEach-Object { Write-Host "    $($_.FullName)" }

$jf = Join-Path $dstRoot 'Jenkinsfile'
if (Test-Path $jf) {
    Write-Host ""
    Write-Host "OK. Jenkinsfile lezhit v korne Job: $jf"
    Write-Host "Sleduyushchiy shag: otkroy Jenkins http://localhost:8080, sozdaj Job '$jobName'"
    Write-Host "(New Item -> Pipeline -> OK), v razdele Pipeline vyberish"
    Write-Host "'Pipeline script from SCM' / SCM = None / Script Path = Jenkinsfile."
} else {
    throw "Jenkinsfile ne popal v workspace - chto-to poshlo ne tak."
}