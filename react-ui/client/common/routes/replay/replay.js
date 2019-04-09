import React, { Component } from 'react'
import { Row, Col, Clearfix, Button, Glyphicon, Tabs, Tab  } from 'react-bootstrap';
import { XPanel, PageTitle } from '../../components'
import { connect } from 'react-redux';
import ServiceGraph from './ServiceGraph';
import ReplayAttribute from "./ReplayAttribute";
import "./ReplayAttribute.css";
import Results from "./Results";

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

        <Clearfix />
        <ReplayAttribute />
        <Clearfix />
        <div className="replay-container">
            <Row>
                {/*Service Graph: */}
                <div className="col-sm-9 border-bottom">
                {
                    <ServiceGraph />
                }
                </div>

                <div className="col-sm-3 border-bottom">
                    <div className="right-head">
                        Replay
                    </div>
                    <input type="radio" name="replayType" value="male" checked /> <span className="rep">Replay All</span><br/>
                    <input type="radio" name="replayType" value="female" /> <span className="rep">Filter by Paths</span><br/>
                    <input type="radio" name="replayType" value="other" /> <span className="rep">Filter for Specific Requests (max 25)</span>
                </div>
            </Row>

            <div className="result-container">
                <Tabs defaultActiveKey="result" id="uncontrolled-tab-example">
                    <Tab eventKey="result" title="Result">
                        <Results/>
                    </Tab>
                    <Tab eventKey="analysis" title="Analysis">
                        <div>In Prog</div>
                    </Tab>
                </Tabs>
            </div>
        </div>
        
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
