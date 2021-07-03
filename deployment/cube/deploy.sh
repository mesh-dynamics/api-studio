#!/bin/bash

init () {
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
