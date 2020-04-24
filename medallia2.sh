#!/usr/bin/env bash

set_variables() {
	#Calculate the length of array
	length=$(cat medallia.conf | jq '. | length')
	for ((i=0;i<$length;i++)); do
		RECORDING_ID=$(cat medallia.conf | jq ".[$i].recordingid")
		EXCLUDEPATH=$(cat medallia.conf | jq ".[$i].excludePath")
		RECORDING_ID=$(echo $RECORDING_ID | tr -d '"')
		PATHS=$(cat medallia.conf | jq ".[$i].paths")
		PATHS=$(echo $PATHS | tr -d '[]"' | tr "," "\n")
		for path in $PATHS
		do
		  TEMP_PATH="$TEMP_PATH""paths=$path&"
		done
		REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
		echo $RECORDING_ID
		echo $EXCLUDEPATH
		echo $REPLAY_PATHS
		replay
		unset TEMP_PATH
	done
}

replay() {
	BODY="$REPLAY_PATHS&endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&templateSetVer=$TEMPLATE&userId=$USER_ID&excludePaths=$EXCLUDEPATH"
	echo $BODY
	REPLAY_ID=$(curl -X POST \
		$CUBE_ENDPOINT/api/rs/start/$RECORDING_ID \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H "Authorization: Bearer $AUTH_TOKEN" \
		-H 'cache-control: no-cache' \
		-d $BODY \
	| jq -r ".replayId" | sed -e 's/ /%20/g')

	echo "REPLAYID:" $REPLAY_ID
	#Status Check
	COUNT=0
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ]; do
		STATUS=$(curl -f -X GET $CUBE_ENDPOINT/api/rs/status/$REPLAY_ID -H "Authorization: Bearer $AUTH_TOKEN" | jq -r '.status')
		sleep 10
		COUNT=$((COUNT+1))
	done
	analyze
}

analyze() {
	ANALYZE=$(curl -X POST $CUBE_ENDPOINT/api/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache')
	echo "<br> Recording ID: $RECORDING_ID" >> body.txt
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
	set -x
	REPLAY_ENDPOINT=http://moviebook-test.prod.meshdynamics.io
	CUBE_ENDPOINT=https://app.meshdynamics.io
	INSTANCE_ID=test
	USER_ID=demo@cubecorp.io
	TEMPLATE=RespPartialMatch
	AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTU4NzcyNTA4NywiZXhwIjoxNTg4MzI5ODg3fQ.y-IxiKlmUxGJNF8UdNQmZFYvmDUxyArt1faJ85fVuhQ"
	export SENDGRID_API_KEY='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'
	set_variables
	mail
	rm body.txt
}

main "$@"
