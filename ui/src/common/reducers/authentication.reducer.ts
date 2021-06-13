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

import { authConstants } from '../constants';
import { ipcRenderer } from '../helpers/ipc-renderer';
import { IAuthenticationState } from './state.types';

export interface IAuthenticationAction{
  type: string,
  payload:any;
}

const initialState: IAuthenticationState = {
  user: {
    access_token: "",
    customer_name: "",
    expires_in: 0,
    refresh_token: "",
    roles: [],
    timestamp: "",
    token_type: "",
    username: ""
  }, 
  role: "",
  loggedIn: false,
  isFetching: false,
  messages: [],
  accessViolation: false,
  rememberMe: false,
  credentials: {
    username: '',
    password: ''
  }
};

const authenticationReducer = (state = initialState, action: IAuthenticationAction) => {
  switch (action.type) {
  case authConstants.REQUEST_BEGIN:
    return { 
      ...state, 
      isFetching: true,
      messages: []
    };
  case authConstants.REQUEST_SUCCESS:
    return {
      ...state,
      isFetching: false,
      loggedIn: true,
      user: {...action.payload.user, timestamp: new Date()}
    };
  case authConstants.REQUEST_FAILURE:
    return {
      ...state,
      isFetching: false,
      loggedIn: false,
      user: "",
      messages: [action.payload] 
      };
  case authConstants.SET_USER:
    return {
      ...state,
      user: {...action.payload, timestamp: new Date()},
      loggedIn: true,
    };
  case authConstants.LOGOUT:
    return {
      ...state,
      loggedIn: false,
      accessViolation: false,
      user: {},
    };
  case authConstants.SET_MESSAGE:
    return {
      ...state,
      messages: [action.payload]
    };
  case authConstants.CLEAR_MESSAGE:
    return {
      ...state,
      messages: []
    };
  case authConstants.ACCESS_VIOLATION:
    return {
      ...state,
      accessViolation: true
    };
  case authConstants.REMEMBER_CREDENTIALS:
    return {
      ...state,
      credentials: {
        username: action.payload.username,
        password: action.payload.password
      }
    }
  case authConstants.TOGGLE_REMEMBER_ME:
    return {
      ...state,
      rememberMe: !state.rememberMe,
    };
  case authConstants.FORGET_CREDENTIALS:
    return {
      ...state,
      credentials: {
        username: '',
        password: ''
      }
    };
  default:
    return state
  }
}

export { authenticationReducer as authentication };