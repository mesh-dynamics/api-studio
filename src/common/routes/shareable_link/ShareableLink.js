import React, { Component } from 'react';
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';
import _ from 'lodash';
import axios from "axios";
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
import statusCodeList from "../../StatusCodeList"

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
            showRequestMessageParams: false,
            shownRequestMessageParams: false,
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
        };
        this.handleSearchFilterChange = this.handleSearchFilterChange.bind(this);
        this.handleReqRespMtChange = this.handleReqRespMtChange.bind(this)
        this.toggleMessageContents = this.toggleMessageContents.bind(this);
        this.changePageNumber = this.changePageNumber.bind(this);

        this.inputElementRef = React.createRef();
        this.pageSize = 5;
        this.pages = 0;
        this.layoutDataWithDiff = [];
        this.getSearchHistoryParams = getSearchHistoryParams();
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

    changePageNumber(e) {
        this.setState({ currentPageNumber: +e.target.innerHTML.trim()});
    }

    toggleMessageContents(e) {
        const { history } = this.props;

        if (e.target.value === "responseHeaders") this.setState({ showResponseMessageHeaders: e.target.checked, shownResponseMessageHeaders: true });
        if (e.target.value === "responseBody") this.setState({ showResponseMessageBody: e.target.checked, shownResponseMessageBody: true });
        if (e.target.value === "requestHeaders") this.setState({ showRequestMessageHeaders: e.target.checked, shownRequestMessageHeaders: true });
        if (e.target.value === "requestParams") this.setState({ showRequestMessageParams: e.target.checked, shownRequestMessageParams: true });
        if (e.target.value === "requestBody") this.setState({ showRequestMessageBody: e.target.checked, shownRequestMessageBody: true });

        setTimeout(() => {
            const { showResponseMessageHeaders, showResponseMessageBody, showRequestMessageHeaders, showRequestMessageParams, showRequestMessageBody } = this.state;

            if(showResponseMessageHeaders === false && showResponseMessageBody === false && showRequestMessageHeaders === false &&  showRequestMessageParams === false && showRequestMessageBody === false) {
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
        const {apiPath, replayId} = this.state;
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
                let diffLayoutData = this.validateAndCreateDiffLayoutData(dataList.data.res);
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
                        let eachDiffLayoutData = this.validateAndCreateDiffLayoutData(eachResponse.data.data.res);
                        this.layoutDataWithDiff.push(...eachDiffLayoutData);
                    });
                    this.setState({
                        fetchComplete: true
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
            let isJson = true;
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
            if (item.diff) {
                diff = item.diff;
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
        let { selectedAPI, selectedResolutionType, selectedService, currentPageNumber, fetchedResults, selectedReqRespMatchType} = this.state;
        let apiPaths = [], services = [], resolutionTypes = [];
        let apiPathIndicators = {};
        const {cube, history} = this.props;
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
            if (eachItem.reqmt === "NoMatch" || eachItem.respmt === "NoMatch") {
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
                    if (eachItem.reqmt !== "NoMatch") {
                        eachItem.show = false;
                    }
                } else if (selectedReqRespMatchType === "responseMismatch") {
                    // hide non-mismatch
                    if (eachItem.respmt !== "NoMatch") {
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
        if(fetchedResults > 0 && this.pages > 0 && diffLayoutDataFiltered.length > 0) {
            let startCount = (currentPageNumber - 1 ) * (this.pageSize);
            for(let i = startCount; i < this.pageSize + startCount; i++) {
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
                <Button onClick={this.changePageNumber} bsStyle={currentPageNumber === idx ? "primary" : "default"} style={{}}>{idx}</Button>
            );
        }
        let jsxContent = pagedDiffLayoutData.map((item, index) => {
            return (<div key={item.recordReqId + "_" + index} style={{ borderBottom: "1px solid #eee", display: "block" }}>
                <div style={{ backgroundColor: "#EAEAEA", paddingTop: "18px", paddingBottom: "18px", paddingLeft: "10px" }}>
                    {item.path}
                </div>
                {(this.state.showRequestMessageHeaders || this.state.shownRequestMessageHeaders) && item.recordedRequestHeaders && item.replayedRequestHeaders && (
                    <div style={{ display: this.state.showRequestMessageHeaders ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Headers</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedRequestHeaders, undefined, 4)}
                                newValue={JSON.stringify(item.replayedRequestHeaders, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={null}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {(this.state.showRequestMessageParams || this.state.shownRequestMessageParams) && item.recordedRequestParams && item.replayedRequestParams && (
                    <div style={{ display: this.state.showRequestMessageParams ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Params</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedRequestParams, undefined, 4)}
                                newValue={JSON.stringify(item.replayedRequestParams, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={null}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {(this.state.showRequestMessageBody || this.state.shownRequestMessageBody) && item.recordedRequestBody && item.replayedRequestBody && (
                    <div style={{ display: this.state.showRequestMessageBody ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Body (Includes Form Params)</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedRequestBody, undefined, 4)}
                                newValue={JSON.stringify(item.replayedRequestBody, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={null}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {(this.state.showResponseMessageHeaders || this.state.shownResponseMessageHeaders) && item.recordedResponseHeaders && item.replayedResponseHeaders && (
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
                                    diffArray={null}
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
            <div className="content-wrapper">
                <div className="back" style={{ marginBottom: "10px", padding: "5px", background: "#454545" }}>
                    <Link to={"/"} onClick={this.handleBackToDashboardClick}><span className="link"><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO DASHBOARD</span></Link>
                    <span className="link pull-right" onClick={this.showSaveGoldenModal}>&nbsp;&nbsp;&nbsp;&nbsp;<i className="fas fa-save font-15"></i>&nbsp;Save Golden</span>
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
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestParams" checked={this.state.showRequestMessageParams}>Request Params</Checkbox>
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
                    <ButtonGroup style={{marginBottom: "9px", width: "100%"}}>
                        <div style={{textAlign: "left"}}>{pageButtons}</div>
                    </ButtonGroup>
                </div>
                <div>
                    {jsxContent}
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

const connectedShareableLink = connect(mapStateToProps)(ShareableLink);

export default connectedShareableLink;
