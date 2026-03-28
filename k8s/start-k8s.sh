Write-Host "Waking up THESIS IS COMMMMMINGGGG" -ForegroundColor Cyan

# 1. Scale up all deployments
kubectl scale deployment tg-bot --replicas=1
kubectl scale deployment ai-service --replicas=1
kubectl scale deployment scraper --replicas=1

# 2. Scale up infrastructure (Check your specific names from 'kubectl get statefulset')
kubectl scale statefulset news-kafka-controller --replicas=1
kubectl scale sts postgres --replicas=1

Write-Host "Waiting 10 seconds for services to initialize..." -ForegroundColor Yellow
Start-Sleep -s 10

# 3. Launch Ngrok in a separate window
Write-Host "Launching Ngrok Tunnel..." -ForegroundColor Green
Start-Process powershell -ArgumentList "ngrok http 80"

Write-Host "✅ Run 'kubectl get pods' to monitor progress." -ForegroundColor White