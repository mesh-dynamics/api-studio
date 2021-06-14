#!/bin/bash

# Usage: ./update_deployment.sh $DEPLOYMENT_NAME $TAG
# where $DEPLOYMENT_NAME could be cubews, cubeui or cubeui-backend
# and $TAG is the container image tag to be deployed.

cubews () {
	echo Updating deployment
	kubectl -n $NAMESPACE set image deployment/cubews-mock-v1 cubews=cubeiocorp/cubews:$TAG --record
	kubectl -n $NAMESPACE set image deployment/cubews-record-v1 cubews=cubeiocorp/cubews:$TAG --record
	kubectl -n $NAMESPACE set image deployment/cubews-replay-v1 cubews=cubeiocorp/cubews:$TAG --record
}

cubeui () {
	kubectl -n $NAMESPACE set image deployment/cubeui-v1 cubeui=cubeiocorp/cubeui:$TAG --record
}

cubeui-backend () {
	kubectl -n $NAMESPACE set image deployment/cubeui-backend-v1 cubeui-backend=cubeiocorp/cubeuibackend:$TAG --record
}
main () {
# To debug this script, run it with TRACE=1 in the enviornment
	# -x option will trace each command that is run
	set -eox pipefail; [[ "$TRACE" ]] && set -x
	NAMESPACE=cube
	TAG=$2
	case "$1" in
		cubews) shift; cubews "$@";;
		cubeui) shift; cubeui "$@";;
		cubeui-backend) shift; cubeui-backend "$@";;
		*) echo "Invalid option. Valid options are cubews, cubeui, cubeui-backend"
	esac
}

main "$@"