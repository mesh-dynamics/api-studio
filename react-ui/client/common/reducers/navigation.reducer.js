import {navigationConstants} from '../constants';
const initialState = {
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

export function navigation(state = initialState, action) {
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