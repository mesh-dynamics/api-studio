url="${1}";

accessToken=$(curl "$url/api/login" -H 'Connection: keep-alive' -H 'Content-Type: application/json' -H 'Accept: */*' --data-binary '{"username":"admin@meshdynamics.io","password":"admin"}'| jq -r '.access_token');

token="Bearer $accessToken";
echo $token;

customerId=$(curl -X POST "$url/api/customer/save" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"cubeCorp","email":"cubecorp@meshdynamics.io","domainURL":"meshdynamics.io"}' | jq -r '.id');
echo $customerId;

appData=$(curl -X POST "$url/api/app" -H 'Content-Type: application/json' -H "Authorization: $token" --data-binary '{"name":"MovieInfo","customerId": "'"$customerId"'"}');

echo $appData;



