# Deploy Sample Apps

## Setup Environment (macOS)

1. Ensure [`homebrew`](https://brew.sh) is installed.

2. Install [`virtualbox`](https://www.virtualbox.org/wiki/VirtualBox) via `homebrew`, if not already installed  
```
$ brew update
$ brew cask install virtualbox
```
3. Install minikube  
```
$ brew cask install minikube
```

4. Install docker  
```
$ brew cask install docker
```

5. Add the [`kubectl`](https://kubernetes.io/docs/reference/kubectl/overview/)  
```
$ brew install kubectl
```

6. Install [`Telepresence`](https://www.telepresence.io/)  
```
brew cask install osxfuse
brew install datawire/blackbird/telepresence
```

7. Start local kubernetes cluster with minikube  
```
minikube start --memory=8192 --cpus=4
```

8. Enable ingress setup on `minikube`  
```
minikube addons enable ingress
```

9. Apply the `minikube` docker env to your shell. This will make the host
docker use the docker registry inside the minikube vm.  
```
$ eval $(minikube docker-env)
```

10. Download [`Istio`](https://istio.io/)  
```
$ curl -L https://git.io/getLatestIstio | ISTIO_VERSION=1.0.6 sh -
```

11. Add Istio to PATH variable to make it easy to access Istio binaries.  
```
$ export PATH=$PWD/istio-1.0.6/bin:$PATH
```

12. Install Istio’s Custom Resource Definitions via kubectl apply, and wait a few seconds for the CRDs to be committed in the kube-apiserver.  
```
$ kubectl apply -f istio-1.0.6/install/kubernetes/helm/istio/templates/crds.yaml
```

> NOTE: Since we are running Istio with Minikube, we need to make one change before going ahead – changing the Ingress Gateway service from type LoadBalancer to NodePort.  
<b> IN CASE OF AWS, SKIP STEP 13. </b>

13. Open the file istio-1.0.6/install/kubernetes/istio-demo.yaml, search for LoadBalancer and replace it with NodePort.

14. Install Istio  
```
$ kubectl apply -f istio-1.0.6/install/kubernetes/istio-demo.yaml
```

15. Jaeger dashboard
```
kubectl port-forward -n istio-system $(kubectl get pod -n istio-system -l app=jaeger -o jsonpath='{.items[0].metadata.name}') 16686:16686 &
```
Access the Jaeger dashboard by opening your browser to http://localhost:16686.

16. minikube dashboard -- useful for ssh, browsing logs per pod, etc.
```
minikube dashboard
```

17. [Setup moviebook/Cube app](docs/README.md)
