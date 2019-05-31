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

init_default() {
	kubectl apply -f cube/secret.yaml
	kubectl apply -f <(istioctl kube-inject -f moviebook/moviebook.yaml)
	kubectl apply -f <(istioctl kube-inject -f cube/service.yaml)
	kubectl apply -f moviebook-gateway.yaml
	kubectl apply -f moviebook/bookinfo_virtualservice.yaml
	kubectl apply -f moviebook/movieinfo-v1.yaml
	kubectl apply -f cube/virtualservice.yaml
	kubectl apply -f cube/service_entry.yaml
	./fetch_servicenames.py
	echo "waiting for cubews to come online"
	until $(curl --output /dev/null --silent --head --fail http://$GATEWAY_URL/cs/health); do
	  printf '.'
	  sleep 2
	done
	printf "\n"
	setup
}

init_staging() {
	kubectl create namespace staging
	kubectl apply -f cube/secret.yaml -n staging
	kubectl apply -f <(istioctl kube-inject -f moviebook/moviebook.yaml) -n staging
	kubectl apply -f <(istioctl kube-inject -f cube/service.yaml) -n staging
	kubectl apply -f moviebook-gateway.yaml
	kubectl apply -f moviebook/bookinfo_virtualservice_staging.yaml
	kubectl apply -f moviebook/movieinfo-v1_staging.yaml
	kubectl apply -f cube/virtualservice_staging.yaml
	kubectl apply -f cube/service_entry.yaml
	./fetch_servicenames.py
	echo "waiting for cubews to come online"
	until $(curl --output /dev/null --silent --head --fail -HHost:staging.cubecorp.io http://$GATEWAY_URL/cs/health); do
	  printf '.'
	  sleep 2
	done
	printf "\n"
	HEADER="-H Host:staging.cubecorp.io"
	setup $HEADER
}

init() {
		init_$CUBE_ENV
}

switch() {
	if [ "$1" = "ide" ] || [ "$1" = "IDE" ]; then
		kubectl delete deployment cubews-v1
		kubectl delete svc cubews
		echo "Run the following command in your shell: telepresence --new-deployment cubews --expose 8080"
	elif [ "$1" = "minikube" ]; then
		kubectl delete deployments cubews
		kubectl delete svc cubews
		kubectl apply -f cube/service.yaml
	else
		echo "Invaild choice, enter a valid option(ide, minikube)"
	fi
}

register_templates() {
	echo "Registering Templates"
	./update_templates.py  $1 $GATEWAY_URL $CUBE_USER $CUBE_APPLICATION
}

setup() {
	echo "Setting default responses"
	curl -X POST \
  http://$GATEWAY_URL/cs/setdefault/$CUBE_USER/movieinfo/restwrapjdbc/GET/restsql/initialize \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	$1 $2 \
  -d 'body=%7B%20%22status%22%3A%20%22Connection%20pool%20created.%22%7D&status=200&content-type=application%2Fjson&undefined='
	curl -X POST \
  http://$GATEWAY_URL/cs/setdefault/$CUBE_USER/movieinfo/restwrapjdbc/POST/restsql/update \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	$1 $2 \
  -d 'body=%7B%22num_updates%22%3A1%7D&status=200&content-type=application%2Fjson&undefined='
	echo "Setting response templates"
	curl -X POST \
  http://$GATEWAY_URL/as/registerTemplate/response/$CUBE_USER/$CUBE_APPLICATION/movieinfo/minfo/listmovies \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
	$1 $2 \
  -d '{
      "prefixPath": "",
      "rules": [
        {
          "path": "/body",
          "pt": "Required",
          "dt": "Str",
          "ct": "Equal"
        }
      ]
 }'
	curl -X POST \
  http://$GATEWAY_URL/as/registerTemplate/response/$CUBE_USER/$CUBE_APPLICATION/movieinfo/minfo/liststores \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
	$1 $2 \
  -d '{
      "prefixPath": "",
      "rules": [
        {
          "path": "/body",
          "pt": "Required",
          "dt": "Str",
          "ct": "Equal"
        }
      ]
 }'
	curl -X POST \
  http://$GATEWAY_URL/as/registerTemplate/response/$CUBE_USER/$CUBE_APPLICATION/movieinfo/minfo/rentmovie \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
	$1 $2 \
  -d '{
      "prefixPath": "",
      "rules": [
        {
          "path": "/body",
          "pt": "Required",
          "dt": "Str",
          "ct": "Equal"
        }
      ]
 }'
	curl -X POST \
  http://$GATEWAY_URL/as/registerTemplate/response/$CUBE_USER/$CUBE_APPLICATION/movieinfo/minfo/returnmovie \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
	$1 $2 \
  -d '{
      "prefixPath": "",
      "rules": [
        {
          "path": "/body",
          "pt": "Required",
          "dt": "Str",
          "ct": "Equal"
        }
      ]
 }'
}
record_default() {
	export NAMESPACE=default
	./generate_lua_filters.py $CUBE_USER $NAMESPACE
	echo "lua filters generated"
	kubectl apply -f moviebook/moviebook-envoy-cs.yaml
	curl -X POST \
  http://$GATEWAY_URL/cs/start/$CUBE_USER/$CUBE_APPLICATION/$CUBE_INSTANCEID/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
}

record_staging() {
	export NAMESPACE=staging
	./generate_lua_filters.py $CUBE_USER $NAMESPACE
	echo "lua filters generated"
	kubectl apply -f moviebook/moviebook-envoy-cs.yaml -n staging
	curl -X POST \
  http://$GATEWAY_URL/cs/start/$CUBE_USER/$CUBE_APPLICATION/$CUBE_INSTANCEID/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'Host:staging.cubecorp.io' \
  -H 'cache-control: no-cache'
}

record() {
	echo "Enter collection name"
	read COLLECTION_NAME
	record_$CUBE_ENV
}

stop_record_default() {
	COLLECTION_NAME=$(curl -X GET \
  "http://$GATEWAY_URL/cs/currentcollection?customerid=$CUBE_USER&app=$CUBE_APPLICATION&instanceid=$CUBE_INSTANCEID" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache')
	curl -X POST \
  http://$GATEWAY_URL/cs/stop/$CUBE_USER/$CUBE_APPLICATION/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	kubectl delete -f moviebook/moviebook-envoy-cs.yaml
}

stop_record_staging() {
	COLLECTION_NAME=$(curl -X GET \
  "http://$GATEWAY_URL/cs/currentcollection?customerid=$CUBE_USER&app=$CUBE_APPLICATION&instanceid=$CUBE_INSTANCEID" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'Host:staging.cubecorp.io' \
  -H 'cache-control: no-cache')
	curl -X POST \
  http://$GATEWAY_URL/cs/stop/$CUBE_USER/$CUBE_APPLICATION/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	-H 'Host:staging.cubecorp.io'
	kubectl delete -f moviebook/moviebook-envoy-cs.yaml -n staging
}

stop_record() {
	stop_record_$CUBE_ENV
}

generate_mock_all_yaml() {
	sed -e "s/{{customer}}/$CUBE_USER/g" moviebook/templates/mock-all-except-moviebook.j2 > moviebook/mock-all-except-moviebook.yaml
	sed -i '' -e "s/{{cube_application}}/$CUBE_APPLICATION/g" moviebook/mock-all-except-moviebook.yaml
	sed -i '' -e "s/{{cube_instance}}/$CUBE_INSTANCEID/g" moviebook/mock-all-except-moviebook.yaml
}

get_replay_id() {
	REPLAY_ID=$(curl -X POST \
	http://$GATEWAY_URL/rs/init/$CUBE_USER/$CUBE_APPLICATION/$COLLECTION_NAME \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'cache-control: no-cache' \
	-d "$1" | awk -F ',' '{print $7}' | cut -d '"' -f 4)
}

custom_replay() {
	echo "Enter comma separate request ID(press enter key to skip this)"
	read REQUESTIDS
	if [ -z $REQUESTIDS ]; then
		echo "enter comma separate paths(eg: minfo/listmovies,minfo/liststores)"
		read INPUTPATHS
		PATHS_SEPARATED=$(echo $INPUTPATHS | tr "," "\n")
		for path in $PATHS_SEPARATED
		do
			TEMP_PATH="$TEMP_PATH""paths=$path&"
		done
		FINAL_PATH=${TEMP_PATH::${#TEMP_PATH}-1}
		echo "Enter sample rate(press enter key to skip this)"
		read SAMPLERATE
		if [ -z $SAMPLERATE ]; then
			BODY="$FINAL_PATH&endpoint=http://$GATEWAY_URL&instanceid=$CUBE_INSTANCEID"
			get_replay_id $BODY
		else
			BODY="$FINAL_PATH&endpoint=http://$GATEWAY_URL&instanceid=$CUBE_INSTANCEID&samplerate=$SAMPLERATE"
			get_replay_id $BODY
		fi
	else
		REQUESTIDS=$(echo $REQUESTIDS | tr "," "\n")
		for REQUEST in $REQUESTIDS
		do
			TEMP_REQUESTIDS="$TEMP_REQUESTIDS""reqids=$REQUEST&"
		done
		FINAL_REQUESTIDS=${TEMP_REQUESTIDS::${#TEMP_REQUESTIDS}-1}
		BODY="$FINAL_REQUESTIDS&endpoint=http://$GATEWAY_URL&instanceid=$CUBE_INSTANCEID"
		get_replay_id $BODY
	fi
}

replay_setup_default() {
	export NAMESPACE=default
	./generate_lua_filters.py $CUBE_USER $NAMESPACE
	kubectl apply -f moviebook/moviebook-envoy-replay-cs.yaml
	kubectl apply -f moviebook/mock-all-except-moviebook.yaml
	echo "Which version of movieinfo you want to test?(v1/v2)"
	read VERSION
	if [ "$VERSION" = "v1" ]; then
		echo "Routing traffic to v1"
		kubectl apply -f moviebook/movieinfo-v2.yaml
	elif [ "$VERSION" = "v2" ]; then
		echo "Routing traffic to v2"
		kubectl apply -f moviebook/movieinfo-v2.yaml
	else
		echo "Invalid Input, enter a valid version(v1/v2)"
		exit 1
	fi
}

replay_setup_staging() {
	export NAMESPACE=staging
	./generate_lua_filters.py $CUBE_USER $NAMESPACE
	kubectl apply -f moviebook/moviebook-envoy-replay-cs.yaml -n staging
	kubectl apply -f moviebook/mock-all-except-moviebook.yaml -n staging
	echo "Which version of movieinfo you want to test?(v1/v2)"
	read VERSION
	if [ "$VERSION" = "v1" ]; then
		echo "Routing traffic to v1"
		kubectl apply -f moviebook/movieinfo-v1_staging.yaml
	elif [ "$VERSION" = "v2" ]; then
		echo "Routing traffic to v2"
		kubectl apply -f moviebook/movieinfo-v2_staging.yaml
	else
		echo "Invalid Input, enter a valid version(v1/v2)"
		exit 1
	fi
}

replay_setup() {
	generate_mock_all_yaml
	replay_setup_$CUBE_ENV
}

replay() {
	echo "Enter collection name"
	read COLLECTION_NAME
	echo "Do you want to replay with default paths?(yes/no)"
	read CHOICE
	if [ "$CHOICE" = "no" ]; then
		custom_replay
	else
		BODY="paths=minfo%2Flistmovies&paths=minfo%2Fliststores&paths=minfo%2Frentmovie&paths=minfo%2Freturnmovie&endpoint=http://$GATEWAY_URL&instanceid=$CUBE_INSTANCEID&samplerate=0.05"
		get_replay_id $BODY
	fi
	curl -f -X POST \
  http://$GATEWAY_URL/rs/start/$CUBE_USER/$CUBE_APPLICATION/$COLLECTION_NAME/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
	if [ $? -eq 0 ]; then
		echo "Replay started"
	else
		echo "Replay did not started"
	fi
	echo $REPLAY_ID > replayid.temp
}

stop_replay_default() {
	kubectl delete -f moviebook/moviebook-envoy-replay-cs.yaml
	kubectl delete -f moviebook/mock-all-except-moviebook.yaml
	kubectl apply -f moviebook/movieinfo-v1.yaml
}
stop_replay_staging() {
	kubectl delete -f moviebook/moviebook-envoy-replay-cs.yaml -n staging
	kubectl delete -f moviebook/mock-all-except-moviebook.yaml -n staging
	kubectl apply -f moviebook/movieinfo-v1_staging.yaml
}
stop_replay() {
	stop_replay_$CUBE_ENV
}

analyze() {
	REPLAY_ID=$(cat replayid.temp)
	echo "Analyzing for replay ID:" $REPLAY_ID
	curl -X POST \
  http://$GATEWAY_URL/as/analyze/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'
}

clean_default() {
	stop_replay 2> /dev/null
	kubectl delete -f moviebook/moviebook.yaml
	kubectl delete -f cube/service.yaml 2> /dev/null
	kubectl delete -f cube/service_entry.yaml
	kubectl delete -f moviebook-gateway.yaml
	kubectl delete -f moviebook/bookinfo_virtualservice.yaml
	kubectl delete -f moviebook/movieinfo-v1.yaml
	kubectl delete -f cube/virtualservice.yaml
	kubectl delete deployments cubews 2> /dev/null
}
clean_staging() {
	kubectl delete namespaces staging
	kubectl delete virtualservice -l env=staging
}
clean() {
	clean_$CUBE_ENV
	if [ -f replayid.temp ]; then
	  rm replayid.temp
	fi
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
	if [ "$CUBE_ENV" = "default" ]; then
		echo "Environment set to default"
	elif [ "$CUBE_ENV" = "staging" ]; then
		echo "Environment set to staging"
	else
		echo "Kindly export CUBE_ENV varible, valid values are default, staging"
		exit 1
	fi
}


main() {
# To debug this script, run it with TRACE=1 in the enviornment
# -x option will trace each command that is run
  set -o pipefail; [[ "$TRACE" ]] && set -x
	get_environment
  case "$1" in
    init) shift; init "$@";;
    switch) shift; switch "$@";;
    record) shift; record "$@";;
    stop_recording) shift; stop_record "$@";;
    register_templates) shift; register_templates "$@";;
    replay_setup) shift; replay_setup "$@";;
    replay) shift; replay "$@";;
    stop_replay) shift; stop_replay "$@";;
    analyze) shift; analyze "$@";;
    clean) shift; clean "$@";;
    *) echo "This script expect one of these system argument(init, switch, record, stop_recording, register_templates, replay_setup, replay, stop_replay, analyze, clean).";;
  esac
}

main "$@"