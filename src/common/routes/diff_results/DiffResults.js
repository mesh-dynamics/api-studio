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
import { constructUrlParamsDiffResults } from "../../utils/lib/url-utils";
import config from "../../config";

const DiffResultsContext = createContext();

class DiffResults extends Component {
    constructor(props) {
        super(props);
        this.state = {
            filter : {
                selectedService: "All",
                selectedAPI: "All",
                
                selectedReqMatchType: "match",
                selectedDiffType: "All",
                selectedResolutionType: "All",
                //selectedReqCompareResType: "All",
                //selectedRespCompareResType: "All",
    
                startIndex: 0,
                endIndex: 5,
            },
            diffToggleRibbon: {
                showResponseMessageHeaders: false,
                showResponseMessageBody: true,
                showRequestMessageHeaders: false,
                showRequestMessageQParams: false,
                showRequestMessageFParams: false,
                showRequestMessageBody: false,
            },

            diffLayoutData : [],
            
            // facets
            serviceFacets: {},
            apiPathFacets: {},
            resolutionTypeFacets: [],
            
            // golden
            showNewGolden: false,
            showSaveGoldenModal: false,
            nameG: "",
            branch: "",
            version: "",
            tag: "",
            commitId: "",
            saveGoldenError: "",

            isFetching: true,
            numResults: 0,
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
        
        const app = urlParameters["app"];
        const selectedAPI = urlParameters["selectedAPI"] || "All"; //"%2A";
        const replayId = urlParameters["replayId"];
        const recordingId = urlParameters["recordingId"];
        const currentTemplateVer = urlParameters["currentTemplateVer"];
        const selectedService = urlParameters["selectedService"] || "All";
        const selectedReqMatchType = urlParameters["selectedReqMatchType"] || "match";
        const selectedDiffType = urlParameters["selectedDiffType"] || "All";
        const selectedResolutionType = urlParameters["selectedResolutionType"] || "All";
        //const selectedReqCompareResType = urlParameters["selectedReqCompareResType"] || "All";
        //const selectedRespCompareResType = urlParameters["selectedRespCompareResType"] || "All";

        const requestHeaders = urlParameters["requestHeaders"];
        const requestQParams = urlParameters["requestQParams"];
        const requestFParams = urlParameters["requestFParams"];
        const requestBody = urlParameters["requestBody"];
        const responseHeaders =  urlParameters["responseHeaders"];
        const responseBody = urlParameters["responseBody"];

        const searchFilterPath = urlParameters["searchFilterPath"] || "";
        const timeStamp = decodeURI(urlParameters["timeStamp"]) || "";
        
        const startIndex = urlParameters["startIndex"] || null;
        const endIndex = urlParameters["endIndex"] || null;

        // if only start index is present in the url, fetch forward
        // else if only end index is present, fetch backward
        // checking 'null' because 0 is also valid
        let updateFunc;
        if (startIndex != null) {
            updateFunc  = () => this.updateResults(true, Math.max(startIndex, 0));
        } else if (endIndex != null) {
            updateFunc  = () => this.updateResults(false, endIndex);
        } else {
            // if none of the above, start with 0, fetching forward
            updateFunc  = () => this.updateResults(true, 0);
        }
        this.setServiceFacetData(replayId); // needs to be done only once per page load
        dispatch(cubeActions.setSelectedApp(app));
        let newFilter = {
            ...this.state.filter,
            selectedService: selectedService,
            selectedAPI: selectedAPI,
            
            selectedReqMatchType: selectedReqMatchType,
            selectedDiffType: selectedDiffType,
            selectedResolutionType: selectedResolutionType,
            //selectedReqCompareResType: selectedReqCompareResType,
            //selectedRespCompareResType: selectedRespCompareResType,

            startIndex: startIndex,
            endIndex: endIndex,
        }
        this.setState({
            filter : newFilter,
            // set the toggle ribbon 'show' states (parse the strings from url params to boolean)
            diffToggleRibbon: {
                // response headers
                showResponseMessageHeaders: responseHeaders ? JSON.parse(responseHeaders) : false,

                // response body
                showResponseMessageBody: responseBody ? JSON.parse(responseBody) : true,

                // request header
                showRequestMessageHeaders: requestHeaders ? JSON.parse(requestHeaders) : false,

                // request query params
                showRequestMessageQParams: requestQParams ? JSON.parse(requestQParams) : false,

                // request form params
                showRequestMessageFParams: requestFParams ? JSON.parse(requestFParams) : false,

                // request body
                showRequestMessageBody: requestBody ? JSON.parse(requestBody) : false,
            },
            replayId: replayId,
            recordingId: recordingId,
            currentTemplateVer: currentTemplateVer,
            app: app,
            searchFilterPath: searchFilterPath,
            timeStamp: timeStamp,
        },
        updateFunc);

        setTimeout(() => {
            const { dispatch } = this.props;
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
        });
    }


    // update the filter, which will update the values in the DiffResultsFilter component,
    // and then fetch the new set of results    
    handleFilterChange = (metaData, value) => {
        let { filter: newFilter } = this.state;
        
        // utilize the fallthrough mechanism to set hierarchical defaults for filters
        switch(metaData){
            case "selectedService":
                newFilter["selectedService"] = "All";
            case "selectedAPI":
                newFilter["selectedAPI"] = "All";
            case "selectedReqMatchType":
                newFilter["selectedReqMatchType"] = "match";
            case "selectedDiffType":
                newFilter['selectedDiffType'] = "All";
            case "selectedResolutionType":
                newFilter['selectedResolutionType'] = "All";
    
            /* keeping around in case needed later */ 
            // case "selectedReqCompareResType":
            // case "selectedRespCompareResType":
            //     // set to defaults only if the higher ones are changed
            //     if (!metaData.includes("CompareResType")) {
            //         newFilter["selectedReqCompareResType"] = "All";
            //         newFilter["selectedRespCompareResType"] = "All";        
            //     }
                
            default:
                newFilter[metaData] = value;       
        }

        // set the new filter and fetch new set of results
        this.setState({
                filter: newFilter,
                showAll: (newFilter['selectedResolutionType'] === "All"),
            },
            () => this.updateResults(true, 0) // fetch and update results from 0
        );    
    }

    updateResults = (isNextPage, index) => {
        this.updatePageResults(isNextPage, index)
        .then(
            () => this.updateUrlPathWithFilters(isNextPage)
        )
    };

    updateUrlPathWithFilters = (isNextPage) => {
        const { history } = this.props;
        const constructedUrlParams = constructUrlParamsDiffResults(this.state, isNextPage);

        history.push(`/diff_results?${constructedUrlParams}`)
    };

    updateResolutionFilterPaths = (diffLayoutData) => {
        const selectedResolutionType = this.state.filter.selectedResolutionType;
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

    // fetch and set the service facets
    setServiceFacetData = async (replayId) => {
        this.fetchFacetData(replayId)
        .then(
            (resultsData) => {
                const facets = resultsData.data && resultsData.data.facets || {};
                this.setState({
                    serviceFacets: facets.serviceFacets,
                })
            }
        );
    }


    // todo: move to utils
    pruneResults = (diffLayoutData, fromBeginning) => {
        let accumulatedObjectSize = 0;
        const diffObjectSizeThreshold = config.diffObjectSizeThreshold;
        const maxDiffResultsPerPage = config.maxDiffResultsPerPage;
        let len = diffLayoutData.length;
        let i;
        if (fromBeginning) { // prune from top of the list
            i = 0;
            while (accumulatedObjectSize <= diffObjectSizeThreshold && i < len && i < maxDiffResultsPerPage) {
                accumulatedObjectSize += this.roughSizeOfObject(diffLayoutData[i]);
                i++;
            }
            let diffLayoutDataPruned = diffLayoutData.slice(0, i)
            return {diffLayoutDataPruned, i};
        } else { // prune from bottom of the list
            i = 0;
            while (accumulatedObjectSize <= diffObjectSizeThreshold && i < len && i < maxDiffResultsPerPage) {
                accumulatedObjectSize += this.roughSizeOfObject(diffLayoutData[len-i-1]);
                i++;
            }
            let diffLayoutDataPruned = diffLayoutData.slice(len - i, len);
            return {diffLayoutDataPruned, i} 
        }
    }

    // todo: move to utils
    roughSizeOfObject = ( object ) => {

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
    

    // fetch the analysis results
    // todo: move to service file 
    async fetchAnalysisResults(replayId, filter) {
        console.log("fetching replay list")
        let analysisResUrl = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}`;
        
        let searchParams = new URLSearchParams();
        searchParams.set("start", filter.startIndex);
        searchParams.set("numResults", config.defaultFetchDiffResults); // 
        searchParams.set("includeDiff", true);

        if (filter.selectedService !== "All") {
            searchParams.set("service", filter.selectedService);
        }

        if (filter.selectedAPI !== "All") {
            searchParams.set("path", filter.selectedAPI);
        }

        if (filter.selectedResolutionType !== "All") {
            searchParams.set("diffRes", filter.selectedResolutionType)
        }

        let reqMatchType = filter.selectedReqMatchType === "mismatch" ? "NoMatch" : "ExactMatch"; // 
        searchParams.set("reqMatchType", reqMatchType); 
        
        switch (filter.selectedDiffType) {
            case "All":
                break;
            case "requestDiff":
                searchParams.set("reqCmpResType", "NoMatch"); 
                break;
            case "responseDiff":
                searchParams.set("respMatchType", "NoMatch"); // misnomer in the API, should've been respCmpResType
                break;
        }
        
        let url = analysisResUrl + "?" + searchParams.toString();
        let user = JSON.parse(localStorage.getItem('user'));
        try {
        
            let response = await fetch(url, { 
                headers: { 
                    "Authorization": "Bearer " + user['access_token']
                }, 
                "method": "GET", 
            });
            
            if (response.ok) {
                let dataList = {}
                let json = await response.json();
                dataList = json;
                if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.res)) {
                    console.log("results list is empty")
                    return {};
                } 
                return dataList;
            } else {
                console.error("unable to fetch analysis results");
                throw new Error("unable to fetch analysis results");
            }
        } catch (e) {
            console.error("Error fetching analysis results list");
            throw e;
        }
    }

    async fetchFacetData(replayId) {
        let analysisResUrl = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}`;
        let searchParams = new URLSearchParams();
        searchParams.set("numResults", 0);
        
        let url = analysisResUrl + "?" + searchParams.toString();

        let user = JSON.parse(localStorage.getItem('user'));
        try {
        
            let response = await fetch(url, { 
                headers: { 
                    "Authorization": "Bearer " + user['access_token']
                }, 
                "method": "GET", 
            });
            
            if (response.ok) {
                let dataList = {}
                let json = await response.json();
                dataList = json;
                if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.facets)) {
                    console.log("facets data is empty")
                    return {};
                }
                return dataList;
            } else {
                console.error("unable to fetch facet data");
                throw new Error("unable to fetch facet data");
            }
        } catch (e) {
            console.error("Error fetching facet data");
            throw e;
        }
        //return respData.facets;
    }

    preProcessResults = (results) => {
        let diffLayoutData = this.validateAndCreateDiffLayoutData(results);
        this.updateResolutionFilterPaths(diffLayoutData);
        return diffLayoutData;
    }

    handlePageNav = (isNextPage, index) => {
        this.updateResults(isNextPage, index);
    }
    
    updatePageResults = async (isNextPage, index) => {
        let {filter, replayId} = this.state;
        let {pageSize} = config.defaultFetchDiffResults;
        let startIndex, endIndex;
        let diffLayoutDataPruned, resultsData;

        this.setState({isFetching: true})

        if (isNextPage) {
            startIndex = index;
            resultsData = await this.fetchAnalysisResults(replayId, {...filter, startIndex});
            
            const results = resultsData.data && resultsData.data.res || [];
            const diffLayoutData = this.preProcessResults(results);
            
            let pruneEndIndex;
            ({diffLayoutDataPruned, i: pruneEndIndex} = this.pruneResults(diffLayoutData, true));
            
            endIndex = startIndex + pruneEndIndex;
            
        } else {
            endIndex = index;
            startIndex = Math.max(endIndex - pageSize, 0);
            resultsData = await this.fetchAnalysisResults(replayId, {...filter, startIndex});
            
            const results = resultsData.data && resultsData.data.res || [];
            const diffLayoutData = this.preProcessResults(results);
            
            let pruneStartIndex;
            ({diffLayoutDataPruned, i: pruneStartIndex} = this.pruneResults(diffLayoutData, false))
            
            startIndex = Math.max(endIndex - pruneStartIndex, 0);

        }

        const facets = resultsData.data && resultsData.data.facets || {};
        const numFound = resultsData.data && resultsData.data.numFound || 0;

        this.setState({
            diffLayoutData: diffLayoutDataPruned,
            apiPathFacets: facets.pathFacets,
            resolutionTypeFacets: facets.diffResFacets,
            isFetching: false,
            filter: {
                ...filter,
                startIndex,
                endIndex,
            },
            numResults: numFound,
        });
    }

    updateDiffToggleRibbon = (updatedRibbonState) => {

        console.log(updatedRibbonState)
        let newDiffToggleRibbon = {
            ...this.state.diffToggleRibbon,
            ...updatedRibbonState
        }

        this.setState({ 
            diffToggleRibbon: newDiffToggleRibbon,
        }, this.updateUrlPathWithFilters);
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
        const showAll = this.state.showAll;
        const facetListData = {
            services: this.state.serviceFacets,
            apiPaths: this.state.apiPathFacets,
            resolutionTypes: this.state.resolutionTypeFacets,
        };
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
                            facetListData={facetListData} 
                            app={this.state.app ? this.state.app : "(Unknown)"} 
                            pages={this.state.pages}
                        ></DiffResultsFilter>

                        <DiffResultsList 
                            showAll={showAll} 
                            diffLayoutData={this.state.diffLayoutData} 
                            diffToggleRibbon={this.state.diffToggleRibbon}
                            updateDiffToggleRibbon={this.updateDiffToggleRibbon}
                            isFetching={this.state.isFetching}
                            handlePageNav={this.handlePageNav}
                            startIndex={this.state.filter.startIndex}
                            endIndex={this.state.filter.endIndex}
                            numResults={this.state.numResults}
                        ></DiffResultsList>
                    </div>
                    
                    {this.renderModals()}
                </div>
            </DiffResultsContext.Provider>
        )
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
    
    // todo: move to utils
    validateAndCleanHTTPMessageParts = (messagePart) => {
        let cleanedMessagepart = "";
        if (messagePart &&_.isObject(messagePart)) {
            cleanedMessagepart = messagePart;
        } else if (messagePart) {
            try {
                cleanedMessagepart = JSON.parse(messagePart);
            } catch (e) {
                cleanedMessagepart = JSON.parse('"' + this.cleanEscapedString(_.escape(messagePart)) + '"')
            }
        } else {
            cleanedMessagepart = messagePart || JSON.parse('""');
        }

        return cleanedMessagepart;
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
        let diffLayoutData = replayList.map((item) => {
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
                let replayedResponseMime = replayedResponseHeaders["content-type"] ? replayedResponseHeaders["content-type"][0] : "";
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
}

const mapStateToProps = (state) => ({
    cube: state.cube
})

const connectedDiffResults = connect(mapStateToProps)(DiffResults);
export default connectedDiffResults;
export { connectedDiffResults as DiffResults, DiffResultsContext };
