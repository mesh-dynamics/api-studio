# Deploy Cube Apps

Helm chart to deploy Cube apps.

## Requirements

1. Ensure helm is installed  
```
$ brew install helm
```

2. Install [`kubectl`](https://kubernetes.io/docs/reference/kubectl/overview/)  
```
$ brew install kubectl
```

3. Update values.yaml and deploy.cfg file(if needed).

## Usage


1. Deploy cube Apps  
This will cube services in cube namespace.  
```
./deploy.sh init
```

2. Start Recording  
```
./deploy.sh record
```

3. Stop recording  
```
./deploy.sh stop_recording
```
