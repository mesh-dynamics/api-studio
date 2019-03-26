#!/bin/bash

#http://redsymbol.net/articles/unofficial-bash-strict-mode

export_dev_env_variables() {
	export INGRESS_HOST=$(minikube ip)
	export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
	export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
}

export_aws_env_variables() {
	export INGRESS_HOST=$(kubectl describe services istio-ingressgateway -n istio-system | grep "LoadBalancer Ingress" | awk '{print $3}')
	export INGRESS_PORT=80
	export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
}

init() {
	kubectl apply -f <(istioctl kube-inject -f moviebook/moviebook.yaml)
	kubectl apply -f cube/service.yaml
	kubectl apply -f moviebook-gateway.yaml
	kubectl apply -f moviebook/moviebook_virtualservice.yaml
	kubectl apply -f cube/virtualservice.yaml
	kubectl apply -f cube/service_entry.yaml
	kubectl apply -f cube/solr_service_entry.yaml
	./fetch_servicenames.py
	./generate_lua_filters.py
	echo "lua filters generated"

	sleep 15
	open http://$GATEWAY_URL/minfo/health
}

record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	kubectl apply -f moviebook/moviebook-envoy-cs.yaml
	curl -X POST \
  http://$GATEWAY_URL/cs/start/$USER/$CUBE_APPLICATION/$CUBE_INSTANCEID/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
}

stop_record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	curl -X POST \
  http://$GATEWAY_URL/cs/stop/$USER/$CUBE_APPLICATION/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
	kubectl delete -f moviebook/moviebook-envoy-cs.yaml
}

generate_mock_all_yaml() {
	sed -e "s/{{customer}}/$USER/g" moviebook/templates/mock-all-except-moviebook.j2 > moviebook/mock-all-except-moviebook.yaml
	sed -i '' -e "s/{{cube_application}}/$CUBE_APPLICATION/g" moviebook/mock-all-except-moviebook.yaml
	sed -i '' -e "s/{{cube_instance}}/$CUBE_INSTANCEID/g" moviebook/mock-all-except-moviebook.yaml
}

replay() {
	echo "Enter collection name"
	read COLLECTION_NAME
	generate_mock_all_yaml 
	kubectl apply -f moviebook/moviebook-envoy-replay-cs.yaml
	kubectl apply -f moviebook/mock-all-except-moviebook.yaml
	REPLAY_ID=$(curl -X POST \
  http://$GATEWAY_URL/rs/init/$USER/$CUBE_APPLICATION/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
  -d "endpoint=http://$GATEWAY_URL&instanceid=$CUBE_INSTANCEID" | awk -F ',' '{print $7}' | cut -d '"' -f 4)
	curl -f -X POST \
  http://$GATEWAY_URL/rs/start/$USER/$CUBE_APPLICATION/$COLLECTION_NAME/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
	if [ $? -eq 0 ]; then
		echo "Replay started"
	else
		echo "Replay did not started"
	fi

	echo $REPLAY_ID > replayid.temp
}

stop_replay() {
	kubectl delete -f moviebook/moviebook-envoy-replay-cs.yaml
	kubectl delete -f moviebook/mock-all-except-moviebook.yaml
}

analyze() {
	REPLAY_ID=$(cat replayid.temp)
	echo "Analyzing for replay ID:" $REPLAY_ID
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

get_environment() {
	ENVIRONMENT=$(kubectl config current-context)
	if [ "$ENVIRONMENT" = "minikube" ]; then
	  export_dev_env_variables
	  echo "Environment varibales set for dev environemt"
	else
	  export_aws_env_variables
	  echo "Environment varibales set for AWS environment"
	fi
}


main() {
  set -eo pipefail; [[ "$TRACE" ]] && set -x
	get_environment
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
