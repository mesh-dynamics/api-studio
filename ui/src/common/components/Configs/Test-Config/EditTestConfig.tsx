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

import React, { ChangeEvent, useCallback, useEffect, useState } from 'react';
import { Col, Row, Grid, FormControl, Button } from 'react-bootstrap';
import { connect } from 'react-redux';
import { IStoreState, ITestConfigDetails } from '../../../reducers/state.types';
import { configsService } from '../../../services/configs.service';
import { v4 as uuid } from 'uuid';
import { MultiLineInputComponent } from './MultiLineInputComponent';
import { IPathListResponse } from 'src/src/common/apiResponse.types';

interface IEditTestConfigProps {
    testConfigId: number,
    onClose: (refresh: boolean) => void,
    testConfigToEdit?: ITestConfigDetails,
    appId: string | undefined,
    appName: string | undefined,
    customerName: string | undefined
}

export interface IValueConfig {
    uniqueId: string,
    value: string
}


function EditTestConfig(props: IEditTestConfigProps) {
    const [name, setName] = useState<string>("");
    const [pathList, setPathList] = useState<IValueConfig[]>([]);
    const [mockList, setMockList] = useState<IValueConfig[]>([]);
    const [serviceListInApp, setServiceListInApp] = useState<string[]>([]);
    const [pathListInApp, setPathListInApp] = useState<string[]>([]);

    useEffect(() => {
        if (props.testConfigToEdit) {
            setName(props.testConfigToEdit.testConfigName);
            setPathList(props.testConfigToEdit.testPaths.map(item => {
                return {
                    value: item,
                    uniqueId: uuid()
                }
            }));
            setMockList(props.testConfigToEdit.mocks.map(item => {
                return {
                    value: item,
                    uniqueId: uuid()
                }
            }));

        }

    }, [props.testConfigToEdit]);
    useEffect(() => {
        if (props.appId) {
            configsService.getPathsList(props.appId).then((data) => {
                let paths: string[] = [];
                data.forEach((currentValue: IPathListResponse) => {
                    paths.push(...currentValue.paths);
                }, []);
                paths = paths.filter((item, index) => !!item && !(paths.indexOf(item) < index));
                setPathListInApp(paths);
            }).catch(error => {
                console.error(error);
            })

            configsService.getServicesList(props.appId).then((data) => {
                setServiceListInApp(data.map(service => service.service.name));
            }).catch(error => {
                console.error(error);
            })
        }

    }, [props.appId]);
    const onNameChange = useCallback(
        (event: ChangeEvent<HTMLInputElement & FormControl>) => {
            setName(event.target.value);
        },
        [],
    );
    const onAddOrUpdateClick = () => {
        if (props.appId && props.appName && props.customerName) {
            const testConfig = {
                testConfigName: name,
                services: mockList.map(u => u.value).filter(u => !!u),
                paths: pathList.map(u => u.value).filter((u, index) => !!u)
            } as unknown as ITestConfigDetails;
            if (props.testConfigId) {
                testConfig.id = props.testConfigId;
            }
            configsService.createOrUpdateTestConfig(props.customerName, props.appName, testConfig).then(data => {
                props.onClose(true);
            }).catch(error => {
                alert(error.message)
                console.error(error);
            })
        }

    }
    return (
        <div className="edit-test-config prop-rules">
            <h3>{props.testConfigId ? "Edit config" : "Add Config"}</h3>
            <Grid>
                <Row>
                    <Col sm={10} md={3} lg={3}>
                        Name
                        </Col>
                    <Col sm={2} md={2} lg={1}>
                        :
                        </Col>
                    <Col sm={12} md={7} lg={8}>
                        <FormControl as="input" name="name" id="name" value={name} onChange={onNameChange} disabled={!!props.testConfigId} />
                    </Col>
                </Row>
                <Row className="margin-top-10">
                    <Col sm={10} md={3} lg={3}>
                        Mock Services
                        </Col>
                    <Col sm={2} md={2} lg={1}>
                        :
                        </Col>
                    <Col sm={12} md={7} lg={8}>
                        <datalist id="mockServiceList">
                            {serviceListInApp.map(service => <option value={service}>{service}</option>)}
                        </datalist>
                        <MultiLineInputComponent value={mockList} onChange={setMockList} name="Mock" listId="mockServiceList" />
                    </Col>
                </Row>
                <Row className="margin-top-10">
                    <Col sm={10} md={3} lg={3}>
                        Paths
                        </Col>
                    <Col sm={2} md={2} lg={1}>
                        :
                        </Col>
                    <Col sm={12} md={7} lg={8}>
                        <datalist id="pathListSuggestion">
                            {pathListInApp.map(service => <option value={service}>{service}</option>)}
                        </datalist>
                        <MultiLineInputComponent value={pathList} onChange={setPathList} name="Path" listId="pathListSuggestion" />
                    </Col>
                </Row>
                <Row className="margin-top-10">
                    <Col sm={10} md={3} lg={3}>

                    </Col>
                    <Col sm={2} md={2} lg={1}>

                    </Col>
                    <Col sm={12} md={7} lg={8}>
                        <Button onClick={onAddOrUpdateClick}>{props.testConfigId ? "Update" : "Add"}</Button>
                        <Button onClick={() => props.onClose(false)}>Cancel</Button>
                    </Col>
                </Row>
            </Grid>


        </div>
    );
};


function mapStateToProps(state: IStoreState, props: IEditTestConfigProps) {

    const testConfigToEdit = (state.cube.testConfigList || []).find(config => config.id == props.testConfigId);
    const appId = state.cube.selectedAppObj?.app.id?.toString();
    const appName = state.cube.selectedAppObj?.app.name;
    const customerName = state.cube.selectedAppObj?.app.customer.name;

    return {
        testConfigToEdit,
        appId: appId,
        appName,
        customerName
    }
}

const connectedEditTestConfig = connect(mapStateToProps)(EditTestConfig);

export default connectedEditTestConfig

