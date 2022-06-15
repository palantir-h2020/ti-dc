#!/bin/sh

# sudo docker stop iframe-opensearch
# sudo docker rm iframe-opensearch

# sudo docker run \
#     -d \
#     -p 5590:5590 \
#     -v /media/palantir-nfs/ti-dc/iframe-nginx/templates:/etc/nginx/templates \
#     --name iframe-opensearch \
#     nginx:1.20.2

kubectl delete -f deployment.yaml

kubectl create -f deployment.yaml
