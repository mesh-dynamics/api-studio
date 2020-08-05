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
    curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/getOrderByIndex?index=0"  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ'
    sleep 1
    curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/getOrders/" --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ' --header 'Content-Type: application/json'
    sleep 1
    curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/getOrderByIndexQP?index=2"  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ'
    sleep 1
    curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/getOrderByIndexPV/1"  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ'
    sleep 1
  done

curl --location --request POST "$SPRINGBOOT_HOST:8080/orders/postOrder/" --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ' --header 'Content-Type: application/json' --data-raw '{
	"id": 5,
    "productId": 1,
    "customer": {
        "firstName": "Lokesh1",
        "lastName": "Gupta1",
        "email": "xyz@gmail.com"
    }
}'
sleep 5

curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/getOrders/" --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ' --header 'Content-Type: application/json'
sleep 1

curl --location --request POST "$SPRINGBOOT_HOST:8080/orders/postFormParams/" \
-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ' \
-H 'Content-Type: application/x-www-form-urlencoded' \
-d 'key1=value1&key2=value2'

sleep 5

curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/largePayload/"  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ'
sleep 5

curl --location --request GET "$SPRINGBOOT_HOST:8080/orders/testChunkedResponse/"  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsib3JkZXJzIl0sInVzZXJfbmFtZSI6ImFkbWluQGFkbWluLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE1ODg4MzczMDksImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6ImQyMGEyYWY0LTNmOTYtNDdkMS05ZTM4LTRhMWI4MmE1MjQ1YiIsImNsaWVudF9pZCI6Im9yZGVyLXJlY2VpdmVyIn0.UZIlg5nGhL5QGpHrlupTI8qGFTwIS3jnbnaYNpXeRqQ'
sleep 5

}

stop_recording() {
  curl -X POST $CUBE_ENDPOINT/api/cs/stop/$RECORDING_ID \
  -H "Authorization: Bearer $AUTH_TOKEN"
}

replay() {
  BODY="endPoint=$SPRINGBOOT_HOST&instanceId=$INSTANCE_ID-replay&templateSetVer=$TEMPLATE&userId=$USER_ID"
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

clean() {
	call_deploy_script cube clean $CONFIG_FILE
	call_deploy_script springboot clean $CONFIG_FILE
	kubectl delete ns $DRONE_COMMIT_AUTHOR
	kubectl delete ns $DRONE_COMMIT_AUTHOR-springboot
	echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
  exit $EXIT_CODE
}
main() {
  set -x
  if [[ ! -f /usr/local/bin/jq ]]
  then
	  apk add jq
  fi
  sleep 60
	# DRONE_BRANCH="develop"
	# DRONE_COMMIT="411e4ee4dfeb290932122f3ad56141c5b8ec6b15"
	# DRONE_COMMIT_AUTHOR="ethicalaakash"
	# DRONE_BUILD_NUMBER="test102"
  CUBE_ENDPOINT=https://$DRONE_COMMIT_AUTHOR.dev.cubecorp.io
  TEMPLATE=DEFAULT
  USER_ID=CubeCorp
  SPRINGBOOT_HOST=http://$DRONE_COMMIT_AUTHOR-springboot.dev.cubecorp.io
  INSTANCE_ID=$DRONE_COMMIT
  AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZXNoZHluYW1pY3MuaW8iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoxLCJpYXQiOjE1ODI4ODE2MjgsImV4cCI6MTg5ODI0MTYyOH0.P4DAjXyODV8cFPgObaULjAMPg-7xSbUsVJ8Ohp7xTQI"
  record
  generate_traffic 5
  sleep 20
  stop_recording
  sleep 20
  replay
  analyze
	clean
}
main "$@"

