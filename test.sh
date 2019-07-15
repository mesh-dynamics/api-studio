#!/usr/bin/env bash

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

#print replay ID
echo "Replay ID:" $REPLAY_ID
