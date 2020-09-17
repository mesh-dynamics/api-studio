import React, { Component, Fragment } from 'react'
import { connect } from "react-redux";
import {Modal,Grid, Row, Col } from 'react-bootstrap';
import { httpClientActions } from '../../actions/httpClientActions';
import _ from "lodash";

class EnvVar extends Component {
    constructor(props) {
        super(props)
        this.state = {
            selectedEnv: {},
            addNew: false,
        }
    }

    
    handleEnvRowClick = (index) => {
        const {httpClient: {
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
        dispatch(httpClientActions.showEnvList(show));
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
        const {httpClient: {
            environmentList
        }, dispatch} = this.props;
        const {id, name} = environmentList[index];
        dispatch(httpClientActions.removeEnvironment(id, name))
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
        dispatch(httpClientActions.saveEnvironment(selectedEnv));
    }

    handleUpdateEnvironment = () => {
        const {dispatch} = this.props;
        const {selectedEnv} = this.state;
        if (_.isEmpty(selectedEnv.name)) {
            this.setEnvStatusText("Environment name cannot be empty", true)
            return
        }
        dispatch(httpClientActions.updateEnvironment(selectedEnv));
    }

    setEnvStatusText = (text, isError) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.setEnvStatusText(text, isError))
    }

    resetEnvStatusText = () => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.resetEnvStatusText())
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
        const {httpClient: {
            environmentList, envStatusText, envStatusIsError, showEnvList
        }} = this.props;
        return (
            <Fragment>
                <Modal.Header closeButton>
                    Configure Environments
                </Modal.Header>
                <Modal.Body>
                    <div style={{height: "400px", overflowY: "scroll"}}>
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
                        {!showEnvList && 
                        <Grid>
                            <Row>
                                <Col xs={2}>
                                    <label style={{ marginTop: "8px" }}>Environment Name: </label>
                                </Col>
                                <Col xs={6}>
                                    <input value={selectedEnv["name"]} onChange={this.handleSelectedEnvNameChange} class="form-control"/>
                                </Col>  
                            </Row>
                            
                                <Row className="show-grid margin-top-15">
                                    <Col xs={5}>
                                        <b>Variable</b>
                                    </Col>
                                    <Col xs={5}>
                                        <b>Value</b>
                                    </Col>
                                </Row>
                                {(selectedEnv.vars || [])
                                    .map(({key, value}, index) => (
                                            <Row className="show-grid margin-top-10">
                                                <Col xs={5}>
                                                    <input value={key} onChange={(e) => this.handleEnvVarKeyChange(e, index)} class="form-control"/>
                                                </Col>
                                                <Col xs={6}>
                                                    <input value={value} onChange={(e) => this.handleEnvVarValueChange(e, index)} class="form-control"/>
                                                </Col>
                                                <Col xs style={{marginTop: "5px"}}>
                                                    <span  onClick={() => this.handleRemoveEnvVariable(index)}>
                                                        <i className="fas fa-times pointer"/>
                                                    </span>
                                                </Col>
                                            </Row>
                                    )
                                )}                                    
                                <Row className="show-grid margin-top-15">
                                    <Col xs={3}>
                                        <div onClick={this.handleAddNewEnvVariable} className="pointer btn btn-sm cube-btn text-center">
                                            <i className="fas fa-plus" style={{marginRight: "5px"}}></i><span>Add new variable</span>
                                        </div>
                                    </Col>
                                </Row>
                        </Grid>
                        }
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

const mapStateToProps = (state) =>  ({httpClient: state.httpClient});

export default connect(mapStateToProps)(EnvVar);

