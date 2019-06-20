#!/bin/bash

#syntax to use this script ./deploy.sh appname operation Configration

generate_menifest() {
	APP_CONF=$APP_DIR/config/"$1".conf
	if [ ! -f "$APP_CONF" ]; then #Check if config file exist
		echo "Configration files does not exist"
		exit 1
	fi
	if [ "$OPERATION" = "init" ]; then
		source $APP_CONF
		COMMON_DIR=common
		./generate_yamls.py $COMMON_DIR $NAMESPACE $NAMESPACE_HOST
		./generate_yamls.py $APP_DIR $NAMESPACE $NAMESPACE_HOST
	fi
}

init() {
	kubectl apply -f $COMMON_DIR/kubernetes/namespace.yaml
	kubectl apply -f $COMMON_DIR/kubernetes/secret.yaml
	kubectl apply -f $COMMON_DIR/kubernetes/gateway.yaml
	kubectl apply -f $APP_DIR/kubernetes
}
main () {
	# To debug this script, run it with TRACE=1 in the enviornment
	# -x option will trace each command that is run
	set -o pipefail; [[ "$TRACE" ]] && set -x
	if [ -d $1 ]; then #Check if the App directory exists
		APP_DIR=$1
		shift
	else
		echo "App directory does not exist"
		exit 1 #Exist with nonzero exit code
	fi
	case "$1" in
		init) OPERATION="init"; shift; generate_menifest $1; shift; init "$@";;
	esac
}

main "$@"