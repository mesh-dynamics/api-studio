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

export const authConstants = {
    REQUEST_BEGIN: "auth/REQUEST_BEGIN",
    REQUEST_SUCCESS: "auth/REQUEST_SUCCESS",
    REQUEST_FAILURE: "auth/REQUEST_FAILURE",

    LOGOUT: "auth/LOGOUT",
    
    SET_USER: "auth/SET_USER",

    SET_MESSAGE: "auth/SET_MESSAGE",
    CLEAR_MESSAGE: "auth/CLEAR_MESSAGE",

    REMEMBER_CREDENTIALS: "auth/REMEMBER_CREDENTIALS",
    TOGGLE_REMEMBER_ME: "auth/TOGGLE_REMEMBER_ME",
    FORGET_CREDENTIALS: "auth/FORGET_CREDENTIALS",


    ACCESS_VIOLATION: "auth/ACCESS_VIOLATION",
};