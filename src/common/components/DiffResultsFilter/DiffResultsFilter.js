import React, { Component, Fragment } from 'react'
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';
import {cubeActions} from "../../actions";
import _ from 'lodash';
import {resolutionsIconMap} from '../../components/Resolutions.js'

export default class DiffResultsFilter extends Component {

    constructor(props) {
        super(props);
    }

    renderPageButtons = () => {
        const { pages = 1 } = this.props;
        let pageButtons = [];

        /* todo: use this for refactor
        const { 
            facetListData: { pages },
            filter: { currentPageNumber }
        } = this.props;
    
        return pages && _.isEmpty(pages) 
            ? 
            <Button
                onClick={() => this.handleMetaDataSelect("currentPageNumber", 1)} 		
                bsStyle="primary"
            />
            :
            <Fragment>
                pages.map(pageNumber => 
                        <Button 
                            onClick={() => this.handleMetaDataSelect("currentPageNumber",1)} 
                            bsStyle={currentPageNumber === pageNumber ? "primary" : "default"}
                        />
                    );
            </Fragment>
        */
        for(let idx = 1; idx <= pages; idx++) {
            pageButtons.push(
                <Button onClick={() => this.handleMetaDataSelect("currentPageNumber", idx)} bsStyle={this.props.filter.currentPageNumber === idx ? "primary" : "default"}>{idx}</Button>
            );
        }
        return pageButtons;
    }
    
    handleMetaDataSelect = (metaDataType, value) => {
        this.props.filterChangeHandler(metaDataType, value);
    }

    renderServiceDropdown() {
        const {facetListData} = this.props;
        const services = _.isEmpty(facetListData.services) ? {} : facetListData.services;
        const selectedService = this.props.filter.selectedService;
        const servicesEntries = Object.entries(services);
        let totalServiceCounts = servicesEntries.reduce((accumulator, [service, count]) => accumulator += count, 0);
        
        return (
            <Fragment>
                <DropdownButton title={selectedService} id="dropdown-size-medium">
                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedService", "All")}>
                        <Glyphicon style={{ visibility: selectedService === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({totalServiceCounts})
                    </MenuItem>
                    <MenuItem divider />
                    {servicesEntries.map(([service, count]) => {return (
                    <MenuItem key={service + "-" + count} eventKey={service} onClick={() => this.handleMetaDataSelect("selectedService", service)}>
                        <Glyphicon style={{ visibility: selectedService === service ? "visible" : "hidden" }} glyph="ok" /> {service} ({count})
                    </MenuItem>);
                    })}
                </DropdownButton>
            </Fragment>
        )
    }
    
    renderAPIPathDropdown() {
        const {facetListData} = this.props;
        const apiPaths = _.isEmpty(facetListData.apiPaths) ? {"minfo/listmovies": 103, "minfo/liststores": 100,  "minfo/rentmovie": 56, "minfo/returnmovie": 56} : facetListData.apiPaths; 
        const selectedAPI = this.props.filter.selectedAPI; 
        console.log(this.props)
        const apiPathEntries = Object.entries(apiPaths);
        let totalAPIPathCounts = apiPathEntries.reduce((accumulator, [apiPath, count]) => accumulator += count, 0);
        
        return (
            <Fragment>
                <DropdownButton title={selectedAPI ? selectedAPI : "Select API Path"} id="dropdown-size-medium">
                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedAPI", "All")}>
                        <Glyphicon style={{ visibility: selectedAPI === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({totalAPIPathCounts})
                    </MenuItem>
                    <MenuItem divider />
                    {apiPathEntries.map(([apiPath, count]) => {return (
                        <MenuItem key={apiPath+ "-" + count} eventKey={apiPath} onClick={() => this.handleMetaDataSelect("selectedAPI", apiPath)}>
                            <Glyphicon style={{ visibility: selectedAPI === apiPath ? "visible" : "hidden" }} glyph="ok" /> {apiPath} ({count})
                        </MenuItem>);
                    })}
                </DropdownButton>
            </Fragment>
        )
    }

    renderServiceAPIBreadcrumb() {
        return (
            <Fragment>
                <Breadcrumb.Item href="javascript:void(0);">
                        <strong>Service:&nbsp;</strong>
                        {this.renderServiceDropdown()}
                    </Breadcrumb.Item>

                    <Breadcrumb.Item active>
                        <strong>API Path:&nbsp;</strong>
                        {this.renderAPIPathDropdown()}
                    </Breadcrumb.Item>
            </Fragment>
        );
    }

    getResolutionTypeDescription = (resolutionType) => {
        switch (resolutionType) {
            case "All":
                return "All"
            
            case "ERR*":
                return "All Errors"
            
            default:
                return resolutionsIconMap[resolutionType] ? resolutionsIconMap[resolutionType].description : "(Unknown) [" + resolutionType + "]";
        }
    }

    // build the list of menu items for resolution types, based on whether they are for error types
    resolutionTypeMenuItems = (resolutionTypes, errKind) => {
        const {filter} = this.props;
        // get the selected value of the resolution type
        const selectedResolutionType = filter.selectedResolutionType;

        let resTypeMenuJsx = (item, index) => {
            return (
            <MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResolutionType", item.val)}>
                <Glyphicon style={{ visibility: selectedResolutionType === item.val ? "visible" : "hidden" }} glyph="ok" /> {this.getResolutionTypeDescription(item.val)} ({item.count})
            </MenuItem>);
        }

        return resolutionTypes.filter((item) => {
            return ((errKind == "error") ? item.val.indexOf("ERR_") > -1 : item.val.indexOf("ERR_") == -1);
        }).map(resTypeMenuJsx);
    }

    // render the resolution types dropdown    
    renderResolutionTypesDropdown = () => {
        const {filter, facetListData} = this.props;
        const selectedResolutionType = filter.selectedResolutionType;
        const resolutionTypes = _.isEmpty(facetListData.resolutionTypes) 
        ? 
        [{
            "val": "ERR_ValTypeMismatch",
            "count": 551
        },
        {
            "val": "OK_Ignore",
            "count": 417
        },
        {
            "val": "ERR_Required",
            "count": 196
        },
        {
            "val": "OK_OtherValInvalid",
            "count": 167
        },
        {
            "val": "OK_Optional",
            "count": 162
        },
        {
            "val": "ERR_ValMismatch",
            "count": 14
        },
        {
            "val": "ERR",
            "count": 1
        },
        {
            "val": "OK_DefaultCT",
            "count": 1
        }] 
        : facetListData.resolutionTypes; // todo: remove
        
        return (
            <Fragment>
                <div style={{display: "inline-block"}}>
                    <label class="checkbox-inline">
                        Resolution Type:
                    </label>
                    <div style={{ paddingLeft: "9px", display: "inline-block" }}>
                        <DropdownButton title={this.getResolutionTypeDescription(selectedResolutionType)} id="dropdown-size-medium">
                            <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "All")}>
                                <Glyphicon style={{ visibility: selectedResolutionType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({resolutionTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                            </MenuItem>
                            <MenuItem divider />
                            <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "ERR*")}>
                                <Glyphicon style={{ visibility: selectedResolutionType === "ERR*" ? "visible" : "hidden" }} glyph="ok" /> All Errors ({resolutionTypes.filter((r) => {return r.val.indexOf("ERR_") > -1}).reduce((accumulator, item) => accumulator += item.count, 0)})
                            </MenuItem>
                            {this.resolutionTypeMenuItems(resolutionTypes, "error")}
                            <MenuItem divider />
                            {this.resolutionTypeMenuItems(resolutionTypes, "other")}
                        </DropdownButton>
                    </div>
                </div>
            </Fragment>
        )
    }
    
    renderDiffTypesDropdown = () => {
        const {filter} = this.props;
        const {selectedDiffType} = filter;
        console.log("sew" + this.getDiffTypeDescription(selectedDiffType));
        
        return (
            <Fragment>
                <div style={{display: "inline-block"}}>
                    <label class="checkbox-inline">
                        Diff Type:
                    </label>
                    <div style={{ paddingLeft: "9px", display: "inline-block" }}>
                        <DropdownButton title={this.getDiffTypeDescription(selectedDiffType)} id="dropdown-size-medium">
                            {["All", "requestDiff", "responseDiff"].map((diffType) => {
                                return (
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedDiffType", diffType)}>
                                        <Glyphicon style={{ visibility: selectedDiffType === diffType ? "visible" : "hidden" }} glyph="ok" /> 
                                        {this.getDiffTypeDescription(diffType)} 
                                    </MenuItem>
                                );
                            })}
                        </DropdownButton>
                    </div>
                </div>
            </Fragment>
        );
    }   

    getDiffTypeDescription = (diffType) => {
        switch (diffType){
            case "All":
                return "All";
            case "requestDiff":
                return "Request diff";
            case "responseDiff":
                return "Response diff";
            default:
                return "(Unknown) [" + diffType + "]";
        }
    }

    renderMatchCompareRibbon = () => {
        const {filter} = this.props;
        const selectedReqMatchType = filter.selectedReqMatchType;
        return (
            <Fragment>
                <div style={{ marginBottom: "18px" }}>
                    <DropdownButton title={selectedReqMatchType === "match" ? "Matched Requests" : "Mismatched Requests"} id="dropdown-size-medium">
                        <MenuItem onClick={() => this.handleMetaDataSelect("selectedReqMatchType", "match")}>
                            <Glyphicon style={{ visibility: selectedReqMatchType === "match" ? "visible" : "hidden" }} glyph="ok" />
                            Matched Requests
                        </MenuItem>
                        <MenuItem onClick={() => this.handleMetaDataSelect("selectedReqMatchType", "mismatch")}>
                            <Glyphicon style={{ visibility: selectedReqMatchType === "mismatch" ? "visible" : "hidden" }} glyph="ok" />
                            Mismatched Requests
                        </MenuItem>
                    </DropdownButton>
                        {(selectedReqMatchType === "match") && this.renderDiffTypesDropdown()}
                        {(selectedReqMatchType === "match") && this.renderResolutionTypesDropdown()}
                        {/* {(selectedReqMatchType === "match") && this.renderReqResolutionTypesDropdown()}
                    
                        {(selectedReqMatchType === "match") && this.renderRespResolutionTypesDropdown()} */}
                    
                </div>
            </Fragment>
        );
    }

    render() {
        return (
            <div>
                <Breadcrumb style={{}}>
                    <Breadcrumb.Item href="/">{this.props.app}</Breadcrumb.Item>
                    {this.renderServiceAPIBreadcrumb()}
                </Breadcrumb>

                {/* <div style={{ marginBottom: "18px" }}>
                    {this.renderSelectReqRespRadio()}
                <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px"}}></span>
                        
                {this.renderResolutionTypesDropdown()}
                </div> */}
                {this.renderMatchCompareRibbon()}
                <ButtonGroup style={{ marginBottom: "9px", width: "100%" }}>
                    <div style={{ textAlign: "left" }}>
                        {this.renderPageButtons()}
                    </div>
                </ButtonGroup>
            </div>
        )
    }

    /* leaving this code here so that it can be used later if needed
    // render the request resolution type dropdown
    renderReqResolutionTypesDropdown = () => {
        const {filter, facetListData} = this.props;
        const selectedReqCompareResType = filter.selectedReqCompareResType;
        const resolutionTypes = _.isEmpty(facetListData.resolutionTypes) ? [] : facetListData.resolutionTypes;
        
        return (
            <Fragment>
                <div style={{display: "inline-block"}}>
                    <label class="checkbox-inline">
                        Request Compare Resolution Type:
                    </label>
                    <div style={{ paddingLeft: "9px", display: "inline-block" }}>
                        <DropdownButton title={this.getResolutionTypeDescription(selectedReqCompareResType)} id="dropdown-size-medium">
                            <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedReqCompareResType", "All")}>
                                <Glyphicon style={{ visibility: selectedReqCompareResType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({resolutionTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                            </MenuItem>
                            <MenuItem divider />
                            <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedReqCompareResType", "ERR*")}>
                                <Glyphicon style={{ visibility: selectedReqCompareResType === "ERR*" ? "visible" : "hidden" }} glyph="ok" /> All Errors ({resolutionTypes.filter((r) => {return r.val.indexOf("ERR_") > -1}).reduce((accumulator, item) => accumulator += item.count, 0)})
                            </MenuItem>
                            {this.resolutionTypeMenuItems(resolutionTypes, "error", "request")}
                            <MenuItem divider />
                            {this.resolutionTypeMenuItems(resolutionTypes, "other", "request")}
                        </DropdownButton>
                    </div>
                </div>
            </Fragment>
        )
    }

    // render the response resolution type dropdown
    renderRespResolutionTypesDropdown = () => {
        const {filter, facetListData} = this.props;
        const selectedRespCompareResType = filter.selectedRespCompareResType;
        const resolutionTypes = _.isEmpty(facetListData.resolutionTypes) ? [] : facetListData.resolutionTypes;
        
        return (
            <Fragment>
                <div style={{display: "inline-block"}}>
                    <label class="checkbox-inline">
                    Response Compare Resolution Type:
                    </label>
                    <div style={{ paddingLeft: "9px", display: "inline-block" }}>
                        <DropdownButton title={this.getResolutionTypeDescription(selectedRespCompareResType)} id="dropdown-size-medium">
                            <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedRespCompareResType", "All")}>
                                <Glyphicon style={{ visibility: selectedRespCompareResType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({resolutionTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                            </MenuItem>
                            <MenuItem divider />
                            <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedRespCompareResType", "ERR*")}>
                                <Glyphicon style={{ visibility: selectedRespCompareResType === "ERR*" ? "visible" : "hidden" }} glyph="ok" /> All Errors ({resolutionTypes.filter((r) => {return r.val.indexOf("ERR_") > -1}).reduce((accumulator, item) => accumulator += item.count, 0)})
                            </MenuItem>
                            {this.resolutionTypeMenuItems(resolutionTypes, "error", "response")}
                            <MenuItem divider />
                            {this.resolutionTypeMenuItems(resolutionTypes, "other", "response")}
                        </DropdownButton>
                    </div>
                </div>
            </Fragment>
        )
    }
    // build the list of menu items for resolution types, based on whether they are for error types, and for request or response
    resolutionTypeMenuItems = (resolutionTypes, errKind, reqRespKind) => {
        const {filter} = this.props;
        // get whether we are dealing with request compare or response compare resolution type
        const compareMetaResolutionType = (reqRespKind === "request") ? "selectedReqCompareResType" : "selectedRespCompareResType";
        // get the actual selected value of the resolution type
        const selectedCompareResType = filter[compareMetaResolutionType];

        let resTypeMenuJsx = (item, index) => {
            return (
            <MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect(compareMetaResolutionType, item.value)}>
                <Glyphicon style={{ visibility: selectedCompareResType === item.val ? "visible" : "hidden" }} glyph="ok" /> {this.getResolutionTypeDescription(item.val)} ({item.count})
            </MenuItem>);
        }

        return resolutionTypes.filter((item) => {
            return ((errKind == "error") ? item.val.indexOf("ERR_") > -1 : item.val.indexOf("ERR_") == -1);
        }).map(resTypeMenuJsx);
    }

    */

}
