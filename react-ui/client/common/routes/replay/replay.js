import React, { Component } from 'react'
import { Row, Col, Clearfix, Button, Glyphicon } from 'react-bootstrap'
import { XPanel, PageTitle } from '../../components'
import { connect } from 'react-redux';
import ServiceGraph from './ServiceGraph';
import ReplayAttribute from "./ReplayAttribute";

class replay extends Component {
  constructor (props) {
    super(props)
    this.state = {
      panelVisible: true,
    }
    this.handleChangeNeDropDown = this.handleChangeNeDropDown.bind(this);
    this.refreshButtonHandler = this.refreshButtonHandler.bind(this);
  }
  
  componentDidMount () {
    const { dispatch, user, neList } = this.props;
    //dispatch(nodMonActions.getNeList(neList, user.user));
  }


  componentWillUnmount() {
    const { dispatch, user, nctData } = this.props;
    //dispatch(nodMonActions.stopNctData(user.user, nctData.socket));
  }

  handleChangeNeDropDown(e){
    const { user, match, history, dispatch, nctData } = this.props;
    if (e && e.label) {
      let neIp = e.label.props.children[0];
      history.push(`${match.url}/${neIp}`);
    }
  } 

  
  refreshButtonHandler() {
    //console.log('refresh clicked');
    //const { dispatch, user, neList } = this.props;       
    //dispatch(nodMonActions.getNeList(neList, user.user, 'REFRESH'));    
  }

  render () {
    const { panelVisible } = this.state
    const onHide = e => this.setState({panelVisible: !panelVisible})
    const { user, match, nctData, neList, version } = this.props;
    return (
      <div>
        
        {/*<PageTitle title="Replay configuration" />*/}
        <Clearfix />
        <ReplayAttribute />
        <Clearfix />
        <Row style={{ minHeight: '100vh' }} >
            {/*Service Graph: */}
            <div className="col-sm-9">
            { 
                <ServiceGraph />
            }
            </div>

      <div className="col-sm-3">Hi</div>
        </Row>
        
      </div>
    )
  }
}




function mapStateToProps(state) {
  const { user } = state.authentication;

  return {
    user,
  }
}

const connectedReplay = connect(mapStateToProps)(replay);

export default connectedReplay
