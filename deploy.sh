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
		#TODO: why not delete everything? Otherwise fluentd_patch_*.json remain
		CUBEIO_TAG=$(git ls-remote git@github.com:cube-io-corp/cubeio.git refs/heads/master | awk '{print $1}')-master
		CUBEUI_TAG=$(git ls-remote git@github.com:cube-io-corp/cubeui.git refs/heads/master | awk '{print $1}')-master
		CUBEUI_BACKEND_TAG=$(git ls-remote git@github.com:cube-io-corp/cubeui-backend.git refs/heads/master | awk '{print $1}')-master
		MOVIEINFO_TAG=$(git ls-remote git@github.com:cube-io-corp/cubeui-backend.git refs/heads/master | awk '{print $1}')-master
		find $APP_DIR/kubernetes -name "*.yaml" -type f -delete #Delete old files
		COMMON_DIR=apps/common
		./generate_yamls.py $OPERATION $COMMON_DIR $NAMESPACE $CUBE_APP $CUBE_CUSTOMER $CUBE_SERVICE_ENDPOINT $NAMESPACE_HOST $CUBE_HOST $STAGING_HOST $INSTANCEID $SPRINGBOOT_PROFILE $SOLR_CORE $CUBEIO_TAG $CUBEUI_TAG $CUBEUI_BACKEND_TAG $MOVIEINFO_TAG
		./generate_yamls.py $OPERATION $APP_DIR $NAMESPACE $CUBE_APP $CUBE_CUSTOMER $CUBE_SERVICE_ENDPOINT $NAMESPACE_HOST $CUBE_HOST $STAGING_HOST $INSTANCEID $SPRINGBOOT_PROFILE $SOLR_CORE $CUBEIO_TAG $CUBEUI_TAG $CUBEUI_BACKEND_TAG $MOVIEINFO_TAG
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
	# TODO: This tries to apply fluentd_path_*.jsons which are not valid
	kubectl apply -f $APP_DIR/kubernetes || :
	kubectl patch ds fluentd --type=json --patch "$(cat $APP_DIR/kubernetes/fluentd_patch.json)" -n logging --record
	#Check if route exists
	if ls $APP_DIR/kubernetes/route* 1> /dev/null 2>&1; then
		kubectl apply -f $APP_DIR/kubernetes/route-v1.yaml
	fi

}

register_matcher() {
echo "Registering Templates"
if [ -z "$1" ]; then
		echo "Enter template_scenario"
		read TEMPLATE_SCENARIO
	else
		TEMPLATE_SCENARIO=$1
	fi
./update_templates.py $TEMPLATE_SCENARIO $GATEWAY_URL $CUBE_CUSTOMER $CUBE_APP $TEMPLATE_VERSION_TEMP_FILE $NAMESPACE_HOST $APP_DIR
}

set_default() {
echo "Setting default responses for RESTWrap!"

NOW=$(date +%Y-%m-%dT%H:%M:%SZ)

# reqId cannot be NA as we are creating Solr doc id with reqId value. Otherwise second request
# will override the first request here.

RESPONSE="$(curl -X POST \
  http://$GATEWAY_URL/cs/event/setDefaultResponse \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -H "Host:$CUBE_HOST" \
  -d '{"event":{"customerId":"CubeCorp","app":"MovieInfo","service":"restwrapjdbc","instanceId":"NA","collection":"NA",
  "traceId":"NA","runType":"Manual","timestamp":"'$NOW'","reqId":"'$NOW'","apiPath":"restsql/initialize","eventType":"HTTPRequest",
  "rawPayloadString":"{\"hdrs\":{},\"queryParams\":{},\"formParams\":{},\"method\":\"GET\",\"body\":\"\"}"},
  "rawRespPayloadString":"{\"hdrs\":{\"content-type\":[\"application\/json\"]},\"body\":\"{status:Connection pool created.}\",\"status\":200}"}')"

echo $RESPONSE

RESPONSE="$(curl -X POST \
  http://$GATEWAY_URL/cs/event/setDefaultResponse \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -H "Host:$CUBE_HOST" \
  -d '{"event":{"customerId":"CubeCorp","app":"MovieInfo","service":"restwrapjdbc",
  "instanceId":"NA","collection":"NA","traceId":"NA","runType":"Manual","timestamp":"'$NOW'",
  "reqId":"'$NOW'","apiPath":"restsql/update","eventType":"HTTPRequest",
  "rawPayloadString":"{\"hdrs\":{},\"queryParams\":{},\"formParams\":{},\"method\":\"POST\",\"body\":\"\"}"},
  "rawRespPayloadString":"{\"hdrs\":{\"content-type\":[\"application\/json\"]},\"body\":\"{num_updates:1}\",\"status\":200}"}')"

echo $RESPONSE

}

start_record() {
	if [ -z "$1" ]; then
		echo "Enter collection name"
		read COLLECTION_NAME
	else
		COLLECTION_NAME=$1
	fi

	if [ -e "$TEMPLATE_VERSION_TEMP_FILE" ]; then
		echo "Picking up template_version from file $TEMPLATE_VERSION_TEMP_FILE. Okay ? y/n"
		read USER_INP_TEMPLATE_VERSION
	else
		USER_INP_TEMPLATE_VERSION="n"
	fi

	if [ $USER_INP_TEMPLATE_VERSION = "y" ]; then
    local TEMPLATE_VERSION=$(cat "$TEMPLATE_VERSION_TEMP_FILE")
 		echo "Template version : $TEMPLATE_VERSION"
	else
		echo "Enter Template version"
		read TEMPLATE_VERSION
	fi

  echo "Enter unique recording name"
	read GOLDEN_NAME

	BODY="name=$GOLDEN_NAME&userId=$CUBE_CUSTOMER"

	kubectl apply -f $APP_DIR/kubernetes/envoy-record-cs.yaml

	RESPONSE="$(curl -X POST \
  http://$GATEWAY_URL/cs/start/$CUBE_CUSTOMER/$CUBE_APP/$INSTANCEID/$COLLECTION_NAME/$TEMPLATE_VERSION \
  -H 'Content-Type: application/x-www-form-urlencoded' \
	-H "Host:$CUBE_HOST" \
  -H 'cache-control: no-cache'\
  -d "$BODY" )"
  echo $RESPONSE
  RECORDING_ID=$(echo $RESPONSE | sed 's/^.*"id":"\([^"]*\)".*/\1/')
  echo "RECORDING_ID:" $RECORDING_ID

  if [ $? -eq 0 ]; then
		echo "Recording started"
	else
		echo "ERROR!! Recording did not started"
	fi
	echo $RECORDING_ID > $RECORDING_ID_TEMP_FILE
}

stop_record() {
  RECORDING_ID=$(cat "$RECORDING_ID_TEMP_FILE")
  echo "Stopping recording for recording ID:" $RECORDING_ID

	curl -X POST \
	http://$GATEWAY_URL/cs/stop/$RECORDING_ID \
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
	if [ ! -z "$REPLAY_PATHS" ]; then
		REPLAY_PATHS=$(echo $REPLAY_PATHS | tr "," "\n")
		for path in $REPLAY_PATHS
		do
			TEMP_PATH="$TEMP_PATH""paths=$path&"
		done
		REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
		BODY="$REPLAY_PATHS&endPoint=http://$REPLAY_ENDPOINT&instanceId=$INSTANCEID&userId=$CUBE_CUSTOMER"
	else
		BODY="endPoint=http://$REPLAY_ENDPOINT&instanceId=$INSTANCEID&userId=$CUBE_CUSTOMER"
	fi

	if [ -e "$RECORDING_ID_TEMP_FILE" ]; then
		echo "Picking up Recording Id from file $RECORDING_ID_TEMP_FILE. Okay ? y/n"
		read USER_INP_RECORDING_ID
	else
		USER_INP_RECORDING_ID="n"
	fi

	if [ $USER_INP_RECORDING_ID = "y" ]; then
    local RECORDING_ID=$(cat "$RECORDING_ID_TEMP_FILE")
 		echo "Recording id for replay : $RECORDING_ID"
	else
		echo "Enter Recording Id"
		read RECORDING_ID
	fi

  REPLAY_ID=$(curl -f -X POST \
  http://$GATEWAY_URL/rs/start/$RECORDING_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache' \
	-H "Host: $CUBE_HOST" \
	-d "$BODY" | sed 's/^.*"replayId":"\([^"]*\)".*/\1/')
	if [ $? -eq 0 ]; then
		echo "Replay started"
	else
		echo "ERROR!! Replay did not started"
	fi

	echo "Replay id created : $REPLAY_ID"
	echo $REPLAY_ID > $APP_DIR/kubernetes/replayid.temp
}

stop_replay() {
	kubectl delete -f $APP_DIR/kubernetes/envoy-replay-cs.yaml
	if ls $APP_DIR/kubernetes/mock-all-except-* 1> /dev/null 2>&1; then
		kubectl delete -f $APP_DIR/kubernetes/mock-all-except-$APP_NAME.yaml
	fi
}

replay_status() {
	if [ -z "$1" ]; then
		echo "Enter collection name to check replay status"
		read COLLECTION_NAME
	else
		COLLECTION_NAME=$1
	fi
	REPLAY_ID=$(cat $APP_DIR/kubernetes/replayid.temp)
	curl http://$GATEWAY_URL/rs/status/$CUBE_CUSTOMER/$CUBE_APP/$COLLECTION_NAME/$REPLAY_ID \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	  -H 'cache-control: no-cache' \
		-H "Host: $CUBE_HOST" | jq -r "."
}

analyze() {
	REPLAY_ID=$(cat $APP_DIR/kubernetes/replayid.temp)
	echo "Analyzing for replay ID:" $REPLAY_ID
	curl -X POST \
	  http://$GATEWAY_URL/as/analyze/$REPLAY_ID \
	  -H 'Content-Type: application/x-www-form-urlencoded' \
	  -H 'cache-control: no-cache' \
		-H "Host: $CUBE_HOST" | jq -r "."
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
	# TODO: fix permissions issue
	kubectl delete all --all -n $NAMESPACE
	kubectl delete virtualservices.networking.istio.io --all -n $NAMESPACE
	kubectl delete envoyfilters.networking.istio.io --all -n $NAMESPACE
	kubectl delete destinationrules.networking.istio.io --all -n $NAMESPACE
	kubectl delete gateways.networking.istio.io --all -n $NAMESPACE
	kubectl delete serviceentries.networking.istio.io --all -n $NAMESPACE
	volumeMountsindex=$(kubectl get ds fluentd -n logging -o json | jq '.spec.template.spec.containers[0].volumeMounts[].name' | awk "/fluentd-$APP_NAME-conf-$NAMESPACE/{print NR-1}")
	volumeindex=$(kubectl get ds fluentd -n logging -o json | jq '.spec.template.spec.volumes[].name' | awk "/fluentd-$APP_NAME-conf-$NAMESPACE/{print NR-1}")
	# TODO: check that volumeMountsindex and volumeindex are not empty, otherwise
	# it throws an error
	sed -e "s/add/remove/g" $APP_DIR/kubernetes/fluentd_patch.json > $APP_DIR/kubernetes/fluentd_patch_remove.json
	sed -i -e "s:/spec/template/spec/containers/0/volumeMounts/-:/spec/template/spec/containers/0/volumeMounts/$volumeMountsindex:g" $APP_DIR/kubernetes/fluentd_patch_remove.json
	sed -i -e "s:/spec/template/spec/volumes/-:/spec/template/spec/volumes/$volumeindex:g" $APP_DIR/kubernetes/fluentd_patch_remove.json
	rm $APP_DIR/kubernetes/fluentd_patch_remove.json-e
	kubectl patch ds fluentd --type=json --patch "$(cat $APP_DIR/kubernetes/fluentd_patch_remove.json)" -n logging --record

}

update_deployment() {
	source $APP_DIR/scripts/update_deployment.sh
	update_deployment_app "$@"
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
	TEMPLATE_VERSION_TEMP_FILE=$APP_DIR/kubernetes/template_version.temp
	RECORDING_ID_TEMP_FILE=$APP_DIR/kubernetes/recording_id.temp
	get_environment #check kubernetes context
	case "$1" in
		init) OPERATION="init"; shift; generate_manifest $1; shift; init "$@";;
		record) OPERATION="record"; shift; generate_manifest $1; shift; start_record "$@";;
		stop_record) OPERATION="record"; shift; generate_manifest $1; shift; stop_record "$@";;
		setup_replay) OPERATION="replay"; shift; generate_manifest $1; shift; replay_setup "$@";;
		replay) OPERATION="replay"; shift; generate_manifest $1; shift; replay "$@";;
		stop_replay) OPERATION="stopreplay"; shift; generate_manifest $1; shift; stop_replay "$@";;
		replay_status) OPERATION="replay_status"; shift; generate_manifest $1; shift; replay_status "$@";;
		register_matcher) OPERATION="none"; shift; generate_manifest $1; shift; register_matcher "$@";;
		set_default) OPERATION="none"; shift; generate_manifest $1; shift; set_default "$@";;
		analyze) OPERATION="analyze"; shift; generate_manifest $1; shift; analyze "$@";;
		update) OPERATION="update"; shift; generate_manifest $1; shift; update_deployment "$@";;
		clean) OPERATION="clean"; shift; generate_manifest $1; shift; clean "$@";;
		*) echo "This script expect one of these system argument(init, record, stop_record, setup_replay, replay, stop_replay, register_matcher, set_default, analyze, clean)."
	esac
}

main "$@"
