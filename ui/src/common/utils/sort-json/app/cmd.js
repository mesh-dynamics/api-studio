/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#!/usr/bin/env node

// Core dependencies
const path = require('path');

// NPM dependencies
const minimist = require('minimist');
const sortJson = require('./');

const alias = {
  depth: ['d'],
  reverse: ['r'],
  ignoreCase: ['ignore-case', 'i'],
  indentSize: ['indent-size', 'spaces'],
  noFinalNewLine: ['no-final-newline', 'nn'],
};

const argv = minimist(process.argv.slice(2), { alias });

// Get all the files
const files = argv._.filter(arg => arg.endsWith('.json') || arg.endsWith('.rc'));

sortJson.overwrite(files.map(file => path.resolve(file)), argv);
