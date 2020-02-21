import React, { Component, Fragment } from 'react'
import {resolutionsIconMap} from '../../components/Resolutions.js'
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';

export default class DiffResultsList extends Component {
    constructor(props) {
        super(props);
        this.state = {
            showRequestMessageHeaders: false,
            showRequestMessageQParams: false,
            showRequestMessageFParams: false,
            showRequestMessageBody: false,
            showResponseMessageHeaders: false,
            showResponseMessageBody: true,
        }
        this.selectedResolutionType = "All";
        this.resolutionTypes = [{value: "ERR", count: 2}];
    }

    handleMetaDataSelect = (metaDataType, value) => {
    }

    resolutionTypeMenuItems = (type) => {
        let resTypeMenuJsx = (item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResolutionType", item.value)}>
                <Glyphicon style={{ visibility: this.selectedResolutionType === item.value ? "visible" : "hidden" }} glyph="ok" /> {resolutionsIconMap[item.value].description} ({item.count})
            </MenuItem>);
        }

        return this.resolutionTypes.filter((item) => {
            return ((type == "error") ? item.value.indexOf("ERR_") > -1 : item.value.indexOf("ERR_") == -1);
        }).map(resTypeMenuJsx);
    }

    toggleMessageContents = (e) => {
        if (e.target.value === "responseHeaders") this.setState({ showResponseMessageHeaders: e.target.checked, shownResponseMessageHeaders: true });
        if (e.target.value === "responseBody") this.setState({ showResponseMessageBody: e.target.checked, shownResponseMessageBody: true });
        if (e.target.value === "requestHeaders") this.setState({ showRequestMessageHeaders: e.target.checked, shownRequestMessageHeaders: true });
        if (e.target.value === "requestQParams") this.setState({ showRequestMessageQParams: e.target.checked, shownRequestMessageQParams: true });
        if (e.target.value === "requestFParams") this.setState({ showRequestMessageFParams: e.target.checked, shownRequestMessageFParams: true });
        if (e.target.value === "requestBody") this.setState({ showRequestMessageBody: e.target.checked, shownRequestMessageBody: true });

        setTimeout(() => {
            const { showResponseMessageHeaders, showResponseMessageBody, showRequestMessageHeaders, showRequestMessageQParams, showRequestMessageFParams, showRequestMessageBody } = this.state;

            if(showResponseMessageHeaders === false && showResponseMessageBody === false && showRequestMessageHeaders === false &&  showRequestMessageQParams === false && showRequestMessageFParams === false && showRequestMessageBody === false) {
                this.setState({ showResponseMessageBody: true, shownResponseMessageBody: true });
            }
        });

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

    renderToggleRibbon = () => {
        // TODO
        let selectedResolutionType = this.selectedResolutionType;

        return (
            <Fragment>
                <FormGroup>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestHeaders" checked={this.state.showRequestMessageHeaders}>Request Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestQParams" checked={this.state.showRequestMessageQParams}>Request Query Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestFParams" checked={this.state.showRequestMessageFParams}>Request Form Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestBody" checked={this.state.showRequestMessageBody}>Request Body</Checkbox>
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={this.state.showResponseMessageHeaders}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={this.state.showResponseMessageBody} >Response Body</Checkbox>
                        
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px"}}></span>
                        <div style={{display: "inline-block"}}>
                            <label class="checkbox-inline">
                                Resolution Type:
                            </label>
                            <div style={{ paddingLeft: "9px", display: "inline-block" }}>
                                <DropdownButton title={this.getResolutionTypeDescription(selectedResolutionType)} id="dropdown-size-medium">
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "All")}>
                                        <Glyphicon style={{ visibility: selectedResolutionType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({this.resolutionTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    <MenuItem divider />
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "ERR")}>
                                        <Glyphicon style={{ visibility: selectedResolutionType === "ERR" ? "visible" : "hidden" }} glyph="ok" /> All Errors ({this.resolutionTypes.filter((r) => {return r.value.indexOf("ERR_") > -1}).reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    {this.resolutionTypeMenuItems("error")}
                                    <MenuItem divider />
                                    {this.resolutionTypeMenuItems("other")}
                                </DropdownButton>
                            </div>
                        </div>
                        {/* <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.searchFilterPath}
                            placeholder="Search"
                            onChange={this.handleSearchFilterChange}
                            id="filterPathInputId"
                            inputRef={ref => { this.input = ref; }}
                        /> */}
                    </FormGroup>
            </Fragment>
        )
    }

    render() {
        // TODO
        
        return (
            <div>
                {this.renderToggleRibbon()}
            </div>
        )
    }
}
