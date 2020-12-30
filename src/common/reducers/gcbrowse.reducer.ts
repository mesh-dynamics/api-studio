import { stat } from 'fs';
import { gcbrowseConstants } from '../constants';
import { IGoldenCollectionBrowseState } from './state.types';

interface IGCBrowseAction {
    type: string;
    payload: any;
}

const initialState: IGoldenCollectionBrowseState = {
    currentCollectionType: '',
    selectedCollectionItem: {
        app: '',
        archived: false,
        branch: '',
        codeVersion: '',
        collec: '',
        collectionUpdOpSetId: '',
        comment: '',
        cust: '',
        dynamicInjectionConfigVersion: '',
        gitCommitId: '',
        id: '',
        instance: '',
        jarPath: '',
        label: '',
        name: '',
        prntRcrdngId: '',
        recordingType: '',
        rootRcrdngId: '',
        runId: '',
        status: '',
        tags: [],
        templateUpdOpSetId: '',
        templateVer: '',
        timestmp: 0,
        userId: '',
    },
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
                userGoldens: action.payload
            }
        }
        case gcbrowseConstants.UPDATE_SELECTED_ITEM: {
            return {
                ...state,
                selectedCollectionItem: action.payload
            }
        }
        case gcbrowseConstants.CLEAR_SELECTED_ITEM: {
            return {
                ...state,
                selectedCollectionItem: initialState.selectedCollectionItem
            }
        }
        default:
            return state;
    }
}

export {
    IGCBrowseAction,
    gcbrowseReducer as gcbrowse
}