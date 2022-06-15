#!/bin/sh

# setup kube-state-metrics deployment
echo "Deleting the previous kube-state-metrics deployment ..."
kubectl delete -f /media/palantir-nfs/kube-state-metrics-configs/
echo "Waiting a bit ..."
sleep 5
echo "Creating a new kube-state-metrics deployment ..."
kubectl apply -f /media/palantir-nfs/kube-state-metrics-configs/
echo "Waiting a bit ..."
sleep 5

# setup metricbeat
echo "Building metricbeat docker image ..."
sudo docker build -t metricbeat-dcp:7.12.1 -f Dockerfile-MetricbeatDCP ./
sudo docker tag metricbeat-dcp:7.12.1 10.101.10.244:5000/metricbeat-dcp:7.12.1
echo "Pushing to registry the metricbeat docker image ..."
sudo docker push 10.101.10.244:5000/metricbeat-dcp:7.12.1

echo "Deleting previous metricbeat pod ..."
# kubectl delete pod metricbeat-dcp -n ti-dcp
kubectl delete pod metricbeat-dcp

# echo "Deleting metricbeat elastic templates and indices ..."
# curl -XDELETE -k "https://10.101.41.42:9200/metricbeat-*" -u admin:admin
# curl -XDELETE -k "https://10.101.41.42:9200/_template/metricbeat-7.12.1" -u admin:admin

echo "Creating a new metricbeat pod ..."
kubectl create -f metricbeat-dcp.yml