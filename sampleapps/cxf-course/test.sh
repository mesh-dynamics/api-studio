#!/usr/bin/env bash


register_config() {
#Recording configuration
  curl --location --request POST 'https://demo.dev.cubecorp.io/api/cs/storeAgentConfig' \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $AUTH_TOKEN" \
--data-raw '{
    "customerId":"CubeCorp",
    "app":"CourseApp",
    "instanceId" : "test",
    "service":"course1",
    "tag" : "CICDRecordCXF",
    "configJson" :
    {
        "type" : "AgentConfig",
        "config": "{\"io\": {\"md\": {\"service\": {\"record\": \"https://demo.dev.cubecorp.io/api\",\"mock\": \"https://demo.dev.cubecorp.io/api\"},\"authtoken\": \"Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s\", \"read\": {\"timeout\": 100000},\"connect\": {\"timeout\": 100000,\"retries\": 3},\"intent\": \"record\",\"samplerconfig\": {\"type\": \"simple\",\"accuracy\": 1000,\"rate\": 1},\"sampler\": {\"veto\": false},\"nodeselectionconfig\": {\"type\": \"adaptive\",\"accuracy\": 1000,\"fieldCategory\": \"customerAttributes\",\"attributes\": [{\"field\": \"io.md.serviceinstance\",\"value\": \"1\",\"rate\": 0},{\"field\": \"io.md.serviceinstance\",\"value\":\"2\" ,\"rate\": 1},{\"field\": \"io.md.serviceinstance\",\"value\": \"3\",\"rate\": 0},{\"field\": \"io.md.serviceinstance\",\"value\":\"4\" ,\"rate\": 1},{\"field\": \"io.md.serviceinstance\",\"value\": \"other\",\"rate\": 1}]}}}}"
    }
}'
#Replay configuration
curl --location --request POST 'https://demo.dev.cubecorp.io/api/cs/storeAgentConfig' \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $AUTH_TOKEN" \
--data-raw '{
    "customerId":"CubeCorp",
    "app":"CourseApp",
    "instanceId" : "test",
    "service":"course1",
    "tag" : "CICDReplayCXF",
    "configJson" :
    {
        "type" : "AgentConfig",
        "config": "{\"io\": {\"md\": {\"service\": {\"record\": \"https://demo.dev.cubecorp.io/api\",\"mock\": \"https://demo.dev.cubecorp.io/api\"},\"read\": {\"timeout\": 100000},\"connect\": {\"timeout\": 100000,\"retries\": 3},\"intent\": \"mock\",\"samplerconfig\": {\"type\": \"simple\",\"accuracy\": 1000,\"rate\": 1},\"sampler\": {\"veto\": false},\"performance\": {\"test\": false},\"nodeselectionconfig\": {\"type\": \"adaptive\",\"accuracy\": 1000,\"fieldCategory\": \"customerAttributes\",\"attributes\": [{\"field\": \"ipAddress\",\"value\": \"other\",\"rate\": 1}]},\"mock\": {\"services\": [\"student:8080\"]}}}}"
    }
}'

}

check_sampling_status() {
var=$(curl --location --request GET 'https://demo.dev.cubecorp.io/api/cs/getAgentSamplingFacets/CubeCorp/CourseApp/course1/test' --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s' | jq .Results)
for ((i=0;i<4;i++)); do
  serviceinstance=$(echo $var | jq -r ".[$i].acknowledgeInfo.\"io.md.serviceinstance\"")
  nodeselected=$(echo $var | jq -r ".[$i].acknowledgeInfo.isNodeSelected")
  if [ "$serviceinstance" = "1" ] || [ "$serviceinstance" = "3" ] && [ "$nodeselected" = "false" ]; then
    echo "Configuration set"
  elif [ "$serviceinstance" = "2" ] && [ "$serviceinstance" = "4" ] || [ "$nodeselected" = "true" ]; then
    echo "Configuration set"
  else
    echo "configuration not Set"
    exit 1
  fi
done
}


record() {
  curl --location --request POST 'https://demo.dev.cubecorp.io/api/cs/setCurrentAgentConfigTag/' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s' \
--data-raw '{
    "customerId":"CubeCorp",
    "app":"CourseApp",
    "instanceId" : "test",
    "service":"course1",
    "tag" : "CICDRecordCXF"
}'

  sleep 35
  check_sampling_status
  RESPONSE=$(curl -X POST \
  $CUBE_ENDPOINT/api/cs/start/CubeCorp/CourseApp/$INSTANCE_ID/default \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H 'cache-control: no-cache' \
  -d "name=course-$DRONE_BUILD_NUMBER&userId=CubeCorp&label=$(date +%s)")
  echo $RESPONSE
  RECORDING_ID=$(echo $RESPONSE | jq -r ".id")
  echo $RECORDING_ID
}

generate_traffic() {
  for ((i=1;i<=$1;i++)); do
    curl --location --request GET 'http://apachecxf.dev.cubecorp.io/meshd/dummyCourseList?count=4&changeNameCount=2'
    curl --location --request GET 'http://apachecxf.dev.cubecorp.io/meshd/dummyStudentList?count=6'
    TIMESTAMP=$(date +%s)
    curl --location --request POST 'http://apachecxf.dev.cubecorp.io/meshd/createStudentNew' \
    --header 'custom_header: trial' \
    --header 'Content-Type: application/json' \
    --header 'Content-Type: text/plain' \
    --data-raw '{
      "id" : "'$TIMESTAMP'",
      "name" : "sample"
    }'
done
}

stop_recording() {
  curl -X POST https://demo.dev.cubecorp.io/api/cs/stop/$RECORDING_ID \
  -H "Authorization: Bearer $AUTH_TOKEN"
}
replay() {
  curl --location --request POST 'https://demo.dev.cubecorp.io/api/cs/setCurrentAgentConfigTag/' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s' \
--data-raw '{
    "customerId":"CubeCorp",
    "app":"CourseApp",
    "instanceId" : "test",
    "service":"course1",
    "tag" : "CICDReplayCXF"
}'
sleep 35
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
  if [ "$REQCOUNT" != "0" ] && [ "$REQMATCHED" = "$REQCOUNT" ]; then
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
  INSTANCE_ID=test
  AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s"
  register_config
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
