import React, { Component } from "react";
import PageContent, { Menu } from "../routes";
import { connect } from "react-redux";

import authActions from '../actions/auth.actions'
import Navigation from "./Navigation/Navigation";
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
            <div className="container body">
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
