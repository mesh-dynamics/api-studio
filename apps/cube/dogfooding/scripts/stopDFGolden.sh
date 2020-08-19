#change the mode from record to normal to stop data capture
sed -i 's/intent=record/intent=normal/g' kubernetes/dogfood_mock_agent_conf.yaml
sed -i 's/intent=record/intent=normal/g' kubernetes/dogfood_record_agent_conf.yaml
sed -i 's/intent=record/intent=normal/g' kubernetes/dogfood_replay_agent_conf.yaml

#apply the configmap changes
kubectl apply -f kubernetes/dogfood_mock_agent_conf.yaml
kubectl apply -f kubernetes/dogfood_record_agent_conf.yaml
kubectl apply -f kubernetes/dogfood_replay_agent_conf.yaml

#stop the existing recording by name golden name (don't pass lable, the latest can be stopped)
RESPONSE="$(curl --location --request POST 'https://demo.prod.cubecorp.io/api/cs/stopRecordingByNameLabel?customerId=CubeCorp&app=CubeApp&golden_name=two_hrs_dogfood_collec' \
--header 'Content-Type: application/x-www-form-urlencoded' --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBtZXNoZHluYW1pY3MuaW8iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwidHlwZSI6InBhdCIsImN1c3RvbWVyX2lkIjoxLCJpYXQiOjE1ODI4ODE2MjgsImV4cCI6MTg5ODI0MTYyOH0.P4DAjXyODV8cFPgObaULjAMPg-7xSbUsVJ8Ohp7xTQI' )"
echo $RESPONSE