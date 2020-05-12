#!/usr/bin/env bash

set_variables() {
	#Calculate the length of array
	goldensets=$(cat medallia.conf | jq '. | length')
	for ((i=0;i<$goldensets;i++)); do
		goldencount=$(cat medallia.conf | jq ".[$i] | length")
		PREHOOK=$(cat medallia.conf | jq ".[$i][0].prehook" |  tr -d '[]"')
		POSTHOOK=$(cat medallia.conf | jq ".[$i][$((goldencount-1))].posthook" |  tr -d '[]"')
		prehook
		for ((j=1;j<$((goldencount-1));j++)); do
			GOLDEN_NAME=$(cat medallia.conf | jq ".[$i][$j].goldenname" | tr -d '"')
			EXCLUDEPATH=$(cat medallia.conf | jq ".[$i][$j].excludePath" | tr -d '"')
			PATHS=$(cat medallia.conf | jq ".[$i][$j].paths" | tr -d '[]"' | tr "," "\n")
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
	BODY="$REPLAY_PATHS&endPoint=$REPLAY_ENDPOINT&analyze=true&instanceId=$INSTANCE_ID&templateSetVer=$TEMPLATE&userId=$USER_ID&excludePaths=$EXCLUDEPATH"
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
	echo "<br> Golden Name: $GOLDEN_NAME" >> body.txt
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
    from_email='mail@meshdynamics.io',
    to_emails='aakash.singhal@meshdynamics.io',
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

main() {
	set -xe
	REPLAY_ENDPOINT=https://demo.dev.cubecorp.io
	CUBE_ENDPOINT=https://demo.dev.cubecorp.io
	CUSTOMERID=CubeCorp
	APP=MovieInfo
	INSTANCE_ID=prod
	USER_ID=demo@cubecorp.io
	TEMPLATE=RespPartialMatch
	AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZXNoZHluYW1pY3MuaW8iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoxLCJpYXQiOjE1ODI4ODE2MjgsImV4cCI6MTg5ODI0MTYyOH0.P4DAjXyODV8cFPgObaULjAMPg-7xSbUsVJ8Ohp7xTQI"
	export SENDGRID_API_KEY='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'
	set_variables
	mail
	rm body.txt
}

main "$@"
