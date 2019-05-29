#!/usr/bin/env bash

#This script is to deploy cube server which can then test other cube servers
#Deploys Cube services in an isolated cube namespace.

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
	kubectl apply -f cube/cube_namespace.yaml
	kubectl apply -f cube/secret.yaml
	kubectl apply -f cube/service.yaml -n cube
	kubectl apply -f cube/dogfooding_virtualservice.yaml
	kubectl apply -f cube/service_entry.yaml -n cube
}

generate_record_cs_yaml() {
	sed -e "s/{{customer}}/$CUBE_USER/g" cube/templates/envoy_record_cs.j2 > cube/envoy_record_cs.yaml
	sed -i '' -e "s/{{cube_instance}}/$CUBE_INSTANCEID/g" cube/envoy_record_cs.yaml
}
record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	generate_record_cs_yaml
	kubectl apply -f cube/envoy_record_cs.yaml -n staging
	curl -X POST \
  http://$GATEWAY_URL/cs/start/$CUBE_USER/cube/$CUBE_INSTANCEID/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	-H 'Host: dogfooding.cubecorp.io'
}

stop_record() {
	COLLECTION_NAME=$(curl -X GET \
  "http://$GATEWAY_URL/cs/currentcollection?customerid=$CUBE_USER&app=cube&instanceid=$CUBE_INSTANCEID" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	-H 'Host: dogfooding.cubecorp.io')
	curl -X POST \
  http://$GATEWAY_URL/cs/stop/$CUBE_USER/cube/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	-H 'Host: dogfooding.cubecorp.io'
	kubectl delete -f cube/envoy_record_cs.yaml -n staging
}

replay() {
	echo "Enter Collection name"
	read COLLECTION_NAME
	kubectl apply -f cube/envoy_mock_cs.yaml -n staging
	REPLAY_ID=$(curl -X POST http://dogfooding.cubecorp.io/rs/init/$CUBE_USER/cube/$COLLECTION_NAME \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H 'cache-control: no-cache' \
		-d "endpoint=http://staging.cubecorp.io&instanceid=$CUBE_INSTANCEID" | awk -F ',' '{print $7}' | cut -d '"' -f 4)
	curl -f -X POST \
		http://dogfooding.cubecorp.io/rs/start/$CUBE_USER/cube/$COLLECTION_NAME/REPLAY_ID \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H 'cache-control: no-cache'
	if [ $? -eq 0 ]; then
		echo "Replay Started"
	else
		echo "Replay did not start"
	fi
	echo $REPLAY_ID > dogfooding_replayid.temp
}

stop_replay() {
	kubectl delete -f cube/envoy_mock_cs.yaml -n staging
}

clean() {
	kubectl delete namespaces cube
	kubectl delete virtualservices -l env=cube
}

get_environment() {
	if [ -z "$CUBE_USER" ]; then
		export CUBE_USER=$USER
	fi
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
# To debug this script, run it with TRACE=1 in the enviornment
# -x option will trace each command that is run
set -eo pipefail; [[ "$TRACE" ]] && set -x
: <<CHECKVARIABLES
${CUBE_INSTANCEID?}${CUBE_USER?} # Print error message if one of the variables not set.
CHECKVARIABLES
get_environment
  case "$1" in
    init) shift; init "$@";;
    record) shift; record "$@";;
    stop_recording) shift; stop_record "$@";;
		replay) shift; replay "$@";;
		stop_replay) shift; stop_replay "$@";;
    clean) shift; clean "$@";;
    *) echo "This script expect one of these system argument(init, record, stop_recording, replay, clean).";;
  esac
}

main "$@"
