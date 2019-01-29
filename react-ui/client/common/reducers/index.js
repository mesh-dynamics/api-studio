import { combineReducers } from 'redux';

import { authentication } from './authentication.reducer';
import { navigation } from './navigation.reducer';
import { cube } from './cube.reducer';

const rootReducer = combineReducers({
  authentication,
  navigation,
  cube
});

export default rootReducer;