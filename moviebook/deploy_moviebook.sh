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
	if [ "$ENVIRONMENT" = "minikube" ]; then
	  echo "Do you want to run cubews on your IDE?(yes/no)"
	  read CHOICE
	fi
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
	if [ "$ENVIRONMENT" = "minikube" ]; then
	  if [ "$CHOICE" = "yes" ]; then
	    kubectl delete deployments cubews-v1
	    kubectl delete svc cubews
	    echo "Run the following command in your shell: telepresence --new-deployment cubews --expose 8080"
	  fi
	fi
}

setup() {
	echo "Setting default responses"
	curl -X POST \
  http://$GATEWAY_URL/cs/setdefault/$USER/movieinfo/restwrapjdbc/GET/restsql/initialize \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
  -d 'body=%7B%20%22status%22%3A%20%22Connection%20pool%20created.%22%7D&status=200&content-type=application%2Fjson&undefined='
	curl -X POST \
  http://$GATEWAY_URL/cs/setdefault/$USER/movieinfo/restwrapjdbc/POST/restsql/update \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
  -d 'body=%7B%22num_updates%22%3A1%7D&status=200&content-type=application%2Fjson&undefined='
	echo "Setting response templates"
	curl -X POST \
  http://$GATEWAY_URL/as/registerTemplate/response/$USER/$CUBE_APPLICATION/movieinfo/minfo/listmovies \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
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
  http://$GATEWAY_URL/as/registerTemplate/response/$USER/$CUBE_APPLICATION/movieinfo/minfo/liststores \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
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
  http://$GATEWAY_URL/as/registerTemplate/response/$USER/$CUBE_APPLICATION/movieinfo/minfo/rentmovie \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
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
  http://$GATEWAY_URL/as/registerTemplate/response/$USER/$CUBE_APPLICATION/movieinfo/minfo/returnmovie \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
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
  -d "paths=minfo%2Flistmovies&paths=minfo%2Fliststores&paths=minfo%2Frentmovie&paths=minfo%2Freturnmovie&endpoint=http://$GATEWAY_URL&instanceid=$CUBE_INSTANCEID" | awk -F ',' '{print $7}' | cut -d '"' -f 4)
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
	kubectl delete -f moviebook/moviebook-envoy-replay-cs.yaml
	kubectl delete -f moviebook/mock-all-except-moviebook.yaml
	kubectl delete -f moviebook/moviebook.yaml
	kubectl delete -f cube/service.yaml 2> /dev/null
	kubectl delete -f cube/service_entry.yaml
	kubectl delete -f moviebook-gateway.yaml
	kubectl delete -f moviebook/moviebook_virtualservice.yaml
	kubectl delete -f cube/virtualservice.yaml
	kubectl delete -f cube/solr_service_entry.yaml
	kubectl delete deployments cubews 2> /dev/null
	if [ -f replayid.temp ]; then
	  rm replayid.temp
	fi
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
# To debug this script, run it with TRACE=1 in the enviornment
# -x option will trace each command that is run
  set -o pipefail; [[ "$TRACE" ]] && set -x
	get_environment
  case "$1" in
    init) shift; init "$@";;
    setup) shift; setup "$@";;
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
