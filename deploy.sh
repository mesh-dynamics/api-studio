#!/bin/bash

#syntax to use this script ./deploy.sh appname operation Configration

generate_manifest() {
	APP_CONF=$APP_DIR/config/"$1".conf
	if [ ! -f "$APP_CONF" ]; then #Check if config file exist
		echo "Configration files does not exist"
		exit 1
	fi
	source $APP_CONF
	if [ "$OPERATION" = "init" ]; then
		find $APP_DIR/kubernetes -name "*.yaml" -type f -delete #Delete old files
		COMMON_DIR=apps/common
		./generate_yamls.py $OPERATION $COMMON_DIR $NAMESPACE $CUBE_APP $CUBE_CUSTOMER $CUBE_SERVICE_ENDPOINT $NAMESPACE_HOST $CUBE_HOST $STAGING_HOST $INSTANCEID $SPRINGBOOT_PROFILE $SOLR_CORE
		./generate_yamls.py $OPERATION $APP_DIR $NAMESPACE $CUBE_APP $CUBE_CUSTOMER $CUBE_SERVICE_ENDPOINT $NAMESPACE_HOST $CUBE_HOST $STAGING_HOST $INSTANCEID $SPRINGBOOT_PROFILE $SOLR_CORE
	elif [ "$OPERATION" = "record" ] || [ "$OPERATION" = "replay" ]; then
		./generate_yamls.py $OPERATION $APP_DIR $NAMESPACE $CUBE_APP $CUBE_CUSTOMER $INSTANCEID $MASTER_NAMESPACE
	fi
}

init() {
	kubectl apply -f $COMMON_DIR/kubernetes/namespace.yaml
	#Automatically inject Envoy container in application Pod if they are started in namespaces labeled with istio-injection=enabled
	kubectl label namespace $NAMESPACE istio-injection=enabled || : #http://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html#tag_18_16
	kubectl apply -f $COMMON_DIR/kubernetes/secret.yaml
	kubectl apply -f $COMMON_DIR/kubernetes/gateway.yaml
	kubectl apply -f $APP_DIR/kubernetes
	#Check if route exists
	if ls $APP_DIR/kubernetes/route* 1> /dev/null 2>&1; then
		kubectl apply -f $APP_DIR/kubernetes/route-v1.yaml
	fi

}

register_matcher() {
echo "Registering Templates"
./update_templates.py $1 $GATEWAY_URL $CUBE_CUSTOMER $CUBE_APP $NAMESPACE_HOST $APP_DIR
}

start_record() {
	if [ -z "$1" ]; then
		echo "Enter collection name"
		read COLLECTION_NAME
	else
		COLLECTION_NAME=$1
	fi
	kubectl apply -f $APP_DIR/kubernetes/envoy-record-cs.yaml
	curl -X POST \
  http://$GATEWAY_URL/cs/start/$CUBE_CUSTOMER/$CUBE_APP/$INSTANCEID/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
	-H "Host:$CUBE_HOST" \
  -H 'cache-control: no-cache'
}

stop_record() {
	COLLECTION_NAME=$(curl -X GET \
  "http://$GATEWAY_URL/cs/currentcollection?customerid=$CUBE_CUSTOMER&app=$CUBE_APP&instanceid=$INSTANCEID" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
	-H "Host:$CUBE_HOST" \
  -H 'cache-control: no-cache')
	echo "recording complete"

	curl -X POST \
	http://$GATEWAY_URL/cs/stop/$CUBE_CUSTOMER/$CUBE_APP/$COLLECTION_NAME \
  -H 'Content-Type: application/x-www-form-urlencoded' \
	-H "Host:$CUBE_HOST" \
  -H 'cache-control: no-cache'
	kubectl delete -f $APP_DIR/kubernetes/envoy-record-cs.yaml
}

replay_setup() {
	kubectl apply -f $APP_DIR/kubernetes/envoy-replay-cs.yaml
	if ls $APP_DIR/kubernetes/mock-all-except-* 1> /dev/null 2>&1; then
		kubectl apply -f $APP_DIR/kubernetes/mock-all-except-$APP_NAME.yaml
	fi
	if ls $APP_DIR/kubernetes/route* 1> /dev/null 2>&1; then
		if [ -z "$1" ]; then
			echo "Which version of App you want to test?(v1/v2)"
			read VERSION
		else
			VERSION=$1
		fi
		if [ "$VERSION" = "v1" ] || [ "$VERSION" = "v2" ]; then
			kubectl apply -f $APP_DIR/kubernetes/route-$VERSION.yaml
		else
			echo "echo Invalid, enter a valid version(v1/v2)"
			exit 1
		fi
	fi
}

replay() {
	if [ -z "$1" ]; then
		echo "Enter collection name to replay"
		read COLLECTION_NAME
	else
		COLLECTION_NAME=$1
	fi
	if [ ! -z "$REPLAY_PATHS" ]; then
		REPLAY_PATHS=$(echo $REPLAY_PATHS | tr "," "\n")
		for path in $REPLAY_PATHS
		do
			TEMP_PATH="$TEMP_PATH""paths=$path&"
		done
		REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
		BODY="$REPLAY_PATHS&endpoint=http://$REPLAY_ENDPOINT&instanceid=$INSTANCEID"
	else
		BODY="endpoint=http://$REPLAY_ENDPOINT&instanceid=$INSTANCEID"
	fi
	#Make replay init call and get replay ID
	REPLAY_ID=$(curl -X POST \
	http://$GATEWAY_URL/rs/init/$CUBE_CUSTOMER/$CUBE_APP/$COLLECTION_NAME \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'cache-control: no-cache' \
	-H "Host: $CUBE_HOST" \
	-d "$BODY" | sed 's/^.*"replayid":"\([^"]*\)".*/\1/')
	#Make reply start call
	curl -f -X POST \
  http://$GATEWAY_URL/rs/start/$CUBE_CUSTOMER/$CUBE_APP/$COLLECTION_NAME/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	-H "Host: $CUBE_HOST"
	if [ $? -eq 0 ]; then
		echo "Replay started"
	else
		echo "ERROR!! Replay did not started"
	fi
	echo $REPLAY_ID > $APP_DIR/kubernetes/replayid.temp
}

stop_replay() {
	kubectl delete -f $APP_DIR/kubernetes/envoy-replay-cs.yaml
	if ls $APP_DIR/kubernetes/mock-all-except-* 1> /dev/null 2>&1; then
		kubectl delete -f $APP_DIR/kubernetes/mock-all-except-$APP_NAME.yaml
	fi
}

analyze() {
	REPLAY_ID=$(cat $APP_DIR/kubernetes/replayid.temp)
	echo "Analyzing for replay ID:" $REPLAY_ID
	curl -X POST \
	  http://$GATEWAY_URL/as/analyze/$REPLAY_ID \
	  -H 'Content-Type: application/x-www-form-urlencoded' \
	  -H 'cache-control: no-cache' \
		-H "Host: $CUBE_HOST"
}

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

get_environment() {
	ENVIRONMENT=$(kubectl config current-context)
	if [ "$ENVIRONMENT" = "minikube" ]; then
	  export_dev_env_variables #export gatway URL for minikube
	  echo "Environment varibales set for dev environemt"
	else
	  export_aws_env_variables #export gatway URL for AWS
	  echo "Environment varibales set for AWS environment"
	fi
}

clean() {
	kubectl delete namespace $NAMESPACE
}
main () {
	# To debug this script, run it with TRACE=1 in the enviornment
	# -x option will trace each command that is run
	set -eo pipefail; [[ "$TRACE" ]] && set -x
	if [ -d "apps/$1" ]; then #Check if the App directory exists
		APP_NAME=$1
		APP_DIR="apps/$APP_NAME"
		shift
	else
		echo "App directory does not exist"
		exit 1 #Exist with nonzero exit code
	fi
	get_environment #check kubernetes context
	case "$1" in
		init) OPERATION="init"; shift; generate_manifest $1; shift; init "$@";;
		record) OPERATION="record"; shift; generate_manifest $1; shift; start_record "$@";;
		stop_record) OPERATION="record"; shift; generate_manifest $1; shift; stop_record "$@";;
		setup_replay) OPERATION="replay"; shift; generate_manifest $1; shift; replay_setup "$@";;
		replay) OPERATION="replay"; shift; generate_manifest $1; shift; replay "$@";;
		stop_replay) OPERATION="stopreplay"; shift; generate_manifest $1; shift; stop_replay "$@";;
		register_matcher) OPERATION="none"; shift; generate_manifest $1; shift; register_matcher "$@";;
		analyze) OPERATION="analyze"; shift; generate_manifest $1; shift; analyze "$@";;
		clean) OPERATION="clean"; shift; generate_manifest $1; shift; clean "$@";;
		*) echo "This script expect one of these system argument(init, record, stop_record, setup_replay, replay, stop_replay, register_matcher, analyze, clean)."
	esac
}

main "$@"
