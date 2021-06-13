#!/bin/bash

kubectl create ns postgres
helm template . > postgres.yaml
kubectl apply -f postgres.yaml -n postgres
rm postgres.yaml


