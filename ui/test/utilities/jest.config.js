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

module.exports = {
    "roots": [
      "<rootDir>/../../test"
    ],
    "transform": {
      "^.+\\.(ts|tsx)$": "ts-jest"
    },
    
    verbose: false,
    "testTimeout": 90000,

    "testMatch": [
      "**/test/electron/**.+(ts|tsx|js)",
      "**/test/common/**.+(ts|tsx|js)",
      "**/test/web/**.+(ts|tsx|js)",
  ],
  
  }