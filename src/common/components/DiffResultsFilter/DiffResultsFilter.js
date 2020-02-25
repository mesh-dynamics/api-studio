import React, { Component, Fragment } from 'react'
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';
import {cubeActions} from "../../actions";
import _ from 'lodash';
import {resolutionsIconMap} from '../../components/Resolutions.js'

export default class DiffResultsFilter extends Component {

    constructor(props) {
        super(props);
        this.state = {

        }
    }

    componentDidMount() {
    }


    renderPageButtons = () => {
        let pages = _.isEmpty(this.props.facetListData) ? 1 : this.props.facetListData.pages; // todo: props
        let pageButtons = [];
        for(let idx = 1; idx <= pages; idx++) {
            pageButtons.push(
                <Button onClick={() => this.handleMetaDataSelect("currentPageNumber", idx)} bsStyle={this.props.filter.currentPageNumber === idx ? "primary" : "default"} style={{}}>{idx}</Button>
            );
        }
        return pageButtons;
    }
    
    handleMetaDataSelect = (metaDataType, value) => {
        this.props.filterChangeHandler(metaDataType, value);
    }

    toggleMessageContents = (e) => {    
    }

    handleBackToDashboardClick = () => {
        const { history, dispatch } = this.props;
        dispatch(cubeActions.clearPathResultsParams());
    }

    renderServiceDropdown() {
        const services = _.isEmpty(this.props.facetListData) ? [] : this.props.facetListData.services;
        const selectedService = this.props.filter.selectedService;

        return (
            <Fragment>
                <DropdownButton title={selectedService} id="dropdown-size-medium">
                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedService", "All")}>
                        <Glyphicon style={{ visibility: selectedService === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({services.reduce((accumulator, item) => accumulator += item.count, 0)})
                    </MenuItem>
                    <MenuItem divider />
                    {services.map((item, index) => {return (
                    <MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedService", item.value)}>
                        <Glyphicon style={{ visibility: selectedService === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
                    </MenuItem>);
                    })}
                </DropdownButton>
            </Fragment>
        )
    }
    
    renderAPIPathDropdown() {
        const apiPaths = _.isEmpty(this.props.facetListData) ? [] : this.props.facetListData.apiPaths; 
        const selectedAPI = this.props.filter.selectedAPI; 

        return (
            <Fragment>
                <DropdownButton title={selectedAPI ? selectedAPI : "Select API Path"} id="dropdown-size-medium">
                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedAPI", "All")}>
                        <Glyphicon style={{ visibility: selectedAPI === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({apiPaths.reduce((accumulator, item) => accumulator += item.count, 0)})
                    </MenuItem>
                    <MenuItem divider />
                    {apiPaths.map((item, index) => {return (
                        <MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedAPI", item.value)}>
                            <Glyphicon style={{ visibility: selectedAPI === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
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

    renderSelectReqRespRadio() {
        return (
            <Fragment>
                <Radio inline value="responseMismatch" checked={this.props.filter.selectedReqRespMatchType === "responseMismatch"} onChange={() => this.handleMetaDataSelect("selectedReqRespMatchType", "responseMismatch")}> Response Mismatches only </Radio>
                <Radio inline value="requestMismatch" checked={this.props.filter.selectedReqRespMatchType === "requestMismatch"} onChange={() => this.handleMetaDataSelect("selectedReqRespMatchType", "requestMismatch")}> Request Mismatches only </Radio>
                <Radio inline value="All" checked={this.props.filter.selectedReqRespMatchType === "All"} onChange={() => this.handleMetaDataSelect("selectedReqRespMatchType", "All")}> All </Radio>
            </Fragment>
        )
    }

    getResolutionTypeDescription = (resolutionType) => {
        switch (resolutionType) {
            case "All":
                return "All"
            
            case "ERR":
                return "All Errors"
            
            default:
                return resolutionsIconMap[resolutionType].description;
        }
    }

    resolutionTypeMenuItems = (resolutionTypes, kind) => {
        let resTypeMenuJsx = (item, index) => {
            return (
            <MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResolutionType", item.value)}>
                <Glyphicon style={{ visibility: this.state.selectedResolutionType === item.value ? "visible" : "hidden" }} glyph="ok" /> {resolutionsIconMap[item.value].description} ({item.count})
            </MenuItem>);
        }

        return resolutionTypes.filter((item) => {
            return ((kind == "error") ? item.value.indexOf("ERR_") > -1 : item.value.indexOf("ERR_") == -1);
        }).map(resTypeMenuJsx);
    }

    renderResolutionTypesDropdown = () => {
        let selectedResolutionType = this.props.filter.selectedResolutionType;
        let resolutionTypes = _.isEmpty(this.props.facetListData) ? [] : this.props.facetListData.resolutionTypes;
        
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
                            <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "ERR")}>
                                <Glyphicon style={{ visibility: selectedResolutionType === "ERR" ? "visible" : "hidden" }} glyph="ok" /> All Errors ({resolutionTypes.filter((r) => {return r.value.indexOf("ERR_") > -1}).reduce((accumulator, item) => accumulator += item.count, 0)})
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

    render() {
        return (
            <div>
                <Breadcrumb style={{}}>
                    <Breadcrumb.Item href="/">{this.props.app}</Breadcrumb.Item>
                    {this.renderServiceAPIBreadcrumb()}
                </Breadcrumb>

                <div style={{ marginBottom: "18px" }}>
                    {this.renderSelectReqRespRadio()}
                <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px"}}></span>
                        
                {this.renderResolutionTypesDropdown()}
                </div>
                <ButtonGroup style={{ marginBottom: "9px", width: "100%" }}>
                    <div style={{ textAlign: "left" }}>
                        {this.renderPageButtons()}
                    </div>
                </ButtonGroup>
            </div>
        )
    }
}
