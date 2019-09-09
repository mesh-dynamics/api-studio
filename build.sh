#!/usr/bin/env bash

mvn package
IMAGE_NAME="cubeiocorp/cubews"
VERSION="batchrr-1.9"
DOCKER_BUILDKIT=1 docker build --target dev --no-cache --tag $IMAGE_NAME:$VERSION .
