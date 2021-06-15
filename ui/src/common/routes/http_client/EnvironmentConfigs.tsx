/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { Component, Fragment } from 'react'
import { connect } from "react-redux";
import _ from "lodash";
import Tippy from '@tippy.js/react';
import { Modal, Grid, Row, Col, Checkbox, Tabs, Tab, FormControl, Glyphicon } from 'react-bootstrap';
import EnvVar from "./EnvVar";
import { getCurrentEnvironment } from "../../utils/http_client/envvar";
import { getCurrentMockConfig } from "../../utils/http_client/utils";
import { httpClientActions } from '../../actions/httpClientActions';
import { IHttpClientStoreState, IStoreState, IEnvironmentConfig, IUserAuthDetails, IMockConfig, ICubeState, IMockConfigValue, IConfigVars } from '../../reducers/state.types';
import commonUtils from '../../utils/commonUtils';
import { isTrueOrUndefined } from '../../utils/http_client/httpClientUtils';
import commonConstants from '../../utils/commonConstants';

export interface IMockConfigsState{
    selectedEditMockConfig: any, //Need to verify type: {name: string; serviceConfigs: []}
    selectedEditMockConfigId: number | null,
    addNewMockConfig: boolean,
    addNewEnv: boolean,
    selectedTabKey: number;
    selectedEnv: IEnvironmentConfig,
    sessionVars: IConfigVars[],
}
export interface IMockConfigsProps{
    httpClient: IHttpClientStoreState;
    tabIndexForEdit: number;
    user: IUserAuthDetails;
    cube: ICubeState;
    dispatch: any;
    hideModal: () => void;
    addServiceName? : string;
}
class EnvironmentConfigs extends Component<IMockConfigsProps, IMockConfigsState> {
    constructor(props: IMockConfigsProps) {
        super(props)
        this.state = {
            selectedEditMockConfig: {},
            selectedEditMockConfigId: null,
            addNewMockConfig: false,
            selectedTabKey : 0,
            selectedEnv: { name:"", appId: 0, vars: [] },
            sessionVars: [],
        }
    }

    componentDidMount() {
        const {
            httpClient: {
                showEnvList,
                environmentList,
                selectedEnvironment,
                showMockConfigList,
                mockConfigList,
                selectedMockConfig,
                contextMap
            },
            tabIndexForEdit,
        } = this.props;

        const { selectedEnv, selectedEditMockConfig  } = this.state;
        const {sessionVars, } = this.state;

        Object.entries(contextMap || {}).forEach(([k, data]) => {
            sessionVars.push({key: k, value: data["value"].toString()});
        });
        this.setState({ sessionVars: sessionVars});

        

        if(!showEnvList && selectedEnv?.appId === 0 && tabIndexForEdit === 0) {
            
            const currentEnvironment: IEnvironmentConfig = getCurrentEnvironment(
                environmentList,
                selectedEnvironment
            );
            
            // this.updateEnvState(currentEnvironment, false)
            this.setState({ selectedEnv: currentEnvironment, addNewEnv: false });
        }

        if(!showMockConfigList && _.isEmpty(selectedEditMockConfig) && tabIndexForEdit === 1) {
            const currentMockConfig: IMockConfigValue = getCurrentMockConfig(
                mockConfigList,
                selectedMockConfig
            );


            const currentMockConfigObject: any = mockConfigList.find(eachMockConfig => eachMockConfig.key === selectedMockConfig);
            if(currentMockConfigObject){
                if(this.props.addServiceName){
                    currentMockConfig.serviceConfigs.push({
                        isMocked: false,
                        service: this.props.addServiceName,
                        url: "",
                        servicePrefix: ""
                    })
                }
                this.setState({ 
                    addNewMockConfig: false, 
                    selectedEditMockConfig: currentMockConfig, 
                    selectedEditMockConfigId: currentMockConfigObject.id
                });
            }else{
                this.handleAddNewMockConfig()
            }
        }

        this.handleSelectedTabChange(tabIndexForEdit);
    }

    handleSelectedTabChange = (changedKey) => {
        this.setState({ selectedTabKey: changedKey });
    }
    
    handleMockConfigRowClick = (index) => {
        const {httpClient: {
            mockConfigList
        }} = this.props;
        this.showMockConfigList(false)
        // parse the value json string, attach id from the parent object
        const selectedEditMockConfig = {...JSON.parse(mockConfigList[index].value)}
        this.setState({selectedEditMockConfig: selectedEditMockConfig, addNewMockConfig: false, selectedEditMockConfigId: mockConfigList[index].id})
    }

    handleServiceChange = (e, index) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs[index].service = e.target.value;
        this.setState({selectedEditMockConfig})
        this.setMockConfigStatusText("", false)
    }

    handleTargetURLChange = (e, index) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs[index].url = e.target.value;
        this.setState({selectedEditMockConfig})
        this.setMockConfigStatusText("", false)
    }

    handleIsMockedCheckChange = (index) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs[index].isMocked = !selectedEditMockConfig.serviceConfigs[index].isMocked;
        this.setState({selectedEditMockConfig})
        this.setMockConfigStatusText("", false)
    }

    handleServicePrefixChange = (e, index) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs[index].servicePrefix = e.target.value;
        this.setState({selectedEditMockConfig})
        this.setMockConfigStatusText("", false)
    }

    handleSelectedMockConfigNameChange = (e) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.name = e.target.value;
        this.setState({selectedEditMockConfig})
        this.setMockConfigStatusText("", false)
    }

    handleAddNewMockConfig = () => {
        let selectedEditMockConfig = {
            name: this.props.addServiceName || "",
            serviceConfigs: this.props.addServiceName ? [{
                service: this.props.addServiceName, 
                servicePrefix: "", 
                url: "", 
                isMocked: false,
            }] : []
        }
        this.showMockConfigList(false)
        this.setState({selectedEditMockConfig, addNewMockConfig: true})
    }

    showMockConfigList = (show: boolean) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.showMockConfigList(show));
    }

    handleAddNewServiceConfig = () => {
        let {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs.push({
            service: "", 
            servicePrefix: "", 
            url: "", 
            isMocked: false,
        })
        this.setState({selectedEditMockConfig})
        this.setMockConfigStatusText("", false)
    }

    handleRemoveMockConfig = (index) => {
        const {
            dispatch,
            httpClient: {
                mockConfigList
            }
        } = this.props;
        const {id, key} = mockConfigList[index];
        dispatch(httpClientActions.removeMockConfig(id, key))
    }

    handleRemoveServiceConfig = (index: Number) => {
        let {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs.splice(index, 1)
        this.setMockConfigStatusText("", false)
        this.setState({selectedEditMockConfig})
    }

    validateMockConfig = (mockConfig) => {
        const {name, serviceConfigs} = mockConfig;
        if (_.isEmpty(name)) {
            this.setMockConfigStatusText("Configuration name cannot be empty", true)
            return false
        }

        for(const {service, url, isMocked} of serviceConfigs) {
            if(!service) {
                this.setMockConfigStatusText("Service name cannot be empty", true)
                return false
            }

            if(!isMocked) {
                if(!url) {
                    this.setMockConfigStatusText("Target URL is required for non-mocked service '" + service + "'", true)
                    return false
                }

                // validate url
                try {
                    new URL(url);
                } catch (_) {
                    this.setMockConfigStatusText("Invalid Target URL provided for service '" + service + "'", true)
                    return false; 
                }
            }
        }

        return true; 
    }

    handleSaveMockConfig = () => {
        const {dispatch} = this.props;
        const {selectedEditMockConfig} = this.state;
        
        if (!this.validateMockConfig(selectedEditMockConfig)) {
            return
        }

        dispatch(httpClientActions.saveMockConfig(selectedEditMockConfig));
    }

    handleUpdateMockConfig = () => {
        const { dispatch } = this.props;
        const { selectedEditMockConfig, selectedEditMockConfigId } = this.state;
        
        if (!this.validateMockConfig(selectedEditMockConfig)) {
            return
        }
        
        dispatch(httpClientActions.updateMockConfig(selectedEditMockConfigId, selectedEditMockConfig));
    }

    setMockConfigStatusText =  (text: string, isError: boolean) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.setMockConfigStatusText(text, isError))
    }

    resetMockConfigStatusText = () => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.resetMockConfigStatusText())
    }

    handleBackMockConfig = () => {
        this.resetMockConfigStatusText()
        this.showMockConfigList(true)
    }

    componentWillUnmount() {
        this.showMockConfigList(true);
    }

    handleMockContextLookupCollectionChange = (e) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.setMockContextLookupCollection(e.target.value))
    }
    
    handleMockContextSaveToCollectionChange = (e) => {
        const {dispatch} = this.props;
        const {httpClient: {userCollections}} = this.props;
        const collectionId = e.target.value;
        const saveToCollection = collectionId ? _.find(userCollections, {id: collectionId}) : {};
        dispatch(httpClientActions.setMockContextSaveToCollection(saveToCollection))
    }

    areAllMocked = (serviceConfigs) => {
        return _.isEmpty(serviceConfigs) ? false : serviceConfigs.reduce((acc, conf) => conf.isMocked && acc, true)
    }

    handleMockAllCheckChange = (allMocked) => {
        let {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs.forEach(config => {
            config.isMocked = !allMocked;
        })
        this.setState({selectedEditMockConfig})
        this.setMockConfigStatusText("", false)
    }

    updateSelectedEnv = (selectedEnv: IEnvironmentConfig) => {
        this.setState({  selectedEnv });
    }

    updateEnvState = (selectedEnv: IEnvironmentConfig, addNewEnv: boolean) => {
        this.setState({ selectedEnv, addNewEnv });
    }

    resetEnvStatusText = () => {
        const { dispatch } = this.props;
        dispatch(httpClientActions.resetEnvStatusText())
    }

    showEnvList = (show) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.showEnvList(show));
    }

    handleBackEnv = () => {
        this.resetEnvStatusText()
        this.showEnvList(true)
    }

    handleSaveEnvironment = () => {
        const {dispatch} = this.props;
        const {selectedEnv} = this.state;
        if (_.isEmpty(selectedEnv.name)) {
            this.setEnvStatusText("Environment name cannot be empty", true)
            return
        }
        dispatch(httpClientActions.saveEnvironment(selectedEnv));
    }

    handleUpdateEnvironment = () => {
        const {dispatch, cube: { selectedAppObj }} = this.props;
        const {selectedEnv} = this.state;
        selectedEnv.appId = selectedAppObj.id;
        if (_.isEmpty(selectedEnv.name)) {
            this.setEnvStatusText("Environment name cannot be empty", true)
            return
        }
        dispatch(httpClientActions.updateEnvironment(selectedEnv));
    }

    setEnvStatusText = (text, isError) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.setEnvStatusText(text, isError))
    }

    renderEnvironmentVariableConfig = () => {
        return (
                <div className="margin-top-10">
                    <EnvVar
                        selectedEnv={this.state.selectedEnv}
                        updateSelectedEnv={this.updateSelectedEnv}
                        updateEnvState={this.updateEnvState}
                        hideModal={() => {}} 
                    />
                </div>
            );
    }

    renderMockContextConfig = () => {
        const {httpClient: {mockContextLookupCollection,mockContextSaveToCollection, userCollections}} = this.props;
        return <div className="margin-top-10">
            <Grid>
                <Row>            
                    <Col xs={2}> <label style={{marginTop: "8px"}}>Lookup collection</label> </Col>
                    <Col xs={6}> 
                        <FormControl
                            componentClass="select"
                            placeholder="Collection"
                            style={{ fontSize: "12px" }}
                            value={mockContextLookupCollection}
                            onChange={this.handleMockContextLookupCollectionChange}
                            className="btn-sm"
                        >
                        <option value="">History</option>
                        {
                            userCollections.length && userCollections.map((collection) => (
                            <option key={collection.collec} value={collection.collec}>
                                {collection.name}
                            </option>
                            ))
                        }
                        </FormControl>
                    </Col>
                </Row>    
                <Row style={{marginTop: "10px"}}>       
                    <Col xs={2}> <label style={{marginTop: "8px"}}>Save to collection</label> </Col>
                    <Col xs={6}> 
                        <FormControl
                            componentClass="select"
                            placeholder="User Collection"
                            style={{ fontSize: "12px" }}
                            value={mockContextSaveToCollection?.id}
                            onChange={this.handleMockContextSaveToCollectionChange}
                            className="btn-sm"
                        >
                        <option value="">History</option>
                        {
                            userCollections.length && userCollections.map((collection) => (
                            <option key={collection.id} value={collection.id}>
                                {collection.name}
                            </option>
                            ))
                        }
                        </FormControl>
                    </Col>
                </Row>
            </Grid>
        </div>
    }
    handleSessionVarKeyChange = (e, index) => {
        const {sessionVars, } = this.state;
        sessionVars[index].key = e.target.value;
        this.setState({sessionVars: sessionVars});
    }

    handleSessionVarValueChange = (e, index) => {
        const {sessionVars, } = this.state;
        sessionVars[index].value = e.target.value;
        this.setState({sessionVars: sessionVars});
    }

    handleRemoveSessionVariable =(index) => {
        const {dispatch} = this.props;
        const {sessionVars, } = this.state;
        sessionVars.splice(index, 1);
        if(sessionVars.length == 0) {
            dispatch(httpClientActions.deleteContextMap());
        }
        this.setState({sessionVars: sessionVars});
    }

    handleAddNewSessionVariable = () => {
        const {sessionVars, } = this.state;
        sessionVars.push({key:"", value:""});
        this.setState({sessionVars: sessionVars});
    }

    handleSaveSessionVariable = () => {
        const {dispatch} = this.props;
        const {sessionVars} = this.state;
        const newContextMap = {};
        sessionVars.forEach(({key, value}, index) => {
            newContextMap[key] = value;
        });
        dispatch(httpClientActions.deleteContextMap());
        dispatch(httpClientActions.updateContextMap(newContextMap));
        this.props.hideModal();
    }

    handleMockConfigExport = (mockConfig) => {
        const configData = JSON.parse(mockConfig.value);
        const configFileName = `${configData.name}.json`;
        
        commonUtils.exportServiceConfigToClient(configFileName, mockConfig.value);
    }

    handleImportedFileContent = (fileContent: string) => {
        const { dispatch } = this.props;

        try {
            const importedServiceConfig = JSON.parse(fileContent);

            if (!this.validateMockConfig(importedServiceConfig)) {
                return
            }
    
            dispatch(httpClientActions.saveMockConfig(importedServiceConfig));
        } catch(e) {
            this.setMockConfigStatusText("Error importing config file.", true)
        }
    }

    handleImportServiceConfigButtonClick = () => {
        const importFileButton = document.getElementById("fileInput");
        importFileButton.value = null;
        importFileButton?.click();
    }

    handleImportServiceConfigFileImportChange = (event: React.ChangeEvent<HTMLInputElement>) => {

        this.resetMockConfigStatusText();

        const environmentConfigClass = this;
        
        const fileReader = new FileReader();

        fileReader.onload = function(){
            environmentConfigClass.handleImportedFileContent(String(fileReader.result));
        }

        fileReader.readAsText(event?.target?.files[0]);
    }

    renderSessionVariableConfigFooter = () => {
        const { sessionVars } = this.state;     
        return (
            <>
                {sessionVars.length > 0 && <span className="cube-btn margin-left-15" onClick={this.handleSaveSessionVariable}>SAVE</span>}
            </>
        );
    }

    onChangeAllowCertification = (event: React.ChangeEvent<HTMLInputElement>) => {
        this.props.dispatch(httpClientActions.updateGeneralSettings(commonConstants.ALLOW_CERTIFICATE_VALIDATION, event.target.checked));
    }

    renderCertificatesTab = () => {
        const {
            httpClient: { generalSettings },
        } = this.props;
        const value = generalSettings && generalSettings[commonConstants.ALLOW_CERTIFICATE_VALIDATION];
        const isAllowCertiValidation = isTrueOrUndefined(value);
        return <Fragment>
            <Grid>
                <Row className="show-grid margin-top-15">
                    <Col xs={5}>
                        <label htmlFor="allowCertificateValidation">Allow certificate validation</label>
                    </Col>
                    <Col xs={5}>
                        <input type="checkbox" name="allowCertificateValidation" checked={isAllowCertiValidation} onChange={this.onChangeAllowCertification} />
                    </Col>
                </Row>
            </Grid>
        </Fragment>
    }

    renderSessionVariables = () => {
        const {
            sessionVars,
        } = this.state;
        return <Fragment>
            <Grid>
                <Row className="show-grid margin-top-15">
                    <Col xs={5}>
                        <b>Variable</b>
                    </Col>
                    <Col xs={5}>
                        <b>Value</b>
                    </Col>
                </Row>
                {
                    (sessionVars || []).map(({key, value}, index) => (
                        <Row key={index} className="show-grid margin-top-15">
                            <Col xs={5}>
                                <input value={key} onChange={(e) => this.handleSessionVarKeyChange(e, index)} className="form-control"/>
                            </Col>
                            <Col xs={6}>
                                <input value={value} onChange={(e) => this.handleSessionVarValueChange(e, index)} className="form-control"/>
                            </Col>
                            <Col xs={1} style={{marginTop: "5px"}}>
                                <span  onClick={(e) => this.handleRemoveSessionVariable(index)}>
                                    <i className="fas fa-times pointer"/>
                                </span>
                            </Col>
                        </Row>
                    ))
                }
                <Row className="show-grid margin-top-15">
                    <Col xs={3}>
                        <div onClick={this.handleAddNewSessionVariable} className="pointer btn btn-sm cube-btn text-center">
                            <i className="fas fa-plus" style={{marginRight: "5px"}}></i><span>Add new variable</span>
                        </div>
                    </Col>
                </Row>
            </Grid>
        </Fragment>
    }

    renderMockConfig = () => {
        const { selectedEditMockConfig } = this.state;
        const {
            httpClient: {
                mockConfigList, 
                showMockConfigList
            }
        } = this.props;

        const allMocked = this.areAllMocked(selectedEditMockConfig.serviceConfigs);

        return (<>
            <div className="margin-top-10">
                {showMockConfigList && <div>
                    <table className="table table-hover">
                        <tbody>
                            {mockConfigList.map((mockConfig, index) => (
                                <tr key={index}>
                                    <td style={{cursor: "pointer"}} onClick={() => this.handleMockConfigRowClick(index)}>
                                        {mockConfig.key}
                                    </td>
                                    <td style={{width: "10%", textAlign: "right"}}>
                                        <i title="Export" style={{ marginRight: "10px" }} onClick={() => this.handleMockConfigExport(mockConfig)} className="fas fa-download pointer" aria-hidden="true"></i>
                                        <i title="Delete" className="fas fa-trash pointer" onClick={() => this.handleRemoveMockConfig(index)}/>
                                    </td>
                                </tr>)
                            )}
                            <tr>
                                <td onClick={this.handleAddNewMockConfig} className="pointer">
                                    <i className="fas fa-plus" style={{marginRight: "5px"}}></i><span>Add New Configuration</span>
                                </td>
                                <td></td>
                            </tr>
                        </tbody>
                    </table>
                </div>}

                {!showMockConfigList && <>
                    <Grid>
                        <Row>
                            <Col xs={2}>
                                <label style={{ marginTop: "8px" }}>Configuration Name: </label>
                            </Col>
                            <Col xs={6}>
                                <input value={selectedEditMockConfig["name"]} onChange={this.handleSelectedMockConfigNameChange} className="form-control"/>
                            </Col>  
                        </Row>
                        
                        <Row className="show-grid margin-top-15">
                            <Col xs={3}>
                                <b>Service</b>
                            </Col>
                            <Col xs={3}>
                                <b>Prefix</b>{" "}
                                <Tippy content={"The path prefix to use. If not specified, will default to the service name, which will also be stripped before lookup and saving the request."} placement="bottom"  arrow={true}>
                                    <i className="fa fa-info-circle"></i>
                                </Tippy>
                            </Col>
                            <Col xs={1}>
                                <Checkbox inline disabled={_.isEmpty(selectedEditMockConfig.serviceConfigs)} checked={allMocked} onChange={() => this.handleMockAllCheckChange(allMocked)}>
                                    <b>Mock</b>
                                </Checkbox>
                            </Col>
                            <Col xs={4}>
                                <b>Target URL</b>
                            </Col>
                            <Col xs={1}></Col>
                        </Row>
                        {(selectedEditMockConfig.serviceConfigs || [])
                        .map(({service, url, isMocked, servicePrefix}, index) => (
                                    <Row className="show-grid margin-top-10" key={index}>
                                        <Col xs={3}>
                                            <input value={service} onChange={(e) => this.handleServiceChange(e, index)} className="form-control"/>
                                        </Col>
                                        <Col xs={3}>
                                            <input value={servicePrefix} onChange={(e) => this.handleServicePrefixChange(e, index)} className="form-control"/>
                                        </Col>
                                        <Col xs={1} style={{}}>
                                            <Checkbox inline checked={isMocked} onChange={() => this.handleIsMockedCheckChange(index)}/>
                                        </Col>
                                        <Col xs={4}>
                                            <input value={url} onChange={(e) => this.handleTargetURLChange(e, index)} className="form-control" disabled={isMocked}/>
                                        </Col>
                                        <Col xs={1}>
                                            <span  onClick={() => this.handleRemoveServiceConfig(index)}>
                                                <i className="fas fa-times pointer"/>
                                            </span>
                                        </Col>
                                    </Row>
                            )
                        )}                                    
                        <Row className="show-grid margin-top-15">
                            <Col xs={3}>
                                <div onClick={this.handleAddNewServiceConfig} className="pointer btn btn-sm cube-btn text-center">
                                    <i className="fas fa-plus-circle" style={{marginRight: "5px"}}></i><span>Add New Service</span>
                                </div>
                            </Col>
                        </Row>
                    </Grid>
                </>}
            </div>
        </>)
    }

    renderEnvironmentVariableConfigFooter = () => {
        const { addNewEnv } = this.state;
        const {
            httpClient: {
                envStatusText, 
                envStatusIsError, 
                showEnvList
            }
        } = this.props;
        
        return (
            <>
                <span className="pull-left" style={{color: envStatusIsError ? "red" : ""}}>{envStatusText}</span>
                {/* {showEnvList && <span className="cube-btn margin-left-15" onClick={this.props.hideModal}>DONE</span>} */}
                {!showEnvList && <span className="cube-btn margin-left-15" onClick={this.handleBackEnv}>BACK</span>}
                {!showEnvList && addNewEnv && <span className="cube-btn margin-left-15" onClick={this.handleSaveEnvironment}>SAVE</span>}
                {!showEnvList && !addNewEnv && <span className="cube-btn margin-left-15" onClick={this.handleUpdateEnvironment}>SAVE</span>}
            </>
        );
    }

    renderMockConfigFooter = () => {
        const { addNewMockConfig, selectedTabKey} = this.state;
        const {
            httpClient: {
                mockConfigStatusText, 
                mockConfigStatusIsError, 
                showMockConfigList
            }
        } = this.props;

        return (
            <>
                <span className="pull-left md-service-config-footer-error" style={{color: mockConfigStatusIsError ? "red" : ""}}>{mockConfigStatusText}</span>
                {
                    
                    showMockConfigList 
                    && 
                    (
                        <>
                            <span
                                className="cube-btn margin-right-10 margin-left-15" 
                                onClick={this.handleImportServiceConfigButtonClick}
                            >
                                <Glyphicon glyph="import" /> Import Service Config
                            </span>
                            <input 
                                
                                id="fileInput"
                                type="file"
                                onChange={this.handleImportServiceConfigFileImportChange}
                                accept=".json"
                                style={{ display: "none" }} 
                            />
                        </>
                    )
                }
                {
                    selectedTabKey == 0 && 
                    !showMockConfigList &&                         
                    <>
                        <span 
                            className="cube-btn margin-right-10 margin-left-15" 
                            onClick={this.handleBackMockConfig}
                        >
                            <i className="fa fa-arrow-circle-left"></i>
                            &nbsp;BACK
                        </span>
                    
                        { 
                            addNewMockConfig 
                            ? 
                                <span 
                                    className="cube-btn margin-right-10" 
                                    onClick={this.handleSaveMockConfig}
                                >
                                    <i className="fa fa-save"></i>&nbsp;SAVE
                                </span>
                            :   <span 
                                    className="cube-btn margin-right-10" 
                                    onClick={this.handleUpdateMockConfig}
                                >
                                    <i className="fa fa-save"></i>&nbsp;UPDATE A
                                </span>
                        }
                    </>
                }
                {!showMockConfigList && <span className="cube-btn margin-left-15" onClick={this.handleBackMockConfig}>BACK</span>}
                {!showMockConfigList && addNewMockConfig && <span className="cube-btn margin-left-15" onClick={this.handleSaveMockConfig}>SAVE</span>}
                {!showMockConfigList && !addNewMockConfig && <span className="cube-btn margin-left-15" onClick={this.handleUpdateMockConfig}>SAVE</span>}
            </>
        );
    }
  
    render() {
        const { selectedTabKey } = this.state;

        return (
            <Fragment>
                <Modal.Header>
                    <div className="md-env-config-header-container">
                        <span className="md-env-config-header-label">Configuration</span>
                        <Glyphicon onClick={this.props.hideModal} glyph="remove" className="md-env-config-close" />
                    </div>
                </Modal.Header>
                <Modal.Body>
                    <div className="md-env-config-modal-body">
                        <Tabs 
                            id="proxyDialogBoxTabs" 
                            activeKey={selectedTabKey}
                            onSelect={this.handleSelectedTabChange}>
                            <Tab eventKey={0} title="Environment" style={{ paddingTop: "15px", height: "400px", overflowY: "auto" }}>
                                {this.renderEnvironmentVariableConfig()}
                            </Tab>
                            <Tab eventKey={1} title="Service" style={{ paddingTop: "15px", height: "400px", overflowY: "auto" }}>
                                {this.renderMockConfig()}
                            </Tab>
                            <Tab eventKey={2} title="Mock Settings">
                                {this.renderMockContextConfig()}
                            </Tab>
                            <Tab eventKey={3} title="Session Variables">
                                {this.renderSessionVariables()}
                            </Tab>
                            <Tab eventKey={4} title="Certificates">
                                {this.renderCertificatesTab()}
                            </Tab>
                        </Tabs>
                    </div>
                </Modal.Body>
                <Modal.Footer style={{ height: "60px" }}>
                    {selectedTabKey === 0 && this.renderEnvironmentVariableConfigFooter()}
                    {selectedTabKey === 1 && this.renderMockConfigFooter()}
                    {selectedTabKey === 3 && this.renderSessionVariableConfigFooter()}
                </Modal.Footer>
            </Fragment>
        )
    }
}

const mapStateToProps = (state: IStoreState) => ({
    cube: state.cube,
    httpClient: state.httpClient,
    user: state.authentication.user
});

export default connect(mapStateToProps)(EnvironmentConfigs);

