#!/bin/bash

#http://redsymbol.net/articles/unofficial-bash-strict-mode


init() {
	kubectl apply -f <(istioctl kube-inject -f moviebook/moviebook.yaml)
	kubectl apply -f <(istioctl kube-inject -f cube/service.yaml)
	kubectl apply -f moviebook-gateway.yaml
	kubectl apply -f moviebook/moviebook_virtualservice.yaml
	kubectl apply -f cube/virtualservice.yaml
	kubectl apply -f cube/service_entry.yaml
	kubectl apply -f cube/solr_service_entry.yaml
	./fetch_servicenames.py
	./generate_lua_filters.py
	echo "lua filters generated"

	export INGRESS_HOST=$(minikube ip)
	export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
	export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
	sleep 15
	open http://$GATEWAY_URL/minfo/health
}

record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	export INGRESS_HOST=$(minikube ip)
	export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
	export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
	kubectl apply -f moviebook/moviebook-envoy-cs.yaml
	curl -X POST \
  http://$GATEWAY_URL/cs/start/$USER/$APPLICATION/$INSTANCEID/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
}

stop_record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	export INGRESS_HOST=$(minikube ip)
	export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
	export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
	curl -X POST \
  http://$GATEWAY_URL/cs/stop/$USER/$APPLICATION/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
	kubectl delete -f moviebook/moviebook-envoy-cs.yaml
}

replay() {
	kubectl apply -f moviebook/moviebook-envoy-replay-cs.yaml
}

stop_replay() {
	kubectl delete -f moviebook/moviebook-envoy-replay-cs.yaml
}

clean() {
	kubectl delete -f moviebook/moviebook.yaml
	kubectl delete -f cube/service.yaml
	kubectl delete -f cube/service_entry.yaml
	kubectl delete -f moviebook-gateway.yaml
	kubectl delete -f moviebook/moviebook_virtualservice.yaml
	kubectl delete -f cube/virtualservice.yaml
	kubectl delete -f cube/solr_service_entry.yaml
}

main() {
  set -eo pipefail; [[ "$TRACE" ]] && set -x
  case "$1" in
    init) shift; init "$@";;
    record) shift; record "@";;
    stop_recording) shift; stop_record "@";;
    replay) shift; replay "@";;
    stop_replay) shift; stop_replay "@";;
    clean) shift; clean "$@";;
    *) echo "This script expect one of these system argument(init, record, stop_recording, replay, stop_replay, clean).";;
  esac
}

main "$@"
