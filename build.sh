#!/usr/bin/env bash

IMAGE_NAME="cubeiocorp/cubeuibackend"
VERSION="v-1.0.0"
DOCKER_BUILDKIT=1 docker build --no-cache --tag $IMAGE_NAME:$VERSION .
