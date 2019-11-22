import React from 'react';
import {Link} from "react-router-dom";
import {connect} from "react-redux";
import {cubeActions} from "../actions";
import {cubeConstants} from "../constants";
import Modal from "react-bootstrap/es/Modal";
import config from "../config";
import axios from "axios";

class ViewSelectedTestConfig extends React.Component {
    constructor(props) {
        super(props)
        this.state = {
            panelVisible: true,
            testIdPrefix: '',
            fcId: null,
            replayId: null,
            show: false,
            showCT: false,
        };
        this.doReplay = true;
        this.hideModalForFC = true;
        this.statusInterval;
        this.handleChangeForTestIds = this.handleChangeForTestIds.bind(this);
        this.replay = this.replay.bind(this);
        this.handleChangeForInstance = this.handleChangeForInstance.bind(this);
        this.getReplayStatus = this.getReplayStatus.bind(this);
        this.handleFC = this.handleFC.bind(this);
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.clear());
    }
  
    handleChangeForInstance(e) {
        const { dispatch } = this.props;
        if (e && e.target.value) {
            dispatch(cubeActions.setSelectedInstance(e.target.value));
        }
    }

    handleFC() {
        const { dispatch, cube } = this.props;
        dispatch(cubeActions.forceCompleteReplay(this.state.fcId));
        setTimeout(() => {
            this.setState({fcId: null});
        });
    }

    handleFCDone = () => {
        this.setState({fcId: null});
    };

    getReplayStatus() {
        const {cube, dispatch} = this.props;
        dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayId, cube.selectedApp));
    }


    handleChangeForTestIds (e) {
        const { user, match, history, dispatch, cube } = this.props;
        cube.selectedTestId = e.target.value;
        if (e) {
            dispatch(cubeActions.clear());
            let version = null;
            let golden = null;
            for (const collec of cube.testIds) {
                if (collec.collec == e.target.value) {
                    golden = collec.id
                    version = collec.templateVer;
                    break;
                }
            }
            //dispatch(cubeActions.getGraphData(cube.selectedApp));
            dispatch(cubeActions.setSelectedTestIdAndVersion(e.target.value, version, golden));
        }
    }

    renderInstances(cube) {
        if (!cube.instances) {
            return ''
        }
        let options = [];
        options = cube.instances.map(item => {
            if(item.app.name == cube.selectedApp)
                return (<option key={item.id} value={item.name}>{item.name}</option>);
        });
        let jsxContent = '';
        if (options.length) {
            jsxContent = <div>
                <select className="r-att" onChange={this.handleChangeForInstance} value={cube.selectedInstance} placeholder={'Select...'}>
                    <option value="">Select Instance</option>
                    {options}
                </select>
            </div>
        } else {
            jsxContent = <select className="r-att" value={cube.selectedInstance} placeholder={'Select...'}>
                <option value="">Select Instance</option>
            </select>
        }

        return jsxContent;
    }

    renderCollectionDD ( cube ) {
        if (cube.testIdsReqStatus != cubeConstants.REQ_SUCCESS || cube.testIdsReqStatus == cubeConstants.REQ_NOT_DONE)
            return <select className="r-att" disabled value={cube.selectedTestId} placeholder={'Select...'}>
                <option value="">No App Selected</option>
            </select>
        let options = [];
        if (cube.testIdsReqStatus == cubeConstants.REQ_SUCCESS) {
            options = cube.testIds.map(item => (<option key={item.collec} value={item.collec}>{item.id}</option>));
        }
        let jsxContent = '';
        if (options.length) {
            let selectedTestIdObj = ''
            if (cube.selectedTestId)
                selectedTestIdObj = { label: cube.selectedTestId, value: cube.selectedTestId};
            jsxContent = <div>
                <select className="r-att" onChange={this.handleChangeForTestIds} value={cube.selectedTestId} placeholder={'Select...'}>
                    <option value="">Select Golden</option>
                    {options}
                </select>
            </div>
        }
        if (cube.testIdsReqStatus == cubeConstants.REQ_LOADING)
            jsxContent = <div><br/>Loading...</div>
        if (cube.testIdsReqStatus == cubeConstants.REQ_FAILURE)
            jsxContent = <div><br/>Request failed!</div>

        return jsxContent;
    }

    showCT = () => {
        this.setState({showCT: true});
    };

    handleClose = () => {
        const {cube} = this.props;
        this.handleChangeForTestIds({target: {value: cube.selectedTestId}});
        this.setState({ show: false, showCT: false });
    };


    render() {
        const { cube } = this.props;

        return (
            <div>
                <div className="div-label">
                    Test Configuration
                    <Link to="/test_config">
                        <i className="fas fa-link pull-right link"></i>
                    </Link>
                </div>
                <div className="margin-top-10">
                    <div className="label-n">TEST NAME</div>
                    <div className="value-n">{cube.testConfig ? cube.testConfig.testConfigName : ''}</div>
                </div>
                <div className={cube.golden ? "margin-top-10" : "hidden"}>
                    <div className="label-n">GOLDEN</div>
                    <div className="value-n">{cube.golden ? cube.golden : ''}</div>
                </div>
                <div className={cube.testConfig && cube.testConfig.gatewayService ? "margin-top-10" : "hidden"}>
                    <div className="label-n">GATEWAY</div>
                    <div className="value-n">{cube.testConfig && cube.testConfig.gatewayService ? cube.testConfig.gatewayService.name : ''}</div>
                </div>
                <div className={cube.testConfig && cube.testConfig.criteria ? "margin-top-10" : "hidden"}>
                    <div className="label-n">CRITERIA</div>
                    <div className="value-n">{cube.testConfig && cube.testConfig.criteria ? cube.testConfig.criteria : ''}</div>
                </div>
                <div className={cube.testConfig && cube.testConfig.mocks ? "margin-top-10" : "hidden"}>
                    <div className="label-n">MOCK(S)</div>
                    <div className="value-n">{cube.testConfig && cube.testConfig.mocks ? cube.testConfig.mocks.join(",") : ''}</div>
                </div>

                <div className="margin-top-10">
                    <div className="cube-btn width-100 text-center">SAVE TEST CONFIG</div>
                </div>

                <div className="margin-top-10">
                    <div className="label-n">SELECT TEST INSTANCE</div>
                    <div className="value-n">
                        {this.renderInstances(cube)}
                    </div>
                </div>

                <div className="margin-top-10">
                    <div className="label-n">SELECT GOLDEN</div>
                    <div className="value-n">
                        {this.renderCollectionDD(cube)}
                    </div>
                </div>

                <div className="margin-top-10">
                    <div onClick={() => this.replay()} className="cube-btn width-100 text-center">RUN TEST</div>
                </div>

                <Modal show={this.state.show}>
                    <Modal.Header>
                        <Modal.Title>{cube.testConfig ? cube.testConfig.testConfigName : ''}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div className={!cube.replayStatusObj || (cube.replayStatusObj.status != "Completed" && cube.replayStatusObj.status != "Error") ? "" : "hidden"}>Test In Progress...</div>
                        <div className={cube.replayStatusObj && (cube.replayStatusObj.status == "Completed" || cube.replayStatusObj.status == "Error") ? "" : "hidden"}>Test Completed</div>
                        <h3>
                            Status: {cube.replayStatus}&nbsp;&nbsp;
                            {cube.replayStatusObj ? (<small>{cube.replayStatusObj.status + ': ' + cube.replayStatusObj.reqsent + '/' + cube.replayStatusObj.reqcnt}</small>) : null}
                        </h3>
                    </Modal.Body>
                    <Modal.Footer className={cube.replayStatusObj && (cube.analysis && (cube.replayStatusObj.status == "Completed" || cube.replayStatusObj.status == "Error")) ? "text-center" : "hidden"}>
                        <Link to="/">
                            <span onClick={this.handleClose} className="cube-btn">View Results</span>&nbsp;&nbsp;
                        </Link>
                        <span onClick={this.handleClose} className="cube-btn">Done</span>
                    </Modal.Footer>
                </Modal>

                <Modal show={this.state.fcId}>
                    <Modal.Header>
                        <Modal.Title>Force Complete</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <p>
                            A replay with id {this.state.fcId} is in progress.
                        </p>
                    </Modal.Body>
                    <Modal.Footer>
                        <span onClick={this.handleFC} className="cube-btn">Force Complete</span>&nbsp;&nbsp;
                        <span onClick={this.handleFCDone} className="cube-btn">Done</span>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }

    replay () {
        const {cube, dispatch} = this.props;
        cubeActions.clearReplayStatus();
        if (!cube.selectedTestId) {
            alert('select golden to replay');
        } else {
            this.setState({show: true});
            let user = JSON.parse(localStorage.getItem('user'));
            let url = `${config.replayBaseUrl}/start/${cube.selectedGolden}`;
            let instance = cube.selectedInstance ? cube.selectedInstance : 'prod';
            let selectedInstances = cube.instances.filter((item) => item.name == instance && item.app.name == cube.selectedApp);
            let gatewayEndpoint = selectedInstances.length > 0 ? selectedInstances[0].gatewayEndpoint : "http://demo.dev.cubecorp.io";
            const searchParams = new URLSearchParams();
            searchParams.set('endPoint', gatewayEndpoint);
            searchParams.set('instanceId', instance);
            searchParams.set('templateSetVer', cube.collectionTemplateVersion);
            searchParams.set('userId', user.username);
            if (cube.selectedApp != 'Cube') {
                searchParams.set('paths', 'minfo/listmovies');
                searchParams.append('paths', 'minfo/returnmovie');
                searchParams.append('paths', 'minfo/rentmovie');
                searchParams.append('paths', 'minfo/liststores');
            }
            const configForHTTP = {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    "Authorization": "Bearer " + user['access_token']
                }
            };
            axios.post(url, searchParams, configForHTTP).then((response) => {
                this.setState({replayId: response.data});
                this.statusInterval = setInterval(() => {
                    const {cube} = this.props;
                    if (cube.replayStatusObj && (cube.replayStatus == 'Completed' || cube.replayStatus == 'Error')) {
                        clearInterval(this.statusInterval);
                    } else {
                        checkStatus();
                    }
                }, 1000);
            }).catch((error) => {
                if(error.response.data) {
                    if (error.response.data['replayId'] !== "None") {
                        this.setState({fcId: error.response.data['replayId'], show: false});
                    } else {
                        this.setState({show: false});
                        alert(error.response.data['message']);
                    }
                } else {
                    this.setState({show: false});
                    alert(error.response.statusText);
                }
            });
        }

        let checkStatus = () => {
            dispatch(cubeActions.getReplayStatus(cube.selectedTestId, this.state.replayId.replayId, cube.selectedApp));
        };
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedViewSelectedTestConfig = connect(mapStateToProps)(ViewSelectedTestConfig);

export default connectedViewSelectedTestConfig
