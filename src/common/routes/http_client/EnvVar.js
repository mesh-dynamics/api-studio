import React, { Component, Fragment } from 'react'
import { apiCatalogActions } from '../../actions/api-catalog.actions'
import { connect } from "react-redux";
import {Modal} from 'react-bootstrap';
import _, { isError } from 'lodash';
import { cubeService } from '../../services';


class EnvVar extends Component {
    constructor(props) {
        super(props)
        this.state = {
            selectedEnv: {},
            addNew: false,
        }
    }

    
    handleEnvRowClick = (index) => {
        const {apiCatalog: {
            environmentList
        }} = this.props;
        this.showEnvList(false)
        const selectedEnv = {...environmentList[index]}
        this.setState({selectedEnv: selectedEnv, addNew: false})
    }

    handleEnvVarKeyChange = (e, index) => {
        const {selectedEnv} = this.state;
        selectedEnv.vars[index].key = e.target.value;
        this.setState({selectedEnv})
    }

    handleEnvVarValueChange = (e, index) => {
        const {selectedEnv} = this.state;
        selectedEnv.vars[index].value = e.target.value;
        this.setState({selectedEnv})
    }

    handleSelectedEnvNameChange = (e) => {
        const {selectedEnv} = this.state;
        selectedEnv.name = e.target.value;
        this.setState({selectedEnv})
    }

    handleAddNewEnv = () => {
        let selectedEnv = {
            name: "",
            vars: [],
        }
        this.showEnvList(false)
        this.setState({selectedEnv, addNew: true})
    }

    showEnvList = (show) => {
        const {dispatch} = this.props;
        dispatch(apiCatalogActions.showEnvList(show));
    }

    handleAddNewEnvVariable = () => {
        let {selectedEnv} = this.state;
        selectedEnv.vars.push({
            key: "",
            value: "",
        })
        this.setState({selectedEnv})
    }

    handleRemoveEnv = (index) => {
        const {apiCatalog: {
            environmentList
        }, dispatch} = this.props;
        
        dispatch(apiCatalogActions.removeEnvironment(environmentList[index].id))
    }

    handleRemoveEnvVariable = (index) => {
        let {selectedEnv} = this.state;
        selectedEnv.vars.splice(index, 1)
        this.setState({selectedEnv})
    }

    handleSaveEnvironment = () => {
        const {dispatch} = this.props;
        const {selectedEnv} = this.state;
        if (_.isEmpty(selectedEnv.name)) {
            this.setEnvStatusText("Environment name cannot be empty", true)
            return
        }
        dispatch(apiCatalogActions.saveEnvironment(selectedEnv));
    }

    handleUpdateEnvironment = () => {
        const {dispatch} = this.props;
        const {selectedEnv} = this.state;
        if (_.isEmpty(selectedEnv.name)) {
            this.setEnvStatusText("Environment name cannot be empty", true)
            return
        }
        dispatch(apiCatalogActions.updateEnvironment(selectedEnv));
    }

    setEnvStatusText = (text, isError) => {
        const {dispatch} = this.props;
        dispatch(apiCatalogActions.setEnvStatusText(text, isError))
    }

    resetEnvStatusText = () => {
        const {dispatch} = this.props;
        dispatch(apiCatalogActions.resetEnvStatusText())
    }

    handleBackEnv = () => {
        this.resetEnvStatusText()
        this.showEnvList(true)
    }

    componentWillUnmount() {
        this.showEnvList(true)
    }

    render() {
        const {selectedEnv, addNew} = this.state;
        const {apiCatalog: {
            environmentList, envStatusText, envStatusIsError, showEnvList
        }} = this.props;
        return (
            <Fragment>
                <Modal.Header closeButton>
                    Configure Environments
                </Modal.Header>
                <Modal.Body>
                    <div style={{height: "300px", overflowY: "scroll"}}>
                        {showEnvList && <div>
                            <label>Environments</label>
                            <table className="table table-hover">
                                <tbody>
                                    {environmentList.map((environment, index) => (
                                        <tr>
                                            <td style={{cursor: "pointer"}} onClick={() => this.handleEnvRowClick(index)}>
                                                {environment.name}
                                            </td>
                                            <td style={{width: "10%", textAlign: "right"}}>
                                                <i className="fas fa-trash pointer" onClick={() => this.handleRemoveEnv(index)}/>
                                            </td>
                                        </tr>)
                                    )}
                                    <tr>
                                        <td onClick={this.handleAddNewEnv} className="pointer">
                                            <i className="fas fa-plus" style={{marginRight: "5px"}}></i><span>Add new environment</span>
                                        </td>
                                        <td></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>}
                        {!showEnvList && <div>
                            <label>Environment Name: </label> <input type="text" value={selectedEnv["name"]} onChange={this.handleSelectedEnvNameChange}></input>
                            <table className="table table-hover table-bordered">
                                <thead>
                                    <tr>
                                        <th>Variable</th>
                                        <th>Value</th>
                                        <th></th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {(selectedEnv.vars || [])
                                        .map(({key, value}, index) => (
                                        <tr>
                                            <td>
                                                <input value={key} onChange={(e) => this.handleEnvVarKeyChange(e, index)}/>
                                            </td>
                                            <td>
                                                <input value={value} onChange={(e) => this.handleEnvVarValueChange(e, index)}/>
                                            </td>
                                            <td style={{textAlign: "center", verticalAlign: "middle"}} onClick={() => this.handleRemoveEnvVariable(index)}>
                                                <i className="fas fa-times pointer"/>
                                            </td>
                                        </tr>)
                                    )}
                                    <tr>
                                        <td onClick={this.handleAddNewEnvVariable} className="pointer">
                                            <i className="fas fa-plus" style={{marginRight: "5px"}}></i><span>Add new variable</span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <span className="pull-left" style={{color: envStatusIsError ? "red" : ""}}>{envStatusText}</span>
                    {showEnvList && <span className="cube-btn margin-left-15" onClick={this.props.hideModal}>DONE</span>}
                    {!showEnvList && <span className="cube-btn margin-left-15" onClick={this.handleBackEnv}>BACK</span>}
                    {!showEnvList && addNew && <span className="cube-btn margin-left-15" onClick={this.handleSaveEnvironment}>SAVE</span>}
                    {!showEnvList && !addNew && <span className="cube-btn margin-left-15" onClick={this.handleUpdateEnvironment}>UPDATE</span>}
                </Modal.Footer>
            </Fragment>
        )
    }
}

const mapStateToProps = (state) =>  ({apiCatalog: state.apiCatalog});

export default connect(mapStateToProps)(EnvVar);

