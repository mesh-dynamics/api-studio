import React from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import "./App.css";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import PropTypes from "prop-types";
import { Provider } from "react-redux";
import { history } from "./common/helpers";
import { RouterComponent } from "./common/routes";
import { PersistGate } from "redux-persist/integration/react";
import { store, persistor } from "./common/Store";

const App = () => (
	<Provider store={store}>
		<PersistGate loading={null} persistor={persistor}>
			<Router history={history}>
				<div className="App">
					<RouterComponent />
				</div>
			</Router>
		</PersistGate>
	</Provider>
);

App.propTypes = {
	store: PropTypes.oneOfType([
		PropTypes.func.isRequired,
		PropTypes.object.isRequired,
	]).isRequired,
};

export default App;
