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