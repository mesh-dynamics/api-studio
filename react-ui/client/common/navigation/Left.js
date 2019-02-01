import React, { Component } from 'react'
import { Button, Dropdown, DropdownButton, MenuItem } from 'react-bootstrap'
// import { Link } from 'react-router-dom'

class Left extends Component {
  constructor (props) {
    super(props);
    this.state = {
        btnTitle: 'Test Application'
    };
    this.handleChange = this.handleChange.bind(this);        
  }
  handleChange (eventKey, event) {
      let val = `${eventKey}`;
      this.setState({
          btnTitle: val
      });        
  }

  render() {
    const { user, visible } = this.props;
    if (!visible) 
      return (<div></div>);
    return (
      <div className="col-md-3 left_col">
        <div className="left_col scroll-view">
          <div className="navbar nav_title" style={ { border: 0 } }>
            <a href="index.html" className="site_title"><img src="/assets/images/cubeLogo.png" alt="cube.io"/><span> Cube.io</span></a>
          </div>

          <div className="clearfix"></div>

          { /* menu profile quick info */ }
          <div className="profile clearfix">
            <div className="profile_pic">
              <img src={user.userphoto} alt="..." className="img-circle profile_img"/>
            </div>
            <div className="profile_info">
              <span>Welcome,</span>
              <h2>{user.user}</h2>
            </div>
          </div>
          { /* /menu profile quick info */ }

          <br/>
          {/*
          <Dropdown className="cube-dropdown" onSelect={this.handleChange}>
            <Button className="cube-dropdown-btn">{this.state.btnTitle}</Button>
            <Dropdown.Toggle className="cube-dropdown-btn-toggle"/>
            <Dropdown.Menu className="super-colors">
              <MenuItem eventKey="Action">Action</MenuItem>
              <MenuItem eventKey="Another action">Another action</MenuItem>
              <MenuItem eventKey="Active Item" active>
                Active Item
              </MenuItem>
              <MenuItem divider />
              <MenuItem eventKey="Separated Link">Separated link</MenuItem>
            </Dropdown.Menu>
          </Dropdown>
          <br/><br/>
          */}

          { /* sidebar menu */ }
          { this.props.children }

          { /*/menu footer buttons*/ }
          <div className="sidebar-footer hidden-small">
            <a data-toggle="tooltip" data-placement="top" title="Settings">
              <span className="glyphicon glyphicon-cog" aria-hidden="true"></span>
            </a>
            <a data-toggle="tooltip" data-placement="top" title="FullScreen">
              <span className="glyphicon glyphicon-fullscreen" aria-hidden="true"></span>
            </a>
            <a data-toggle="tooltip" data-placement="top" title="Lock">
              <span className="glyphicon glyphicon-eye-close" aria-hidden="true"></span>
            </a>
            <a data-toggle="tooltip" data-placement="top" title="Logout" href="login.html">
              <span className="glyphicon glyphicon-off" aria-hidden="true"></span>
            </a>
          </div>
          { /*/menu footer buttons*/ }
        </div>
      </div>
    )
  }
}


export default Left
