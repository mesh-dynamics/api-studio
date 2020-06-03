import React, { Component } from "react";
import { connect } from "react-redux";
import { Glyphicon } from "react-bootstrap";
import Modal from "react-bootstrap/lib/Modal";
import GoldenPopover from "../GoldenPopover";
import { cubeActions } from "../../actions";

class DiffResultsMissingItems extends Component{
    constructor(props) {
        super(props);
        this.state = {
            showPopover: false,
            showBugTippy: false,
        };
    }

    handleShowPopoverClick = () => this.setState({ showPopover: true });

    handleHidePopoverClick = () => this.setState({ showPopover: false });

    setShowBugTippy = () => this.setState({ showBugTippy: true });

    setHideBugTippy = () => this.setState({ showBugTippy: false });

    getIssueId = (jsonPath) => {
        const { cube: { jiraBugs } } = this.props;
        if(jsonPath) {
            const bugItem = jiraBugs.find(item => item.jsonPath === jsonPath.replace("<BEGIN>", ""));

            return  jiraBugs.length > 0 && bugItem ? bugItem.issueKey : "";
        }
        
        return;
    };

    getIssueUrl = (jsonPath) => {
        const { cube: { jiraBugs } } = this.props;
        
        if(jsonPath) {
            const bugItem = jiraBugs.find(item => item.jsonPath === jsonPath.replace("<BEGIN>", ""));

            return  jiraBugs.length > 0 && bugItem ? bugItem.issueUrl : "";
        }

        return;
    }

    handleIssueUrlClick = (jsonPath) => {
        this.setHideBugTippy();
        window.open(this.getIssueUrl(jsonPath))
    }

    findInJiraBugs = (jsonPath) => {
        const { cube } = this.props;
        for (const op of cube.jiraBugs) {
            if (jsonPath.replace("<BEGIN>", "") == op.jsonPath) {
                return true;
            }
        }
        return false;
    }

    findInOperations = (jsonPath) => {
        const { cube } = this.props;
        for (let key in cube.templateOperationSetObject) {
            if (cube.templateOperationSetObject.hasOwnProperty(key) && cube.templateOperationSetObject[key].operations) {
                const keyObj = JSON.parse(key);
                if (keyObj['path'] == cube.pathResultsParams.path) {
                    for (const op of cube.templateOperationSetObject[key].operations) {
                        if (jsonPath.replace("<BEGIN>", "") == (op.path)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    };

    removeFromOperations = (jsonPath) => {
        const {cube, dispatch} = this.props;
        for (let key in cube.templateOperationSetObject) {
            if (cube.templateOperationSetObject.hasOwnProperty(key) && cube.templateOperationSetObject[key].operations) {
                const keyObj = JSON.parse(key);
                if (keyObj['path'] == cube.pathResultsParams.path) {
                    let opList = cube.templateOperationSetObject[key].operations;
                    for (let ind = 0; ind < opList.length; ind++) {
                        if (jsonPath.replace("<BEGIN>", "") == (opList[ind].path)) {
                            dispatch(cubeActions.removeFromRuleBook(jsonPath.replace("<BEGIN>", "")));
                            dispatch(cubeActions.removeFromOperations(ind, opList.length, key));
                            break;
                        }
                    }
                }
            }
        }
        return;
    };

    render(){
        const { missedRequiredFields } = this.props;

        return(

            <div style={{ padding: "10px 0" }}>
                <strong>Missing expected items in Test and Golden:</strong>
                {
                    missedRequiredFields.map(
                        (eachMissedField) => 
                            (
                                <div style={{ padding: "3px 0"}}>
                                    <Glyphicon style={{ color: "red" }} glyph="remove-circle" />
                                    {
                                        this.findInOperations(eachMissedField.path) &&
                                        <Glyphicon
                                            glyph="retweet" 
                                            style={{ 
                                                margin: "0 5px",
                                                cursor: "pointer",
                                            }} 
                                            onDoubleClick={() => this.removeFromOperations(eachMissedField.path)}
                                        />
                                    }
                                    {
                                        this.findInJiraBugs(eachMissedField.path) &&
                                        <span
                                            style={{ 
                                                    margin: "0 5px",
                                                    cursor: "pointer",
                                            }} 
                                            onClick={this.setShowBugTippy} 
                                        >
                                            <i className="fas fa-bug"></i>
                                        </span>
                                    }                                

                                    <span style={{ padding: "3px 0" }}>{eachMissedField.path}</span>
                                    
                                    {eachMissedField.fromValue && <span>` : ${eachMissedField.fromValue}`</span>}
                                    
                                    <Glyphicon 
                                        style={{ 
                                            marginLeft: "5px",
                                            cursor: "pointer",
                                        }} 
                                        glyph="plus" 
                                        onClick={this.handleShowPopoverClick}
                                    />
                                    <Modal show={this.state.showPopover} dialogClassName="os-popover-modal popover-golden">
                                        <div 
                                            className="os-popover-wrapper grey" 
                                            id={`tooltip-${this.props.jsonPath}`}
                                        >
                                            <GoldenPopover
                                                jsonPath={eachMissedField.path}
                                                eventType={eachMissedField.eventType}
                                                hideMarkForUpdate={true}
                                                showDeleteRuleAction={true}
                                                handleHidePopoverClick={this.handleHidePopoverClick} 
                                            />
                                        </div>
                                    </Modal>
                                    <Modal show={this.state.showBugTippy} dialogClassName="os-popover-modal">
                                        <div 
                                            style={{ 
                                                background: "#ECE7E6", 
                                                cursor: "default", 
                                                borderRadius: "3px", 
                                                padding: "3px" 
                                            }}
                                        >
                                            <span
                                                onClick={this.setHideBugTippy}
                                                style={{ 
                                                    display: "flex", 
                                                    justifyContent: "flex-end", 
                                                    padding: "3px", 
                                                    cursor: "pointer", 
                                                    width: "100%", 
                                                    fontSize: "12px"
                                                }}
                                            >
                                                <i className="fas fa-times" style={{ color: "#616060"}}></i>
                                            </span>
                                            <div style={{ 
                                                fontSize: "14px",
                                                display: "flex", 
                                                flexDirection: "column", 
                                                alignItems: "flex-start", 
                                                padding: "10px" 
                                            }}>
                                                <div>
                                                    <span>JSON Path: </span>
                                                    <span>{eachMissedField.path}</span>
                                                </div>
                                                <div>
                                                    <span>Jira Issue: </span>
                                                    <span 
                                                        style={{ 
                                                            cursor: "pointer", 
                                                            color: "#0052CC"
                                                        }} 
                                                        onClick={()=>this.handleIssueUrlClick(eachMissedField.path)}
                                                    >
                                                        {this.getIssueId(eachMissedField.path)}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    </Modal>
                                </div>
                            )
                    )
                }
            </div>
        )
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube
});

export default connect(mapStateToProps)(DiffResultsMissingItems);

