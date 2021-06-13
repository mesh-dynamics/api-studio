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

import {navigationConstants} from '../constants';
import { INavigationState } from './state.types';

export interface INavigationAction{
    type: string,
    title:any; 
  }


const initialState : INavigationState = {
    top: {},
    left: {
        replayList: [
            '20190108-2053-rl',
            '20190103-1934-vg',
            '20190104-2345-pd'
        ]
    },
    footer: {},
    sidebar: {}
};

export function navigation(state = initialState, action: INavigationAction) {
    switch (action.type) {
        case navigationConstants.UPDATE_SIDEBAR_GRP_TITLE:
        return {
            ...state,
            sidebar: {
                ...state.sidebar,
                title: action.title
            }
        };
        default:
            return state
    }
}