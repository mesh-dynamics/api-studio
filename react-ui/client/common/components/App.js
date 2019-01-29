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
                    <Left user={ user } visible={ leftVisible }>
                        { /* Menu */ }
                        <SideBar>
                            <SideBar.MenuSection title=" ">
                                <MenuItem title="Test Config" to="/config" icon="home"/>
                                <GroupMenuItem title="Replay" to="/replay">
                                    <MenuItem title="Replay point" to="/replay/point" />
                                    <MenuItem title="Virtualization points" to="/replay/virtualPoints" />
                                    <MenuItem title="Profile" to="/replay/profile" />
                                    <MenuItem title="Replay" to="/replay/replay" />
                                </GroupMenuItem>
                                <GroupMenuItem title="Results" to="/results">
                                    { left.replayList.map((r, idx) => {
                                        return <MenuItem key= {idx} title={r} to={'/' + r} />
                                    })}
                                </GroupMenuItem>
                            </SideBar.MenuSection>
                        </SideBar>
                    </Left>
                    <Top user={ user } toggleCb={ this.toggleLeft } needMargin={ leftVisible }/>
                    <PageContent needMargin={ leftVisible } />
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
