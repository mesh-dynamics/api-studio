import { cubeConstants } from '../constants';

const initialState = {
    left: {
        replayList: [
            '20190108-2053-rl',
            '20190103-1934-vg',
            '20190104-2345-pd'
        ]
    },
    sidebar: {},

    appsListReqStatus: cubeConstants.REQ_NOT_DONE,
    appsListReqErr: '',
    appsList: [],
    selectedApp: '',

    testIdsReqStatus: cubeConstants.REQ_NOT_DONE,
    testIdsReqErr: '',
    testIds: [],
    selectedTestId: ''
};

export function cube (state = initialState, action) {
    switch (action.type) {
        case cubeConstants.APPS_REQUEST: 
            return {
                ...state,
                appsListReqStatus: cubeConstants.REQ_LOADING,
                appsListReqErr: ''
            };
        case cubeConstants.APPS_SUCCESS: 
            return {
                ...state,
                appsListReqStatus: cubeConstants.REQ_SUCCESS,
                appsListReqErr: '',
                appsList: action.data
            };
        case cubeConstants.APPS_FAILURE: 
            return {
                ...state,
                appsListReqStatus: cubeConstants.REQ_FAILURE,
                appsListReqErr: action.err
            };
        case cubeConstants.SET_SELECTED_APP: 
            return {
                ...state,
                selectedApp: action.data
            };
        case cubeConstants.TESTIDS_REQUEST: 
            return {
                ...state,
                testIdsReqStatus: cubeConstants.REQ_LOADING,
                testIdsReqErr: ''
            };
        case cubeConstants.TESTIDS_SUCCESS: 
            return {
                ...state,
                testIdsReqStatus: cubeConstants.REQ_SUCCESS,
                testIdsReqErr: '',
                testIds: action.data
            };
        case cubeConstants.TESTIDS_FAILURE: 
            return {
                ...state,
                testIdsReqStatus: cubeConstants.REQ_FAILURE,
                testIdsReqErr: action.err
            };
        case cubeConstants.SET_SELECTED_TESTID: 
            return {
                ...state,
                selectedTestId: action.data
            };

        default:
            return state
    }
}