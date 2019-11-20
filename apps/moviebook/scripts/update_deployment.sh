#!/bin/bash

update_deployment_app() {
  echo Updating deployment
	TAG=$1; shift
	kubectl -n $NAMESPACE set image deployment/movieinfo-v1 movieinfo=cubeiocorp/sample_apps-mirest:$TAG
	kubectl -n $NAMESPACE set image deployment/movieinfo-v2 movieinfo=cubeiocorp/sample_apps-mirest:$TAG
	kubectl -n $NAMESPACE set image deployment/restwrapjdbc-v1 restwrapjdbc=cubeiocorp/restwrapjdbc:$TAG
}
