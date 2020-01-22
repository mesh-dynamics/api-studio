import config from '../config';

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
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    };
    
    return fetch(`${config.apiBaseUrl}/account/activate${searchString}`, requestOptions);
}

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

export {
    createUser,
    resetPassword,
    sendResetLink,
    validateReCaptcha,
    validateCredentials,
    verifyActivationToken,
};