import React, { Fragment } from 'react';
import {Link} from "react-router-dom";
import {connect} from "react-redux";
import {cubeActions} from "../actions";
import {cubeConstants} from "../constants";
import Modal from "react-bootstrap/es/Modal";
import config from "../config";
import {getTransformHeaders} from "../utils/lib/url-utils";
import axios from "axios";
import {GoldenMeta} from "./Golden-Visibility";
import {goldenActions} from '../actions/golden.actions'
import {validateGoldenName} from "../utils/lib/golden-utils";
// import { history } from "../helpers";
// import { Glyphicon } from 'react-bootstrap';

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
            showAddCustomHeader: false,
            showGoldenMeta: false,
            showGoldenFilter: false,
            goldenNameFilter: "",
            goldenIdFilter: "",
            goldenBranchFilter: "",
            goldenVersionFilter: "",
            selectedGoldenFromFilter: "",
            recordModalVisible: false,
            recStatus: null,
            recName: "",
            recLabel:"",
            recId: null,
            stopDisabled: true,
            goldenNameErrorMessage: "",
            customHeaders: {
                default: {
                    key: "",
                    value: ""
                }
            }
        };
        //this.statusInterval;
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.clear());
    }
  
    handleChangeForInstance = (e) => {
        const { dispatch } = this.props;
        if (e && e.target.value) {
            dispatch(cubeActions.setSelectedInstance(e.target.value));
        }
    }

    handleFC = () => {
        const { dispatch, cube } = this.props;
        dispatch(cubeActions.forceCompleteReplay(this.state.fcId));
        setTimeout(() => {
            this.setState({fcId: null});
        });
    }

    handleFCDone = () => {
        this.setState({fcId: null});
    };

    getReplayStatus = () => {
        const {cube, dispatch} = this.props;
        dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayId, cube.selectedApp));
    };

    changeRecName = (e) => {
        this.setState({recName: e.target.value.replace(/  /g, " ")});
    };

    showAddCustomHeaderModal = () => this.setState({ showAddCustomHeader: true });

    cancelAddCustomHeaderModal = () => this.setState({ 
        showAddCustomHeader: false, 
        customHeaders: { default: { key: "", value: "" }} 
    });

    closeAddCustomHeaderModal = () => this.setState({ showAddCustomHeader: false })

    addKeyValueInput = () => {
        const headerId = Math.random().toString(36).slice(2);

        this.setState({ 
            customHeaders: {
                ...this.state.customHeaders,
                [headerId]: { key: "", value: "" },
            }
        });
    };

    removeKeyValueInput = (headerId) => {
        const { customHeaders } = this.state;
        
        delete customHeaders[headerId];

        this.setState({ customHeaders });
    };

    handleCustomHeaderKeyChange = (key, headerId) => {
        const { customHeaders } = this.state;

        customHeaders[headerId].key = key;

        this.setState({ customHeaders });
    };
    
    handleCustomHeaderValueChange = (value, headerId) => {
        const { customHeaders } = this.state;

        customHeaders[headerId].value = value;

        this.setState({ customHeaders });
    };

    handleChangeForTestIds = (e) => {
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
    };
    
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

        let trList = collectionList.map(item => (<tr key={item.collec} value={item.collec} className={this.state.selectedGoldenFromFilter == item.collec ? "selected-g-row" : ""} onClick={() => this.selectGoldenFromFilter(item.collec)}><td>{item.name}</td><td>{item.label}</td><td>{item.id}</td><td>{this.getFormattedDate(new Date(item.timestmp*1000))}</td><td>{item.userId}</td><td>{item.prntRcrdngId}</td></tr>));
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
                    return (<option key={item.collec + index} value={item.collec}>{item.name}</option>);

                else
                    return (<option className="hidden" key={item.collec + index} value={item.collec}>{item.name}</option>);
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

    showRecordModal = () => {
        const { cube } = this.props;
        
        if(!cube.selectedInstance){
            alert('Select an Instance to Record')
        } else {
            this.setState({recordModalVisible: true});
        }
    };

    handleCloseRecModal = () => {
        this.setState({recordModalVisible: false, recStatus: null});
    };

    handleViewGoldenClick = () => {
        const { dispatch } = this.props;
        
        this.setState({ showGoldenMeta: true });
        dispatch(cubeActions.hideGoldenVisibility(false))
        // history.push({ pat}"/test_config_view/`");
    };

    handleBackToTestInfoClick = () => {
        const { dispatch } = this.props;

        this.setState({ showGoldenMeta: false });

        dispatch(cubeActions.hideGoldenVisibility(true));
        dispatch(goldenActions.resetGoldenVisibilityDetails());
    };

    renderTestInfo = () => {
        const { cube, authentication: { user: { username } } } = this.props;

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
                            cube.selectedTestId &&
                                    <span className="pull-right" onClick={this.handleViewGoldenClick} style={{ marginLeft: "5px", cursor: "pointer" }}>
                                        <i className="fas fa-eye margin-right-10" style={{ fontSize: "12px", color: "#757575"}} aria-hidden="true"></i>
                                    </span>
                        }
                    </div>
                    <div className="value-n">
                        {this.renderCollectionDD(cube)}
                    </div>
                </div>

                <div style={{ fontSize: "12px" }} className="margin-top-10 row">
                    <span  className="label-link col-sm-12 pointer" onClick={this.showAddCustomHeaderModal}>
                        <i className="fas fa-plus" style={{ color: "#333333", marginRight: "5px" }} aria-hidden="true"></i>
                        Custom Headers
                    </span>
                </div>

                <div className="margin-top-10 row">
                    <div className="col-sm-6">
                        <div onClick={() => this.replay()} className="cube-btn width-100 text-center">RUN TEST</div>
                    </div>
                    <div className="col-sm-6"><div onClick={this.showRecordModal} className="cube-btn width-100 text-center">RECORD</div></div>
                </div>
            </Fragment>
        );

    };

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

    startRecord = () => {
        const { cube, authentication } = this.props;
        let user = authentication.user;
        let url = `${config.recordBaseUrl}/start/${user.customer_name}/${cube.selectedApp}/${cube.selectedInstance}/RespPartialMatch`;
        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                "Authorization": "Bearer " + user['access_token']
            }
        };

        const recLabel = Date.now().toString();
        const searchParams = new URLSearchParams();
        searchParams.set('name', this.state.recName);
        searchParams.set('userId', user.username);
        searchParams.set('label', recLabel);

        axios.post(url, searchParams, configForHTTP).then((response) => {
            this.setState({stopDisabled: false, recId: response.data.id})
            this.recStatusInterval = setInterval(() => {
                if (!this.state.recId) {
                    clearInterval(this.recStatusInterval);
                } else {
                    checkStatus();
                }
            }, 1000);
        });

        const checkStatus = () => {
            const csUrl = `${config.recordBaseUrl}/status/${user.customer_name}/${cube.selectedApp}/${this.state.recName}/${recLabel}`;
            axios.get(csUrl, configForHTTP).then(response => {
                this.setState({recStatus: response.data});
            });
        };
    };

    handleStartRecordClick = () => {
        
        const { recName } = this.state;

        const { goldenNameIsValid, goldenNameErrorMessage } = validateGoldenName(recName);

        if(goldenNameIsValid) {
            this.setState({ goldenNameErrorMessage });
            this.startRecord();
        } else {
            this.setState({ goldenNameErrorMessage });
        }
    };

    stopRecord = () => {
        const { cube, authentication } = this.props;
        const user = authentication.user;
        const url = `${config.recordBaseUrl}/stop/${this.state.recId}`;
        const recLabel = Date.now().toString();
        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                "Authorization": "Bearer " + user['access_token']
            }
        };
        axios.post(url, {}, configForHTTP).then((response) => {
            this.setState({stopDisabled: true, recId: null});
            const csUrl = `${config.recordBaseUrl}/status/${user.customer_name}/${cube.selectedApp}/${this.state.recName}/${recLabel}`;
            axios.get(csUrl, configForHTTP).then(response => {
                this.setState({recStatus: response.data});
            });
        });
    };

    replay = async () => {
        const { cube, dispatch, authentication, checkReplayStatus } = this.props;
        const { testConfig: { testPaths, testMockServices, testConfigName }} = cube;
        const selectedInstances = cube.instances
            .filter((item) => item.name == cube.selectedInstance && item.app.name == cube.selectedApp);
        cubeActions.clearReplayStatus();
         if(!cube.selectedInstance){
            alert('select an instance to replay')
        } else if (!cube.selectedTestId) {
            alert('select golden to replay');
        } else if(selectedInstances.length === 0) {
            alert('Gateway endpoint is unavailable')
        } else {
            this.setState({show: true});
            let user = authentication.user;
            let url = `${config.replayBaseUrl}/start/${cube.selectedGolden}`;

            const transforms = JSON.stringify(getTransformHeaders(this.state.customHeaders));

            const searchParams = new URLSearchParams();
            searchParams.set('endPoint', selectedInstances[0].gatewayEndpoint);
            searchParams.set('instanceId', cube.selectedInstance);
            searchParams.set('templateSetVer', cube.collectionTemplateVersion);
            searchParams.set('userId', user.username);
            searchParams.set('transforms', transforms);
            searchParams.set('mockServices',testMockServices);
            searchParams.set('testConfigName', testConfigName);
            searchParams.set('analyze', true);
            // Append Test Paths
            // If not specified, it will run all paths
            if(testPaths && testPaths.length !== 0) {
                testPaths.map(path => searchParams.append("paths", path))
            }

            const configForHTTP = {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    "Authorization": "Bearer " + user['access_token']
                }
            };
            axios.post(url, searchParams, configForHTTP).then((response) => {
                this.setState({replayId: response.data});
                // check replay status periodically and call analyze at the end; and update timeline
                // this method is run in the parent component (Navigation)
                checkReplayStatus(this.state.replayId.replayId); 
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
    };

    render() {
        const { cube } = this.props;
        const { 
            showGoldenMeta, customHeaders, recordModalVisible, 
            show, fcId, showGoldenFilter, selectedGoldenFromFilter,
            recName, stopDisabled, recStatus, showAddCustomHeader,
            goldenNameErrorMessage
        } = this.state;

        const replayDone = (cube.replayStatus === "Completed" || cube.replayStatus === "Error");
        const analysisDone = (cube.analysisStatus === "Completed" || cube.analysisStatus === "Error");

        return (
            <div>
                {!showGoldenMeta && this.renderTestInfo()}

                {showGoldenMeta && <GoldenMeta {...cube} handleBackToTestInfoClick={this.handleBackToTestInfoClick} />}

                <Modal show={recordModalVisible}>
                    <Modal.Header>
                        <Modal.Title>Record</Modal.Title>
                    </Modal.Header>

                    <Modal.Body className={"text-center padding-15"}>
                        <div style={{ display: "flex", flex: 1, justifyContent: "center"}}>
                            <div className="margin-right-10" style={{ display: "flex", flexDirection: "column" }}>
                                <input placeholder={"Enter Name"} onChange={this.changeRecName} type="text" value={recName}/>
                                
                            </div>
                            <div style={{ display: "flex", alignItems: "flex-start" }}>
                                <span onClick={this.handleStartRecordClick} className={stopDisabled ? "cube-btn margin-right-10" : "cube-btn disabled margin-right-10"}>START</span>
                                <span onClick={this.stopRecord} className={stopDisabled ? "cube-btn disabled" : "cube-btn"}>STOP</span>
                            </div>
                            
                        </div>
                        <div style={{ display: "flex", justifyContent: "center", marginTop: "10px" }}>
                            <span style={{ color: "#c24b4b"}}>{goldenNameErrorMessage}</span>
                        </div>
                        <div className={"padding-15 bold"}>
                            <span className={!recStatus ? "hidden" : ""}>Recording Id: {recStatus ? recStatus.id : ""}</span>&nbsp;&nbsp;&nbsp;&nbsp;
                            Status: {recStatus ? recStatus.status : "Initialize"}
                        </div>
                    </Modal.Body>

                    <Modal.Footer>
                        <span onClick={this.handleCloseRecModal} className={stopDisabled ? "cube-btn" : "cube-btn disabled"}>CLOSE</span>
                    </Modal.Footer>
                </Modal>

                <Modal show={show}>
                    <Modal.Header>
                        <Modal.Title>{cube.testConfig ? cube.testConfig.testConfigName : ''}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div>
                            {
                            !replayDone ? "Test in progress..." : 
                                !analysisDone ? "Analyzing" : "Test Completed"
                            }
                        </div>
                        <h4 style={{color: replayDone ? "green" : "#aab614", fontSize: "medium"}}><strong>Replay:</strong> {cube.replayStatus} {!replayDone && (cube.replayStatusObj ? (<small>{ cube.replayStatusObj.reqsent + '/' + cube.replayStatusObj.reqcnt}</small>) : null)}</h4>
                        <h4 style={{color: replayDone ? (analysisDone ? "green" : "#aab614") : "grey", fontSize: "medium"}}><strong>Analysis:</strong> {cube.analysisStatus}</h4>
                        
                        {cube.replayStatusObj && <p>
                            Replay ID: {cube.replayStatusObj.replayId}
                        </p>}
                    </Modal.Body>
                    <Modal.Footer >
                        {analysisDone ? 
                        <Link to="/">
                            <span onClick={this.handleClose} className="cube-btn">View Results</span>&nbsp;&nbsp;
                        </Link>
                    :
                    <span className="modal-footer-text">The results will be available on the test results page once the test completes</span>
                    }
                        <span onClick={this.handleClose} className="cube-btn">Close</span>
                    </Modal.Footer>
                </Modal>
                
                <Modal show={fcId}>
                    <Modal.Header>
                        <Modal.Title>Force Complete</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <p>
                            A replay with id {fcId} is in progress.
                        </p>
                    </Modal.Body>
                    <Modal.Footer>
                        <span onClick={this.handleFC} className="cube-btn">Force Complete</span>&nbsp;&nbsp;
                        <span onClick={this.handleFCDone} className="cube-btn">Done</span>
                    </Modal.Footer>
                </Modal>

                <Modal show={showGoldenFilter} bsSize="large">
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
                                    <td className="bold" style={{ minWidth: "100px" }}>Label</td>
                                    <td className="bold" style={{ minWidth: "175px" }}>ID</td>
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
                        <span onClick={this.selectHighlighted} className={selectedGoldenFromFilter ? "cube-btn" : "disabled cube-btn"}>Select</span>&nbsp;&nbsp;
                        <span onClick={this.hideGoldenFilter} className="cube-btn">Cancel</span>
                    </Modal.Footer>
                </Modal>
                <Modal show={showAddCustomHeader} bsSize="large">
                    <Modal.Header>
                        <Modal.Title>Add Custom Headers</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {Object.keys(customHeaders).map((headerId, index) => 
                            <div key={headerId} className="add-header-row margin-bottom-10">
                                <input 
                                    placeholder="Header" 
                                    className="add-header-input" 
                                    value={customHeaders[headerId].key}
                                    onChange={(e) => this.handleCustomHeaderKeyChange(e.target.value, headerId)} 
                                />
                                <input 
                                    placeholder="Value" 
                                    className="add-header-input margin-left-15" 
                                    value={customHeaders[headerId].value}
                                    onChange={(e) => this.handleCustomHeaderValueChange(e.target.value, headerId)}
                                />
                                {
                                    (index + 1) === Object.keys(customHeaders).length 
                                    ?
                                    <i className="fas fa-plus pointer" style={{ color: "#757575", marginLeft: "5px" }} aria-hidden="true" onClick={this.addKeyValueInput}></i>
                                    :
                                    <i className="fas fa-close pointer" style={{ color: "#757575", marginLeft: "5px" }} aria-hidden="true" onClick={() => this.removeKeyValueInput(headerId)}></i>
                                }
                            </div>
                            )
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        <span onClick={this.closeAddCustomHeaderModal} className="cube-btn">Add</span>
                        <span onClick={this.cancelAddCustomHeaderModal} className="cube-btn margin-left-15">Cancel</span>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
    authentication: state.authentication
});

export default connect(mapStateToProps)(ViewSelectedTestConfig);