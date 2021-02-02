#!/usr/bin/env bash

generate_conf_file() {
cat << EOF > replay.conf
[
  [
    {"prehook": ["echo PreHook"]},
    {
      "goldenname": "Pronto-test-2",
      "paths": ["api/v1/relationship-logo/"],
      "excludePath": true
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

	sessionId=$(curl -i 'https://mesh.gopronto.io/login/'   -H 'authority: mesh.gopronto.io'   -H 'cache-control: max-age=0'   -H 'upgrade-insecure-requests: 1'   -H 'origin: https://mesh.gopronto.io'   -H 'content-type: application/x-www-form-urlencoded'   -H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36'   -H 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9'   -H 'sec-fetch-site: same-origin'   -H 'sec-fetch-mode: navigate'   -H 'sec-fetch-user: ?1'   -H 'sec-fetch-dest: document'   -H 'referer: https://mesh.gopronto.io/'   -H 'accept-language: en-GB,en-US;q=0.9,en;q=0.8'   -H 'cookie: _ga=GA1.2.1699158814.1594282238; ajs_group_id=null; ajs_anonymous_id=%223a7c5358-a068-4b66-bf57-8afd193dfc56%22; __cfduid=d35ac9093766e09a4ba9c4f9578847b1a1599734769; csrftoken=zKQiB6MplbadMtxA6uClN24hce632LqBvouLYB1efMtgHfn6sW3gOhVEdZoUH0VZ; _gid=GA1.2.461535925.1599734895'   --data-raw 'csrfmiddlewaretoken=XLXUsQqkMIc3l4ndSl2w4ZsGDuEXklV3TpBnPlF9Gjv6gQdJeNtr5ej3EfWOZAqr&email=manoj%40gopronto.io&password=QXViZXJnaW5lJDEyMwo='   --compressed -v 2>/dev/null | grep "set-cookie: sessionid" | head -1 | cut -d"=" -f2 | cut -d";" -f1)


	csrftoken=$(curl -i 'https://mesh.gopronto.io/login/'   -H 'authority: mesh.gopronto.io'   -H 'cache-control: max-age=0'   -H 'upgrade-insecure-requests: 1'   -H 'origin: https://mesh.gopronto.io'   -H 'content-type: application/x-www-form-urlencoded'   -H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36'   -H 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9'   -H 'sec-fetch-site: same-origin'   -H 'sec-fetch-mode: navigate'   -H 'sec-fetch-user: ?1'   -H 'sec-fetch-dest: document'   -H 'referer: https://mesh.gopronto.io/'   -H 'accept-language: en-GB,en-US;q=0.9,en;q=0.8'   -H 'cookie: _ga=GA1.2.1699158814.1594282238; ajs_group_id=null; ajs_anonymous_id=%223a7c5358-a068-4b66-bf57-8afd193dfc56%22; __cfduid=d35ac9093766e09a4ba9c4f9578847b1a1599734769; csrftoken=zKQiB6MplbadMtxA6uClN24hce632LqBvouLYB1efMtgHfn6sW3gOhVEdZoUH0VZ; _gid=GA1.2.461535925.1599734895'   --data-raw 'csrfmiddlewaretoken=XLXUsQqkMIc3l4ndSl2w4ZsGDuEXklV3TpBnPlF9Gjv6gQdJeNtr5ej3EfWOZAqr&email=manoj%40gopronto.io&password=Mesh%24123'   --compressed -v 2>/dev/null | grep "set-cookie: csrftoken" | head -1 | cut -d"=" -f2 | cut -d";" -f1)

	TRANSFORMS={\"requestTransforms\":{\"cookie\":[{\"source\":\"*\",\"target\":\"sessionid=$sessionId\;csrftoken=$csrftoken\"}],\"x-csrftoken\":[{\"source\":\"*\",\"target\":\"$csrftoken\"}]}}
	DICONFIGNAME=ProntoDynamicInject

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
    from_email='mail@meshdynamics.io',
    to_emails=['muthumani.nambi@meshdynamics.io', 'kirtan@auberginesolutions.com'],
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
	generate_conf_file
	REPLAY_ENDPOINT=https://mesh.gopronto.io
	CUBE_ENDPOINT=https://pronto.meshdynamics.io
	CUSTOMERID=Pronto
	APP=ProntoApp
	INSTANCE_ID=test
	USER_ID=manoj@gopronto.io
	TEMPLATE=DEFAULT
	AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBnb3Byb250by5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjc5NiwiaWF0IjoxNTk0MTk2MzIxLCJleHAiOjE5MDk1NTYzMjF9.jQuntH--B-owUPnNW_rSwwtaXCQtrHRh4wA4xpAaTDw"
	SERVICE=DjangoApp
	export SENDGRID_API_KEY='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'
	set_variables
	#mail
	rm body.txt
	echo $TEST_STATUS
	exit $EXIT_CODE
}

main "$@"

