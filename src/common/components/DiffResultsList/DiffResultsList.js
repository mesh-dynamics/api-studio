import React, { Component, Fragment } from "react";
import {resolutionsIconMap} from "../../components/Resolutions.js";
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from "react-bootstrap";
import ReactDiffViewer from "../../utils/diff/diff-main";
import statusCodeList from "../../StatusCodeList"
import _ from "lodash";
import "../../components/Diff.css"

export default class DiffResultsList extends Component {
    constructor(props) {
        super(props);
        this.state = {
            searchFilterPath: "",

            shownResponseMessageHeaders: false,
            shownResponseMessageBody: true,
            shownRequestMessageHeaders: false,
            shownRequestMessageQParams: false,
            shownRequestMessageFParams: false,
            shownRequestMessageBody: false,
        }
        //this.selectedResolutionType = "All";
        //this.resolutionTypes = [{value: "ERR", count: 2}];
        this.inputElementRef = React.createRef();
    }

    newStyles = {
        variables: {
            addedBackground: "#e6ffed !important",
            addedColor: "#24292e  !important",
            removedBackground: "#ffeef0  !important",
            removedColor: "#24292e  !important",
            wordAddedBackground: "#acf2bd  !important",
            wordRemovedBackground: "#fdb8c0  !important",
            addedGutterBackground: "#cdffd8  !important",
            removedGutterBackground: "#ffdce0  !important",
            gutterBackground: "#f7f7f7  !important",
            gutterBackgroundDark: "#f3f1f1  !important",
            highlightBackground: "#fffbdd  !important",
            highlightGutterBackground: "#fff5b1  !important",
        },
        line: {
            padding: "10px 2px",
            "&:hover": {
                background: "#f7f7f7",
            },
        }
    };

    handleMetaDataSelect = (metaDataType, value) => {
    }

    toggleMessageContents = (e) => {
        const { updateDiffToggleRibbon } = this.props;
        console.log("I AM TRIGGERED:::::::::::::");

        switch (e.target.value) {
            case "responseHeaders":
                updateDiffToggleRibbon({
                    showResponseMessageHeaders: e.target.checked, 
                    shownResponseMessageHeaders: true 
                })
                this.setState
                break;
        
            case "responseBody":
                updateDiffToggleRibbon({ 
                    showResponseMessageBody: e.target.checked,
                    shownResponseMessageBody: true
                });
                break;

            case "requestHeaders":
                updateDiffToggleRibbon({ 
                    showRequestMessageHeaders: e.target.checked,
                    shownRequestMessageHeaders: true
                });
                break;

            case "requestQParams":
                updateDiffToggleRibbon({ 
                    showRequestMessageQParams: e.target.checked,
                    shownRequestMessageQParams: true
                });
                break;

            case "requestFParams":
                updateDiffToggleRibbon({ 
                    showRequestMessageFParams: e.target.checked,
                    shownRequestMessageFParams: true
                });
                break;

            case "requestBody":
                updateDiffToggleRibbon({ 
                    showRequestMessageBody: e.target.checked, 
                    shownRequestMessageBody: true 
                });
                break;
        }
        
        setTimeout(() => {
            const { 
                    diffToggleRibbon: { 
                        showResponseMessageHeaders, 
                        showResponseMessageBody, 
                        showRequestMessageHeaders, 
                        showRequestMessageQParams, 
                        showRequestMessageFParams, 
                        showRequestMessageBody 
                    }
                } = this.props;

            if(showResponseMessageHeaders === false && showResponseMessageBody === false && showRequestMessageHeaders === false &&  showRequestMessageQParams === false && showRequestMessageFParams === false && showRequestMessageBody === false) {
                updateDiffToggleRibbon({ showResponseMessageBody: true, shownResponseMessageBody: true });
            }
        });

    }

    getHttpStatus = (code) => {
        for (let httpStatus of statusCodeList) {
            if (code == httpStatus.status) {
                return httpStatus.value;
            }
        }

        return code;
    };

    handleSearchFilterChange = (e) => {
        //const { history } = this.props;

        this.setState({ searchFilterPath: e.target.value });

        //this.historySearchParams = updateSearchHistoryParams("searchFilterPath", e.target.value, this.state);

        // history.push({
        //     pathname: "/shareable_link",
        //     search: this.historySearchParams
        // });
    }

    renderToggleRibbon = () => {
        const { 
            diffToggleRibbon: {
                showResponseMessageBody, // Response Message Body
                showResponseMessageHeaders, // Response Message Headers
                showRequestMessageHeaders, // Request Message Headers
                showRequestMessageQParams, // Request Message Q Params
                showRequestMessageFParams, // Request Message F Params
                showRequestMessageBody,// Request Message Body
            }
        } = this.props;

        console.log(this.props.diffToggleRibbon);

        return (
            <Fragment>
                <FormGroup>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestHeaders" checked={showRequestMessageHeaders}>Request Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestQParams" checked={showRequestMessageQParams}>Request Query Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestFParams" checked={showRequestMessageFParams}>Request Form Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestBody" checked={showRequestMessageBody}>Request Body</Checkbox>
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={showResponseMessageHeaders}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={showResponseMessageBody} >Response Body</Checkbox>
                        
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
        const { 
            diffLayoutData, 
            diffToggleRibbon: {
                showResponseMessageBody, // Response Message Body
                showResponseMessageHeaders, // Response Message Headers
                showRequestMessageHeaders, // Request Message Headers
                showRequestMessageQParams, // Request Message Q Params
                showRequestMessageFParams, // Request Message F Params
                showRequestMessageBody,// Request Message Body
                // shownRequestMessageBody, // Request Message Body
                // // shownResponseMessageBody, // Response Message Body
                // shownRequestMessageHeaders,// Request Message Headers
                // shownResponseMessageHeaders,  // Response Message Headers
                // shownRequestMessageQParams, // Request Message Q Params
                // shownRequestMessageFParams, // Request Message F Params
            }
        } = this.props;

        const {
            shownRequestMessageBody, // Request Message Body
            // shownResponseMessageBody, // Response Message Body
            shownRequestMessageHeaders,// Request Message Headers
            shownResponseMessageHeaders,  // Response Message Headers
            shownRequestMessageQParams, // Request Message Q Params
            shownRequestMessageFParams, // Request Message F Params
        } = this.state;

        console.log(this.props.diffToggleRibbon)
        
        const { searchFilterPath } = this.state;

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
                {(showRequestMessageHeaders || shownRequestMessageHeaders) && (
                    <div style={{ display: showRequestMessageHeaders ? "" : "none" }}>
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
                {(showRequestMessageQParams || shownRequestMessageQParams) && (
                    <div style={{ display: showRequestMessageQParams ? "" : "none" }}>
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
                {(showRequestMessageFParams || shownRequestMessageFParams) && (
                    <div style={{ display: showRequestMessageFParams ? "" : "none" }}>
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
                {(showRequestMessageBody || shownRequestMessageBody) && (
                    <div style={{ display: showRequestMessageBody ? "" : "none" }}>
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
                {(showResponseMessageHeaders || shownResponseMessageHeaders) && (
                    <div style={{ display: showResponseMessageHeaders ? "" : "none" }}>
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
                                showAll={this.props.showAll}
                                searchFilterPath={searchFilterPath}
                                filterPaths={item.filterPaths}
                                inputElementRef={this.inputElementRef}
                            />
                        </div>
                    </div>
                )}
                {(
                    <div style={{ display: showResponseMessageBody ? "" : "none" }}>
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
                                    showAll={this.props.showAll}
                                    searchFilterPath={searchFilterPath}
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
