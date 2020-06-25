import { combineReducers } from 'redux';

import { authentication } from './authentication.reducer';
import { navigation } from './navigation.reducer';
import { golden } from './golden.reducer';
import { cube } from './cube.reducer';
import { apiCatalog } from './api-catalog.reducer';

const rootReducer = combineReducers({
  authentication,
  navigation,
  golden,
  cube,
  apiCatalog,
});

export default rootReducer;