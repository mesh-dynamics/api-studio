import config from '../config';
import { getAccesToken, getRefreshToken } from "../utils/lib/common-utils";
import { store } from "../helpers";
import authActions from '../actions/auth.actions'
import Deferred from './deferred.ts';
import {getApi} from '../api';

const handleResponseLogin = (response) => {
    return response.json().then(json => {
        const data = json;
        if (!response.ok) {
            if (response.status === 401) {
                // auto logout if 401 response returned from api
                logout();
                // XXX: react complains here
                //location.reload(true);
            }

            const error = (data && data.message) || response.statusText;
            return Promise.reject(error);
        }
        localStorage.setItem('user', JSON.stringify(json));
        return (data);
    });
}

const validateCredentials = (username, password) => {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    };

    return fetch(`${config.apiBaseUrl}/login`, requestOptions)
        .then(handleResponseLogin)
}

const logout = () => {
    // remove user from local storage to log user out
    localStorage.removeItem('user');
}

const createUser = (user) => {
    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(user)
    };

    return fetch(`${config.apiBaseUrl}/account/create-user`, requestOptions);
}

const getCaptchaConfig = (domain) => {

    const requestOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    };
    
    return fetch(`${config.apiBaseUrl}/config/get?configType=captcha&domain=${domain}`, requestOptions);
};

const refreshAuthLogic = (failedRequest) => {
    const dataToPost = JSON.stringify({ refreshToken: getRefreshToken(store.getState()), grantType: "refreshToken" });
    window.authRefeshInProgress = true;
    window.authRefreshPromise = new Deferred();
    return new Promise((resolve, reject) => {

        fetch(`${config.apiBaseUrl}/token`, {
            body: dataToPost,
            method: "POST",
            headers: {
                'Content-Type': 'application/json'
            },
            mode: 'cors'
        })
        .then(async (response) => {
            window.authRefeshInProgress = false;
            const data = await response.json();
            if(response.ok && data.status != 401){

                localStorage.setItem('user', JSON.stringify(data));
                store.dispatch(authActions.setUser(data));
                
                if (PLATFORM_ELECTRON) {
                    ipcRenderer.send('set_user', data);
                }
                failedRequest.response.config.headers['Authorization'] = 'Bearer ' + data.access_token;
                window.authRefreshPromise.resolve();
                resolve();
            }else{
                store.dispatch(authActions.logout());
                const error = (data && data.message) || response.statusText;
                window.authRefreshPromise.reject();
                return Promise.reject(error);
            }
        }).catch(error => {
            window.authRefeshInProgress = false;
            store.dispatch(authActions.accessViolationDetected());
            window.authRefreshPromise.reject();
            reject(error);
        });

    });
}

/**
 * 
 * @param {string} token <Recaptcha Token>
 */
const validateReCaptcha = (token) => {
    const requestOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    return fetch(`${config.apiBaseUrl}/account/validate-recaptcha?g-recaptcha-response=${token}`, requestOptions);
}

const verifyActivationToken = (searchString) => {
    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    return fetch(`${config.apiBaseUrl}/account/activate${searchString}`, requestOptions);
}

const resendActivationToken = (email) => {
    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    return fetch(`${config.apiBaseUrl}/account/resend-activation-mail?email=${email}`, requestOptions);
};

const sendResetLink = (email) => {
    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    return fetch(`${config.apiBaseUrl}/account/reset-password/init?email=${email}`, requestOptions);
}

const resetPassword = (key, password) => {

    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ key, password })
    };

    return fetch(`${config.apiBaseUrl}/account/reset-password/finish`, requestOptions);
}

const retryRequest = async (error)=>{
    return  window.authRefreshPromise.promise.then(async()=>{
        error.config.headers['Authorization'] = 'Bearer ' + getAccesToken(store.getState());
        return await getApi().request(error.config);
    });
}

export {
    createUser,
    resetPassword,
    sendResetLink,
    validateReCaptcha,
    validateCredentials,
    resendActivationToken,
    verifyActivationToken,
    getCaptchaConfig,
    refreshAuthLogic,
    retryRequest
};