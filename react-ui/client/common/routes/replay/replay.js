import React, { Component } from 'react'
import { Row, Col, Clearfix, Button, Glyphicon, Tabs, Tab  } from 'react-bootstrap';
import { XPanel, PageTitle } from '../../components'
import { connect } from 'react-redux';
import ServiceGraph from './ServiceGraph';
import ReplayAttribute from "./ReplayAttribute";
import "./ReplayAttribute.css";
import Results from "./Results";
import PiChart from "../../components/Graph/PiChart";

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
    const { panelVisible } = this.state;
    const { cube } = this.props;
    const onHide = e => this.setState({panelVisible: !panelVisible})
    const { user, match, nctData, neList, version } = this.props;
    let result = '';
      let rightPane = '';
    if (cube.report) {
        result = (
            <div className="result-container">
                <Tabs defaultActiveKey="result" id="uncontrolled-tab-example">
                    <Tab eventKey="result" title="Test Results: Detailed view">
                        <Results/>
                    </Tab>
                    <Tab eventKey="analysis" title="Analysis">
                        <div>In Prog</div>
                    </Tab>
                </Tabs>
            </div>
        );

        rightPane = (
            <div>
                <div>
                    <div className="right-head">
                        Test Results Overview
                    </div>
                    <PiChart/>
                </div>

                <div>
                    <div className="right-head">
                        Test Configuration
                    </div>
                    <div className="row">
                        <div className="col-md-4">
                            Test Configuration
                        </div>
                        <div className="col-md-8 pull-right">
                            HR-Primary-2019-3-RC-18
                        </div>
                    </div>
                    <div className="row">
                        <div className="col-md-4">
                            Collection
                        </div>
                        <div className="col-md-8 pull-right">
                            {cube.selectedTestId}
                        </div>
                    </div>
                    <div className="row">
                        <div className="col-md-4">

                        </div>
                        <div className="col-md-8 pull-right">

                        </div>
                    </div>
                </div>
            </div>
        );
    } else {
        result = '';
        rightPane = (
            <div>
                <div className="right-head">
                    Replay
                </div>
                <input type="radio" name="replayType" value="male" checked /> <span className="rep">Replay All</span><br/>
                <input type="radio" name="replayType" value="female" /> <span className="rep">Filter by Paths</span><br/>
                <input type="radio" name="replayType" value="other" /> <span className="rep">Filter for Specific Requests (max 25)</span>
            </div>
        );
    }
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
                    {rightPane}
                </div>
            </Row>
            {result}
        </div>
        
      </div>
    )
  }
}




function mapStateToProps(state) {
  const { user } = state.authentication;
  const cube = state.cube;
  return {
    user, cube
  }
}

const connectedReplay = connect(mapStateToProps)(replay);

export default connectedReplay
