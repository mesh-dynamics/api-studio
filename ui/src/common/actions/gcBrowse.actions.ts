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

import { gcbrowseConstants } from '../constants';
import { cubeService } from '../services';
import { IActionsType } from './actions.types';
import { cubeActions } from './cube.actions';


const gcbrowseActions : IActionsType= {
    beginFetch: () => ({ type: gcbrowseConstants.REQUEST_BEGIN }),

    fetchSuccess: () => ({ type: gcbrowseConstants.REQUEST_SUCCESS }),

    fetchFailure: (payload) => ({ type: gcbrowseConstants.REQUEST_FAILURE, payload }),

    loadActualGoldens: (payload) => ({ type: gcbrowseConstants.LOAD_GOLDENS, payload}),

    loadUserGoldens: (payload) => ({ type: gcbrowseConstants.LOAD_USER_GOLDENS, payload }),

    fetchGoldensCollections: (selectedSource: string) => async (dispatch, getState) => {
        const { cube: { selectedApp }, authentication: { user }} = getState();

        dispatch(gcbrowseActions.beginFetch());

        try {
            const results = await cubeService.fetchCollectionList(user, selectedApp!, selectedSource);

            dispatch(gcbrowseActions.fetchSuccess());

            switch(selectedSource) {
                case 'UserGolden':
                    dispatch(gcbrowseActions.loadUserGoldens(results));
                case 'Golden':
                    dispatch(gcbrowseActions.loadActualGoldens(results));
                default:
                    // Do not do anything (for now) if it is not the right source type
            }
        } catch (error) {
            dispatch(gcbrowseActions.fetchFailure('Failed to fetch collections from server'));
        }
    },

    deleteGolden: (selectedItemId, selectedSource) => async (dispatch) => {
        try {
            await cubeService.deleteGolden(selectedItemId);
            dispatch(cubeActions.removeSelectedGoldenFromTestIds(selectedItemId));
            dispatch(gcbrowseActions.fetchGoldensCollections(selectedSource))
        } catch (error) {
            console.log("Error caught in softDelete Golden: " + error);
        }
    }
};

export default gcbrowseActions;