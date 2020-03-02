import React, { Component, Fragment } from 'react'
import {resolutionsIconMap} from '../../components/Resolutions.js'
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';
import ReactDiffViewer from '../../utils/diff/diff-main';
import statusCodeList from "../../StatusCodeList"
import _ from 'lodash';
import "../../components/Diff.css"

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
            searchFilterPath: "",
            showAll: true, // todo
            //selectedResolutionType: "All",
        }
        //this.selectedResolutionType = "All";
        //this.resolutionTypes = [{value: "ERR", count: 2}];
        this.inputElementRef = React.createRef();
    }

    newStyles = {
        variables: {
            addedBackground: '#e6ffed !important',
            addedColor: '#24292e  !important',
            removedBackground: '#ffeef0  !important',
            removedColor: '#24292e  !important',
            wordAddedBackground: '#acf2bd  !important',
            wordRemovedBackground: '#fdb8c0  !important',
            addedGutterBackground: '#cdffd8  !important',
            removedGutterBackground: '#ffdce0  !important',
            gutterBackground: '#f7f7f7  !important',
            gutterBackgroundDark: '#f3f1f1  !important',
            highlightBackground: '#fffbdd  !important',
            highlightGutterBackground: '#fff5b1  !important',
        },
        line: {
            padding: '10px 2px',
            '&:hover': {
                background: '#f7f7f7',
            },
        }
    };

    handleMetaDataSelect = (metaDataType, value) => {
    }

    // todo: remove
    resolutionTypeMenuItems = (kind) => {
        let resTypeMenuJsx = (item, index) => {
            return (
            <MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResolutionType", item.value)}>
                <Glyphicon style={{ visibility: this.state.selectedResolutionType === item.value ? "visible" : "hidden" }} glyph="ok" /> {resolutionsIconMap[item.value].description} ({item.count})
            </MenuItem>);
        }

        let resolutionTypes = _.isEmpty(this.props.facetListData) ? [] : this.props.facetListData.resolutionTypes;


        return resolutionTypes.filter((item) => {
            return ((kind == "error") ? item.value.indexOf("ERR_") > -1 : item.value.indexOf("ERR_") == -1);
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

    // todo: remove
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

    getHttpStatus = (code) => {
        for (let httpStatus of statusCodeList) {
            if (code == httpStatus.status) {
                return httpStatus.value;
            }
        }

        return code;
    };


    // todo: remove
    renderResolutionTypesDropdown = () => {
        let selectedResolutionType = this.state.selectedResolutionType;
        let resolutionTypes = _.isEmpty(this.props.facetListData) ? [] : this.props.facetListData.resolutionTypes;
        console.log(resolutionTypes)
        
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
                            {this.resolutionTypeMenuItems("error")}
                            <MenuItem divider />
                            {this.resolutionTypeMenuItems("other")}
                        </DropdownButton>
                    </div>
                </div>
            </Fragment>
        )
    }

    handleSearchFilterChange = (e) => {
        //const { history } = this.props;

        this.setState({ searchFilterPath: e.target.value });

        //this.historySearchParams = updateSearchHistoryParams("searchFilterPath", e.target.value, this.state);

        // history.push({
        //     pathname: '/shareable_link',
        //     search: this.historySearchParams
        // });
    }

    renderToggleRibbon = () => {
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
                        
                        {/* todo: remove */}
                        {/* <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px"}}></span>
                        
                        {this.renderResolutionTypesDropdown()} */}

                        <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.searchFilterPath}
                            placeholder="Search"
                            onChange={this.handleSearchFilterChange}
                            id="filterPathInputId"
                            //inputRef={ref => { this.input = ref; }}
                        />
                    </FormGroup>
            </Fragment>
        )
    }

    renderResultsList = () => {
        const newStyles = this.newStyles;

        const { diffLayoutData } = this.props;
        return diffLayoutData.map((item, index) => {
            return (<div key={item.recordReqId + "_" + index} style={{ borderBottom: "1px solid #eee", display: "block" }}>
                <div style={{ backgroundColor: "#EAEAEA", display: "flex", justifyContent: "space-between", alignItems: "center", padding: "5px" }}>
                    <div style={{display: "inline-block"}}>{item.path}</div>
                    <div style={{ marginTop: "5px" }}>
                        <Button bsSize="small" bsStyle={"primary"} href={"/view_trace" + this.historySearchParams + "&traceId=" + item.recordTraceId} syle={{color: "#fff"}}>
                            <span><Glyphicon className="font-15" glyph="search" /> VIEW TRACE</span>
                        </Button>
                    </div>
                </div>
                {(this.state.showRequestMessageHeaders || this.state.shownRequestMessageHeaders) && (
                    <div style={{ display: this.state.showRequestMessageHeaders ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Headers</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedRequestHeaders, undefined, 4)}
                                newValue={JSON.stringify(item.replayedRequestHeaders, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={item.reductedDiffArrayReqHeaders}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {(this.state.showRequestMessageQParams || this.state.shownRequestMessageQParams) && (
                    <div style={{ display: this.state.showRequestMessageQParams ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Query Params</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedRequestQParams, undefined, 4)}
                                newValue={JSON.stringify(item.replayedRequestQParams, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={item.reductedDiffArrayReqQParams}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {(this.state.showRequestMessageFParams || this.state.shownRequestMessageFParams) && (
                    <div style={{ display: this.state.showRequestMessageFParams ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Form Params</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedRequestFParams, undefined, 4)}
                                newValue={JSON.stringify(item.replayedRequestFParams, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={item.reductedDiffArrayReqFParams}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {(this.state.showRequestMessageBody || this.state.shownRequestMessageBody) && (
                    <div style={{ display: this.state.showRequestMessageBody ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Body</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedRequestBody, undefined, 4)}
                                newValue={JSON.stringify(item.replayedRequestBody, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={item.reductedDiffArrayReqBody}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {(this.state.showResponseMessageHeaders || this.state.shownResponseMessageHeaders) && (
                    <div style={{ display: this.state.showResponseMessageHeaders ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Headers</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedResponseHeaders, undefined, 4)}
                                newValue={JSON.stringify(item.replayedResponseHeaders, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={item.updatedReducedDiffArrayRespHdr}
                                onLineNumberClick={(lineId, e) => { return; }}
                                showAll={this.state.showAll}
                                searchFilterPath={this.state.searchFilterPath}
                                filterPaths={item.filterPaths}
                                inputElementRef={this.inputElementRef}
                            />
                        </div>
                    </div>
                )}
                {(
                    <div style={{ display: this.state.showResponseMessageBody ? "" : "none" }}>
                        <div className="row">
                            <div className="col-md-6">
                                <h4>
                                    <Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Body</Label>&nbsp;&nbsp;
                                    {item.recordResponse ? <span className="font-12">Status:&nbsp;<span className="green">{this.getHttpStatus(item.recordResponse.status)}</span></span> : <span className="font-12" style={{"color": "magenta"}}>No Recorded Data</span>}
                                </h4>
                            </div>

                            <div className="col-md-6">
                                <h4 style={{marginLeft: "18%"}}>
                                {item.replayResponse ? <span className="font-12">Status:&nbsp;<span className="green">{this.getHttpStatus(item.replayResponse.status)}</span></span> : <span className="font-12" style={{"color": "magenta"}}>No Replayed Data</span>}
                                </h4>
                            </div>
                        </div>
                        <div>
                            {item.missedRequiredFields.map((eachMissedField) => {
                                return(<div><span style={{paddingRight: "5px"}}>{eachMissedField.path}:</span><span>{eachMissedField.fromValue}</span></div>)
                            })}
                        </div>
                        {(item.recordedData || item.replayedData) && (
                            <div className="diff-wrapper">
                                < ReactDiffViewer
                                    styles={newStyles}
                                    oldValue={item.expJSON}
                                    newValue={item.actJSON}
                                    splitView={true}
                                    disableWordDiff={false}
                                    diffArray={item.reductedDiffArray}
                                    filterPaths={item.filterPaths}
                                    onLineNumberClick={(lineId, e) => { return; }}
                                    inputElementRef={this.inputElementRef}
                                    showAll={this.state.showAll}
                                    searchFilterPath={this.state.searchFilterPath}
                                />
                            </div>
                        )}
                    </div>
                )}
            </div >);
        });
    }

    render() {
        // TODO
        
        return (
            <div>
                {this.renderToggleRibbon()}
                {this.renderResultsList()}
            </div>
        )
    }
}
