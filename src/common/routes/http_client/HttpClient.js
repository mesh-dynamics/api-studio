import  React , { Component, Fragment, createContext } from "react";
import { 
    Checkbox, FormGroup, FormControl, Glyphicon, 
    DropdownButton, MenuItem, Label, Table, Button, Modal
} from 'react-bootstrap';

import HttpRequestMessage from "./HttpRequestMessage.tsx";
import HttpResponseMessage from "./HttpResponseMessage.tsx";

import GRPCRequestMessage from "./GRPCRequestMessage.tsx";

import ErrorBoundary from '../../components/ErrorHandling/ErrorBoundary';
import ReactDiffViewer from '../../utils/diff/diff-main';
import config from "../../config";
import { getHttpStatus } from "../../status-code-list";
import { resolutionsIconMap } from '../../components/Resolutions.js';
import api from '../../api';
import { validateAndCreateDiffLayoutData , addCompressToggleData } from "../../utils/diff/diff-process.js";
import { AbortRequest } from "./abortRequest";
import SaveToCollection from './SaveToCollection.tsx';
import SplitSlider from "../../components/SplitSlider.tsx";
import EditableLabel from "./EditableLabel";
import { hasTabDataChanged } from "../../utils/http_client/utils";
import { isRequestTypeGrpc, getGrpcTabName } from "../../utils/http_client/grpc-utils";
import Tippy from "@tippy.js/react";
import RequestMatchType from './RequestMatchType.tsx';
import { HttpRequestFields } from "./HttpRequestFields";
import { httpClientConstants } from "../../constants/httpClientConstants";
import RunButton from "./components/RunButton";

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
            incrementStartJsonPath: null,
            diffLayoutData: null,
            showCompleteDiff: false,
            prevSelectedTraceTableReqTabId: this.props.currentSelectedTab.selectedTraceTableReqTabId,
            prevSelectedTraceTableTestReqTabId: this.props.currentSelectedTab.selectedTraceTableTestReqTabId,
            httpRequestRef: null,
            matchRequestShowPopup: false,
            showDiffErrorModal: false,
        };
        this.toggleMessageContents = this.toggleMessageContents.bind(this);
        this.handleSearchFilterChange = this.handleSearchFilterChange.bind(this);
        this.increaseCollapseLength = this.increaseCollapseLength.bind(this);

        this.handleClick = this.handleClick.bind(this);
        this.handleShowDiff = this.handleShowDiff.bind(this);
        this.handleShowCompleteDiffClick = this.handleShowCompleteDiffClick.bind(this);
        this.handleSetAsReference = this.handleSetAsReference.bind(this);
        this.handleAddMockRequestClick = this.handleAddMockRequestClick.bind(this);
    }

    static getDerivedStateFromProps(props, state) {   
        let newState = {};   
        if(props.currentSelectedTab.selectedTraceTableReqTabId != state.prevSelectedTraceTableReqTabId){
            newState = {
                prevSelectedTraceTableReqTabId: props.currentSelectedTab.selectedTraceTableReqTabId,
                showCompleteDiff: false
            }
        }
        if(props.currentSelectedTab.selectedTraceTableTestReqTabId != state.prevSelectedTraceTableTestReqTabId){
            newState = {
                ...newState,
                prevSelectedTraceTableTestReqTabId: props.currentSelectedTab.selectedTraceTableTestReqTabId,
                showCompleteDiff: false
            }
        }
        return newState;
    }


    preProcessResults = (results) => {
        //Improvement required: All values from state are undefined. None of value is being set in state in this page.
        const {app, replayId, recordingId, templateVersion} = this.state;
        let diffLayoutData = validateAndCreateDiffLayoutData(results, app, replayId, recordingId, templateVersion, config.diffCollapseLength, config.diffMaxLinesLength);
        this.updateResolutionFilterPaths(diffLayoutData);
        return diffLayoutData;
    }

    updateResolutionFilterPaths = (diffLayoutData) => {
        // const selectedResolutionType = this.state.filter.selectedResolutionType;
        const selectedResolutionType = "All";
        diffLayoutData && diffLayoutData.forEach(item => {
            item.filterPaths = [];
            for (let jsonPathParsedDiff of item.parsedDiff) {
                // add path to the filter list if the resolution is All or matches the current selected one,
                // and if the selected type is 'All Errors' it is an error type
                if (selectedResolutionType === "All"
                || selectedResolutionType === jsonPathParsedDiff.resolution
                || (selectedResolutionType === "ERR" && jsonPathParsedDiff.resolution.indexOf("ERR_") > -1)) {
                    // add only the json paths we want to show in the diff
                    let path = jsonPathParsedDiff.path;
                    item.filterPaths.push(path);
                }
            }
        });
    }

    /**
     * Maybe keep for sometime in case of bugs - may need to look back
     */
    // handleRequestTypeChange = (event, selectedTraceTableReqTabId) => {
    //     const { currentSelectedTab, updateRequestTypeOfTab } = this.props;
    //     const value = {};
    //     if(event.target.value === 'grpcData') {
    //         value.bodyType = 'grpcData';
    //         value.paramsType = 'showBody';
    //         value.payloadRequestEventName = 'GRPCRequestPayload';
    //         value.payloadResponseEventName = 'GRPCResponsePayload';
    //         value.tabName = getGrpcTabName(currentSelectedTab.grpcConnectionSchema)
    //     } else {
    //         value.bodyType = 'rawData';
    //         value.paramsType = 'showQueryParams';
    //         value.tabName = currentSelectedTab.httpURL;
    //         value.payloadRequestEventName = 'HTTPRequestPayload';
    //         value.payloadResponseEventName = 'HTTPResponsePayload';
    //     }
        

    //     if(currentSelectedTab.id === selectedTraceTableReqTabId) {
    //         updateRequestTypeOfTab(false, currentSelectedTab.id, selectedTraceTableReqTabId, value);
    //     } else {
    //         updateRequestTypeOfTab(true, currentSelectedTab.id, selectedTraceTableReqTabId, value);
    //     }
    // }

    handleEditServiceNameForEgress = (updatedServiceName, requestId) => {
        const { currentSelectedTab: { id: tabId, outgoingRequests }, updateParam, isOutgoingRequest } = this.props;

        const updatedOutgoingRequests = outgoingRequests.map(request => {
            // If request id matches, update the concerned values
            if(request.requestId === requestId) {
                // update service name outside
                request.service = updatedServiceName;
                // update in events
                request.eventData.forEach(event => event.service = updatedServiceName) 
                // return updated value
                return request;
            }
            // else return the request object as is
            return request;
        });

        updateParam(isOutgoingRequest, tabId, "outgoingRequests", "outgoingRequests", updatedOutgoingRequests);
    }

    handleEditServiceNameForGateway = (updatedServiceName) => {
        const { currentSelectedTab: { id: tabId, eventData }, updateParam, isOutgoingRequest } = this.props;
        const eventsWithUpdatedServiceName = eventData.map(event => event.service = updatedServiceName);

        // Update service name on top level
        updateParam(isOutgoingRequest, tabId, "service", "service", updatedServiceName);
        // Update service name in event objects
        updateParam(isOutgoingRequest, tabId, "eventData", "eventData", eventsWithUpdatedServiceName);
    }

    handleShowDiff() {
        const { currentSelectedTab } = this.props;
        const selectedTraceTableReqTabId = currentSelectedTab.selectedTraceTableReqTabId;
        const selectedTraceTableTestReqTabId = currentSelectedTab.selectedTraceTableTestReqTabId;
        let selectedTraceTableReqTab, selectedTraceTableTestReqTab;

        if(hasTabDataChanged(currentSelectedTab)) {
            this.setState({showDiffErrorModal: true});
            return
        }

        if(currentSelectedTab.selectedTraceTableReqTabId === currentSelectedTab.id) {
            selectedTraceTableReqTab = currentSelectedTab;
        } else {
            selectedTraceTableReqTab = currentSelectedTab.outgoingRequests ? currentSelectedTab.outgoingRequests.find((eachTab) => eachTab.id === selectedTraceTableReqTabId) : {};
        }
        if(selectedTraceTableTestReqTabId === currentSelectedTab.recordedHistory.id) {
            selectedTraceTableTestReqTab = currentSelectedTab.recordedHistory;
        } else {
            selectedTraceTableTestReqTab = currentSelectedTab.recordedHistory.outgoingRequests ? currentSelectedTab.recordedHistory.outgoingRequests.find((eachTab) => eachTab.id === selectedTraceTableTestReqTabId) : {};
        }

        const tabToProcess = selectedTraceTableReqTab;

        let diffLayoutData = [];
        if(tabToProcess && tabToProcess.eventData && tabToProcess.eventData[0].apiPath) {
            try {
                api.get(`${config.apiBaseUrl}/as/getReqRespMatchResult?lhsReqId=${tabToProcess.requestId}&rhsReqId=${selectedTraceTableTestReqTab.requestId}`)
                    .then((serverRes) => {
                        const results = serverRes.res && [serverRes.res];
                        diffLayoutData = this.preProcessResults(results);
                        this.setState({
                            diffLayoutData: diffLayoutData,
                            showCompleteDiff: true
                        })
                    }, (error) => {
                        console.error("error: ", error);
                    })
            } catch(error) {
                console.error("Error ", error);
                throw new Error("Error");
            }
        }
    }

    handleCloseDiffErrorModal = () => {
        this.setState({showDiffErrorModal: false});
    }

    handleShowCompleteDiffClick() {
        const { showCompleteDiff } = this.state;
        this.setState({
            showCompleteDiff: !showCompleteDiff
        })
    }

    handleRowClick(isOutgoingRequest, selectedTraceTableReqTabId) {
        const { currentSelectedTab } = this.props;
        this.props.handleRowClick(isOutgoingRequest, selectedTraceTableReqTabId, currentSelectedTab.id);
    }

    handleTestRowClick(selectedTraceTableTestReqTabId) {
        const { currentSelectedTab } = this.props;
        this.props.handleTestRowClick(selectedTraceTableTestReqTabId, currentSelectedTab.id);
    }

    handleClick(evt) {
        const { currentSelectedTab } = this.props;
        if(currentSelectedTab.requestRunning){
            this.props.initiateAbortRequest(currentSelectedTab.id, currentSelectedTab.currentRunId);
            // currentSelectedTab.abortRequest?.stopRequest();
        }else{
            this.props.updateAbortRequest(currentSelectedTab.id, new AbortRequest());
            this.props.driveRequest(false, currentSelectedTab.id);
            this.setState({
                showCompleteDiff: false
            })
        }
    }

    handleDuplicateTabClick = () => {
        const { currentSelectedTab } = this.props;
        this.props.handleDuplicateTab(currentSelectedTab.id);
    }

    handleSetAsReference(evt) {
        const { currentSelectedTab } = this.props;
        this.props.setAsReference(currentSelectedTab.id);
        this.setState({
            showCompleteDiff: false
        })
    }

    handleAddMockRequestClick(evt) {
        const { currentSelectedTab } = this.props;
        this.props.showAddMockReqModal(currentSelectedTab.id);
    }

    //Improvement required: There is bug in below collapse functionality. It is fixed in "DiffRequestIds.tsx" component
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

    handleDeleteReq = (evt, outgoingReqTabId) => {
        evt.stopPropagation();
        const { currentSelectedTab } = this.props;
        this.props.handleDeleteOutgoingReq(outgoingReqTabId, currentSelectedTab.id);
        
    }

    renderHasChangedTippy = (hasChanged) => {
        return <Tippy content={"Unsaved changes in this request"} arrow={true} placement="bottom">
            {hasChanged ? <i className="fas fa-circle" style={{fontSize: "12px", marginRight: "12px"}}></i> : <i></i>}
        </Tippy>
    }

    renderDeleteButton = (outgoingReqTabId) => {
        return (
            <Tippy content={"Click to delete"} arrow={true} placement="bottom">
                <i className="fas fa-trash pointer" style={{fontSize: "12px", marginRight: "12px"}} onClick={(evt) => this.handleDeleteReq(evt, outgoingReqTabId)}></i>
            </Tippy>
        );
    }

    render() {
        const {  currentSelectedTab, appGrpcSchema, selectedApp } = this.props;
        let selectedTraceTableReqTabId = currentSelectedTab.selectedTraceTableReqTabId;
        let selectedTraceTableTestReqTabId = currentSelectedTab.selectedTraceTableTestReqTabId;
        let selectedTraceTableReqTab, selectedTraceTableTestReqTab;
        
        // const selectedTraceTableTestReqTab = getTraceTableTestReqData(currentSelectedTab, currentSelectedTab.selectedTraceTableTestReqTabId);

        if(!selectedTraceTableReqTabId) {
            selectedTraceTableReqTabId = currentSelectedTab.id;
        }

        if(selectedTraceTableReqTabId === currentSelectedTab.id) {
            selectedTraceTableReqTab = currentSelectedTab;
        } else {
            selectedTraceTableReqTab = currentSelectedTab.outgoingRequests ? currentSelectedTab.outgoingRequests.find((eachTab) => eachTab.id === selectedTraceTableReqTabId) : null;
            if(!selectedTraceTableReqTab) selectedTraceTableReqTab = currentSelectedTab;
        }

        if(selectedTraceTableTestReqTabId && currentSelectedTab.recordedHistory && selectedTraceTableTestReqTabId === currentSelectedTab.recordedHistory.id) {
            selectedTraceTableTestReqTab = currentSelectedTab.recordedHistory;
        } else if(selectedTraceTableTestReqTabId && currentSelectedTab.recordedHistory) {
            selectedTraceTableTestReqTab = currentSelectedTab.recordedHistory.outgoingRequests ? currentSelectedTab.recordedHistory.outgoingRequests.find((eachTab) => eachTab.id === selectedTraceTableTestReqTabId) : {};
        } else if(currentSelectedTab.recordedHistory){
            selectedTraceTableTestReqTabId = currentSelectedTab.recordedHistory.id;
            selectedTraceTableTestReqTab = currentSelectedTab.recordedHistory;
        }

        const { outgoingRequests, service, httpURL, httpURLShowOnly, showTrace, hasChanged } = currentSelectedTab;

        const { selectedResolutionType, showLogs, collapseLength, incrementCollapseLengthForRecReqId, incrementCollapseLengthForRepReqId, maxLinesLength, showResponseMessageHeaders, showResponseMessageBody, showRequestMessageHeaders, showRequestMessageQParams, showRequestMessageFParams, showRequestMessageBody, showAll, searchFilterPath,  shownResponseMessageHeaders, shownResponseMessageBody, shownRequestMessageHeaders, shownRequestMessageQParams, shownRequestMessageFParams, shownRequestMessageBody, diffLayoutData, showCompleteDiff } = this.state;

        // if showTrace isn't set, show based on outgoing requests being non empty
        const showTraceV = showTrace == null ? ((outgoingRequests?.length) || (currentSelectedTab.recordedHistory?.outgoingRequests?.length)) : showTrace;

        const selectedDiffItem = diffLayoutData ? diffLayoutData[0] : null;

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
                addCompressToggleData(eachItem.reductedDiffArray, collapseLength, maxLinesLength);
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
        const isGrpc = currentSelectedTab.bodyType == "grpcData" && currentSelectedTab.paramsType == "showBody";
        const isDataReceivedAfterResponse = currentSelectedTab.progressState === httpClientConstants.AFTER_RESPONSE_RECEIVED_DATA;
        const responseBody = isGrpc && isDataReceivedAfterResponse ? selectedTraceTableReqTab.responseBody : "";
        return (<>
            <div>
                <div style={{display: "flex"}}>
                    <div style={{ display: "flex", justifyContent: "flex-start", flex: 1}}>
                        <RunButton handleClick={this.handleClick} requestRunning={currentSelectedTab.requestRunning}/>
                        <SaveToCollection 
                        disabled={currentSelectedTab.httpURL.length === 0} 
                        visible={currentSelectedTab.showSaveBtn} 
                        tabId={currentSelectedTab.id}
                        />
                    </div>
                    <div style={{ display: "flex", justifyContent: "flex-end", flex: 1}}>
                        <div>
                            <div className="btn btn-sm cube-btn text-center" style={{ display: showCompleteDiff ? "none" : currentSelectedTab.recordedHistory ? "inline-block" : "none"}} onClick={this.handleShowDiff}>
                                <Glyphicon glyph="random" /> DIFF
                            </div>
                            <div className="btn btn-sm cube-btn text-center" style={{ display: showCompleteDiff ? "inline-block" : "none"}} onClick={this.handleShowCompleteDiffClick}>
                                <Glyphicon glyph="sort-by-attributes" /> FULL VIEW
                            </div>
                            <div className="btn btn-sm cube-btn text-center" style={{ display: showCompleteDiff ? "inline-block" : currentSelectedTab.recordedHistory ? "inline-block" : "none"}} onClick={this.handleSetAsReference}>
                                <Glyphicon glyph="export" /> SET AS REFERENCE
                            </div>
                        </div>
                        <Button className="cube-btn text-center"  onClick={this.handleDuplicateTabClick} title="Duplicate Tab">
                            <i className="fa fa-clone"></i>
                        </Button>
                    </div>
                </div>
                <div>
                <div style={{marginRight: "7px"}}>
                    <div className="pointer" style={{display: "inline-block", width: "20%", fontSize: "11px"}} onClick={() => this.props.toggleShowTrace(currentSelectedTab.id)}>
                        TRACE
                        <i className={showTraceV ? "fas fa-chevron-circle-up" : "fas fa-chevron-circle-down"} style={{marginLeft: "2px"}}></i>
                    </div>
                </div>
                    <div style={{display: showTraceV ? "flex" : "none", backgroundColor: "#ffffff", marginBottom: "9px"}}>
                        <div style={{flex: "1", padding: "0.5rem", minWidth: "0px", overflow: "hidden"}}>
                            <div>Reference</div>
                            <Table hover style={{backgroundColor: "#fff", border: "1px solid #ddd", borderSpacing: "0px", borderCollapse: "separate", marginBottom: "0px"}}>
                                <thead>
                                    <tr>
                                        <th style={{ minWidth: "180px" }}>SERVICE BY TRACE ORDER</th>
                                        <th>API PATH</th>
                                        <th></th>
                                        <th></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr style={{cursor: "pointer", backgroundColor: selectedTraceTableReqTab.id === currentSelectedTab.id ? "#ccc" : "#fff"}} onClick={() => this.handleRowClick(false, currentSelectedTab.id)}>
                                        <td style={{ display: "inline-flex", width: "100%" }}>
                                            <span><i className="fas fa-arrow-right" style={{fontSize: "14px", marginRight: "12px"}}></i></span>
                                            <span style={{marginRight: "30px", width: "25px"}}>
                                                {
                                                    isRequestTypeGrpc(selectedTraceTableReqTab.id, currentSelectedTab, outgoingRequests) 
                                                    ? <span style={{ fontWeight: "700",fontSize: "11px" }}>gRPC</span> 
                                                    : <span style={{ fontWeight: "700",fontSize: "11px" }}>REST</span>
                                                }
                                            </span>
                                            <EditableLabel label={service} handleEditComplete={this.handleEditServiceNameForGateway} />
                                        </td>
                                        <td>{httpURLShowOnly}</td>
                                        <td>
                                            <span>
                                                {this.renderHasChangedTippy(hasChanged)}
                                            </span>
                                        </td>
                                        <td></td>
                                    </tr>
                                    {outgoingRequests && outgoingRequests.length > 0 && outgoingRequests.map((eachReq) => {
                                        return (
                                            <tr className="service-rows" key={eachReq.id} style={{cursor: "pointer", backgroundColor: selectedTraceTableReqTab.id === eachReq.id ? "#ccc" : "#fff"}} onClick={() => this.handleRowClick(true, eachReq.id)}>
                                                <td style={{ display: "inline-flex", width: "100%" }}>
                                                    <span>
                                                        <i className="fas fa-level-up-alt fa-rotate-90" style={{fontSize: "14px", marginRight: "12px"}}></i>
                                                    </span>
                                                    <span style={{marginRight: "30px", width: "25px", marginLeft: "3.5px"}}>
                                                        {
                                                            isRequestTypeGrpc(eachReq.id, currentSelectedTab, outgoingRequests) 
                                                            ? <span style={{ fontWeight: "700",fontSize: "11px" }}>gRPC</span> 
                                                            : <span style={{ fontWeight: "700",fontSize: "11px" }}>REST</span>
                                                        }
                                                    </span>
                                                    <EditableLabel 
                                                        label={eachReq.service} 
                                                        handleEditComplete={(updatedServiceName) => this.handleEditServiceNameForEgress(updatedServiceName, eachReq.requestId)} 
                                                    />
                                                </td>
                                                <td>{eachReq.httpURLShowOnly}</td>
                                                <td>
                                                    <span>
                                                        {this.renderHasChangedTippy(eachReq.hasChanged)}
                                                    </span>
                                                </td>
                                                <td>
                                                    <span>
                                                        {this.renderDeleteButton(eachReq.id)}
                                                    </span>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </Table>
                            <div style={{ marginTop: "5px", marginRight: "7px"}}>
                                <div style={{display: "inline-block", width: "100%"}}> 
                                    <button className="add-request-options-button" onClick={this.handleAddMockRequestClick}>
                                        <span style={{ fontSize: "20px" }}>+</span>
                                        <span style={{ marginLeft: "5px", fontWeight: 400 }}>Add Mock Request</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div style={{flex: "1", padding: "0.5rem", paddingLeft: "0", minWidth: "0px"}}>
                            {currentSelectedTab.recordedHistory && (
                                <div>
                                    <div>Test</div>
                                    <Table hover style={{backgroundColor: "#fff", border: "1px solid #ddd", borderSpacing: "0px", borderCollapse: "separate", marginBottom: "0px"}}>
                                        <thead>
                                            <tr>
                                                <th>SERVICE BY TRACE ORDER</th>
                                                <th>API PATH</th>
                                                <th></th>
                                                <th></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr 
                                                style={{
                                                    cursor: "pointer", 
                                                    backgroundColor: 
                                                    selectedTraceTableTestReqTab.id === currentSelectedTab.recordedHistory.id ? "#ccc" : "#fff",
                                                }} 
                                                onClick={() => this.handleTestRowClick(currentSelectedTab.recordedHistory.id)}
                                            >
                                                
                                                    <td>
                                                        <span><i className="fas fa-arrow-right" style={{fontSize: "14px", marginRight: "12px"}}></i></span>
                                                        <span style={{marginRight: "30px", width: "25px"}}>
                                                        {
                                                            isRequestTypeGrpc(selectedTraceTableTestReqTab.id, currentSelectedTab.recordedHistory, currentSelectedTab.recordedHistory.outgoingRequests) 
                                                            ? <span style={{ fontWeight: "700",fontSize: "11px" }}>gRPC</span> 
                                                            : <span style={{ fontWeight: "700",fontSize: "11px" }}>REST</span>
                                                        }
                                                    </span>
                                                        {currentSelectedTab.recordedHistory.service}
                                                    </td>
                                                    <td>{currentSelectedTab.recordedHistory.apiPath}</td>
                                                    <td></td>
                                                    <td></td>
                                            </tr>
                                            {currentSelectedTab.recordedHistory.outgoingRequests && currentSelectedTab.recordedHistory.outgoingRequests.length > 0 && currentSelectedTab.recordedHistory.outgoingRequests.map((eachReq) => {
                                                return (
                                                    <tr 
                                                        key={eachReq.requestId} 
                                                        style={{
                                                            cursor: "pointer", 
                                                            backgroundColor: selectedTraceTableTestReqTab.id === eachReq.id ? "#ccc" : "#fff",
                                                            }} 
                                                        onClick={() => this.handleTestRowClick(eachReq.id)} 
                                                    >
                                                        <td>
                                                            <span>
                                                                <i className="fas fa-level-up-alt fa-rotate-90" style={{fontSize: "14px", marginRight: "12px"}}></i>
                                                            </span>
                                                            <span style={{marginRight: "30px", width: "25px", marginLeft: "3.5px"}}>
                                                                {
                                                                    isRequestTypeGrpc(eachReq.id, currentSelectedTab.recordedHistory, currentSelectedTab.recordedHistory.outgoingRequests) 
                                                                    ? <span style={{ fontWeight: "700",fontSize: "11px" }}>gRPC</span> 
                                                                    : <span style={{ fontWeight: "700",fontSize: "11px" }}>REST</span>
                                                                }
                                                            </span>
                                                            {eachReq.service}
                                                        </td>
                                                        <td>{eachReq.apiPath}</td>
                                                        <td></td>
                                                        <td>
                                                            {eachReq.metaData && eachReq.metaData.matchType &&
                                                                <RequestMatchType 
                                                                metaData={eachReq.metaData}
                                                                originalReqId={eachReq.requestId}/>
                                                            }
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </Table>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
                {/* <div style={{ display: "flex", width: "10%" }}>
                    <select 
                        value={isRequestTypeGrpc(selectedTraceTableReqTabId, currentSelectedTab, outgoingRequests) ? "grpcData" : "rawData"}
                        className="form-control md-request-type-dropdown" 
                        onChange={(event) => this.handleRequestTypeChange(event, selectedTraceTableReqTabId)}
                    >
                        <option value="grpcData">gRPC</option>
                        <option value="rawData">REST</option>
                    </select>
                </div> */}
                {!showCompleteDiff && (
                    <div>
                    {
                        !isRequestTypeGrpc(selectedTraceTableReqTabId, currentSelectedTab, outgoingRequests)
                        // selectedTraceTableReqTabId.bodyType !== "grpcData"  // this.state.requestType === "REST" 
                        ? 
                            (
                                <Fragment>
                                    <div style={{display: "flex"}}>
                                        <div style={{flex: "1", padding: "0.5rem", height:'100%'}}>
                                            <HttpRequestMessage 
                                                tabId={selectedTraceTableReqTab.id}
                                                httpMethod={selectedTraceTableReqTab.httpMethod}
                                                httpURL={selectedTraceTableReqTab.httpURL}
                                                headers={selectedTraceTableReqTab.headers} 
                                                queryStringParams={selectedTraceTableReqTab.queryStringParams}
                                                bodyType={selectedTraceTableReqTab.bodyType}
                                                formData={selectedTraceTableReqTab.formData} 
                                                multipartData={selectedTraceTableReqTab.multipartData} 
                                                rawData={selectedTraceTableReqTab.rawData}
                                                grpcData={selectedTraceTableReqTab.grpcData}
                                                rawDataType={selectedTraceTableReqTab.rawDataType}
                                                paramsType={selectedTraceTableReqTab.paramsType}
                                                requestPathURL={selectedTraceTableReqTab.requestPathURL || ""}
                                                service={selectedTraceTableReqTab.service}
                                                updateParam={this.props.updateParam}
                                                replaceAllParams={this.props.replaceAllParams}
                                                updateBodyOrRawDataType={this.props.updateBodyOrRawDataType}
                                                isOutgoingRequest={selectedTraceTableReqTab.isOutgoingRequest} 
                                                id="" 
                                                readOnly={false}
                                                clientTabId={currentSelectedTab.id}
                                            />
                                        </div>
                                        <div style={{flex: "1", padding: "0.5rem", paddingLeft: "0", height:'100%'}}>
                                            {selectedTraceTableReqTab && selectedTraceTableTestReqTab && (
                                                <HttpRequestMessage
                                                    tabId={selectedTraceTableTestReqTab.id}
                                                    httpMethod={selectedTraceTableTestReqTab.httpMethod}
                                                    httpURL={selectedTraceTableTestReqTab.httpURL}
                                                    headers={selectedTraceTableTestReqTab.headers} 
                                                    queryStringParams={selectedTraceTableTestReqTab.queryStringParams}
                                                    bodyType={selectedTraceTableTestReqTab.bodyType}
                                                    formData={selectedTraceTableTestReqTab.formData} 
                                                    multipartData={selectedTraceTableTestReqTab.multipartData} 
                                                    rawData={selectedTraceTableTestReqTab.rawData}
                                                    grpcData={selectedTraceTableTestReqTab.grpcData}
                                                    rawDataType={selectedTraceTableTestReqTab.rawDataType}
                                                    paramsType={selectedTraceTableReqTab.paramsType}
                                                    requestPathURL={selectedTraceTableReqTab.requestPathURL || ""}
                                                    service={selectedTraceTableTestReqTab.service}
                                                    updateParam={this.props.updateParam}
                                                    replaceAllParams={this.props.replaceAllParams}
                                                    updateBodyOrRawDataType={this.props.updateBodyOrRawDataType}
                                                    isOutgoingRequest={selectedTraceTableTestReqTab.isOutgoingRequest}
                                                    readOnly={true}
                                                    id="test"
                                                    disabled={true}
                                                    clientTabId={currentSelectedTab.id}
                                                />
                                            )}
                                        </div>
                                    </div>
                                    <div style={{display: "flex",  minHeight: (selectedTraceTableReqTab.paramsType == "body" ?'200px': '50px'), overflowY: "auto"}} 
                                        ref={e=> (!this.state.httpRequestRef && this.setState({httpRequestRef : e}))}
                                    >
                                        <div style={{flex: "1", padding: "0.5rem", height:'100%', minWidth: "0px"}}>
                                            <HttpRequestFields 
                                                // Remove not required props
                                                tabId={selectedTraceTableReqTab.id}
                                                requestId={selectedTraceTableReqTab.requestId}
                                                
                                                headers={selectedTraceTableReqTab.headers} 
                                                queryStringParams={selectedTraceTableReqTab.queryStringParams}
                                                bodyType={selectedTraceTableReqTab.bodyType}
                                                formData={selectedTraceTableReqTab.formData} 
                                                multipartData={selectedTraceTableReqTab.multipartData || []}
                                                rawData={selectedTraceTableReqTab.rawData}
                                                grpcData={selectedTraceTableReqTab.grpcData}
                                                rawDataType={selectedTraceTableReqTab.rawDataType}
                                                paramsType={selectedTraceTableReqTab.paramsType}
                                                addOrRemoveParam={this.props.addOrRemoveParam} 
                                                updateParam={this.props.updateParam}
                                                updateAllParams={this.props.updateAllParams}
                                                updateBodyOrRawDataType={this.props.updateBodyOrRawDataType}
                                                isOutgoingRequest={selectedTraceTableReqTab.isOutgoingRequest} 
                                                id="" 
                                                readOnly={false}
                                                clientTabId={currentSelectedTab.id}
                                            />
                                        </div>
                                        <div style={{flex: "1", padding: "0.5rem", height:'100%', minWidth: "0px"}}>
                                            {selectedTraceTableReqTab && selectedTraceTableTestReqTab && (
                                                <HttpRequestFields 
                                                    // Remove not required props
                                                        tabId={selectedTraceTableTestReqTab.id}
                                                        requestId={selectedTraceTableTestReqTab.requestId}
                                                        httpMethod={selectedTraceTableTestReqTab.httpMethod}
                                                        httpURL={selectedTraceTableTestReqTab.httpURL}
                                                        headers={selectedTraceTableTestReqTab.headers} 
                                                        queryStringParams={selectedTraceTableTestReqTab.queryStringParams}
                                                        bodyType={selectedTraceTableTestReqTab.bodyType}
                                                        formData={selectedTraceTableTestReqTab.formData} 
                                                        multipartData={selectedTraceTableTestReqTab.multipartData || []}
                                                        rawData={selectedTraceTableTestReqTab.rawData}
                                                        grpcData={selectedTraceTableTestReqTab.grpcData}
                                                        rawDataType={selectedTraceTableTestReqTab.rawDataType}
                                                        paramsType={selectedTraceTableReqTab.paramsType}
                                                        addOrRemoveParam={this.props.addOrRemoveParam} 
                                                        updateParam={this.props.updateParam}
                                                        updateAllParams={this.props.updateAllParams}
                                                        updateBodyOrRawDataType={this.props.updateBodyOrRawDataType}
                                                        isOutgoingRequest={selectedTraceTableTestReqTab.isOutgoingRequest} 
                                                        id="test" 
                                                        setBodyRef={this.setRequestBodyRef}
                                                        readOnly={true}
                                                        clientTabId={currentSelectedTab.id}
                                                    />
                                                )
                                            }
                                        </div>
                                    </div>
                                </Fragment>
                            )
                        :
                            (
                                <div style={{display: "flex"}}>
                                    <div style={{flex: "1", padding: "0.5rem", height:'100%'}}>
                                        <GRPCRequestMessage
                                            readOnly={false}
                                            disabled={false}
                                            selectedApp={selectedApp}
                                            appGrpcSchema={appGrpcSchema}
                                            tabId={selectedTraceTableReqTab.id}
                                            headers={selectedTraceTableReqTab.headers} 
                                            httpURL={selectedTraceTableReqTab.httpURL}
                                            currentSelectedTabId={currentSelectedTab.id}
                                            grpcData={selectedTraceTableReqTab.grpcData}
                                            paramsType={selectedTraceTableReqTab.paramsType}
                                            grpcConnectionSchema={selectedTraceTableReqTab.grpcConnectionSchema}
                                            updateParam={this.props.updateParam}
                                            updateAllParams={this.props.updateAllParams}
                                            addOrRemoveParam={this.props.addOrRemoveParam} 
                                            updateGrpcConnectData={this.props.updateGrpcConnectData}
                                            isOutgoingRequest={selectedTraceTableReqTab.isOutgoingRequest} 
                                            clientTabId={currentSelectedTab.id}
                                        />
                                    </div>
                                    <div style={{flex: "1", padding: "0.5rem", paddingLeft: "0", height:'100%'}}>
                                        {selectedTraceTableReqTab && selectedTraceTableTestReqTab && (
                                            <GRPCRequestMessage
                                                readOnly={true}
                                                disabled={true}
                                                selectedApp={selectedApp}
                                                appGrpcSchema={appGrpcSchema}
                                                tabId={selectedTraceTableTestReqTab.id}
                                                currentSelectedTabId={currentSelectedTab.id}
                                                httpURL={selectedTraceTableTestReqTab.httpURL}
                                                headers={selectedTraceTableTestReqTab.headers} 
                                                grpcData={selectedTraceTableTestReqTab.grpcData}
                                                paramsType={selectedTraceTableReqTab.paramsType}
                                                grpcConnectionSchema={selectedTraceTableTestReqTab.grpcConnectionSchema}
                                                updateParam={this.props.updateParam}
                                                updateAllParams={this.props.updateAllParams}
                                                addOrRemoveParam={this.props.addOrRemoveParam} 
                                                updateGrpcConnectData={this.props.updateGrpcConnectData}
                                                isOutgoingRequest={selectedTraceTableTestReqTab.isOutgoingRequest}
                                                clientTabId={currentSelectedTab.id}
                                            />
                                        )}
                                    </div>
                                </div>
                            )        
                    }
                        
                        <SplitSlider 
                            slidingElement={this.state.httpRequestRef} 
                            horizontal 
                            persistKey={`HorizontalSplitter_${selectedTraceTableReqTabId}`}
                            minSpace={(selectedTraceTableReqTab.paramsType == "body" ? 200: 50)}/> 
                        <HttpResponseMessage 
                            tabId={selectedTraceTableReqTab.id}
                            clientTabId = {currentSelectedTab.id}
                            /** Belongs to RHS */
                            responseStatus={selectedTraceTableTestReqTab ? selectedTraceTableTestReqTab.recordedResponseStatus : selectedTraceTableReqTab.responseStatus}
                            responseStatusText={""}
                            responseHeaders={selectedTraceTableTestReqTab ? selectedTraceTableTestReqTab.recordedResponseHeaders : selectedTraceTableReqTab.responseHeaders}
                            responseBody={selectedTraceTableTestReqTab ? selectedTraceTableTestReqTab.recordedResponseBody : responseBody}
                            isGrpcRHS={selectedTraceTableTestReqTab?.bodyType==="grpcData"}
                            /** Belongs to LHS */
                            recordedResponseHeaders={selectedTraceTableReqTab.recordedResponseHeaders}
                            recordedResponseBody={ selectedTraceTableReqTab.recordedResponseBody}
                            recordedResponseStatus={selectedTraceTableReqTab.recordedResponseStatus}
                            updateParam={this.props.updateParam}
                            isOutgoingRequest={ selectedTraceTableReqTab.isOutgoingRequest}
                            requestRunning={currentSelectedTab.requestRunning}
                            isGrpcLHS={selectedTraceTableReqTab?.bodyType==="grpcData"}
                            >
                        </HttpResponseMessage>
                    </div>
                
                )}
                
                {showCompleteDiff && selectedDiffItem && (
                    <div style={{marginTop: "27px", backgroundColor: "#fff", padding: "9px"}}>
                        <div style={{opacity: 0.6, marginTop: "9px"}}>
                            <h4><Glyphicon style={{ visibility:  "visible", paddingRight: "5px", fontSize: "14px" }} glyph="random" /> <span>Selected Diff</span></h4>
                        </div>
                        <FormGroup>
                            <Checkbox inline onChange={this.toggleMessageContents} value="requestHeaders" checked={showRequestMessageHeaders}>Request Headers</Checkbox>
                            <Checkbox inline onChange={this.toggleMessageContents} value="requestQParams" checked={showRequestMessageQParams}>Request Query Params</Checkbox>
                            <Checkbox inline onChange={this.toggleMessageContents} value="requestFParams" checked={showRequestMessageFParams}>Request Form Params</Checkbox>
                            <Checkbox inline onChange={this.toggleMessageContents} value="requestBody" checked={showRequestMessageBody}>Request Body</Checkbox>
                            <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                            <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={showResponseMessageHeaders}>Response Headers</Checkbox>
                            <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={showResponseMessageBody} >Response Body</Checkbox>
                            <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px"}}></span>
                            {/** Removed below code as it is not working */}
                            {/* <div style={{display: "inline-block"}}>
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
                            </div> */}
                            <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                                ref={this.inputElementRef}
                                type="text"
                                placeholder="Search"
                                id="filterPathInputId"
                                value={searchFilterPath}
                                onChange={this.handleSearchFilterChange}
                            />
                        </FormGroup>
                        <div style={{marginTop: "9px", display: showLogs ? "none": ""}}>
                            {(showRequestMessageHeaders || shownRequestMessageHeaders) && (
                                <div style={{ display: showRequestMessageHeaders ? "" : "none" }}>
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
                                            showAll={showAll}
                                            searchFilterPath={searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(showRequestMessageQParams || shownRequestMessageQParams) && (
                                <div style={{ display: showRequestMessageQParams ? "" : "none" }}>
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
                                            showAll={showAll}
                                            searchFilterPath={searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(showRequestMessageFParams || shownRequestMessageFParams) && (
                                <div style={{ display: showRequestMessageFParams ? "" : "none" }}>
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
                                            showAll={showAll}
                                            searchFilterPath={searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(showRequestMessageBody || shownRequestMessageBody) && (
                                <div style={{ display: showRequestMessageBody ? "" : "none" }}>
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
                                            showAll={showAll}
                                            searchFilterPath={searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
                                        />
                                    </div>
                                </div>
                            )}
                            {(showResponseMessageHeaders || shownResponseMessageHeaders) && (
                                <div style={{ display: showResponseMessageHeaders ? "" : "none" }}>
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
                                            showAll={showAll}
                                            searchFilterPath={searchFilterPath}
                                            disableOperationSet={true}
                                            enableClientSideDiff={true}
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
                                                {selectedDiffItem.recordResponse ? <span className="font-12">Status:&nbsp;<span className="green">{getHttpStatus(selectedDiffItem.recordResponse.status)}</span></span> : <span className="font-12" style={{"color": "magenta"}}>No Recorded Data</span>}
                                            </h4>
                                        </div>

                                        <div className="col-md-6">
                                            <h4 style={{marginLeft: "18%"}}>
                                            {selectedDiffItem.replayResponse ? <span className="font-12">Status:&nbsp;<span className="green">{getHttpStatus(selectedDiffItem.replayResponse.status)}</span></span> : <span className="font-12" style={{"color": "magenta"}}>No Replayed Data</span>}
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
                                                showAll={showAll}
                                                searchFilterPath={searchFilterPath}
                                                disableOperationSet={true}
                                                handleCollapseLength={this.increaseCollapseLength}
                                                handleMaxLinesLength={this.increaseCollapseLength}
                                                enableClientSideDiff={true}
                                            />
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
            <Modal show={this.state.showDiffErrorModal} onHide={this.handleCloseDiffErrorModal}>
                <Modal.Header closeButton>
                    <Modal.Title>Error</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>{"Please save the modified request before proceeding with the diff."}</p>
                </Modal.Body>
                <Modal.Footer>
                    <div onClick={this.handleCloseDiffErrorModal} className="btn btn-sm cube-btn text-center">Close</div>
                </Modal.Footer>
            </Modal>
        </>);
    }
}



function errorBoundedHttpClient(props) {
    const fallBackMessage = (
      <div>
        <h3>An error occurred</h3>
        <p>
          Please close this tab to resolve the issue. If the error
          persists, please contact us.
        </p>
      </div>
    );
    return (
      <ErrorBoundary fallbackUI={fallBackMessage}>
        <HttpClient {...props} />
      </ErrorBoundary>
    );
  }
  


export default errorBoundedHttpClient;
