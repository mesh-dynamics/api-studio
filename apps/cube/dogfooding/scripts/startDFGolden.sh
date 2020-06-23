#create a new recording
TIMESTAMP=$(date +%s)
RESPONSE="$(curl -X POST \
          https://demo.prod.cubecorp.io/api/cs/start/CubeCorp/CubeApp/devcluster/DEFAULT \
          -H 'Content-Type: application/x-www-form-urlencoded' \
          -H 'cache-control: no-cache' \
          -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZXNoZHluYW1pY3MuaW8iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoxLCJpYXQiOjE1ODI4ODE2MjgsImV4cCI6MTg5ODI0MTYyOH0.P4DAjXyODV8cFPgObaULjAMPg-7xSbUsVJ8Ohp7xTQI' \
          -d "name=two_hrs_dogfood_collec&userId=demo@meshdynamics.io&label=$TIMESTAMP" )"

echo $RESPONSE

#change the mode again back to record
sed -i 's/intent=normal/intent=record/g' kubernetes/dogfood_mock_agent_conf.yaml
sed -i 's/intent=normal/intent=record/g' kubernetes/dogfood_record_agent_conf.yaml
sed -i 's/intent=normal/intent=record/g' kubernetes/dogfood_replay_agent_conf.yaml

#apply the configmap changes
kubectl apply -f kubernetes/dogfood_mock_agent_conf.yaml
kubectl apply -f kubernetes/dogfood_record_agent_conf.yaml
kubectl apply -f kubernetes/dogfood_replay_agent_conf.yaml