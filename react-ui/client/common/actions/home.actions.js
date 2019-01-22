import { homeConstants } from '../constants';
import { homeService } from '../services';

export const homeActions = {
    getNeInfo,
    getServerInfo,
    destroyInfo
}

function getNeInfo (username) {
    return async dispatch => {
        dispatch(request());
        try {
            let neInfo = await homeService.truestreamDeviceInfo(username);
            dispatch(success(neInfo));
        } catch (error) {
            dispatch(failure("Login incorrect: username or password is wrong"));
        }
    };

    function request() { return { type: homeConstants.NE_INFO_REQUEST } }
    function success(neInfo) { return { type: homeConstants.NE_INFO_SUCCESS, data: neInfo } }
    function failure(message) { return { type: homeConstants.NE_INFO_FAILURE, message } }
}

function getServerInfo (username) {
    return async dispatch => {
        dispatch(request());
        try {
            let serverInfo = await homeService.serverInfo(username);
            dispatch(success(serverInfo));
        } catch (error) {
            dispatch(failure("Login incorrect: username or password is wrong"));
        }
    };

    function request() { return { type: homeConstants.SVR_INFO_REQUEST } }
    function success(serverInfo) { return { type: homeConstants.SVR_INFO_SUCCESS, data: serverInfo } }
    function failure(message) { return { type: homeConstants.SVR_INFO_FAILURE, message } }
}

function destroyInfo(username) {
    return async dispatch => {
        dispatch(destroy());
    }
    function destroy() {return {type: homeConstants.DESTROY_INFO} }
}