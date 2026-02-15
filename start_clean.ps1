$port = 8080
$process = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
if ($process) {
    Write-Host "Killing process $($process.OwningProcess) on port $port"
    Stop-Process -Id $process.OwningProcess -Force
    Start-Sleep -Seconds 2
}

$check = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
if ($check) {
    Write-Host "Port $port is still in use by $($check.OwningProcess). Aborting."
    exit 1
}

Write-Host "Port $port is free. Starting application..."
cmd /c "mvnw.cmd spring-boot:run > run_final_v8.log 2>&1"
