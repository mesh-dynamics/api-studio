// Top Navigation
import React, { Component } from 'react'
import TopNavBar from '../components/TopNavBar'
import UserMenuItem from './UserMenuItem'
import NotificationsMenuItem from './NotificationsMenuItem'

class Top extends Component {
  render () {
    const { user, toggleCb, needMargin} = this.props;
    return (
      <div className={ needMargin ? "top_nav" : "" }>
        <div className="nav_menu">
          <nav>
            <div className="nav toggle">
              <a id="menu_toggle" onClick={ toggleCb }><i className="fas fa-bars"></i></a>
            </div>
            <TopNavBar>
              <UserMenuItem user={ user }/>
              <NotificationsMenuItem user={ user }/>
            </TopNavBar>
          </nav>
        </div>
      </div>
    )
  }
}

export default Top