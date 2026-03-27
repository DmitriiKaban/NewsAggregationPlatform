Write-Host "Hibernating services to save RAM..." -ForegroundColor Magenta

# 1. Scale everything to zero
kubectl scale deployment --all --replicas=0
kubectl scale statefulset --all --replicas=0

# 2. Kill the ngrok process
Write-Host "Closing Ngrok..." -ForegroundColor Red
Stop-Process -Name "ngrok" -ErrorAction SilentlyContinue

Write-Host "All pods terminated. Let the PC breathe." -ForegroundColor White