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

import React, { Component } from "react";
import PageContent, { Menu } from "../routes";
import { connect } from "react-redux";

import authActions from '../actions/auth.actions'
import Navigation from "./Navigation/Navigation.tsx";
import AuthTimer from "./Authentication/AuthTimer";
class App extends Component {
    constructor (props) {
        super(props);
        this.state = {
            leftVisible: true
        };
    }
    
    toggleLeft = () => {
        this.setState({
            // Disabled for now
            //leftVisible: !this.state.leftVisible
            leftVisible: true
        });        
    }
    
    handleLogout = () => {
        const { dispatch } = this.props;
        dispatch(authActions.logout());
    }

    render() {
        const { user, left } = this.props;
        const { leftVisible } = this.state;
        // In Top, need to remove top_nav class (or make up one more without the margin)
        // In PageContent, remove right_col class (or alternate class)
        return (
            <div className="body">
                <div className="main_container">
                    <AuthTimer />
                    <Navigation lo={this.handleLogout} user={ user } toggleCb={ this.toggleLeft }/>
                    <PageContent/>
                </div>
            </div>
        );
    }
}

const mapStateToProps = (state) => ({
    user: state.authentication.user,
    left: state.navigation.left
});

const connectedApp = connect(mapStateToProps)(App);

export default connectedApp;
