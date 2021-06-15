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

import React, { Component, Fragment } from 'react';
import { connect } from "react-redux";
import { history } from '../../../helpers';
import "./TestConfig.css"
import { cubeActions } from "../../../actions";
import { Link } from "react-router-dom";
import { ICubeState, IStoreState, ITestConfigDetails } from '../../../../common/reducers/state.types';
import EditTestConfig from './EditTestConfig';
import { Modal } from 'react-bootstrap';
import { configsService } from '../../../../common/services/configs.service';
export interface ITestConfigProps {
    dispatch: any,
    cube: ICubeState
}
export interface ITestConfigState {
    inEditMode: boolean,
    testConfigIdToUpdate: number,
    testConfigIdToDelete: number
}
class TestConfig extends Component<ITestConfigProps, ITestConfigState> {

    constructor(props: ITestConfigProps) {
        super(props);
        this.state = {
            inEditMode: false,
            testConfigIdToUpdate: 0,
            testConfigIdToDelete: 0,
        }
    }


    setTestConfig(config: ITestConfigDetails) {
        const { dispatch } = this.props;
        dispatch(cubeActions.setTestConfig(config));
        setTimeout(() => {
            history.push("/test_config_view");
        })
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(true));
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(false));
    }

    createTestConfig(cube: ICubeState) {
        if (!cube.testConfigList || cube.testConfigList.length == 0) {
            return 'No Test Config Found'
        }
        let jsxContent = cube.testConfigList.map((item) => {
            return (
                <div key={item.id} className="grid-content" onClick={() => this.setTestConfig(item)}>
                    <div className={cube.testConfig && cube.testConfig.testConfigName == item.testConfigName ? "g-head selected" : "g-head"}>
                        {item.testConfigName}
                        {(cube.testConfig && cube.testConfig.testConfigName == item.testConfigName) ? null : <span className="margin-left-5 pull-right link"><i className="fas fa-trash" title="Delete" onClick={this.deleteTestConfig(item.id)} /></span>}
                        <span className=" pull-right link"><i className="fas fa-edit" title="Edit" onClick={this.addOrUpdateTestConfig(item.id)} /></span>
                    </div>

                    <div className="g-body">
                        {/* <div className="margin-bottom-10">
                            <span className="t-name">
                                GATEWAY:&nbsp;
                            </span>
                            <strong>{item.gatewayServiceName ? item.gatewayServiceName : ""}</strong>
                        </div> */}

                        <div className="margin-bottom-10">
                            <span className="t-name">
                                Paths:&nbsp;
                            </span>
                            <strong>{item.paths && item.paths.length > 0 ? item.paths.join(', ') : ""}</strong>
                        </div>

                        <div className={item.criteria ? "margin-bottom-10" : "hidden"}>
                            <span className="t-name">
                                CRITERIA:&nbsp;
                            </span>
                            <strong>{item.criteria}</strong>
                        </div>

                        {item.testServices && item.testServices.length > 0 && <div className="margin-bottom-10">
                            <span className="t-name">
                                Test Service(s):&nbsp;
                            </span>
                            <strong>{item.testServices && item.testServices.length > 0 ? item.testServices.join(', ') : ""}</strong>
                        </div>}

                        <div className="margin-bottom-10">
                            <span className="t-name">
                                MOCK(s):&nbsp;
                            </span>
                            <strong>{item.mocks && item.mocks.length > 0 ? item.mocks.join(', ') : ""}</strong>
                        </div>
                    </div>
                </div>
            );
        });

        return jsxContent;
    }

    addOrUpdateTestConfig = (testConfigId: number) => {
        return (event: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
            this.setState({ inEditMode: true, testConfigIdToUpdate: testConfigId });
            event.stopPropagation();
        }

    }
    deleteTestConfig = (testConfigId: number) => {
        return (event: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
            this.setState({ testConfigIdToDelete: testConfigId });
            event.stopPropagation();
        }
    }

    onExitEditMode = (refresh: boolean) => {
        this.setState({ inEditMode: false });
        const appId = this.props.cube.selectedAppObj?.app.id;
        if (refresh && appId) {
            this.props.dispatch(cubeActions.getTestConfigByAppId(appId));
        }
    }

    confirmDelete = () => {
        configsService.deleteTestConfig(this.state.testConfigIdToDelete).then(data => {
            const appId = this.props.cube.selectedAppObj?.app.id;
            this.props.dispatch(cubeActions.getTestConfigByAppId(appId));
            this.setState({ testConfigIdToDelete: 0 });
        }).catch(error => {
            alert("Some error occurred while deleting. Please see console for more details");
            console.error(error);
        })
    }
    dismissHandler = () => {
        this.setState({ testConfigIdToDelete: 0 });
    }

    confirmationBox = () => {
        return (<Modal show={true} onHide={this.dismissHandler}>
            <Modal.Header>
                <Modal.Title>Confirm delete</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <div>
                    Please confirm to delete selected test-config. Once deleted, it can not be retrieved.
                    </div>
            </Modal.Body>
            <Modal.Footer>
                <span onClick={this.confirmDelete} className="cube-btn margin-right-10">Confirm</span>
                <span onClick={this.dismissHandler} className="cube-btn">Cancel</span>
            </Modal.Footer>
        </Modal>);
    }

    render() {
        const { cube } = this.props;
        if (this.state.inEditMode) {
            return <EditTestConfig testConfigId={this.state.testConfigIdToUpdate} onClose={this.onExitEditMode} />
        }
        return (
            <Fragment>
                {this.state.testConfigIdToDelete ? this.confirmationBox() : null}
                {/*<div className="crumb-wrap">
                    <Breadcrumb crumbs={[{label: "Application", value: "MovieInfo"}, {label: "Service", value: "List"},
                        {label: "Logical Service", value: "Reviews"}, {label: "API", value: "IMDb"}]}></Breadcrumb>
                </div>*/}
                {/* <div>
                    <Link to="/test_config_setup">
                        <span className="cube-btn">CONFIGURE NEW TEST</span>
                    </Link>
                </div> */}

                <div className="tc-grid">
                    {this.createTestConfig(cube)}
                    <div className="grid-content add-grid" onClick={this.addOrUpdateTestConfig(0)}>
                        <p><i className="fas fa-plus" style={{ color: "#333333", marginRight: "5px" }} aria-hidden="true" /> Add</p>
                    </div>
                </div>
            </Fragment>
        );
    }
}

function mapStateToProps(state: IStoreState) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedTestConfig = connect(mapStateToProps)(TestConfig);

export default connectedTestConfig
