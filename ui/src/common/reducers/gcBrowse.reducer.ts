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

import { gcbrowseConstants, defaultCollectionItem } from '../constants';
import { IGoldenCollectionBrowseState } from './state.types';

interface IGCBrowseAction {
    type: string;
    payload: any;
}

const initialState: IGoldenCollectionBrowseState = {
    currentCollectionType: '',
    actualGoldens: {
        numFound: 0,
        recordings: []
    },
    userGoldens: {
        numFound: 0,
        recordings: []
    },
    isCollectionLoading: false,
    messages: [],
}

const gcbrowseReducer = (state = initialState, action: IGCBrowseAction) => {
    switch (action.type) {
        case gcbrowseConstants.REQUEST_BEGIN: {
            return {
                ...state,
                isCollectionLoading: true,
                messages: []
            }
        }
        case gcbrowseConstants.REQUEST_FAILURE: {
            return {
                ...state,
                isCollectionLoading: false,
                message: [action.payload]
            }
        }
        case gcbrowseConstants.REQUEST_SUCCESS: {
            return {
                ...state,
                isCollectionLoading: false,
                message: []
            }

        }
        case gcbrowseConstants.LOAD_GOLDENS: {
            return {
                ...state,
                actualGoldens: action.payload,
            }
        }
        case gcbrowseConstants.LOAD_USER_GOLDENS: {
            return {
                ...state,
                userGoldens: action.payload,
            }
        }
        default:
            return state;
    }
}

export {
    IGCBrowseAction,
    gcbrowseReducer as gcBrowse,
    defaultCollectionItem
}