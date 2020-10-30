#!/usr/bin/env bash

#Download Istio
curl -L https://git.io/getLatestIstio | ISTIO_VERSION=1.6.8 sh -

#Add Istioctl to PATH
cd istio-1.6.8 && export PATH=$PWD/bin:$PATH

#Install Istio with Minimal components
./bin/istioctl install --set profile=minimal --set addonComponents.tracing.enabled=true

