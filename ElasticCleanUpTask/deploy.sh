#!/bin/bash

kubectl delete -f pod.yaml

sudo docker build -t elastic-cleanup-task-app:v1.0 -f Dockerfile .

sudo docker tag elastic-cleanup-task-app:v1.0 10.101.10.244:5000/elastic-cleanup-task-app:v1.0

sudo docker push 10.101.10.244:5000/elastic-cleanup-task-app:v1.0

kubectl create -f pod.yaml