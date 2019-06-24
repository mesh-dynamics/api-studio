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
    selectedApp: null,

    instances: [],
    selectedInstance: null,

    testIdsReqStatus: cubeConstants.REQ_NOT_DONE,
    testIdsReqErr: '',
    testIds: [],
    selectedTestId: '',

    replayId: null,

    graphDataReqStatus: cubeConstants.REQ_NOT_DONE,
    graphDataReqErr: '',
    graphData: null,

    replayStatus:'Not Initialized',
    replayStatusObj: null,

    analysis: null,
    report: null,

    timelineData: null,

    diffData: null,
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
        case cubeConstants.INSTANCE_SUCCESS:
            return {
                ...state,
                //appsListReqStatus: cubeConstants.REQ_SUCCESS,
                //appsListReqErr: '',
                instances: action.data
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
        case cubeConstants.REPLAY_ID_SUCCESS:
            return {
                ...state,
                replayId: action.data
            };
        case cubeConstants.GRAPH_REQUEST:
            return {
                ...state,
                graphDataReqStatus: cubeConstants.REQ_LOADING,
                graphDataReqErr: ''
            };
        case cubeConstants.GRAPH_REQUEST_FAILURE:
            return {
                ...state,
                graphDataReqStatus: cubeConstants.REQ_FAILURE,
                graphDataReqErr: action.err
            };
        case cubeConstants.GRAPH_REQUEST_SUCCESS:
            return {
                ...state,
                graphDataReqStatus: cubeConstants.REQ_SUCCESS,
                graphDataReqErr: '',
                graphData: action.data
            };
        case cubeConstants.REPLAY_STARTED:
            return{
                ...state,
                replayStatus: 'Initializing'
            };
        case cubeConstants.REPLAY_STATUS_FETCHED:
            return{
                ...state,
                replayStatusObj: action.data,
                replayStatus: action.data.status,
            };
        case cubeConstants.ANALYSIS_FETCHED:
            return {
                ...state,
                analysis: action.data
            };
        case cubeConstants.REPORT_FETCHED:
            return {
                ...state,
                report: action.data
            };
        case cubeConstants.TIMELINE_DATA_SUCCESS:
            return {
                ...state,
                timelineData: action.data
            };
        case cubeConstants.CLEAR_PREVIOUS_DATA:
            return {
                ...state,
                selectedTestId: '',

                replayId: null,

                graphDataReqStatus: cubeConstants.REQ_NOT_DONE,
                graphDataReqErr: '',

                replayStatus: 'Not Initialized',
                replayStatusObj: null,

                analysis: null,
                report: null,
                timelineData: null,
            };
        case cubeConstants.DIFF_SUCCESS:
            return {
                ...state,
                diffData: action.data
            };

        default:
            return state
    }
}
