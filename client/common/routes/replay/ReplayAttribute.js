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
import Diff from "../../components/Diff";

const compTemp = [
    {
        "id": "ResponseTemplate-productpage",
        "path": "productpage",
        "service": "productpage",
        "template": {
            "prefixPath": "",
            "rules": [

            ]
        }
    },
    {
        "id": "ResponseTemplate-minfo-returnmovie",
        "path": "minfo/returnmovie",
        "service": "movieinfo",
        "template": {
            "prefixPath": "",
            "rules": [
                {
                    "path": "/body/return_updates",
                    "pt": "Required",
                    "dt": "Int",
                    "ct": "Equal"
                },
                {
                    "path": "/body/payment_updates",
                    "pt": "Required",
                    "dt": "Int",
                    "ct": "Equal"
                },
                {
                    "path": "/body/rental_id",
                    "pt": "Required",
                    "dt": "Int",
                    "ct": "Equal"
                }
            ]
        }
    },
    {
        "id": "ResponseTemplate-minfo-liststores",
        "path": "minfo/liststores",
        "service": "movieinfo",
        "template": {
            "prefixPath": "",
            "rules": [
                {
                    "path": "/body/0/store_id",
                    "pt": "Optional",
                    "dt": "Int",
                    "ct": "Default"
                },
                {
                    "path": "/body/1/store_id",
                    "pt": "Optional",
                    "dt": "Int",
                    "ct": "Default"
                }
            ]
        }
    },
    {
        "id": "ResponseTemplate-minfo-rentmovie",
        "path": "minfo/rentmovie",
        "service": "movieinfo",
        "template": {
            "prefixPath": "",
            "rules": [
                {
                    "path": "/body/inventory_id",
                    "pt": "Required",
                    "dt": "Int",
                    "ct": "Equal"
                },
                {
                    "path": "/body/rent",
                    "pt": "Required",
                    "dt": "Float",
                    "ct": "Equal"
                },
                {
                    "path": "/body/num_updates",
                    "pt": "Required",
                    "dt": "Int",
                    "ct": "Equal"
                }
            ]
        }
    },
    {
        "id": "ResponseTemplate-minfo-listmovies",
        "path": "minfo/listmovies",
        "service": "movieinfo",
        "template": {
            "prefixPath": "",
            "rules": [
                {
                    "path": "/body/0/actors_lastnames",
                    "pt": "Optional",
                    "dt": "RptArray",
                    "ct": "Equal"
                },
                {
                    "path": "/body/0/display_actors",
                    "pt": "Required",
                    "dt": "RptArray",
                    "ct": "EqualOptional"
                },
                {
                    "path": "/body/0/display_actors/*",
                    "pt": "Optional",
                    "dt": "Str",
                    "ct": "EqualOptional"
                },
                {
                    "path": "/body/0/film_id",
                    "pt": "Required",
                    "dt": "Int",
                    "ct": "Equal"
                },
                {
                    "path": "/body/0/title",
                    "pt": "Required",
                    "dt": "Str",
                    "ct": "Equal"
                },
                {
                    "path": "/body/0/actors_firstnames",
                    "pt": "Optional",
                    "dt": "RptArray",
                    "ct": "Equal"
                },
                {
                    "path": "/body/0/film_counts",
                    "pt": "Required",
                    "dt": "RptArray",
                    "ct": "Equal"
                },
                {
                    "path": "/body/0/film_counts/*",
                    "pt": "Required",
                    "dt": "Int",
                    "ct": "Equal"
                },
                {
                    "path": "/body/0/bookinfo",
                    "pt": "Optional",
                    "dt": "RptArray"
                },
                {
                    "path": "/body/0/timestamp",
                    "pt": "Required",
                    "dt": "Default",
                    "ct": "EqualOptional",
                    "customization": "[0-9]{14}"
                }
            ]
        }
    }
];
class ReplayAttribute extends Component {
    constructor(props) {
        super(props)
        this.state = {
            panelVisible: true,
            testIdPrefix: '',
            show: false,
            showCT: false,
        };
        this.doAnalysis = true;
        this.statusInterval;
        this.handleChangeForApps = this.handleChangeForApps.bind(this);
        this.handleChangeForTestIds = this.handleChangeForTestIds.bind(this);
        this.replay = this.replay.bind(this);
        this.handleChangeForInstance = this.handleChangeForInstance.bind(this);
        this.getReplayStatus = this.getReplayStatus.bind(this);
    }

    componentDidMount() {
        const {
            dispatch,
            cube
        } = this.props;
        setTimeout(() => {
            dispatch(cubeActions.getApps());
            dispatch(cubeActions.getInstances());
            dispatch(cubeActions.getGraphData(cube.selectedApp));
            //dispatch(cubeActions.getTestIds(cube.selectedApp));
        }, 0);
    }

    componentWillReceiveProps(nextProps, prevState) {
        // do things with nextProps.someProp and prevState.cachedSomeProp
        const cube = nextProps.cube;
        if (cube.replayStatusObj && (cube.replayStatusObj.status == 'Completed' || cube.replayStatusObj.status == 'Error')) {
            clearInterval(this.statusInterval);
            this.setState({show: false, toAnalysis: true});
            const {dispatch} = this.props;
            if(this.doAnalysis) {
                dispatch(cubeActions.getAnalysis(cube.selectedTestId, cube.replayId.replayid));
                if (cube.analysis) {
                    dispatch(cubeActions.getReport(cube.selectedTestId, cube.replayId.replayid));
                    dispatch(cubeActions.getTimelineData(cube.selectedTestId, cube.replayId));
                    this.doAnalysis = false;
                }
            }

        }

    }

    handleChangeForApps (e) {
        const { dispatch } = this.props;
        if (e && e.target.value) {
            dispatch(cubeActions.setSelectedApp(e.target.value));
            dispatch(cubeActions.getTestIds(e.target.value));
            dispatch(cubeActions.setSelectedTestId(''));
        }
    }

    handleChangeForInstance(e) {
        const { dispatch } = this.props;
        if (e && e.target.value) {
            dispatch(cubeActions.setSelectedInstance(e.target.value));
        }
    }

    replay () {
        const {cube, dispatch} = this.props;
        if (!cube.selectedTestId) {
            alert('select collection to replay');
        } else if (!cube.gateway) {
            alert('select gateway point');
        } else {
            this.setState({show: true});
            dispatch(cubeActions.startReplay(cube.selectedTestId, cube.replayId.replayid, cube.selectedApp));
            this.doAnalysis = true;
            this.statusInterval = setInterval(checkStatus, 400);
        }

        function checkStatus() {
            dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayid));
        }
    }

    getReplayStatus() {
        const {cube, dispatch} = this.props;
        dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayid, cube.selectedApp));
    }


    handleChangeForTestIds (e) {
        const { user, match, history, dispatch, cube } = this.props;
        cube.selectedTestId = e.target.value;
        if (e) {
            dispatch(cubeActions.clear());
            //dispatch(cubeActions.getGraphData(cube.selectedApp));
            dispatch(cubeActions.setSelectedTestId(e.target.value));
            dispatch(cubeActions.getReplayId(e.target.value, cube.selectedApp , (cube.selectedInstance ? cube.selectedInstance : 'PROD')));
        }
    }

    renderInstances(cube) {
        if (!cube.instances) {
            return ''
        }
        let options = [];
        options = cube.instances.map(item => (<option key={item.id} value={item.name}>{item.name}</option>));
        let jsxContent = '';
        if (options.length) {
            jsxContent = <div className="inline-block">
                <select onChange={this.handleChangeForInstance} value={cube.selectedInstance} placeholder={'Select...'}>
                    <option value="">Select Instance</option>
                    {options}
                </select>
            </div>
        } else {
            <select value={cube.selectedInstance} placeholder={'Select...'}>
                <option value="">Select Instance</option>
            </select>
        }

        return <div className="inline-block">
            { jsxContent }
        </div>
    }

    renderAppList (cube) {
        if (cube.appsListReqStatus != cubeConstants.REQ_SUCCESS || !cube.appsList) {
            return ''
        }
        let options = [];
        options = cube.appsList.map(item => {
            if (item.customer.username == 'demo@cubecorp.io') {
                return (<option key={item.id} value={item.name}>{item.name}</option>);
            }
            return null;
        });
        let jsxContent = '';
        if (options.length) {
            jsxContent = <div className="inline-block">
                <select onChange={this.handleChangeForApps} value={cube.selectedApp} placeholder={'Select...'}>
                    <option value="">Select App</option>
                    {options}
                </select>
            </div>
        } else {
            <select onChange={this.handleChangeForApps} value={cube.selectedApp} placeholder={'Select...'}>
                <option value="">Select App</option>
            </select>
        }

        return <div className="inline-block">
            { jsxContent }
        </div>
    }

    renderTestIds ( cube ) {
        if (cube.testIdsReqStatus != cubeConstants.REQ_SUCCESS || cube.testIdsReqStatus == cubeConstants.REQ_NOT_DONE)
           return <select disabled value={cube.selectedTestId} placeholder={'Select...'}>
                <option value="">No App Selected</option>
            </select>
        let options = [];
        if (cube.testIdsReqStatus == cubeConstants.REQ_SUCCESS) {
            options = cube.testIds.map(item => (<option key={item.collection} value={item.collection}>{item.collection}</option>));
        }
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

    showCT = () => {
        this.setState({showCT: true});
    }

    handleClose = () => {
        this.setState({ show: false, showCT: false });
    }

    render() {
        const { user, cube } = this.props;
        const ct = JSON.stringify(compTemp, undefined, 4);
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
                                            {this.renderAppList(cube)}&nbsp;&nbsp;
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
                                <div style={{marginTop: '15px'}}>
                                    <span className="cube-btn" onClick={this.showCT}>View Comparison Rules</span>
                                </div>
                            </Col>
                        </Row>
                    </div>
                    <div className="ra-row3">
                        <Row>
                            <Col md={12}>
                                <div className="inline-block">
                                    <span className="label">Collection</span><br/>
                                    {this.renderTestIds(cube)}&nbsp;&nbsp;
                                </div>
                                <div className="inline-block margin-left-15">
                                    <span className="label">Test Instance</span><br/>
                                    {this.renderInstances(cube)}&nbsp;&nbsp;
                                </div>

                                <span className="cube-btn" onClick={this.replay}>Test</span>&nbsp;&nbsp;
                                <span className="cube-btn disabled">SAVE & RUN</span>&nbsp;&nbsp;
                            </Col>
                        </Row>
                    </div>
                </div>

                <Modal show={this.state.showCT} onHide={this.handleClose}>
                    <Modal.Header closeButton>
                        <Modal.Title>Comparison Rules</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <pre>
                            {ct}
                        </pre>
                    </Modal.Body>
                    <Modal.Footer>

                    </Modal.Footer>
                </Modal>

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
