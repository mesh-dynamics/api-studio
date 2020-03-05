import  React , { Component, Fragment, createContext } from "react";
import DiffResultsFilter from '../../components/DiffResultsFilter/DiffResultsFilter.js';
import DiffResultsList from '../../components/DiffResultsList/DiffResultsList.js';
import { Glyphicon} from 'react-bootstrap';
import {Link} from "react-router-dom";
import _ from 'lodash';
import sortJson from "../../utils/sort-json";
import ReduceDiff from '../../utils/ReduceDiff';
import generator from '../../utils/generator/json-path-generator';
import Modal from "react-bootstrap/lib/Modal";
import {connect} from "react-redux";
import axios from "axios";
import {cubeActions} from "../../actions";
import { constructUrlParams } from "../../utils/lib/url-utils";
import config from "../../config";

const respData = {
    facets: {
        services: [{value: "s1", count: 2}, {value: "s2", count: 2}],
        apiPaths: [{value: "a1", count: 2}, {value: "a2", count: 2}],
        resolutionTypes: [{value: "ERR_ValTypeMismatch", count: 2}, {value: "OK_OtherValInvalid", count: 4}],
        pages: 10,
    }
}

const DiffResultsContext = createContext();

class DiffResults extends Component {
    constructor(props) {
        super(props);
        this.state = {
            filter : {
                selectedService: "s1",
                selectedAPI: "a1",
                selectedReqRespMatchType: "responseMismatch",
                selectedResolutionType: "All",
                currentPageNumber: 1,
                pageSize: 5,
            },
            diffToggleRibbon: {
                showResponseMessageHeaders: false,
                showResponseMessageBody: true,
                showRequestMessageHeaders: false,
                showRequestMessageQParams: false,
                showRequestMessageFParams: false,
                showRequestMessageBody: false,
                shownResponseMessageHeaders: false,
                shownResponseMessageBody: true,
                shownRequestMessageHeaders: false,
                shownRequestMessageQParams: false,
                shownRequestMessageFParams: false,
                shownRequestMessageBody: false
            },
            diffLayoutData : [],
            facetListData: {},
            
            // golden
            showNewGolden: false,
            showSaveGoldenModal: false,
            nameG: "",
            branch: "",
            version: "",
            tag: "",
            commitId: "",
            saveGoldenError: "",

            showAll: false,
        }
    }

    componentDidMount() {
        const {dispatch} = this.props;
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();
        
        //let url = new URL(window.location.search);
        //let urlSearchParams = url.searchParams;

        console.log("URL PARAMETERS:::", urlParameters["responseHeaders"]);

        const app = urlParameters["app"];
        const selectedAPI = urlParameters["selectedAPI"] ? urlParameters["selectedAPI"]  : "All"; //"%2A";
        const replayId = urlParameters["replayId"];
        const recordingId = urlParameters["recordingId"];
        const currentTemplateVer = urlParameters["currentTemplateVer"];
        const selectedService = urlParameters["selectedService"];
        const selectedReqRespMatchType = urlParameters["selectedReqRespMatchType"];
        const selectedResolutionType = urlParameters["selectedResolutionType"];
        const searchFilterPath = urlParameters["searchFilterPath"];
        const requestHeaders = urlParameters["requestHeaders"] || false;
        const requestQParams = urlParameters["requestQParams"] || false;
        const requestFParams = urlParameters["requestFParams"] || false;
        const requestBody = urlParameters["requestBody"] || false;
        const responseHeaders =  urlParameters["responseHeaders"] || false;
        const responseBody = urlParameters["responseBody"] || true;
        const timeStamp = decodeURI(urlParameters["timeStamp"]);
        const currentPageNumber = urlParameters["currentPageNumber"]
        const pageSize = urlParameters["pageSize"]
        

        dispatch(cubeActions.setSelectedApp(app));
        this.setState({
            filter : {
                selectedService: selectedService || "All",
                selectedAPI: selectedAPI || "All",
                selectedReqRespMatchType: selectedReqRespMatchType || "responseMismatch",
                selectedResolutionType: selectedResolutionType || "All",
                currentPageNumber: currentPageNumber || 1,
                pageSize: pageSize || 5,
            },
            diffToggleRibbon: {
                // response headers
                showResponseMessageHeaders: responseHeaders,
                shownResponseMessageHeaders: responseHeaders,
                // response body
                showResponseMessageBody: responseBody,
                shownResponseMessageBody: responseBody,
                // request header
                showRequestMessageHeaders: requestHeaders,
                shownRequestMessageHeaders: requestHeaders,
                // request query params
                showRequestMessageQParams: requestQParams,
                shownRequestMessageQParams: requestQParams,
                // request form params
                showRequestMessageFParams: requestFParams,
                shownRequestMessageFParams: requestFParams,
                // request body
                showRequestMessageBody: requestBody,
                shownRequestMessageBody: requestBody
            },
            replayId: replayId,
            recordingId: recordingId,
            currentTemplateVer: currentTemplateVer,
            app: app,
            searchFilterPath: searchFilterPath || "",
            timeStamp: timeStamp || "",
            showAll: (selectedResolutionType === "All"),
            
            // TODO: improve
            
        });
        setTimeout(() => {
            const { dispatch, history, cube } = this.props;
            dispatch(cubeActions.setPathResultsParams({
                path: selectedAPI,
                service: selectedService,
                replayId: replayId,
                recordingId: recordingId,
                currentTemplateVer: currentTemplateVer,
                timeStamp: timeStamp
            }));
            dispatch(cubeActions.getCollectionUpdateOperationSet(app));
            dispatch(cubeActions.setGolden({golden: recordingId, timeStamp: ""}));
            dispatch(cubeActions.getNewTemplateVerInfo(app, currentTemplateVer));
            dispatch(cubeActions.getJiraBugs(replayId, selectedAPI));
            this.fetchAndUpdateResults();
        });
    }


    // update the filter, which will update the values in the DiffResultsFilter component,
    // and then fetch the new set of results    
    handleFilterChange = (metaData, value) => {
        console.log("filter changed " + metaData + " : " + value)
        let { filter: newFilter } = this.state;
        
        // utilize the fallthrough mechanism to set hierarchical defaults for filters
        switch(metaData){
            case "selectedService":
                newFilter["selectedService"] = "All";
            case "selectedAPI":
                newFilter["selectedAPI"] = "All";
            case "selectedReqRespMatchType":
                newFilter["selectedReqRespMatchType"] = "responseMismatch";
            case "selectedResolutionType":
                newFilter["selectedResolutionType"] = "All";
            case "currentPageNumber":
                newFilter["currentPageNumber"] = 1;
            default:
                newFilter[metaData] = value;       
        }

        console.log(newFilter)

        // set the new filter and fetch new set of results
        this.setState({
                filter: newFilter,
            },
            this.updateResults
            // this.fetchAndUpdateResults
        );    
    }

    // todo: move to utils
    cleanEscapedString = (str) => {
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

    updateResults = () => {
        this.updateUrlPathWithFilters();
        this.fetchAndUpdateResults();
    };

    // todo: move to utils
    validateAndCleanHTTPMessageParts = (messagePart) => {
        if(messagePart) {

            if (_.isObject(messagePart)) {
                return messagePart;
            }

            try {
                return JSON.parse(messagePart);
            } catch (e) {
                return JSON.parse('"' + this.cleanEscapedString(_.escape(messagePart)) + '"')
            }            
        }

        return JSON.parse('""');
    }

    
    // todo: move to utils
    getDiffForMessagePart = (replayedPart, recordedPart, serverSideDiff, prefix, service, path) => {
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

    // todo: move to utils
    validateAndCreateDiffLayoutData = (replayList) => {
        let diffLayoutData = replayList.map((item, index) => {
            let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders, prefix = "/body",
                recordedRequestHeaders, replayedRequestHeaders, recordedRequestQParams, replayedRequestQParams, recordedRequestFParams, replayedRequestFParams,recordedRequestBody, replayedRequestBody, reductedDiffArrayReqHeaders, reductedDiffArrayReqBody, reductedDiffArrayReqQParams, reductedDiffArrayReqFParams;
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
                        recordedData = JSON.parse('"' + this.cleanEscapedString(_.escape(item.recordResponse.body)) + '"')
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
                let replayedResponseMime = replayedResponseHeaders["content-type"][0];
                isJson = replayedResponseMime.toLowerCase().indexOf("json") > -1;
                if (item.replayResponse.body && isJson) {
                    try {
                        replayedData = JSON.parse(item.replayResponse.body);
                    } catch (e) {
                        replayedData = JSON.parse('"' + this.cleanEscapedString(_.escape(item.replayResponse.body)) + '"')
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
                reductedDiffArray: updatedReductedDiffArray,
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

    updateResolutionFilterPaths = (diffLayoutData) => {
        const selectedResolutionType = this.state.filter.selectedResolutionType;
        diffLayoutData && diffLayoutData.forEach(item => {
            item.filterPaths = [];
            for (let jsonPathParsedDiff of item.parsedDiff) {
                // TODO: count of ERR/non-ERR types
                // add non error types to resolutionTypes list
                //resolutionTypes.push({value: eachJsonPathParsedDiff.resolution, count: 0});

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

    // fetch the analysis results
    // todo: move to service file 
    async fetchAnalysisResults(replayId, filter, start, numResults) {
        console.log("fetching replay list")
        let dataList = {}
        //let start = 0; //todo
        let analysisResUrl = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}`;
        /*
                selectedService: "s1",
                selectedAPI: "a1",
                selectedReqRespMatchType: "responseMismatch",
                selectedResolutionType: "All",
                currentPageNumber: 1,
        */

        let service = filter.selectedService === "All" ? "*" : filter.selectedService;
        let path = filter.selectedAPI === "All" ? "*" : filter.selectedAPI;
        let resolutionType = filter.selectedResolutionType === "All" ? "*" : filter.selectedResolutionType;

        let u = new URL(analysisResUrl);
        u.searchParams.set("start", start);
        u.searchParams.set("numResults", numResults);
        u.searchParams.set("includeDiff", true);
        u.searchParams.set("service", service);
        u.searchParams.set("path", path);
        u.searchParams.set("diffRes", resolutionType); 
        u.searchParams.set("reqMatchType", ""); // todo
        u.searchParams.set("respMatchType", ""); // todo
        // todo: timestamp field
        console.log("fetch url " + u)
        console.log(u)

        //let url = "https://app.meshdynamics.io/api/as/analysisResByPath/a48fd5a0-fc01-443b-a2db-685d2cc72b2c-753a5807-84e8-4c00-b3c9-e053bd10ff0f?start=20&includeDiff=true&path=%2A";
        //let url = "http://www.mocky.io/v2/5e565e05300000660028e608";
        try {
        
            let response = await fetch(u, { 
                // todo
                headers: { 
                    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTU4MzEyOTkxMCwiZXhwIjoxNTgzNzM0NzEwfQ.HeIczS9Ey0cEKZmPzOFQcTb_QmAJet63M0MlxpNTK9s", 
                }, 
                "method": "GET", 
            });
            
            if (response.ok) {
                let json = await response.json();
                dataList = json;
                if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.res)) {
                    console.log("results list is empty")
                    return {};
                } 
                return dataList;
                // let diffLayoutData = this.validateAndCreateDiffLayoutData(dataList.data.res);
                // return diffLayoutData;

                // add filter paths based on the selected resolution type 
                //this.updateResolutionFilterPaths(diffLayoutData); 
                //console.log(diffLayoutData)
                //this.setState({diffLayoutData: diffLayoutData});
            } else {
                console.error("unable to fetch analysis results");
                throw new Error("unable to fetch analysis results");
            }
        } catch (e) {
            console.error("Error fetching analysis results list");
            throw e;
        }

        // this.setState({
        //     //diffLayoutData: diffLayoutData, 
        //     //facetListData: respData.facets,
        // });
    }

    updateUrlPathWithFilters = () => {
        const { history } = this.props;
        const constructedUrlParams = constructUrlParams(this.state);

        history.push(`/diff_results${constructedUrlParams}`)
    };

    updateDiffToggleRibbon = (updatedRibbonState) => {
        console.log(updatedRibbonState)
        this.setState({ 
            ...this.state, 
            diffToggleRibbon: {
                ...this.state.diffToggleRibbon,
                ...updatedRibbonState
            }
        }, this.updateUrlPathWithFilters);
    }
        
    fetchAndUpdateResults = () => {
        console.log("fetching results and updating")
        console.log(this.state.filter)
        // fetch results from the backend
        // todo: is this pattern (using `then`) right?
        const { filter : { 
            currentPageNumber, pageSize 
        } } = this.state;
        
        let start = (currentPageNumber - 1) * pageSize;

        this.fetchAnalysisResults(this.state.replayId, this.state.filter, start, pageSize)
            .then(  
                (dataList) => {
                    const results = dataList.data && dataList.data.res || [];
                    let diffLayoutData = this.validateAndCreateDiffLayoutData(results);
                    this.updateResolutionFilterPaths(diffLayoutData);
                    
                    const numFound = dataList.data && dataList.data.numFound || 0;
                    const pages = Math.ceil(numFound/pageSize);
                    this.setState({
                        pages: pages,
                        diffLayoutData: diffLayoutData,
                        facetListData: respData.facets,
                    });
                }
        ); //todo: page number
        // create the diff layout formatted data
        //diffLayoutData = this.validateAndCreateDiffLayoutData(diffLayoutData);
        // add filter paths based on the selected resolution type 
        //this.updateResolutionFilterPaths(diffLayoutData);
        
        //this.setState({
        //    diffLayoutData: diffLayoutData,
        //    facetListData: respData.facets,
        //});
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
    }

    handleCloseSG = () => {
        this.setState({showSaveGoldenModal: false, saveGoldenError: ""});
    }

    handleSaveGolden = () => {
        if (!this.state.nameG.trim()) {
            this.setState({saveGoldenError: "Name is a Required Field, cannot be Empty.",})
        } else {
            this.updateGolden();
        }
    }

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
    }

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

    handleCurrentPopoverPathChange = (popoverCurrentPath) => this.setState({ popoverCurrentPath });


    // todo: move these to a separate component in the next refactor
    renderModals = () => {
        const {cube} = this.props;
        return (
            <Fragment>
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
            </Fragment>
        );
    }

    handleBackToDashboardClick = () => {
        const { history, dispatch } = this.props;
        dispatch(cubeActions.clearPathResultsParams());
    }

    render() {
        const showAll = (this.state.filter.selectedResolutionType === "All")
        return (
            <DiffResultsContext.Provider 
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
                        <DiffResultsFilter 
                            filter={this.state.filter} 
                            filterChangeHandler={this.handleFilterChange} 
                            facetListData={this.state.facetListData} 
                            app={this.state.app ? this.state.app : "(Unknown)"} 
                            pages={this.state.pages}
                        ></DiffResultsFilter>
                        <DiffResultsList 
                            showAll={showAll} 
                            facetListData={this.state.facetListData} 
                            diffLayoutData={this.state.diffLayoutData} 
                            diffToggleRibbon={this.state.diffToggleRibbon}
                            updateDiffToggleRibbon={this.updateDiffToggleRibbon}
                        ></DiffResultsList>
                    </div>
                    
                    {this.renderModals()}
                </div>
            </DiffResultsContext.Provider>
        )
    } 
}

const mapStateToProps = (state) => ({
    cube: state.cube
})

const connectedDiffResults = connect(mapStateToProps)(DiffResults);
export default connectedDiffResults;
export { connectedDiffResults as DiffResults, DiffResultsContext };
