import { createStore, applyMiddleware, compose } from "redux";
import thunk from "redux-thunk";
import { persistStore, persistReducer, createTransform } from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import { createLogger } from "redux-logger";
import rootReducer from '../reducers';

const tabDefaultState = {
    //More default values for tab can be added here, if new are added
    grpcConnectionSchema: { app: "", service: "", endpoint:"", method:"" },
}

const configureStore = () => {

    const persistConfig = {
        key: 'root', storage,
        transforms: [
            // Add the required fields default values for reset purpose.
            createTransform((inboundState, key) => {
                if(key == "httpClient"){
                    const tabs = (inboundState.tabs || []).map( tab =>  { 
                            const recordedHistory = tab.recordedHistory ? {...tabDefaultState, ...tab.recordedHistory }: tab.recordedHistory
                            return {...tabDefaultState, 
                            ...tab, 
                            abortRequest: null, requestRunning: false, multipartData: tab.multipartData || [],
                            recordedHistory : recordedHistory }
                        }
                        );
                    return {
                        ...inboundState,
                        tabs: tabs,
                        showMockConfigList: true,
                        mockConfigStatusText : ''
                    }
                } else if (key == "cube") {
                    return {
                        ...inboundState,
                        fetchingReplayStatus: false,
                        fetchingAnalysisStatus: false,
                    }
                }
                return inboundState;
            }, null),
        ],
    };

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