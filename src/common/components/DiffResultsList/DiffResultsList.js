import React, { Component, Fragment } from 'react'
import {resolutionsIconMap} from '../../components/Resolutions.js'
import { Checkbox, FormGroup, FormControl, Glyphicon, DropdownButton, MenuItem, Label, Breadcrumb, ButtonGroup, Button, Radio} from 'react-bootstrap';
import ReactDiffViewer from '../../utils/diff/diff-main';
import statusCodeList from "../../StatusCodeList"
import _ from 'lodash';
import "../../components/Diff.css"

export default class DiffResultsList extends Component {
    constructor(props) {
        super(props);
        this.state = {
            showRequestMessageHeaders: false,
            showRequestMessageQParams: false,
            showRequestMessageFParams: false,
            showRequestMessageBody: false,
            showResponseMessageHeaders: false,
            showResponseMessageBody: true,
            searchFilterPath: "",
            //selectedResolutionType: "All",
            showFragments: false,
        }
        //this.selectedResolutionType = "All";
        //this.resolutionTypes = [{value: "ERR", count: 2}];
        this.inputElementRef = React.createRef();
    }

    newStyles = {
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

    toggleShowFragments = () => {
        const {showFragments} = this.state;
        this.setState({showFragments: !showFragments})
    }

    toggleMessageContents = (e) => {
        switch (e.target.value) {
            case "responseHeaders":
                this.setState({ showResponseMessageHeaders: e.target.checked, shownResponseMessageHeaders: true });       
                break;
        
            case "responseBody":
                this.setState({ showResponseMessageBody: e.target.checked, shownResponseMessageBody: true });
                break;

            case "requestHeaders":
                this.setState({ showRequestMessageHeaders: e.target.checked, shownRequestMessageHeaders: true });
                break;

            case "requestQParams":
                this.setState({ showRequestMessageQParams: e.target.checked, shownRequestMessageQParams: true });
                break;

            case "requestFParams":
                this.setState({ showRequestMessageFParams: e.target.checked, shownRequestMessageFParams: true });
                break;

            case "requestBody":
                this.setState({ showRequestMessageBody: e.target.checked, shownRequestMessageBody: true });
                break;
        }
        
        setTimeout(() => {
            const { showResponseMessageHeaders, showResponseMessageBody, showRequestMessageHeaders, showRequestMessageQParams, showRequestMessageFParams, showRequestMessageBody } = this.state;

            if(showResponseMessageHeaders === false && showResponseMessageBody === false && showRequestMessageHeaders === false &&  showRequestMessageQParams === false && showRequestMessageFParams === false && showRequestMessageBody === false) {
                this.setState({ showResponseMessageBody: true, shownResponseMessageBody: true });
            }
        });

    }

    getHttpStatus = (code) => {
        for (let httpStatus of statusCodeList) {
            if (code == httpStatus.status) {
                return httpStatus.value;
            }
        }

        return code;
    };

    handleSearchFilterChange = (e) => {
        //const { history } = this.props;

        this.setState({ searchFilterPath: e.target.value });

        //this.historySearchParams = updateSearchHistoryParams("searchFilterPath", e.target.value, this.state);

        // history.push({
        //     pathname: '/shareable_link',
        //     search: this.historySearchParams
        // });
    }

    renderToggleRibbon = () => {
        return (
            <Fragment>
                <FormGroup>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestHeaders" checked={this.state.showRequestMessageHeaders}>Request Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestQParams" checked={this.state.showRequestMessageQParams}>Request Query Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestFParams" checked={this.state.showRequestMessageFParams}>Request Form Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="requestBody" checked={this.state.showRequestMessageBody}>Request Body</Checkbox>
                        
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={this.state.showResponseMessageHeaders}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={this.state.showResponseMessageBody} >Response Body</Checkbox>
                        
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        
                        <Checkbox inline onChange={this.toggleShowFragments} checked={this.state.showFragments}>Show fragments only</Checkbox>
                        
                        <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.searchFilterPath}
                            placeholder="Search"
                            onChange={this.handleSearchFilterChange}
                            id="filterPathInputId"
                        />
                    </FormGroup>
            </Fragment>
        )
    }

    renderResultsList = () => {
        const newStyles = this.newStyles;

        const { diffLayoutData } = this.props;

        if (diffLayoutData.length == 0) {
            return (
                <div className="loading-text">
                    No Results Found
                </div>
            )
        }
        
        return diffLayoutData.map((item, index) => {
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
                                showAll={!this.state.showFragments}
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
                                    diffArray={item.reductedDiffArray}
                                    filterPaths={item.filterPaths}
                                    onLineNumberClick={(lineId, e) => { return; }}
                                    inputElementRef={this.inputElementRef}
                                    showAll={!this.state.showFragments}
                                    searchFilterPath={this.state.searchFilterPath}
                                />
                            </div>
                        )}
                    </div>
                )}
            </div >);
        });
    }

    renderLoading = () => {
        return (
            <div className={this.props.fetching ? "loading-text" : ""}>
                Loading...
            </div>
        );
    }

    render() {
        return (
            <div>
                {this.props.fetching 
                ? this.renderLoading() 
                : 
                <Fragment>
                    {this.renderToggleRibbon()}
                    {this.renderResultsList()}
                </Fragment>}
            </div>
        )
    }
}
