import { cubeConstants } from '../constants';

const initialState = {
    appsListReqStatus: cubeConstants.REQ_NOT_DONE,
    appsListReqErr: '',
    appsList: [],
    selectedApp: null,
    selectedAppObj: null,
    gateway: null,
    hideTestConfig: false,
    hideServiceGraph: false,
    hideTestConfigSetup: true,
    hideTestConfigView: true,
    hideGoldenVisibilityView: true,
    newOperationSet:[],
    multiOperationsSet: [],
    operations:[],
    templateOperationSetObject: {},
    ruleBook: {},
    defaultRuleBook: {},
    newTemplateVerInfo: null,

    testConfigList: [],
    testConfig: null,

    instances: [],
    selectedInstance: null,

    testIdsReqStatus: cubeConstants.REQ_NOT_DONE,
    testIdsReqErr: '',
    testIds: [],
    fcId: null,
    selectedTestId: null,
    selectedGolden: null,
    collectionTemplateVersion: null,
    golden: null,
    goldenTimeStamp: null,

    replayId: null,

    graphDataReqStatus: cubeConstants.REQ_NOT_DONE,
    graphDataReqErr: '',
    graphData: null,

    replayStatus:'Fetching Replay ID',  
    replayStatusObj: null,
    analysisStatus: "Not Started",
    analysisStatusObj: null,

    analysis: null,
    report: null,

    timelineData: null,

    diffData: null,

    pathResultsParams: null,

    collectionUpdateOperationSetId: null,
    newGoldenId: null,
    goldenInProg: false,
    jiraBugs: [],
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
        case cubeConstants.TEST_CONFIG_SUCCESS:
            return {
                ...state,
                testConfigList: action.data
            };
        case cubeConstants.NEW_GOLDEN_ADDED:
            return {
                ...state,
                newGoldenId: action.data['ID'],
                goldenInProg: false
            };
        case cubeConstants.GOLDEN_REQUEST:
            return {
                ...state,
                goldenInProg: true
            };
        case cubeConstants.NEED_FORCE_COMPLETE:
            return {
                ...state,
                selectedTestId: null,
                selectedGolden: null,
                collectionTemplateVersion: null,
                fcId: action.data
            };
        case cubeConstants.CLEAR_FORCE_COMPLETE:
            return {
                ...state,
                selectedTestId: null,
                selectedGolden: null,
                collectionTemplateVersion: null,
                fcId: null
            };
        case cubeConstants.SET_TEST_CONFIG:
            return {
                ...state,
                testConfig: action.data
            };
        case cubeConstants.HIDE_TEST_CONFIG:
            return {
                ...state,
                hideTestConfig: action.data,
            };
        case cubeConstants.HIDE_GOLDEN_VISIBILITY:
            return {
                ...state,
                hideGoldenVisibilityView: action.data,
            };
        case cubeConstants.TEST_CONFIG_SETUP:
            return {
                ...state,
                hideTestConfig: action.data,
                hideServiceGraph: action.data,
                hideTestConfigSetup: !action.data,
                hideGoldenVisibilityView: action.data,
            };
        case cubeConstants.TEST_CONFIG_VIEW:
            return {
                ...state,
                hideTestConfig: action.data,
                hideServiceGraph: action.data,
                hideTestConfigView: !action.data,
                hideGoldenVisibilityView: action.data,
            };
        case cubeConstants.GOLDEN_VISIBILITY_VIEW:
            return {
                ...state,
                hideTestConfig: action.data,
                hideServiceGraph: action.data,
                hideTestConfigSetup: action.data,
                hideGoldenVisibilityView: !action.data,
            }
        case cubeConstants.REPLAY_VIEW:
            return {
                ...state,
                hideServiceGraph: action.data,
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
                instances: action.data,
            };
        case cubeConstants.SET_SELECTED_INSTANCE:
            return {
                ...state,
                selectedInstance: action.data
            };
        case cubeConstants.SET_SELECTED_APP:
            let appObj = null;
            if (state.appsList) {
                for (const app of state.appsList) {
                    if (app.name == action.data) {
                        appObj = app;
                        break;
                    }
                }
            }

            return {
                ...state,
                selectedApp: action.data,
                selectedAppObj: appObj
            };
        case cubeConstants.SET_GATEWAY:
            return {
                ...state,
                gateway: action.data
            }
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
                selectedTestId: action.data.collec,
                selectedGolden: action.data.golden,
                collectionTemplateVersion: action.data.ver
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
        
        case cubeConstants.ANALYSIS_STATUS_FETCHED:
            return{
                ...state,
                analysisStatusObj: action.data,
                analysisStatus: action.data.status,
            };
        
        case cubeConstants.ANALYSIS_FETCHED:
            return {
                ...state,
                analysis: action.data
            };
        case cubeConstants.INIT_ANALYSIS:
            return {
                ...state,
                analysis: null
            }
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
        case cubeConstants.CLEAR_REPLAY_STATUS:
            return {
                ...state,
                replayStatusObj: null,
                analysisStatusObj: null,
            };
        case cubeConstants.CLEAR_PREVIOUS_DATA:
            return {
                ...state,

                replayId: null,

                graphDataReqStatus: cubeConstants.REQ_NOT_DONE,
                graphDataReqErr: '',
                gateway: null,

                replayStatus: 'Fetching Replay ID',
                replayStatusObj: null,
                analysisStatusObj: null,
                analysisStatus: "Not Started",

                analysis: null,
                report: null,
                newGoldenId: null,
                newOperationSet:[],
                operations:[],
                templateOperationSetObject: {},
                ruleBook: {},
                multiOperationsSet: []
            };
        case cubeConstants.DIFF_SUCCESS:
            return {
                ...state,
                diffData: action.data
            };
        case cubeConstants.SET_PATH_RESULTS_PARAMS:
            return {
                ...state,
                pathResultsParams: {
                    ...state.pathResultsParams,
                    ...action.data
                }
            };
        case cubeConstants.CLEAR_PATH_RESULTS_PARAMS:
            return {
                ...state,
                pathResultsParams: null,
            };
        case cubeConstants.ADD_TO_RULE_BOOK:
            return {
                ...state,
                ruleBook: {
                    ...state.ruleBook,
                    [action.data.key]: action.data.val
                }
            };
        case cubeConstants.ADD_TO_DEFAULT_RULE_BOOK:
            return {
                ...state,
                defaultRuleBook: {
                    ...state.defaultRuleBook,
                    [action.data.key]: action.data.val
                },
            };
        case cubeConstants.REMOVE_FROM_RULE_BOOK:
            let ruleBDel = state.ruleBook;
            let defRuleBDel =  state.defaultRuleBook;
            delete ruleBDel[action.data];
            delete defRuleBDel[action.data];

            return {
                ...state,
                ruleBook: ruleBDel,
                defaultRuleBook: defRuleBDel
            };
        case cubeConstants.PUSH_TO_OS:
            let mos = state.multiOperationsSet;
            mos[action.data.ind].operationSet.push(action.data.os);
            return {
                ...state,
                multiOperationsSet: mos
            };
        case cubeConstants.PUSH_TO_MOS:
            return {
                ...state,
                multiOperationsSet: state.multiOperationsSet.concat(action.data)
            };
        case cubeConstants.PUSH_TO_OPERATIONS:
            let tempp = state.templateOperationSetObject;
            tempp[action.data.key].operations.push(action.data.op);
            return {
                ...state,
                templateOperationSetObject: tempp
            };
        case cubeConstants.NEW_KEY_PUSH_TO_OPERATIONS:
            let temp = state.templateOperationSetObject;
            temp[action.data.key] = {operations: [action.data.op]};
            return {
                ...state,
                templateOperationSetObject: temp
            };
        case cubeConstants.REMOVE_FROM_OPERATION_GOLDEN:
            state.multiOperationsSet[action.data.indexMOS].operationSet.splice(action.data.index, 1);
            return {
                ...state,
                multiOperationsSet: state.multiOperationsSet
            };
        case cubeConstants.REMOVE_ENTIRE_OPERATIONS_GOLDEN_OBJ:
            state.multiOperationsSet.splice(action.data.indexMOS, 1);
            return {
                ...state,
                multiOperationsSet: state.multiOperationsSet
            };
        case cubeConstants.REMOVE_FROM_OPERATIONS:
            state.templateOperationSetObject[action.data.key].operations.splice(action.data.index, 1);
            return {
                ...state,
                templateOperationSetObject: state.templateOperationSetObject
            };
        case cubeConstants.REMOVE_ENTIRE_OPERATIONS_OBJ:
            delete state.templateOperationSetObject[action.data.key];
            return {
                ...state,
                templateOperationSetObject: state.templateOperationSetObject
            };
        case cubeConstants.TEMPLATE_VER_SUCCESS:
            return {
                ...state,
                newTemplateVerInfo: action.data
            };
        case cubeConstants.COLLECTION_UOS_SUCCESS:
            return {
                ...state,
                collectionUpdateOperationSetId: action.data,
                newOperationSet:[]
            };
        case cubeConstants.UPDATE_TOS_SUCCESS:
            return {
                ...state,
                operations:[]
            };
        case cubeConstants.SET_GOLDEN:
            let golden = action.data.golden;
            for (const gold of state.testIds){
                if (gold.id == golden) {
                    golden = gold.name;
                    break;
                }
            }
            return {
                ...state,
                golden: golden,
                goldenTimeStamp: action.data.timeStamp,
            };
        case cubeConstants.CLEAR_GOLDEN:
            return {
                ...state,
                golden: null,
                goldenTimeStamp: null,
                newGoldenId: null,
                goldenInProg: false
            };
        case cubeConstants.SET_JIRA_BUGS:
            return {
                ...state,
                jiraBugs: action.data,
            };
        default:
            return state
    }
}
