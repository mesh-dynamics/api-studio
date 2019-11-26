import React, { Component } from 'react';
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button } from 'react-bootstrap';
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
            filterPath: '',
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
            selectedDiffOperationType: "All",
            showNewGolden: false,
            app: "",
            templateVersion: "",
            newTemplateVerInfo: null,
            golden: null,
            apiPath: "",
            service: "",
            replayId: null,
            recordingId: null,
            showOnlyFailures: false,
            showOnlyMarkedForGolden: false,
            currentPageNumber: 1,
            fetchComplete: false,
            fetchedResults: 0
        };
        this.handleChange = this.handleChange.bind(this);
        this.toggleMessageContents = this.toggleMessageContents.bind(this);
        this.toggleShowOnlyFailures = this.toggleShowOnlyFailures.bind(this);
        this.toggleShowOnlyMarkedForUpdate = this.toggleShowOnlyMarkedForUpdate.bind(this);
        this.changePageNumber = this.changePageNumber.bind(this);

        this.inputElementRef = React.createRef();
        this.pageSize = 5;
        this.pages = 0;
        this.layoutDataWithDiff = []
    }

    componentDidMount() {
        const {dispatch} = this.props;
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();
        const apiPath = urlParameters["apiPath"] ? urlParameters["apiPath"]  : "%2A",
            replayId = urlParameters["replayId"],
            app = urlParameters["app"],
            recordingId = urlParameters["recordingId"],
            currentTemplateVer = urlParameters["currentTemplateVer"],
            service = urlParameters["service"];

        dispatch(cubeActions.setSelectedApp(app));
        this.setState({
            apiPath: apiPath,
            replayId: replayId,
            service: service,
            recordingId: recordingId,
            currentTemplateVer: currentTemplateVer,
            app: app,
            selectedAPI: urlParameters["apiPath"] ? urlParameters["apiPath"] : "All",
            selectedService: urlParameters["service"] ? urlParameters["service"] : "All",
        });
        setTimeout(() => {
            const { dispatch, history, cube } = this.props;
            dispatch(cubeActions.setPathResultsParams({
                path: apiPath,
                service: service,
                replayId: replayId,
                recordingId: recordingId,
                currentTemplateVer: currentTemplateVer
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

    handleChange(e) {
        this.setState({ filterPath: e.target.value });
    }

    changePageNumber(e) {
        this.setState({ currentPageNumber: +e.target.innerHTML.trim()});
    }

    toggleShowOnlyFailures(e) {
        this.setState({showOnlyFailures : e.target.checked});
    }

    toggleShowOnlyMarkedForUpdate(e) {
        this.setState({showOnlyMarkedForGolden : e.target.checked});
    }

    toggleMessageContents(e) {

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
    }

    handleMetaDataSelect(metaDataType, value) {
        if (metaDataType == "selectedAPI") {
            const {dispatch} = this.props;
            this.setState({apiPath: value, [metaDataType] : value});
            setTimeout(() => {
                dispatch(cubeActions.setPathResultsParams({
                    path: value,
                    service: this.state.service,
                    replayId: this.state.replayId,
                    recordingId: this.state.recordingId,
                    currentTemplateVer: this.state.currentTemplateVer
                }));
            });
        } else if (metaDataType == "selectedService") {
            this.setState({service: value, [metaDataType] : value, selectedAPI: ""});
        } else {
            this.setState({[metaDataType] : value});
        }
    }

    async fetchReplayList() {
        const {apiPath, replayId} = this.state;
        if(!replayId) throw new Error("replayId is required");
        let response, json;
        let user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}?start=0&includediff=true&path=%2A`;
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
                    url = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}?start=${fetchedResults}&includediff=true&path=%2A`;
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
            if (item.recordResponse) {
                recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
                if (item.recordResponse.body) {
                    try {
                        if (item.recordResponse.mimeType.indexOf('json') > -1 && item.replayResponse.mimeType.indexOf('json') > -1) {
                            recordedData = JSON.parse(item.recordResponse.body);
                        }
                        else recordedData = item.recordResponse.body;
                    } catch (e) {
                        recordedData = JSON.parse('"' + cleanEscapedString(_.escape(item.recordResponse.body)) + '"')
                    }
                }
                else {
                    recordedData = JSON.parse('""');
                }
            } else {
                recordedResponseHeaders = null;
                recordedData = null;
            }
            if (item.replayResponse) {
                replayedResponseHeaders = item.replayResponse.hdrs ? item.replayResponse.hdrs : [];
                if (item.replayResponse.body) {
                    try {
                        if (item.recordResponse.mimeType.indexOf('json') > -1 && item.replayResponse.mimeType.indexOf('json') > -1) {
                            replayedData = JSON.parse(item.replayResponse.body);
                        }
                        else replayedData = item.replayResponse.body;
                    } catch (e) {
                        replayedData = JSON.parse('"' + cleanEscapedString(_.escape(item.replayResponse.body)) + '"')
                    }
                }
                else {
                    replayedData = JSON.parse('""');
                }
            } else {
                replayedResponseHeaders = null;
                replayedData = null;
            }
            let diff;
            if (item.diff) {
                try {
                    diff = JSON.parse(item.diff);
                } catch (e) {
                    diff = JSON.parse('"' + item.diff + '"')
                }
            }
            else diff = [];
            let actJSON = JSON.stringify(replayedData, undefined, 4),
                expJSON = JSON.stringify(recordedData, undefined, 4);
            let reductedDiffArray = null, missedRequiredFields = [];
            if (diff && diff.length > 0) {
                let reduceDiff = new ReduceDiff(prefix, actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
                let expJSONPaths = generator(recordedData, "", "", prefix);
                missedRequiredFields = diff.filter((eachItem) => {
                    return eachItem.op === "noop" && eachItem.resolution.indexOf("ERR_REQUIRED") > -1 && !expJSONPaths.has(eachItem.path);
                })
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
            if (item.recordRequest) {
                recordedRequestHeaders = item.recordRequest.hdrs ? item.recordRequest.hdrs : {};
                recordedRequestParams = item.recordRequest.queryParams ? item.recordRequest.queryParams : {};
                if (item.recordRequest.body) {
                    try {
                        if (item.recordRequest.mimeType.indexOf('json') > -1 && item.recordRequest.mimeType.indexOf('json') > -1) {
                            recordedRequestBody = JSON.parse(item.recordRequest.body);
                        }
                        else recordedRequestBody = item.recordRequest.body;
                    } catch (e) {
                        recordedRequestBody = JSON.parse('"' + cleanEscapedString(_.escape(item.recordRequest.body)) + '"')
                    }
                }
                else {
                    recordedRequestBody = JSON.parse('""');
                }
            } else {
                recordedRequestHeaders = null;
                recordedRequestBody = null;
                recordedRequestParams = null;
            }
            if (item.replayRequest) {
                replayedRequestHeaders = item.replayRequest.hdrs ? item.replayRequest.hdrs : {};
                replayedRequestParams = item.replayRequest.queryParams ? item.replayRequest.queryParams : {};
                if (item.replayRequest.body) {
                    try {
                        if (item.replayRequest.mimeType.indexOf('json') > -1 && item.replayRequest.mimeType.indexOf('json') > -1) {
                            replayedRequestBody = JSON.parse(item.replayRequest.body);
                        }
                        else replayedRequestBody = item.replayRequest.body;
                    } catch (e) {
                        replayedRequestBody = JSON.parse('"' + cleanEscapedString(_.escape(item.replayRequest.body)) + '"')
                    }
                }
                else {
                    replayedRequestBody = JSON.parse('""');
                }
            } else {
                replayedRequestHeaders = null;
                replayedRequestBody = null;
                replayedRequestParams = null;
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
                replayedRequestBody
            }
        });
        return diffLayoutData;
    }

    render() {
        let { selectedAPI, selectedRequestMatchType, selectedResponseMatchType, selectedResolutionType, selectedDiffOperationType, selectedService, showOnlyFailures, currentPageNumber, fetchedResults } = this.state;
        let requestMatchTypes = [], responseMatchTypes = [], apiPaths = [], services = [], resolutionTypes = [], diffOperationTypes = [];
        const {cube} = this.props;
        this.layoutDataWithDiff.filter(function (eachItem) {
            services.push({value: eachItem.service, count: 0});
            if (selectedService === "All" || selectedService === eachItem.service) {
                eachItem.show = true;
            }
            else {
                eachItem.show = false;
            }
            return eachItem.show === true;
        }).filter(function (eachItem) {
            apiPaths.push({value: eachItem.path, count: 0});
            if (eachItem.show === true && (selectedAPI === "All" || selectedAPI === eachItem.path)) {
                
            }
            else {
                eachItem.show = false;
            }
            return eachItem.show === true;
        }).filter(function (eachItem) {
            requestMatchTypes.push({value: eachItem.reqmt, count: 0});
            if (eachItem.show === true && (selectedRequestMatchType === "All" || selectedRequestMatchType === eachItem.reqmt)) {
                
            } else {
                eachItem.show = false;
            }
            return eachItem.show === true;
        }).filter(function (eachItem) {
            responseMatchTypes.push({value: eachItem.respmt, count: 0});
            if (eachItem.show === true && (selectedResponseMatchType === "All" || selectedResponseMatchType === eachItem.respmt)) {
            } else {
                eachItem.show = false;
            }
            return eachItem.show === true;
        }).filter(function (eachItem) {
            let toFilter = false;
            if (eachItem.show === true) {
                for (let eachJsonPathParsedDiff of eachItem.parsedDiff) {
                    resolutionTypes.push({value: eachJsonPathParsedDiff.resolution, count: 0});
                    if (selectedResolutionType === "All" || selectedResolutionType === eachJsonPathParsedDiff.resolution) {
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
        }).filter(function (eachItem) {
            let toFilter = false;
            if (eachItem.show === true) {
                for (let eachJsonPathParsedDiff of eachItem.parsedDiff) {
                    diffOperationTypes.push({value: eachJsonPathParsedDiff.op, count: 0});
                    if (selectedDiffOperationType === "All" || selectedDiffOperationType === eachJsonPathParsedDiff.op) {
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
        let diffLayoutDataFiltered = this.layoutDataWithDiff.filter(function(eachItem) {
            return eachItem.show === true;
        });
        let pagedDiffLayoutData = [];
        this.pages = Math.ceil(diffLayoutDataFiltered.length / this.pageSize);
        if(fetchedResults > 0 && this.pages > 0 && diffLayoutDataFiltered.length > 0) {
            let startCount = (currentPageNumber - 1 ) * (this.pageSize);
            for(let i = startCount; i < this.pageSize + startCount; i++) {
                diffLayoutDataFiltered[i] && pagedDiffLayoutData.push(diffLayoutDataFiltered[i]);
            }
        }
        requestMatchTypes = requestMatchTypes.filter(filterFunction);
        responseMatchTypes = responseMatchTypes.filter(filterFunction);
        services = services.filter(filterFunction);
        apiPaths = apiPaths.filter(filterFunction);
        resolutionTypes = resolutionTypes.filter(filterFunction);
        diffOperationTypes = diffOperationTypes.filter(filterFunction);
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
                <Glyphicon style={{ visibility: selectedAPI === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
        let requestMatchTypeMenuItems = requestMatchTypes.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedRequestMatchType", item.value)}>
                <Glyphicon style={{ visibility: selectedRequestMatchType === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
        let responseMatchTypeMenuItems = responseMatchTypes.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResponseMatchType", item.value)}>
                <Glyphicon style={{ visibility: selectedResponseMatchType === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
        let resolutionTypeMenuItems = resolutionTypes.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedResolutionType", item.value)}>
                <Glyphicon style={{ visibility: selectedResolutionType === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
        let diffOperationTypeMenuItems = diffOperationTypes.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedDiffOperationType", item.value)}>
                <Glyphicon style={{ visibility: selectedDiffOperationType === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
        let pageButtons = [];
        for(let idx = 1; idx <= this.pages; idx++) {
            pageButtons.push(
                <Button onClick={this.changePageNumber} bsStyle={currentPageNumber === idx ? "primary" : "default"} style={{}}>{idx}</Button>
            );
        }
        let jsxContent = pagedDiffLayoutData.map((item, index) => {
            let toShow = showOnlyFailures ? item.respmt === "NoMatch" ? true : false : item.show;
            return (<div key={item.recordReqId + "_" + index} style={{ borderBottom: "1px solid #eee", display: toShow ? "block" : "none" }}>
                <div style={{ backgroundColor: "#EAEAEA", paddingTop: "18px", paddingBottom: "18px", paddingLeft: "10px" }}>
                    {item.path}
                </div>
                {(this.state.showRequestMessageHeaders || this.state.shownRequestMessageHeaders) && item.recordedRequestHeaders != null && item.replayedRequestHeaders != null && (
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
                {(this.state.showRequestMessageParams || this.state.shownRequestMessageParams) && item.recordedRequestParams != null && item.replayedRequestParams != null && (
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
                {(this.state.showRequestMessageBody || this.state.shownRequestMessageBody) && item.recordedRequestBody != null && item.replayedRequestBody != null && (
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
                {(this.state.showResponseMessageHeaders || this.state.shownResponseMessageHeaders) && item.recordedResponseHeaders != null && item.replayedResponseHeaders != null && (
                    <div style={{ display: this.state.showResponseMessageHeaders ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Headers</Label></h4>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedResponseHeaders, undefined, 4)}
                                newValue={JSON.stringify(item.replayedResponseHeaders, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={null}
                                onLineNumberClick={(lineId, e) => { return; }}
                            />
                        </div>
                    </div>
                )}
                {item.recordedData == null && (
                    <div style={{ margin: "27px", textAlign: "center", fontSize: "24px" }}>No Recorded Data</div>
                )}
                {item.replayedData == null && (
                    <div style={{ margin: "27px", textAlign: "center", fontSize: "24px" }}>No Replayed Data</div>
                )}
                {item.recordedData != null && item.replayedData != null && (
                    <div style={{ display: this.state.showResponseMessageBody ? "" : "none" }}>
                        <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Body</Label></h4>
                        <div>
                            {item.missedRequiredFields.map((eachMissedField) => {
                                return(<div><span style={{paddingRight: "5px"}}>{eachMissedField.path}:</span><span>{eachMissedField.fromValue}</span></div>)
                            })}
                        </div>
                        <div className="diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={item.expJSON}
                                newValue={item.actJSON}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={item.reductedDiffArray}
                                filterPath={this.state.filterPath}
                                onLineNumberClick={(lineId, e) => { return; }}
                                inputElementRef={this.inputElementRef}
                            />
                        </div>
                    </div>
                )}
            </div >);
        });

        return (
            <div className="content-wrapper">
                <div className="back" style={{ marginBottom: "10px", padding: "5px", background: "#454545" }}>
                    <Link to={"/"}><span className="link"><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO DASHBOARD</span></Link>
                    <span className="link pull-right" onClick={this.updateGolden}>&nbsp;&nbsp;&nbsp;&nbsp;<i className="fas fa-check-square font-15"></i>&nbsp;UPDATE OPERATIONS</span>
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
                        <div style={{ display: "inline-block" }}>
                            <div style={{ paddingRight: "9px", display: "inline-block" }}>
                                <DropdownButton title="Request Match Type" id="dropdown-size-medium">
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedRequestMatchType", "All")}>
                                        <Glyphicon style={{ visibility: selectedRequestMatchType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({requestMatchTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    <MenuItem divider />
                                    {requestMatchTypeMenuItems}
                                </DropdownButton>
                            </div>
                            <div style={{ paddingRight: "9px", display: "inline-block" }}>
                                <DropdownButton title="Response Match Type" id="dropdown-size-medium">
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResponseMatchType", "All")}>
                                        <Glyphicon style={{ visibility: selectedResponseMatchType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({responseMatchTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    <MenuItem divider />
                                    {responseMatchTypeMenuItems}
                                </DropdownButton>
                            </div>
                            <div style={{ paddingRight: "9px", display: "inline-block" }}>
                                <DropdownButton title="Resolution Type" id="dropdown-size-medium">
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedResolutionType", "All")}>
                                        <Glyphicon style={{ visibility: selectedResolutionType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({resolutionTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    <MenuItem divider />
                                    {resolutionTypeMenuItems}
                                </DropdownButton>
                            </div>
                            <div style={{ paddingRight: "9px", display: "inline-block" }}>
                                <DropdownButton title="Diff Operation Type" id="dropdown-size-medium">
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedDiffOperationType", "All")}>
                                        <Glyphicon style={{ visibility: selectedDiffOperationType === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({diffOperationTypes.reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    <MenuItem divider />
                                    {diffOperationTypeMenuItems}
                                </DropdownButton>
                            </div>
                        </div>
                    </div>
                    <FormGroup>
                        <FormControl style={{marginBottom: "12px"}}
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.filterPath}
                            placeholder="Enter text"
                            onChange={this.handleChange}
                            id="filterPathInputId"
                            inputRef={ref => { this.input = ref; }}
                        />
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestHeaders">Request Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestParams">Request Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestBody">Request Body</Checkbox>
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={this.state.showResponseMessageHeaders}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={this.state.showResponseMessageBody} >Response Body</Checkbox>
                        <span style={{borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        <Checkbox inline onChange={this.toggleShowOnlyFailures}>Show requests with failures only</Checkbox>
                        <span style={{borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        <Checkbox inline onChange={this.toggleShowOnlyMarkedForUpdate}>Marked for golden update</Checkbox>
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
                        <Modal.Title>Golden Update</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <p className={cube.newGoldenId ? "" : "hidden"}>Golden ID: {cube.newGoldenId}</p>
                        <p className={cube.newGoldenId ? "hidden" : ""}>Updating Operations...</p>
                    </Modal.Body>
                    <Modal.Footer className={cube.newGoldenId ? "" : "hidden"}>
                        <div>
                            <span onClick={this.handleClose} className="cube-btn">Go TO Test Config</span>&nbsp;&nbsp;
                            <span onClick={this.handleCloseDone} className="cube-btn">Done</span>
                        </div>
                    </Modal.Footer>
                </Modal>
            </div>

        );
    }

    updateGolden = () => {
        const { cube, dispatch } = this.props;

        let user = JSON.parse(localStorage.getItem('user'));

        const headers = {
            "Content-Type": "application/json",
            'Access-Control-Allow-Origin': '*',
            "Authorization": "Bearer " + user['access_token']
        };

        const post1 = axios({
            method: 'post',
            url: `${config.analyzeBaseUrl}/updateTemplateOperationSet/${cube.newTemplateVerInfo['ID']}`,
            data: cube.templateOperationSetObject,
            headers: headers
        });

        const post2 = axios({
            method: 'post',
            url: `${config.analyzeBaseUrl}/goldenUpdate/recordingOperationSet/updateMultiPath`,
            data: cube.multiOperationsSet,
            headers: headers
        });
        const _self = this;
        axios.all([post1, post2]).then(axios.spread(function (r1, r2) {
            dispatch(cubeActions.updateRecordingOperationSet());
            dispatch(cubeActions.updateGoldenSet(_self.state.replayId, cube.collectionUpdateOperationSetId.operationSetId, cube.newTemplateVerInfo['ID'], _self.state.recordingId, _self.state.app));
        }));
    };
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedShareableLink = connect(mapStateToProps)(ShareableLink);

export default connectedShareableLink;
