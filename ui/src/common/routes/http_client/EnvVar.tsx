/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { Component, Fragment } from 'react'
import { connect } from "react-redux";
import {Modal,Grid, Row, Col } from 'react-bootstrap';
import { httpClientActions } from '../../actions/httpClientActions';
import _ from "lodash";
import { IHttpClientStoreState, IEnvironmentConfig, IStoreState } from '../../reducers/state.types';

export interface IEnvVarProps{
    httpClient: IHttpClientStoreState;
    selectedEnv: IEnvironmentConfig,
    dispatch:any;
    updateSelectedEnv: (selectedEnv: IEnvironmentConfig) => void;
    updateEnvState: (selectedEnv: IEnvironmentConfig, flag: boolean) => void;
    hideModal: ()=> void;
}
class EnvVar extends Component <IEnvVarProps>{

    componentWillUnmount() {
        this.showEnvList(true)
    }


    showEnvList = (show: boolean) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.showEnvList(show));
    };

    handleEnvRowClick = (index) => {
        const {
            updateEnvState,
            httpClient: {
                    environmentList
                }
        } = this.props;
        
        const selectedEnv: IEnvironmentConfig = {...environmentList[index]};

        this.showEnvList(false);
        updateEnvState(selectedEnv, false); // this.setState({selectedEnv: selectedEnv, addNewEnv: false})
    }

    handleRemoveEnv = (index) => {
        const {
            dispatch,
            httpClient: {
                environmentList
            }
        } = this.props;
        const {id, name} = environmentList[index];

        dispatch(httpClientActions.removeEnvironment(id, name))
    }

    handleAddNewEnv = () => {
        const {
                cube: { 
                    selectedAppObj 
                },
                updateEnvState,
            } = this.props;

        let selectedEnv: IEnvironmentConfig = {
            name: "",
            appId: selectedAppObj.id,
            vars: [],
        }
        this.showEnvList(false)
        updateEnvState(selectedEnv, true); // this.setState({selectedEnv, addNewEnv: true})
    }

    handleSelectedEnvNameChange = (e) => {
        const { selectedEnv, updateSelectedEnv } = this.props;
        
        selectedEnv.name = e.target.value;
        
        updateSelectedEnv(selectedEnv); // this.setState({selectedEnv})
    }

    handleEnvVarKeyChange = (e, index) => {
        const { selectedEnv, updateSelectedEnv } = this.props;
        selectedEnv.vars[index].key = e.target.value;
        updateSelectedEnv(selectedEnv); // this.setState({ selectedEnv })
    }

    handleEnvVarValueChange = (e, index) => {
        const { selectedEnv, updateSelectedEnv } = this.props;
        selectedEnv.vars[index].value = e.target.value;
        updateSelectedEnv(selectedEnv); // this.setState({selectedEnv})
    }

    handleRemoveEnvVariable = (index) => {
        const { selectedEnv, updateSelectedEnv } = this.props;
        selectedEnv.vars.splice(index, 1);
        updateSelectedEnv(selectedEnv); // this.setState({selectedEnv})
    }

    handleAddNewEnvVariable = () => {
        const {selectedEnv, updateSelectedEnv} = this.props;
        
        selectedEnv.vars.push({ key: "", value: "" });

        updateSelectedEnv(selectedEnv); // this.setState({selectedEnv})
    }


    render() {
        const { 
            selectedEnv, 
            httpClient: {
                environmentList, 
                showEnvList 
            }
        } = this.props;
        
        return (
            <Fragment>
                {
                    showEnvList &&
                    <table className="table table-hover">
                        <tbody>
                            {environmentList.map((environment, index) => (
                                <tr key={index}>
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
                }
                {
                !showEnvList && 
                    <Grid>
                        <Row>
                            <Col xs={2}>
                                <label style={{ marginTop: "8px" }}>Environment Name: </label>
                            </Col>
                            <Col xs={6}>
                                <input value={selectedEnv["name"]} onChange={this.handleSelectedEnvNameChange} className="form-control"/>
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
                                        <Row className="show-grid margin-top-10" key={index}>
                                            <Col xs={5}>
                                                <input value={key} onChange={(e) => this.handleEnvVarKeyChange(e, index)} className="form-control"/>
                                            </Col>
                                            <Col xs={6}>
                                                <input value={value} onChange={(e) => this.handleEnvVarValueChange(e, index)} className="form-control"/>
                                            </Col>
                                            <Col xs={1} style={{marginTop: "5px"}}>
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
            </Fragment>
        )
    }
}

const mapStateToProps = (state: IStoreState) =>  ({httpClient: state.httpClient, cube: state.cube});

export default connect(mapStateToProps)(EnvVar);