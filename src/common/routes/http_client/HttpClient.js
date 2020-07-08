import  React , { Component, Fragment, createContext } from "react";
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Table, ButtonGroup, Button, Radio, Tabs, Tab} from 'react-bootstrap';

import HttpRequestMessage from "./HttpRequestMessage";
import HttpResponseMessage from "./HttpResponseMessage";

import ReactDiffViewer from '../../utils/diff/diff-main';
import config from "../../config";
import statusCodeList from "../../StatusCodeList";
import {resolutionsIconMap} from '../../components/Resolutions.js';

const newStyles = {
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

class HttpClient extends Component {

    constructor(props) {
        super(props);
        this.state = { 
            searchFilterPath: '',
            showResponseMessageHeaders: false,
            shownResponseMessageHeaders: false,
            showResponseMessageBody: true,
            shownResponseMessageBody: true,
            showRequestMessageHeaders: false,
            shownRequestMessageHeaders: false,
            showRequestMessageQParams: false,
            shownRequestMessageQParams: false,
            showRequestMessageFParams: false,
            shownRequestMessageFParams: false,
            showRequestMessageBody: false,
            shownRequestMessageBody: false,
            selectedService: "All",
            selectedAPI: "All",
            selectedRequestMatchType: "All",
            selectedResponseMatchType: "All",
            selectedResolutionType: "All",
            showAll: true,
            collapseLength: parseInt(config.diffCollapseLength),
            collapseLengthIncrement: parseInt(config.diffCollapseLengthIncrement),
            maxLinesLength: parseInt(config.diffMaxLinesLength),
            maxLinesLengthIncrement: parseInt(config.diffMaxLinesLengthIncrement),
            incrementCollapseLengthForRecReqId: null,
            incrementCollapseLengthForRepReqId: null,
            incrementStartJsonPath: null
        };
        this.toggleMessageContents = this.toggleMessageContents.bind(this);
        this.handleSearchFilterChange = this.handleSearchFilterChange.bind(this);
        this.increaseCollapseLength = this.increaseCollapseLength.bind(this);
    }

    increaseCollapseLength(e, jsonPath, recordReqId, replayReqId, typeOfChunkHandler) {
        const { collapseLength, collapseLengthIncrement, maxLinesLength, maxLinesLengthIncrement } = this.state;
        let newCollapseLength = collapseLength, newMaxLinesLength = maxLinesLength;
        if(typeOfChunkHandler === "collapseChunkLength") {
            newCollapseLength = collapseLength + collapseLengthIncrement;
        } else {
            newMaxLinesLength = maxLinesLength + maxLinesLengthIncrement;
        }
        this.setState({
            collapseLength: newCollapseLength, 
            maxLinesLength: newMaxLinesLength,
            incrementCollapseLengthForRecReqId: recordReqId,
            incrementCollapseLengthForRepReqId: replayReqId,
            incrementStartJsonPath: jsonPath
        });
    }

    addCompressToggleData(diffData, collapseLength, maxLinesLength) {
        let indx  = 0, atleastADiff = false;
        if(!diffData) return diffData;
        for (let i = config.diffCollapseStartIndex; i < diffData.length; i++) {
            let diffDataChunk = diffData[i];
            if(diffDataChunk.serverSideDiff !== null || (diffDataChunk.added || diffDataChunk.removed)) {
                let j = i - 1, chunkTopLength = 0;
                diffDataChunk["collapseChunk"] = false;
                atleastADiff = true;
                while (diffData[j] && diffData[j].serverSideDiff === null && chunkTopLength < collapseLength) {
                    diffData[j]["collapseChunk"] = false;
                    chunkTopLength++;
                    j--;
                }
                let k = i + 1, chunkBottomLength = 0;
                while (diffData[k] && diffData[k].serverSideDiff === null && chunkBottomLength < collapseLength) {
                    diffData[k]["collapseChunk"] = false;
                    chunkBottomLength++;
                    k++;
                }
            } else {
                if(!diffDataChunk.hasOwnProperty("collapseChunk")) diffDataChunk["collapseChunk"] = true;
            }
        }
        if(!atleastADiff) {
            for (let m = 0; m < collapseLength; m++) {
                let tempDiffDataChunk = diffData[m];
                if(tempDiffDataChunk) tempDiffDataChunk["collapseChunk"] = false;
                if(m >= diffData.length) break;
            }
        }
        let toggleDrawChunk  = false, arbitratryCount = 0;
        let jsonPath, previousChunk, showMaxChunkToggle = false, arrayCount = 0, activatedCount;
        for (let eachChunk of diffData) {
            eachChunk["showMaxChunk"] = false;
            eachChunk["showMaxChunkToggle"] = false;
            if(arbitratryCount >= maxLinesLength && !showMaxChunkToggle) {
                eachChunk["showMaxChunk"] = true;
                showMaxChunkToggle = true;
                activatedCount = arrayCount;
            }
            if(showMaxChunkToggle) {
                eachChunk["showMaxChunkToggle"] = true;
            }
            if(jsonPath === eachChunk.jsonPath && showMaxChunkToggle && activatedCount === arrayCount) {
                previousChunk["showMaxChunk"] = true;
            }
            if(eachChunk.collapseChunk === true && toggleDrawChunk === false) {
                toggleDrawChunk = true;
                eachChunk["drawChunk"] = true;
                arbitratryCount++;
            } else if(eachChunk.collapseChunk === true && toggleDrawChunk === true) {
                eachChunk["drawChunk"] = false;
            } else if(eachChunk.collapseChunk === false) {
                toggleDrawChunk = false;
                eachChunk["drawChunk"] = false;
                if(jsonPath !== eachChunk.jsonPath) {
                    arbitratryCount++;
                }
            } else if (!eachChunk.collapseChunk) {
                arbitratryCount++;
            }
            jsonPath = eachChunk.jsonPath;
            previousChunk = eachChunk;
            arrayCount++;
        }
        return diffData;
    }

    handleSearchFilterChange(e) {

        this.setState({ searchFilterPath: e.target.value });
    }

    toggleMessageContents(e) {
        const { history } = this.props;

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

    getHttpStatus = (code) => {
        for (let httpStatus of statusCodeList) {
            if (code == httpStatus.status) {
                return httpStatus.value;
            }
        }

        return code;
    }

    render() {
        const { diffLayoutData, showCompleteDiff } = this.props;
        const selectedDiffItem = diffLayoutData ? diffLayoutData[0] : null;
        console.log("diffLayoutData: ", diffLayoutData);
        let { selectedResolutionType, showTrace, showLogs, collapseLength, incrementCollapseLengthForRecReqId, incrementCollapseLengthForRepReqId, maxLinesLength } = this.state;
        let resolutionTypesForMenu = [];
        const filterFunction = (item, index, itself) => {
            item.count = itself.reduce((counter, currentItem, currentIndex) => {
                if(item.value === currentItem.value) return (counter + 1);
                else return (counter + 0);
            }, 0);
            let idx = -1;
            for(let i = 0; i < itself.length; i++) {
                if(itself[i].value === item.value) {
                    idx = i;
                    break; 
                }
            }
            return idx === index;
        };
        let diffLayoutDataResCount = diffLayoutData && diffLayoutData.map(eachItem => {
            let resolutionTypes = [];
            if (incrementCollapseLengthForRepReqId && eachItem.replayReqId === incrementCollapseLengthForRepReqId) {
                this.addCompressToggleData(eachItem.reductedDiffArray, collapseLength, maxLinesLength);
            }
            for (let eachJsonPathParsedDiff of eachItem.parsedDiff) {
                resolutionTypes.push({value: eachJsonPathParsedDiff.resolution, count: 0});
            }
            resolutionTypes = resolutionTypes.filter(filterFunction);
            return {
                ...eachItem,
                resolutionTypes
            };
        });
      
        let filterPaths = [];
        if(selectedDiffItem) {
            for (let eachJsonPathParsedDiff of selectedDiffItem.parsedDiff) {
                resolutionTypesForMenu.push({value: eachJsonPathParsedDiff.resolution, count: 0});
                if (selectedResolutionType === "All"
                || selectedResolutionType === eachJsonPathParsedDiff.resolution
                || (selectedResolutionType === "ERR" && eachJsonPathParsedDiff.resolution.indexOf("ERR_") > -1)) {
                    // add only the json paths we want to show in the diff
                    filterPaths.push(eachJsonPathParsedDiff.path);
                }
            }
        }
        resolutionTypesForMenu = resolutionTypesForMenu.filter(filterFunction);
        let resTypeMenuJsx = (item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResolutionType", item.value)}>
                <Glyphicon style={{ visibility: selectedResolutionType === item.value ? "visible" : "hidden" }} glyph="ok" /> {resolutionsIconMap[item.value].description} ({item.count})
            </MenuItem>);
        };
        
        let resolutionTypeErrorMenuItems 
                    = resolutionTypesForMenu.filter((item) => {
                        return item.value.indexOf("ERR_") > -1;
                    })
                    .map(resTypeMenuJsx);
        
        let resolutionTypeOtherMenuItems 
                    = resolutionTypesForMenu.filter((item) => {
                        return item.value.indexOf("ERR_") == -1;
                    })
                    .map(resTypeMenuJsx);
        let getResolutionTypeDescription = function (resolutionType) {
            switch (resolutionType) {
                case "All":
                    return "All"
                
                case "ERR*":
                case "ERR":
                    return "All Errors"
                
                default:
                    return resolutionsIconMap[resolutionType] ? resolutionsIconMap[resolutionType].description : "(Unknown) [" + resolutionType + "]";
            }
        };
        return (
            <div>
                <div>
                    <HttpRequestMessage tabId={this.props.tabId}
                        requestId={this.props.requestId}
                        httpMethod={this.props.httpMethod}
                        httpURL={this.props.httpURL}
                        headers={this.props.headers} 
                        queryStringParams={this.props.queryStringParams}
                        bodyType={this.props.bodyType}
                        formData={this.props.formData} 
                        rawData={this.props.rawData}
                        rawDataType={this.props.rawDataType}
                        addOrRemoveParam={this.props.addOrRemoveParam} 
                        updateParam={this.props.updateParam}
                        updateBodyOrRawDataType={this.props.updateBodyOrRawDataType}
                        driveRequest={this.props.driveRequest}
                        showOutgoingRequests={this.props.showOutgoingRequests}
                        showOutgoingRequestsBtn={this.props.showOutgoingRequestsBtn}
                        showSaveBtn={this.props.showSaveBtn}
                        showSaveModal={this.props.showSaveModal}
                        isOutgoingRequest={this.props.isOutgoingRequest} >
                    </HttpRequestMessage>
                    <HttpResponseMessage tabId={this.props.tabId}
                        updateParam={this.props.updateParam}
                        responseStatus={this.props.responseStatus}
                        responseStatusText={this.props.responseStatusText}
                        responseHeaders={this.props.responseHeaders}
                        responseBody={this.props.responseBody}
                        recordedResponseHeaders={this.props.recordedResponseHeaders}
                        recordedResponseBody={this.props.recordedResponseBody}
                        updateParam={this.props.updateParam}
                        isOutgoingRequest={this.props.isOutgoingRequest}
                        handleShowCompleteDiff={this.props.handleShowCompleteDiff} >
                    </HttpResponseMessage>
                </div>
                {showCompleteDiff && selectedDiffItem && (
                    <div style={{marginTop: "27px"}}>
                        <div style={{opacity: 0.6, marginTop: "9px"}}>
                            <h4><Glyphicon style={{ visibility:  "visible", paddingRight: "5px", fontSize: "14px" }} glyph="random" /> <span>Selected Diff</span></h4>
                        </div>
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
                                <label className="checkbox-inline">
                                    Resolution Type:
                                </label>
                                <div style={{ paddingLeft: "9px", display: "inline-block" }}>
                                    <DropdownButton title={getResolutionTypeDescription(selectedResolutionType)} id="dropdown-size-medium">
                                        <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "All")}>
                                            <Glyphicon style={{ visibility: selectedResolutionType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({resolutionTypesForMenu.reduce((accumulator, item) => accumulator += item.count, 0)})
                                        </MenuItem>
                                        <MenuItem divider />
                                        <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "ERR")}>
                                            <Glyphicon style={{ visibility: selectedResolutionType === "ERR" ? "visible" : "hidden" }} glyph="ok" /> All Errors ({resolutionTypesForMenu.filter((r) => {return r.value.indexOf("ERR_") > -1}).reduce((accumulator, item) => accumulator += item.count, 0)})
                                        </MenuItem>
                                        {resolutionTypeErrorMenuItems}
                                        <MenuItem divider />
                                        {resolutionTypeOtherMenuItems}
                                    </DropdownButton>
                                </div>
                            </div>
                            {/* <div style={{display: "inline-block"}} className="pull-right">
                                <Button bsSize="small" bsStyle={"primary"} style={{}} onClick={this.toggleBetweenTraceAndLogs}>
                                    {showTrace ? "VIEW LOGS" : "VIEW TRACE"}
                                </Button>
                            </div> */}
                            <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                                ref={this.inputElementRef}
                                type="text"
                                placeholder="Search"
                                id="filterPathInputId"
                                inputRef={ref => { this.input = ref; }}
                                value={this.state.searchFilterPath}
                                onChange={this.handleSearchFilterChange}
                            />
                        </FormGroup>
                        {/* <div style={{marginTop: "9px", display: showTrace ? "none": ""}}>
                            <Iframe url={selectedDiffItem.loggingURL}
                                width="100%"
                                height="720px"
                                id="myId"
                                className="myClassname"
                                display="initial"
                                position="relative"
                                frameBorder="1"
                                styles={{ border: "1px solid" }}
                            />
                        </div> */}
                        <div style={{marginTop: "9px", display: showLogs ? "none": ""}}>
                            {(this.state.showRequestMessageHeaders || this.state.shownRequestMessageHeaders) && (
                                <div style={{ display: this.state.showRequestMessageHeaders ? "" : "none" }}>
                                    <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Headers</Label></h4>
                                    <div className="headers-diff-wrapper" style={{border: "1px solid #ccc"}}>
                                        < ReactDiffViewer
                                            styles={newStyles}
                                            oldValue={JSON.stringify(selectedDiffItem.recordedRequestHeaders, undefined, 4)}
                                            newValue={JSON.stringify(selectedDiffItem.replayedRequestHeaders, undefined, 4)}
                                            splitView={true}
                                            disableWordDiff={false}
                                            diffArray={selectedDiffItem.reductedDiffArrayReqHeaders}
                                            onLineNumberClick={(lineId, e) => { return; }}
                                            filterPaths={filterPaths}
                                            inputElementRef={this.inputElementRef}
                                            showAll={this.state.showAll}
                                            searchFilterPath={this.state.searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(this.state.showRequestMessageQParams || this.state.shownRequestMessageQParams) && (
                                <div style={{ display: this.state.showRequestMessageQParams ? "" : "none" }}>
                                    <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Query Params</Label></h4>
                                    <div className="headers-diff-wrapper" style={{border: "1px solid #ccc"}}>
                                        < ReactDiffViewer
                                            styles={newStyles}
                                            oldValue={JSON.stringify(selectedDiffItem.recordedRequestQParams, undefined, 4)}
                                            newValue={JSON.stringify(selectedDiffItem.replayedRequestQParams, undefined, 4)}
                                            splitView={true}
                                            disableWordDiff={false}
                                            diffArray={selectedDiffItem.reductedDiffArrayReqQParams}
                                            onLineNumberClick={(lineId, e) => { return; }}
                                            filterPaths={filterPaths}
                                            inputElementRef={this.inputElementRef}
                                            showAll={this.state.showAll}
                                            searchFilterPath={this.state.searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(this.state.showRequestMessageFParams || this.state.shownRequestMessageFParams) && (
                                <div style={{ display: this.state.showRequestMessageFParams ? "" : "none" }}>
                                    <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Form Params</Label></h4>
                                    <div className="headers-diff-wrapper" style={{border: "1px solid #ccc"}}>
                                        < ReactDiffViewer
                                            styles={newStyles}
                                            oldValue={JSON.stringify(selectedDiffItem.recordedRequestFParams, undefined, 4)}
                                            newValue={JSON.stringify(selectedDiffItem.replayedRequestFParams, undefined, 4)}
                                            splitView={true}
                                            disableWordDiff={false}
                                            diffArray={selectedDiffItem.reductedDiffArrayReqFParams}
                                            onLineNumberClick={(lineId, e) => { return; }}
                                            filterPaths={filterPaths}
                                            inputElementRef={this.inputElementRef}
                                            showAll={this.state.showAll}
                                            searchFilterPath={this.state.searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(this.state.showRequestMessageBody || this.state.shownRequestMessageBody) && (
                                <div style={{ display: this.state.showRequestMessageBody ? "" : "none" }}>
                                    <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Body</Label></h4>
                                    <div className="headers-diff-wrapper" style={{border: "1px solid #ccc"}}>
                                        < ReactDiffViewer
                                            styles={newStyles}
                                            oldValue={JSON.stringify(selectedDiffItem.recordedRequestBody, undefined, 4)}
                                            newValue={JSON.stringify(selectedDiffItem.replayedRequestBody, undefined, 4)}
                                            splitView={true}
                                            disableWordDiff={false}
                                            diffArray={selectedDiffItem.reductedDiffArrayReqBody}
                                            onLineNumberClick={(lineId, e) => { return; }}
                                            filterPaths={filterPaths}
                                            inputElementRef={this.inputElementRef}
                                            showAll={this.state.showAll}
                                            searchFilterPath={this.state.searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(this.state.showResponseMessageHeaders || this.state.shownResponseMessageHeaders) && (
                                <div style={{ display: this.state.showResponseMessageHeaders ? "" : "none" }}>
                                    <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Headers</Label></h4>
                                    <div className="headers-diff-wrapper" style={{border: "1px solid #ccc"}}>
                                        < ReactDiffViewer
                                            styles={newStyles}
                                            oldValue={JSON.stringify(selectedDiffItem.recordedResponseHeaders, undefined, 4)}
                                            newValue={JSON.stringify(selectedDiffItem.replayedResponseHeaders, undefined, 4)}
                                            splitView={true}
                                            disableWordDiff={false}
                                            diffArray={selectedDiffItem.updatedReducedDiffArrayRespHdr}
                                            onLineNumberClick={(lineId, e) => { return; }}
                                            filterPaths={filterPaths}
                                            inputElementRef={this.inputElementRef}
                                            showAll={this.state.showAll}
                                            searchFilterPath={this.state.searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={this.state.enableClientSideDiff}
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
                                                {selectedDiffItem.recordResponse ? <span className="font-12">Status:&nbsp;<span className="green">{this.getHttpStatus(selectedDiffItem.recordResponse.status)}</span></span> : <span className="font-12" style={{"color": "magenta"}}>No Recorded Data</span>}
                                            </h4>
                                        </div>

                                        <div className="col-md-6">
                                            <h4 style={{marginLeft: "18%"}}>
                                            {selectedDiffItem.replayResponse ? <span className="font-12">Status:&nbsp;<span className="green">{this.getHttpStatus(selectedDiffItem.replayResponse.status)}</span></span> : <span className="font-12" style={{"color": "magenta"}}>No Replayed Data</span>}
                                            </h4>
                                        </div>
                                    </div>
                                    <div>
                                        {selectedDiffItem.missedRequiredFields.map((eachMissedField) => {
                                            return(<div><span style={{paddingRight: "5px"}}>{eachMissedField.path}:</span><span>{eachMissedField.fromValue}</span></div>)
                                        })}
                                    </div>
                                    {(selectedDiffItem.recordedData || selectedDiffItem.replayedData) && (
                                        <div className="diff-wrapper" style={{border: "1px solid #ccc"}}>
                                            < ReactDiffViewer
                                                styles={newStyles}
                                                oldValue={selectedDiffItem.expJSON}
                                                newValue={selectedDiffItem.actJSON}
                                                splitView={true}
                                                disableWordDiff={false}
                                                diffArray={selectedDiffItem.reductedDiffArray}
                                                filterPaths={filterPaths}
                                                onLineNumberClick={(lineId, e) => { return; }}
                                                inputElementRef={this.inputElementRef}
                                                showAll={this.state.showAll}
                                                searchFilterPath={this.state.searchFilterPath}
                                                disableOperationSet={true}
                                                handleCollapseLength={this.increaseCollapseLength}
                                                handleMaxLinesLength={this.increaseCollapseLength}
                                                enableClientSideDiff={this.state.enableClientSideDiff}
                                            />
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        );
    }
}

export default HttpClient;
