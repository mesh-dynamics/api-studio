#!/usr/bin/env bash

record() {
  RECORDING_ID=$(curl -X POST $CUBE_ENDPOINT/api/cs/start/CubeCorp/CourseApp/prod/default \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H 'cache-control: no-cache' \
  -d "name=course-$DRONE_BUILD_NUMBER&userId=CubeCorp&label=$(date +%s)" | jq .id | tr -d '"')
  echo $RECORDING_ID
}

generate_traffic() {
  #for ((i=1;i<=$1;i++)); do
    curl -f --location --request GET 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses/1/students/1' \
    --header 'CUSTOM: ASD'
  #done
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
  curl --location --request POST 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses/1/students' \
	--header 'Content-Type: application/json' \
	--data-raw '{
    		"id": 3,
    		"name": "Student C"
	}'
  curl --location --request DELETE 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses/1/students/3' \
	--header 'Content-Type: application/json' \
	--data-raw '{
    		"id": 3,
    	"name": "Student C"
	}'
  curl --location --request POST 'http://apachecxf.dev.cubecorp.io:8080/meshd/courses' \
	--header 'Content-Type: application/x-www-form-urlencoded' \
	--data-urlencode 'name=testcourse'
}

stop_recording() {
  curl -X POST https://demo.dev.cubecorp.io/api/cs/stop/$RECORDING_ID \
  -H "Authorization: Bearer $AUTH_TOKEN"
}
replay() {
  BODY="endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&templateSetVer=$TEMPLATE&userId=$USER_ID"
  REPLAY_ID=$(curl -X POST \
		$CUBE_ENDPOINT/api/rs/start/$RECORDING_ID \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H "Authorization: Bearer $AUTH_TOKEN" \
		-H 'cache-control: no-cache' \
		-d $BODY | jq .replayId | tr -d '"')
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

  #Display replay ID
  echo "Replay ID:" $REPLAY_ID
  #Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
  if [ "$RESPNOTMATCHED" = "0" ] && [ "$REQCOUNT" != "0" ]; then
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
  INSTANCE_ID=prod
  AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
  record
  sleep 20
  generate_traffic 5
  sleep 20
  stop_recording
  sleep 20
  replay
  sleep 30
  analyze
  echo "Replay ID:" $REPLAY_ID
	echo $TEST_STATUS
  exit $EXIT_CODE
}
main "$@"
