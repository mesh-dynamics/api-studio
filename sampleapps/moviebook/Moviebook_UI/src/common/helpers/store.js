import { createStore, compose, applyMiddleware } from "redux";
import thunk from "redux-thunk";
import { createLogger } from "redux-logger";
import rootReducer from '../reducers';

const configureStore = () => {
    const middlewares = [ thunk ];


    if ( process.env.NODE_ENV !== "production" ) {
        middlewares.push( createLogger() );
    }

    const composeEnhancer = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose;


    /* eslint-disable no-underscore-dangle */
    return createStore(
        rootReducer,
        composeEnhancer(applyMiddleware( ...middlewares )),
    );
    /* eslint-enable */
};

export const store = configureStore();
