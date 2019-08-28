#!/usr/bin/env bash

# Usage
# ./create_collection.sh cube_image_tag environment version collection_name no_of_request_to_moviebook
update_deployment() {
	kubectl -n $NAMESPACE set image deployment/cubews-mock-v1 cubews=cubeiocorp/cubews:$TAG
	kubectl -n $NAMESPACE set image deployment/cubews-record-v1 cubews=cubeiocorp/cubews:$TAG
	kubectl -n $NAMESPACE set image deployment/cubews-replay-v1 cubews=cubeiocorp/cubews:$TAG
	sleep 30
}

call_deploy_script() {
	./deploy.sh $1 $2 $3 $4
}

generate_traffic() {
	#for ((i=1;i<=$1;i++)); do
	#	curl -X GET "http://$NAMESPACE_HOST/minfo/listmovies?filmName=BEVERLY%20OUTLAW" -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache';
	#done
	java -jar lib/MIClient-V1-SNAPSHOT-jar-with-dependencies.jar http://$NAMESPACE_HOST/minfo $1
}

main() {
	set -eo pipefail; [[ "$TRACE" ]] && set -x
	TAG=$1; shift
	CUBE_ENVIRONMENT=$1; shift
	VERSION=$1; shift
	COLLECTION_NAME=$1; shift
	NO_OF_REQUEST=$1; shift
	MB_COLLECTION_NAME=mb-$COLLECTION_NAME
	source apps/moviebook/config/"$CUBE_ENVIRONMENT".conf
	update_deployment $TAG
	call_deploy_script cube record $CUBE_ENVIRONMENT $COLLECTION_NAME
	sleep 240
	call_deploy_script moviebook record $CUBE_ENVIRONMENT $MB_COLLECTION_NAME
	sleep 240
	generate_traffic $NO_OF_REQUEST
	#exit 1
	sleep 240
	call_deploy_script moviebook stop_record $CUBE_ENVIRONMENT
	sleep 120
	call_deploy_script moviebook setup_replay $CUBE_ENVIRONMENT $VERSION
	sleep 240
	call_deploy_script moviebook replay $CUBE_ENVIRONMENT $MB_COLLECTION_NAME
	sleep 240
	#call_deploy_script moviebook stop_replay $CUBE_ENVIRONMENT
	call_deploy_script moviebook analyze $CUBE_ENVIRONMENT
	sleep 240
	call_deploy_script cube stop_record $CUBE_ENVIRONMENT
}

main "$@"
