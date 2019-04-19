import React, { Component } from 'react';
import logo from './logo.svg';
import { Left, Top, Footer } from '../navigation';
import PageContent, { Menu } from '../routes';
import { connect } from 'react-redux';
import SideBar, { GroupMenuItem, MenuItem } from './SideBar';

class App extends Component {
    constructor (props) {
        super(props);
        this.state = {
            leftVisible: true
        };
        this.toggleLeft = this.toggleLeft.bind(this);        
    }
    toggleLeft () {
        this.setState({
            // Disabled for now
            //leftVisible: !this.state.leftVisible
            leftVisible: true
        });        
    }
    render() {
        const { user, left } = this.props;
        const { leftVisible } = this.state;
        // In Top, need to remove top_nav class (or make up one more without the margin)
        // In PageContent, remove right_col class (or alternate class)
        return (
            <div className="container body">
                <div className="main_container">
                    <Top user={ user } toggleCb={ this.toggleLeft }/>
                    <PageContent/>
                    <Footer/>
                </div>
            </div>
        );
    }
}
function mapStateToProps(state) {
    const { user } = state.authentication;
    const { left } = state.navigation;
    return {
      user,
      left
    }
  }

const connectedApp = connect(mapStateToProps)(App);

export default connectedApp;
