import React, { Component } from 'react';
import { Row, Col, Clearfix, Button, Glyphicon, Tabs, Tab  } from 'react-bootstrap';
import { XPanel, PageTitle } from '../../components';
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
        };
        this.pieRef = React.createRef();
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

    }

    render () {
        const { panelVisible } = this.state;
        const { cube } = this.props;
        const onHide = e => this.setState({panelVisible: !panelVisible})
        const { user, match, nctData, neList, version } = this.props;
        let result = '';
        let rightPane = '';
        let pieData = [];
        let width = this.pieRef && this.pieRef.current ? this.pieRef.current.offsetWidth : 0;
        if (cube.analysis) {
            pieData.push({name: 'success', value: cube.analysis.respmatched + cube.analysis.resppartiallymatched});
            pieData.push({name: 'error', value: cube.analysis.respnotmatched});
            pieData.push({name: 'incomplete', value: cube.analysis.reqcnt - (cube.analysis.respmatched + cube.analysis.resppartiallymatched + cube.analysis.respnotmatched)});

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
                        <PiChart width={width} data={pieData}/>
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
                                Total Requests
                            </div>
                            <div className="col-md-8 pull-right">
                                {cube.analysis.reqcnt}
                            </div>
                        </div>
                    </div>
                </div>
            );
        } else {
            pieData = [];
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
                        <div className="col-sm-8 border-bottom">
                            {
                                <ServiceGraph />
                            }
                        </div>

                        <div className="col-sm-4 border-bottom no-padding" ref={this.pieRef}>
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
