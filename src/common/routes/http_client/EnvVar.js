import React, { Component, Fragment } from 'react'
import { apiCatalogActions } from '../../actions/api-catalog.actions'
import { connect } from "react-redux";
import {Modal} from 'react-bootstrap';
import _ from 'lodash';


class EnvVar extends Component {
    constructor(props) {
        super(props)
        this.state = {
            environments: [],
            selectedEnvIndex: 0,
            selectedEnv: {},
            showEnvList: true,
        }
    }

    componentDidMount() {
        const {apiCatalog} = this.props;
        this.setState({environments: apiCatalog.environmentList})
    }
    
    handleEnvRowClick = (index) => {
        this.setState({selectedEnvIndex: index, showEnvList: false, selectedEnv: this.state.environments[index]})
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
        let {environments} = this.state;
        environments.push({
            name: "",
            vars: [],
        })
        let selectedEnvIndex = environments.length - 1
        let selectedEnv = environments[selectedEnvIndex]

        this.setState({environments, selectedEnvIndex, selectedEnv, showEnvList: false})
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
        let {environments} = this.state;
        environments.splice(index, 1)
        this.setState({environments})
    }

    handleRemoveEnvVariable = (index) => {
        let {selectedEnv} = this.state;
        selectedEnv.vars.splice(index, 1)
        this.setState({selectedEnv})
    }

    handleSaveEnvironments = () => {
        const {dispatch, hideModal} = this.props;
        dispatch(apiCatalogActions.updateEnvironments(this.state.environments))
        hideModal();
    }

    handleDoneEnv = () => {
        const {selectedEnv} = this.state;
        if (_.isEmpty(selectedEnv.name)) {
            alert("Environment name cannot be empty")
            return
        }
        this.setState({showEnvList: true})
    }

    render() {
        const {environments, selectedEnvIndex, showEnvList, selectedEnv} = this.state;
        return (
            <Fragment>
                <Modal.Header closeButton>
                    Configure Environment
                </Modal.Header>
                <Modal.Body>
                    <div style={{height: "300px", overflowY: "scroll"}}>
                        {showEnvList && <div>
                            <label>Environments</label>
                            <table className="table table-hover">
                                <tbody>
                                    {environments.map((environment, index) => (
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
                    {showEnvList && <span className="cube-btn margin-left-15" onClick={this.handleSaveEnvironments}>SAVE</span>}
                    {!showEnvList && <span className="cube-btn margin-left-15" onClick={this.handleDoneEnv}>DONE</span>}
                </Modal.Footer>
            </Fragment>
        )
    }
}

const mapStateToProps = (state) =>  ({apiCatalog: state.apiCatalog});

export default connect(mapStateToProps)(EnvVar);

