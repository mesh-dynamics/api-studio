#!/usr/bin/env bash

generate_config_file() {
	echo "
NAMESPACE=$DRONE_COMMIT_AUTHOR
NAMESPACE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_APP=Cube
CUBE_CUSTOMER=CubeCorp
INSTANCEID=prod
REPLAY_ENDPOINT=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_SERVICE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
SPRINGBOOT_PROFILE=prod
CUBEIO_TAG=$DRONE_COMMIT-$DRONE_BRANCH
CUBEUI_TAG=master-latest
CUBEUI_BACKEND_TAG=master-latest
MOVIEINFO_TAG=master-latest
AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZXNoZHluYW1pY3MuaW8iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoxLCJpYXQiOjE1ODI4ODE2MjgsImV4cCI6MTg5ODI0MTYyOH0.P4DAjXyODV8cFPgObaULjAMPg-7xSbUsVJ8Ohp7xTQI"
SOLR_CORE=cube" > apps/cube/config/temp.conf

echo "
NAMESPACE=$DRONE_COMMIT_AUTHOR
NAMESPACE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_APP=MovieInfo
CUBE_CUSTOMER=CubeCorp
INSTANCEID=prod
REPLAY_PATHS=minfo/listmovies,minfo/liststores,minfo/rentmovie,minfo/returnmovie
REPLAY_ENDPOINT=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZXNoZHluYW1pY3MuaW8iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoxLCJpYXQiOjE1ODI4ODE2MjgsImV4cCI6MTg5ODI0MTYyOH0.P4DAjXyODV8cFPgObaULjAMPg-7xSbUsVJ8Ohp7xTQI"
CUBEIO_TAG=$DRONE_COMMIT-$DRONE_BRANCH
CUBEUI_TAG=master-latest
CUBEUI_BACKEND_TAG=master-latest
MOVIEINFO_TAG=master-latest" > apps/moviebook/config/temp.conf
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
		$CUBE_ENDPOINT/api/rs/start/$RECORDING_ID \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H "Authorization: Bearer $AUTH_TOKEN" \
		-H 'cache-control: no-cache' \
		-d $BODY \
	| sed 's/^.*"replayId":"\([^"]*\)".*/\1/')

	echo "REPLAYID:" $REPLAY_ID

	#Status Check
	COUNT=0
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "30" ]; do
		STATUS=$(curl -X GET $CUBE_ENDPOINT/api/rs/status/$REPLAY_ID -H "Authorization: Bearer $AUTH_TOKEN" | jq .status)
		sleep 20
		COUNT=$((COUNT+1))
	done
}

analyze() {
	ANALYZE=$(curl -X POST $CUBE_ENDPOINT/api/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache')
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
	while [ "$STATUS" != 1 ] && [ "$COUNT" != "20" ]; do
		kubectl get ns $DRONE_COMMIT_AUTHOR
		STATUS=$(echo $?)
		COUNT=$((COUNT+1))
		echo "Waiting for previous test to finish"
		sleep 30
	done
}

main() {
	set -x
	AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZXNoZHluYW1pY3MuaW8iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoxLCJpYXQiOjE1ODI4ODE2MjgsImV4cCI6MTg5ODI0MTYyOH0.P4DAjXyODV8cFPgObaULjAMPg-7xSbUsVJ8Ohp7xTQI"
	check_test_status
	generate_config_file
	CONFIG_FILE="temp"
	NO_OF_REQUEST=5
	VERSION="v1"
	call_deploy_script cube init $CONFIG_FILE
	kubectl get deploy -o name -l app=cube -n $DRONE_COMMIT_AUTHOR | xargs -n1 -t kubectl rollout status -n $DRONE_COMMIT_AUTHOR
	call_deploy_script moviebook init $CONFIG_FILE
	kubectl get deploy -o name -l app=moviebook -n $DRONE_COMMIT_AUTHOR | xargs -n1 -t kubectl rollout status -n $DRONE_COMMIT_AUTHOR
	call_deploy_script moviebook record $CONFIG_FILE moviebook-$DRONE_BUILD_NUMBER RespPartialMatch moviebook-$DRONE_BUILD_NUMBER
	sleep 30
	generate_traffic $NO_OF_REQUEST
	sleep 5
	call_deploy_script moviebook stop_record $CONFIG_FILE
	call_deploy_script moviebook setup_replay $CONFIG_FILE $VERSION
	sleep 10
	kubectl get deploy -o name -l app=moviebook -n $DRONE_COMMIT_AUTHOR | xargs -n1 -t kubectl rollout restart -n $DRONE_COMMIT_AUTHOR
	kubectl get deploy -o name -l app=moviebook -n $DRONE_COMMIT_AUTHOR | xargs -n1 -t kubectl rollout status -n $DRONE_COMMIT_AUTHOR
	sleep 60

	call_replay
	sleep 20
	analyze
	call_deploy_script moviebook clean $CONFIG_FILE
	call_deploy_script cube clean $CONFIG_FILE
	kubectl delete ns $DRONE_COMMIT_AUTHOR
	echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
	exit $EXIT_CODE
}

main "$@"
