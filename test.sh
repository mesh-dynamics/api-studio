#!/usr/bin/env bash

set -x
#Init Replay
REPLAY_ID=$(curl -X POST \
	http://dogfooding.cubecorp.io/cube/rs/init/demo@cubecorp.io/Cube/dogfood-14-june-1 \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'cache-control: no-cache' \
	-d 'endpoint=http://staging.cubecorp.io&instanceid=PROD' | awk -F ',' '{print $7}' | cut -d '"' -f 4)

#Start replay
curl -f -X POST \
  http://dogfooding.cubecorp.io/rs/start/demo@cubecorp.io/Cube/dogfood-14-june-1/$REPLAY_ID \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'cache-control: no-cache'

#Status Check
while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ]; do
	STATUS=$(curl -X GET http://dogfooding.cubecorp.io/rs/status/demo@cubecorp.io/Cube/dogfood-14-june-1/$REPLAY_ID | awk -F ',' '{print $9}' | cut -d '"' -f 4)
	sleep 5
done

#Run analyze
ANALYZE=$(curl -X POST http://dogfooding.cubecorp.io/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache')
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
