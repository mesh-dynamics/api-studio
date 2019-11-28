#!/usr/bin/env bash

#Add settings.xml file for github auth
RUN echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>e7d95f942f6d4f68f23245ad7ddcdb02cafb99a4</password></server></servers></settings>" > ~/.m2/settings.xml
mvn package
IMAGE_NAME="cubeiocorp/cubews"
VERSION=`git rev-parse HEAD`
DOCKER_BUILDKIT=1 docker build --target dev --no-cache --tag $IMAGE_NAME:$VERSION .
