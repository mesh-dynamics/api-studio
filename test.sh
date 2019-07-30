#!/usr/bin/env bash

set -x
#Init Replay
REPLAY_ID=$(curl -X POST \
	http://demo.dev.cubecorp.io/rs/init/CubeCorp/Cube/dogfood-14-june-1 \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'cache-control: no-cache' \
	-d 'endpoint=http://staging.dev.cubecorp.io&instanceid=PROD' | awk -F ',' '{print $7}' | cut -d '"' -f 4)

#Start replay
curl -f -X POST \
  http://demo.dev.cubecorp.io/rs/start/CubeCorp/Cube/dogfood-14-june-1/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'

#Status Check
COUNT=0
while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "20" ]; do
	STATUS=$(curl -X GET http://demo.dev.cubecorp.io/rs/status/CubeCorp/Cube/dogfood-14-june-1/$REPLAY_ID | awk -F ',' '{print $9}' | cut -d '"' -f 4)
	sleep 5
	COUNT==$((COUNT+1))
done

#Run analyze
ANALYZE=$(curl -X POST http://demo.dev.cubecorp.io/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache')
REQNOTMATCHED=$(echo $ANALYZE | awk -F ',' '{print $8}' | cut -d '"' -f 4)
RESPNOTMATCHED=$(echo $ANALYZE | awk -F ',' '{print $11}' | cut -d '"' -f 4)

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
