import { combineReducers } from 'redux';

import { authentication } from './authentication.reducer';
import { navigation } from './navigation.reducer';
import { golden } from './golden.reducer';
import { cube } from './cube.reducer';

const rootReducer = combineReducers({
  authentication,
  navigation,
  golden,
  cube
});

export default rootReducer;