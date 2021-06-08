import { authConstants } from "../constants/auth.constants";
import { validateCredentials } from "../services/auth.service";
import { history } from "../helpers";

const authActions = {
	beginFetch: () => ({ type: authConstants.REQUEST_BEGIN }),

	fetchSuccess: () => ({ type: authConstants.REQUEST_SUCCESS }),

	fetchFailure: (payload) => ({
		type: authConstants.REQUEST_FAILURE,
		payload,
	}),

	setUser: (payload) => ({ type: authConstants.SET_USER, payload }),

	setMessage: (message) => ({ type: authConstants.SET_MESSAGE, message }),

	clearMessage: () => ({ type: authConstants.CLEAR_MESSAGE }),

	clearUser: () => ({ type: authConstants.LOGOUT }),

	accessViolationDetected: () => ({ type: authConstants.ACCESS_VIOLATION }),

	rememberCredentials: (payload) => ({
		type: authConstants.REMEMBER_CREDENTIALS,
		payload,
	}),

	toggleRememberMe: () => ({ type: authConstants.TOGGLE_REMEMBER_ME }),

	forgetCredentials: () => ({ type: authConstants.FORGET_CREDENTIALS }),

	login: (username, password) => async (dispatch) => {
		dispatch(authActions.clearMessage());

		dispatch(authActions.beginFetch());

		try {
			const response = await validateCredentials(username, password);

			dispatch(authActions.setUser({ username, password, ...response }));

			history.push("/");
		} catch (e) {
			dispatch(
				authActions.fetchFailure("Incorrect Username or Password")
			);
		}
	},

	logout: () => (dispatch) => {
		dispatch(authActions.clearUser());

		history.push("/login");
	},
};

export default authActions;
