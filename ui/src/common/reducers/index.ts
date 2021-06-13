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

import { combineReducers } from 'redux';

import { authentication } from './authentication.reducer';
import { navigation } from './navigation.reducer';
import { golden } from './golden.reducer';
import { cube } from './cube.reducer';
import { apiCatalog } from './api-catalog.reducer';
import { httpClient } from './httpClientReducer';
import { gcBrowse } from './gcBrowse.reducer';

const rootReducer = combineReducers({
  authentication,
  navigation,
  golden,
  cube,
  apiCatalog,
  httpClient,
  gcBrowse
});

export default rootReducer;