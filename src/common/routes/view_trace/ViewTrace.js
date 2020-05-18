import React, { Component } from 'react';
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Table, ButtonGroup, Button, Radio, Tabs, Tab} from 'react-bootstrap';
import _ from 'lodash';
import arrayToTree  from 'array-to-tree';
import axios from "axios";
import Iframe from 'react-iframe'
import * as moment from 'moment';
// import sortJson from "sort-json";
import sortJson from "../../utils/sort-json";
import ReactDiffViewer from '../../utils/diff/diff-main';
import ReduceDiff from '../../utils/ReduceDiff';
import config from "../../config";
import generator from '../../utils/generator/json-path-generator';

import {connect} from "react-redux";
import {cubeActions} from "../../actions";
import {Link} from "react-router-dom";
import {getSearchHistoryParams, updateSearchHistoryParams} from "../../utils/lib/url-utils";
import statusCodeList from "../../StatusCodeList";
import {resolutionsIconMap} from '../../components/Resolutions.js';
import { cubeService } from '../../services';

const cleanEscapedString = (str) => {
    // preserve newlines, etc - use valid JSON
    str = str.replace(/\\n/g, "\\n")
        .replace(/\\'/g, "\\'")
        .replace(/\\"/g, '\\"')
        .replace(/\\&/g, "\\&")
        .replace(/\\r/g, "\\r")
        .replace(/\\t/g, "\\t")
        .replace(/\\b/g, "\\b")
        .replace(/\\f/g, "\\f");
    // remove non-printable and other non-valid JSON chars
    str = str.replace(/[\u0000-\u0019]+/g, "");
    return str;
}

const removeURLParameter = (url, parameter) => {
    //prefer to use l.search if you have a location/link object
    var urlparts = url.split('?');   
    if (urlparts.length >= 2) {

        var prefix = encodeURIComponent(parameter) + '=';
        var pars = urlparts[1].split(/[&;]/g);

        //reverse iteration as may be destructive
        for (var i = pars.length; i-- > 0;) {    
            //idiom for string.startsWith
            if (pars[i].lastIndexOf(prefix, 0) !== -1) {  
                pars.splice(i, 1);
            }
        }

        return urlparts[0] + (pars.length > 0 ? '?' + pars.join('&') : '');
    }
    return url;
}

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

class ViewTrace extends Component {
    constructor(props) {
        super(props);
        this.state = {
            recProcessedTraceDataTree: [],
            repProcessedTraceDataTree: [],
            recProcessedTraceDataFlattenTree: [],
            repProcessedTraceDataFlattenTree: [],
            selectedDiffItem: null,
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
            app: "",
            templateVersion: "",
            newTemplateVerInfo: null,
            apiPath: "",
            service: "",
            replayId: null,
            recordingId: null,
            fetchComplete: false,
            fetchedResults: 0,
            selectedReqRespMatchType: "responseMismatch",
            showAll: true,
            timeStamp: "",
            traceId: null,
            showTrace: true,
            showLogs: false,
            collapseLength: parseInt(config.diffCollapseLength),
            collapseLengthIncrement: parseInt(config.diffCollapseLengthIncrement),
            maxLinesLength: parseInt(config.diffMaxLinesLength),
            maxLinesLengthIncrement: parseInt(config.diffMaxLinesLengthIncrement),
            incrementCollapseLengthForRecReqId: null,
            incrementCollapseLengthForRepReqId: null,
            incrementStartJsonPath: null,
            testMockServices: [],
            enableClientSideDiff: config.enableClientSideDiff === "true" ? true : false
        }

        this.inputElementRef = React.createRef();
        this.layoutDataWithDiff = [];
        this.uniqueRecordReplayData = [];
        this.loggingURL = "";

        this.handleSearchFilterChange = this.handleSearchFilterChange.bind(this);
        this.toggleMessageContents = this.toggleMessageContents.bind(this);
        this.toggleBetweenTraceAndLogs = this.toggleBetweenTraceAndLogs.bind(this);
        this.increaseCollapseLength = this.increaseCollapseLength.bind(this);
    }

    componentDidMount() {
        const {dispatch} = this.props;
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();
        
        const app = urlParameters["app"];
        const apiPath = urlParameters["apiPath"] ? urlParameters["apiPath"]  : "%2A";
        const replayId = urlParameters["replayId"];
        const recordingId = urlParameters["recordingId"];
        const currentTemplateVer = urlParameters["currentTemplateVer"];
        const service = urlParameters["service"];
        const selectedReqRespMatchType = urlParameters["selectedReqRespMatchType"];
        const selectedResolutionType = urlParameters["selectedResolutionType"];
        const searchFilterPath = urlParameters["searchFilterPath"];
        const requestHeaders = urlParameters["requestHeaders"];
        const requestQParams = urlParameters["requestQParams"];
        const requestFParams = urlParameters["requestFParams"];
        const requestBody = urlParameters["requestBody"];
        const responseHeaders = urlParameters["responseHeaders"];
        const responseBody = urlParameters["responseBody"];
        const timeStamp = decodeURI(urlParameters["timeStamp"]);
        const traceId = urlParameters["traceId"];

        dispatch(cubeActions.setSelectedApp(app));
        this.checkStatusForReplay(replayId);
        this.setState({
            apiPath: apiPath,
            replayId: replayId,
            traceId: traceId,
            service: service,
            recordingId: recordingId,
            currentTemplateVer: currentTemplateVer,
            app: app,
            selectedAPI: apiPath || "All",
            selectedService: service || "All",
            selectedReqRespMatchType: selectedReqRespMatchType || "responseMismatch",
            selectedResolutionType: selectedResolutionType || "All",
            searchFilterPath: searchFilterPath || "",
            timeStamp: timeStamp || "",
            showAll: true,
            // response headers
            showResponseMessageHeaders: responseHeaders ? JSON.parse(responseHeaders) : false,
            shownResponseMessageHeaders: responseHeaders ?  JSON.parse(responseHeaders) : false,
            // response body
            showResponseMessageBody: responseBody ? JSON.parse(responseBody) : true,
            shownResponseMessageBody: responseBody ? JSON.parse(responseBody) : true,
            // request header
            showRequestMessageHeaders: requestHeaders ? JSON.parse(requestHeaders) : false,
            shownRequestMessageHeaders: requestHeaders ? JSON.parse(requestHeaders) : false,
            // request query params
            showRequestMessageQParams: requestQParams ? JSON.parse(requestQParams) : false,
            shownRequestMessageQParams: requestQParams ? JSON.parse(requestQParams) : false,
            // request form params
            showRequestMessageFParams: requestFParams ? JSON.parse(requestFParams) : false,
            shownRequestMessageFParams: requestFParams ? JSON.parse(requestFParams) : false,
            // request body
            showRequestMessageBody: requestBody ? JSON.parse(requestBody) : false,
            shownRequestMessageBody: requestBody ? JSON.parse(requestBody) : false,
        });
        setTimeout(() => {
            const { dispatch, history, cube } = this.props;
            dispatch(cubeActions.setPathResultsParams({
                path: apiPath,
                service: service,
                replayId: replayId,
                recordingId: recordingId,
                currentTemplateVer: currentTemplateVer,
                timeStamp: timeStamp
            }));
            dispatch(cubeActions.getJiraBugs(replayId, apiPath));
            this.fetchReplayList();
        });
    }

    async checkStatusForReplay(replayId) {
        try {
            let status = await cubeService.checkStatusForReplay(replayId);
            this.setState({
                testMockServices: status.mockServices
            })
        } catch(error) {
            console.error("Error While fething status of Replay:" + error)
        }

    }

    handleSearchFilterChange(e) {

        this.setState({ searchFilterPath: e.target.value });
    }

    handleMetaDataSelect(metaDataType, value) {
        const { history, dispatch } = this.props;
        this.setState({
            selectedResolutionType : value, 
            showAll : (value ===  "All")
        });
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

    toggleBetweenTraceAndLogs(e) {
        e.preventDefault();
        let { showTrace, showLogs } = this.state;
        this.setState({
            showTrace: !showTrace,
            showLogs: !showLogs
        })
    }

    flattenTree(traceDataTree) {
        let depth = 0, result = [], queue = [];
        const {testMockServices} = this.state;

        for(let eachRootNode of traceDataTree) {
            queue.push({
                depth: 0,
                show: true,
                showChildren: true,
                ...eachRootNode
            });
        }
        while (queue.length > 0) {
            let current = queue.shift();
            result.push({
                ...current
            })
            let isParentmocked = testMockServices ? testMockServices.some(function(element, i) {
                if (current.service.toLowerCase() === element.toLowerCase()) {
                    return true;
                }
            }) : false;
            if(current.children && current.children.length > 0) {
                depth++;
                for(let eachTempNode of current.children) {
                    queue.unshift({
                        isParentmocked: isParentmocked,
                        depth: depth,
                        show: true,
                        showChildren: true,
                        ...eachTempNode
                    });
                }
            }
        }
        return result;
    }

    getIndents(depth) {
        let indents = [];
        for (let i = 0; i < depth; i++) {
            indents.push(<span style={{marginRight: "30px", width: "25px"}}></span>);
        }
        return indents;
    }

    getAllNames(node) {
        let names = [], queue = [], count = 0;
        queue.push({
            ...node
        });
        while (queue.length > 0) {
            let current = queue.shift();
            if(count > 0) names.push(current.service);
            if(current.children && current.children.length > 0) {
                count++;
                for(let eachTempNode of current.children) {
                    queue.unshift({
                        ...eachTempNode
                    });
                }
            }
        }
        return names;
    }

    toggleShowChildren(node, traceDataTree) {
        let names = this.getAllNames(node);
        for(let eachNode of traceDataTree) {
            if(names.indexOf(eachNode.service) > -1){
                eachNode.show = !eachNode.show;
            }
            if(node.service === eachNode.service) {
                eachNode.showChildren = !eachNode.showChildren;
            }
        }
        this.setState({ traceDataTree });
    }

    showDiff(selectedDiffItem) {
        this.setState({ selectedDiffItem });
    }

    async fetchReplayList() {
        const {apiPath, replayId, traceId} = this.state;
        if(!replayId) throw new Error("replayId is required");
        if(!traceId) throw new Error("traceId is required");
        let response, json;
        let user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}?start=0&includeDiff=true&path=%2A&traceId=${traceId}`;
        let dataList = {};

        
        let promises = [], fetchedResults = 0, totalNumberOfRequest = 0, resultSize = 20, replayListData = [];


        try {
            response = await fetch(url, {
                method: "get",
                headers: new Headers({
                    "cache-control": "no-cache",
                    "Authorization": "Bearer " + user['access_token']
                })
            });
            if (response.ok) {
                json = await response.json();
                dataList = json;
                const { cube } = this.props;
                const { instances, selectedApp } = cube;
                let instanceId = "";
                for(let eachRequestItem of dataList.data.res) {
                    if(eachRequestItem.instanceId) {
                        instanceId = eachRequestItem.instanceId;
                        break;
                    }
                }
                for(let eachInstance of instances) {
                    if(eachInstance.app.name === selectedApp && eachInstance.name.toLowerCase() === instanceId.toLowerCase()) {
                        this.loggingURL = eachInstance.loggingURL;
                        break;
                    }
                }
                let diffLayoutData = this.validateAndCreateDiffLayoutData(dataList.data.res);
                this.layoutDataWithDiff.push(...diffLayoutData);
                fetchedResults = dataList.data.res.length;
                totalNumberOfRequest = dataList.data.numFound;
                let allFetched = false;
                let requestHeaders = {
                    headers: {
                        "cache-control": "no-cache",
                        "Authorization": "Bearer " + user['access_token']
                    }
                };
                while(!allFetched) {
                    if(fetchedResults >= totalNumberOfRequest) {
                        allFetched = true;
                        break;
                    }
                    url = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}?start=${fetchedResults}&includeDiff=true&path=%2A&traceId=${traceId}`;
                    promises.push(axios.get(url, requestHeaders));
                    fetchedResults = fetchedResults + resultSize;
                }
                axios.all(promises).then((results) => {
                    results.forEach((eachResponse) => {
                        let eachDiffLayoutData = this.validateAndCreateDiffLayoutData(eachResponse.data.data.res);
                        this.layoutDataWithDiff.push(...eachDiffLayoutData);
                    });
                    this.layoutDataWithDiff.sort((a, b) => {
                        let serviceA = a.service,
                            serviceB = b.service,
                            pathA = a.path,
                            pathB = b.path;
                        if(serviceA === serviceB) {
                            return pathA < pathB ? -1 : pathA > pathB ? 1 : 0;
                        } else {
                            return serviceA < serviceB ? -1 : 1;
                        }
                    });
                    let recordOnlyTree = this.layoutDataWithDiff.filter((item) => {
                        return (item.recordReqId && item.replayReqId) || item.recordReqId; 
                    });
                    let replayOnlyTree = this.layoutDataWithDiff.filter((item) => {
                        return (item.recordReqId && item.replayReqId) || item.replayReqId; 
                    });
                    for (let eachElementObject of this.layoutDataWithDiff) {
                        let tempId = eachElementObject.recordReqId + eachElementObject.replayReqId;
                        let isFound = false;
                        for (let eachUniqueElement of this.uniqueRecordReplayData) {
                            let uniqueTempKey = eachUniqueElement.recordReqId + eachUniqueElement.replayReqId;
                            if(uniqueTempKey === tempId) {
                                isFound = true;
                                break;
                            }
                        }
                        if(!isFound) this.uniqueRecordReplayData.push(eachElementObject);
                    }
                    this.uniqueRecordReplayData.sort((a, b) => {
                        let serviceA = a.service,
                            serviceB = b.service,
                            pathA = a.path,
                            pathB = b.path;
                        if(serviceA === serviceB) {
                            return pathA < pathB ? -1 : pathA > pathB ? 1 : 0;
                        } else {
                            return serviceA < serviceB ? -1 : 1;
                        }
                    });
                    
                    let recProcessedTraceDataTree = arrayToTree(recordOnlyTree, { customID: 'recordedSpanId', parentProperty: 'recordedParentSpanId' });
                    let repProcessedTraceDataTree = arrayToTree(replayOnlyTree, { customID: 'replayedSpanId', parentProperty: 'replayedParentSpanId' });
                    let recProcessedTraceDataFlattenTree = this.flattenTree(recProcessedTraceDataTree);
                    let repProcessedTraceDataFlattenTree = this.flattenTree(repProcessedTraceDataTree);
                    this.setState({
                        recProcessedTraceDataTree: this.state.recProcessedTraceDataTree.concat(recProcessedTraceDataTree),
                        repProcessedTraceDataTree: this.state.repProcessedTraceDataTree.concat(repProcessedTraceDataTree),
                        recProcessedTraceDataFlattenTree: this.state.recProcessedTraceDataFlattenTree.concat(recProcessedTraceDataFlattenTree),
                        repProcessedTraceDataFlattenTree: this.state.repProcessedTraceDataFlattenTree.concat(repProcessedTraceDataFlattenTree),
                        fetchComplete: true,
                        app: dataList.data.app,
                        templateVersion: dataList.data.templateVersion,
                        fetchedResults: fetchedResults
                    });
                });
            } else {
                throw new Error("Response not ok fetchTimeline");
            }
        } catch (e) {
            console.error("fetchTimeline has errors!", e);
            throw e;
        }
    }

    validateAndCleanHTTPMessageParts (messagePart) {
        let cleanedMessagepart = "";
        if (messagePart &&_.isObject(messagePart)) {
            cleanedMessagepart = messagePart;
        } else if (messagePart) {
            try {
                cleanedMessagepart = JSON.parse(messagePart);
            } catch (e) {
                cleanedMessagepart = JSON.parse('"' + cleanEscapedString(_.escape(messagePart)) + '"')
            }
        } else {
            cleanedMessagepart = JSON.parse('""');
        }

        return cleanedMessagepart;
    }

    getDiffForMessagePart(replayedPart, recordedPart, serverSideDiff, prefix, service, path) {
        if (!serverSideDiff) return null; 
        let actpart = JSON.stringify(replayedPart, undefined, 4);
        let expPart = JSON.stringify(recordedPart, undefined, 4);
        let reducedDiffArrayMsgPart = new ReduceDiff(prefix, actpart, expPart, serverSideDiff);
        let reductedDiffArrayMsgPart = reducedDiffArrayMsgPart.computeDiffArray()
        let updatedReductedDiffArrayMsgPart = reductedDiffArrayMsgPart && reductedDiffArrayMsgPart.map((eachItem) => {
            return {
                ...eachItem,
                service,
                app: this.state.app,
                templateVersion: this.state.templateVersion,
                apiPath: path,
                replayId: this.state.replayId,
                recordingId: this.state.recordingId
            }
        });
        return updatedReductedDiffArrayMsgPart;
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

    validateAndCreateDiffLayoutData(replayList) {
        let loggingURL = this.loggingURL;
        let diffLayoutData = replayList.map((item, index) => {
            let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders, prefix = "/body",
                recordedRequestHeaders, replayedRequestHeaders, recordedRequestQParams, replayedRequestQParams, recordedRequestFParams, replayedRequestFParams,recordedRequestBody, replayedRequestBody, reductedDiffArrayReqHeaders, reductedDiffArrayReqBody, reductedDiffArrayReqQParams, reductedDiffArrayReqFParams;
            let isJson = true;
            // processing Response    
            // recorded response body and headers
            if (item.recordResponse) {
                recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
                // check if the content type is JSON and attempt to parse it
                let recordedResponseMime = recordedResponseHeaders["content-type"] ? recordedResponseHeaders["content-type"][0] : "" ;
                isJson = recordedResponseMime.toLowerCase().indexOf("json") > -1;
                if (item.recordResponse.body && isJson) {
                    try {
                        recordedData = JSON.parse(item.recordResponse.body);
                    } catch (e) {
                        recordedData = JSON.parse('"' + cleanEscapedString(_.escape(item.recordResponse.body)) + '"')
                    }
                }
                else {
                    // in case the content type isn't json, display the entire body if present, or else an empty string
                    recordedData = item.recordResponse.body ? item.recordResponse.body : '""';
                }
            } else {
                recordedResponseHeaders = "";
                recordedData = "";
            }

            // same as above but for replayed response
            if (item.replayResponse) {
                replayedResponseHeaders = item.replayResponse.hdrs ? item.replayResponse.hdrs : [];
                // check if the content type is JSON and attempt to parse it
                let replayedResponseMime = replayedResponseHeaders["content-type"] ? replayedResponseHeaders["content-type"][0] : "";
                isJson = replayedResponseMime.toLowerCase().indexOf("json") > -1;
                if (item.replayResponse.body && isJson) {
                    try {
                        replayedData = JSON.parse(item.replayResponse.body);
                    } catch (e) {
                        replayedData = JSON.parse('"' + cleanEscapedString(_.escape(item.replayResponse.body)) + '"')
                    }
                }
                else {
                    // in case the content type isn't json, display the entire body if present, or else an empty string
                    replayedData = item.replayResponse.body ? item.replayResponse.body : '""';
                }
            } else {
                replayedResponseHeaders = "";
                replayedData = "";
            }
            let diff;
            
            if (item.respCompDiff && item.respCompDiff.length !== 0) {
                diff = item.respCompDiff;
            } else {
                diff = [];
            }
            let actJSON = JSON.stringify(sortJson(replayedData), undefined, 4),
                expJSON = JSON.stringify(sortJson(recordedData), undefined, 4);
            let reductedDiffArray = null, missedRequiredFields = [], reducedDiffArrayRespHdr = null;

            let actRespHdrJSON = JSON.stringify(replayedResponseHeaders, undefined, 4);
            let expRespHdrJSON = JSON.stringify(recordedResponseHeaders, undefined, 4);
            

            // use the backend diff and the two JSONs to generate diff array that will be passed to the diff renderer
            if (diff && diff.length > 0) {
                // skip calculating the diff array in case of non json data 
                // pass diffArray as null so that the diff library can render it directly
                if (isJson) { 
                    let reduceDiff = new ReduceDiff(prefix, actJSON, expJSON, diff);
                    reductedDiffArray = reduceDiff.computeDiffArray();
                }
                let expJSONPaths = generator(recordedData, "", "", prefix);
                missedRequiredFields = diff.filter((eachItem) => {
                    return eachItem.op === "noop" && eachItem.resolution.indexOf("ERR_REQUIRED") > -1 && !expJSONPaths.has(eachItem.path);
                })

                let reduceDiffHdr = new ReduceDiff("/hdrs", actRespHdrJSON, expRespHdrJSON, diff);
                reducedDiffArrayRespHdr = reduceDiffHdr.computeDiffArray();

            } else if (diff && diff.length == 0) {
                if (_.isEqual(expJSON, actJSON)) {
                    let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                    reductedDiffArray = reduceDiff.computeDiffArray();
                    let reduceDiffHdr = new ReduceDiff("/hdrs", actRespHdrJSON, expRespHdrJSON, diff);
                    reducedDiffArrayRespHdr = reduceDiffHdr.computeDiffArray();
                }
            }
            let updatedReductedDiffArray = reductedDiffArray && reductedDiffArray.map((eachItem) => {
                return {
                    ...eachItem,
                    recordReqId: item.recordReqId,
                    replayReqId: item.replayReqId,
                    service: item.service,
                    app: this.state.app,
                    templateVersion: this.state.templateVersion,
                    apiPath: item.path,
                    replayId: this.state.replayId,
                    recordingId: this.state.recordingId
                }
            });

            let updatedReductedDiffArrayWithCollapsible = this.addCompressToggleData(updatedReductedDiffArray, this.state.collapseLength, this.state.maxLinesLength);

            let updatedReducedDiffArrayRespHdr = reducedDiffArrayRespHdr && reducedDiffArrayRespHdr.map((eachItem) => {
                return {
                    ...eachItem,
                    service: item.service,
                    app: this.state.app,
                    templateVersion: this.state.templateVersion,
                    apiPath: item.path,
                    replayId: this.state.replayId,
                    recordingId: this.state.recordingId
                }
            });

            // process Requests
            // recorded request header and body
            // parse and clean up body string
            if (item.recordRequest) {
                recordedRequestHeaders = this.validateAndCleanHTTPMessageParts(item.recordRequest.hdrs);
                recordedRequestBody = this.validateAndCleanHTTPMessageParts(item.recordRequest.body);
                recordedRequestQParams = this.validateAndCleanHTTPMessageParts(item.recordRequest.queryParams);
                recordedRequestFParams = this.validateAndCleanHTTPMessageParts(item.recordRequest.formParams);
            } else {
                recordedRequestHeaders = "";
                recordedRequestBody = "";
                recordedRequestQParams = "";
                recordedRequestFParams = "";
            }

            // replayed request header and body
            // same as above
            if (item.replayRequest) {
                replayedRequestHeaders = this.validateAndCleanHTTPMessageParts(item.replayRequest.hdrs);
                replayedRequestBody = this.validateAndCleanHTTPMessageParts(item.replayRequest.body);
                replayedRequestQParams = this.validateAndCleanHTTPMessageParts(item.replayRequest.queryParams);
                replayedRequestFParams = this.validateAndCleanHTTPMessageParts(item.replayRequest.formParams);
            } else {
                replayedRequestHeaders = "";
                replayedRequestBody = "";
                replayedRequestQParams = "";
                replayedRequestFParams = "";
            }

            reductedDiffArrayReqHeaders = this.getDiffForMessagePart(replayedRequestHeaders, recordedRequestHeaders, item.reqCompDiff, "/hdrs", item.service, item.path);
            reductedDiffArrayReqQParams = this.getDiffForMessagePart(replayedRequestQParams, recordedRequestQParams, item.reqCompDiff, "/queryParams", item.service, item.path);
            reductedDiffArrayReqFParams = this.getDiffForMessagePart(replayedRequestFParams, recordedRequestFParams, item.reqCompDiff, "/queryParams", item.service, item.path);
            reductedDiffArrayReqBody = this.getDiffForMessagePart(replayedRequestBody, recordedRequestBody, item.reqCompDiff, "/body", item.service, item.path);

            return {
                ...item,
                recordedResponseHeaders,
                replayedResponseHeaders,
                recordedData,
                replayedData,
                actJSON,
                expJSON,
                parsedDiff: diff,
                reductedDiffArray: updatedReductedDiffArrayWithCollapsible,
                missedRequiredFields,
                show: true,
                recordedRequestHeaders,
                replayedRequestHeaders,
                recordedRequestQParams,
                replayedRequestQParams,
                recordedRequestFParams,
                replayedRequestFParams,
                recordedRequestBody,
                replayedRequestBody,
                updatedReducedDiffArrayRespHdr,
                reductedDiffArrayReqHeaders,
                reductedDiffArrayReqQParams,
                reductedDiffArrayReqFParams,
                reductedDiffArrayReqBody,
                loggingURL: loggingURL ? loggingURL.replace("$STARTTIME", "'" + moment(item.replayReqTime).toISOString() + "'").replace("$ENDTIME", "'" + moment(Math.ceil(item.replayRespTime / (2 * 60 * 1000)) * (1.5 * 60 * 1000)).toISOString() + "'") : ""
            }
        });
        return diffLayoutData;
    }

    getErrResoultionCount(resolutionType, eachMessage) {
        let givenResType = eachMessage.resolutionTypes.filter( resType => resType.value === resolutionType);
        return givenResType.length > 0 ? {count : givenResType[0].count, label: givenResType[0].count} : {count : 0, label: ""};
    }

    getHttpStatus = (code) => {
        for (let httpStatus of statusCodeList) {
            if (code == httpStatus.status) {
                return httpStatus.value;
            }
        }

        return code;
    };

    render() {
        let { recProcessedTraceDataFlattenTree, repProcessedTraceDataFlattenTree, app, service, apiPath, selectedDiffItem, selectedResolutionType, showTrace, showLogs, collapseLength, incrementCollapseLengthForRecReqId, incrementCollapseLengthForRepReqId, maxLinesLength } = this.state;
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
        let recProcessedTraceDataFlattenTreeResCount = recProcessedTraceDataFlattenTree.map(eachItem => {
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
        let repProcessedTraceDataFlattenTreeResCount = repProcessedTraceDataFlattenTree.map(eachItem => {
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
            <div className="content-wrapper">
                <div style={{opacity: 0.6}}><h4><Glyphicon style={{ visibility:  "visible" }} glyph="search" /> <span>TRACE REQUEST</span></h4></div>
                <div>
                    <div style={{display: "inline-block"}}>
                            <span>MovieInfo</span>
                            <span>&nbsp;&nbsp; / &nbsp;&nbsp;</span>
                            <span>
                                <strong>Service:&nbsp;</strong>
                                <span>{service}</span>
                            </span>
                            <span>&nbsp;&nbsp; / &nbsp;&nbsp;</span>
                            <span>
                                <strong>API Path:&nbsp;</strong>
                                <span>{apiPath}</span>
                            </span>
                    </div>
                    <div style={{display: "inline-block"}} className="pull-right">
                        <Button bsSize="small" bsStyle={"primary"} href={"/test_config_view"} style={{}}>VIEW SERVICE MESH</Button>
                        <span style={{borderRight: "1px solid #ccc", paddingLeft: "5px", marginRight: "9px"}}></span>
                        <Button bsSize="small" bsStyle={"primary"} href={"/diff_results" + removeURLParameter(window.location.search, "traceId")} style={{}}><Glyphicon style={{ visibility:  "visible" }} glyph="menu-left" /> <span>BACK TO DIFF</span></Button>
                    </div>
                </div>
                
                <div style={{marginTop: "18px", border: "1px solid #ccc", padding: "0"}}>
                    <Tabs defaultActiveKey={1} id="uncontrolled-tab-example">
                        <Tab eventKey={1} title="Recorded Requests">
                            <Table hover responsive style={{borderSpacing: "0 0", borderCollapse: "separate"}}>
                                <tbody>
                                    <tr>
                                        <th style={{verticalAlign: "middle", padding: "12px"}}>SERVICE BY TRACE ORDER</th>
                                        <th style={{verticalAlign: "middle", padding: "12px"}}>API PATH</th>
                                        <th style={{verticalAlign: "middle", padding: "12px"}} className="text-center">TEST RESULTS (BY RESOLUTION)</th>
                                    </tr>
                                    <tr>
                                        <td style={{verticalAlign: "middle", padding: "12px"}}><Glyphicon style={{ visibility:  "visible", marginRight: "9px" }} glyph="home" /> <span>{app}</span></td>
                                        <td style={{verticalAlign: "middle", padding: "12px"}}></td>
                                        <td style={{verticalAlign: "middle", padding: "12px"}} className="text-center">
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>REQ MIS</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>RES ERR</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}></div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>DT MIS</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>VAL ERR</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>ITM MIS</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>GLD ERR</div>
                                        </td>
                                    </tr>
                                    {recProcessedTraceDataFlattenTreeResCount.map((item, index) => {
                                        return (<tr key={item.recordReqId + item.replayReqId + index} onClick={(event) =>  item.isParentmocked ? event.stopPropagation() : this.showDiff(item)} style={{display: item.show ? "" : "none", cursor: "pointer", backgroundColor: item.isParentmocked ? "#A9A9A9": (selectedDiffItem && item.recordReqId === selectedDiffItem.recordReqId && item.replayReqId === selectedDiffItem.replayReqId) ? "#eee" : "#fff"}}>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>
                                                {this.getIndents(item.depth)}
                                                {item.depth === 0 ? (<span><i className="fas fa-arrow-right" style={{fontSize: "14px", marginRight: "12px"}}></i></span>) : (<span><i className="fas fa-level-up-alt fa-rotate-90" style={{fontSize: "14px", marginRight: "12px"}}></i></span>)}
                                                {item.children && item.children.length > 0 ? (<span><i className={item.showChildren ? "far fa-minus-square" : "far fa-plus-square"} style={{fontSize: "12px", marginRight: "12px", cursor: "pointer"}} onClick={(evt) => {evt.stopPropagation(); this.toggleShowChildren(item, recProcessedTraceDataFlattenTreeResCount); return false;}}></i></span>) : ("")}
                                                <span>{item.service}</span>
                                            </td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>{item.path}</td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}} className="text-center">
                                                
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: item.reqMatchResType === "NoMatch" ? "red" : "green", lineHeight: "30px", margin: "0 auto", textAlign: "center", opacity: 0.7}}></div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: item.respCompResType === "NoMatch" ? "red" : "green", lineHeight: "30px", margin: "0 auto", textAlign: "center", opacity: 0.7}}></div>
                                                </div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_ValTypeMismatch", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_ValTypeMismatch", item).label}</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_ValMismatch", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_ValMismatch", item).label}</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_Required", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_Required", item).label}</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_RequiredGolden", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_RequiredGolden", item).label}</div>
                                                </div>
                                            </td>
                                        </tr>)
                                    })}
                                </tbody>
                            </Table>
                        </Tab>
                        <Tab eventKey={2} title="Replayed Requests">
                            <Table hover responsive style={{borderSpacing: "0 0", borderCollapse: "separate"}}>
                                <tbody>
                                    <tr>
                                        <th style={{verticalAlign: "middle", padding: "12px"}}>SERVICE BY TRACE ORDER</th>
                                        <th style={{verticalAlign: "middle", padding: "12px"}}>API PATH</th>
                                        <th style={{verticalAlign: "middle", padding: "12px"}} className="text-center">TEST RESULTS (BY RESOLUTION)</th>
                                    </tr>
                                    <tr>
                                        <td style={{verticalAlign: "middle", padding: "12px"}}><Glyphicon style={{ visibility:  "visible", marginRight: "9px" }} glyph="home" /> <span>{app}</span></td>
                                        <td style={{verticalAlign: "middle", padding: "12px"}}></td>
                                        <td style={{verticalAlign: "middle", padding: "12px"}} className="text-center">
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>REQ MIS</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>RES ERR</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}></div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>DT MIS</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>VAL ERR</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>ITM MIS</div>
                                            <div style={{width: "12px", textAlign: "center", display: "inline-block"}}>|</div>
                                            <div style={{width: "54px", textAlign: "center", display: "inline-block"}}>GLD ERR</div>
                                        </td>
                                    </tr>
                                    {repProcessedTraceDataFlattenTreeResCount.map((item, index) => {
                                        return (<tr key={item.recordReqId + item.replayReqId + index} onClick={() => this.showDiff(item)} style={{display: item.show ? "" : "none", cursor: "pointer", backgroundColor: (selectedDiffItem && item.recordReqId === selectedDiffItem.recordReqId && item.replayReqId === selectedDiffItem.replayReqId) ? "#eee" : "#fff"}}>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>
                                                {this.getIndents(item.depth)}
                                                {item.depth === 0 ? (<span><i className="fas fa-arrow-right" style={{fontSize: "14px", marginRight: "12px"}}></i></span>) : (<span><i className="fas fa-level-up-alt fa-rotate-90" style={{fontSize: "14px", marginRight: "12px"}}></i></span>)}
                                    {item.children && item.children.length > 0 ? (<span><i className={item.showChildren ? "far fa-minus-square" : "far fa-plus-square"} style={{fontSize: "12px", marginRight: "12px", cursor: "pointer"}} onClick={(evt) => {evt.stopPropagation(); this.toggleShowChildren(item, repProcessedTraceDataFlattenTreeResCount); return false;}}></i></span>) : ("")}
                                                <span>{item.service}</span>
                                            </td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>{item.path}</td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}} className="text-center">
                                                
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: item.reqMatchResType === "NoMatch" ? "red" : "green", lineHeight: "30px", margin: "0 auto", textAlign: "center", opacity: 0.7}}></div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: item.respCompResType === "NoMatch" ? "red" : "green", lineHeight: "30px", margin: "0 auto", textAlign: "center", opacity: 0.7}}></div>
                                                </div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_ValTypeMismatch", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_ValTypeMismatch", item).label}</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_ValMismatch", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_ValMismatch", item).label}</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_Required", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_Required", item).label}</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: this.getErrResoultionCount("ERR_RequiredGolden", item).count > 0 ? "#fdb8c0" : "#acf2bd", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>{this.getErrResoultionCount("ERR_RequiredGolden", item).label}</div>
                                                </div>
                                            </td>
                                        </tr>)
                                    })}
                                </tbody>
                            </Table>
                        </Tab>
                    </Tabs>
                </div>
                {selectedDiffItem && (
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
                            <div style={{display: "inline-block"}} className="pull-right">
                                <Button bsSize="small" bsStyle={"primary"} style={{}} onClick={this.toggleBetweenTraceAndLogs}>
                                    {showTrace ? "VIEW LOGS" : "VIEW TRACE"}
                                </Button>
                            </div>
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
                        <div style={{marginTop: "9px", display: showTrace ? "none": ""}}>
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
                        </div>
                        <div style={{marginTop: "9px", display: showLogs ? "none": ""}}>
                            {(this.state.showRequestMessageHeaders || this.state.shownRequestMessageHeaders) && (
                                <div style={{ display: this.state.showRequestMessageHeaders ? "" : "none" }}>
                                    <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Headers</Label></h4>
                                    <div className="headers-diff-wrapper">
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
                                    <div className="headers-diff-wrapper">
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
                                    <div className="headers-diff-wrapper">
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
                                    <div className="headers-diff-wrapper">
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
                                    <div className="headers-diff-wrapper">
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
                                        <div className="diff-wrapper">
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


function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedViewTrace = connect(mapStateToProps)(ViewTrace);

export default connectedViewTrace;
