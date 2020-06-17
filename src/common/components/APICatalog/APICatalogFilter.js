import React, { Component } from 'react'
import DatePicker from 'react-datepicker';
import './APICatalog.css';
import { history } from '../../helpers';
import { connect } from "react-redux";
import _ from "lodash";
import { apiCatalogActions } from '../../actions/api-catalog.actions';

class APICatalogFilter extends Component {

    renderServiceDropdown = () => {
        const {handleFilterChange, services, selectedService} = this.props;
        const handleServiceDropDownChange = (event) => handleFilterChange("selectedService", event.target.value);

        return (
        <div>
            <select className="r-att form-control" placeholder="Select Service" value={selectedService || "DEFAULT"} onChange={handleServiceDropDownChange}>   
                <option value="DEFAULT" disabled>Select Service</option>
                {services.map(service => 
                    <option key={service.val} value={service.val}>
                        {service.val}
                    </option>)
                }
            </select>
        </div>);
    }

    renderDayPicker = () => {
        const {prevDays} = this.props;
        return (
            <div>
                <input type="number" defaultValue={prevDays} onKeyPressCapture={this.handlePrevDaysChange} className="text-center form-control" min="1"/>
            </div>
        )
    }

    renderAPIPathDropdown = () => {
        const {handleFilterChange, apiPaths, selectedApiPath, selectedService} = this.props;
        const handleAPIDropDownChange = (event) => handleFilterChange("selectedApiPath", event.target.value);

        return <select className="r-att form-control" placeholder="Select API" value={selectedApiPath} onChange={handleAPIDropDownChange} disabled={!selectedService}>
            {apiPaths.map(apiPath => <option key={apiPath.val} value={apiPath.val}>{apiPath.val}</option>)}
        </select>;
    }

    renderStartTime = () => {
        const {startTime, handleFilterChange} = this.props;
        return <DatePicker
            className="form-control"
            selected={new Date(startTime)}
            //todayButton="Today"
            showTimeSelect
            timeFormat="HH:mm"
            timeIntervals={15}
            timeCaption="time"
            dateFormat="yyyy/MM/dd HH:mm"
            onChange={dateTime => handleFilterChange("startTime", dateTime)}
        />;
    }

    renderEndTime = () => {
        const {endTime, handleFilterChange} = this.props;
        return <DatePicker
        className="form-control"
        selected={new Date(endTime)}
            //todayButton="Today"
            className="form-control"
            showTimeSelect
            timeFormat="HH:mm"
            timeIntervals={15}
            timeCaption="time"
            dateFormat="yyyy/MM/dd HH:mm"
            onChange={dateTime => handleFilterChange("endTime", dateTime)}
        />;
    }

    renderInstanceDropdown = () => {
        const {handleFilterChange, instances, selectedInstance, selectedService} = this.props;
        const handleInstanceDropDownChange = (event) => handleFilterChange("selectedInstance", event.target.value);

        return <select className="r-att form-control" placeholder="Select Instance" value={selectedInstance} onChange={handleInstanceDropDownChange} disabled={!selectedService}>
            {instances.map(instance => <option key={instance.val} value={instance.val}>{instance.val}</option>)}
        </select>;
    }

    handlePrevDaysChange = (e) => {
        let value = parseInt(e.target.value)
          if (e.key !== "Enter")
              return;
  
          if (value <= 0) {
              alert("Invalid prevDays value")
              console.error("Invalid prevDays value")
              return;
          }
          this.props.handlePrevDaysChange(value);
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

    handleCompareSubmit = () => {
        const {apiCatalog : {
            compareRequests
        }, app} = this.props;

        history.push({
            pathname: "diff",
            search: `?app=${app}&requestId1=${compareRequests[0].parentReqId}&requestId2=${compareRequests[1].parentReqId}`
        })
    }

    render() {
        const {currentPage, app, apiCatalog} = this.props;
        const {diffRequestLeft, diffRequestRight} = apiCatalog;
        
        return (
            <div>
                <div>
                    <div className="label-n">APPLICATION</div>
                    <div className="application-name">{app}</div>
                </div>
                {
                    currentPage==="api" && 
                    <div>
                        <div className="margin-top-10">
                            <div className="label-n">SELECT SERVICE</div>
                            {this.renderServiceDropdown()}
                        </div>
                
                        <div className="margin-top-10">
                            <div className="label-n">API</div>
                            {this.renderAPIPathDropdown()}
                        </div>

                        <div className="margin-top-10">
                            <div className="label-n">START TIME</div>
                            {this.renderStartTime()}
                        </div>

                        <div className="margin-top-10">
                            <div className="label-n">END TIME</div>
                            {this.renderEndTime()}
                        </div>

                        <div className="margin-top-10">
                            <div className="label-n">SOURCE INSTANCE</div>
                            {this.renderInstanceDropdown()}
                        </div>
                        
                        <div className="margin-top-10" style={{borderTop: "1px solid grey", position: "absolute", bottom: 0, minHeight: "20%"}}>
                            <div className="label-n">COMPARE REQUESTS (any two):</div>
                                {this.renderCompareData()}
                                {apiCatalog.compareRequests.length==2 &&
                                    <div style={{display: "flex", flexDirection: "row"}}>
                                        <div className="cube-btn text-center width-50" style={{margin: "0 5px 0 0"}} onClick={this.handleCompareSubmit}>COMPARE</div>
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
        )
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
    apiCatalog: state.apiCatalog,
});

const connectedAPICatalogFilter = connect(mapStateToProps)(APICatalogFilter);

export default connectedAPICatalogFilter;
export {connectedAPICatalogFilter as APICatalogFilter}