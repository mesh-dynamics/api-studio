import React, { Component } from 'react'
import { Row, Col, Clearfix } from 'react-bootstrap'
import { XPanel, PageTitle } from '../../components'
import { Route, Link } from 'react-router-dom';
import { connect } from 'react-redux';
import ServerInfo from './ServerInfo';
import NeInfo from './NeInfo';
import { homeActions } from '../../actions/home.actions';


class Dashboard extends Component {
  constructor (props) {
    super(props)
    this.state = {
      panelVisible: true,
    }
  }

  componentDidMount () {
    const { dispatch, user } = this.props;
    
    if (!Object.keys(this.props.home.svrInfo).length || !Object.keys(this.props.home.neInfo).length) {
      dispatch(homeActions.destroyInfo(user.user));
    }
    dispatch(homeActions.getServerInfo(user.user));
    dispatch(homeActions.getNeInfo(user.user));
    
    /*
    if(!Object.keys(this.props.home.neInfo).length) {
      dispatch(homeActions.getNeInfo(user.user));
    }*/    
  }

  componentWillUnmount () {
    const { dispatch, user } = this.props;
    dispatch(homeActions.destroyInfo(user.user));
  }
  render () {
    const { match, user, home } = this.props;
    const { panelVisible } = this.state
    return (

      <div>
        {
          <Row>
            <Col md={12} sm={12} xs={12}>
              <ServerInfo {...this.props} data={this.props.home.svrInfo.data} loadStatus={this.props.home.svrInfo.loadStatus} />
              <NeInfo {...this.props} data={this.props.home.neInfo.data} loadStatus={this.props.home.neInfo.loadStatus}/>
            </Col>
          </Row>
      }              
      </div>
    )
  }
}

function mapStateToProps(state) {
  const { user } = state.authentication;
  const { home } = state;
  return {
    user,
    home
  }
}

const connectedDashboard = connect(mapStateToProps)(Dashboard);

export default connectedDashboard