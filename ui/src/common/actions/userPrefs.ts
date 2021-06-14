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

import { apiCatalogConstants } from "../constants/api-catalog.constants";
import { httpClientConstants } from "../constants/httpClientConstants";
import { IStoreState, IUserAuthDetails } from "../reducers/state.types";

const persistKey = "persist:UserPref";

const userPref = {
  saveUserPreferences: () => (dispatch: any, getState: any) => {
    const existingStateString = localStorage.getItem(persistKey);
    let existingState = {};
    if (existingStateString) {
      try {
        existingState = JSON.parse(existingStateString);
      } catch (error) {
        console.error("Old user preference could not be retained", error);
      }
    }
    const currentState: IStoreState = getState();
    const {
      httpClient: { selectedMockConfig, selectedEnvironment },
      apiCatalog: { selectedSource, startTime, endTime },
      authentication: { user },
    } = currentState;
    const currentUserState = {
      httpClient: {
        selectedMockConfig,
        selectedEnvironment,
      },
      apiCatalog: {
        selectedSource,
        startTime,
        endTime
      }
    } as Partial<IStoreState>;
    const savedState = {
      ...existingState,
      [(user as IUserAuthDetails).username]: currentUserState,
    };
    localStorage.setItem(persistKey, JSON.stringify(savedState));
  },
  loadUserPreference: (username?: string) => (dispatch: any, getState: any) => {
    const existingStateString = localStorage.getItem(persistKey);
    let existingState: any = {};
    if (existingStateString) {
      try {
        existingState = JSON.parse(existingStateString);
      } catch (error) {
        console.error("Old user preference could not be loaded", error);
      }

      const currentState: IStoreState = getState();
      const {
        authentication: { user },
      } = currentState;
      const currentUserState =
        existingState[username || (user as IUserAuthDetails).username];
      if (currentUserState) {
        dispatch({
          type: httpClientConstants.MERGE_STATE,
          data: currentUserState.httpClient,
        });
        dispatch({
          type: apiCatalogConstants.MERGE_STATE,
          data: currentUserState.apiCatalog,
        });
      }
    }
  },
};

export default userPref;
