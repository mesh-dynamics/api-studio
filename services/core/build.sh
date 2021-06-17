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

#Add settings.xml file for github auth
echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>e7d95f942f6d4f68f23245ad7ddcdb02cafb99a4</password></server></servers></settings>" > ~/.m2/settings.xml
mvn package
IMAGE_NAME="cubeiocorp/cubews"
VERSION=`git rev-parse HEAD`
DOCKER_BUILDKIT=1 docker build --target dev --no-cache --tag $IMAGE_NAME:$VERSION .
