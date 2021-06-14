#!/bin/bash

init () {
	#Download an install Istio
	curl -L https://git.io/getLatestIstio | ISTIO_VERSION=1.3.8 sh -
	kubectl create namespace istio-system
	for i in istio-1.3.8/install/kubernetes/helm/istio-init/files/crd*yaml; do kubectl apply -f $i; done
	helm template istio-1.3.8/install/kubernetes/helm/istio --name istio --namespace istio-system --set global.proxy.accessLogFile="/dev/stdout" | kubectl apply -f -
	#Cleanup
	rm -rf istio-1.3.8
	exit 0
	#Install cube Application
	helm template . | kubectl apply -f -
	GATEWAYIP=$(kubectl describe services istio-ingressgateway -n istio-system | grep "LoadBalancer Ingress" | awk '{print $3}')
	echo "Kindly add the DNS entry, Ingress gateway IP:$GATEWAYIP"
}

record () {
#start_record function can have 2 arguments, if these arguments are passed then
#script will run in non-interactive mode:
# $1 collection name
# $2 golden name
	source deploy.cfg
	if [ -z "$1" ]; then
		echo "Enter collection name"
		read COLLECTION_NAME
	else
		COLLECTION_NAME=$1
	fi
	if [ -z "$2" ]; then
		echo "Enter unique recording name"
		read GOLDEN_NAME
	else
		GOLDEN_NAME=$2
	fi
	BODY="name=$GOLDEN_NAME&userId=$CUBE_CUSTOMER"
	RESPONSE="$(curl -X POST \
  http://$CUBE_HOST/api/cs/start/$CUBE_CUSTOMER/$CUBE_APP/$INSTANCEID/$COLLECTION_NAME/$TEMPLATE_VERSION \
  -H 'Content-Type: application/x-www-form-urlencoded' \
	-H "Authorization: Bearer $AUTH_TOKEN" \
  -H 'cache-control: no-cache'\
  -d "$BODY" )"
	RECORDING_ID=$(echo $RESPONSE | sed 's/^.*"id":"\([^"]*\)".*/\1/')
	echo "RECORDING_ID:" $RECORDING_ID
	echo $RECORDING_ID > recordingid.tmp
}

stop_recording() {
source deploy.cfg
RECORDING_ID=$(cat recordingid.tmp)
echo "Stopping recording for recording ID:" $RECORDING_ID

curl -X POST \
http://$CUBE_HOST/api/cs/stop/$RECORDING_ID \
-H 'Content-Type: application/x-www-form-urlencoded' \
-H "Authorization: Bearer $AUTH_TOKEN" \
-H 'cache-control: no-cache'
}
main () {
# To debug this script, run it with TRACE=1 in the enviornment
	# -x option will trace each command that is run
	set -eo pipefail; [[ "$TRACE" ]] && set -x
	case "$1" in
		init) shift; init "$@";;
		record) shift; record "$@";;
		stop_recording) shift; stop_recording "$@";;
		*) echo "Invalid option"
	esac
}

main "$@"
