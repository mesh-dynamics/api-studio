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

const respData = {
    facets: {
        services: [{value: "s1", count: 2}, {value: "s2", count: 2}],
        apiPaths: [{value: "a1", count: 2}, {value: "a2", count: 2}],
        resolutionTypes: [{value: "ERR_ValTypeMismatch", count: 2}],
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
            },
            diffLayoutData : [],
            facetListData: {},
        }
    }

    componentDidMount = () => {
        this.fetchResults();
    }

    handleFilterChange = (metaData, value) => {
        console.log("filter changed " + metaData + " : " + value)
        this.setState({
            filter : {
                ...this.state.filter,
                [metaData] : value,
            }
        })
        this.fetchResults();
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
            cleanedMessagepart = JSON.parse('""');
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

    async fetchResults() {
        console.log("fetching replay list")
        let dataList = {}
        //let url = "https://app.meshdynamics.io/api/as/analysisResByPath/a48fd5a0-fc01-443b-a2db-685d2cc72b2c-753a5807-84e8-4c00-b3c9-e053bd10ff0f?start=20&includeDiff=true&path=%2A";
        let url = "http://www.mocky.io/v2/5e565e05300000660028e608";
        try {
        
            let response = await fetch(url, { 
                "credentials": "include", 
                "headers": { 
                    "accept": "application/json, text/plain, */*", 
                    "accept-language": "en-US,en;q=0.9", 
                    "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTU4MjE3MTA5MSwiZXhwIjoxNTgyNzc1ODkxfQ.N5ZUkK29_B588MWMeezK1bRb_7l26t7ti_2k2T8E0pE", 
                    "cache-control": "no-cache", 
                    "sec-fetch-mode": "cors", 
                    "sec-fetch-site": "same-origin" 
                }, 
                "body": null, 
                "method": "GET", 
                "mode": "cors" 
            });
            
            if (response.ok) {
                let json = await response.json();
                dataList = json;
                if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.res)) {
                    console.log("results list is empty")
                }
                let diffLayoutData = this.validateAndCreateDiffLayoutData(dataList.data.res);
                this.setState({diffLayoutData: diffLayoutData});
            } else {
                console.error("unable to fetch analysis results");
                throw new Error("unable to fetch analysis results");
            }
        } catch (e) {
            console.error("Error fetching analysis results list");
            throw e;
        }

        //console.log(respData.facets)
        //let diffLayoutData = this.validateAndCreateDiffLayoutData(respData.results);
        this.setState({
            //diffLayoutData: diffLayoutData, 
            facetListData: respData.facets,
        });
    }

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

    render() {
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
                        <DiffResultsFilter filter={this.state.filter} filterChangeHandler={this.handleFilterChange} facetListData={this.state.facetListData} app={"app"}></DiffResultsFilter>
                        <DiffResultsList diffLayoutData={this.state.diffLayoutData} facetListData={this.state.facetListData}></DiffResultsList>
                    </div>
                    
                    {this.renderModals()}
                </div>
            </DiffResultsContext.Provider>
        )
    } 
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedDiffResults = connect(mapStateToProps)(DiffResults);
export default connectedDiffResults;
export { connectedDiffResults as DiffResults, DiffResultsContext };
