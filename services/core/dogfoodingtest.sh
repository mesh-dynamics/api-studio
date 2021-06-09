#!/usr/bin/env bash

set -x
#Init Replay

REPLAY_PATHS=cs/start/*,cs/stop/*,cs/rrbatch/*,cs/currentcollection,rs/init/*,rs/start/*,ms/CubeCorp/*
REPLAY_PATHS=$(echo $REPLAY_PATHS | tr "," "\n")
for path in $REPLAY_PATHS
do
  TEMP_PATH="$TEMP_PATH""paths=$path&"
done
REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
BODY="$REPLAY_PATHS&endpoint=http://staging.dev.cubecorp.io&instanceId=test&templateSetVer=DEFAULT&userId=CubeCorp"

REPLAY_ID=$(curl -X POST \
	http://demo.dev.cubecorp.io/rs/start/Recording-1398381203 \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'cache-control: no-cache' \
	-d $BODY \
| sed 's/^.*"replayid":"\([^"]*\)".*/\1/')

#Start replay
#curl -f -X POST \
#  http://demo.dev.cubecorp.io/rs/start/CubeCorp/Cube/fluentd-test-df-49/$REPLAY_ID \
#  -H 'Content-Type: application/x-www-form-urlencoded' \
#  -H 'cache-control: no-cache'

#Status Check
COUNT=0
while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "20" ]; do
	STATUS=$(curl -X GET http://demo.dev.cubecorp.io/rs/status/CubeCorp/Cube/fluentd-test-df-49/$REPLAY_ID | sed 's/^.*"status":"\([^"]*\)".*/\1/')
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
