import { authHeader } from '../helpers';
import config from '../config';
import {initialize} from 'redux-form'

export const userService = {
    login,
    logout,
    register,
    getAll,
    getById,
    update,
    createUser,
    sendResetLink,
    resetPassword,
    validateReCaptcha,
    verifyActivationToken,
    delete: _delete,
};


function login(username, password) {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    };
    console.log(requestOptions);

    return fetch(`${config.apiBaseUrl}/login`, requestOptions)
        .then(handleResponseLogin)
        /*.then(user => {
            // login successful if there's a jwt token in the response
            if (user.token) {
                // store user details and jwt token in local storage to keep user logged in between page refreshes
                localStorage.setItem('user', JSON.stringify(user));
            }

            return user;
        });*/
}

function logout() {
    // remove user from local storage to log user out
    localStorage.removeItem('user');
}

function getAll() {
    const requestOptions = {
        method: 'GET',
        headers: authHeader()
    };

    return fetch(`${config.apiBaseUrl}/users`, requestOptions).then(handleResponse);
}

function getById(id) {
    const requestOptions = {
        method: 'GET',
        headers: authHeader()
    };

    return fetch(`${config.apiBaseUrl}/users/${id}`, requestOptions).then(handleResponse);
}

function register(user) {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(user)
    };

    return fetch(`${config.apiBaseUrl}/users/register`, requestOptions).then(handleResponse);
}

function update(user) {
    const requestOptions = {
        method: 'PUT',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify(user)
    };

    return fetch(`${config.apiBaseUrl}/users/${user.id}`, requestOptions).then(handleResponse);;
}

// prefixed function name with underscore because delete is a reserved word in javascript
function _delete(id) {
    const requestOptions = {
        method: 'DELETE',
        headers: authHeader()
    };

    return fetch(`${config.apiBaseUrl}/users/${id}`, requestOptions).then(handleResponse);
}

function handleResponse(response) {
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
        return data;
    });
}

function handleResponseLogin(response) {
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

/**
 * TODO: Remove before merge
 */
const success = "http://www.mocky.io/v2/5e1ee6a4310000360018957d";
const failure = "http://www.mocky.io/v2/5e1ebd30310000780018941d";
// reset-password/init
// reset-password/finish
// return fetch(success, requestOptions);

function createUser(user){
    const requestOptions = {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(user)
    };

    return fetch(`${config.apiBaseUrl}/account/create-user`, requestOptions);
}

/**
 * 
 * @param {string} token <Recaptcha Token>
 */
function validateReCaptcha(token){
    const requestOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    return fetch(`${config.apiBaseUrl}/account/validate-recaptcha?g-recaptcha-response=${token}`, requestOptions);
}

function verifyActivationToken(searchString){
    const requestOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    return fetch(`${config.apiBaseUrl}/account/activate${searchString}`, requestOptions);
}

function sendResetLink(email){
    const requestOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    return fetch(`${config.apiBaseUrl}/account/reset-password/init?email=${email}`, requestOptions);
}

function resetPassword(key, password){

    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ key, password })
    };

    return fetch(`${config.apiBaseUrl}/account/reset-password/finish`, requestOptions);
}