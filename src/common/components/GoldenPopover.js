import React from "react";
import {connect} from "react-redux";
import {cubeActions} from "../actions";
import config from "../config";

class GoldenPopover extends React.Component {
    constructor(props) {
        super(props);
        this.showGoldenModal = this.showGoldenModal.bind(this);
        this.showRuleModal = this.showRuleModal.bind(this);
        this.updateGolden = this.updateGolden.bind(this);
        this.updateRule = this.updateRule.bind(this);
        this.setRule = this.setRule.bind(this);
        this.hideGR = this.hideGR.bind(this);

        this.state = {
            showGolden: false,
            showRule: false,
            defaultRule: {
                "path": this.props.jsonPath.replace("<BEGIN>", ""),
                "dt": "",
                "pt": "",
                "ct": "",
                "em": "",
                "customization": null
            },
            newRule: {
                "path": this.props.jsonPath.replace("<BEGIN>", ""),
                "dt": "",
                "pt": "",
                "ct": "",
                "em": "",
                "customization": null
            }
        };
    }

    setRule(tag, evt) {
        let {newRule} = this.state;
        newRule[tag] = evt.target.value;
        this.setState({newRule: newRule});
    }

    updateRule() {
        const {dispatch, serverSideDiff, jsonPath} = this.props;
        if (serverSideDiff) {
            this.hideGR();
            let obj = {};
            obj.type = "REPLACE";
            obj.path = jsonPath.replace("<BEGIN>", "");
            obj.newRule = this.state.newRule;
            dispatch(cubeActions.pushToOperations(obj));
        } else {
            this.hideGR();
            alert("Can't update rule for this line");
        }
    }

    updateGolden() {
        const {dispatch, serverSideDiff, cube} = this.props;
        if (serverSideDiff) {
            let operation = {
                op: serverSideDiff.op.toUpperCase(),
                path: serverSideDiff.path,
                value: serverSideDiff.value
            };
            this.hideGR();
            dispatch(cubeActions.pushToOperationSet(operation));
        } else {
            this.hideGR();
            alert("Can't update golden for this line");
        }
    }

    showGoldenModal() {
        this.setState({showGolden: true});
    }

    showRuleModal() {
        this.getResponseTemplate();
        this.setState({showRule: true});
    }

    hideGR() {
        this.setState({showRule: false, showGolden: false});
    }

    render() {
        return (
            <React.Fragment>
                <div className={!this.state.showGolden && !this.state.showRule ? "text-center" : "hidden"}
                     style={{color: "#333333"}}>
                    <div style={{width: "300px", height: "100px", background: "#D5D5D5", padding: "20px"}}>
                        <div className="margin-bottom-10">STATUS</div>
                        <div>
                            <span>{this.props.serverSideDiff && this.props.serverSideDiff.resolution ? this.props.serverSideDiff.resolution : "OK"}</span>
                        </div>
                    </div>
                    <div style={{width: "300px", height: "100px", background: "#ECECE7", padding: "15px"}}>
                        <div>
                            <span className="back-grey"><i className="fas fa-bug"></i></span>&nbsp;&nbsp;
                            <span className="back-grey"><i className="fas fa-comments"></i></span>&nbsp;&nbsp;
                            <span className="back-grey"><i className="fas fa-code"></i></span>&nbsp;&nbsp;
                            <span className="back-grey"><i className="fas fa-share-alt"></i></span>
                        </div>
                        <div className="margin-top-15">
                            <span onClick={this.showGoldenModal}
                                  className="cube-btn font-12">UPDATE GOLDEN</span>&nbsp;&nbsp;
                            <span onClick={this.showRuleModal} className="cube-btn font-12">UPDATE RULE</span>
                        </div>
                    </div>
                </div>

                <div className={this.state.showRule ? "update-rule" : "hidden"} style={{color: "#333333"}}>
                    <div onClick={this.hideGR} style={{width: "500px", background: "#D5D5D5", padding: "5px 20px"}}>
                        UPDATE ASSERTION RULE
                    </div>
                    <div style={{width: "500px", background: "#ECECE7", padding: "15px 20px", textAlign: "left"}}>
                        <div>Path:&nbsp;<b>{this.props.jsonPath}</b></div>
                        <div>Data Type:&nbsp;<b>{this.state.newRule.dt}</b></div>
                        <div>Count of similar items:&nbsp;<b>105</b></div>

                        <div className="table-responsive margin-top-10">
                            <table className="table table-striped" style={{textAlign: "left"}}>
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
                                    <td>{this.state.defaultRule.dt}</td>
                                    <td>
                                        <select value={this.state.newRule.dt} className="width-100" onChange={(e) => this.setRule("dt", e)}>
                                            <option value="Default">Default</option>
                                            <option value="Str">Str</option>
                                            <option value="Int">Int</option>
                                            <option value="Float">Float</option>
                                            <option value="RptArray">RptArray</option>
                                            <option value="NrptArray">NrptArray</option>
                                            <option value="Obj">Obj</option>
                                        </select>
                                    </td>
                                </tr>

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
                                    <td>Comparision Type</td>
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
                            <span>Comments:</span><br/>
                            <textarea style={{height: "50px", width: "100%"}}></textarea>
                        </div>

                        <div className="text-right margin-top-20">
                            <span onClick={this.updateRule} className="cube-btn font-12">APPLY</span>&nbsp;&nbsp;
                            <span onClick={this.hideGR} className="cube-btn font-12">CANCEL</span>
                        </div>
                    </div>
                </div>

                <div className={this.state.showGolden ? "update-golden" : "hidden"} style={{color: "#333333"}}>
                    <div onClick={this.hideGR} style={{maxWidth: "400px", background: "#D5D5D5", padding: "5px 20px"}}>
                        UPDATE GOLDEN
                    </div>
                    <div style={{width: "300px", background: "#ECECE7", padding: "15px 20px", textAlign: "left"}}>
                        <div>Path:&nbsp;<b>{this.props.jsonPath.replace("<BEGIN>", "")}</b></div>
                        <div>Data Type:&nbsp;<b>{this.state.newRule.dt}</b></div>
                        <div>Count of similar items:&nbsp;<b>105</b></div>
                        <div className="text-center margin-top-20">
                            <span onClick={this.updateGolden}
                                  className="cube-btn font-12">MARK FOR UPDATE</span>&nbsp;&nbsp;
                            <span onClick={this.hideGR} className="cube-btn font-12">CANCEL</span>
                        </div>
                    </div>
                </div>
            </React.Fragment>
        );
    }

    async getResponseTemplate() {
        let user = JSON.parse(localStorage.getItem('user'));
        let {cube, jsonPath} = this.props;
        jsonPath = jsonPath.replace("<BEGIN>", "");
        let response, json;
        let url = `${config.analyzeBaseUrl}/getRespTemplate/${user.customer_name}/${cube.selectedApp}/${cube.pathResultsParams.currentTemplateVer}/${cube.pathResultsParams.service}?apipath=${cube.pathResultsParams.path}&jsonpath=${jsonPath}`;

        let newRule = {};
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
                newRule = json;
            } else {
                console.log("Response not ok in fetchTimeline", response);
                throw new Error("Response not ok fetchTimeline");
            }
        } catch (e) {
            console.log("fetchTimeline has errors!", e);
            throw e;
        }
        this.setState({ defaultRule: {...newRule}, newRule: {...newRule} });
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedGoldenPopover = connect(mapStateToProps)(GoldenPopover);

export default connectedGoldenPopover;
