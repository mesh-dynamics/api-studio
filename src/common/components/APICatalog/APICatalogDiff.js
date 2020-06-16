import React, { Component, Fragment } from 'react'
import ReactDiffViewer from "../../utils/diff/diff-main";
import _ from "lodash";
import { cubeActions } from '../../actions';
import { cubeService } from '../../services';
import { connect } from "react-redux";
import { Checkbox, FormGroup, Label } from "react-bootstrap";
import { apiCatalogActions } from '../../actions/api-catalog.actions';
import {getHttpStatus} from "../../StatusCodeList.js"

const newStyles = {
    variables: {
        addedBackground: "#e6ffed !important",
        addedColor: "#24292e  !important",
        removedBackground: "#ffeef0  !important",
        removedColor: "#24292e  !important",
        wordAddedBackground: "#acf2bd  !important",
        wordRemovedBackground: "#fdb8c0  !important",
        addedGutterBackground: "#cdffd8  !important",
        removedGutterBackground: "#ffdce0  !important",
        gutterBackground: "#f7f7f7  !important",
        gutterBackgroundDark: "#f3f1f1  !important",
        highlightBackground: "#fffbdd  !important",
        highlightGutterBackground: "#fff5b1  !important",
    },
    line: {
        padding: "10px 2px",
        "&:hover": {
            background: "#f7f7f7",
        },
    } 
};

class APICatalogDiff extends Component {
    constructor(props) {
        super(props);
        this.state = {
            requestIdLeft: "",
            requestIdRight: "",
            
            showResponseBody: true,
            showResponseHeaders: true,
            
            showRequestHeaders: true,
            showRequestQParams: true,
            showRequestFParams: true,
            showRequestBody: true,
        }
    }

    componentDidMount() {
        const {dispatch} = this.props
        this.props.setCurrentPage("diff");

        let urlParameters = _.chain(window.location.search)
        .replace('?', '')
        .split('&')
        .map(_.partial(_.split, _, '=', 2))
        .fromPairs()
        .value();

        const app = urlParameters["app"]; // todo: app
        //dispatch(cubeActions.setSelectedApp(app));
        
        const requestIdLeft = urlParameters["requestId1"] || "";
        const requestIdRight = urlParameters["requestId2"] || "";
        if (!(requestIdLeft && requestIdRight)) {
            // todo
            console.error("Need 2 requests to compare")
        }
        this.setState(
            {requestIdLeft, requestIdRight}, 
            this.getRequestDetails
        )
    }

    getRequestDetails = () => {
        const {cube, dispatch} = this.props;
        const {requestIdLeft, requestIdRight} = this.state
        dispatch(apiCatalogActions.getDiffData(cube.selectedApp, requestIdLeft, requestIdRight));
    }

    toggleContents = (e) => {
        this.setState({[e.target.value]: e.target.checked})
    }

    renderToggleRibbon = () => {
        const {
            showResponseBody, // Response Body
            showResponseHeaders, // Response Headers
            
            showRequestHeaders, // Request Headers
            showRequestQParams, // Request Q Params
            showRequestFParams, // Request F Params
            showRequestBody,// Request Body
        } = this.state;

        return (
            <Fragment>
                <FormGroup>
                        <Checkbox inline onChange={this.toggleContents} value="showRequestHeaders" checked={showRequestHeaders}>Request Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleContents} value="showRequestQParams" checked={showRequestQParams}>Request Query Params</Checkbox>
                        <Checkbox inline onChange={this.toggleContents} value="showRequestFParams" checked={showRequestFParams}>Request Form Params</Checkbox>
                        <Checkbox inline onChange={this.toggleContents} value="showRequestBody" checked={showRequestBody}>Request Body</Checkbox>
                        
                        <span style={{height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px"}}></span>
                        
                        <Checkbox inline onChange={this.toggleContents} value="showResponseHeaders" checked={showResponseHeaders}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleContents} value="showResponseBody" checked={showResponseBody} >Response Body</Checkbox>
                    </FormGroup>
            </Fragment>
        )
    }


    renderDiff = (requestLeftPayload, requestRightPayload, responseLeftPayload, responseRightPayload) => {
        const {showRequestHeaders, showRequestQParams, showRequestFParams, showRequestBody, showResponseHeaders, showResponseBody} = this.state;

        return <div>
            {showRequestHeaders && <div>
                <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Headers</Label></h4>
                <div className="headers-diff-wrapper">
                    <ReactDiffViewer
                        styles={newStyles}
                        oldValue={JSON.stringify(requestLeftPayload.hdrs, undefined, 4)}
                        newValue={JSON.stringify(requestRightPayload.hdrs, undefined, 4)}
                        splitView={true}
                        disableWordDiff={false}
                        onLineNumberClick={(lineId, e) => { return; }}
                        showAll={true}
                        filterPaths={[]}
                        searchFilterPath=""
                        enableClientSideDiff={true}
                    />
                </div>
            </div>}

            {showRequestQParams && <div>
                <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Query Params</Label></h4>
                <div className="headers-diff-wrapper">
                    <ReactDiffViewer
                        styles={newStyles}
                        oldValue={JSON.stringify(requestLeftPayload.queryParams, undefined, 4)}
                        newValue={JSON.stringify(requestRightPayload.queryParams, undefined, 4)}
                        splitView={true}
                        disableWordDiff={false}
                        onLineNumberClick={(lineId, e) => { return; }}
                        showAll={true}
                        filterPaths={[]}
                        searchFilterPath=""
                        enableClientSideDiff={true}
                    />
                </div>
            </div>}

            {showRequestFParams && <div>
                <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Form Params</Label></h4>
                <div className="headers-diff-wrapper">
                    <ReactDiffViewer
                        styles={newStyles}
                        oldValue={JSON.stringify(requestLeftPayload.formParams, undefined, 4)}
                        newValue={JSON.stringify(requestLeftPayload.formParams, undefined, 4)}
                        splitView={true}
                        disableWordDiff={false}
                        onLineNumberClick={(lineId, e) => { return; }}
                        showAll={true}
                        filterPaths={[]}
                        searchFilterPath=""
                        enableClientSideDiff={true}
                    />
                </div>
            </div>}

            {showRequestBody && <div>
                <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Request Body</Label></h4>
                <div className="headers-diff-wrapper">
                    <ReactDiffViewer
                        styles={newStyles}
                        oldValue={JSON.stringify(requestLeftPayload.body || {}, undefined, 4)}
                        newValue={JSON.stringify(requestRightPayload.body || {}, undefined, 4)}
                        splitView={true}
                        disableWordDiff={false}
                        onLineNumberClick={(lineId, e) => { return; }}
                        showAll={true}
                        filterPaths={[]}
                        searchFilterPath=""
                        enableClientSideDiff={true}
                    />
                </div>
            </div>}

            {showResponseHeaders && <div>
                <h4><Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Headers</Label></h4>
                <div className="headers-diff-wrapper">
                    <ReactDiffViewer
                        styles={newStyles}
                        oldValue={JSON.stringify(responseLeftPayload.hdrs, undefined, 4)}
                        newValue={JSON.stringify(responseRightPayload.hdrs, undefined, 4)}
                        splitView={true}
                        disableWordDiff={false}
                        onLineNumberClick={(lineId, e) => { return; }}
                        showAll={true}
                        filterPaths={[]}
                        searchFilterPath=""
                        enableClientSideDiff={true}
                    />
                </div>
            </div>}

            {showResponseBody && <div>
                <div className="row">
                    <div className="col-md-6">
                        <h4>
                            <Label bsStyle="primary" style={{textAlign: "left", fontWeight: "400"}}>Response Body</Label>&nbsp;&nbsp;
                            <span className="font-12">Status:&nbsp;<span className="green">{getHttpStatus(responseLeftPayload.status)}</span></span>
                        </h4>
                    </div>

                    <div className="col-md-6">
                        <h4 style={{marginLeft: "18%"}}>
                            <span className="font-12">Status:&nbsp;<span className="green">{getHttpStatus(responseRightPayload.status)}</span></span>
                        </h4>
                    </div>
                </div>
                <div className="headers-diff-wrapper">
                    <ReactDiffViewer
                        styles={newStyles}
                        oldValue={JSON.stringify(responseLeftPayload.body, undefined, 4)}
                        newValue={JSON.stringify(responseRightPayload.body, undefined, 4)}
                        splitView={true}
                        disableWordDiff={false}
                        onLineNumberClick={(lineId, e) => { return; }}
                        showAll={true}
                        filterPaths={[]}
                        searchFilterPath=""
                        enableClientSideDiff={true}
                    />
                </div>
            </div>}

        </div>
    }

    render() {
        const {apiCatalog : {diffRequestLeft, diffRequestRight, diffResponseLeft, diffResponseRight}} = this.props;
        
        const requestLeftPayload = diffRequestLeft ?  (_.isEmpty(diffRequestLeft.payload) ? {} : diffRequestLeft.payload[1]) : {};
        const requestRightPayload = diffRequestRight ? (_.isEmpty(diffRequestRight.payload) ? {} : diffRequestRight.payload[1]) : {};

        const responseLeftPayload = diffResponseLeft ? (_.isEmpty(diffResponseLeft.payload) ? {} : diffResponseLeft.payload[1]) : {};
        const responseRightPayload = diffResponseRight ? (_.isEmpty(diffResponseRight.payload) ? {} : diffResponseRight.payload[1]) : {};

        return (
            <div className="margin-top-10">
                <p style={{fontWeight: 300}}>COMPARE REQUESTS</p>
                
                {this.renderToggleRibbon()}

                <div className="row margin-top-10 border-bottom">
                    <div className="col-md-6">
                        <span className="diff-section-label">{diffRequestLeft && diffRequestLeft.apiPath}</span>
                        <span className="diff-section-label margin-left-15">{new Date(diffRequestLeft && diffRequestLeft.timestamp * 1000).toLocaleString()}</span>
                    </div>
                    <div className="col-md-6">
                        <span className="diff-section-label shift-left-align">{diffRequestRight && diffRequestRight.apiPath}</span>
                        <span className="diff-section-label margin-left-15">{new Date(diffRequestRight && diffRequestRight.timestamp * 1000).toLocaleString()}</span>
                    </div>
                </div>

                {this.renderDiff(requestLeftPayload, requestRightPayload, responseLeftPayload, responseRightPayload)}
            </div>
        )
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
    apiCatalog: state.apiCatalog,
});
  
const connectedAPICatalogDiff = connect(mapStateToProps)(APICatalogDiff);

export default connectedAPICatalogDiff;
export {connectedAPICatalogDiff as APICatalogDiff}