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

import { goldenConstants } from "../constants/golden.constants";

const initialState = {
    isFetching: false,
    fetchComplete: false,
    message: "",
    selectedService: "",
    selectedApi: "",
    selectedGolden: {},
    requestContract: {
        params: {},
        body: {}
    },
    responseContract: {
        body: {
            table: [],
            json: {}
        }
    },
    requestExamples: {
        params: {},
        body: {},
    },
    responseExamples: {}
}

export const golden = (state = initialState, { type, data }) => {
    switch (type) {
    case goldenConstants.BEGIN_FETCH:
        return {
            ...state,
            isFetching: true,
            fetchComplete: false,
        };
    case goldenConstants.FETCH_COMPLETE:
        return {
            ...state,
            isFetching: false,
            fetchComplete: true,
        };
    case goldenConstants.SET_SELECTED_SERVICE:
        return { 
            ...state,  
            selectedService: data
        }
    case goldenConstants.SET_SELECTED_API_PATH:
        return {
            ...state,
            selectedApi: data
        }
    case goldenConstants.SET_GOLDEN_CONTRACTS:
        return {
            ...state,
            requestContract: data.request,
            responseContract: data.response
        }
    case goldenConstants.SET_GOLDEN_EXAMPLES:
        return {
            ...state,
            requestExamples: data.request,
            responseExamples: data.response
        };
    case goldenConstants.SET_SELECTED_GOLDEN:
        return {
            ...state,
            selectedGolden: {
                ...state.selectedGolden,
                ...data
            }
        };
    case goldenConstants.RESET_GOLDEN_VISIBILITY_DETAILS: 
        return initialState;
    case goldenConstants.SET_MESSAGE:
        return {
            ...state,
            message: data,
        };
    case goldenConstants.RESET_MESSAGE:
        return {
            ...state,
            message: ""
        };
    default:
        return state
    }
}
