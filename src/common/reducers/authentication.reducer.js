import { authConstants } from '../constants';
import { ipcRenderer } from '../helpers/ipc-renderer';

const initialState = {
  user: {},
  role: "",
  loggedIn: false,
  isFetching: false,
  messages: [],
  accessViolation: false,
};

const authenticationReducer = (state = initialState, action) => {
  switch (action.type) {
  case authConstants.REQUEST_BEGIN:
    return { 
      ...state, 
      isFetching: true,
      messages: []
    };
  case authConstants.REQUEST_SUCCESS:
    return {
      ...state,
      isFetching: false,
      loggedIn: true,
      user: action.payload.user
    };
  case authConstants.REQUEST_FAILURE:
    return {
      ...state,
      isFetching: false,
      loggedIn: false,
      user: "",
      messages: [action.payload] 
      };
  case authConstants.SET_USER:
    return {
      ...state,
      user: {...action.payload},
      loggedIn: true,
    };
  case authConstants.LOGOUT:
    return {
      ...state,
      loggedIn: false,
      accessViolation: false,
      user: {},
    };
  case authConstants.SET_MESSAGE:
    return {
      ...state,
      messages: [action.payload]
    };
  case authConstants.CLEAR_MESSAGE:
    return {
      ...state,
      messages: []
    };
  case authConstants.ACCESS_VIOLATION:
    return {
      ...state,
      accessViolation: true
    };
  default:
    return state
  }
}

export { authenticationReducer as authentication };