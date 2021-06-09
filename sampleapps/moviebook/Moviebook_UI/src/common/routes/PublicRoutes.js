import React, { Component } from "react";
import { Route, Switch } from "react-router";
import { LoginPage } from "./Auth";

class RouterComponent extends Component {
  render() {
    return (
      <Switch>
        <Route exact key="Login" path="/login" component={LoginPage} />
        <Route key="Login" path="/*" component={LoginPage} />
      </Switch>
    );
  }
}

export default RouterComponent;

//   <div style={{ margin: "0 50px", marginTop: "56px" }}>
//   </div>
