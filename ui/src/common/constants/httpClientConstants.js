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

export const httpClientConstants = {
    DELETE_PARAM_IN_OUTGOING_TAB: "httpClient/DELETE_PARAM_IN_OUTGOING_TAB",
    ADD_PARAM_TO_OUTGOING_TAB: "httpClient/ADD_PARAM_TO_OUTGOING_TAB",
    DELETE_PARAM_IN_TAB: "httpClient/DELETE_PARAM_IN_TAB",
    UPDATE_EVENT_DATA_IN_TAB: "httpClient/UPDATE_EVENT_DATA_IN_TAB",
    UPDATE_GENERAL_SETTINGS: "httpClient/UPDATE_GENERAL_SETTINGS",
    UPDATE_CONTEXT_MAP: "httpClient/UPDATE_CONTEXT_MAP",
    UPDATE_CONTEXT_MAP_AFTER_RESPONSE: "httpClient/UPDATE_CONTEXT_MAP_AFFTER_RESPONSE",
    DELETE_CONTEXT_MAP: "httpClient/DELETE_CONTEXT_MAP",
    ADD_PARAM_TO_TAB: "httpClient/ADD_PARAM_TO_TAB",
    UPDATE_PARAM_IN_OUTGOING_TAB: "httpClient/UPDATE_PARAM_IN_OUTGOING_TAB",
    UPDATE_PARAM_IN_TAB: "httpClient/UPDATE_PARAM_IN_TAB",
    UPDATE_ALL_PARAMS_IN_OUTGOING_TAB: "httpClient/UPDATE_ALL_PARAMS_IN_OUTGOING_TAB",
    UPDATE_ALL_PARAMS_IN_TAB: "httpClient/UPDATE_ALL_PARAMS_IN_TAB",
    UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_OUTGOING_TAB: "httpClient/UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_OUTGOING_TAB",
    UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_TAB: "httpClient/UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_TAB",
    PRE_DRIVE_REQUEST: "httpClient/PRE_DRIVE_REQUEST",
    POST_SUCCESS_DRIVE_REQUEST: "httpClient/POST_SUCCESS_DRIVE_REQUEST",
    AFTER_RESPONSE_RECEIVED_DATA: "httpClient/AFTER_RESPONSE_RECEIVED_DATA",
    POST_ERROR_DRIVE_REQUEST: "httpClient/POST_ERROR_DRIVE_REQUEST",
    ADD_TAB: "httpClient/ADD_TAB",
    ADD_CACHED_COLLECTIONS: "httpClient/ADD_CACHED_COLLECTIONS",
    MERGE_STATE: "httpClient/MERGE_STATE",
    ADD_OUTGOING_REQUESTS_TO_TAB: "httpClient/ADD_OUTGOING_REQUESTS_TO_TAB",
    ADD_USER_HISTORY_COLLECTION: "httpClient/ADD_USER_HISTORY_COLLECTION",
    SET_HISTORY_LOADING: "httpClient/SET_HISTORY_LOADING",
    SET_COLLECTION_LOADING: "httpClient/SET_COLLECTION_LOADING",
    SET_COLLECTION_TAB_STATE: "httpClient/SET_COLLECTION_TAB_STATE",
    ADD_CUBE_RUN_HISTORY: "httpClient/ADD_CUBE_RUN_HISTORY",
    DELETE_CUBE_RUN_HISTORY: "httpClient/DELETE_CUBE_RUN_HISTORY",
    ADD_USER_COLLECTIONS: "httpClient/ADD_USER_COLLECTIONS",
    ADD_ALL_USER_COLLECTIONS: "httpClient/ADD_ALL_USER_COLLECTIONS",
    DELETE_USER_COLLECTION : "httpClient/DELETE_USER_COLLECTION",
    POST_SUCCESS_LOAD_RECORDED_HISTORY: "httpClient/POST_SUCCESS_LOAD_RECORDED_HISTORY",
    SET_INACTIVE_HISTORY_CURSOR: "httpClient/SET_INACTIVE_HISTORY_CURSOR",
    SET_ACTIVE_HISTORY_CURSOR: "httpClient/SET_ACTIVE_HISTORY_CURSOR",
    SET_SELECTED_TAB_KEY: "httpClient/SET_SELECTED_TAB_KEY",
    REMOVE_TAB: "httpClient/REMOVE_TAB",
    SET_ENVIRONMENT_LIST: "httpClient/SET_ENVIRONMENT_LIST",
    SET_ENV_STATUS_TEXT: "httpClient/SET_ENV_STATUS_TEXT",
    RESET_ENV_STATUS_TEXT: "httpClient/RESET_ENV_STATUS_TEXT",
    SHOW_ENV_LIST: "httpClient/SHOW_ENV_LIST",
    SET_SELECTED_ENVIRONMENT: "httpClient/SET_SELECTED_ENVIRONMENT",
    SET_AS_REFERENCE: "httpClient/SET_AS_REFERENCE",
    CLOSE_ADD_MOCK_REQ_MODAL: "httpClient/CLOSE_ADD_MOCK_REQ_MODAL",
    SET_UPDATED_MODAL_MOCK_REQ_DETAILS: "httpClient/SET_UPDATED_MODAL_MOCK_REQ_DETAILS",
    SHOW_ADD_MOCK_REQ_MODAL: "httpClient/SHOW_ADD_MOCK_REQ_MODAL",
    SET_SELECTED_TRACE_TABLE_REQ_TAB: "httpClient/SET_SELECTED_TRACE_TABLE_REQ_TAB",
    SET_SELECTED_TRACE_TABLE_TEST_REQ_TAB: "httpClient/SET_SELECTED_TRACE_TABLE_TEST_REQ_TAB",
    RESET_RUN_STATE: "httpClient/RESET_RUN_STATE",
    SET_REQUEST_RUNNING: "httpClient/SET_REQUEST_RUNNING",
    UNSET_REQUEST_RUNNING: "httpClient/UNSET_REQUEST_RUNNING",
    CREATE_DUPLICATE_TAB: "httpClient/CREATE_DUPLICATE_TAB",
    TOGGLE_SHOW_TRACE: "httpClient/TOGGLE_SHOW_TRACE",

    SET_MOCK_CONFIG_LIST: "httpClient/SET_MOCK_CONFIG_LIST",
    SET_MOCK_CONFIG_STATUS_TEXT: "httpClient/SET_MOCK_CONFIG_STATUS_TEXT",
    RESET_MOCK_CONFIG_STATUS_TEXT: "httpClient/RESET_MOCK_CONFIG_STATUS_TEXT",
    SHOW_MOCK_CONFIG_LIST: "httpClient/SHOW_MOCK_CONFIG_LIST",
    SET_SELECTED_MOCK_CONFIG: "httpClient/SET_SELECTED_MOCK_CONFIG",

    RESET_HTTP_CLIENT_TO_INITIAL_STATE: "httpClient/RESET_HTTP_CLIENT_TO_INITIAL_STATE",
    SET_MOCK_CONTEXT_LOOKUP_COLLECTION: "httpClient/SET_MOCK_CONTEXT_LOOKUP_COLLECTION",
    SET_MOCK_CONTEXT_SAVE_TO_COLLECTION: "httpClient/SET_MOCK_CONTEXT_SAVE_TO_COLLECTION",
    UNSET_HAS_CHANGED_ALL: "httpClient/UNSET_HAS_CHANGED_ALL",
    UPDATE_ABORT_REQUEST: "httpClient/UPDATE_ABORT_REQUEST",
    SET_TAB_IS_HIGHLIGHTED: "httpClient/SET_TAB_IS_HIGHLIGHTED",
    UPDATE_UI_PREFERENCE: "httpClient/UPDATE_UI_PREFERENCE",
    DELETE_OUTGOING_REQ: "httpClient/DELETE_OUTGOING_REQ",
    REPLACE_ALL_PARAMS_IN_TAB: "httpClient/REPLACE_ALL_PARAMS_IN_TAB",
    REPLACE_ALL_PARAMS_IN_OUTGOING_TAB: "httpClient/REPLACE_ALL_PARAMS_IN_OUTGOING_TAB",
    SET_HISTORY_PATH_FILTER: "httpClient/SET_HISTORY_PATH_FILTER",
    TOGGLE_HIDE_INTERNAL_HEADERS: "httpClient/TOGGLE_HIDE_INTERNAL_HEADERS",

    UPDATE_HTTP_STATUS_IN_TAB: "httpClient/UPDATE_HTTP_STATUS_IN_TAB",
    SET_PROTO_DESCRIPTOR_VALUES: "httpClient/SET_PROTO_DESCRIPTOR_VALUES",
    // CLEAR_GRPC_SCHEMA_ON_APP_CHANGE: "httpClient/CLEAR_GRPC_SCHEMA_ON_APP_CHANGE",
    UPDATE_REQUEST_TYPE_IN_SELECTED_TAB: "httpClient/UPDATE_REQUEST_TYPE_IN_SELECTED_TAB: ",
    UPDATE_GRPC_CONNECTION_DETAILS_IN_TAB: "httpClient/UPDATE_GRPC_CONNECTION_DETAILS_IN_TAB",
    UPDATE_REQUEST_TYPE_IN_SELECTED_OUTGOING_TAB: "httpClient/UPDATE_REQUEST_TYPE_IN_SELECTED_OUTGOING_TAB: ",
    UPDATE_GRPC_CONNECTION_DETAILS_IN_OUTGOING_TAB: "httpClient/UPDATE_GRPC_CONNECTION_DETAILS_IN_OUTGOING_TAB",
    UPDATE_TAB_WITH_NEW_DATA: "httpClient/UPDATE_TAB_WITH_NEW_DATA",
    UPDATE_OUTGOING_TAB_WITH_NEW_DATA: "httpClient/UPDATE_OUTGOING_TAB_WITH_NEW_DATA",
    CHANGE_TAB_POSITION: "httpClient/CHANGE_TAB_POSITION",
    UPDATE_ADD_TO_SERVICE: "httpClient/UPDATE_ADD_TO_SERVICE",
    UPDATE_REQUEST_METADATA_TAB: "httpClient/UPDATE_REQUEST_METADATA_TAB",
    SET_SIDEBAR_TAB_ACTIVE_KEY: "httpCLient/SET_SIDEBAR_TAB_ACTIVE_KEY",
}