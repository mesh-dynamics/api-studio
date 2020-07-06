#!/usr/bin/env bash

record() {
  RESPONSE=$(curl -X POST \
  $CUBE_ENDPOINT/api/cs/start/CubeCorp/springboot_demo/$INSTANCE_ID/default \
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
    curl --location --request GET 'http://springboot.dev.cubecorp.io:8080/orders/getOrderByIndex?index=0'  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ'
    sleep 1
    curl --location --request GET 'http://springboot.dev.cubecorp.io:8080/orders/getOrders/' --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ' --header 'Content-Type: application/json'
    sleep 1
  done
}

stop_recording() {
  curl -X POST https://demo.dev.cubecorp.io/api/cs/stop/$RECORDING_ID \
  -H "Authorization: Bearer $AUTH_TOKEN"
}

replay() {
  BODY="endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&templateSetVer=$TEMPLATE&userId=$USER_ID"
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
      exit 1
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
  ANALYZE=$(curl -X POST $CUBE_ENDPOINT/api/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache')
  REQCOUNT=$(echo $ANALYZE | jq .reqCnt )
  RESPNOTMATCHED=$(echo $ANALYZE | jq .respNotMatched )
  REQMATCHED=$(echo $ANALYZE | jq .reqMatched)
  #Display replay ID
  echo "Replay ID:" $REPLAY_ID
  #Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
  if [ "$RESPNOTMATCHED" = "0" ] && [ "$REQCOUNT" != "0" ] && [ "$REQMATCHED" = "$REQCOUNT" ]; then
    TEST_STATUS="test passed"
    EXIT_CODE=0
  else
    TEST_STATUS="test failed"
    EXIT_CODE=1
  fi
}
main() {
  set -x
  if [[ ! -f /usr/local/bin/jq ]]
  then
	  apk add jq
  fi
  sleep 60
  CUBE_ENDPOINT=https://demo.dev.cubecorp.io
  TEMPLATE=DEFAULT
  USER_ID=CubeCorp
  REPLAY_ENDPOINT=http://springboot.dev.cubecorp.io
  INSTANCE_ID=prod
  AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
  record
  generate_traffic 5
  sleep 20
  stop_recording
  sleep 20
  replay
  analyze
  echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
  exit $EXIT_CODE
}
main "$@"
