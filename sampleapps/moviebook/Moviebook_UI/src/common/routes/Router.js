import React from "react";
import { Router, Route, Switch } from "react-router-dom";
import PublicRoute from "./PublicRoutes";
import PrivateRouter from "./PrivateRouter";
import InternalRoutes from "./InternalRoutes";
import { history } from "../helpers";

const RouterComponent = () => (
  <Router history={history}>
    <div>
      <Switch>
        <Route path="/login" component={PublicRoute} />
        <PrivateRouter path="/*" component={InternalRoutes} />
        <Route path="/*" component={InternalRoutes} />
      </Switch>
    </div>
  </Router>
);

export default RouterComponent;
