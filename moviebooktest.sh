#!/usr/bin/env bash

generate_config_file() {

	echo "
NAMESPACE=$DRONE_COMMIT_AUTHOR
NAMESPACE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
STAGING_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_APP=Cube
CUBE_CUSTOMER=CubeCorp
INSTANCEID=prod
MASTER_NAMESPACE=dummy
REPLAY_ENDPOINT=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_SERVICE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
SPRINGBOOT_PROFILE=prod
CUBEIO_TAG=$DRONE_COMMIT-$DRONE_BRANCH
CUBEUI_TAG=master-latest
CUBEUI_BACKEND_TAG=master-latest
MOVIEINFO_TAG=master-latest
SOLR_CORE=cube" > apps/cube/config/temp.conf

echo "
NAMESPACE=$DRONE_COMMIT_AUTHOR
MASTER_NAMESPACE=$DRONE_COMMIT_AUTHOR
NAMESPACE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
STAGING_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_APP=MovieInfo
CUBE_CUSTOMER=CubeCorp
INSTANCEID=prod
CUBE_SERVICE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
SPRINGBOOT_PROFILE=prod
CUBEIO_TAG=$DRONE_COMMIT-$DRONE_BRANCH
CUBEUI_TAG=master-latest
CUBEUI_BACKEND_TAG=master-latest
MOVIEINFO_TAG=master-latest
SOLR_CORE=cube" > apps/moviebook/config/temp.conf
}

call_deploy_script() {
	TRACE=1 ./deploy.sh $@
}

call_replay() {
	RECORDING_ID=$(cat apps/moviebook/kubernetes/recording_id.temp)
	REPLAY_PATHS=minfo/listmovies,minfo/returnmovie,minfo/rentmovie,minfo/liststores
	REPLAY_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
	CUBE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
	INSTANCE_ID=prod
	USER_ID=demo@cubecorp.io
	REPLAY_PATHS=$(echo $REPLAY_PATHS | tr "," "\n")
	for path in $REPLAY_PATHS
	do
	  TEMP_PATH="$TEMP_PATH""paths=$path&"
	done

	REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
	BODY="$REPLAY_PATHS&endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&templateSetVer=DEFAULT&userId=$USER_ID"

	REPLAY_ID=$(curl -X POST \
		$CUBE_ENDPOINT/rs/start/$RECORDING_ID \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H 'cache-control: no-cache' \
		-d $BODY \
	| sed 's/^.*"replayId":"\([^"]*\)".*/\1/')

	echo "REPLAYID:" $REPLAY_ID

	#Status Check
	COUNT=0
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "20" ]; do
		STATUS=$(curl -X GET $CUBE_ENDPOINT/rs/status/CubeCorp/MovieInfo/moviebook-$DRONE_BUILD_NUMBER/$REPLAY_ID | sed 's/^.*"status":"\([^"]*\)".*/\1/')
		sleep 5
		COUNT=$((COUNT+1))
	done
}

analyze() {
	ANALYZE=$(curl -X POST $CUBE_ENDPOINT/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache')
	REQNOTMATCHED=$(echo $ANALYZE | sed 's/^.*"reqNotMatched":\([^"]*\).*/\1/' | cut -d ',' -f 1)
	RESPNOTMATCHED=$(echo $ANALYZE | sed 's/^.*"respNotMatched":\([^"]*\).*/\1/' | cut -d ',' -f 1)

	#Display replay ID
	echo "Replay ID:" $REPLAY_ID
	#Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
	if [ "$RESPNOTMATCHED" = "0" ]; then
		TEST_STATUS="test passed"
		EXIT_CODE=0
	else
		TEST_STATUS="test failed"
		EXIT_CODE=1
	fi
}

generate_traffic() {
	for ((i=1;i<=$1;i++)); do
		curl -X GET "https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io/minfo/listmovies?filmName=BEVERLY%20OUTLAW" -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache';
	done
}

check_test_status() {
	COUNT=0
	while [ "$STATUS" != 1 ] && [ "$COUNT" != "30" ]; do
		kubectl get ns $DRONE_COMMIT_AUTHOR
		STATUS=$(echo $?)
		COUNT=$((COUNT+1))
		echo "Waiting for previous test to finish"
		sleep 30
	done
}

main() {
	set -x
	check_test_status
	generate_config_file
	CONFIG_FILE="temp"
	NO_OF_REQUEST=10
	VERSION="v1"
	call_deploy_script cube init $CONFIG_FILE
	sleep 60
	call_deploy_script moviebook init $CONFIG_FILE
	sleep 60
	call_deploy_script moviebook record $CONFIG_FILE moviebook-$DRONE_BUILD_NUMBER RespPartialMatch moviebook-$DRONE_BUILD_NUMBER
	sleep 5
	generate_traffic $NO_OF_REQUEST
	sleep 5
	call_deploy_script moviebook stop_record $CONFIG_FILE
	call_deploy_script moviebook setup_replay $CONFIG_FILE $VERSION
	sleep 20
	call_replay
	analyze
	call_deploy_script moviebook clean $CONFIG_FILE
	call_deploy_script cube clean $CONFIG_FILE
	kubectl delete ns $DRONE_COMMIT_AUTHOR
	echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
	exit $EXIT_CODE
}

main "$@"
https