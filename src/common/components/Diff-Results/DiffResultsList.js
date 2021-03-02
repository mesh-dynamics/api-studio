import React, { Component, Fragment } from "react";
import { 
    Checkbox, 
    FormGroup, 
    FormControl, 
    Glyphicon, 
    Label, 
    ButtonGroup, Button
} from "react-bootstrap";
import ReactDiffViewer from "../../utils/diff/diff-main";
import DiffResultsMissingItems from "./DiffResultsMissingItems";
import statusCodeList from "../../status-code-list";
import _ from "lodash";
import "../Diff.css";
import config from "../../config.js";
import { history } from '../../helpers';
import { addCompressToggleData } from "../../utils/diff/diff-process";

export default class DiffResultsList extends Component {
    constructor(props) {
        super(props);

        this.state = {           
            diffLayoutData: props.diffLayoutData,

            searchFilterPath: "",

            // the 'shown' flags should match the corresponding 'show' flags coming from the initial props
            // because we have shown the ones having 'show' true
            shownResponseMessageHeaders: props.showResponseMessageHeaders, 
            shownResponseMessageBody: props.showResponseMessageBody, 
            shownRequestMessageHeaders: props.showRequestMessageHeaders, 
            shownRequestMessageQParams: props.showRequestMessageQParams, 
            shownRequestMessageFParams: props.showRequestMessageFParams, 
            shownRequestMessageBody: props.showRequestMessageBody, 

            showFragments: false,

            collapseLength: parseInt(config.diffCollapseLength),
            collapseLengthIncrement: parseInt(config.diffCollapseLengthIncrement),

            maxLinesLength: parseInt(config.diffMaxLinesLength),
            maxLinesLengthIncrement: parseInt(config.diffMaxLinesLengthIncrement),
            enableClientSideDiff: config.enableClientSideDiff === "true" ? true : false
        }
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

    componentWillReceiveProps = (newProps) => {
        this.setState({diffLayoutData: newProps.diffLayoutData})
    }

    toggleShowFragments = () => {
        const {showFragments} = this.state;
        this.setState({showFragments: !showFragments})
    }

    toggleMessageContents = (e) => {
        const { updateDiffToggleRibbon } = this.props;
        switch (e.target.value) {
            case "responseHeaders":
                updateDiffToggleRibbon({
                    showResponseMessageHeaders: e.target.checked, 
                })
                this.setState({shownResponseMessageHeaders: true})
                break;
        
            case "responseBody":
                updateDiffToggleRibbon({ 
                    showResponseMessageBody: e.target.checked,
                });
                this.setState({shownResponseMessageBody: true})
                break;

            case "requestHeaders":
                updateDiffToggleRibbon({ 
                    showRequestMessageHeaders: e.target.checked,
                });
                    this.setState({shownRequestMessageHeaders: true})
                break;

            case "requestQParams":
                updateDiffToggleRibbon({ 
                    showRequestMessageQParams: e.target.checked,
                });
                this.setState({shownRequestMessageQParams: true})
                break;

            case "requestFParams":
                updateDiffToggleRibbon({ 
                    showRequestMessageFParams: e.target.checked,
                });
                this.setState({shownRequestMessageFParams: true})
                break;

            case "requestBody":
                updateDiffToggleRibbon({ 
                    showRequestMessageBody: e.target.checked,  
                });
                this.setState({shownRequestMessageBody: true})
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
        this.setState({ searchFilterPath: e.target.value });
    }

    handleStartIndexChange = (e) => {
        let value = parseInt(e.target.value)
        if (e.key !== "Enter")
            return;

        if (!(value >= 0)) {
            alert("Invalid index value")
            console.error("Invalid index value")
            return;
        }
        
        this.props.handlePageNav(true, value);
    }

    handleViewTraceClick = (recordTraceId) => {
        history.push({
            pathname: '/view_trace',
            search: `${window.location.search}&traceId=${recordTraceId}`,
        })
    };

    increaseCollapseLength = (e, jsonPath, recordReqId, replayReqId, typeOfChunkHandler) => {
        const { collapseLength, collapseLengthIncrement, diffLayoutData, maxLinesLength, maxLinesLengthIncrement } = this.state;
        let newCollapseLength = collapseLength, newMaxLinesLength = maxLinesLength;
        if(typeOfChunkHandler === "collapseChunkLength") {
            newCollapseLength = collapseLength + collapseLengthIncrement;
        } else {
            newMaxLinesLength = maxLinesLength + maxLinesLengthIncrement;
        }

        let newDiffLayoutData = diffLayoutData.map(diffItem => {
            if (diffItem.replayReqId === replayReqId) {
                addCompressToggleData(diffItem.reductedDiffArray, newCollapseLength, newMaxLinesLength);
            }
            return diffItem;
        });

        this.setState({ 
            collapseLength: newCollapseLength, 
            maxLinesLength: newMaxLinesLength,
            diffLayoutData: newDiffLayoutData,
        });
    }

    renderSectionLabel = () => (
        <div className="row margin-top-10">
            <div className="col-md-6">
                <span className="diff-section-label">Baseline</span>
            </div>
            <div className="col-md-6">
                <span className="diff-section-label shift-left-align">Test</span>
            </div>
        </div>
    );
    
    // page navigation
    renderPageNav = () => {
        const {startIndex, endIndex, numResults} = this.props;
        return(
            <div>
                <ButtonGroup>
                    <Button onClick={() => this.props.handlePageNav(false, startIndex)} disabled={(startIndex === 0) || (numResults === 0)}>&lt;</Button>
                    <Button onClick ={() => this.props.handlePageNav(true, endIndex)} disabled={(endIndex === numResults) || (numResults === 0)}>&gt;</Button>
                    
                </ButtonGroup>
                <div class="checkbox-inline">
                    <label class="checkbox-inline" style={{paddingLeft: 0}}>
                        Results 
                    </label>
                    <input class="checkbox-inline" defaultValue={startIndex} onKeyPressCapture={this.handleStartIndexChange} style={{width: "60px"}}/>
                    <label class="checkbox-inline" style={{paddingLeft: 0}}>
                    to {endIndex} of {numResults}
                    </label>
                </div>
            </div>
        )
    }

    renderRespTimeAndMethod = (isReplay, item)=>{
        let timeMs = 0;
        let method = "";
        if(!isReplay){
            timeMs = item.recordRespTime - item.recordReqTime;
            if(item.recordRequest){
                method = item.recordRequest.method;
            }
        }else{
            timeMs = item.replayRespTime - item.replayReqTime;
            if(item.replayRequest){
                method = item.replayRequest.method;
            }
        }
        const timeSeconds = timeMs/1000;
        const decimalSec = timeMs % 1000;
        var hours = Math.floor(timeSeconds / 3600) % 24;  
         var minutes = Math.floor(timeSeconds / 60) % 60;
         var seconds = Math.floor(timeSeconds % 60);
        return <>&nbsp;&nbsp;<span className="font-12">Time: <span className="green">
            {hours > 0? hours + "h ":""}
            {minutes > 0? minutes + "m ":""}
            {seconds > 0? seconds + "s ":""}
            { decimalSec + "ms "}
            </span></span>
            {method && <span className="font-12">&nbsp;&nbsp;Method: <span className="green">
                {method}
            </span></span>}
        </>
    }

    getCheckBoxProps(isDiffError){
        if(isDiffError){
            return {className : "isDiffError"};
        }else{
            return {}
        }
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

                isRequestHdrsError,
                isRequestBodyError,
                isRequestQueryError,
                isRequestFormError,
                isResponseBodyError,
                isResponseHdrsError
            }
        } = this.props;

        return (
            <Fragment>
                <FormGroup>
                        <Checkbox inline onChange={this.toggleMessageContents} {...this.getCheckBoxProps(isRequestHdrsError)} value="requestHeaders" checked={showRequestMessageHeaders}>Request Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} {...this.getCheckBoxProps(isRequestQueryError)} value="requestQParams" checked={showRequestMessageQParams}>Request Query Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} {...this.getCheckBoxProps(isRequestFormError)} value="requestFParams" checked={showRequestMessageFParams}>Request Form Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} {...this.getCheckBoxProps(isRequestBodyError)} value="requestBody" checked={showRequestMessageBody}>Request Body</Checkbox>
                        
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        
                        <Checkbox inline onChange={this.toggleMessageContents} {...this.getCheckBoxProps(isResponseHdrsError)} value="responseHeaders" checked={showResponseMessageHeaders}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} {...this.getCheckBoxProps(isResponseBodyError)} value="responseBody" checked={showResponseMessageBody} >Response Body</Checkbox>
                        
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        
                        <Checkbox inline onChange={this.toggleShowFragments} checked={this.state.showFragments}>Show fragments only</Checkbox>
                        
                        <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.searchFilterPath}
                            placeholder="Search"
                            onChange={this.handleSearchFilterChange}
                            id="filterPathInputId"
                        />
                    </FormGroup>
            </Fragment>
        )
    }

    renderResultsList = () => {
        const newStyles = this.newStyles;
        const { 
            //diffLayoutData, 
            diffToggleRibbon: {
                showResponseMessageBody, // Response Message Body
                showResponseMessageHeaders, // Response Message Headers
                showRequestMessageHeaders, // Request Message Headers
                showRequestMessageQParams, // Request Message Q Params
                showRequestMessageFParams, // Request Message F Params
                showRequestMessageBody,// Request Message Body
            }
        } = this.props;

        const {
            diffLayoutData,
            shownRequestMessageBody, // Request Message Body
            // shownResponseMessageBody, // Response Message Body
            shownRequestMessageHeaders,// Request Message Headers
            shownResponseMessageHeaders,  // Response Message Headers
            shownRequestMessageQParams, // Request Message Q Params
            shownRequestMessageFParams, // Request Message F Params
        } = this.state;

        if (diffLayoutData.length == 0) {
            return (
                <div className="loading-text">
                    No Results Found
                </div>
            )
        }
        
        return diffLayoutData.map((item, index) => {
            const method = item.recordRequest?.method || "";
            return (<div key={item.recordReqId + "_" + index} style={{ borderBottom: "1px solid #eee", display: "block" }}>
                <div style={{ backgroundColor: "#EAEAEA", display: "flex", justifyContent: "space-between", alignItems: "center", padding: "5px" }}>
                    <div style={{display: "inline-block"}}>{item.path}</div>
                    <div style={{ marginTop: "5px" }}>
                        <Button 
                            bsSize="small" 
                            bsStyle={"primary"} 
                            style={{color: "#fff"}}
                            onClick={() => this.handleViewTraceClick(item.recordTraceId)}
                        >
                            <span><Glyphicon className="font-15" glyph="search" /> VIEW TRACE</span>
                        </Button>
                    </div>
                </div>
                {(showRequestMessageHeaders || shownRequestMessageHeaders) && (
                    <div style={{ display: showRequestMessageHeaders ? "" : "none" }}>
                        {this.renderSectionLabel()}
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
                                showAll={!this.state.showFragments}
                                filterPaths={item.filterPaths}
                                searchFilterPath={this.state.searchFilterPath}
                                enableClientSideDiff={true}
                                method={method}
                            />
                        </div>
                    </div>
                )}
                {(showRequestMessageQParams || shownRequestMessageQParams) && (
                    <div style={{ display: showRequestMessageQParams ? "" : "none" }}>
                        {this.renderSectionLabel()}
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
                                showAll={!this.state.showFragments}
                                filterPaths={item.filterPaths}
                                searchFilterPath={this.state.searchFilterPath}
                                enableClientSideDiff={true}
                                method={method}
                            />
                        </div>
                    </div>
                )}
                {(showRequestMessageFParams || shownRequestMessageFParams) && (
                    <div style={{ display: showRequestMessageFParams ? "" : "none" }}>
                        {this.renderSectionLabel()}
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
                                showAll={!this.state.showFragments}
                                filterPaths={item.filterPaths}
                                searchFilterPath={this.state.searchFilterPath}
                                enableClientSideDiff={true}
                                method={method}
                            />
                        </div>
                    </div>
                )}
                {(showRequestMessageBody || shownRequestMessageBody) && (
                    <div style={{ display: showRequestMessageBody ? "" : "none" }}>
                        {this.renderSectionLabel()}
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
                                showAll={!this.state.showFragments}
                                filterPaths={item.filterPaths}
                                searchFilterPath={this.state.searchFilterPath}
                                enableClientSideDiff={true}
                                method={method}
                            />
                        </div>
                    </div>
                )}
                {(showResponseMessageHeaders || shownResponseMessageHeaders) && (
                    <div style={{ display: showResponseMessageHeaders ? "" : "none" }}>
                        {this.renderSectionLabel()}
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
                                showAll={!this.state.showFragments}
                                searchFilterPath={this.state.searchFilterPath}
                                filterPaths={item.filterPaths}
                                inputElementRef={this.inputElementRef}
                                enableClientSideDiff={true}
                                method={method}
                            />
                        </div>
                    </div>
                )}
                {(
                    <div style={{ display: showResponseMessageBody ? "" : "none" }}>
                        {this.renderSectionLabel()}
                        <div className="row">
                            <div className="col-md-6">
                                <h4>
                                    <Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Body</Label>&nbsp;&nbsp;
                                    {item.recordResponse ? <><span className="font-12">Status:&nbsp;<span className="green">{this.getHttpStatus(item.recordResponse.status)}</span></span>{this.renderRespTimeAndMethod(false, item)}</> : <span className="font-12" style={{"color": "magenta"}}>No Recorded Data</span>}
                                </h4>
                            </div>

                            <div className="col-md-6">
                                <h4 style={{marginLeft: "18%"}}>
                                {item.replayResponse ? <><span className="font-12">Status:&nbsp;<span className="green">{this.getHttpStatus(item.replayResponse.status)}</span></span>{this.renderRespTimeAndMethod(true, item)}</> : <span className="font-12" style={{"color": "magenta"}}>No Replayed Data</span>}
                                </h4>
                            </div>
                        </div>
                        {
                            item.missedRequiredFields.length > 0 &&
                            <DiffResultsMissingItems 
                                missedRequiredFields={item.missedRequiredFields} 
                                method={method}
                            />
                        }
                        {(item.recordedData || item.replayedData) && (
                            <div className="diff-wrapper">
                                <ReactDiffViewer
                                    styles={newStyles}
                                    oldValue={item.expJSON}
                                    newValue={item.actJSON}
                                    splitView={true}
                                    disableWordDiff={false}
                                    diffArray={item.reductedDiffArray}
                                    filterPaths={item.filterPaths}
                                    onLineNumberClick={(lineId, e) => { return; }}
                                    inputElementRef={this.inputElementRef}
                                    showAll={!this.state.showFragments}
                                    searchFilterPath={this.state.searchFilterPath}
                                    handleCollapseLength={this.increaseCollapseLength}
                                    handleMaxLinesLength={this.increaseCollapseLength}
                                    enableClientSideDiff={this.state.enableClientSideDiff}
                                    method={method}
                                />
                            </div>
                        )}
                    </div>
                )}
            </div>);
        });
    }

    renderLoading = () => {
        return (
            <div className={"loading-text"}>
                Loading...
            </div>
        );
    }

    render() {
        return (
            <div>
                {this.props.isFetching 
                ? this.renderLoading() 
                : 
                <Fragment>
                    {this.renderPageNav()}
                    {this.renderToggleRibbon()}
                    {this.renderResultsList()}
                    {this.renderPageNav()}
                </Fragment>}
            </div>
        )
    }
}
