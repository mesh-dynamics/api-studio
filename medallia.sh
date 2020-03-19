#!/usr/bin/env bash
replay() {
	BODY="endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&templateSetVer=$TEMPLATE&userId=$USER_ID"

	REPLAY_ID=$(curl -X POST \
		$CUBE_ENDPOINT/api/rs/start/$RECORDING_ID \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-H "Authorization: Bearer $AUTH_TOKEN" \
		-H 'cache-control: no-cache' \
		-d $BODY \
	| jq -r ".replayId")

	echo "REPLAYID:" $REPLAY_ID

	#Status Check
	COUNT=0
	while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ]; do
		STATUS=$(curl -f -X GET $CUBE_ENDPOINT/api/rs/status/$REPLAY_ID -H "Authorization: Bearer $AUTH_TOKEN" | jq -r '.status')
		sleep 20
		COUNT=$((COUNT+1))
	done
}

analyze() {
	ANALYZE=$(curl -X POST $CUBE_ENDPOINT/api/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H "Authorization: Bearer $AUTH_TOKEN" -H 'cache-control: no-cache')
	REQNOTMATCHED=$(echo $RESP | jq -r ".reqNotMatched")
	RESPNOTMATCHED=$(echo $RESP | jq -r ".respNotMatched")

	#Display replay ID
	echo "Replay ID:" $REPLAY_ID
	RESP="Link to the results: $CUBE_ENDPOINT/shareable_link?replayId=$REPLAY_ID"
	#Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
	if [ "$RESPNOTMATCHED" = "0" ]; then
		TEST_STATUS="test passed"
		BODY=$(echo "{\"personalizations\": [{\"to\": [{\"email\": \"aakash.singhal@meshdynamics.io\"}]}],\"from\": {\"email\": \"mail@aakashsinghal.io\"},\"subject\": \"Test Passed\",\"content\": [{\"type\": \"text\/plain\", \"value\": \"$RESP\"}]}")
	  curl --request POST \
	    --url https://api.sendgrid.com/v3/mail/send \
	    --header "Authorization: Bearer $SENDGRID_API_KEY" \
	    --header 'Content-Type: application/json' \
	    --data "$BODY"
		EXIT_CODE=0
	else
		TEST_STATUS="test failed"
		BODY=$(echo "{\"personalizations\": [{\"to\": [{\"email\": \"$SENTTOEMAIL\"}]}],\"from\": {\"email\": \"mail@meshdynamics.io\"},\"subject\": \"Test Failed\",\"content\": [{\"type\": \"text\/plain\", \"value\": \"$RESP\"}]}")
	  curl --request POST \
	    --url https://api.sendgrid.com/v3/mail/send \
	    --header "Authorization: Bearer $SENDGRID_API_KEY" \
	    --header 'Content-Type: application/json' \
	    --data "$BODY"
		EXIT_CODE=1
	fi
}

main() {
	set -ex
	RECORDING_ID=Recording--1680696020
	if [ -z "$RECORDING_ID" ]; then
		echo "Enter Recording Id"
		read RECORDING_ID
	fi
	REPLAY_ENDPOINT=https://sdlitedev2-api.random.com
	CUBE_ENDPOINT=https://medallia.meshdynamics.io
	INSTANCE_ID=test-md
	USER_ID=MedalliaUser
	TEMPLATE=DEFAULT
	SENTTOEMAIL=aakash.singahl@meshdynamics.io
	SENDGRID_API_KEY='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'
	AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZWRhbGxpYS5jb20iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoyLCJpYXQiOjE1ODMwNTE4NDEsImV4cCI6MTg5ODQxMTg0MX0.9hOCHHkf1N6wxyG7w-MJ8M22yQEy8qiGzuCBSjTYY9o"
	replay
	analyze
	echo $TEST_STATUS
	exit $EXIT_CODE
}

main "$@"