import React, { Fragment } from 'react';
import {Link, withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {cubeActions} from "../actions";
import {cubeConstants} from "../constants";
import Modal from "react-bootstrap/es/Modal";
import config from "../config";
import {getTransformHeaders} from "../utils/lib/url-utils";
import api from "../api";
import {GoldenMeta} from "./Golden-Visibility";
import {GoldenCollectionBrowse} from "./GoldenCollectionBrowse";
// import {GoldenCollectionBrowse} from "./APICatalog";
// import {GoldenCollectionDropdown} from "./GoldenCollectionDropdown";
import {goldenActions} from '../actions/golden.actions'
import {validateGoldenName} from "../utils/lib/golden-utils";
import classNames from "classnames";
import { cubeService } from '../services';
import { apiCatalogActions } from '../actions/api-catalog.actions';
import MDLoading from '../../../public/assets/images/md-loading.gif';
import Tippy from '@tippy.js/react'
import {isURL} from 'validator';

class ViewSelectedTestConfig extends React.Component {
    constructor(props) {
        super(props)
        this.state = {
            panelVisible: true,
            testIdPrefix: '',
            fcId: null,
            fcEnabled: false,
            replayId: null,
            showReplayModal: false,
            showCT: false,
            showDeleteGoldenConfirmation:false,
            showAddCustomHeader: false,
            showGoldenMeta: false,
            showGoldenFilter: false,
            goldenNameFilter: "",
            goldenIdFilter: "",
            goldenBranchFilter: "",
            goldenVersionFilter: "",
            selectedGoldenFromFilter: "",
            dbWarningModalVisible: false,
            instanceWarningModalVisible: false,
            goldenSelectWarningModalVisible: false,
            resumeModalVisible: false,
            recordModalVisible: false,
            recStatus: null,
            recName: "",
            recLabel:"",
            recId: null,
            stopDisabled: true,
            stoppingStatus: false,
            forceStopping: false,
            goldenNameErrorMessage: "",
            recordingMode: "new", //allowed values ["new", "resume"]
            userAlertMessage: {
                header: "",
                message: ""
            },
            customHeaders: {
                default: {
                    key: "",
                    value: ""
                }
            },
            fetchingRecStatus: false,
            showOngoingRecModal: false,
            ongoingRecStatus: {},
            forceStopped: false,
            otherInstanceEndPoint: "",
            storeToDatastore: true,
        };
        //this.statusInterval;
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.clearPreviousData());
    }

    handleRecordingModeChange = (value) => this.setState({ recordingMode: value, goldenNameErrorMessage: "" });
  
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

    changeRecName = (e) => this.setState({recName: e.target.value.replace(/  /g, " ")});

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
        const { dispatch, cube } = this.props;
        cube.selectedTestId = e.target.value;
        if (e) {
            dispatch(cubeActions.clearPreviousData());
            let version = null;
            let golden = null;
            let name = "";
            for (const collec of cube.testIds) {
                if (collec.collec == e.target.value) {
                    golden = collec.id
                    version = collec.templateVer;
                    name = collec.name;
                    break;
                }
            }
            //dispatch(cubeActions.getGraphData(cube.selectedApp));
            dispatch(cubeActions.setSelectedTestIdAndVersion(e.target.value, version, golden, name));
        }
    };

    handleChangeInBrowseCollection = (selectedCollectionObject) => {
        const { dispatch } = this.props;
        const { 
            name,
            id: golden,
            templateVer: version,
            collec: collectionId 
        } = selectedCollectionObject;

        dispatch(cubeActions.clearPreviousData());
        dispatch(cubeActions.setSelectedTestIdAndVersion(collectionId, version, golden, name));
    };
    
    showCT = () => this.setState({showCT: true});

    handleClose = () => {
        const {cube} = this.props;
        this.handleChangeForTestIds({target: {value: cube.selectedTestId}});
        this.setState({ showReplayModal: false, showCT: false });
    };

    selectGoldenFromFilter = (g) => {
        this.setState({selectedGoldenFromFilter: g});
    };

    applyGoldenFilter = (filter, event) => {
        this.setState({[filter] : event.target.value});
    };

    handleRecordButtonClick = () => {
        const { recordingMode } = this.state;
        const { cube } = this.props;

        if(!cube.selectedInstance) {
            this.showInstanceWarningModal();
            return;
        } 
        if(cube.selectedInstance == "other") {
            this.showPredefinedInstanceWarningModal();
            return;
        } 

        const mode = {
            new: () => this.showRecordModal(),
            resume: () => this.showResumeModal()
        };

        mode[recordingMode]();
    };

    showDBWarningModal = () => this.setState({
        dbWarningModalVisible: true,
        userAlertMessage: {
            header: "Reminder",
            message: "You are about to start/resume a recording. Please reset the db state before proceeding."
        }
    })

    showInstanceWarningModal = () => this.setState({ 
        instanceWarningModalVisible: true,
        userAlertMessage: {
            header: "Alert",
            message: "Select an Instance to proceed."
        }
    });

    showPredefinedInstanceWarningModal = () => this.setState({ 
        instanceWarningModalVisible: true,
        userAlertMessage: {
            header: "Alert",
            message: "Select a predefined instance to proceed."
        }
    });

    showGoldenWarningForRunTest =  () => this.setState({
        goldenSelectWarningModalVisible: true,
        userAlertMessage: {
            header: "Alert",
            message: "Select a Golden to Run Test."
        }
    });

    showGatewayEndPointUnavailable =  () => this.setState({
        goldenSelectWarningModalVisible: true,
        userAlertMessage: {
            header: "Alert",
            message: "Gateway endpoint is unavailable."
        }
    });

    showGatewayEndPointInvalid =  () => this.setState({
        goldenSelectWarningModalVisible: true,
        userAlertMessage: {
            header: "Alert",
            message: "Gateway endpoint is invalid url."
        }
    });

    showGoldenWarningModal = () => this.setState({ 
        goldenSelectWarningModalVisible: true,
        userAlertMessage: {
            header: "Alert",
            message: "Select a Golden to resume recording."
        }
    });

    showRecordModal = () => {
        const { cube } = this.props;
        if(!cube.selectedInstance) {
            this.showInstanceWarningModal();
            return;
        } 

        this.setState({ recordModalVisible: true });

    };

    showResumeModal = () => {
        const { cube } = this.props;

        if(!cube.selectedInstance) {
            this.showInstanceWarningModal();
            return;
        } 
        
        if(!cube.selectedGolden){
            this.showGoldenWarningModal();
            return;
        } 
        
        this.setState({ resumeModalVisible: true});

    }

    handleCloseRecModal = () => {
        this.setState({
            recordModalVisible: false, 
            resumeModalVisible: false,
            recStatus: null
        });
    };

    handleViewGoldenClick = () => {
        const { dispatch } = this.props;
        
        this.setState({ showGoldenMeta: true });
        dispatch(cubeActions.hideGoldenVisibility(false))
    };

    handleBackToTestInfoClick = () => {
        const { dispatch } = this.props;

        this.setState({ showGoldenMeta: false });

        dispatch(cubeActions.hideGoldenVisibility(true));
        dispatch(goldenActions.resetGoldenVisibilityDetails());
    };

    hideGoldenFilter = () => {
        this.setState({showGoldenFilter: false});
    };

    handleDismissCallBack = () => {
        const { recordingMode } = this.state;

        const mode = {
            new: () => this.handleStartRecordClick(),
            resume: () => this.resumeRecording()
        };

        mode[recordingMode]();
    };

    handleDBWarningModalDismissClick = () => {
        this.setState({ 
            dbWarningModalVisible: false,
            userAlertMessage: {
                header: "",
                message: ""
            }
        }, this.handleDismissCallBack);
    }
        

    handleInstanceSelectDismissClick = () => 
        this.setState({ 
            instanceWarningModalVisible: false,
            userAlertMessage: {
                header: "",
                message: ""
            }
        });
    
    handleGoldenSelectDismissClick = () => 
        this.setState({ 
            goldenSelectWarningModalVisible: false,
            userAlertMessage: {
                header: "",
                message: ""
            }
        });

    
    handleStartRecordClick = () => {
        
        const { recName } = this.state;

        const { goldenNameIsValid, goldenNameErrorMessage } = validateGoldenName(recName);

        this.setState({ goldenNameErrorMessage });

        if(goldenNameIsValid) {
            this.startRecord();
        }
    };

    handleRunTestClick = () => {
        const { cube } = this.props;

        const instancesForSelectedApp = cube.instances
            .filter((item) => item.name == cube.selectedInstance && item.app.name == cube.selectedApp);

        if(!cube.selectedInstance) {
            this.showInstanceWarningModal();
            return;
        }

        if (!cube.selectedTestId) {
            this.showGoldenWarningForRunTest();
            return;
        }
        let gatewayEndpoint = "";
        if(instancesForSelectedApp.length !== 0) {
            gatewayEndpoint = instancesForSelectedApp[0].gatewayEndpoint;            
        }else if(cube.selectedInstance == "other" && this.state.otherInstanceEndPoint){
            if(isURL(this.state.otherInstanceEndPoint)){
                gatewayEndpoint = this.state.otherInstanceEndPoint;
            }else{
                this.showGatewayEndPointInvalid();
                return;
            }
        }else{
            this.showGatewayEndPointUnavailable();
            return;
        }

        // When the conditions above are met
        // then trigger replay
        this.setState({ showReplayModal: true });
        this.replay(gatewayEndpoint);
    };

    handleReplayErrorCatchAll = (message, statusText) => {
        this.setState({ showReplayModal: false });
        alert(message || statusText);
    }

    handleReplayError = (data, status, statusText, username) => 
        (
            status && status === 409 && data.recordOrReplay?.replay
            ?
                this.setState({ 
                    fcId: data.recordOrReplay.replay.replayId, 
                    fcEnabled: (data.recordOrReplay.replay.userId === username), 
                    showReplayModal: false
                })
                
            : this.handleReplayErrorCatchAll(data["message"], statusText)
        );

    checkStatus = (statusUrl, configForHTTP) => {
        this.setState(
            {
                fetchingRecStatus: true
            }, () => {
                api.get(statusUrl, configForHTTP)
                    .then(
                        data => this.setState({ 
                            fetchingRecStatus: false, 
                            recStatus: data 
                        }), 
                        (err) => this.setState({fetchingRecStatus: false})
                    )
            }
        )
    };

    resumeRecording = () => {
        const { 
            cube: { 
                testIds,
                selectedApp, 
                selectedGolden,
                testConfig: { 
                    tag
                }
            }, 
            authentication: { 
                user: {
                    customer_name,
                    access_token
                } 
            }, 
            dispatch 
        } = this.props;

        const { name: recName, label: recLabel } = testIds.find(recording => recording.id === selectedGolden);
        
        const searchParams = new URLSearchParams();
        searchParams.set('tag', `default${selectedApp}Record`);
        searchParams.set('resettag', `default${selectedApp}Noop`);

        const resumeUrl = `${config.recordBaseUrl}/resumeRecording/${selectedGolden}`;
        const statusUrl = `${config.recordBaseUrl}/status/${customer_name}/${selectedApp}/${recName}/${recLabel}`;

        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                // "Authorization": `Bearer ${access_token}`
            }
        };

        api.post(resumeUrl, searchParams, configForHTTP).then((data) => {
            this.setState({ stopDisabled: false, recId: data.id, recName, recLabel });
            this.recStatusInterval = setInterval(
                () => {
                    if(this.state.recStatus?.status === "Completed") { // in case it's stopped externally
                        this.setState({stopDisabled: true, stoppingStatus: false, forceStopping: false});
                        clearInterval(this.recStatusInterval);
                        dispatch(cubeActions.getTestIds(selectedApp));
                        dispatch(apiCatalogActions.fetchGoldenCollectionList(selectedApp, "Golden"));
                    } else if(!this.state.recId) {
                        clearInterval(this.recStatusInterval) 
                    } else if(!this.state.fetchingRecStatus) {
                        this.checkStatus(statusUrl, configForHTTP)
                    }
                }, 
                1000);
        });

    };

    startRecord = () => {
        const { 
            cube: { 
                selectedApp, 
                selectedInstance 
            }, 
            authentication: { 
                user: {
                    username,
                    customer_name,
                    access_token
                } 
            },
            dispatch 
        } = this.props;
        const { recName } = this.state;
        const recLabel = Date.now().toString();

        const recordUrl = `${config.recordBaseUrl}/start/${customer_name}/${selectedApp}/${selectedInstance}/Default${selectedApp}`;
        const statusUrl = `${config.recordBaseUrl}/status/${customer_name}/${selectedApp}/${recName}/${recLabel}`;

        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                // "Authorization": `Bearer ${access_token}` //"Bearer " + user['access_token']
            }
        };
        
        const searchParams = new URLSearchParams();

        searchParams.set('name', recName);
        searchParams.set('userId', username);
        searchParams.set('label', recLabel);
        searchParams.set('tag', `default${selectedApp}Record` );
        searchParams.set('resettag', `default${selectedApp}Noop`);

        // axios.post(recordUrl, searchParams, configForHTTP
        api.post(recordUrl, searchParams, configForHTTP)
        .then((data) => {
            this.setState({ stopDisabled: false, recId: data.id, recLabel });
            this.recStatusInterval = setInterval(
                () => {
                    if(this.state.recStatus?.status === "Completed") { // in case it's stopped externally
                        this.setState({stopDisabled: true, stoppingStatus: false, forceStopping: false});
                        clearInterval(this.recStatusInterval);
                        dispatch(cubeActions.getTestIds(selectedApp));
                        dispatch(apiCatalogActions.fetchGoldenCollectionList(selectedApp, "Golden"));
                    } else if(!this.state.recId) {
                        clearInterval(this.recStatusInterval) 
                    } else if(!this.state.fetchingRecStatus) {
                        this.checkStatus(statusUrl, configForHTTP)
                    }
                }, 
                1000);
            }, (error) => {
                if(error.response.status==409) {
                    const ongoingRecStatus = error.response.data
                    this.setState({ongoingRecStatus, showOngoingRecModal: true})
                } else {
                    console.error("Errror starting recording" ,error);
                }
            });
    };

    stopRecord = () => {
        const { 
            dispatch,
            cube: { 
                selectedApp, 
            }, 
            authentication: { 
                user: {
                    customer_name,
                    access_token
                } 
            } 
        } = this.props;

        const { recName, recLabel } = this.state;

        const stopUrl = `${config.recordBaseUrl}/stop/${this.state.recId}`;
        const statusUrl = `${config.recordBaseUrl}/status/${customer_name}/${selectedApp}/${recName}/${recLabel}`;

        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                "Authorization": `Bearer ${access_token}`
            }
        };
        const searchParams = new URLSearchParams();
        searchParams.set('resettag', `default${selectedApp}Noop`);

        // axios.post(stopUrl, {}, configForHTTP)
        api.post(stopUrl, searchParams, configForHTTP).then(() => {
            this.setState({ recId: null, stoppingStatus: true});
            this.stopStatusInterval = setInterval(
                () => { 
                    if(this.state.recStatus.status === "Completed") {
                        this.setState({stopDisabled: true, stoppingStatus: false, forceStopping: false});
                        clearInterval(this.stopStatusInterval);
                        dispatch(cubeActions.getTestIds(selectedApp));
                        dispatch(apiCatalogActions.fetchGoldenCollectionList(selectedApp, "Golden"));
                    } else if(!this.state.fetchingRecStatus) {
                        this.checkStatus(statusUrl, configForHTTP)
                    }
                }, 
                1000);
        });

    };

    replay = async (gatewayEndpoint) => {
        const { 
            cube: {
                selectedInstance,
                collectionTemplateVersion,
                selectedGolden,
                selectedApp, 
                testConfig: { 
                    testPaths, 
                    testMockServices, 
                    testConfigName,
                    dynamicInjectionConfigVersion,
                    tag
                }
            }, 
            authentication: { 
                user: { 
                    username 
                } 
            }, 
            checkReplayStatus 
        } = this.props;
        const replayStartUrl = `${config.replayBaseUrl}/start/${selectedGolden}`;

        const transforms = JSON.stringify(getTransformHeaders(this.state.customHeaders));

        const searchParams = new URLSearchParams();

        searchParams.set('endPoint', gatewayEndpoint);
        searchParams.set('instanceId', selectedInstance == "other" ? gatewayEndpoint : selectedInstance);
        searchParams.set('templateSetVer', collectionTemplateVersion);
        searchParams.set('userId', username);
        searchParams.set('transforms', transforms);
        searchParams.set('testConfigName', testConfigName);
        searchParams.set('analyze', true);
        if(selectedInstance == "other"){
            searchParams.set('storeToDatastore', this.state.storeToDatastore.toString());
        }
        if(tag){
            searchParams.set('tag', tag);
            searchParams.set('resettag', `default${selectedApp}Noop`);
        }

        // TODO: Change this to proper application config. Remove it after the demo @Sam
        if(selectedApp === 'grpc') {
            searchParams.set('replayType', 'GRPC');
        }

        // Append dynamic injection configuration if available
        dynamicInjectionConfigVersion && searchParams.set('dynamicInjectionConfigVersion', dynamicInjectionConfigVersion);
        // Append mock services
        testMockServices && testMockServices.length != 0 &&
            testMockServices.map(testMockService => searchParams.append('mockServices',testMockService))
        // Append Test Paths, If not specified, it will run all paths
        testPaths && testPaths.length !== 0 &&
            testPaths.map(path => searchParams.append("paths", path))

        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            }
        };

        try {
            const data = await api.post(replayStartUrl, searchParams, configForHTTP);
            this.setState({ replayId: data });
            // check replay status periodically and call analyze at the end; and update timeline
            // this method is run in the parent component (Navigation)
            checkReplayStatus(this.state.replayId.replayId);
        } catch(error) {
            const { data, status, statusText } = error.response;
            this.handleReplayError(data, status, statusText, username);
        }
    };
    
    handleForceStopRecording = async (recordingId) => {
        try {

            const {cube: { 
                selectedApp, 
            }, dispatch} = this.props;
            const searchParams = new URLSearchParams();
            searchParams.set('resettag', `default${selectedApp}Noop`);

            this.setState({forceStopping: true})
            await cubeService.forceStopRecording(recordingId, searchParams)
            this.setState({forceStopped: true, forceStopping: false})
            dispatch(cubeActions.getTestIds(selectedApp));
            dispatch(apiCatalogActions.fetchGoldenCollectionList(selectedApp, "Golden"));
        } catch (error) {
            console.error("Unable to force stop recording: " + error)
            alert("Unable to force stop recording")
            this.setState({forceStopping: false})
        }
    }

    handleCloseOngoingRecModal = () => {
        this.setState({showOngoingRecModal: false, forceStopped: false})
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
                <select id="ddlInstance" className="r-att" onChange={this.handleChangeForInstance} value={cube.selectedInstance || ""} placeholder={'Select...'}>
                    <option value="">Select Instance</option>
                    {options}
                    <option value="other">Other</option>
                </select>
            </div>
        } else {
            jsxContent = <select id="ddlInstance" className="r-att" value={cube.selectedInstance} placeholder={'Select...'}>
                <option value="">Select Instance</option>
            </select>
        }

        return jsxContent;
    }

    onOtherInstanceValueChange = (event)=>{
        this.setState({otherInstanceEndPoint: event.target.value});
    }

    renderEndPoint(cube) {
        if(cube.selectedInstance == "other"){
            return <input type="text" onChange={this.onOtherInstanceValueChange} value={this.state.otherInstanceEndPoint}  style={{width: "100%"}} />
        }else{
            const selectedInstance = cube.instances.find(item => {
                return item.app.name == cube.selectedApp && item.name == cube.selectedInstance;
            });
            const currentEndpoint = selectedInstance ? selectedInstance.gatewayEndpoint : "";
            return  <input disabled type="text" value={currentEndpoint} style={{width: "100%"}} />
        }
    }

    onStoreToDatabaseChange = (event) =>{
        this.setState({storeToDatastore: event.target.checked});
    }

    renderStoreToDatastore(cube){
        if(cube.selectedInstance == "other"){
            return <div className="margin-top-10">
                <label className="label-n"><input type="checkbox" onChange={this.onStoreToDatabaseChange} checked={this.state.storeToDatastore}/>
                &nbsp; STORE TO DATASTORE </label>
                
            </div>
        }else{
            return <></>
        }
    }

    renderRecordingInfo = () => {
        const { cube: { selectedGolden, testIds }, gcbrowse: { actualGoldens }} = this.props;

        if (selectedGolden && testIds.length !== 0) {
            const { id, label, name } = testIds.find(test => test.id === selectedGolden) 
                                        || actualGoldens.recordings.find(test => test.id === selectedGolden);

            return(
                <div className="resume-modal-info-container">
                    <div className="resume-modal-info-line">
                        <div className="resume-modal-identifier"><b>Id:</b></div>
                        <div className="resume-modal-content">{id}</div>
                    </div>
                    <div className="resume-modal-info-line">
                        <div className="resume-modal-identifier"><b>Name:</b></div>
                        <div className="resume-modal-content">{name}</div>
                    </div>
                    <div className="resume-modal-info-line"s>
                        <div className="resume-modal-identifier"><b>Label:</b></div>
                        <div className="resume-modal-content">{label}</div>
                    </div>
                </div>
            );

        }

        return null;        
    }

    renderAlertModals = (isVisible, dismissHandler) => {
        const { userAlertMessage: { header, message } } = this.state;
        return (
            <Modal show={isVisible}>
                <Modal.Header>
                    <Modal.Title>{header}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>
                        {message}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <span onClick={dismissHandler} className="cube-btn">Dismiss</span>
                </Modal.Footer>
            </Modal>
        );
    };


    renderTestInfo = () => {
        const { cube, authentication: { user: { username } } } = this.props;
        const { recStatus } = this.state;
        
        return(
            <Fragment>
                <div className="div-label">
                    Test Configuration
                    <Link to="/configs">
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
                <div className="test-config-divider" />

                {/* 
                // Hiding this for now. Maybe used later
                <div className="margin-top-10">
                    <div className="cube-btn width-100 text-center">SAVE TEST CONFIG</div>
                </div> 
                */}

                <div className="margin-top-10">
                    <div className="label-n">SELECT TEST INSTANCE</div>
                    <div className="value-n">
                        {this.renderInstances(cube)}
                    </div>
                </div>

                <div className="margin-top-10">
                    <div className="label-n">GATEWAY END POINT</div>
                    <div className="value-n">
                        {this.renderEndPoint(cube)}
                    </div>
                </div>

                {this.renderStoreToDatastore(cube)}
                
                <div className="margin-top-10">
                    <div className="label-n">SELECT RECORD MODE&nbsp;
                        <select  id="ddlRecordMode" 
                            className="r-att" 
                            style={{ fontSize: "12px", fontWeight: "500", color: "#5f5f5f" }} 
                            onChange={(event) => this.handleRecordingModeChange(event.target.value)}
                            value={this.state.recordingMode}
                        >
                            <option value="new">New</option>
                            <option value="resume">Resume</option>
                        </select>
                    </div>
                </div>
                <GoldenCollectionBrowse 
                    showDeleteOption
                    selectedSource="Golden" 
                    dropdownLabel="SELECT GOLDEN"
                    handleViewGoldenClick={this.handleViewGoldenClick}
                    handleChangeCallback={this.handleChangeInBrowseCollection}
                    showVisibilityOption={(!recStatus || recStatus.status !== "Running")}
                />
                <div style={{ fontSize: "12px" }} className="margin-top-10 row">
                    <span  className="label-link col-sm-12 pointer" onClick={this.showAddCustomHeaderModal}>
                        <i className="fas fa-plus" style={{ color: "#333333", marginRight: "5px" }} aria-hidden="true"></i>
                        Custom Headers
                    </span>
                </div>

                <div className="margin-top-10 row">
                    <div className="col-sm-6">
                        <div onClick={this.handleRunTestClick} id="btnRunTest" className="cube-btn width-100 text-center">RUN TEST</div>
                    </div>
                    <div className="col-sm-6"><div onClick={this.handleRecordButtonClick} id="btnRecord"  className="cube-btn width-100 text-center">RECORD</div></div>
                </div>
                <div className="test-config-divider" />
                <div className="margin-top-10 row">
                    <div className="col-sm-12">
                        <Link to="/test_config_view/test_cluster">
                            <div className="cube-btn width-100 text-center">TEST CLUSTER STATUS</div>
                        </Link>
                    </div>
                </div>

            </Fragment>
        );

    };

    renderTestClusterPanel = () => (
        <div className="margin-top-10 row">
            <div className="col-sm-12">
                <Link to="/test_config_view">
                    <div className="cube-btn width-100 text-center">BACK TO TEST CONFIG</div>
                </Link>
            </div>
        </div>
    );

    renderGoldenMeta = () => {
        const { cube } = this.props;
        return (<GoldenMeta {...cube} handleBackToTestInfoClick={this.handleBackToTestInfoClick} />);
    }

    renderLeftPanelInfo = () => {
        const { location: { pathname }} = this.props;
        
        const panel = {
            ['/test_config_view']: () => this.renderTestInfo(),
            ['/test_config_view/golden_visibility']: () => this.renderGoldenMeta(),
            ['/test_config_view/test_cluster']: () => this.renderTestClusterPanel()
        };

        return panel[pathname] ? panel[pathname]() : (<div />);
    };

    render() {
        const { cube } = this.props;
        const { 
            customHeaders, recordModalVisible, showReplayModal, 
            fcId, showGoldenFilter, selectedGoldenFromFilter,
            recName, stopDisabled, stoppingStatus, recStatus, showAddCustomHeader,
            goldenNameErrorMessage, fcEnabled, resumeModalVisible,
            dbWarningModalVisible, instanceWarningModalVisible, 
            goldenSelectWarningModalVisible, showDeleteGoldenConfirmation, forceStopping, forceStopped, ongoingRecStatus
        } = this.state;

        const replayDone = (cube.replayStatus === "Completed" || cube.replayStatus === "Error");
        const analysisDone = (cube.analysisStatus === "Completed" || cube.analysisStatus === "Error");
        const ongoingRecording = ongoingRecStatus.recordOrReplay?.recording || {}
        return (
            <div>
                {this.renderLeftPanelInfo()}
                
                {dbWarningModalVisible && this.renderAlertModals(dbWarningModalVisible, this.handleDBWarningModalDismissClick)}
                
                {instanceWarningModalVisible && this.renderAlertModals(instanceWarningModalVisible, this.handleInstanceSelectDismissClick)}

                {goldenSelectWarningModalVisible && this.renderAlertModals(goldenSelectWarningModalVisible, this.handleGoldenSelectDismissClick)}

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
                                <span onClick={this.showDBWarningModal} className={stopDisabled ? "cube-btn margin-right-10" : "cube-btn disabled margin-right-10"}>START</span>
                                <span onClick={this.stopRecord} className={stopDisabled || stoppingStatus ? "cube-btn disabled" : "cube-btn"}>STOP</span>
                            </div>
                            
                        </div>
                        <div style={{ display: "flex", justifyContent: "center", marginTop: "10px" }}>
                            <span style={{ color: "#c24b4b"}}>{goldenNameErrorMessage}</span>
                        </div>
                        
                        {
                            stoppingStatus &&
                            <div>
                                <img src={MDLoading} alt="Stopping..."/>   
                                <br />
                                {forceStopping ? <span>Force stopping</span> : <span>Please wait for 15 seconds to complete recording.</span>}
                            </div>
                        }
                        <div className={"padding-15 bold"}>
                            <span className={!recStatus ? "hidden" : ""}>Recording Id: {recStatus ? recStatus.id : ""}</span>&nbsp;&nbsp;&nbsp;&nbsp;
                            Status: {recStatus ? (stoppingStatus ? "Stopping": recStatus.status) : "Initialize"}
                        </div>
                    </Modal.Body>

                    <Modal.Footer>
                        <span onClick={() => this.handleForceStopRecording(recStatus.id)} className={classNames("cube-btn","pull-left", {"hidden" : !stoppingStatus, "disabled" : forceStopping})}>FORCE STOP</span>&nbsp;&nbsp;

                        <span onClick={this.handleCloseRecModal} className={stopDisabled ? "cube-btn" : "cube-btn disabled"}>CLOSE</span>
                    </Modal.Footer>
                </Modal>

                <Modal show={resumeModalVisible}>
                    <Modal.Header>
                        <Modal.Title>Resume Recording</Modal.Title>
                    </Modal.Header>
                    <Modal.Body className={"text-center padding-15"}>
                        <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                            <div 
                                className="margin-right-10" 
                                style={{ display: "flex", flexDirection: "column", justifyContent: "center", fontWeight: "500", width: "50%" }}
                            >
                                {this.renderRecordingInfo()}
                            </div>
                            <div className="margin-top-10" style={{ display: "flex", alignItems: "flex-start" }}>
                                    <span onClick={this.showDBWarningModal} className={stopDisabled ? "cube-btn margin-right-10" : "cube-btn disabled margin-right-10"}>RESUME</span>
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

                <Modal show={showReplayModal}>
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
                        <h4 style={{color: replayDone ? "green" : "#aab614", fontSize: "medium"}}><strong>Replay:</strong> {replayDone? 'Complete' : cube.replayStatus} {!replayDone && (cube.replayStatusObj ? (<small>{ cube.replayStatusObj.reqsent + '/' + cube.replayStatusObj.reqcnt}</small>) : null)}</h4>
                        <h4 style={{color: replayDone ? (analysisDone ? "green" : "#aab614") : "grey", fontSize: "medium"}}><strong>Analysis:</strong> {cube.analysisStatus}</h4>
                        
                        {cube.replayStatusObj && <p>
                            Replay ID: {cube.replayStatusObj.replayId}
                        </p>}
                    </Modal.Body>
                    <Modal.Footer >
                        {analysisDone ? 
                        <Link to="/test_results">
                            <span onClick={this.handleClose} id="btnRunTestViewResults" className="cube-btn">View Results</span>&nbsp;&nbsp;
                        </Link>
                    :
                    <span className="modal-footer-text">The results will be available on the test results page once the test completes</span>
                    }
                        <span onClick={this.handleClose} className="cube-btn">Close</span>
                    </Modal.Footer>
                </Modal>
                
                <Modal show={fcId}>
                    <Modal.Header>
                        <Modal.Title>Cluster currently in use</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <p>
                            A replay with id {fcId} is in progress on this cluster. Please use another cluster or try again later.
                        </p>
                    </Modal.Body>
                    <Modal.Footer>
                        <span onClick={this.handleFC} className={classNames("cube-btn","pull-left", {"disabled" : !fcEnabled})}>Force Complete</span>&nbsp;&nbsp;
                        <span onClick={this.handleFCDone} className="cube-btn pull-right">Done</span>
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
                <Modal show={showDeleteGoldenConfirmation}>
                    <Modal.Body>
                        <div style={{ display: "flex", flex: 1, justifyContent: "center"}}>
                            <div className="margin-right-10" style={{ display: "flex", flexDirection: "column", fontSize:20 }}>
                                This will delete the {cube.selectedGoldenName}. Please confirm.
                            </div>
                            <div style={{ display: "flex", alignItems: "flex-start" }}>
                                    <span className="cube-btn margin-right-10" onClick={() => this.deleteGolden()}>Confirm</span>
                                    <span className="cube-btn" onClick={() => this.closeDeleteGoldenConfirm()}>No</span>
                            </div>
                        </div>
                    </Modal.Body>
                </Modal>
                <Modal show={this.state.showOngoingRecModal}>
                    <Modal.Header>
                        Ongoing recording
                    </Modal.Header>
                    <Modal.Body>
                        <p>
                            There is an ongoing recording in the selected instance. Please select another instance or wait for the recording to complete and try again later.
                        </p>
                        <div style={{display: "flex", flexDirection: "column"}}>
                        <span><label>Golden Name: </label><span>&nbsp;{ongoingRecording?.name || "N/A"}</span></span>
                        <span><label>Started by: </label><span>&nbsp;{ongoingRecording?.userId || "N/A"}</span></span>
                        <span><label>Recording ID: </label><span>&nbsp;{ongoingRecording?.id || "N/A"}</span></span>
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                            <div className="pull-left">
                                <Tippy content="Please coordinate with the owner of the recording before force stopping" arrow={true} placement="6">
                                    <span onClick={() => this.handleForceStopRecording(ongoingRecording.id)} className={classNames("cube-btn", {"disabled" : forceStopping || forceStopped})}>
                                        <i className={classNames("fa", !forceStopping ? "fa-exclamation-triangle" : "fa-spinner fa-spin")}></i>
                                        &nbsp; {!forceStopped ? "FORCE STOP" : "STOPPED"}
                                    </span>
                                </Tippy>
                            </div>
                        <span onClick={this.handleCloseOngoingRecModal} className="cube-btn">CLOSE</span>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
    gcbrowse: state.gcbrowse,
    authentication: state.authentication
});

export default withRouter(connect(mapStateToProps)(ViewSelectedTestConfig));