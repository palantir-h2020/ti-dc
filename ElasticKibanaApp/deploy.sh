#!/bin/bash

kubectl delete -f odfe.yaml
kubectl delete -f odfe-kibana.yaml

kubectl create -f odfe.yaml
kubectl create -f odfe-kibana.yaml
