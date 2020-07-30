import { cubeService } from "../services";
import { httpClientConstants } from "../constants/httpClientConstants";
import _ from "lodash";


export const httpClientActions = {
    deleteParamInSelectedOutgoingTab: (tabId, type, id) => {
        return {type: httpClientConstants.DELETE_PARAM_IN_OUTGOING_TAB, data: {tabId, type, id}};
    },

    addParamToSelectedOutgoingTab: (tabId, type) => {
        return {type: httpClientConstants.ADD_PARAM_TO_OUTGOING_TAB, data: {tabId, type}};
    },

    deleteParamInSelectedTab: (tabId, type, id) => {
        return {type: httpClientConstants.DELETE_PARAM_IN_TAB, data: {tabId, type, id}};
    },

    addParamToSelectedTab: (tabId, type) => {
        return {type: httpClientConstants.ADD_PARAM_TO_TAB, data: {tabId, type}};
    },

    updateParamInSelectedOutgoingTab: (tabId, type, key, value, id) => {
        return {type: httpClientConstants.UPDATE_PARAM_IN_OUTGOING_TAB, data: {tabId, type, key, value, id}};
    },

    updateParamInSelectedTab: (tabId, type, key, value, id) => {
        return {type: httpClientConstants.UPDATE_PARAM_IN_TAB, data: {tabId, type, key, value, id}};
    },

    updateBodyOrRawDataTypeInOutgoingTab: (tabId, type, value) => {
        return {type: httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_OUTGOING_TAB, data: {tabId, type, value}};
    },

    updateBodyOrRawDataTypeInTab: (tabId, type, value) => {
        return {type: httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_TAB, data: {tabId, type, value}};
    },

    preDriveRequest: (tabId, responseStatus, showCompleteDiff) => {
        return {type: httpClientConstants.PRE_DRIVE_REQUEST, data: {tabId, responseStatus, showCompleteDiff}}; 
    },

    postSuccessDriveRequest: (tabId, responseStatus, responseStatusText, responseHeaders, responseBody) => {
        return {type: httpClientConstants.POST_SUCCESS_DRIVE_REQUEST, data: {tabId, responseStatus, responseStatusText, responseHeaders, responseBody}}; 
    },

    postErrorDriveRequest: (tabId, responseStatus) => {
        return {type: httpClientConstants.POST_ERROR_DRIVE_REQUEST, data: {tabId, responseStatus}}; 
    },

    addTab: (tabId, reqObject, app, selectedTabKey, tabName) => {
        return {type: httpClientConstants.ADD_TAB, data: {tabId, reqObject, app, selectedTabKey, tabName}}; 
    },

    addOutgoingRequestsToTab: (tabId, outgoingRequests) => {
        return {type: httpClientConstants.ADD_OUTGOING_REQUESTS_TO_TAB, data: {tabId, outgoingRequests}};
    },

    addUserHistoryCollection: (userHistoryCollection) => {
        return {type: httpClientConstants.ADD_USER_HISTORY_COLLECTION, data: {userHistoryCollection}};
    },

    addCubeRunHistory: (apiTraces, cubeRunHistory) => {
        return {type: httpClientConstants.ADD_CUBE_RUN_HISTORY, data: {apiTraces, cubeRunHistory}};
    },

    addUserCollections: (userCollections) => {
        return {type: httpClientConstants.ADD_USER_COLLECTIONS, data: {userCollections}};
    },

    postSuccessSaveToCollection: (tabId, showSaveModal, modalErroSaveMessage, clearIntervalHandle) => {
        return {type: httpClientConstants.POST_SUCCESS_SAVE_TO_COLLECTION, data: {tabId, showSaveModal, modalErroSaveMessage, clearIntervalHandle}};
    },

    postErrorSaveToCollection: (showSaveModal, modalErroSaveMessage) => {
        return {type: httpClientConstants.POST_ERROR_SAVE_TO_COLLECTION, data: {showSaveModal, modalErroSaveMessage}};
    },

    catchErrorSaveToCollection: (showSaveModal, modalErroSaveMessage) => {
        return {type: httpClientConstants.CATCH_ERROR_SAVE_TO_COLLECTION, data: {showSaveModal, modalErroSaveMessage}};
    },

    postSuccessLoadRecordedHistory: (tabId, recordedHistory) => {
        return {type: httpClientConstants.POST_SUCCESS_LOAD_RECORDED_HISTORY, data: {tabId, recordedHistory}};
    },

    closeSaveModal: (showSaveModal) => {
        return {type: httpClientConstants.CLOSE_SAVE_MODAL, data: {showSaveModal}};
    },

    showSaveModal: (selectedSaveableTabId, showSaveModal, collectionName, collectionLabel, modalErroSaveMessage, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.SHOW_SAVE_MODAL, data: {selectedSaveableTabId, showSaveModal, collectionName, collectionLabel, modalErroSaveMessage, modalErroCreateCollectionMessage}};
    },

    setInactiveHistoryCursor: (historyCursor, active) => {
        return {type: httpClientConstants.SET_INACTIVE_HISTORY_CURSOR, data: {historyCursor, active}};
    },

    setActiveHistoryCursor: (historyCursor) => {
        return {type: httpClientConstants.SET_ACTIVE_HISTORY_CURSOR, data: {historyCursor}};
    },

    postSuccessCreateCollection: (showSaveModal, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.POST_SUCCESS_CREATE_COLLECTION, data: {showSaveModal, modalErroCreateCollectionMessage}};
    },

    postErrorCreateCollection: (showSaveModal, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.POST_ERROR_CREATE_COLLECTION, data: {showSaveModal, modalErroCreateCollectionMessage}};
    },

    catchErrorCreateCollection: (showSaveModal, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.CATCH_ERROR_CREATE_COLLECTION, data: {showSaveModal, modalErroCreateCollectionMessage}};
    },

    setSelectedTabKey: (selectedTabKey) => {
        return {type: httpClientConstants.SET_SELECTED_TAB_KEY, data: {selectedTabKey}};
    },

    removeTab: (tabs, selectedTabKey) => {
        return {type: httpClientConstants.REMOVE_TAB, data: {tabs, selectedTabKey}};
    },

    setUpdatedModalUserCollectionDetails: (name, value) => {
        return {type: httpClientConstants.SET_UPDATED_MODAL_USER_COLLECTION_DETAILS, data: {name, value}};
    },
}