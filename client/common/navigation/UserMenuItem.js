import React, { Component } from 'react'
import { Item } from '../components/TopNavBar'

class UserMenuItem extends Component {
  render() {
    const { user, lo } = this.props;
    return (
      <Item {...this.props}>
        <Item.Content className="user-profile">
          <img src={user.userphoto} alt=""/>{user.user}
          <span className=" fa fa-angle-down"></span>
        </Item.Content>
        <Item.SubMenu className="dropdown-usermenu pull-right">
          <li><a href="#!"> Profile</a></li>
          <li>
            <a href="#!">
              <span className="badge bg-red pull-right">50%</span>
              <span>Settings</span>
            </a>
          </li>
          <li><a href="#!">Help</a></li>
          <li><a onClick={lo}><i className="fa fa-sign-out pull-right"></i> Log Out</a></li>
        </Item.SubMenu>
      </Item>
    )
  }
}

export default UserMenuItem
