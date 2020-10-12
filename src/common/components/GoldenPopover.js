import React from "react";
import { connect } from "react-redux";
import { cubeActions } from "../actions";
import { cubeService } from "../services";
import config from "../config";

class GoldenPopover extends React.Component {
    constructor(props) {
        super(props);
        this.showGoldenModal = this.showGoldenModal.bind(this);
        this.showRuleModal = this.showRuleModal.bind(this);
        this.showBugModal = this.showBugModal.bind(this);
        this.updateGolden = this.updateGolden.bind(this);
        this.updateRule = this.updateRule.bind(this);
        this.setRule = this.setRule.bind(this);
        this.hideGR = this.hideGR.bind(this);
        this.createIssue = this.createIssue.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleSelectProjectChange = this.handleSelectProjectChange.bind(this);
        this.renderSummary = this.renderSummary.bind(this);
        this.renderDescription = this.renderDescription.bind(this);
        this.getDefaultSummary = this.getDefaultSummary.bind(this)
        this.getDefaultDescription = this.getDefaultDescription.bind(this);
        this.openJiraLink = this.openJiraLink.bind(this);
        this.refreshList = this.refreshList.bind(this);
        this.closeTippy = this.closeTippy.bind(this);
        this.fetchRuleAndPopulate = this.fetchRuleAndPopulate.bind(this);
        this.getInitialTemplateMatchType =  this.getInitialTemplateMatchType.bind(this);
        this.handleTemplateMatchTypeChange = this.handleTemplateMatchTypeChange.bind(this);

        this.state = {
            showGolden: false,
            showRule: false,
            showBug: false,
            showBugResponse: false,
            defaultRule: {
                "path": this.props.jsonPath.replace("<BEGIN>", ""),
                "dt": "",
                "pt": "",
                "ct": "",
                "em": "",
                "customization": null,
                "arrayCompKeyPath":""
            },
            newRule: {
                "path": this.props.jsonPath.replace("<BEGIN>", ""),
                "dt": "Default",
                "pt": "Optional",
                "ct": "Ignore",
                "em": "Default",
                "customization": null,
                "arrayCompKeyPath":""
            },
            summaryInput: this.getDefaultSummary(this.props.cube),
            descriptionInput: this.getDefaultDescription(this.props.cube),
            issueTypeId: 10004,
            projectInput: null,
            projectList: [],
            jiraErrorMessage: null,
            showJiraError: false,
            templateMatchType: null
        };
    }

    componentDidMount() {
        this.getInitialTemplateMatchType();
    }

    getInitialTemplateMatchType(){
        const { eventType } = this.props;

        switch(eventType) {
            case "Request":
                this.setState({ templateMatchType: "RequestCompare" });
            case "Response":
                this.setState({ templateMatchType: "ResponseCompare" });
            default:
                this.setState({ templateMatchType: "ResponseCompare" });
        } 
        
    }

    setRule(tag, evt) {
        const { dispatch, jsonPath } = this.props;
        const { defaultRule, newRule, templateMatchType } = this.state;
        newRule[tag] = evt.target.value;
        //TODO: Optimize here, dispatch should be done on Apply button click
        dispatch(cubeActions.addToDefaultRuleBook(jsonPath.replace("<BEGIN>", ""), defaultRule, templateMatchType));
        dispatch(cubeActions.addToRuleBook(jsonPath.replace("<BEGIN>", ""), newRule, templateMatchType));
        this.setState({ newRule: newRule });

    }

    getKeyFromTOS = () => {
        const {cube} = this.props;
        for (let key in cube.templateOperationSetObject) {
            if (cube.templateOperationSetObject.hasOwnProperty(key)) {
                const keyObj = JSON.parse(key);
                if (keyObj['path'] == cube.pathResultsParams.path) {
                    return key;
                }
            }
        }

        return "";
    };

    updateRule(operationType) {
        const { dispatch, jsonPath, cube, user } = this.props;
        const { templateMatchType } = this.state;
        const operationsObj = {
            type: operationType, // "REPLACE" or "REMOVE";
            path: jsonPath.replace("<BEGIN>", ""),
            newRule: this.state.newRule,
        };
        
        const key = this.getKeyFromTOS(); // This is already stringified
        
        const newKey = JSON.stringify({
            customerId: user.customer_name,
            appId: cube.selectedApp,
            serviceId: cube.pathResultsParams.service,
            path: cube.pathResultsParams.path,
            version: cube.pathResultsParams.currentTemplateVer,
            reqOrResp: templateMatchType,
        });

        if(key === newKey) {
            // If the key already exists in some form then push operation for the same key
            dispatch(cubeActions.pushToOperations(operationsObj, key));
        } else {
            // else create a new key and push
            dispatch(cubeActions.pushNewOperationKeyToOperations(operationsObj, newKey));
        }
        this.hideGR();
        this.props.handleHidePopoverClick();
    }

    updateGolden() {
        const { dispatch, serverSideDiff, cube, jsonPath, handleHidePopoverClick, eventType, user } = this.props;
        const operation = {};

        if (serverSideDiff) {
            // golden update for lines with diff/resolutions from the server
            operation["op"] = serverSideDiff.op.toUpperCase();
            operation["path"] = serverSideDiff.path;
            operation["value"] = serverSideDiff.value;
            operation["eventType"] = eventType;
        } else {
            // golden update for lines without diff/resolutions
            operation["op"] = "REPLACE"; // replace left with right side
            operation["path"] = jsonPath.replace("<BEGIN>", "");
            operation["value"] = null; // not required
            operation["eventType"] = eventType; 
        }

        this.hideGR();

        let indexMOS = cube.multiOperationsSet.findIndex((elem) => elem.path && elem.path == cube.pathResultsParams.path);
        if (indexMOS != -1) {
            dispatch(cubeActions.pushToOperationSet(operation, indexMOS));
        } else {
            dispatch(cubeActions.pushToMOS({
                "operationSetId": cube.collectionUpdateOperationSetId.operationSetId,
                "service": cube.pathResultsParams.service,
                "path": cube.pathResultsParams.path,
                "operationSet": [operation],
                "customer": user.customer_name,
                "app": cube.selectedApp
            }));
        }
        
        handleHidePopoverClick();
    }

    createIssue() {
        const { cube } = this.props;
        let summary = this.state.summaryInput
        let description = this.state.descriptionInput
        let projectId = this.state.projectInput
        let issueTypeId = this.state.issueTypeId
        let apiPath = cube.pathResultsParams.path;
        let jsonPath = this.props.jsonPath.replace("<BEGIN>", "");
        let replayId = cube.pathResultsParams.replayId;
        let requestId  = ""; // TODO
        cubeService
            .createJiraIssue(summary, description, issueTypeId, projectId, replayId, apiPath, requestId, jsonPath)
                .then(r => {
                        this.hideGR()
                        this.setState({ jiraIssueId: r.id, jiraIssueKey: r.key, jiraIssueURL: r.url, showBugResponse: true })
                        this.refreshList();
                    })
                .catch(e => {
                    this.setState({ showJiraError: true, jiraErrorMessage: e.message, showBug: false })
                });
    }

    showGoldenModal() {
        this.setState({ showGolden: true });
    }

    async fetchRuleAndPopulate(reqOrRespCompare){
        const { cube, jsonPath, user: { customer_name: customerId } } = this.props;

        try {
            const { path, dt, pt, ct, em , customization } = await cubeService.getResponseTemplate(
                customerId,
                cube.selectedApp, 
                cube.pathResultsParams, 
                reqOrRespCompare, 
                jsonPath.replace("<BEGIN>", "")
            );
            
            const newlyFetchedRule = {
                path: jsonPath.replace("<BEGIN>", ""),
                dt, 
                pt: pt === "Default" ? "Optional" : pt, 
                ct: ct === "Default" ? "Ignore": ct, 
                em, 
                customization  
            };

            this.setState({ templateMatchType: reqOrRespCompare, defaultRule: { ...newlyFetchedRule }, newRule: { ...newlyFetchedRule } });
        } catch (e) {
            console.log("Failed to fetch rules from api. Setting default rules");
            this.setState({ 
                templateMatchType: reqOrRespCompare, 
                defaultRule: {
                    "path": this.props.jsonPath.replace("<BEGIN>", ""),
                    "dt": "",
                    "pt": "",
                    "ct": "",
                    "em": "",
                    "customization": null,
                    "arrayCompKeyPath":""
                },
                newRule: {
                    "path": this.props.jsonPath.replace("<BEGIN>", ""),
                    "dt": "Default",
                    "pt": "Optional",
                    "ct": "Ignore",
                    "em": "Default",
                    "customization": null,
                    "arrayCompKeyPath":""
                }
            });
        }
    }

    async handleTemplateMatchTypeChange(event){
        await this.fetchRuleAndPopulate(event.target.value);
    }

    async showRuleModal() {
        const { cube, jsonPath } = this.props;
        const { templateMatchType: reqOrRespCompare } = this.state;

        // Keep this old implementation
        // const reqOrRespCompare = (eventType === "Response" ? "ResponseCompare" : "RequestCompare"); eventType

        if(cube.defaultRuleBook[jsonPath.replace("<BEGIN>", "")]) {
            const defaultRule = cube.defaultRuleBook[jsonPath.replace("<BEGIN>", "")];
            const { templateMatchType } = cube.defaultRuleBook[jsonPath.replace("<BEGIN>", "")];
            this.setState({ templateMatchType, defaultRule: { ...defaultRule }});
        }

        if (cube.ruleBook[jsonPath.replace("<BEGIN>", "")]) {
            const rule = cube.ruleBook[jsonPath.replace("<BEGIN>", "")];
            const { templateMatchType } = cube.ruleBook;
            this.setState({ templateMatchType, newRule: { ...rule } });
        } else {
            await this.fetchRuleAndPopulate(reqOrRespCompare)
        }

        this.setState({ showRule: true });
    }

    showBugModal() {
        const firstElement = 0;
        this.setState({ showBug: true });
        cubeService.getProjectList()
            .then(r => {
                // On success, set the project list and set the
                // default project id to first element on the list.
                this.setState({ projectList: r.values, projectInput: r.values[firstElement].id});
            }, err => {
                console.error(err);
            }).catch(err => {
                console.error(err);
            });
    }

    openJiraLink() {
        const { cube: { jiraBugs }, jsonPath, handleHidePopoverClick } = this.props;
        const { issueUrl } = jiraBugs.find(bug => bug.jsonPath === jsonPath.replace("<BEGIN>", ""));

        window.open(issueUrl)
        handleHidePopoverClick();
    }

    refreshList() {
        const { apiPath, replayId, dispatch } = this.props;

        dispatch(cubeActions.getJiraBugs(replayId, apiPath))
    }


    renderProjectList() {
        if(!this.state.projectList.length) {
            return ""
        }

        let options = this.state.projectList
            .filter(project => project.style === "classic")
            .map(e => <option key={e.id} value={e.id}>{e.name}</option>)

        let jsxContent = <div>
            <select placeholder="Select Project" onChange={this.handleSelectProjectChange}>
                {options}
            </select>
        </div>

        return jsxContent;
    }

    hideGR() {
        this.setState({
            showRule: false,
            showGolden: false,
            showBug: false,
            showBugResponse: false,
            jiraErrorMessage: null,
            showJiraError: false
        });
    }

    handleInputChange(event) {
        this.setState({
            [event.target.name]: event.target.value
        });
    }

    handleSelectProjectChange(event) {
        this.setState({
            projectInput: event.target.value
        })
    }

    getDefaultSummary(cube) {
        let summary = "Bug in " + cube.pathResultsParams.path;
        return summary;
    }

    findInJiraBugs(){
        const {cube, jsonPath} = this.props;
        for (const op of cube.jiraBugs) {
            if(jsonPath.replace("<BEGIN>", "")  == op.jsonPath){
                return true;
            }
        }
        return false;
    }

    formatDtValue(value){
        switch(value){
            case "RptArray": return "List [array]";
            case "NrptArray": return "Unstructured [array]";
            case "Set": return "Set [array]";
            default: return value;
        }
    }

    renderSummary() {
        return (
            <div>
                <input name="summaryInput" defaultValue={this.state.summaryInput} onChange={this.handleInputChange} style={{width:"93%", margin:"9px"}}></input>
            </div>
        )
    }

    getDefaultDescription(cube) {
        let description =
            `Issue Details:
                API Path: ${cube.pathResultsParams.path}
                JSON Path: ${this.props.jsonPath.replace("<BEGIN>", "")}
                Analysis URL: ${window.location.href}
            `
        return description;
    }

    closeTippy() {
        this.hideGR();
        this.props.handleHidePopoverClick();
    }

    renderDescription() {
        return (
            <div>
                <textarea name="descriptionInput" defaultValue={this.state.descriptionInput} onChange={this.handleInputChange} rows="10" style={{resize:"none", width:"93%", margin:"9px"}}></textarea>
            </div>
        )
    }

    render() {

        return (
            <React.Fragment>
                <span
                    onClick={this.closeTippy}
                    style={{ display: "flex", justifyContent: "flex-end", padding: "3px", cursor: "pointer"}}
                >
                    <i className="fas fa-times" style={{ color: "#616060"}}></i>
                </span>
                <div className={!this.state.showGolden && !this.state.showRule && !this.state.showBug && !this.state.showBugResponse && !this.state.showJiraError ? "text-center" : "hidden"}
                    style={{ color: "#333333" }}>
                    <div style={{ width: "300px", height: "100px", background: "#D5D5D5", padding: "20px" }}>
                        <div className="margin-bottom-10">STATUS</div>
                        <div>
                            <span>{this.props.serverSideDiff && this.props.serverSideDiff.resolution ? this.props.serverSideDiff.resolution : "OK"}</span>
                        </div>
                    </div>
                    <div style={{ width: "300px", height: "100px", background: "#ECECE7", padding: "15px" }}>
                        <div>
                            <span onClick={this.findInJiraBugs() ? this.openJiraLink : this.showBugModal} className="back-grey link">
                                <i className="fas fa-bug" style={{color: this.findInJiraBugs() ? 'blue' : '', cursor: "pointer"}}></i>
                                {this.findInJiraBugs() && <i class="fa fa-check-circle" style={{
                                    "color": "green",
                                    "fontSize": ".65em",
                                    "position": "absolute",
                                    "marginLeft": "-6px",
                                    "marginTop": "-3px"
                                }} aria-hidden="true"></i>}
                            </span>&nbsp;&nbsp;
                            <span className="back-grey inactive-link"><i className="fas fa-comments"></i></span>&nbsp;&nbsp;
                            <span className="back-grey inactive-link"><i className="fas fa-code"></i></span>&nbsp;&nbsp;
                            <span className="back-grey inactive-link"><i className="fas fa-share-alt"></i></span>
                        </div>
                        <div className="margin-top-15">
                            {
                                !this.props.hideMarkForUpdate &&
                                <span onClick={this.showGoldenModal}
                                    className="cube-btn font-12 margin-right-10">UPDATE GOLDEN
                                </span>
                            }
                            <span onClick={this.showRuleModal} className="cube-btn font-12">UPDATE RULE</span>
                            {
                                this.props.showDeleteRuleAction &&
                                <span onClick={() => this.updateRule("REMOVE")}className="cube-btn font-12 margin-left-15">
                                    DELETE RULE
                                </span>
                            }
                        </div>
                    </div>
                </div>

                <div className={this.state.showRule ? "update-rule" : "hidden"} style={{ color: "#333333" }}>
                    <div onClick={this.hideGR} style={{ width: "500px", background: "#D5D5D5", padding: "5px 20px" }}>
                        UPDATE ASSERTION RULE
                    </div>
                    <div style={{ width: "500px", background: "#ECECE7", padding: "15px 20px", textAlign: "left" }}>
                        <div style={{ display: "flex", justifyContent: "space-between" }}>
                            <div>
                                <div>Path:&nbsp;<b>{this.props.jsonPath}</b></div>
                                <div>Data Type:&nbsp;<b>{this.state.newRule.dt}</b></div>
                                <div>Count of similar items:&nbsp;<b>105</b></div>
                                {/* TODO: Above value is hardcoded. Find out more */}
                            </div>
                            { 
                                this.props.eventType === 'Request'
                                &&
                                (
                                    <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end" }}>
                                        <span>Template Type</span>
                                        <div>
                                            <select style={{ width: '100px' }} onChange={this.handleTemplateMatchTypeChange} value={this.state.templateMatchType}>
                                                <option value="RequestMatch">Match</option>
                                                <option value="RequestCompare">Compare</option>
                                            </select>
                                        </div>
                                    </div>
                                )
                            }
                            
                        </div>
                        <div className="table-responsive margin-top-10">
                            <table className="table table-striped" style={{ textAlign: "left" }}>
                                <thead>
                                    <tr>
                                        <th>Properties</th>
                                        <th>Current</th>
                                        <th>New</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    <tr>
                                        <td>Presence</td>
                                        <td>{this.state.defaultRule.pt}</td>
                                        <td>
                                            <select value={this.state.newRule.pt} className="width-100" onChange={(e) => this.setRule("pt", e)}>
                                                <option value="Required">Required</option>
                                                <option value="Optional">Optional</option>
                                            </select>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td>Data Type</td>
                                        <td>{this.formatDtValue(this.state.defaultRule.dt)}</td>
                                        <td>
                                            <select value={this.state.newRule.dt} className="width-100" onChange={(e) => this.setRule("dt", e)}>
                                                <option value="Default">Default</option>
                                                <option value="Str">Str</option>
                                                <option value="Int">Int</option>
                                                <option value="Float">Float</option>
                                                <option value="RptArray">List [array]</option>
                                                <option value="Set">Set [array]</option>
                                                <option value="NrptArray">Unstructured Array</option>
                                                <option value="Obj">Obj</option>
                                            </select>
                                        </td>
                                    </tr>
                                    {
                                        this.state.newRule.dt == "Set" &&
                                        <tr>
                                        <td>Match Criteria</td>
                                        <td>{this.state.defaultRule.arrayCompKeyPath}</td>
                                        <td>
                                            <input type="text" value={this.state.newRule.arrayCompKeyPath} className="width-100" onChange={(e) => this.setRule("arrayCompKeyPath", e)} />
                                        </td>
                                    </tr>

                                    }
                                    
                                    <tr>
                                        <td>Transformation</td>
                                        <td>{this.state.defaultRule.em}</td>
                                        <td>
                                            <select value={this.state.newRule.em} className="width-100" onChange={(e) => this.setRule("em", e)}>
                                                <option value="Default">Default</option>
                                                <option value="Regex">Regex</option>
                                                <option value="Round">Round</option>
                                                <option value="Floor">Floor</option>
                                                <option value="Ceil">Ceil</option>
                                            </select>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td>Comparison Type</td>
                                        <td>{this.state.defaultRule.ct}</td>
                                        <td>
                                            <select value={this.state.newRule.ct} className="width-100" onChange={(e) => this.setRule("ct", e)}>
                                                <option value="Equal">Equal</option>
                                                <option value="Ignore">Ignore</option>
                                            </select>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        <div className="margin-top-10 margin-bottom-10">
                            <span>Comments:</span><br />
                            <textarea style={{ height: "50px", width: "100%" }}></textarea>
                        </div>

                        <div className="text-right margin-top-20">
                            <span onClick={() => this.updateRule("REPLACE")} className="cube-btn font-12">APPLY</span>&nbsp;&nbsp;
                            {/* <span onClick={this.hideGR} className="cube-btn font-12">CANCEL</span> */}
                        </div>
                    </div>
                </div>

                <div className={this.state.showGolden ? "update-golden" : "hidden"} style={{ color: "#333333" }}>
                    <div onClick={this.hideGR} style={{ maxWidth: "400px", background: "#D5D5D5", padding: "5px 20px" }}>
                        UPDATE GOLDEN
                    </div>
                    <div style={{ width: "300px", background: "#ECECE7", padding: "15px 20px", textAlign: "left" }}>
                        <div>Path:&nbsp;<b>{this.props.jsonPath.replace("<BEGIN>", "")}</b></div>
                        <div>Data Type:&nbsp;<b>{this.formatDtValue(this.state.newRule.dt)}</b></div>
                        <div>Count of similar items:&nbsp;<b>105</b></div> 
                        {/* TODO: Above value is hardcoded. Find out more */}
                        <div className="text-center margin-top-20">
                            <span onClick={this.updateGolden}
                                className="cube-btn font-12">MARK FOR UPDATE</span>&nbsp;&nbsp;
                            {/* <span onClick={this.hideGR} className="cube-btn font-12">CANCEL</span> */}
                        </div>
                    </div>
                </div>

                <div className={this.state.showBug ? "update-rule" : "hidden"} style={{ color: "#333333" }}>
                    <div onClick={this.hideGR} style={{ width: "500px", background: "#D5D5D5", padding: "5px 20px" }}>
                        CREATE JIRA ISSUE
                    </div>
                    <div style={{ width: "500px", background: "#ECECE7", padding: "15px 20px", textAlign: "left" }}>

                        <div>Path:&nbsp;<b>{this.props.jsonPath}</b></div>

                        <div className="table-responsive margin-top-10">
                            <table className="table table-striped" style={{ textAlign: "left" }}>
                                <tbody>
                                    <tr>
                                        <td>Summary</td>
                                        <td>
                                            {this.renderSummary()}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>Description</td>
                                        {this.renderDescription()}
                                    </tr>

                                    <tr>
                                        <td>Project</td>
                                        <td>
                                            {this.renderProjectList()}
                                        </td>
                                    </tr>

                                    <tr>
                                        <td>Issue Type</td>
                                        <td><b>Bug</b></td>
                                    </tr>

                                </tbody>
                            </table>
                        </div>

                        <div className="text-right margin-top-20">
                            <span onClick={this.createIssue} className="cube-btn font-12">CREATE</span>&nbsp;&nbsp;
                            {/* <span onClick={this.hideGR} className="cube-btn font-12">CANCEL</span> */}
                        </div>
                    </div>
                </div>
                <div className={this.state.showBugResponse ? "update-golden" : "hidden"} style={{ color: "#333333" }}>
                    <div onClick={this.hideGR} style={{ maxWidth: "400px", background: "#D5D5D5", padding: "5px 20px" }}>
                        CREATE JIRA ISSUE
                    </div>
                    <div style={{ width: "300px", background: "#ECECE7", padding: "15px 20px", textAlign: "left" }}>
                        <div><b>Jira Issue&nbsp;</b>
                            <p>
                                <a onClick={this.closeTippy} style={{cursor: "pointer"}} href={this.state.jiraIssueURL} target="_">
                                    <p>{this.state.jiraIssueKey}</p>
                                </a>
                            </p>
                        </div>
                        <div className="text-center margin-top-20">
                            <span onClick={this.hideGR} className="cube-btn font-12">CLOSE</span>
                        </div>
                    </div>
                </div>
                <div className={this.state.showJiraError ? "update-golden" : "hidden"} style={{ color: "#333333", padding: "10px 5px", maxWidth: "400px" }}>
                    <div>
                        <b>Jira API Error&nbsp;</b>
                        <p>{this.state.jiraErrorMessage}</p>
                        <div className="text-center">
                            <span onClick={this.hideGR} className="cube-btn font-12">CLOSE</span>
                        </div>
                    </div>
                </div>
            </React.Fragment>
        );
    }
}

const mapStateToProps = (state) => ({
    user: state.authentication.user,
    cube: state.cube
});

const connectedGoldenPopover = connect(mapStateToProps)(GoldenPopover);

export default connectedGoldenPopover;
