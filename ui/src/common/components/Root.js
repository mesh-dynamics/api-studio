/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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