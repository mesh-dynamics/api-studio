#!/usr/bin/env bash

IMAGE_NAME="cubeiocorp/cubeui"
VERSION="v2"
docker build --no-cache --tag $IMAGE_NAME:$VERSION .
