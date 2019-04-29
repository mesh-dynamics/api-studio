#!/usr/bin/env bash

mvn package
IMAGE_NAME="cubeiocorp/cubews"
VERSION="demo-1.0"
DOCKER_BUILDKIT=1 docker build --target dev --no-cache --tag $IMAGE_NAME:$VERSION .
