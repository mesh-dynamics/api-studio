# Moviebook App

## Steps to Deploy Moviebook App


## Requirements

1. A Kubernetes cluster uses the Secret of docker-registry type to authenticate with a container registry to pull a private image.  
Create a Secret by providing credentials on the command line  
```
kubectl create secret docker-registry regcred --docker-server=<your-registry-server> --docker-username=<your-name> --docker-password=<your-pword> --docker-email=<your-email>
```
where,  
`your-registry-server` is your Private Docker Registry FQDN. (https://index.docker.io/v1/ for DockerHub)  
`your-name` is your Docker username.  
`your-pword` is your Docker password.  
`your-email` is your Docker email.  

2. Export `CUBE_APPLICATION`, `CUBE_INSTANCEID` as Environment variables, for example:
```
export CUBE_APPLICATION=movieinfo
export CUBE_INSTANCEID=prod
```
3. Install python dependencies(pip, yaml, jinja2):
```
sudo easy_install pip
sudo pip install pyaml
sudo pip install jinja2
```

4. Add service names to `moviebook/service.yaml` for which lua filters will be generated.

### Deploy

NOTE: If you wish to run cubews in your IDE, make sure your `Application context` is set to `/` under `Deployment` tab in Tomcat Server configuration.
```
./deploy_moviebook.sh init
```
- This will deploy the moviebook app on the local kubernetes cluster.
- Generate Lua filters

### Record Traffic
```
./deploy_moviebook.sh record
```
This will deploy yamls and make curl request to start recording request/response on moviebook app.

### Stop Recording Traffic
```
./deploy_moviebook.sh stop_recording
```
This will make curl request and undeploy the yamls to stop recording traffic on moviebook app.

### Replay
```
./deploy_moviebook.sh replay
```
- Generate mock-all-except-moviebook.yaml
- mock all the apps except moviebook
- Make curl request to init and start replay

### Stop replay
```
./deploy_moviebook stop_replay
```
This will stop replaying.

### Analyze
```
./deploy_moviebook.sh analyze
```
This will make curl request to start analyze.

### clean
```
./deploy_moviebook.sh clean
```
This will delete moviebook app from the kubernetes cluster
