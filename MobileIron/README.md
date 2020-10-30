# Install Istio and generate Evnoy filters to send data to Meshdynamics services

## Install Istio

1. Run `deploy_istio_and_envoy_sidecar.sh` script to install istio.  
```
./deploy_istio_and_envoy_sidecar.sh
```

2. Enable istio on namespace where you want to deploy Meshdynamics envoy filters.  
```
kubectl label namespace <NAMESPACE> istio-injection=enabled
```

3. Restart deployments for sidecar injection.  
```
kubectl rollout restart deployment <DEPLOYMENT_NAME>
```

## Generate Meshdynamics Record filters

1. Install dependencies:  
```
pip3 install -r requirements.txt
```

2. (Optional) Edit `templates/services` file and add the name of deployments for which you want to generate envoy filters and change value of `service_file_provided` in config.py to `yes`.

3. Run script to fetch deployments, lables and generate Meshdynamics record filters.  
```
./prepare_and_deploy_mesh_filters.py
```  
NOTE: If your kubernetes configuration is not at the default location, export `KUBECONFIG` environment variable.  
```
KUBECONFIG=/Users/aakashsinghal/workspace/config ./prepare_and_deploy_mesh_filters.py
```

4. Deploy Envoy filters  
```
cd output && kubectl apply -f .
```

