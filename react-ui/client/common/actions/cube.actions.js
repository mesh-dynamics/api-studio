import { cubeConstants } from '../constants';
import { cubeService } from '../services'

export const cubeActions = {
    getApps,
    setSelectedApp,
    getTestIds,
    setSelectedTestId
};

function getApps () {
    return async dispatch => {
        dispatch(request());
        try {
            let appsList = await cubeService.fetchAppsList();
            dispatch(success(appsList, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getApps", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.APPS_REQUEST } }
    function success(appsList, date) { return { type: cubeConstants.APPS_SUCCESS, data: appsList, date: date } }
    function failure(message, date) { return { type: cubeConstants.APPS_FAILURE, err: message, date: date } }
}

function setSelectedApp ( appLabel ) {
    return {type: cubeConstants.SET_SELECTED_APP, data: appLabel}
}

function getTestIds ( app ) {
    return async dispatch => {
        dispatch(request());
        try {
            let options = {
                app: app
            }
            let testIds = await cubeService.getTestIds( options );
            dispatch(success(testIds.ids, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getTestIds", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.TESTIDS_REQUEST } }
    function success(testIds, date) { return { type: cubeConstants.TESTIDS_SUCCESS, data: testIds, date: date } }
    function failure(message, date) { return { type: cubeConstants.TESTIDS_FAILURE, err: message, date: date } }
}

function setSelectedTestId ( testIdLabel ) {
    return {type: cubeConstants.SET_SELECTED_TESTID, data: testIdLabel}
}
