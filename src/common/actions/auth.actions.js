import { authConstants } from '../constants/auth.constants';
import { 
    validateCredentials, 
    validateReCaptcha, 
    createUser 
} from '../services/auth.service';
import { history } from '../helpers';
import { ipcRenderer } from '../helpers/ipc-renderer';
import { apiCatalogActions } from './api-catalog.actions';
import { cubeActions } from './cube.actions';
import { httpClientActions } from './httpClientActions';
import { goldenActions } from './golden.actions';

const authActions = {
    beginFetch: () => ({ type: authConstants.REQUEST_BEGIN }),

    fetchSuccess: () => ({ type: authConstants.REQUEST_SUCCESS }),

    fetchFailure: (payload) => ({ type: authConstants.REQUEST_FAILURE, payload }),

    setUser: (payload) => ({ type: authConstants.SET_USER, payload }),

    setMessage: (message) => ({ type: authConstants.SET_MESSAGE, message}),

    clearMessage: () => ({ type: authConstants.CLEAR_MESSAGE }),

    clearUser: () => ({ type: authConstants.LOGOUT }),

    accessViolationDetected: () => ({ type: authConstants.ACCESS_VIOLATION }),

    rememberCredentials: (payload) => ({ type: authConstants.REMEMBER_CREDENTIALS, payload }),

    toggleRememberMe: () => ({ type: authConstants.TOGGLE_REMEMBER_ME }),

    forgetCredentials: () => ({ type: authConstants.FORGET_CREDENTIALS }),

    login: (username, password) => async (dispatch) => {
        dispatch(authActions.clearMessage());

        dispatch(authActions.beginFetch());

        try {
            const user = await validateCredentials(username, password);

            dispatch(authActions.setUser(user));
            
            if(PLATFORM_ELECTRON) {
                ipcRenderer.send('set_user', user);
            }
            
            history.push("/")
        } catch(e) {
            dispatch(authActions.fetchFailure(e));
        }

    },

    logout: () => (dispatch) => {
        dispatch(apiCatalogActions.resetApiCatalogToInitialState());
        dispatch(cubeActions.resetCubeToInitialState());
        dispatch(httpClientActions.resetHttpClientToInitialState());
        dispatch(goldenActions.resetGoldenVisibilityDetails());
        dispatch(authActions.clearUser());
        
        if(PLATFORM_ELECTRON){
            const user = {
                access_token: "", 
                customer_name: "", 
                token_type: "", 
                username: ""
            };
            
            ipcRenderer.send('set_user', user);
            // Reset context as well
            ipcRenderer.send('reset_context_to_default');
        }
        
        localStorage.removeItem('user'); // TODO: Remove this after a few release cycles
        
        history.push("/login");
    },
    
    createUser: (user) => createUser(user),

    verifyToken: (token) => validateReCaptcha(token),
};

export default authActions;