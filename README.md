# Deploy Sample Apps

## Setup Environment (macOS)

1. Ensure [`homebrew`](https://brew.sh) and [`homebrew-cask`](https://caskroom.github.io/) are installed.
(Pointer to install cask: https://sourabhbajaj.com/mac-setup/Homebrew/Cask.html)

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

6. Start local kubernetes cluster with minikube  
```
minikube start --memory=8192 --cpus=4
```

7. Enable ingress setup on `minikube`  
```
minikube addons enable ingress
```

8. Apply the `minikube` docker env to your shell. This will make the host
docker use the docker registry inside the minikube vm.  
```
$ eval $(minikube docker-env)
```

9. Download [`Istio`](https://istio.io/)  
```
$ curl -L https://git.io/getLatestIstio | sh -
```

10. Add Istio to PATH variable to make it easy to access Istio binaries.  
```
$ export PATH=$PWD/istio-1.0.6/bin:$PATH
```

11. Install Istio’s Custom Resource Definitions via kubectl apply, and wait a few seconds for the CRDs to be committed in the kube-apiserver.  
```
$ kubectl apply -f istio-1.0.6/install/kubernetes/helm/istio/templates/crds.yaml
```

12. Install Istio  
```
$ kubectl apply -f istio-1.0.6/install/kubernetes/istio-demo.yaml
```

13. [Setup moviebook app](moviebook/README.md)
