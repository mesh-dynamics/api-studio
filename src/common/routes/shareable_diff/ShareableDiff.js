import React, { Component } from 'react';
import { Checkbox, FormGroup, Glyphicon, FormControl, Label } from 'react-bootstrap';
import _ from 'lodash';

import ReactDiffViewer from '../../utils/diff/diff-main';
import ReduceDiff from '../../utils/ReduceDiff';
import config from "../../config";
import generator from '../../utils/generator/json-path-generator';

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

const uniqueFilterFunction = (value, index, self) => {
    return self.indexOf(value) === index;
}

class ShareableDiff extends Component {
    constructor(props) {
        super(props);
        this.state = {
            replayList: [],
            diffLayoutData: [],
            showResponseMessageHeaders: false,
            showResponseMessageBody: true,
            showRequestMessageHeaders: false,
            showRequestMessageParams: false,
            showRequestMessageBody: false,
            filterPath: ''
        }

        this.handleChange = this.handleChange.bind(this);
        this.toggleMessageContents = this.toggleMessageContents.bind(this);

        this.inputElementRef = React.createRef();
    }

    componentDidMount() {
        this.fetchReplayList();
    }

    componentDidUpdate(prevProps, prevState) {
    }

    componentWillUnmount() {
    }

    handleChange(e) {
        this.setState({ filterPath: e.target.value });
    }

    toggleMessageContents(e) {
        if (e.target.value === "responseHeaders") this.setState({ showResponseMessageHeaders: e.target.checked });
        if (e.target.value === "responseBody") this.setState({ showResponseMessageBody: e.target.checked });
        if (e.target.value === "requestHeaders") this.setState({ showRequestMessageHeaders: e.target.checked });
        if (e.target.value === "requestParams") this.setState({ showRequestMessageParams: e.target.checked });
        if (e.target.value === "requestBody") this.setState({ showRequestMessageBody: e.target.checked });
    }

    async fetchReplayList() {
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();
        console.log("urlParameters: ", urlParameters);
        let response, json, { resultsFetched } = this.state;
        let user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.analyzeBaseUrl}/analysisResNoTrace/${urlParameters["replayId"]}/${urlParameters["recordRequestId"]}`;
        let dataList = {};
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
                let diffLayoutData = this.validateAndCreateDiffLayoutData([dataList]);
                this.setState({
                    replayList: this.state.replayList.concat([dataList]),
                    diffLayoutData: this.state.diffLayoutData.concat(diffLayoutData)
                });
            } else {
                console.log("Response not ok in fetchTimeline", response);
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
                    apiPath: item.path
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
                if (item.recordRequest.body) {
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
        let { diffLayoutData } = this.state;
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
        let jsxContent = diffLayoutData.map((item, index) => {
            return (<div key={item.path + '-' + index} style={{ borderBottom: "1px solid #eee", display: item.show ? "block" : "none" }}>
                <div style={{ backgroundColor: "#EAEAEA", paddingTop: "18px", paddingBottom: "18px", paddingLeft: "10px" }}>
                    {item.path}
                </div>
                {item.recordedRequestHeaders != null && item.replayedRequestHeaders != null && (
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
                {item.recordedRequestParams != null && item.replayedRequestParams != null && (
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
                {item.recordedRequestBody != null && item.replayedRequestBody != null && (
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
                {item.recordedResponseHeaders != null && item.replayedResponseHeaders != null && (
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
                                onLineNumberClick={(lineId, e) => { console.log({ lineId, e }); }}
                                inputElementRef={this.inputElementRef}
                            />
                        </div>
                    </div>
                )}
            </div >);
        });

        return (
            <div style={{ padding: "18px", marginTop: "36px" }}>
                <div style={{ marginBottom: "18px" }}>
                    <FormGroup>
                    <Checkbox inline onChange={this.toggleMessageContents} value="requestHeaders">Request Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestParams">Request Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestBody">Request Body</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={this.state.showResponseMessageHeaders}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={this.state.showResponseMessageBody} >Response Body</Checkbox>
                    </FormGroup>
                    <FormGroup>
                        <FormControl
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.filterPath}
                            placeholder="Enter text"
                            onChange={this.handleChange}
                            id="filterPathInputId"
                            inputRef={ref => { this.input = ref; }}
                        />
                    </FormGroup>
                </div>
                <div>
                    {jsxContent}
                </div>
            </div>

        );
    }
}

export default ShareableDiff;