#!/usr/bin/env bash

generate_config_file() {
	echo "
NAMESPACE=$DRONE_COMMIT_AUTHOR
NAMESPACE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_APP=Cube
CUBE_CUSTOMER=CubeCorp
INSTANCEID=$DRONE_COMMIT
REPLAY_ENDPOINT=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_SERVICE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
SPRINGBOOT_PROFILE=dev
CUBEIO_TAG=$DRONE_COMMIT-$DRONE_BRANCH
CUBEUI_TAG=develop-latest
CUBEUI_BACKEND_TAG=develop-latest
MOVIEINFO_TAG=master-latest
SOLR_URL=http://solr-svc.solr.svc.cluster.local:8983/solr/
AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
SOLR_CORE=cube" > apps/cube/config/temp.conf

echo "
NAMESPACE=$DRONE_COMMIT_AUTHOR
NAMESPACE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
CUBE_APP=MovieInfo
CUBE_CUSTOMER=CubeCorp
INSTANCEID=$DRONE_COMMIT
REPLAY_PATHS=minfo/listmovies,minfo/liststores,minfo/rentmovie,minfo/returnmovie
REPLAY_ENDPOINT=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
CUBEIO_TAG=$DRONE_COMMIT-$DRONE_BRANCH
CUBEUI_TAG=develop-latest
CUBEUI_BACKEND_TAG=develop-latest
MOVIEINFO_TAG=master-latest" > apps/moviebook/config/temp.conf

echo "
NAMESPACE=$DRONE_COMMIT_AUTHOR-springboot
NAMESPACE_HOST=$DRONE_COMMIT_AUTHOR-springboot.dev.cubecorp.io
CUBE_APP=springboot_demo
CUBE_CUSTOMER=CubeCorp
INSTANCEID=$DRONE_COMMIT
AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
CUBEIO_TAG=$DRONE_COMMIT-$DRONE_BRANCH
CUBEUI_TAG=develop-latest
CUBEUI_BACKEND_TAG=develop-latest
MOVIEINFO_TAG=master-latest
CUBE_HOST=$DRONE_COMMIT_AUTHOR.dev.cubecorp.io" > apps/springboot/config/temp.conf
}

call_deploy_script() {
	TRACE=1 ./deploy.sh $@
	if [ $? -ne 0 ]; then
		EXIT_CODE=1
		clean
	fi
}

call_replay() {
	RECORDING_ID=$(cat apps/moviebook/kubernetes/recording_id.temp)
	REPLAY_PATHS=minfo/listmovies,minfo/returnmovie,minfo/rentmovie,minfo/liststores
	REPLAY_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
	CUBE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
	INSTANCE_ID=$DRONE_COMMIT
	USER_ID=demo@cubecorp.io
	REPLAY_PATHS=$(echo $REPLAY_PATHS | tr "," "\n")
	for path in $REPLAY_PATHS
	do
	  TEMP_PATH="$TEMP_PATH""paths=$path&"
	done

	REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
	BODY="$REPLAY_PATHS&endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&templateSetVer=DEFAULT&userId=$USER_ID&transforms=$TRANSFORMS"

	COUNT=0
	while [ "$http_code" != "200" ] || [ "$REPLAY_ID" = "none" ] && [ "$COUNT" != "5" ]; do
		resp=$(curl -sw "%{http_code}" -X POST \
			$CUBE_ENDPOINT/api/rs/start/$RECORDING_ID \
			-H 'Content-Type: application/x-www-form-urlencoded' \
			-H "Authorization: Bearer $AUTH_TOKEN" \
			-H 'cache-control: no-cache' \
			-d $BODY)
			http_code="${resp:${#res}-3}"
			body="${resp:0:${#resp}-3}"
			echo $body
			REPLAY_ID=$(echo $body | jq -r ".replayId")
			COUNT=$((COUNT+1))
			if [ $COUNT -eq 5 ]; then
				echo "Replay failed to start multiple times.."
				EXIT_CODE=1
				clean
			fi
			sleep 5
	done
	echo "REPLAYID:" $REPLAY_ID

	#Status Check
	COUNT=0
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "30" ]; do
		STATUS=$(curl -X GET $CUBE_ENDPOINT/api/rs/status/$REPLAY_ID -H "Authorization: Bearer $AUTH_TOKEN" | jq .status | tr -d '"')
		sleep 20
		COUNT=$((COUNT+1))
	done
}

analyze() {
	# Stop replay before analyze
	curl --location --request POST $CUBE_ENDPOINT/api/rs/forcecomplete/$REPLAY_ID \
	-H "Authorization: Bearer $AUTH_TOKEN"

	sleep 30

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
		curl "https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io/minfo/rentmovie" -H 'Content-Type: application/json;charset=UTF-8' --data-binary '{"filmId":4,"storeId":1,"duration":2,"customerId":200,"staffId":1}'
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

clean() {
	call_deploy_script moviebook clean $CONFIG_FILE
	if [ $EXIT_CODE -ne 0 ]; then
		call_deploy_script cube clean $CONFIG_FILE
		call_deploy_script springboot clean $CONFIG_FILE
		kubectl delete ns $DRONE_COMMIT_AUTHOR
		kubectl delete ns $DRONE_COMMIT_AUTHOR-springboot
	fi
	echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
	exit $EXIT_CODE
}

main() {
	set -x
	# DRONE_BRANCH="develop"
	# DRONE_COMMIT="411e4ee4dfeb290932122f3ad56141c5b8ec6b15"
	# DRONE_COMMIT_AUTHOR="ethicalaakash"
	# DRONE_BUILD_NUMBER="test102"
	AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
	check_test_status
	generate_config_file
	CONFIG_FILE="temp"
	NO_OF_REQUEST=5
	VERSION="v1"
	TRANSFORMS="%7B%22requestTransforms%22%3A%7B%22Test%22%3A%5B%7B%22source%22%3A%22*%22%2C%22target%22%3A%22test123%22%7D%5D%7D%7D"
	call_deploy_script cube init $CONFIG_FILE
	kubectl get deploy -o name -l app=cube -n $DRONE_COMMIT_AUTHOR | xargs -n1 -t kubectl rollout status -n $DRONE_COMMIT_AUTHOR
	call_deploy_script moviebook init $CONFIG_FILE
	kubectl get deploy -o name -l app=moviebook -n $DRONE_COMMIT_AUTHOR | xargs -n1 -t kubectl rollout status -n $DRONE_COMMIT_AUTHOR
	call_deploy_script springboot init $CONFIG_FILE
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
	clean
}

main "$@"
