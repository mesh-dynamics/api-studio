#!/bin/bash

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