import { createStore, applyMiddleware, compose } from "redux";
import thunk from "redux-thunk";
import { persistStore, persistReducer } from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import { createLogger } from "redux-logger";
import rootReducer from '../reducers';

const configureStore = () => {
    const persistConfig = { key: 'root', storage };

    const persistedReducer = persistReducer(persistConfig, rootReducer);

    const middlewares = [ thunk ];

    if ( process.env.NODE_ENV !== "production" ) {
        middlewares.push( createLogger() );
    }

    const store = createStore(
        persistedReducer, 
        compose(
            applyMiddleware(...middlewares), 
            window.__REDUX_DEVTOOLS_EXTENSION__ 
            && window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__() 
            || compose
        )
    );

    const persistor = persistStore(store);

    return { store, persistor };
};

const { store, persistor } = configureStore();

export { store, persistor };