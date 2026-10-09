Get-Process | Where-Object { $_.ProcessName -match "java" } | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3
Set-Location -Path "D:\03_Development_dan_Programming\02_Mobile_Development\Dramix_Android"
$o = & .\gradlew.bat --no-daemon :app:compileDebugKotlin 2>&1
$o | Set-Content "compile_out.txt" -Encoding utf8
"__RC=$LASTEXITCODE__" | Add-Content "compile_out.txt"
"done"