#!/usr/bin/env bash

#
# Copyright 2021 MeshDynamics.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -x
#Init Replay

REPLAY_PATHS=minfo/listmovies,minfo/returnmovie,minfo/rentmovie,minfo/liststores
REPLAY_ENDPOINT=http://staging.dev.cubecorp.io
CUBE_ENDPOINT=http://staging.dev.cubecorp.io
INSTANCE_ID=prod
USER_ID=demo@cubecorp.io
REPLAY_PATHS=$(echo $REPLAY_PATHS | tr "," "\n")
for path in $REPLAY_PATHS
do
  TEMP_PATH="$TEMP_PATH""paths=$path&"
done

REPLAY_PATHS=${TEMP_PATH::${#TEMP_PATH}-1}
BODY="$REPLAY_PATHS&endPoint=$REPLAY_ENDPOINT&instanceId=$INSTANCE_ID&templateSetVer=DEFAULT&userId=$USER_ID"

REPLAY_ID=$(curl -X POST \
	$CUBE_ENDPOINT/rs/start/Recording--1834097741 \
	-H 'Content-Type: application/x-www-form-urlencoded' \
	-H 'cache-control: no-cache' \
	-d $BODY \
| sed 's/^.*"replayId":"\([^"]*\)".*/\1/')

echo $REPLAY_ID

#Start replay
#curl -f -X POST \
#  http://demo.dev.cubecorp.io/rs/start/CubeCorp/Cube/fluentd-test-df-49/$REPLAY_ID \
#  -H 'Content-Type: application/x-www-form-urlencoded' \
#  -H 'cache-control: no-cache'

#Status Check
COUNT=0
while [ "$STATUS" != "Completed" ] && [ "$STATUS" != "Error" ] && [ "$COUNT" != "20" ]; do
	STATUS=$(curl -X GET $CUBE_ENDPOINT/rs/status/CubeCorp/MovieInfo/d2b10037-c6be-4fe8-b3ea-fb44213e922e/$REPLAY_ID | sed 's/^.*"status":"\([^"]*\)".*/\1/')
	sleep 5
	COUNT=$((COUNT+1))
done

#Run analyze
ANALYZE=$(curl -X POST $CUBE_ENDPOINT/as/analyze/$REPLAY_ID -H 'Content-Type: application/x-www-form-urlencoded' -H 'cache-control: no-cache')
REQNOTMATCHED=$(echo $ANALYZE | sed 's/^.*"reqnotmatched":\([^"]*\).*/\1/' | cut -d ',' -f 1)
RESPNOTMATCHED=$(echo $ANALYZE | sed 's/^.*"respnotmatched":\([^"]*\).*/\1/' | cut -d ',' -f 1)

#Display replay ID
echo "Replay ID:" $REPLAY_ID
#Exit with non-zero exit code if reqstnotmatched and respnotmatchted are have nono-zero value
if [ "$RESPNOTMATCHED" = "0" ]; then
	echo "test passed"
	exit 0
else
	echo "test failed"
	exit 1
fi
