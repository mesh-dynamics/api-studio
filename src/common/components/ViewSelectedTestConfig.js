import React, { Fragment } from 'react';
import {Link} from "react-router-dom";
import {connect} from "react-redux";
import {cubeActions} from "../actions";
import {cubeConstants} from "../constants";
import Modal from "react-bootstrap/es/Modal";
import config from "../config";
import axios from "axios";
import {GoldenMeta} from "./Golden-Visibility";
import {goldenActions} from '../actions/golden.actions'
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
            showGoldenMeta: false,
            showGoldenFilter: false,
            goldenNameFilter: "",
            goldenIdFilter: "",
            goldenBranchFilter: "",
            goldenVersionFilter: "",
            selectedGoldenFromFilter: ""
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

    
    showCT = () => {
        this.setState({showCT: true});
    };

    handleClose = () => {
        const {cube} = this.props;
        this.handleChangeForTestIds({target: {value: cube.selectedTestId}});
        this.setState({ show: false, showCT: false });
    };

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

    getFormattedDate(date) {
        var year = date.getFullYear();

        var month = (1 + date.getMonth()).toString();
        month = month.length > 1 ? month : '0' + month;

        var day = date.getDate().toString();
        day = day.length > 1 ? day : '0' + day;

        return month + '/' + day + '/' + year;
    }

    renderCollectionTable() {
        const {cube} = this.props;
        let collectionList = cube.testIds;

        if (this.state.goldenNameFilter) {
            collectionList = collectionList.filter(item => item.name.toLowerCase().includes(this.state.goldenNameFilter.toLowerCase()));
        }

        if (this.state.goldenBranchFilter) {
            collectionList = collectionList.filter(item => item.branch && item.branch.toLowerCase().includes(this.state.goldenBranchFilter.toLowerCase()));
        }

        if (this.state.goldenVersionFilter) {
            collectionList = collectionList.filter(item => item.codeVersion && item.codeVersion.toLowerCase().includes(this.state.goldenVersionFilter.toLowerCase()));
        }

        if (this.state.goldenIdFilter) {
            collectionList = collectionList.filter(item => item.id.toLowerCase().includes(this.state.goldenIdFilter.toLowerCase()));
        }

        if (!collectionList || collectionList.length == 0) {
            return <tr><td colSpan="5">NO DATA FOUND</td></tr>
            return;
        }

        let trList = collectionList.map(item => (<tr value={item.collec} className={this.state.selectedGoldenFromFilter == item.collec ? "selected-g-row" : ""} onClick={() => this.selectGoldenFromFilter(item.collec)}><td>{item.name}</td><td>{item.id}</td><td>{this.getFormattedDate(new Date(item.timestmp*1000))}</td><td>{item.userId}</td><td>{item.prntRcrdngId}</td></tr>));
        return trList;
    }

    selectGoldenFromFilter = (g) => {
        this.setState({selectedGoldenFromFilter: g});
    };

    applyGoldenFilter = (filter, event) => {
        this.setState({[filter] : event.target.value});
    };

    renderCollectionDD ( cube ) {
        if (cube.testIdsReqStatus != cubeConstants.REQ_SUCCESS || cube.testIdsReqStatus == cubeConstants.REQ_NOT_DONE)
            return <select className="r-att" disabled value={cube.selectedTestId} placeholder={'Select...'}>
                <option value="">No App Selected</option>
            </select>;
        let options = [];
        if (cube.testIdsReqStatus == cubeConstants.REQ_SUCCESS) {
            options = cube.testIds.map((item, index) => {
                if (index < 8)
                    return (<option key={item.collec} value={item.collec}>{item.name}</option>);

                else
                    return (<option className="hidden" key={item.collec} value={item.collec}>{item.name}</option>);
            });
        }
        let jsxContent = '';
        if (options.length) {
            let selectedTestIdObj = '';
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

    handleViewGoldenClick = () => {
        const { dispatch } = this.props;
        
        this.setState({ showGoldenMeta: true });
        dispatch(cubeActions.hideGoldenVisibility(false))
    }

    handleBackToTestInfoClick = () => {
        const { dispatch } = this.props;

        this.setState({ showGoldenMeta: false });

        dispatch(cubeActions.hideGoldenVisibility(true));
        dispatch(goldenActions.resetServiceAndApiPath());
    }

    renderTestInfo = () => {
        const { cube } = this.props;

        return(
            <Fragment>
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
                    <div className="label-n">SELECT GOLDEN&nbsp;
                        <i onClick={this.showGoldenFilter} title="Browse Golden" className="link fas fa-folder-open pull-right font-15"></i>
                        {
                            cube.selectedInstance && cube.selectedTestId &&
                                    <span className="pull-right" onClick={this.handleViewGoldenClick} style={{ marginLeft: "5px", cursor: "pointer" }}>
                                        <i className="fas fa-eye margin-right-10" style={{ fontSize: "12px", color: "#757575"}} aria-hidden="true"></i>
                                    </span>
                        }
                    </div>
                    <div className="value-n">
                        {this.renderCollectionDD(cube)}
                    </div>
                </div>

                <div className="margin-top-10">
                    <div onClick={() => this.replay()} className="cube-btn width-100 text-center">RUN TEST</div>
                </div>
            </Fragment>
        );

    };

    render() {
        const { cube } = this.props;
        const { showGoldenMeta } = this.state;

        return (
            <div>
                {!showGoldenMeta && this.renderTestInfo()}

                {showGoldenMeta && <GoldenMeta {...cube} handleBackToTestInfoClick={this.handleBackToTestInfoClick} />}

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

                <Modal show={this.state.showGoldenFilter} bsSize="large">
                    <Modal.Header>
                        <Modal.Title>Browse Golden <small style={{color: "white"}}>({cube.selectedApp})</small></Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div className="margin-bottom-10" style={{padding: "10px 25px", border: "1px dashed #ddd"}}>
                            <div className="row margin-bottom-10">
                                <div className="col-md-5">
                                    <div className="label-n">NAME</div>
                                    <div className="value-n">
                                        <input onChange={(event) => this.applyGoldenFilter("goldenNameFilter", event)} className="width-100 h-20px" type="text"/>
                                    </div>
                                </div>

                                <div className="col-md-2"></div>

                                <div className="col-md-5">
                                    <div className="label-n">BRANCH</div>
                                    <div className="value-n">
                                        <input onChange={(event) => this.applyGoldenFilter("goldenBranchFilter", event)} className="width-100 h-20px" type="text"/>
                                    </div>
                                </div>
                            </div>

                            <div className="row margin-bottom-10">
                                <div className="col-md-5">
                                    <div className="label-n">RECORDING ID</div>
                                    <div className="value-n">
                                        <input onChange={(event) => this.applyGoldenFilter("goldenIdFilter", event)} className="width-100 h-20px" type="text"/>
                                    </div>
                                </div>

                                <div className="col-md-2"></div>

                                <div className="col-md-5">
                                    <div className="label-n">CODE VERSION</div>
                                    <div className="value-n">
                                        <input onChange={(event) => this.applyGoldenFilter("goldenVersionFilter", event)} className="width-100 h-20px" type="text"/>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div style={{height: "300px", overflowY: "auto"}}>
                            <table className="table table-condensed table-hover table-striped">
                                <thead>
                                <tr>
                                    <td className="bold">Name</td>
                                    <td className="bold">ID</td>
                                    <td className="bold">Date</td>
                                    <td className="bold">Created By</td>
                                    <td className="bold">Parent ID</td>
                                </tr>
                                </thead>

                                <tbody>
                                {this.renderCollectionTable()}
                                </tbody>
                            </table>
                        </div>

                    </Modal.Body>
                    <Modal.Footer>
                        <span onClick={this.selectHighlighted} className={this.state.selectedGoldenFromFilter ? "cube-btn" : "disabled cube-btn"}>Select</span>&nbsp;&nbsp;
                        <span onClick={this.hideGoldenFilter} className="cube-btn">Cancel</span>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }

    showGoldenFilter = () => {
        this.setState({
            goldenNameFilter: "",
            goldenIdFilter: "",
            goldenBranchFilter: "",
            goldenVersionFilter: "",
            selectedGoldenFromFilter: "",
            showGoldenFilter: true
        });

    };

    hideGoldenFilter = () => {
        this.setState({showGoldenFilter: false});
    };

    selectHighlighted = () => {
        this.handleChangeForTestIds({target: {value: this.state.selectedGoldenFromFilter}});
        this.hideGoldenFilter();
    };

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
