import React, { Component } from 'react'
import DatePicker from 'react-datepicker';
import Modal from "react-bootstrap/es/Modal";
import './APICatalog.css';
import { history } from '../../helpers';
import { connect } from "react-redux";
import _ from "lodash";
import { apiCatalogActions } from '../../actions/api-catalog.actions';

class APICatalogFilter extends Component {

    constructor(props) {
        super(props)
        this.state = {
            showBrowseGoldenCollectionModal: false,
            selectedGoldenCollectionFromModal: "",
        };
    }

    handleFilterChange = (metadata, value) => {
        const {dispatch} = this.props;
        dispatch(apiCatalogActions.handleFilterChange(metadata, value));
    }

    renderSourceDropdown = () => {
        const {apiCatalog: {selectedSource}, dispatch} = this.props;
        const handleSourceDropDownChange = (event) => this.handleFilterChange("selectedSource", event.target.value);
        const sources = [
            {value: "Capture", text: "Capture"}, 
            {value: "UserGolden", text: "Collection"}, 
            {value: "Golden", text: "Golden"}, 
        ]
        return (
        <div>
            <select className="r-att form-control" placeholder="Select Source" value={selectedSource || "DEFAULT"} onChange={handleSourceDropDownChange}>   
                <option value="DEFAULT" disabled>Select Source</option>
                {sources.map(source => 
                    <option key={source.value} value={source.value}>
                        {source.text}
                    </option>)
                }
            </select>
        </div>);
    }

    renderCollectionDropdown = () => {
        const {apiCatalog: {collectionList, selectedCollection}} = this.props;
        const handleCollectionDropDownChange = (event) => this.handleFilterChange("selectedCollection", event.target.value);
        return (
        <div>
            <select className="r-att form-control" placeholder="Select Collection" value={selectedCollection || "DEFAULT"} onChange={handleCollectionDropDownChange}>   
                <option value="DEFAULT" disabled>Select Collection</option>
                {
                    collectionList.map((item, index) => 
                         <option key={item.collec + index} value={item.collec} hidden={(index > 10) && selectedCollection!=item.collec}>{`${item.name} ${item.label}`}</option>
                    )
                }
            </select>
        </div>);
    }

    renderGoldenDropdown = () => {
        const {apiCatalog: {goldenList, selectedGolden}} = this.props;
        const handleGoldenDropDownChange = (event) => this.handleFilterChange("selectedGolden", event.target.value);
        return (
        <div>
            <select className="r-att form-control" placeholder="Select Golden" value={selectedGolden || "DEFAULT"} onChange={handleGoldenDropDownChange}>   
                <option value="DEFAULT" disabled>Select Golden</option>
                {
                    goldenList.map((item, index) => 
                        <option key={item.collec + index} value={item.collec} hidden={(index > 10) && selectedGolden!=item.collec}>{`${item.name} ${item.label}`}</option>
                    )
                }
            </select>
        </div>);
    }

    renderServiceDropdown = () => {
        const {apiCatalog: {selectedSource, selectedCollection, selectedGolden, selectedService, services}} = this.props;
        
        const handleServiceDropDownChange = (event) => this.handleFilterChange("selectedService", event.target.value);
        
        const disabled = (!selectedSource) || (selectedSource=="UserGolden" && !selectedCollection) || (selectedSource=="Golden" && !selectedGolden);
        
        return (
        <div>
            <select className="r-att form-control" placeholder="Select Service" value={selectedService || "DEFAULT"} onChange={handleServiceDropDownChange} disabled={disabled}>
                {!disabled && <option value="DEFAULT" disabled>Select Service</option>}
                {services.map(service => 
                    <option key={service.val} value={service.val}>
                        {service.val}
                    </option>)
                }
            </select>
        </div>);
    }

    renderAPIPathDropdown = () => {
        const {apiCatalog: {selectedService, selectedApiPath, apiPaths}} = this.props;
        const handleAPIDropDownChange = (event) => this.handleFilterChange("selectedApiPath", event.target.value);
        const disabled = !selectedService;
        return <select className="r-att form-control" placeholder="Select API" value={selectedApiPath || "DEFAULT"} onChange={handleAPIDropDownChange} disabled={disabled}>
            {!disabled && <option value="DEFAULT" disabled>Select API</option>}
            {apiPaths.map(apiPath => <option key={apiPath.val} value={apiPath.val}>{apiPath.val}</option>)}
        </select>;
    }

    renderStartTime = () => {
        const {apiCatalog: {startTime}, dispatch} = this.props;
        return <DatePicker
            className="form-control"
            selected={new Date(startTime)}
            showTimeSelect
            timeFormat="HH:mm"
            timeIntervals={15}
            timeCaption="time"
            dateFormat="yyyy/MM/dd HH:mm"
            onChange={dateTime => this.handleFilterChange("startTime",dateTime)}
        />;
    }

    renderEndTime = () => {
        const {apiCatalog: {endTime}, dispatch} = this.props;
        return <DatePicker
        className="form-control"
        selected={new Date(endTime)}
            className="form-control"
            showTimeSelect
            timeFormat="HH:mm"
            timeIntervals={15}
            timeCaption="time"
            dateFormat="yyyy/MM/dd HH:mm"
            onChange={dateTime => this.handleFilterChange("endTime",dateTime)}
        />;
    }

    renderInstanceDropdown = () => {
        const {dispatch,apiCatalog: {selectedService, selectedInstance, instances}} = this.props;
        const handleInstanceDropDownChange = (event) => this.handleFilterChange("selectedInstance", event.target.value);

        return <select className="r-att form-control" placeholder="Select Instance" value={selectedInstance} onChange={handleInstanceDropDownChange} disabled={!selectedService}>
            {instances.map(instance => <option key={instance.val} value={instance.val}>{instance.val}</option>)}
        </select>;
    }

    renderCompareData = () => {
        const {apiCatalog : {
            compareRequests
        }} = this.props;

        return compareRequests.map((reqData) => (
            <div style={{display: "flex", flexDirection: "row", margin: "5px"}}>
                <div style={{display: "flex", flexDirection: "column"}}>
                    <div style={{display: "flex", flexDirection: "row"}}>
                        <span style={{fontWeight: 300}}>INSTANCE: </span>
                        <span style={{marginLeft: "5px"}}>{reqData.instance}</span>
                    </div>
                    <span>{new Date(reqData.parentRequest.reqTimestamp * 1000).toLocaleString()}</span>
                </div>
                <div style={{display: "flex", flexDirection: "column", justifyContent: "center", margin: "15px"}}>
                    <i className="fa fa-times" style={{cursor: "pointer"}} onClick={() => this.handleCompareReqRemove(reqData)}></i>
                </div>
            </div>
        ));
    }
  
    handleCompareReqRemove = (reqData) => {
        const {dispatch} = this.props;
        dispatch(apiCatalogActions.unpinCompareRequest(reqData));
    }

    handleCompareReset = () => {
        const {dispatch} = this.props;
        dispatch(apiCatalogActions.resetCompareRequest());
    }

    handleCompareSubmit = (app) => {
        const {apiCatalog : {
            compareRequests
        }} = this.props;

        history.push({
            pathname: "diff",
            search: `?app=${app}&requestId1=${compareRequests[0].parentReqId}&requestId2=${compareRequests[1].parentReqId}`
        })
    }

    showGoldenCollectionBrowseModal = () => {
        this.setState({
            showBrowseGoldenCollectionModal: true,
        });

    };

    hideGoldenCollectionModal = () => {
        this.setState({showBrowseGoldenCollectionModal: false});
    };

    selectGoldenCollectionFromModal = (collec) => {
        this.setState({selectedGoldenCollectionFromModal: collec});
    }

    selectHighlightedGoldenCollectionFromModal = () => {
        const {apiCatalog: {selectedSource}} = this.props;

        if (selectedSource==="UserGolden") {
             this.handleFilterChange("selectedCollection", this.state.selectedGoldenCollectionFromModal);
        } else if (selectedSource==="Golden") {
             this.handleFilterChange("selectedGolden", this.state.selectedGoldenCollectionFromModal);
        }

        this.setState({showBrowseGoldenCollectionModal: false})
    }

    renderGoldenCollectionTable() {
        const {apiCatalog: {selectedSource, collectionList, goldenList}} = this.props;

        const goldenCollectionList = selectedSource==="UserGolden" ? collectionList : goldenList;

        if (!goldenCollectionList || goldenCollectionList.length == 0) {
            return <tr><td colSpan="2" className="text-center">NO DATA FOUND</td></tr>
        }

        return goldenCollectionList.map(item => (
            <tr key={item.collec} value={item.collec} className={this.state.selectedGoldenCollectionFromModal == item.collec ? "selected-g-row" : ""} onClick={() => this.selectGoldenCollectionFromModal(item.collec)}>
                <td>{item.name}</td>
                <td>{new Date(item.timestmp * 1000).toLocaleString()}</td>
            </tr>)
        );
    }
     
    renderModals() {
        return (
            <Modal show={this.state.showBrowseGoldenCollectionModal}>
                <Modal.Header>
                    <Modal.Title>Browse {this.props.selectedSource==="UserGolden" ? "Collections" : "Goldens"}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div style={{height: "300px", overflowY: "auto"}}>
                        <table className="table table-condensed table-hover table-striped">
                            <thead>
                                <tr>
                                    <th style={{position: "sticky"}}>Name</th>
                                    <th style={{position: "sticky"}}>Created at</th>
                                </tr>
                            </thead>

                            <tbody>
                            {this.renderGoldenCollectionTable()}
                            </tbody>
                        </table>
                    </div>

                </Modal.Body>
                <Modal.Footer>
                    <span onClick={this.selectHighlightedGoldenCollectionFromModal} className={this.state.selectedGoldenCollectionFromModal ? "cube-btn" : "disabled cube-btn"}>Select</span>&nbsp;&nbsp;

                    <span onClick={this.hideGoldenCollectionModal} className="cube-btn">Cancel</span>
                </Modal.Footer>
            </Modal>
        );
    }

    render() {
        const {currentPage, cube, apiCatalog} = this.props;
        const {diffRequestLeft, diffRequestRight, compareRequests, selectedSource} = apiCatalog;
        
        return (
            <div>
                <div>
                    <div className="label-n">APPLICATION</div>
                    <div className="application-name">{cube.selectedApp}</div>
                </div>
                {
                    currentPage==="api" && 
                    <div>
                        <div className="margin-top-10" style={{borderBottom: "1px solid grey", paddingBottom: "10px"}}>
                            <div className="label-n">SOURCE</div>
                            {this.renderSourceDropdown()}
                        </div>
                        

                        {selectedSource && <div>
                            {(selectedSource==="UserGolden") &&
                            <div className="margin-top-10">
                                <div className="label-n">COLLECTION&nbsp;
                                    <i onClick={this.showGoldenCollectionBrowseModal} title="Browse Collection" className="link fas fa-folder-open pull-right font-15"></i>
                                </div>
                                {this.renderCollectionDropdown()}
                            </div>}
                            
                            {(selectedSource==="Golden") &&
                            <div className="margin-top-10">
                                <div className="label-n">GOLDEN&nbsp;
                                    <i onClick={this.showGoldenCollectionBrowseModal} title="Browse Golden" className="link fas fa-folder-open pull-right font-15"></i>
                                </div>
                                {this.renderGoldenDropdown()}
                            </div>}
                            
                            <div className="margin-top-10">
                                <div className="label-n">SERVICE</div>
                                {this.renderServiceDropdown()}
                            </div>
                    
                            <div className="margin-top-10">
                                <div className="label-n">API</div>
                                {this.renderAPIPathDropdown()}
                            </div>

                            {selectedSource=="Capture" && <div>
                                <div className="margin-top-10">
                                    <div className="label-n">START TIME</div>
                                    {this.renderStartTime()}
                                </div>

                                <div className="margin-top-10">
                                    <div className="label-n">END TIME</div>
                                    {this.renderEndTime()}
                                </div>
                            </div>}

                            {selectedSource=="Capture" && <div className="margin-top-10">
                                <div className="label-n">SOURCE INSTANCE</div>
                                {this.renderInstanceDropdown()}
                            </div>}

                        </div>}
                        
                        <div className="margin-top-10" style={{borderTop: "1px solid grey", position: "absolute", bottom: 0, minHeight: "20%"}}>
                            <div className="label-n">COMPARE REQUESTS (any two):</div>
                                {this.renderCompareData()}
                                {compareRequests.length==2 &&
                                    <div style={{display: "flex", flexDirection: "row"}}>
                                        <div className="cube-btn text-center width-50" style={{margin: "0 5px 0 0"}} onClick={()=> this.handleCompareSubmit(cube.selectedApp)}>COMPARE</div>
                                        <div className="cube-btn text-center width-50" style={{margin: "0 0 0 5px"}} onClick={this.handleCompareReset}>RESET</div>
                                    </div>
                                }
                        </div>
                    </div>
                }
                {
                    currentPage==="diff" && 
                    <div style={{display: "flex", flexDirection: "column"}} className="margin-top-10">
                        <p>REQUESTS</p>
                        <div style={{display: "flex", flexDirection: "column"}}>
                            <span style={{fontWeight: 300}}>LEFT</span>
                            <span>INSTANCE: {diffRequestLeft.instanceId}</span>
                            <span>{new Date(diffRequestLeft.timestamp  * 1000).toLocaleString()}</span>
                        </div>
                        <div className="margin-top-10" style={{display: "flex", flexDirection: "column"}}>
                            <span style={{fontWeight: 300}}>RIGHT</span>
                            <span>INSTANCE: {diffRequestRight.instanceId}</span>
                            <span>{new Date(diffRequestRight.timestamp  * 1000).toLocaleString()}</span>
                        </div>
                    </div>
                }
                {this.renderModals()}
            </div>
        );
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
    apiCatalog: state.apiCatalog,
});

const connectedAPICatalogFilter = connect(mapStateToProps)(APICatalogFilter);

export default connectedAPICatalogFilter;
export {connectedAPICatalogFilter as APICatalogFilter}