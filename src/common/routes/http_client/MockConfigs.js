import React, { Component, Fragment } from 'react'
import { connect } from "react-redux";
import { Modal, Grid, Row, Col, Checkbox, Tabs, Tab, FormGroup, FormControl, ControlLabel, Form} from 'react-bootstrap';
import { httpClientActions } from '../../actions/httpClientActions';
import _ from "lodash";

class MockConfigs extends Component {
    constructor(props) {
        super(props)
        this.state = {
            selectedEditMockConfig: {},
            selectedEditMockConfigId: null,
            addNew: false,
            selectedTabKey : 0
        }
    }

    handleSelectedTabChange = (changedKey) => {
        this.setState({selectedTabKey: changedKey});
    }
    
    handleMockConfigRowClick = (index) => {
        const {httpClient: {
            mockConfigList
        }} = this.props;
        this.showMockConfigList(false)
        // parse the value json string, attach id from the parent object
        const selectedEditMockConfig = {...JSON.parse(mockConfigList[index].value)}
        this.setState({selectedEditMockConfig: selectedEditMockConfig, addNew: false, selectedEditMockConfigId: mockConfigList[index].id})
    }

    handleServiceChange = (e, index) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs[index].service = e.target.value;
        this.setState({selectedEditMockConfig})
    }

    handleTargetURLChange = (e, index) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs[index].url = e.target.value;
        this.setState({selectedEditMockConfig})
    }

    handleIsMockedCheckChange = (index) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs[index].isMocked = !selectedEditMockConfig.serviceConfigs[index].isMocked;
        this.setState({selectedEditMockConfig})
    }

    handleSelectedMockConfigNameChange = (e) => {
        const {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.name = e.target.value;
        this.setState({selectedEditMockConfig})
    }

    handleAddNewMockConfig = () => {
        let selectedEditMockConfig = {
            name: "",
            serviceConfigs: [],
        }
        this.showMockConfigList(false)
        this.setState({selectedEditMockConfig, addNew: true})
    }

    showMockConfigList = (show) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.showMockConfigList(show));
    }

    handleAddNewServiceConfig = () => {
        let {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs.push({
            service: "", 
            url: "", 
            isMocked: false,
        })
        this.setState({selectedEditMockConfig})
    }

    handleRemoveMockConfig = (index) => {
        const {httpClient: {
            mockConfigList
        }, dispatch} = this.props;
        const {id, key} = mockConfigList[index];
        dispatch(httpClientActions.removeMockConfig(id, key))
    }

    handleRemoveServiceConfig = (index) => {
        let {selectedEditMockConfig} = this.state;
        selectedEditMockConfig.serviceConfigs.splice(index, 1)
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

    setMockConfigStatusText = (text, isError) => {
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
        this.showMockConfigList(true)
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

    renderMockConfig = () => {
        const {selectedEditMockConfig, addNew} = this.state;
        const {httpClient: {
            mockConfigList, showMockConfigList
        }} = this.props;

        const allMocked = this.areAllMocked(selectedEditMockConfig.serviceConfigs)
        return (<>
            <div className="margin-top-10">
                {showMockConfigList && <div>
                    <label>Configurations</label>
                    <table className="table table-hover">
                        <tbody>
                            {mockConfigList.map((mockConfig, index) => (
                                <tr key={index}>
                                    <td style={{cursor: "pointer"}} onClick={() => this.handleMockConfigRowClick(index)}>
                                        {mockConfig.key}
                                    </td>
                                    <td style={{width: "10%", textAlign: "right"}}>
                                        <i className="fas fa-trash pointer" onClick={() => this.handleRemoveMockConfig(index)}/>
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
                            <Col xs={5}>
                                <b>Service</b>
                            </Col>
                            <Col xs={1}>
                                <Checkbox inline disabled={_.isEmpty(selectedEditMockConfig.serviceConfigs)} checked={allMocked} onChange={() => this.handleMockAllCheckChange(allMocked)}>
                                    <b>Mock</b>
                                </Checkbox>
                            </Col>
                            <Col xs={5}>
                                <b>Target URL</b>
                            </Col>
                            <Col xs={1}></Col>
                        </Row>
                        {(selectedEditMockConfig.serviceConfigs || [])
                            .map(({service, url, isMocked}, index) => (
                                    <Row className="show-grid margin-top-10" key={index}>
                                        <Col xs={5}>
                                            <input value={service} onChange={(e) => this.handleServiceChange(e, index)} className="form-control"/>
                                        </Col>
                                        <Col xs={1} style={{}}>
                                            <Checkbox inline checked={isMocked} onChange={() => this.handleIsMockedCheckChange(index)}/>
                                        </Col>
                                        <Col xs={5}>
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
  
    render() {
        const {selectedEditMockConfig, addNew} = this.state;
        const {httpClient: {
            mockConfigList, mockConfigStatusText, mockConfigStatusIsError, showMockConfigList
        }} = this.props;
        return (
            <Fragment>
                <Modal.Header closeButton>
                    Proxy Settings
                </Modal.Header>
                <Modal.Body>
                    <div style={{height: "400px", overflowY: "auto"}}>
                        <Tabs defaultActiveKey={this.state.selectedTabKey} id="proxyDialogBoxTabs" onSelect={this.handleSelectedTabChange}>
                            <Tab eventKey={0} title="Service Configurations">
                                {this.renderMockConfig()}
                            </Tab>
                            <Tab eventKey={1} title="Mock Settings">
                                {this.renderMockContextConfig()}
                            </Tab>
                        </Tabs>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <span className="pull-left" style={{color: mockConfigStatusIsError ? "red" : ""}}>{mockConfigStatusText}</span>
                    {this.state.selectedTabKey == 0 && !showMockConfigList &&                         
                        <>
                        <span className="cube-btn margin-right-10 margin-left-15" onClick={this.handleBackMockConfig}>
                            <i className="fa fa-arrow-circle-left"></i>
                            &nbsp;BACK
                        </span>
                    
                        { addNew ? <span className="cube-btn margin-right-10 " onClick={this.handleSaveMockConfig}><i className="fa fa-save"></i>&nbsp;SAVE</span>
                                : <span className="cube-btn margin-right-10" onClick={this.handleUpdateMockConfig}><i className="fa fa-save"></i>&nbsp;UPDATE</span>
                        }</>
                    }
                    <span className="cube-btn" onClick={this.props.hideModal}>CLOSE</span>
                    {/* {!showMockConfigList && <span className="cube-btn margin-left-15" onClick={this.handleBackMockConfig}>BACK</span>} */}
                    {/* {!showMockConfigList && addNew && <span className="cube-btn margin-left-15" onClick={this.handleSaveMockConfig}>SAVE</span>}
                    {!showMockConfigList && !addNew && <span className="cube-btn margin-left-15" onClick={this.handleUpdateMockConfig}>UPDATE</span>} */}
                </Modal.Footer>
            </Fragment>
        )
    }
}

const mapStateToProps = (state) => ({
    httpClient: state.httpClient,
    user: state.authentication.user
});

export default connect(mapStateToProps)(MockConfigs);

