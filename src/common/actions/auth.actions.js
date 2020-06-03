import { authConstants } from '../constants/auth.constants';
import { 
    validateCredentials, 
    validateReCaptcha, 
    createUser 
} from '../services/auth.service';
import { history } from '../helpers';

const authActions = {
    beginFetch: () => ({ type: authConstants.REQUEST_BEGIN }),

    fetchSuccess: () => ({ type: authConstants.REQUEST_SUCCESS }),

    fetchFailure: (payload) => ({ type: authConstants.REQUEST_FAILURE, payload }),

    setUser: (payload) => ({ type: authConstants.SET_USER, payload }),

    setMessage: (message) => ({ type: authConstants.SET_MESSAGE, message}),

    clearMessage: () => ({ type: authConstants.CLEAR_MESSAGE }),

    clearUser: () => ({ type: authConstants.LOGOUT }),

    accessViolationDetected: () => ({ type: authConstants.ACCESS_VIOLATION }),

    login: (username, password) => async (dispatch) => {
        dispatch(authActions.clearMessage());

        dispatch(authActions.beginFetch());

        try {
            const user = await validateCredentials(username, password);

            dispatch(authActions.setUser(user));

            history.push("/")
        } catch(e) {
            dispatch(authActions.fetchFailure("Invalid Username or Password"));
        }

    },

    logout: () => (dispatch) => {
        dispatch(authActions.clearUser());
        
        localStorage.removeItem('user');
        
        history.push("/login");
    },
    
    createUser: (user) => createUser(user),

    verifyToken: (token) => validateReCaptcha(token),
};

export default authActions;