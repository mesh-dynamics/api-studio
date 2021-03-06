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

import { authConstants } from '../constants/auth.constants';
import { 
    validateCredentials, 
    validateReCaptcha, 
    createUser 
} from '../services/auth.service';
import { history } from '../helpers';
import { ipcRenderer } from '../helpers/ipc-renderer';
import { apiCatalogActions } from './api-catalog.actions';
import { cubeActions } from './cube.actions';
import { httpClientActions } from './httpClientActions';
import { goldenActions } from './golden.actions';
import config from '../config';
import { getRefreshToken} from "../utils/lib/common-utils";
import userPref from './userPrefs';

const authActions = {
  beginFetch: () => ({ type: authConstants.REQUEST_BEGIN }),

  fetchSuccess: () => ({ type: authConstants.REQUEST_SUCCESS }),

  fetchFailure: (payload) => ({ type: authConstants.REQUEST_FAILURE, payload }),

  setUser: (payload) => ({ type: authConstants.SET_USER, payload }),

  setMessage: (message) => ({ type: authConstants.SET_MESSAGE, message }),

  clearMessage: () => ({ type: authConstants.CLEAR_MESSAGE }),

  clearUser: () => ({ type: authConstants.LOGOUT }),

  accessViolationDetected: () => ({ type: authConstants.ACCESS_VIOLATION }),

  rememberCredentials: (payload) => ({ type: authConstants.REMEMBER_CREDENTIALS, payload }),

  toggleRememberMe: () => ({ type: authConstants.TOGGLE_REMEMBER_ME }),

  forgetCredentials: () => ({ type: authConstants.FORGET_CREDENTIALS }),

  login: (username, password) => async (dispatch) => {
    dispatch(authActions.clearMessage());

    dispatch(authActions.beginFetch());

    try {
      const user = await validateCredentials(username, password);

      dispatch(authActions.setUser(user));

      if (PLATFORM_ELECTRON) {
        ipcRenderer.send("set_user", user);
      }

      history.push("/");
      dispatch(userPref.loadUserPreference(username));
    } catch (e) {
      dispatch(authActions.fetchFailure(e));
    }
  },

  logout: () => (dispatch) => {
    dispatch(userPref.saveUserPreferences());
    dispatch(apiCatalogActions.resetApiCatalogToInitialState());
    dispatch(cubeActions.resetCubeToInitialState());
    dispatch(httpClientActions.resetHttpClientToInitialState());
    dispatch(goldenActions.resetGoldenVisibilityDetails());
    dispatch(authActions.clearUser());

    if (PLATFORM_ELECTRON) {
      const user = {
        access_token: "",
        customer_name: "",
        token_type: "",
        username: "",
      };

      ipcRenderer.send("set_user", user);
      // Reset context as well
      ipcRenderer.send("reset_context_to_default");
    }

    localStorage.removeItem("user"); // TODO: Remove this after a few release cycles

    history.push("/login");
  },

  tryLocalLogin: () => async (dispatch, getState) => {
    fetch(`${config.apiBaseUrl}/health`)
      .then(async () => {
        const username = "admin";
        const password = "admin";
        const user = await validateCredentials(username, password);

        dispatch(authActions.setUser(user));

        if (PLATFORM_ELECTRON) {
          ipcRenderer.send("set_user", user);
        }

        history.push("/");
        dispatch(userPref.loadUserPreference(username));
      })
      .catch((error) => {
        console.error(error);
        setTimeout(() => dispatch(authActions.tryLocalLogin()), 5000);
      });
  },

  localhostLogin: () => async (dispatch, getState) => {
    dispatch(authActions.clearMessage());

    dispatch(authActions.beginFetch());

    dispatch(authActions.tryLocalLogin());
  },

  //Silently refresh token before expiry
  refreshToken: () => async (dispatch, getState) => {
    const dataToPost = JSON.stringify({ refreshToken: getRefreshToken(getState()), grantType: "refreshToken" });
    return new Promise((resolve, reject) => {
      fetch(`${config.apiBaseUrl}/token`, {
        body: dataToPost,
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        mode: "cors",
      })
        .then(async (response) => {
          const data = await response.json();
          if (response.ok && data.status != 401) {
            dispatch(authActions.setUser(data));

            if (PLATFORM_ELECTRON) {
              ipcRenderer.send("set_user", data);
            }
            resolve();
          }
        })
        .catch((error) => {
          reject(error);
        });
    });
  },

  createUser: (user) => createUser(user),

  verifyToken: (token) => validateReCaptcha(token),
};

export default authActions;
