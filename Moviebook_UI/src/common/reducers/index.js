import { combineReducers } from "redux";

import { moviebook } from "./moviebook.reducer";
import authentication from "./auth.reducer";

const rootReducer = combineReducers({
  moviebook,
  authentication,
});

export default rootReducer;
