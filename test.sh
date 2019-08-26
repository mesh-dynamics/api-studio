#!/usr/bin/env bash

set -x
#Init Replay
REPLAY_ID=$(curl -X POST \
	http://demo.dev.cubecorp.io/rs/init/CubeCorp/Cube/df-14-aug-12-0f38fd97-4bb3-472d-8bc8-3afd7bccf08d \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'cache-control: no-cache' \
	-d 'endpoint=http://staging.dev.cubecorp.io&instanceid=test&templateSetVer=c86d21a5-82fb-4a4d-bcc8-411177dc2a23'  | sed 's/^.*"replayid":"\([^"]*\)".*/\1/')

#Start replay
curl -f -X POST \
  http://demo.dev.cubecorp.io/rs/start/CubeCorp/Cube/df-10-aug-7/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'

#Status Check
COUNT=0
while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "20" ]; do
	STATUS=$(curl -X GET http://demo.dev.cubecorp.io/rs/status/CubeCorp/Cube/df-10-aug-7/$REPLAY_ID | sed 's/^.*"status":"\([^"]*\)".*/\1/')
	sleep 5
	COUNT=$((COUNT+1))
done

#Run analyze
ANALYZE=$(curl -X POST http://demo.dev.cubecorp.io/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache')
REQNOTMATCHED=$(echo $ANALYZE | sed 's/^.*"reqnotmatched":\([^"]*\).*/\1/' | cut -d ',' -f 1)
RESPNOTMATCHED=$(echo $ANALYZE | sed 's/^.*"respnotmatched":\([^"]*\).*/\1/' | cut -d ',' -f 1)

#Display replay ID
echo "Replay ID:" $REPLAY_ID
#Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
if [ "$REQNOTMATCHED" = "0" ] && [ "$RESPNOTMATCHED" = "0" ]; then
	echo "test passed"
	exit 0
else
	echo "test failed"
	exit 1
fi
