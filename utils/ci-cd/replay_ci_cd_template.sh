#!/usr/bin/env bash

generate_conf_file() {
cat << EOF > replay.conf
[
  [
    {"prehook": ["echo PreHook"]},
    {
      "goldenname": "<test_suite_name>",
      "paths": ["<api_path_to_be_part_of_replay>"],
    },
    {"posthook": ["echo PostHook"]}
 ]
]
EOF
}

set_variables() {
	#Calculate the length of array
	goldensets=$(cat replay.conf | jq '. | length')
	for ((i=0;i<$goldensets;i++)); do
		goldencount=$(cat replay.conf | jq ".[$i] | length")
		PREHOOK=$(cat replay.conf | jq ".[$i][0].prehook" |  tr -d '[]"')
		POSTHOOK=$(cat replay.conf | jq ".[$i][$((goldencount-1))].posthook" |  tr -d '[]"')
		prehook
		for ((j=1;j<$((goldencount-1));j++)); do
			GOLDEN_NAME=$(cat replay.conf | jq ".[$i][$j].goldenname" | sed -e 's/ /%20/g' | tr -d '"')
			EXCLUDEPATH=$(cat replay.conf | jq ".[$i][$j].excludePath" | tr -d '"')
			PATHS=$(cat replay.conf | jq ".[$i][$j].paths" | tr -d '[]"' | tr "," "\n")
			for path in $PATHS
			do
				TEMP_PATH="$TEMP_PATH""paths=$path&"
			done
			REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
			echo $GOLDEN_NAME
			echo $EXCLUDEPATH
			echo $REPLAY_PATHS
			replay
			sleep 20
			unset TEMP_PATH
		done
		posthook
	done
}

prehook() {
	$PREHOOK
}

posthook() {
	$POSTHOOK
}

replay() {
	# This is to pass customer headers to replay api requests ex: sending basic auth headers
	TRANSFORMS={}
	# This is send the context progation rules to be picked
	DICONFIGNAME=

	BODY="$REPLAY_PATHS&endPoint=$REPLAY_ENDPOINT&analyze=true&instanceId=$INSTANCE_ID&templateSetVer=$TEMPLATE&userId=$USER_ID&service=$SERVICE&excludePaths=$EXCLUDEPATH&transforms=$TRANSFORMS&dynamicInjectionConfigVersion=$DICONFIGNAME"
	echo $BODY
	resp=$(curl -sw "%{http_code}" -X POST \
		$CUBE_ENDPOINT/api/rs/start/byGoldenName/$CUSTOMERID/$APP/$GOLDEN_NAME \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H "Authorization: Bearer $AUTH_TOKEN" \
		-H 'cache-control: no-cache' \
		-d $BODY)
	http_code="${resp:${#res}-3}"
	if [ $http_code -ne 200 ]; then
		echo "Error"
		exit 1
	fi
	body="${resp:0:${#resp}-3}"
	REPLAY_ID=$(echo $body | jq -r ".replayId" | sed -e 's/ /%20/g')

	echo "REPLAYID:" $REPLAY_ID
	#Status Check
	COUNT=0
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ]; do
		STATUS=$(curl -f -X GET $CUBE_ENDPOINT/api/rs/status/$REPLAY_ID -H "Authorization: Bearer $AUTH_TOKEN" | jq -r '.status')
		sleep 10
		COUNT=$((COUNT+1))
	done
	unset STATUS
	analyze
}

analyze() {
	COUNT=0
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ]; do
		STATUS=$(curl -X GET $CUBE_ENDPOINT/api/as/status/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache' | jq '.data.status' | tr -d '"')
		sleep 10
		COUNT=$((COUNT+1))
	done
	unset STATUS
	ANALYZE=$(curl -X GET $CUBE_ENDPOINT/api/as/status/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache' | jq '.data')
	REQNOTMATCHED=$(echo $ANALYZE | jq .reqNotMatched )
        result=$(curl -X GET $CUBE_ENDPOINT/api/as/analysisResByPath/$REPLAY_ID?start=0\&numResults=0\&includeDiff=true\&service=DjangoApp\&reqMatchType=ExactMatch -H "Authorization: Bearer $AUTH_TOKEN" | jq '.data.facets.diffResFacets')
        TOTALERROR=0
        length=$(echo $result | jq '. | length')
        for ((i=0;i<length;i++)); do
                val=$(echo $result | jq -r ".[$i][\"val\"]")
                #if [[ $val == "ERR_ValMismatch" || $val == "ERR_Required" || $val == "ERR_RequiredGolden" || $val == "ERR" ]]; then
                if [[ $val =~ "ERR" ]]; then
                        error=$(echo $result | jq ".[$i][\"count\"]")
                        TOTALERROR=$(($TOTALERROR + $error))
                fi

		if [[ $val == "ERR_ValMismatch" ]]; then
			VAL_MISMATCH=$error
			echo VAL_MISMATCH=$VAL_MISMATCH
		elif [[ $val == "ERR_Required" ]]; then
			REQUIRED=$error
			echo REQUIRED=$REQUIRED
		elif [[ $val == "ERR_RequiredGolden" ]]; then
			REQ_GOLDEN=$error
			echo REQ_GOLDEN=$REQ_GOLDEN
		elif [[ $val == "ERR" ]]; then
			ERR=$error
			echo ERR=$ERR
		fi

        done
        echo $TOTALERROR
	if [[ "$REQNOTMATCHED" == "0" && "$VAL_MISMATCH" -lt 216  && "$REQUIRED" -lt 174 && "$REQ_GOLDEN" -lt 85 && "$ERR" -lt 3 ]]; then
    		TEST_STATUS="test passed"
    		EXIT_CODE=0
  	else
    		TEST_STATUS="test failed"
    		EXIT_CODE=1
  	fi
	echo "<br> Golden Name: $GOLDEN_NAME" >> body.txt
	echo "Test status: $TEST_STATUS" >> body.txt
	echo "Link to test result: $CUBE_ENDPOINT/diff_results?replayId=$REPLAY_ID" >> body.txt
	echo "Test summary:" >> body.txt
	echo $ANALYZE | jq >> body.txt
}

mail() {
  python - << EOF
import os
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'sendgrid'])
from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import Mail

with open('body.txt', 'r') as file:
    data = file.read().replace('\n', '<br>')

message = Mail(
    from_email='test@test.io',
    to_emails=['test@test.io', 'test2@test2.io'],
    subject='Test results',
    html_content=data)
try:
    sg = SendGridAPIClient(os.environ.get('SENDGRID_API_KEY'))
    response = sg.send(message)
    print(response.status_code)
    print(response.body)
    print(response.headers)
except Exception as e:
    print(e.message)
EOF
}

mail_the_report() {
  echo "replayid and token $REPLAY_ID  $AUTH_TOKEN"
  result=$(curl -X GET $CUBE_ENDPOINT/api/sendTestReport/$REPLAY_ID?emails=test@test.io,test2@test.io -H "Authorization: Bearer $AUTH_TOKEN")
  echo "mail send result:"$result
}

main() {
	set -xe
	generate_conf_file
	# This is endpoint of application against which the test has to be run
	REPLAY_ENDPOINT=https://applicationendpointip:port
	# This is the endpoint of the meshd server
	CUBE_ENDPOINT=https://meshendpoint:port
	
	CUSTOMERID=customer_name
	APP=application_name
	INSTANCE_ID=test_env_name
	USER_ID=test@test.io
	TEMPLATE=DEFAULT
	AUTH_TOKEN="api_auth_token_to_call_meshd_apis"
	SERVICE=DjangoApp
	export SENDGRID_API_KEY='send_grid_account_api_key_to_send_email'
	set_variables
	#mail
	mail_the_report
	rm body.txt
	echo $TEST_STATUS
	exit $EXIT_CODE
}

main "$@"

