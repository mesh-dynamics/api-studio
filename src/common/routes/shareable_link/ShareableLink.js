import React, { Component } from 'react';
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem } from 'react-bootstrap';
import _ from 'lodash';
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';

import ReactDiffViewer from '../../utils/diff/diff-main';
import ReduceDiff from '../../utils/ReduceDiff';
import config from "../../config";

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
            replayList: [],
            diffLayoutData: [],
            totalRequests: 0,
            resultsFetched: 0,
            filterPath: '',
            showResponseMessageHeader: false,
            showResponseMessageBody: true,
            selectedAPI: "All",
            selectedRequestMatchType: "All",
            selectedResponseMatchType: "All",
            selectedResolutionType: "All",
            selectedDiffOperationType: "All"
        }
        this.handleChange = this.handleChange.bind(this);
        this.toggleMessageContents = this.toggleMessageContents.bind(this);

        this.inputElementRef = React.createRef();
    }

    componentDidMount() {
        this.fetchReplayList();
    }

    componentDidUpdate(prevProps, prevState) {
        if (this.state.resultsFetched === prevState.resultsFetched) {
            return;
        }
        this.fetchReplayList();
    }

    componentWillUnmount() {
    }

    handleChange(e) {
        this.setState({ filterPath: e.target.value });
    }

    toggleMessageContents(e) {
        if (e.target.value === "responseHeaders") this.setState({ showResponseMessageHeader: e.target.checked });
        if (e.target.value === "responseBody") this.setState({ showResponseMessageBody: e.target.checked });
    }

    handleMetaDataSelect(metaDataType, value) {
        this.setState({[metaDataType] : value});
    }

    async fetchReplayList() {
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();
        const apiPath = urlParameters["apiPath"] ? urlParameters["apiPath"]  : "%2A",
            replayId = urlParameters["replayId"];
        if(!replayId) throw new Error("replayId is required");
        let response, json, { resultsFetched } = this.state;
        let url = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}?start=${resultsFetched}&includediff=true&path=${apiPath}`;
        let dataList = {};
        try {
            response = await fetch(url, {
                method: "get",
                headers: new Headers({
                    "cache-control": "no-cache"
                })
            });
            if (response.ok) {
                json = await response.json();
                dataList = json;
                let diffLayoutData = this.validateAndCreateDiffLayoutData(dataList.res);
                this.setState({
                    replayList: this.state.replayList.concat(dataList.res),
                    diffLayoutData: this.state.diffLayoutData.concat(diffLayoutData),
                    totalRequests: dataList.numFound,
                    resultsFetched: this.state.resultsFetched + dataList.res.length
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
            let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders;
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
            let reductedDiffArray = null;
            if (diff && diff.length > 0) {
                let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
            } else if (diff && diff.length == 0) {
                if (_.isEqual(expJSON, actJSON)) {
                    let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                    reductedDiffArray = reduceDiff.computeDiffArray();
                }
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
                reductedDiffArray,
                show: true
            }
        });
        return diffLayoutData;
    }

    render() {
        let { diffLayoutData, selectedAPI, selectedRequestMatchType, selectedResponseMatchType, selectedResolutionType, selectedDiffOperationType } = this.state;
        let requestMatchTypes = [], responseMatchTypes = [], apiPaths = [], resolutionTypes = [], diffOperationTypes = [];

        diffLayoutData.filter(function (eachItem) {
            apiPaths.push({value: eachItem.path, count: 0});
            if (selectedAPI === "All" || selectedAPI === eachItem.path) eachItem.show = true;
            else eachItem.show = false;
            return selectedAPI === "All" || selectedAPI === eachItem.path;
        }).filter(function (eachItem) {
            if (eachItem.show === true && (selectedRequestMatchType === "All" || selectedRequestMatchType === eachItem.reqmt)) {
                requestMatchTypes.push({value: eachItem.reqmt, count: 0});
            } else {
                eachItem.show = false;
            }
            return eachItem.show === true && selectedRequestMatchType === "All" || selectedRequestMatchType === eachItem.reqmt;
        }).filter(function (eachItem) {
            if (eachItem.show === true && (selectedResponseMatchType === "All" || selectedResponseMatchType === eachItem.respmt)) {
                responseMatchTypes.push({value: eachItem.respmt, count: 0});
            } else {
                eachItem.show = false;
            }
            return eachItem.show === true && selectedResponseMatchType === "All" || selectedResponseMatchType === eachItem.respmt;
        }).filter(function (eachItem) {
            let toFilter = false;
            if (eachItem.show === true) {
                for (let eachJsonPathParsedDiff of eachItem.parsedDiff) {
                    if (selectedResolutionType === "All" || selectedResolutionType === eachJsonPathParsedDiff.resolution) {
                        resolutionTypes.push({value: eachJsonPathParsedDiff.resolution, count: 0});
                        toFilter = true;
                    }
                }
            }
            if (!toFilter) eachItem.show = false;
            return toFilter;
        }).filter(function (eachItem) {
            let toFilter = false;
            if (eachItem.show === true) {
                for (let eachJsonPathParsedDiff of eachItem.parsedDiff) {
                    if (selectedDiffOperationType === "All" || selectedDiffOperationType === eachJsonPathParsedDiff.op) {
                        diffOperationTypes.push({value: eachJsonPathParsedDiff.op, count: 0});
                        toFilter = true;
                    }
                }
            }
            if (!toFilter) eachItem.show = false;
            return toFilter;
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
        requestMatchTypes = requestMatchTypes.filter(filterFunction);
        responseMatchTypes = responseMatchTypes.filter(filterFunction);
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
        let apiPathMenuItems = apiPaths.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedAPI", item.value)}>
                <Glyphicon style={{ visibility: selectedAPI === item.value ? "visible" : "hidden" }} glyph="ok" /> {item.value} ({item.count})
            </MenuItem>);
        });
        let requestMatchTypeMenuItems = requestMatchTypes.map((item, index) => {
            return (<MenuItem key={item.value + "-" + index} eventKey={index + 2} onClick={() => this.handleMetaDataSelect("selectedRequestMatchType", item)}>
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
        let jsxContent = diffLayoutData.map((item, index) => {
            return (<div key={item.path + '-' + index} style={{ borderBottom: "1px solid #eee", display: item.show ? "block" : "none" }}>
                <div style={{ backgroundColor: "#EAEAEA", paddingTop: "18px", paddingBottom: "18px", paddingLeft: "10px" }}>
                    <div className="inline-block margin-right-10">
                        <Tippy arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true} maxWidth={500}
                            content={
                                <div style={{ overflowY: "auto", fontSize: "14px" }} className="grey" id={`tooltip-${index}`}>
                                    <div style={{ padding: "10px", color: "#333333", textAlign: "left" }}>
                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">Method:</div>
                                            <div className="col-md-9 bold">{item.method}</div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">URL:</div>
                                            <div className="col-md-9 bold">{item.path}</div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">Parameters:</div>
                                            <div className="col-md-9">
                                                <table className="table table-bordered" style={{ width: "100%", tableLayout: "fixed" }}>
                                                    <thead>
                                                        <tr>
                                                            <th style={{ background: "#efefef", width: "30%" }}>Key</th>
                                                            <th style={{ background: "#efefef", width: "70%" }}>Value</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {
                                                            item.qparams && !_.isEmpty(item.qparams) ?
                                                                Object.entries(item.qparams).map(([key, value]) => (<tr key={key}>
                                                                    <td style={{ background: "#ffffff", wordBreak: "break-word", whiteSpace: "normal", width: "30%" }}>{key}</td>
                                                                    <td style={{ background: "#ffffff", wordBreak: "break-word", whiteSpace: "normal", width: "70%" }}>{value}</td>
                                                                </tr>))
                                                                : <tr></tr>
                                                        }
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">Body:</div>
                                            <div className="col-md-9">
                                                <pre style={{ backgroundColor: "#D5D5D5" }}>{JSON.stringify(item.fparams, undefined, 4)}</pre>
                                            </div>
                                        </div>
                                    </div>
                                </div>}>
                            <div><Glyphicon glyph="option-horizontal" /> </div>
                        </Tippy>
                    </div>
                    {item.path}
                </div>
                {item.recordedResponseHeaders != null && item.replayedResponseHeaders != null && (
                    <div style={{ display: this.state.showResponseMessageHeader ? "" : "none" }}>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedResponseHeaders, undefined, 4)}
                                newValue={JSON.stringify(item.replayedResponseHeaders, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={null}
                                onLineNumberClick={(lineId, e) => { console.log({ lineId, e }); }}
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
                <div >
                    <div style={{ marginBottom: "18px" }}>
                        <div style={{ display: "inline-block" }}>
                            <div style={{ paddingRight: "9px", display: "inline-block" }}>
                                <DropdownButton title="API Path" id="dropdown-size-medium">
                                    <MenuItem eventKey="1" onClick={() => this.handleMetaDataSelect("selectedAPI", "All")}>
                                        <Glyphicon style={{ visibility: selectedAPI === "All" ? "visible" : "hidden" }} glyph="ok" /> All ({apiPaths.reduce((accumulator, item) => accumulator += item.count, 0)})
                                    </MenuItem>
                                    <MenuItem divider />
                                    {apiPathMenuItems}
                                </DropdownButton>
                            </div>
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
                        <div style={{ display: "inline-block" }}>
                            <FormGroup>
                                <Checkbox inline disabled>Request Headers</Checkbox>
                                <Checkbox inline disabled>Request Params</Checkbox>
                                <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={this.state.showResponseMessageHeader}>Response Headers</Checkbox>
                                <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={this.state.showResponseMessageBody} >Response Body</Checkbox>
                                <Checkbox inline >Marked for golden update</Checkbox>
                            </FormGroup>
                        </div>
                    </div>
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

export default ShareableLink;