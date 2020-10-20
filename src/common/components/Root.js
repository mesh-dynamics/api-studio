import React from "react";
import { Router, Route, Switch } from "react-router-dom";
import App from "./App";
import { LoginPage } from "./Authentication/LoginPage";
import ActivationPage from "./Authentication/Activation";
import { PrivateRoute } from "./PrivateRoute";
import { history } from '../helpers';


const Root = () => (
    <Router history={history}>
        <div>
            <Switch>
                <Route path="/login" component={LoginPage} />
                <Route path="/activate" component={ActivationPage} />
                <PrivateRoute path="/*" component={App} />
                <Route path="/*" component={App} />
            </Switch>
        </div>
    </Router>
);

export default Root;