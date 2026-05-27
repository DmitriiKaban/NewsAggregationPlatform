#!/bin/bash

echo "Hibernating services to save RAM..."

kubectl scale deployment --all --replicas=0
kubectl scale statefulset --all --replicas=0

echo "All pods terminated. Let the PC breathe."
