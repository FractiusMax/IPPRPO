# add-maven-to-machine-path.ps1
# Dobavlyaet Maven v SISTEMNYI (Machine) PATH Windows, chtoby komanda `mvn`
# stala dostupna dlya servisa jenkins (kotoryj rabotaet pod LocalSystem).
#
# Pochemu eto nuzhno:
#   - Maven u tebya propisan tolko v USER PATH (tvoja lichnaya sessiya).
#   - Servis `jenkins` zapuskaetsya pod uchetnoy zapisyu LocalSystem,
#     kotoraya chitaet MACHINE PATH, a ne USER PATH.
#   - Poetomu v Console Output bylo: "mvn" ne yavlyaetsya vnutrenney
#     ili vneshney komandoy.
#
# Zapusk (skript sam sebi podnimet prava cherez UAC):
#   powershell -ExecutionPolicy Bypass -File D:\bbgg\hotel\scripts\add-maven-to-machine-path.ps1

$ErrorActionPreference = 'Stop'
$mavenBin = 'C:\Program Files\Apache\Maven\apache-maven-3.9.16\bin'

# --- 0. Self-elevate -------------------------------------------------------
$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Host 'Skript zapushchen bez prav admina. Podnimayu sebya cherez UAC...'
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = 'powershell.exe'
        $psi.Arguments = "-ExecutionPolicy Bypass -NoProfile -File `"$PSCommandPath`""
        $psi.Verb = 'runas'
        try {
            $p = [System.Diagnostics.Process]::Start($psi)
            $p.WaitForExit()
            exit $p.ExitCode
        } catch {
            Write-Error 'Ne udalos povysit prava (UAC otklonen?). Zapusti PowerShell ot imeni administratora vruchnuyu.'
            exit 1
        }
    }

# --- 1. Proverka mvn.cmd --------------------------------------------------
$mvn = Join-Path $mavenBin 'mvn.cmd'
if (-not (Test-Path $mvn)) {
        throw "mvn.cmd ne nayden po puti: $mvn. Poprav put v etom skripte."
    }
Write-Host "OK, mvn.cmd nayden: $mvn"

# --- 2. Est li uzhe v MACHINE PATH? ---------------------------------------
$machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
if ($machinePath -split ';' | Where-Object { $_.Trim() -ieq $mavenBin }) {
        Write-Host 'Maven uzhe est v MACHINE PATH. Nichego ne menyayu.'
        # Dazhe esli net — pokaжem itog
    } else {
        Write-Host 'Dobavlyayu Maven v MACHINE PATH...'
        $newPath = $machinePath.TrimEnd(';') + ';' + $mavenBin
        [Environment]::SetEnvironmentVariable('Path', $newPath, 'Machine')
        Write-Host 'Zapisano v MACHINE PATH.'
    }

# --- 3. Re broadcast WM_SETTINGCHANGE (choby novye processy uvideli PATH) --
$sig = @'
[DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Auto)]
public static extern IntPtr SendMessageTimeout(IntPtr hWnd, uint Msg, UIntPtr wParam, string lParam, uint fuFlags, uint uTimeout, out UIntPtr lpdwResult);
'@
Add-Type -MemberDefinition $sig -Name Win32 -Namespace Native -PassThru | Out-Null
$HWND_BROADCAST = [IntPtr]0xffff
$WM_SETTINGCHANGE = 0x1A
$result = [UIntPtr]::Zero
[void][Native.Win32]::SendMessageTimeout($HWND_BROADCAST, $WM_SETTINGCHANGE, [UIntPtr]::Zero, 'Environment', 2, 5000, [ref]$result)
Write-Host 'Broadcast WM_SETTINGCHANGE otoslan.'

# --- 4. Itog --------------------------------------------------------------
$check = [Environment]::GetEnvironmentVariable('Path', 'Machine')
Write-Host ''
Write-Host '=== MACHINE PATH teper soderzhit Maven? ==='
if ($check -match 'apache-maven') {
        Write-Host 'DA. Teper `mvn` budet dostupen i servisu jenkins.'
    } else {
        Write-Host 'NET. Chto-to poshlo ne tak.'
        exit 1
    }

Write-Host ''
Write-Host 'VAZHNO: Jenkins uje zapushchen i v ego processe stariy PATH.'
Write-Host 'CHTOBYE PRIME NIT (bez etogo ne srabotaet):'
Write-Host '  1) services.msc -> Jenkins -> Restart (Perezapustit).'
Write-Host '     ili v PowerShell ot admina:'
Write-Host '       Restart-Service jenkins'
Write-Host '  2) Posle perezapuska vernisya v Jenkins -> Lab3-Hotel -> Build Now.'
exit 0