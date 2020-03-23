import React, { Component, createContext } from 'react';
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';
import _ from 'lodash';
import axios from "axios";
import sortJson from "../../utils/sort-json";
import ReactDiffViewer from '../../utils/diff/diff-main';
import ReduceDiff from '../../utils/ReduceDiff';
import config from "../../config";
import generator from '../../utils/generator/json-path-generator';
import {connect} from "react-redux";
import {cubeActions} from "../../actions";
import {Link} from "react-router-dom";
import Modal from "react-bootstrap/lib/Modal";
import {resolutionsIconMap} from '../../components/Resolutions.js'
import {getSearchHistoryParams, updateSearchHistoryParams} from "../../utils/lib/url-utils";
import statusCodeList from "../../StatusCodeList";
import "../../components/Diff.css"
import {validateAndCreateDiffLayoutData, addCompressToggleData} from "../../utils/diff/diff-process.js"

const ShareableLinkContext = createContext();

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

class ShareableLink extends Component {
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
            showNewGolden: false,
            app: "",
            templateVersion: "",
            newTemplateVerInfo: null,
            golden: null,
            apiPath: "",
            service: "",
            replayId: null,
            recordingId: null,
            currentPageNumber: 1,
            isFetching: true,
            fetchComplete: false,
            fetchedResults: 0,
            selectedReqRespMatchType: "responseMismatch",
            showAll: true,
            showSaveGoldenModal: false,
            nameG: "",
            branch: "",
            version: "",
            tag: "",
            commitId: "",
            saveGoldenError: "",
            timeStamp: "",
            popoverCurrentPath: "",
            collapseLength: 2,
            collapseLengthIncrement: 10,
            incrementCollapseLengthForRecReqId: null,
            incrementCollapseLengthForRepReqId: null,
            incrementStartJsonPath: null
        };
        this.handleSearchFilterChange = this.handleSearchFilterChange.bind(this);
        this.handleReqRespMtChange = this.handleReqRespMtChange.bind(this)
        this.toggleMessageContents = this.toggleMessageContents.bind(this);
        this.changePageNumber = this.changePageNumber.bind(this);
        this.increaseCollapseLength = this.increaseCollapseLength.bind(this);

        this.inputElementRef = React.createRef();
        this.pageSize = 5;
        this.pages = 0;
        this.layoutDataWithDiff = [];
        this.getSearchHistoryParams = getSearchHistoryParams();
        this.pageNumberVsDataIndex = []
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

        dispatch(cubeActions.setSelectedApp(app));
        this.setState({
            apiPath: apiPath,
            replayId: replayId,
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
            showAll: (selectedResolutionType === "All"),
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
            dispatch(cubeActions.getCollectionUpdateOperationSet(app));
            dispatch(cubeActions.setGolden({golden: recordingId, timeStamp: ""}));
            dispatch(cubeActions.getNewTemplateVerInfo(app, currentTemplateVer));
            dispatch(cubeActions.getJiraBugs(replayId, apiPath));
            this.fetchReplayList();
        });
    }

    componentDidUpdate(prevProps, prevState) {
    }

    componentWillReceiveProps(nextProps, prevState) {
        let { cube, dispatch } = nextProps;
        if (cube && (cube.goldenInProg || cube.newGoldenId)) {
            this.setState({ showNewGolden: true });
        } else {
            this.setState({ showNewGolden: false });
        }
    }

    componentWillUnmount() {
        let { dispatch } = this.props;
        dispatch(cubeActions.clearGolden());
        this.setState({ showNewGolden: false });
    }

    handleClose = () => {
        const { history, dispatch } = this.props;
        dispatch(cubeActions.clearGolden());
        this.setState({ showNewGolden: false });
        setTimeout(() => {
            history.push("/test_config");
        })
    }

    handleCloseDone = () => {
        let { dispatch } = this.props;
        dispatch(cubeActions.clearGolden());
        this.setState({ showNewGolden: false });
    };

    handleBackToDashboardClick = () => {
        const { history, dispatch } = this.props;
        dispatch(cubeActions.clearPathResultsParams());
    }

    handleSearchFilterChange(e) {
        const { history } = this.props;

        this.setState({ searchFilterPath: e.target.value });

        this.historySearchParams = updateSearchHistoryParams("searchFilterPath", e.target.value, this.state);

        history.push({
            pathname: '/shareable_link',
            search: this.historySearchParams
        });
    }

    increaseCollapseLength(e, jsonPath, recordReqId, replayReqId) {
        const { collapseLength, collapseLengthIncrement } = this.state;
        this.setState({ collapseLength: (collapseLength + collapseLengthIncrement), incrementCollapseLengthForRecReqId: recordReqId, incrementCollapseLengthForRepReqId: replayReqId, incrementStartJsonPath: jsonPath});
    }

    handleReqRespMtChange(e) {
        const { history } = this.props;

        this.setState({
            selectedReqRespMatchType: e.target.value, 
            selectedResolutionType: "All"
        });

        this.historySearchParams = updateSearchHistoryParams("selectedReqRespMatchType", e.target.value, this.state);
        this.historySearchParams = updateSearchHistoryParams("selectedResolutionType", "All", this.state);

        history.push({
            pathname: '/shareable_link',
            search: this.historySearchParams
        });
    }

    handleCurrentPopoverPathChange = (popoverCurrentPath) => this.setState({ popoverCurrentPath });

    changePageNumber(e) {
        this.setState({ currentPageNumber: +e.target.innerHTML.trim()});
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

        this.historySearchParams = updateSearchHistoryParams(e.target.value, e.target.checked, this.state);

        history.push({
            pathname: '/shareable_link',
            search: this.historySearchParams
        });
    }

    handleMetaDataSelect(metaDataType, value) {
        const { history, dispatch } = this.props;
        this.historySearchParams = updateSearchHistoryParams(metaDataType, value, this.state);

        if (metaDataType == "selectedService") {
            this.setState({
                service: value, 
                [metaDataType] : value, 
                selectedAPI: "", 
                selectedResolutionType: "All",
                currentPageNumber: 1
            });
            this.historySearchParams = updateSearchHistoryParams("selectedResolutionType", "All", this.state);

        } else if (metaDataType == "selectedAPI") {
            this.setState({
                apiPath: value, 
                [metaDataType] : value, 
                selectedResolutionType: "All",
                currentPageNumber: 1
            });

            this.historySearchParams = updateSearchHistoryParams("selectedResolutionType", "All", this.state);
            
            setTimeout(() => {
                dispatch(cubeActions.setPathResultsParams({
                    path: value,
                    service: this.state.service,
                    replayId: this.state.replayId,
                    recordingId: this.state.recordingId,
                    currentTemplateVer: this.state.currentTemplateVer
                }));
            });
        } else if (metaDataType == "selectedResolutionType") {
            this.setState({
                selectedResolutionType : value, 
                showAll : (value ===  "All"), 
                currentPageNumber: 1
            });
        } else {
            this.setState({[metaDataType] : value});
        }

        history.push({
                pathname: '/shareable_link',
                search: this.historySearchParams
        })
    }

    async fetchReplayList() {
        const {apiPath, replayId, app, recordingId, templateVersion} = this.state;
        if(!replayId) throw new Error("replayId is required");
        let response, json;
        let user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}?start=0&includeDiff=true&path=%2A`;
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
                let diffLayoutData = validateAndCreateDiffLayoutData(dataList.data.res, app, replayId, recordingId, templateVersion, config.diffCollapseLength);
                this.layoutDataWithDiff.push(...diffLayoutData);

                fetchedResults = dataList.data.res.length;
                totalNumberOfRequest = dataList.data.numFound;
                let allFetched = false;
                this.setState({
                    app: dataList.data.app,
                    templateVersion: dataList.data.templateVersion,
                    fetchedResults: fetchedResults
                });
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
                    url = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}?start=${fetchedResults}&includeDiff=true&path=%2A`;
                    promises.push(axios.get(url, requestHeaders));
                    fetchedResults = fetchedResults + resultSize;
                }
                axios.all(promises).then((results) => {
                    results.forEach((eachResponse) => {
                        let eachDiffLayoutData = validateAndCreateDiffLayoutData(eachResponse.data.data.res, app, replayId, recordingId, templateVersion, config.diffCollapseLength);
                        this.layoutDataWithDiff.push(...eachDiffLayoutData);
                    });
                    this.setState({ isFetching: false, fetchComplete: true });
                });
            } else {
                this.setState({ isFetching: false, fetchComplete: true });
                throw new Error("Response not ok fetchTimeline");
            }
        } catch (e) {
            this.setState({ isFetching: false, fetchComplete: false });
            console.error("fetchTimeline has errors!", e);
            throw e;
        }
    }

    /*
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
            cleanedMessagepart = messagePart || JSON.parse('""');
        }

        return cleanedMessagepart;
    }

    getDiffForMessagePart(replayedPart, recordedPart, serverSideDiff, prefix, service, path) {
        if (!serverSideDiff || serverSideDiff.length === 0) return null; 
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

    addCompressToggleData(diffData, collapseLength) {
        let indx  = 0, atleastADiff = false;
        if(!diffData) return diffData;
        for (let i = 0; i < diffData.length; i++) {
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
        let toggleDrawChunk  = false;
        for (let eachChunk of diffData) {
            if(eachChunk.collapseChunk === true && toggleDrawChunk === false) {
                toggleDrawChunk = true;
                eachChunk["drawChunk"] = true;
            } else if(eachChunk.collapseChunk === true && toggleDrawChunk === true) {
                eachChunk["drawChunk"] = false;
            } else if(eachChunk.collapseChunk === false) {
                toggleDrawChunk = false;
                eachChunk["drawChunk"] = false;
            }
        }
        return diffData;
    }

    addHasDiffToParentPath (diffData) {
        if(!diffData) return diffData;
        const BEGIN_BRACKET = "<BEGIN>", END_BRACKET = "<END>";
        for (let i = 0; i < diffData.length; i++) {
            let diffDataChunk = diffData[i];
            diffDataChunk["hasDiff"] = false;
            diffDataChunk["showDiff"] = false;
            if(diffDataChunk.serverSideDiff !== null || (diffDataChunk.added || diffDataChunk.removed)) {
                if(diffDataChunk.jsonPath.indexOf(BEGIN_BRACKET) > -1) {
                    diffDataChunk["hasDiff"] = true;
                    diffDataChunk["showDiff"] = true;
                } else {
                    diffDataChunk["hasDiff"] = true;
                    diffDataChunk["showDiff"] = true;
                    let j = i - 1, bracketEndStack = [];
                    while (j >= 0) {
                        if(bracketEndStack.length === 0 && diffData[j].jsonPath.indexOf(BEGIN_BRACKET) > -1) {
                            diffData[j]["hasDiff"] = true;
                            diffDataChunk["showDiff"] = true;
                        }
                        if(bracketEndStack.length > 0 && diffData[j].jsonPath.indexOf(BEGIN_BRACKET) > -1) {
                            bracketEndStack.pop();
                        }
                        if(diffData[j].jsonPath.indexOf(END_BRACKET) > -1) {
                            bracketEndStack.push(END_BRACKET);
                        }
                        j--;
                    }
                }
            }
        }
        for (let k = 0; k < diffData.length; k++) {
            if(!diffData[k].jsonPath) continue;
            if(diffData[k].hasDiff && diffData[k].jsonPath.indexOf(BEGIN_BRACKET) > -1) {
                let m = k + 1, bracketBeginStack = [];
                while (diffData[k].jsonPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, "") !== diffData[m].jsonPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, "")) {
                    
                    if(bracketBeginStack.length === 0 && diffData[m].jsonPath.indexOf(BEGIN_BRACKET) < 0) {
                        diffData[m]["hasDiff"] = true;
                        diffData[m]["showDiff"] = true;
                    }

                    if(bracketBeginStack.length === 0 && diffData[m].jsonPath.indexOf(BEGIN_BRACKET) > 0 && !diffData[m].hasDiff) {
                        diffData[m]["showDiff"] = true;
                    }
                    
                    if(diffData[m].jsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        bracketBeginStack.push(BEGIN_BRACKET);
                    }

                    if(bracketBeginStack.length > 0 && diffData[m].jsonPath.indexOf(END_BRACKET) > -1) {
                        bracketBeginStack.pop();
                    }

                    m++;
                }
                diffData[m]["hasDiff"] = true;
            }
        }
        return diffData;
    }

    validateAndCreateDiffLayoutData(replayList) {
        let diffLayoutData = replayList.map((item, index) => {
            let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders, prefix = "/body",
                recordedRequestHeaders, replayedRequestHeaders, recordedRequestQParams, replayedRequestQParams, recordedRequestFParams, replayedRequestFParams,recordedRequestBody, replayedRequestBody, reductedDiffArrayReqHeaders, reductedDiffArrayReqBody, reductedDiffArrayReqQParams, reductedDiffArrayReqFParams;
            let isJson = true;
            // processing Response    
            // recorded response body and headers
            if (item.recordResponse) {
                recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
                // check if the content type is JSON and attempt to parse it
                let recordedResponseMime = recordedResponseHeaders["content-type"] ? recordedResponseHeaders["content-type"][0] : "";
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
                missedRequiredFields = diff.filter(
                    (eachItem) => (
                        eachItem.op === "noop" 
                        && eachItem.resolution.includes("ERR_Required") 
                        && !expJSONPaths.has(eachItem.path)
                        )
                    )
                
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

            let updatedReductedDiffArrayWithCollapsible = this.addCompressToggleData(updatedReductedDiffArray, this.state.collapseLength);

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
                reductedDiffArrayReqBody
            }
        });
        return diffLayoutData;
    }

    */

    generateJsonPathList(resolutionType, diffLayoutDataFiltered) {
        // for each response body
        return  diffLayoutDataFiltered.flatMap((v) => {
            // for each diff in that body
            return v.parsedDiff.map((diff) => {
                // if the resolution matches the required one, get its json path
                if(diff.resolution === resolutionType) {
                    return diff.path;
                }
            })
        })
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
        let { selectedAPI, selectedResolutionType, selectedService, currentPageNumber, fetchedResults, selectedReqRespMatchType, replayId, app, service, apiPath, collapseLength, incrementCollapseLengthForRecReqId, incrementCollapseLengthForRepReqId } = this.state;
        let apiPaths = [], services = [], resolutionTypes = [];
        let apiPathIndicators = {};
        const {cube, history} = this.props;
        this.layoutDataWithDiff.forEach(eachDiffItem => {
            if (incrementCollapseLengthForRepReqId && eachDiffItem.replayReqId === incrementCollapseLengthForRepReqId) {
                addCompressToggleData(eachDiffItem.reductedDiffArray, collapseLength);
            }
        });
        let diffLayoutDataFiltered = this.layoutDataWithDiff.filter(function (eachItem) {
            services.push({value: eachItem.service, count: 0});
            if (selectedService === "All" || selectedService === eachItem.service) {
                eachItem.show = true;
            }
            else {
                eachItem.show = false;
            }
            return eachItem.show === true;
        }).filter(function (eachItem) {
            if (eachItem.reqCompResType === "NoMatch" || eachItem.respCompResType === "NoMatch") {
                apiPathIndicators[eachItem.path] = true;
                if (!selectedAPI) {
                    // set a default selected API path
                    selectedAPI = eachItem.path
                }
            }
            
            apiPaths.push({value: eachItem.path, count: 0});

            if (eachItem.show === true && (selectedAPI === "All" || selectedAPI === eachItem.path)) {
                
            }
            else {
                eachItem.show = false;
            }
            return eachItem.show === true;
        }).filter(function (eachItem) {
            if (eachItem.show === true) {
                if (selectedReqRespMatchType === "All") {
                    // do nothing
                } else if (selectedReqRespMatchType === "requestMismatch") {
                    // hide non-mismatch
                    if (eachItem.reqCompResType !== "NoMatch") {
                        eachItem.show = false;
                    }
                } else if (selectedReqRespMatchType === "responseMismatch") {
                    // hide non-mismatch
                    if (eachItem.respCompResType !== "NoMatch") {
                        eachItem.show = false;
                    }
                }
            }

            return eachItem.show === true;
        }).filter(function (eachItem) {
            eachItem.filterPaths = [];
            let toFilter = false;
            if (eachItem.show === true) {
                for (let eachJsonPathParsedDiff of eachItem.parsedDiff) {
                    // add non error types to resolutionTypes list
                    resolutionTypes.push({value: eachJsonPathParsedDiff.resolution, count: 0});

                    // add path to the filter list if the resolution is All or matches the current selected one,
                    // and if the selected type is 'All Errors' it is an error type
                    if (selectedResolutionType === "All"
                    || selectedResolutionType === eachJsonPathParsedDiff.resolution
                    || (selectedResolutionType === "ERR" && eachJsonPathParsedDiff.resolution.indexOf("ERR_") > -1)) {
                        // add only the json paths we want to show in the diff
                        let path = eachJsonPathParsedDiff.path;
                        eachItem.filterPaths.push(path);
                        toFilter = true;
                    }
                }
            }

            if(eachItem.parsedDiff && eachItem.parsedDiff.length === 0) {
                toFilter = true;
            }

            if (!toFilter) {
                eachItem.show = false;
            }

            return eachItem.show === true;
        });

        if (!selectedAPI) {
            // if after the filters, still the selected API is empty, set to All
            selectedAPI = "All"
        }

        this.historySearchParams = updateSearchHistoryParams("selectedAPI", selectedAPI, this.state);
        
        let pagedDiffLayoutData = [];
        this.pages = Math.ceil(diffLayoutDataFiltered.length / this.pageSize);

        let accumulatedObjectSize = 0;
        let startIndex = 0;

        this.pageNumberVsDataIndex = [];
        for (let i = 0; i < diffLayoutDataFiltered.length; i++) {
            if (diffLayoutDataFiltered) {
                let oneAPIInstanceDiffSize = roughSizeOfObject(diffLayoutDataFiltered[i]);
                accumulatedObjectSize = accumulatedObjectSize + oneAPIInstanceDiffSize;
                if (accumulatedObjectSize > 1000000) {
                    let pgVsIndexMap = {
                        startIndex: startIndex,
                        endIndex: i
                    };
                    this.pageNumberVsDataIndex.push(pgVsIndexMap);
                    startIndex =  i  + 1;
                    accumulatedObjectSize=0;
                }
            }
        }

        if (diffLayoutDataFiltered && diffLayoutDataFiltered.length > 0 && this.pageNumberVsDataIndex.length ==0) {
            let pgVsIndexMap = {
                startIndex: 0,
                endIndex: diffLayoutDataFiltered.length-1
            };
            this.pageNumberVsDataIndex.push(pgVsIndexMap);
        }

        this.pages = this.pageNumberVsDataIndex.length;

        if(fetchedResults > 0 && this.pages > 0 && diffLayoutDataFiltered.length > 0) {
            // let startCount = (currentPageNumber - 1 ) * (this.pageSize);
            //
            // for(let i = startCount; i < this.pageSize + startCount; i++) {
            //     diffLayoutDataFiltered[i] && pagedDiffLayoutData.push(diffLayoutDataFiltered[i]);
            //     }
            // }
            for (let i = this.pageNumberVsDataIndex[currentPageNumber-1].startIndex; i <= this.pageNumberVsDataIndex[currentPageNumber-1].endIndex; i++) {
                diffLayoutDataFiltered[i] && pagedDiffLayoutData.push(diffLayoutDataFiltered[i]);
            }
        }

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
        
        services = services.filter(filterFunction);
        apiPaths = apiPaths.filter(filterFunction);
        resolutionTypes = resolutionTypes.filter(filterFunction);
        
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
        let serviceMenuItems = services.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedService", item.value)}>
                <Glyphicon style={{ visibility: selectedService === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
        let apiPathMenuItems = apiPaths.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedAPI", item.value)}>
                <Glyphicon style={{ visibility: apiPathIndicators[item.value] ? "visible" : "hidden", color: "red"}} glyph="alert" /> 
                <Glyphicon style={{ visibility: selectedAPI === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });

        let resTypeMenuJsx = (item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResolutionType", item.value)}>
                <Glyphicon style={{ visibility: selectedResolutionType === item.value ? "visible" : "hidden" }} glyph="ok" /> {resolutionsIconMap[item.value].description} ({item.count})
            </MenuItem>);
        }
        
        let resolutionTypeErrorMenuItems 
                    = resolutionTypes.filter((item) => {
                        return item.value.indexOf("ERR_") > -1;
                    })
                    .map(resTypeMenuJsx);
        
        let resolutionTypeOtherMenuItems 
                    = resolutionTypes.filter((item) => {
                        return item.value.indexOf("ERR_") == -1;
                    })
                    .map(resTypeMenuJsx);
        
        let pageButtons = [];
        for(let idx = 1; idx <= this.pages; idx++) {
            pageButtons.push(
                <Button key={idx} onClick={this.changePageNumber} bsStyle={currentPageNumber === idx ? "primary" : "default"} style={{}}>{idx}</Button>
            );
        }
        let jsxContent = pagedDiffLayoutData.map((item, index) => {
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
                                showAll={this.state.showAll}
                                searchFilterPath={this.state.searchFilterPath}
                                filterPaths={item.filterPaths}
                                inputElementRef={this.inputElementRef}
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
                        {
                            item.missedRequiredFields.length > 0 &&
                            <div style={{ padding: "10px 0" }}>
                                <strong>Missing expected items in Test and Golden:</strong>
                                {
                                    item.missedRequiredFields.map(
                                        (eachMissedField) => 
                                            (
                                                <div style={{ padding: "3px 0"}}>
                                                    <Glyphicon style={{ color: "red" }} glyph="remove-circle" />
                                                    <span style={{ padding: "3px 0" }}>{eachMissedField.path}</span>
                                                    {eachMissedField.fromValue && <span>` : ${eachMissedField.fromValue}`</span>}
                                                </div>
                                            )
                                    )
                                }
                            </div>
                        }
                        
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
                                    handleCollapseLength={this.increaseCollapseLength}
                                />
                            </div>
                        )}
                    </div>
                )}
            </div >);
        });

        let getResolutionTypeDescription = function (resolutionType) {
            switch (resolutionType) {
                case "All":
                    return "All"
                
                case "ERR":
                    return "All Errors"
                
                default:
                    return resolutionsIconMap[resolutionType].description;
            }
        }

        return (
            <ShareableLinkContext.Provider 
                value={{ 
                    popoverCurrentPath: this.state.popoverCurrentPath, 
                    setPopoverCurrentPath: this.handleCurrentPopoverPathChange 
                }}>
                <div className="content-wrapper">
                <div className="back" style={{ marginBottom: "10px", padding: "5px", background: "#454545" }}>
                    <Link to={"/"} onClick={this.handleBackToDashboardClick}><span className="link-alt"><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO DASHBOARD</span></Link>
                    <span className="link-alt pull-right" onClick={this.showSaveGoldenModal}>&nbsp;&nbsp;&nbsp;&nbsp;<i className="fas fa-save font-15"></i>&nbsp;Save Golden</span>
                    <Link to="/review_golden_updates" className="hidden">
                        <span className="link pull-right"><i className="fas fa-pen-square font-15"></i>&nbsp;REVIEW GOLDEN UPDATES</span>
                    </Link>
                </div>
                <div>
                    <Breadcrumb style={{}}>
                        <Breadcrumb.Item href="/">{this.state.app}</Breadcrumb.Item>
                        <Breadcrumb.Item href="javascript:void(0);">
                            <strong>Service:&nbsp;</strong>
                            <DropdownButton title={selectedService} id="dropdown-size-medium">
                                <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedService", "All")}>
                                    <Glyphicon style={{ visibility: selectedService === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({services.reduce((accumulator, item) => accumulator += item.count, 0)})
                                </MenuItem>
                                <MenuItem divider />
                                {serviceMenuItems}
                            </DropdownButton>
                        </Breadcrumb.Item>
                        <Breadcrumb.Item active>
                            <strong>API Path:&nbsp;</strong>
                            <DropdownButton title={selectedAPI ? selectedAPI : "Select API Path"} id="dropdown-size-medium">
                                <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedAPI", "All")}>
                                    <Glyphicon style={{ visibility: selectedAPI === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({apiPaths.reduce((accumulator, item) => accumulator += item.count, 0)})
                                </MenuItem>
                                <MenuItem divider />
                                {apiPathMenuItems}
                            </DropdownButton>
                        </Breadcrumb.Item>
                    </Breadcrumb>
                    <div style={{ marginBottom: "18px" }}>
                        <Radio inline value="responseMismatch" checked={this.state.selectedReqRespMatchType === "responseMismatch"} onChange={this.handleReqRespMtChange}> Response Mismatches only </Radio>
                        <Radio inline value="requestMismatch" checked={this.state.selectedReqRespMatchType === "requestMismatch"} onChange={this.handleReqRespMtChange}> Request Mismatches only </Radio>
                        <Radio inline value="All" checked={this.state.selectedReqRespMatchType === "All"} onChange={this.handleReqRespMtChange}> All </Radio>
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
                            <label class="checkbox-inline">
                                Resolution Type:
                            </label>
                            <div style={{ paddingLeft: "9px", display: "inline-block" }}>
                                <DropdownButton title={getResolutionTypeDescription(selectedResolutionType)} id="dropdown-size-medium">
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "All")}>
                                        <Glyphicon style={{ visibility: selectedResolutionType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({resolutionTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    <MenuItem divider />
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "ERR")}>
                                        <Glyphicon style={{ visibility: selectedResolutionType === "ERR" ? "visible" : "hidden" }} glyph="ok" /> All Errors ({resolutionTypes.filter((r) => {return r.value.indexOf("ERR_") > -1}).reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    {resolutionTypeErrorMenuItems}
                                    <MenuItem divider />
                                    {resolutionTypeOtherMenuItems}
                                </DropdownButton>
                            </div>
                        </div>
                        <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.searchFilterPath}
                            placeholder="Search"
                            onChange={this.handleSearchFilterChange}
                            id="filterPathInputId"
                            inputRef={ref => { this.input = ref; }}
                        />
                    </FormGroup>
                    {
                        !this.state.isFetching && this.state.fetchComplete && jsxContent.length !== 0 &&
                        (
                            <ButtonGroup style={{marginBottom: "9px", width: "100%"}}>
                                <div style={{textAlign: "left"}}>{pageButtons}</div>
                            </ButtonGroup>
                        )
                    }
                </div>
                <div className={(this.state.isFetching || jsxContent.length === 0) ? "loading-text" : ""}>
                    {
                        !this.state.isFetching && this.state.fetchComplete 
                        ? 
                            (
                                () => (jsxContent.length === 0 ? "No Mismatches Found" : (jsxContent))
                            )() 
                        : "Loading..."
                    }
                </div>
                <Modal show={this.state.showNewGolden}>
                    <Modal.Header>
                        <Modal.Title>{!cube.newGoldenId ? "Saving Golden" : "Golden Saved"}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <p className={cube.newGoldenId ? "" : "hidden"}>Name: {this.state.nameG}</p>
                        <p className={cube.newGoldenId ? "hidden" : ""}>Updating Operations...</p>
                    </Modal.Body>
                    <Modal.Footer className={cube.newGoldenId ? "" : "hidden"}>
                        <div>
                            <span onClick={this.handleClose} className="cube-btn">Go TO Test Config</span>&nbsp;&nbsp;
                            <span onClick={this.handleCloseDone} className="cube-btn">Done</span>
                        </div>
                    </Modal.Footer>
                </Modal>

                <Modal show={this.state.showSaveGoldenModal}>
                    <Modal.Header>
                        <Modal.Title>Application:&nbsp;{cube.selectedApp}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div style={{padding: "15px 25px"}}>
                            <div className={this.state.saveGoldenError ? "error-div" : "hidden"}>
                                <h5 style={{marginTop: 0}}>
                                    <i className="fas fa-warning"></i>&nbsp;Error!
                                </h5>
                                {this.state.saveGoldenError}
                            </div>

                            <div className="row margin-bottom-10">
                                <div className="col-md-3 bold">
                                    Name*:
                                </div>

                                <div className="col-md-9">
                                    <input required placeholder="Enter Golden Name" onChange={(event) => this.changeGoldenMetaData('nameG', event)} value={this.state.nameG} type="text" className="width-100"/>
                                </div>
                            </div>

                            <div className="row margin-bottom-10">
                                <div className="col-md-3 bold">
                                    Branch:
                                </div>

                                <div className="col-md-9">
                                    <input placeholder="Enter Branch Name" onChange={(event) => this.changeGoldenMetaData('branch', event)} value={this.state.branch} type="text" className="width-100"/>
                                </div>
                            </div>

                            <div className="row margin-bottom-10">
                                <div className="col-md-3 bold">
                                    Version:
                                </div>

                                <div className="col-md-9">
                                    <input placeholder="Enter Code Version" onChange={(event) => this.changeGoldenMetaData('version', event)} value={this.state.version} type="text" className="width-100"/>
                                </div>
                            </div>

                            <div className="row margin-bottom-10">
                                <div className="col-md-3 bold">
                                    Commit ID:
                                </div>

                                <div className="col-md-9">
                                    <input placeholder="Enter Git Commit ID" onChange={(event) => this.changeGoldenMetaData('commitId', event)} value={this.state.commitId} type="text" className="width-100"/>
                                </div>
                            </div>

                            <div className="row margin-bottom-10">
                                <div className="col-md-3 bold">
                                    Tags:
                                </div>

                                <div className="col-md-9">
                                    <input placeholder="Enter Tags(Comma Separated)" onChange={(event) => this.changeGoldenMetaData('tag', event)} value={this.state.tag} type="text" className="width-100"/>
                                </div>
                            </div>
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                        <div>
                            <span onClick={this.handleCloseSG} className="cube-btn">CANCEL</span>&nbsp;&nbsp;
                            <span onClick={this.handleSaveGolden} className="cube-btn">SAVE</span>
                        </div>
                    </Modal.Footer>
                </Modal>
            </div>
            </ShareableLinkContext.Provider>
        );
    }

    changeGoldenMetaData = (meta, ev) => {
        this.setState({[meta]: ev.target.value});
    };

    showSaveGoldenModal = () => {
        this.setState({
            nameG: (this.state.recordingId + '_' + Date.now()),
            branch: "",
            version: "",
            tag: "",
            commitId: "",
            saveGoldenError: "",
            showSaveGoldenModal: true
        });
    };

    handleCloseSG = () => {
        this.setState({showSaveGoldenModal: false, saveGoldenError: ""});
    };

    handleSaveGolden = () => {
        if (!this.state.nameG.trim()) {
            this.setState({saveGoldenError: "Name is a Required Field, cannot be Empty.",})
        } else {
            this.updateGolden();
        }
    };

    updateGolden = () => {
        const { cube, dispatch } = this.props;

        let user = JSON.parse(localStorage.getItem('user'));

        const headers = {
            "Content-Type": "application/json",
            'Access-Control-Allow-Origin': '*',
            "Authorization": "Bearer " + user['access_token']
        };

        const updateTemplateOperationSet = axios({
            method: 'post',
            url: `${config.analyzeBaseUrl}/updateTemplateOperationSet/${cube.newTemplateVerInfo['ID']}`,
            data: cube.templateOperationSetObject,
            headers: headers
        });
        
        const goldenUpdate = axios({
            method: 'post',
            url: `${config.analyzeBaseUrl}/goldenUpdate/recordingOperationSet/updateMultiPath`,
            data: cube.multiOperationsSet,
            headers: headers
        });
        const _self = this;
        axios.all([updateTemplateOperationSet, goldenUpdate]).then(axios.spread(function (r1, r2) {
            dispatch(cubeActions.updateRecordingOperationSet());
            _self.updateGoldenSet();
            // dispatch(cubeActions.updateGoldenSet(_self.state.nameG, _self.state.replayId, cube.collectionUpdateOperationSetId.operationSetId, cube.newTemplateVerInfo['ID'], _self.state.recordingId, _self.state.app));
        }));
    };

    updateGoldenSet = () => {
        const {cube, dispatch} = this.props;
        const user = JSON.parse(localStorage.getItem('user'));

        const url = `${config.analyzeBaseUrl}/updateGoldenSet/${this.state.recordingId}/${this.state.replayId}/${cube.collectionUpdateOperationSetId.operationSetId}/${cube.newTemplateVerInfo['ID']}`;
        const headers = {
            'Access-Control-Allow-Origin': '*',
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": "Bearer " + user['access_token']
        };

        let searchParams = new URLSearchParams();
        searchParams.set('name', this.state.nameG);
        searchParams.set('userId', user.username);

        if (this.state.version.trim()) {
            searchParams.set('codeVersion', this.state.version.trim());
        }

        if (this.state.branch.trim()) {
            searchParams.set('branch', this.state.branch.trim());
        }

        if (this.state.commitId.trim()) {
            searchParams.set('gitCommitId', this.state.commitId.trim());
        }

        if (this.state.tag.trim()) {
            let tagList = this.state.tag.split(",");
            for (let tag of tagList) {
                tag = tag.trim();
            }
            searchParams.set('tags', JSON.stringify(tagList));
        }


        axios.post(url, searchParams, {headers: headers})
            .then((result) => {
                this.setState({showSaveGoldenModal: false, saveGoldenError: ""});
                dispatch(cubeActions.updateGoldenSet(result.data));
                dispatch(cubeActions.getTestIds(this.state.app));
            })
            .catch((err) => {
                dispatch(cubeActions.clearGolden());
                this.setState({saveGoldenError: err.response.data["Error"]});
            })
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

function roughSizeOfObject( object ) {

    var objectList = [];
    var stack = [ object ];
    var bytes = 0;

    while ( stack.length ) {
        var value = stack.pop();

        if ( typeof value === 'boolean' ) {
            bytes += 4;
        }
        else if ( typeof value === 'string' ) {
            bytes += value.length * 2;
        }
        else if ( typeof value === 'number' ) {
            bytes += 8;
        }
        else if
        (
            typeof value === 'object'
            && objectList.indexOf( value ) === -1
        )
        {
            objectList.push( value );

            for( var i in value ) {
                stack.push( value[ i ] );
            }
        }
    }
    return bytes;
}

const connectedShareableLink = connect(mapStateToProps)(ShareableLink);

export default connectedShareableLink;
export { connectedShareableLink as ShareableLink, ShareableLinkContext };
