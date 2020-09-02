#!/usr/bin/env bash

register_config() {
# Replay configuration
DATA="{
	\"customerId\":\"CubeCorp\",
	\"app\":\"springboot_demo\",
	\"service\":\"order\",
	\"instanceId\":\"$INSTANCE_ID\",
	\"tag\":\"replay-spring-order\",
	\"configJson\" :
	{
			\"type\" : \"AgentConfig\",
			\"config\": \"{\\\"io\\\": {\\\"md\\\": {\\\"service\\\": {\\\"record\\\": \\\"$CUBE_ENDPOINT/api\\\",\\\"mock\\\": \\\"$CUBE_ENDPOINT/api\\\"},\\\"authtoken\\\": \\\"Bearer $AUTH_TOKEN\\\", \\\"read\\\": {\\\"timeout\\\": 100000},\\\"connect\\\": {\\\"timeout\\\": 100000,\\\"retries\\\": 3},\\\"intent\\\": \\\"mock\\\",\\\"samplerconfig\\\": {\\\"type\\\": \\\"simple\\\",\\\"accuracy\\\": 1000,\\\"rate\\\": 1},\\\"sampler\\\": {\\\"veto\\\": false},\\\"nodeselectionconfig\\\": {\\\"type\\\":\\\"simple\\\",\\\"accuracy\\\":1000,\\\"rate\\\":1},\\\"mock\\\":{\\\"services\\\":[\\\"transformer:8081\\\"]}}}}\"
	}
}"
curl --location --request POST $CUBE_ENDPOINT/api/cs/storeAgentConfig --header 'Content-Type: application/json' --header "Authorization: Bearer $AUTH_TOKEN" --data-raw "$DATA"
#Record configuration
DATA="{
	\"customerId\":\"CubeCorp\",
	\"app\":\"springboot_demo\",
	\"instanceId\" : \"$INSTANCE_ID\",
	\"service\":\"order\",
	\"tag\" : \"record-spring-order\",
	\"configJson\" :
	{
			\"type\" : \"AgentConfig\",
			\"config\": \"{\\\"io\\\":{\\\"md\\\":{\\\"service\\\":{\\\"record\\\":\\\"$CUBE_ENDPOINT/api\\\",\\\"mock\\\":\\\"$CUBE_ENDPOINT/api\\\"},\\\"authtoken\\\":\\\"Bearer $AUTH_TOKEN\\\",\\\"read\\\":{\\\"timeout\\\":100000},\\\"connect\\\":{\\\"timeout\\\":100000,\\\"retries\\\":3},\\\"intent\\\":\\\"record\\\",\\\"samplerconfig\\\":{\\\"type\\\":\\\"simple\\\",\\\"accuracy\\\":1000,\\\"rate\\\":1},\\\"sampler\\\":{\\\"veto\\\":false},\\\"nodeselectionconfig\\\":{\\\"type\\\":\\\"simple\\\",\\\"accuracy\\\":1000,\\\"rate\\\":1}}}}\"
	}
}"
curl --location --request POST $CUBE_ENDPOINT/api/cs/storeAgentConfig \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $AUTH_TOKEN" \
--data-raw "$DATA"
}

record() {
	DATA="{
	    \"customerId\":\"CubeCorp\",
	    \"app\":\"springboot_demo\",
	    \"instanceId\" : \"$INSTANCE_ID\",
	    \"service\":\"order\",
	    \"tag\" : \"record-spring-order\"
	}"
	curl --location --request POST $CUBE_ENDPOINT/api/cs/setCurrentAgentConfigTag/ \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $AUTH_TOKEN" \
--data-raw "$DATA"
sleep 35
  RESPONSE=$(curl -X POST \
  $CUBE_ENDPOINT/api/cs/start/CubeCorp/springboot_demo/$INSTANCE_ID/$TEMPLATE \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H 'cache-control: no-cache' \
  -d "name=cicd-$DRONE_BUILD_NUMBER&userId=CubeCorp&label=$(date +%s)")
  echo $RESPONSE
  RECORDING_ID=$(echo $RESPONSE | jq -r ".id")
  echo $RECORDING_ID
}

generate_traffic() {
  for ((i=1;i<=$1;i++)); do
    curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/getOrderByIndex?index=0"  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ'
    sleep 1
    curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/getOrders/" --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ' --header 'Content-Type: application/json'
    sleep 1
		DATA="{
	\"id\": $((i + 3)),
    \"productId\": 1,
    \"customer\": {
        \"firstName\": \"Lokesh1\",
        \"lastName\": \"Gupta1\",
        \"email\": \"xyz@gmail.com\"
    }
}"
    curl --location --request POST "$SPRINGBOOT_HOST:8080/orders/postOrder/" --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ' --header 'Content-Type: application/json' --data-raw "$DATA"
sleep 1
  done
}

stop_recording() {
  curl -X POST $CUBE_ENDPOINT/api/cs/stop/$RECORDING_ID \
  -H "Authorization: Bearer $AUTH_TOKEN"

  sleep 20

  curl -X POST $CUBE_ENDPOINT/api/cs/forcestop/$RECORDING_ID \
  -H "Authorization: Bearer $AUTH_TOKEN"
}

replay() {
	DATA="{
	    \"customerId\":\"CubeCorp\",
	    \"app\":\"springboot_demo\",
	    \"instanceId\" : \"$INSTANCE_ID\",
	    \"service\":\"order\",
	    \"tag\" : \"replay-spring-order\"
	}"
	curl --location --request POST $CUBE_ENDPOINT/api/cs/setCurrentAgentConfigTag/ \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $AUTH_TOKEN" \
--data-raw "$DATA"
sleep 35
  BODY="endPoint=$SPRINGBOOT_HOST&instanceId=$INSTANCE_ID&templateSetVer=$TEMPLATE&userId=$USER_ID&service=order"
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

  COUNT=0
  while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "30" ]; do
    STATUS=$(curl -X GET $CUBE_ENDPOINT/api/rs/status/$REPLAY_ID -H "Authorization: Bearer $AUTH_TOKEN" | jq .status | tr -d '"')
    sleep 10
    COUNT=$((COUNT+1))
  done
}
analyze() {
	# Stop replay before analyze
	curl --location --request POST $CUBE_ENDPOINT/api/rs/forcecomplete/$REPLAY_ID \
	-H "Authorization: Bearer $AUTH_TOKEN"

	sleep 30

  ANALYZE=$(curl -X POST $CUBE_ENDPOINT/api/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache')
  REQCOUNT=$(echo $ANALYZE | jq .reqCnt )
	RESPNOTMATCHED=$(echo $ANALYZE | jq .respNotMatched)
	REQNOTMATCHED=$(echo $ANALYZE | jq .reqNotMatched)
  REQMATCHED=$(echo $ANALYZE | jq .reqMatched)
  #Display replay ID
  echo "Replay ID:" $REPLAY_ID
  #Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
  if [ "$REQCOUNT" != "0" ] && [ "$RESPNOTMATCHED" = "0" ] && [ "$REQNOTMATCHED" = "0" ]; then
    TEST_STATUS="test passed"
    EXIT_CODE=0
  else
    TEST_STATUS="test failed"
    EXIT_CODE=1
  fi
}

call_deploy_script() {
	TRACE=1 ./deploy.sh $@
	if [ $? -ne 0 ]; then
		EXIT_CODE=1
		clean
	fi
}

clean() {
	NAMESPACE=$1
	APP_NAME=$2
	kubectl delete all -n $NAMESPACE -l app=$APP_NAME
	kubectl delete virtualservices.networking.istio.io -n $NAMESPACE -l app=$APP_NAME
	kubectl delete envoyfilters.networking.istio.io -n $NAMESPACE -l app=$APP_NAME
	kubectl delete destinationrules.networking.istio.io -n $NAMESPACE -l app=$APP_NAME
	kubectl delete gateways.networking.istio.io -n $NAMESPACE -l app=$APP_NAME
	kubectl delete serviceentries.networking.istio.io -n $NAMESPACE -l app=$APP_NAME
	kubectl delete ns $NAMESPACE
}
main() {
  set -x
  if [[ ! -f /usr/local/bin/jq ]]
  then
	  apk add jq
  fi
	# DRONE_BRANCH="develop"
	# DRONE_COMMIT="411e4ee4dfeb290932122f3ad56141c5b8ec6b15"
	# DRONE_COMMIT_AUTHOR="ethicalaakash"
	# DRONE_BUILD_NUMBER="test108"
  CUBE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
	CONFIG_FILE="temp"
  TEMPLATE=DEFAULT
  USER_ID=CubeCorp
  SPRINGBOOT_HOST=http://$DRONE_COMMIT_AUTHOR-springboot.dev.cubecorp.io
  INSTANCE_ID=$DRONE_COMMIT
  AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
	register_config
	sleep 20
  record
	sleep 20
  generate_traffic 5
  sleep 20
  stop_recording
  sleep 20
  replay
  analyze
	clean $DRONE_COMMIT_AUTHOR-springboot springboot
	clean $DRONE_COMMIT_AUTHOR cube
	echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
  exit $EXIT_CODE
}
main "$@"

