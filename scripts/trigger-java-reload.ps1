$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Runtime.InteropServices

$codeWin = Get-Process -Name Code -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -match "hotel" } | Select-Object -First 1
if (-not $codeWin) {
    Write-Host "VS Code window not found"
    exit 1
}

Write-Host "VS Code PID=$($codeWin.Id)"

$signature = @"
[DllImport("user32.dll")]
public static extern bool SetForegroundWindow(IntPtr hWnd);
[DllImport("user32.dll")]
public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);
"@

$type = Add-Type -MemberDefinition $signature -Name "Win32" -Namespace "Native" -PassThru
[void]$type::ShowWindowAsync($codeWin.MainWindowHandle, 9)
Start-Sleep -Milliseconds 500
[void]$type::SetForegroundWindow($codeWin.MainWindowHandle)
Start-Sleep -Milliseconds 500

Write-Host "Opening command palette and running Java: Reload Projects"
[System.Windows.Forms.SendKeys]::SendWait("^+p")
Start-Sleep -Milliseconds 1500
[System.Windows.Forms.SendKeys]::SendWait("Java: Reload Projects")
Start-Sleep -Milliseconds 800
[System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
Write-Host "Command sent"
