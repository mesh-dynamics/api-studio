#!/usr/bin/env bash

set_variables() {
	#Calculate the length of array
	length=$(cat /home/ec2-user/meshdynamics/medallia.conf | jq '. | length')
	for ((i=0;i<$length;i++)); do
		RECORDING_ID=$(cat /home/ec2-user/meshdynamics/medallia.conf | jq ".[$i].recordingid")
		EXCLUDEPATH=$(cat /home/ec2-user/meshdynamics/medallia.conf | jq ".[$i].excludePath")
		RECORDING_ID=$(echo $RECORDING_ID | tr -d '"')
		PATHS=$(cat /home/ec2-user/meshdynamics/medallia.conf | jq ".[$i].paths")
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
	BODY="$REPLAY_PATHS&endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&userId=$USER_ID&excludePaths=$EXCLUDEPATH"
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
	STATUS="Running"
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ]; do
		STATUS=$(curl -f -X GET $CUBE_ENDPOINT/api/rs/status/$REPLAY_ID -H "Authorization: Bearer $AUTH_TOKEN" | jq -r '.status')
		sleep 20
		COUNT=$((COUNT+1))
	done
	analyze
}

analyze() {
	#curl -X POST $CUBE_ENDPOINT/api/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache'
	#echo "$CUBE_ENDPOINT/shareable_link?replayId=$REPLAY_ID" >> body.txt
	ANALYZE=$(curl -X POST $CUBE_ENDPOINT/api/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache')
	echo "<br> Recording ID: $RECORDING_ID" >> /home/ec2-user/meshdynamics/body.txt
	echo "Link to test result: $CUBE_ENDPOINT/diff_results?replayId=$REPLAY_ID" >> /home/ec2-user/meshdynamics/body.txt
	echo "Test summary:" >> /home/ec2-user/meshdynamics/body.txt
	echo $ANALYZE | jq >> /home/ec2-user/meshdynamics/body.txt
}

mail() {
	python - << EOF
import os
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'sendgrid'])
from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import Mail

with open('/home/ec2-user/meshdynamics/body.txt', 'r') as file:
    data = file.read().replace('\n', '<br>')

data = "Links to test results: <br>" + data
message = Mail(
    from_email='mail@meshdynamics.io',
    to_emails=['gmurthy@medallia.com','muthumani.nambi@meshdynamics.io','venky.ganti@meshdynamics.io','rahul.lahiri@meshdynamics.io','prasad.deshpande@meshdynamics.io'],
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
	REPLAY_ENDPOINT=https://sdlitedev2-api.strikedeck.com
	CUBE_ENDPOINT=https://medallia.meshdynamics.io
	INSTANCE_ID=record
	USER_ID=gmurthy@medallia.com
	TEMPLATE=RespPartialMatch
	AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZWRhbGxpYS5jb20iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoyLCJpYXQiOjE1ODMwNTE4NDEsImV4cCI6MTg5ODQxMTg0MX0.9hOCHHkf1N6wxyG7w-MJ8M22yQEy8qiGzuCBSjTYY9o"
	export SENDGRID_API_KEY='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'
	set_variables
	mail
	rm /home/ec2-user/meshdynamics/body.txt
}

main "$@"
