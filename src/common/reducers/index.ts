import { combineReducers } from 'redux';

import { authentication } from './authentication.reducer';
import { navigation } from './navigation.reducer';
import { golden } from './golden.reducer';
import { cube } from './cube.reducer';
import { apiCatalog } from './api-catalog.reducer';
import { httpClient } from './httpClientReducer';
import { gcbrowse } from './gcbrowse.reducer';

const rootReducer = combineReducers({
  authentication,
  navigation,
  golden,
  cube,
  apiCatalog,
  httpClient,
  gcbrowse
});

export default rootReducer;