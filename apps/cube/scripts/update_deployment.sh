#!/bin/bash

update_deployment_app() {
  echo Updating deployment
	TAG=$1; shift
	kubectl -n $NAMESPACE set image deployment/cubews-mock-v1 cubews=cubeiocorp/cubews:$TAG
	kubectl -n $NAMESPACE set image deployment/cubews-record-v1 cubews=cubeiocorp/cubews:$TAG
	kubectl -n $NAMESPACE set image deployment/cubews-replay-v1 cubews=cubeiocorp/cubews:$TAG
}
