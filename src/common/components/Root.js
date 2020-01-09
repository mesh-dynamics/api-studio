import React from "react";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import PropTypes from "prop-types";
import { Provider } from "react-redux";
import App from "./App";
import { LoginPage } from "./Authentication/LoginPage";
import { PrivateRoute } from "./PrivateRoute";
import { history } from '../helpers';


const Root = ( { store } ) => (
    <Provider store={store}>
        <Router history={history}>
            <div>
                <Switch>
                    <Route path="/sign_in" component={LoginPage} />
                    <PrivateRoute path="/*" component={App} />
                    <Route path="/*" component={App} />
                </Switch>
            </div>
        </Router>
    </Provider>
);
Root.propTypes = {
    store: PropTypes.oneOfType( [
        PropTypes.func.isRequired,
        PropTypes.object.isRequired,
    ] ).isRequired,
};


export default Root;
