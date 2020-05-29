import { createStore, applyMiddleware, compose } from "redux";
import thunk from "redux-thunk";
import { createLogger } from "redux-logger";
import rootReducer from '../reducers';
const configureStore = () => {
    const middlewares = [ thunk ];
    if ( process.env.NODE_ENV !== "production" ) {
        middlewares.push( createLogger() );
    }
    /* eslint-disable no-underscore-dangle */
    // return createStore(
    //     rootReducer,
    //     window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__(),
    //     applyMiddleware( ...middlewares ),
    // );
    return createStore(
      rootReducer, 
      compose(applyMiddleware(...middlewares), window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__()))
    /* eslint-enable */
};
export const store = configureStore();