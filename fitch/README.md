# Kubernetes manifests to deploy MeshDynamics services

These manifests with deploy solr, postgres and MeshDynamics services.  

##Steps  

We use NFS storage for solr backups and need static IP for this NFS service.  
1. Get service CIDR block of the kubernetes cluster:
```
kubectl cluster-info dump | grep -m 1 service-cluster-ip-range
```

2. Pick an IP address from the service CIDR block range and update IP address in line number 15 and 40 of solr.yaml

3. Install solr
```
kubectl apply -f solr.yaml
```

4. Install postgres
```
kubectl apply -f postgres.yaml
```

5. Install meshdynamics Services
```
kubectl apply -f cube.yaml
```
