#!/bin/bash

#http://redsymbol.net/articles/unofficial-bash-strict-mode

export_env_variables() {
	export INGRESS_HOST=$(minikube ip)
	export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
	export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
}
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

	export_env_variables
	sleep 15
	open http://$GATEWAY_URL/minfo/health
}

record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	export_env_variables
	kubectl apply -f moviebook/moviebook-envoy-cs.yaml
	curl -X POST \
  http://$GATEWAY_URL/cs/start/$USER/$APPLICATION/$INSTANCEID/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
}

stop_record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	export_env_variables
	curl -X POST \
  http://$GATEWAY_URL/cs/stop/$USER/$APPLICATION/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
	kubectl delete -f moviebook/moviebook-envoy-cs.yaml
}

generate_mock_all_yaml() {
	sed -e "s/{{customer}}/$USER/g" moviebook/templates/mock-all-except-moviebook.j2 > moviebook/mock-all-except-moviebook.yaml
	sed -i '' -e "s/{{application}}/$APPLICATION/g" moviebook/mock-all-except-moviebook.yaml
	sed -i '' -e "s/{{collection}}/$1/g" moviebook/mock-all-except-moviebook.yaml
}

replay() {
	echo "Enter collection name"
	read COLLECTION_NAME
	generate_mock_all_yaml $COLLECTION_NAME
	export_env_variables
	kubectl apply -f moviebook/moviebook-envoy-replay-cs.yaml
	kubectl apply -f moviebook/mock-all-except-moviebook.yaml
	REPLAY_ID=$(curl -X POST \
  http://$GATEWAY_URL/rs/init/$USER/$APPLICATION/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
  -d "endpoint=http://$GATEWAY_URL&instanceid=$INSTANCEID" | awk -F ',' '{print $7}' | cut -d '"' -f 4)
	curl -X POST \
  http://$GATEWAY_URL/rs/start/$USER/$APPLICATION/$COLLECTION_NAME/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
	echo $REPLAY_ID > replayid.temp
}

stop_replay() {
	kubectl delete -f moviebook/moviebook-envoy-replay-cs.yaml
	kubectl delete -f moviebook/mock-all-except-moviebook.yaml
}

analyze() {
	export_env_variables
	REPLAY_ID=$(cat replayid.temp)
	curl -X POST \
  http://$GATEWAY_URL/as/analyze/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
}
clean() {
	kubectl delete -f moviebook/moviebook.yaml
	kubectl delete -f cube/service.yaml
	kubectl delete -f cube/service_entry.yaml
	kubectl delete -f moviebook-gateway.yaml
	kubectl delete -f moviebook/moviebook_virtualservice.yaml
	kubectl delete -f cube/virtualservice.yaml
	kubectl delete -f cube/solr_service_entry.yaml
	rm replayid.temp
}

main() {
  set -eo pipefail; [[ "$TRACE" ]] && set -x
  case "$1" in
    init) shift; init "$@";;
    record) shift; record "@";;
    stop_recording) shift; stop_record "@";;
    replay) shift; replay "@";;
    stop_replay) shift; stop_replay "@";;
		analyze) shift; analyze "@";;
    clean) shift; clean "$@";;
    *) echo "This script expect one of these system argument(init, record, stop_recording, replay, stop_replay, analyze, clean).";;
  esac
}

main "$@"
