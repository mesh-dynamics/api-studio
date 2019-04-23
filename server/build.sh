#!/usr/bin/env bash

IMAGE_NAME="cubeiocorp/cubews"
VERSION="demo-1.0"
docker build --no-cache --tag $IMAGE_NAME:$VERSION .
