/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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