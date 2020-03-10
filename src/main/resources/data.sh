url="${1}";

accessToken=$(curl "$url/api/login" -H 'Connection: keep-alive' -H 'Content-Type: application/json' -H 'Accept: */*' --data-binary '{"username":"admin@meshdynamics.io","password":"admin"}'| jq -r '.access_token');

token="Bearer $accessToken";
echo $token;

customerId=$(curl -X POST "$url/api/customer/save" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"cubeCorp","email":"cubecorp@meshdynamics.io","domainURL":"meshdynamics.io"}' | jq -r '.id');
echo "CustomerId="$customerId;

userId=$(curl -X POST "$url/api/account/create-user" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"cubeCorp","email":"cubecorp@meshdynamics.io","password": "password123", "domain":"meshdynamics.io", "customerId": "'"$customerId"'"}' | jq -r '.id');
echo "UserId="$userId;

appId=$(curl -X POST "$url/api/app" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"MovieInfo","customerId": "'"$customerId"'"}' | jq -r '.id');
echo "AppId="$appId;

appUserId=$(curl -X POST "$url/api/app-user" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"appId":"'"$appId"'","userId": "'"$userId"'"}' | jq -r '.id')
echo "AppUserId="$appUserId;

instanceAppId=$(curl -X POST "$url/api/instance" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"test","appId": "'"$appId"'", "gatewayEndpoint":"testendpoint"}' | jq -r '.id')
echo "InstanceAppId="$instanceAppId;

instanceUserId=$(curl -X POST "$url/api/instance-user" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"instanceId":"'"$instanceAppId"'","userId": "'"$userId"'"}' | jq -r '.id')
echo "InstanceUserId="$instanceUserId;

serviceGroupId=$(curl -X POST "$url/api/service-group" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"testServiceGroup","appId": "'"$appId"'"}' | jq -r '.id')
echo "ServiceGroupId="$serviceGroupId;

serviceId1=$(curl -X POST "$url/api/service" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"testService1","appId": "'"$appId"'", "serviceGroupId": "'"$serviceGroupId"'"}' | jq -r '.id')
echo "ServiceId1="$serviceId1;

serviceId2=$(curl -X POST "$url/api/service" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"testService2","appId": "'"$appId"'", "serviceGroupId": "'"$serviceGroupId"'"}' | jq -r '.id')
echo "ServiceId2="$serviceId2;

serviceGraphId=$(curl -X POST "$url/api/service_graph" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"appId": "'"$appId"'", "fromServiceId": "'"$serviceId1"'", "toServiceId": "'"$serviceId2"'"}' | jq -r '.id')
echo "ServiceGraphId="$serviceGraphId;

pathId=$(curl -X POST "$url/api/path" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"path": "testPath", "serviceId": "'"$serviceId1"'"}' | jq -r '.id')
echo "PathId="$pathId;

recordingId=$(curl -X POST "$url/api/recording" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"appId": "'"$appId"'","instanceId":"'"$instanceAppId"'", "collectionName": "testCollection", "status": "RUNNING"}' | jq -r '.id')
echo "RecordingId="$recordingId;

testConfigId=$(curl -X POST "$url/api/test_config" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"testConfigName":"testConfigTest","description":"descriptionTest","appId": "'"$appId"'","gatewayServiceId":"'"$serviceId1"'", "maxRunTimeMin": 12, "emailId":"cubecorp@meshdynamics.io", "slackId":"testslackId"}' | jq -r '.id')
echo "TestConfigId="$testConfigId;

replayId=$(curl -X POST "$url/api/replay" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"replayName":"testReplay","testId": "'"$testConfigId"'","collectionId":"'"$recordingId"'", "status": "RUNNING", "reqCount":1, "reqSent":1, "reqFailed":0, "analysis":true, "sampleRate":2.3}' | jq -r '.id')
echo "ReplayId="$replayId;

compareTemplateId=$(curl -X POST "$url/api/compare_template" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"appId": "'"$appId"'","serviceId":"'"$serviceId1"'", "path": "testPath", "template":true, "type":"REQUEST"}' | jq -r '.id')
echo "CompareTemplateId="$compareTemplateId;

testIntermediateServiceId=$(curl -X POST "$url/api/test_intermediate_service" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"testId": "'"$testConfigId"'","serviceId":"'"$serviceId1"'"}' | jq -r '.id')
echo "TestIntermediateServiceId="$testIntermediateServiceId;

testpathId=$(curl -X POST "$url/api/test-path" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"testId": "'"$testConfigId"'","pathId":"'"$pathId"'"}' | jq -r '.id')
echo "TestpathId="$testpathId;

testVirtualizedServiceId=$(curl -X POST "$url/api/test_virtualized_service" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"testId": "'"$testConfigId"'","serviceId":"'"$serviceId1"'"}' | jq -r '.id')
echo "TestVirtualizedServiceId="$testVirtualizedServiceId;
