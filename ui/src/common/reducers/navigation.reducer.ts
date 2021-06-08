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