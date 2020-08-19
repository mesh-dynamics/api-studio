#!/usr/bin/env bash

BRANCH=${GITHUB_REF##*/}

npm install

# assign version
jq --arg version "${BRANCH}-${GITHUB_SHA}" '.version = $version' package.json > "tmp" && mv "tmp" package.json

# build js
npx webpack --mode production --display minimal --config webpack.electron.config.js

# build and publish the electron application based on runner OS
if [ "$RUNNER_OS" == "macOS" ]; then
    npx electron-builder build -ml --config electron-builder.${BRANCH}.yml --publish always
elif [ "$RUNNER_OS" == "Windows" ]; then
    npx electron-builder build -w --config electron-builder.${BRANCH}.yml --publish always
elif [ "$RUNNER_OS" == "Linux" ]; then
    npx electron-builder build -l --config electron-builder.${BRANCH}.yml --publish always
fi