#!/bin/bash

echo "Running OpenDistro for ElasticSearch (ODFE) and ODFE-Kibana"
kubectl create -f odfe.yaml
kubectl create -f odfe-kibana.yaml

 # shellcheck disable=SC2164
 cd ElasticSinkConnect/
 echo "Building Netflow Elastic Sink Connector"
 docker build -t elastic-sink-connector:v1.0 -f ./Dockerfile ./
 docker tag elastic-sink-connector:v1.0 10.101.10.244:5000/elastic-sink-connector:v1.0
 docker push 10.101.10.244:5000/elastic-sink-connector:v1.0
 echo "Running Netflow Elastic Sink Connector"
 kubectl create -f deployment.yaml

 # shellcheck disable=SC2164
 cd ../RegistryService/
 echo "Building Registry Service"
 docker build -t registry-app:v1.0 -f ./Dockerfile-RegistryService ./
 docker tag registry-app:v1.0 10.101.10.244:5000/registry-app:v1.0
 docker push 10.101.10.244:5000/registry-app:v1.0
 echo "Running Registry Service"
 kubectl create -f deployment.yaml

 # shellcheck disable=SC2164
 cd ../CollectorLoadBalancerService/
 echo "Building Collector Load Balancer Service"
 docker build -t collector-lb-app:v1.0 -f ./Dockerfile-CollectorLBService ./
 docker tag collector-lb-app:v1.0 10.101.10.244:5000/collector-lb-app:v1.0
 docker push 10.101.10.244:5000/collector-lb-app:v1.0
 echo "Running Collector Load Balancer Service"
 kubectl create -f deployment.yaml

 # shellcheck disable=SC2164
 cd ../NetflowSourceConnect/
 echo "Building Netflow CSV Source Connector"
 docker build -t netflow-source-connector:v1.0 -f ./Dockerfile ./
 docker tag netflow-source-connector:v1.0 10.101.10.244:5000/netflow-source-connector:v1.0
 docker push 10.101.10.244:5000/netflow-source-connector:v1.0
 echo "Running Netflow CSV Source Connector"
 kubectl create -f deployment.yaml
