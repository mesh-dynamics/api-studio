#!/usr/bin/env bash

record() {
  RESPONSE=$(curl -X POST \
  $CUBE_ENDPOINT/api/cs/start/CubeCorp/CourseApp/$INSTANCE_ID/default \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H 'cache-control: no-cache' \
  -d "name=course-$DRONE_BUILD_NUMBER&userId=CubeCorp&label=$(date +%s)")
  echo $RESPONSE
  RECORDING_ID=$(echo $RESPONSE | jq -r ".id")
  echo $RECORDING_ID
  echo $RECORDING_ID
}

generate_traffic() {
  for ((i=1;i<=$1;i++)); do
    curl -f --location --request GET 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses/1/students/1' \
    --header 'CUSTOM: ASD'
  curl --location --request GET 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses/1'
  curl --location --request PUT 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses/1' \
	--header 'Content-Type: application/json' \
	--data-raw '{
    	"id": 1,
    	"name": "REST with Spring2",
    	"students": [
        	1,
        	2,
        	2
    	]
	}'
  curl --location --request GET 'http://apachecxf.dev.cubecorp.io:8080/meshd/dummyCourseList?count=2'
  TIMESTAMP=$(date +%s)
  curl --location --request POST 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses/1/students' \
	--header 'Content-Type: application/json' \
	--data-raw '{
    		"id": "'$TIMESTAMP'",
    		"name": "Student C"
	}'

  curl --location --request POST 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses' \
	--header 'Content-Type: application/x-www-form-urlencoded' \
	--data-urlencode 'name=testcourse'
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
  REQMATCHED=$(echo $ANALYZE | jq .reqMatched)
  RESPNOTMATCHED=$(echo $ANALYZE | jq .respNotMatched )

  #Display replay ID
  echo "Replay ID:" $REPLAY_ID
  #Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
  if [ "$RESPNOTMATCHED" -le 2 ] && [ "$REQCOUNT" != "0" ] && [ "$REQMATCHED" = "$REQCOUNT" ]; then
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
  CUBE_ENDPOINT=https://demo.dev.cubecorp.io
  TEMPLATE=DEFAULT
  USER_ID=CubeCorp
  REPLAY_ENDPOINT=http://apachecxf.dev.cubecorp.io
  INSTANCE_ID=$DRONE_COMMIT
  AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
  record
  sleep 20
  generate_traffic 5
  stop_recording
  sleep 40
  replay
  sleep 30
  analyze
  echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
  exit $EXIT_CODE
}
main "$@"
