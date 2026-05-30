#!/bin/bash
set -e

echo "Waking up THESIS IS COMMMMMINGGGG"

kubectl scale deployment ingress-nginx-controller -n ingress-nginx --replicas=1
kubectl scale statefulset postgres --replicas=1
kubectl scale deployment zookeeper --replicas=1
kubectl scale deployment news-kafka --replicas=1
kubectl scale deployment redis --replicas=1

echo "Waiting 20 seconds for infrastructure to initialize..."
sleep 20

kubectl scale deployment tg-bot --replicas=1
kubectl scale deployment ai-service --replicas=1
kubectl scale deployment scraper --replicas=1
kubectl scale deployment ngrok --replicas=1

NGROK_URL=$(kubectl get secret newsbot-secrets -o go-template='{{.data.API_BASE_URL | base64decode}}')
echo "Ngrok tunnel: $NGROK_URL"
echo "Run 'kubectl get pods' to monitor progress."
