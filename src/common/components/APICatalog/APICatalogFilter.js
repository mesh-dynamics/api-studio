import React, { Component } from 'react'
import DatePicker from 'react-datepicker';
import Modal from "react-bootstrap/es/Modal";
import './APICatalog.scss';
import { history } from '../../helpers';
import { connect } from "react-redux";
import _ from "lodash";
import { apiCatalogActions } from '../../actions/api-catalog.actions';
import classNames from 'classnames';
import { DropdownButton, MenuItem, FormControl} from 'react-bootstrap';
import GoldenCollectionBrowse from './GoldenCollectionBrowse';
import ConvertCollection from './ConvertCollection';

class APICatalogFilter extends Component {

    constructor(props) {
        super(props)
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
        const ddlClass = classNames({
            "r-att form-control": true,
            'select-indicator': !selectedSource
        });
        return (
        <div>
            <select className={ddlClass} placeholder="Select Source" value={selectedSource || "DEFAULT"} onChange={handleSourceDropDownChange}>   
                <option value="DEFAULT" disabled>Select Source</option>
                {sources.map(source => 
                    <option key={source.value} value={source.value}>
                        {source.text}
                    </option>)
                }
            </select>
        </div>);
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

        return compareRequests.map((reqData, index) => (
            <div key={index} style={{display: "flex", flexDirection: "row", margin: "5px"}}>
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

    render() {
        const {currentPage, cube, apiCatalog} = this.props;
        const {diffRequestLeft, diffRequestRight, compareRequests, selectedSource, selectedService, selectedApiPath, selectedCollection, selectedGolden} = apiCatalog;
        
        return (
            <div>
                <div>
                    <div className="label-n">APPLICATION</div>
                    <div className="application-name">{cube.selectedApp}</div>
                </div>
                {
                    currentPage==="api" && 
                    <div className="filters">
                        <div><div className="margin-top-10" style={{borderBottom: "1px solid grey", paddingBottom: "10px"}}>
                            <div className="label-n">SOURCE</div>
                            {this.renderSourceDropdown()}
                        </div>
                        

                        {selectedSource && <div>
                        {(selectedSource==="UserGolden" || selectedSource==="Golden") && 
                        <>
                        <div>
                            <GoldenCollectionBrowse selectedSource={selectedSource}/> 
                        </div>
                            <ConvertCollection  selectedSource={selectedSource} />
                        </>}

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
                            
                            {((selectedSource==="UserGolden" && selectedCollection) || (selectedSource==="Golden" && selectedGolden)) && <>
                                <div className="selected-items margin-top-10">
                                    <div>
                                        <span style={{ fontWeight: 300 }}>Service</span>
                                        <p><b>{selectedService || "All"}</b></p>
                                    </div>
                                        
                                    <div>
                                        <span style={{ fontWeight: 300 }}>API</span>
                                        <p><b>{selectedApiPath || "All"}</b></p>
                                    </div>                    
                                </div>
                                </>}
                        </div>}
                        </div>
                        
                        <div className="margin-top-10" style={{borderTop: "1px solid grey", minHeight: "20%"}}>
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