import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Row, Col, Clearfix } from 'react-bootstrap';
import Modal from "react-bootstrap/es/Modal";
import Button from "react-bootstrap/es/Button";
import Select from 'react-select';
import './ReplayAttribute.css';
import {cubeConstants} from "../../constants";
import {cubeActions} from "../../actions";
import ScatterPlot from "../../components/Graph/ScatterPlot";

class ReplayAttribute extends Component {
    constructor(props) {
        super(props)
        this.state = {
            panelVisible: true,
            testIdPrefix: '',
            show: false,
        };
        this.doAnalysis = true;
        this.statusInterval;
        this.handleChangeForApps = this.handleChangeForApps.bind(this);
        this.handleChangeForTestIds = this.handleChangeForTestIds.bind(this);
        this.replay = this.replay.bind(this);
        this.getReplayStatus = this.getReplayStatus.bind(this);
        /*this.handleTestIdPrefixChange = this.handleTestIdPrefixChange.bind(this);
        this.handleTestIdPrefixSubmit = this.handleTestIdPrefixSubmit.bind(this);*/
    }

    componentDidMount() {
        const {
            dispatch,
            cube
        } = this.props;
        dispatch(cubeActions.getTestIds());
        /*if (cube.selectedTestId == cubeConstants.CREATE_NEW) {
            this.setState({testIdPrefix: cube.selectedApp.replace(' ', '-')})
        }*/
    }

    componentWillReceiveProps(nextProps, prevState) {
        // do things with nextProps.someProp and prevState.cachedSomeProp
        const cube = nextProps.cube;
        console.log(typeof cube.replayStatusObj);
        if (cube.replayStatusObj && (cube.replayStatusObj.status == 'Completed' || cube.replayStatusObj.status == 'Error')) {
            clearInterval(this.statusInterval);
            this.setState({show: false, toAnalysis: true});
            const {dispatch} = this.props;
            if(this.doAnalysis) {
                dispatch(cubeActions.getAnalysis(cube.selectedTestId, cube.replayId.replayid));
                if (cube.analysis) {
                    dispatch(cubeActions.getReport(cube.selectedTestId, cube.replayId.replayid));
                    this.doAnalysis = false;
                }
            }

        }

    }

    handleChangeForApps (e) {
        const { user, match, history, dispatch, nctData } = this.props;
        if (e && e.label) {
            console.log('label is: ', e.label);
            dispatch(cubeActions.setSelectedApp(e.label));
            dispatch(cubeActions.getTestIds(e.label));
            dispatch(cubeActions.setSelectedTestId(''));
        }
    }

    replay () {
        const {cube, dispatch} = this.props;
        if (!cube.selectedTestId) {
            alert('select collection to replay');
        } else {
            this.setState({show: true});
            dispatch(cubeActions.startReplay(cube.selectedTestId, cube.replayId.replayid));
            this.doAnalysis = true;
            this.statusInterval = setInterval(checkStatus, 400);
        }

        function checkStatus() {
            dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayid));
        }
    }

    getReplayStatus() {
        const {cube, dispatch} = this.props;
        dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayid));
    }


    handleChangeForTestIds (e) {
        const { user, match, history, dispatch, cube } = this.props;
        cube.selectedTestId = e.target.value;
        if (e) {
            console.log('test-id label is: ', e.target.value);
            dispatch(cubeActions.clear());
            dispatch(cubeActions.setSelectedTestId(e.target.value));
            dispatch(cubeActions.getGraphData());
            dispatch(cubeActions.getReplayId(e.target.value));
        }
    }

    renderTestIds ( cube ) {
        if (cube.testIdsReqStatus != cubeConstants.REQ_SUCCESS)
            return '';
        let options = [];
        if (cube.testIdsReqStatus == cubeConstants.REQ_SUCCESS) {
            options = cube.testIds.map(item => (<option key={item.collection} value={item.collection}>{item.collection}</option>));
        }
        // options.unshift({label: cubeConstants.CREATE_NEW, value: cubeConstants.CREATE_NEW});
        let jsxContent = '';
        if (options.length) {
            let selectedTestIdObj = ''
            if (cube.selectedTestId)
                selectedTestIdObj = { label: cube.selectedTestId, value: cube.selectedTestId};
            jsxContent = <div className="inline-block" key={cube.selectedTestId}>
                <select onChange={this.handleChangeForTestIds} value={cube.selectedTestId} placeholder={'Select...'}>
                    <option value="">Select Collection</option>
                    {options}
                </select>
            </div>
        }
        if (cube.testIdsReqStatus == cubeConstants.REQ_LOADING)
            jsxContent = <div><br/>Loading...</div>
        if (cube.testIdsReqStatus == cubeConstants.REQ_FAILURE)
            jsxContent = <div><br/>Request failed!</div>

        return <div className="inline-block">
            { jsxContent }
        </div>
    }

    render() {
        const { user, cube } = this.props;

        return (
            <div className="pos-rel" id="rep-attr">
                <div className="row2-dummy"></div>
                <div className="row1-dummy"></div>
                <div className="ra-row2">
                    <div className="ra-row1">
                        <Row>
                            <Col md={8}>
                                <div className="dual">
                                    <Row>
                                        <Col md={6}>
                                            <span className="label">Application</span><br/>
                                            <select name="" id="app-sel">
                                                <option value="">MovieInfo</option>
                                            </select> &nbsp;&nbsp;
                                            <span className="cube-btn">NEW TEST</span>
                                        </Col>
                                        <Col md={6}>
                                            <span className="label">Status</span><br/>
                                            <span className="status">{cube.replayStatus}</span>
                                        </Col>
                                    </Row>
                                </div>
                            </Col>
                            <Col md={4}>
                                <input style={{'marginTop': '10px'}} placeholder="Search Application" type="text"/>&nbsp;&nbsp;
                                <span className="cube-btn">VIEW SCHEDULE</span>
                            </Col>
                        </Row>
                    </div>
                    <div className="ra-row3">
                        <Row>
                            <Col md={8}>
                                <div className="inline-block">
                                    <span className="label">Collection</span><br/>
                                    {this.renderTestIds(cube)}&nbsp;&nbsp;
                                </div>
                                <div className="inline-block margin-left-15">
                                    <span className="label">Test Instance</span><br/>
                                    <select name="" id="app-sel">
                                        <option value="">Prod</option>
                                    </select> &nbsp;&nbsp;
                                </div>

                                <span className="cube-btn" onClick={this.replay}>Test</span>&nbsp;&nbsp;
                                <span className="cube-btn disabled">SAVE & RUN</span>&nbsp;&nbsp;
                                <span className="cube-btn disabled">STOP TEST</span>
                            </Col>
                        </Row>
                    </div>
                </div>

                <Modal show={this.state.show}>
                    <Modal.Header closeButton>
                        <Modal.Title>Gateway</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div>Test In Progress...</div>
                        <h3>
                            Status: {cube.replayStatus}&nbsp;&nbsp;
                            {cube.replayStatusObj ? (<small>Completed: {'' + cube.replayStatusObj.reqsent + '/' + cube.replayStatusObj.reqcnt}</small>) : null}
                        </h3>
                    </Modal.Body>
                    <Modal.Footer>

                    </Modal.Footer>
                </Modal>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
        user, cube
    }
}



const connectedReplayAttribute = connect(mapStateToProps)(ReplayAttribute);
export default connectedReplayAttribute
