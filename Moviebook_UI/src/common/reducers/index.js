import { combineReducers } from 'redux';

import { moviebook } from './moviebook.reducer';

const rootReducer = combineReducers({
    moviebook,
});

export default rootReducer;
