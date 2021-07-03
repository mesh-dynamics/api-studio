# Solr manifest

This manifest  installs a Solr cluster and it's required Zookeeper cluster into a running
kubernetes cluster.

NOTE: Update clusterIP for NFS service as per your cluster service CIDDR block.  
DO a find and replay for 100.67.141.7

# Install

kubectl apply -f solr.yaml