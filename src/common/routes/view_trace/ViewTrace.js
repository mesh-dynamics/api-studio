import React, { Component } from 'react';
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Table, ButtonGroup, Button, Radio, Tabs, Tab} from 'react-bootstrap';
import _ from 'lodash';
import arrayToTree  from 'array-to-tree';
import axios from "axios";

import ReactDiffViewer from '../../utils/diff/diff-main';
import ReduceDiff from '../../utils/ReduceDiff';
import config from "../../config";
import generator from '../../utils/generator/json-path-generator';

import {connect} from "react-redux";
import {cubeActions} from "../../actions";
import {Link} from "react-router-dom";
import {getSearchHistoryParams, updateSearchHistoryParams} from "../../utils/lib/url-utils";

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
            showRequestMessageParams: false,
            shownRequestMessageParams: false,
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
            traceId: null
        }

        this.inputElementRef = React.createRef();
        this.layoutDataWithDiff = [];
        this.uniqueRecordReplayData = [];

        this.handleSearchFilterChange = this.handleSearchFilterChange.bind(this);
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
        const requestParams = urlParameters["requestParams"];
        const requestBody = urlParameters["requestBody"];
        const responseHeaders = urlParameters["responseHeaders"];
        const responseBody = urlParameters["responseBody"];
        const timeStamp = decodeURI(urlParameters["timeStamp"]);
        const traceId = urlParameters["traceId"];

        dispatch(cubeActions.setSelectedApp(app));
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
            // request params
            showRequestMessageParams: requestParams ? JSON.parse(requestParams) : false,
            shownRequestMessageParams: requestParams ? JSON.parse(requestParams) : false,
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

    handleSearchFilterChange(e) {

        this.setState({ searchFilterPath: e.target.value });
    }

    flattenTree(traceDataTree) {
        let depth = 0, result = [], queue = [];
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
            if(current.children && current.children.length > 0) {
                depth++;
                for(let eachTempNode of current.children) {
                    queue.unshift({
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

    validateAndCreateDiffLayoutData(replayList) {
        let diffLayoutData = replayList.map((item, index) => {
            let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders, prefix = "/body",
                recordedRequestHeaders, replayedRequestHeaders, recordedRequestParams, replayedRequestParams, recordedRequestBody,
                replayedRequestBody;
            let isJson = true, recordedSpanId = null, recordedParentSpanId = null, replayedSpanId = null, replayedParentSpanId = null;
            // processing Response    
            // recorded response body and headers
            if (item.recordResponse) {
                recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
                // check if the content type is JSON and attempt to parse it
                let recordedResponseMime = recordedResponseHeaders["content-type"][0];
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
                recordedResponseHeaders = null;
                recordedData = null;
            }

            // same as above but for replayed response
            if (item.replayResponse) {
                replayedResponseHeaders = item.replayResponse.hdrs ? item.replayResponse.hdrs : [];
                // check if the content type is JSON and attempt to parse it
                let replayedResponseMime = replayedResponseHeaders["content-type"][0];
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
                replayedResponseHeaders = null;
                replayedData = "";
            }
            let diff;
            if (item.respCompDiff && item.respCompDiff.length !== 0) {
                diff = item.respCompDiff;
            } else {
                diff = [];
            }
            let actJSON = JSON.stringify(replayedData, undefined, 4),
                expJSON = JSON.stringify(recordedData, undefined, 4);
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
                }
            }
            let updatedReductedDiffArray = reductedDiffArray && reductedDiffArray.map((eachItem) => {
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
                recordedRequestHeaders = item.recordRequest.hdrs ? item.recordRequest.hdrs : {};
                recordedRequestParams = item.recordRequest.queryParams ? item.recordRequest.queryParams : {};
                recordedSpanId = recordedRequestHeaders["x-b3-spanid"] ? recordedRequestHeaders["x-b3-spanid"][0] : null;
                recordedParentSpanId = recordedRequestHeaders["x-b3-parentspanid"] ? recordedRequestHeaders["x-b3-parentspanid"][0] : null;
                if (item.recordRequest.body) {
                    try {
                        recordedRequestBody = JSON.parse(item.recordRequest.body);
                    } catch (e) {
                        recordedRequestBody = JSON.parse('"' + cleanEscapedString(_.escape(item.recordRequest.body)) + '"')
                    }
                }
                else {
                    recordedRequestBody = JSON.parse('""');
                }
            } else {
                recordedRequestHeaders = null;
                recordedRequestBody = "";
                recordedRequestParams = null;
            }

            // replayed request header and body
            // same as above
            if (item.replayRequest) { 
                replayedRequestHeaders = item.replayRequest.hdrs ? item.replayRequest.hdrs : {};
                replayedRequestParams = item.replayRequest.queryParams ? item.replayRequest.queryParams : {};
                replayedSpanId = replayedRequestHeaders["x-b3-spanid"] ? replayedRequestHeaders["x-b3-spanid"][0] : null;
                replayedParentSpanId = replayedRequestHeaders["x-b3-parentspanid"] ? replayedRequestHeaders["x-b3-parentspanid"][0] : null;
                if (item.replayRequest.body) {
                    try {
                        replayedRequestBody = JSON.parse(item.replayRequest.body);
                    } catch (e) {
                        replayedRequestBody = JSON.parse('"' + cleanEscapedString(_.escape(item.replayRequest.body)) + '"')
                    }
                }
                else {
                    replayedRequestBody = JSON.parse('""');
                }
            } else {
                replayedRequestHeaders = "";
                replayedRequestBody = "";
                replayedRequestParams = "";
            }
            return {
                recordedParentSpanId,
                recordedSpanId,
                replayedParentSpanId,
                replayedSpanId,
                ...item,
                recordedResponseHeaders,
                replayedResponseHeaders,
                recordedData,
                replayedData,
                actJSON,
                expJSON,
                parsedDiff: diff,
                reductedDiffArray: updatedReductedDiffArray,
                missedRequiredFields,
                show: true,
                recordedRequestHeaders,
                replayedRequestHeaders,
                recordedRequestParams,
                replayedRequestParams,
                recordedRequestBody,
                replayedRequestBody,
                updatedReducedDiffArrayRespHdr
            }
        });
        return diffLayoutData;
    }

    render() {
        let { recProcessedTraceDataFlattenTree, repProcessedTraceDataFlattenTree, app, service, apiPath, selectedDiffItem } = this.state;
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
                        <Button bsSize="small" bsStyle={"primary"} href={"/shareable_link" + removeURLParameter(window.location.search, "traceId")} style={{}}><Glyphicon style={{ visibility:  "visible" }} glyph="menu-left" /> <span>BACK TO DIFF</span></Button>
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
                                    {recProcessedTraceDataFlattenTree.map((item, index) => {
                                        return (<tr key={item.recordReqId + item.replayReqId} onClick={() => this.showDiff(item)} style={{display: item.show ? "" : "none", cursor: "pointer", backgroundColor: (selectedDiffItem && item.recordReqId === selectedDiffItem.recordReqId && item.replayReqId === selectedDiffItem.replayReqId) ? "#eee" : "#fff"}}>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>
                                                {this.getIndents(item.depth)}
                                                {item.depth === 0 ? (<span><i className="fas fa-arrow-right" style={{fontSize: "14px", marginRight: "12px"}}></i></span>) : (<span><i className="fas fa-level-up-alt fa-rotate-90" style={{fontSize: "14px", marginRight: "12px"}}></i></span>)}
                                                {item.children && item.children.length > 0 ? (<span><i className={item.showChildren ? "far fa-minus-square" : "far fa-plus-square"} style={{fontSize: "12px", marginRight: "12px", cursor: "pointer"}} onClick={(evt) => {evt.stopPropagation(); this.toggleShowChildren(item, recProcessedTraceDataFlattenTree); return false;}}></i></span>) : ("")}
                                                <span>{item.service}</span>
                                            </td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>{item.path}</td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}} className="text-center">
                                                
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
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
                                    {repProcessedTraceDataFlattenTree.map((item, index) => {
                                        return (<tr key={item.recordReqId + item.replayReqId} onClick={() => this.showDiff(item)} style={{display: item.show ? "" : "none", cursor: "pointer", backgroundColor: (selectedDiffItem && item.recordReqId === selectedDiffItem.recordReqId && item.replayReqId === selectedDiffItem.replayReqId) ? "#eee" : "#fff"}}>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>
                                                {this.getIndents(item.depth)}
                                                {item.depth === 0 ? (<span><i className="fas fa-arrow-right" style={{fontSize: "14px", marginRight: "12px"}}></i></span>) : (<span><i className="fas fa-level-up-alt fa-rotate-90" style={{fontSize: "14px", marginRight: "12px"}}></i></span>)}
                                    {item.children && item.children.length > 0 ? (<span><i className={item.showChildren ? "far fa-minus-square" : "far fa-plus-square"} style={{fontSize: "12px", marginRight: "12px", cursor: "pointer"}} onClick={(evt) => {evt.stopPropagation(); this.toggleShowChildren(item, repProcessedTraceDataFlattenTree); return false;}}></i></span>) : ("")}
                                                <span>{item.service}</span>
                                            </td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}}>{item.path}</td>
                                            <td style={{verticalAlign: "middle", padding: "12px"}} className="text-center">
                                                
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
                                                </div>
                                                <div style={{height: "30px", width: "12px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}></div>
                                                <div style={{height: "30px", width: "54px", backgroundColor: "transparent", display: "inline-block", verticalAlign: "top"}}>
                                                    <div style={{height: "30px", width: "40px", backgroundColor: "grey", lineHeight: "30px", margin: "0 auto", textAlign: "center"}}>0</div>
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
                        <div style={{marginTop: "9px"}}>
                            <div className="diff-wrapper">
                                < ReactDiffViewer
                                    styles={newStyles}
                                    oldValue={selectedDiffItem.expJSON}
                                    newValue={selectedDiffItem.actJSON}
                                    splitView={true}
                                    disableWordDiff={false}
                                    diffArray={selectedDiffItem.reductedDiffArray}
                                    onLineNumberClick={(lineId, e) => { return lineId; }}
                                    inputElementRef={this.inputElementRef}
                                    showAll={this.state.showAll}
                                    searchFilterPath={this.state.searchFilterPath}
                                />
                            </div>
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
