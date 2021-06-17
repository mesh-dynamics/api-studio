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

import React, { Fragment } from "react";
import { connect } from "react-redux";
import { getCurrentMockConfig } from "../../utils/http_client/utils";
import { httpClientActions } from "../../actions/httpClientActions";
import _ from "lodash";

import MockConfigs from "./MockConfigs";
//Remove unused Components later
import { FormControl, FormGroup, Modal } from "react-bootstrap";
import {
  IMockConfig,
  IMockConfigValue,
  IStoreState,
} from "../../reducers/state.types";

export interface IMockConfigSectionState {
  showMockConfigModal: boolean;
  showSelectedMockConfigModal: boolean;
}
export interface IMockConfigSectionProps {
  mockConfigList: IMockConfig[];
  selectedMockConfig: string;
  dispatch: any;
}

class MockConfigSection extends React.Component<
  IMockConfigSectionProps,
  IMockConfigSectionState
> {
  constructor(props: IMockConfigSectionProps) {
    super(props);
    this.state = {
      showMockConfigModal: false,
      showSelectedMockConfigModal: false,
    };
  }

  // mock config

  renderMockConfigListDD = () => {
    const { mockConfigList, selectedMockConfig } = this.props;
    return (
      <FormGroup bsSize="small" style={{ marginBottom: "0px" }}>
        <FormControl
          componentClass="select"
          placeholder="Environment"
          style={{ fontSize: "12px" }}
          value={selectedMockConfig}
          onChange={this.handleMockConfigChange}
          className="btn-sm"
        >
          <option value="NONE" disabled>
            Select Mock Configuration
          </option>
          <option value="">All services mocked</option>
          {mockConfigList.length &&
            mockConfigList.map((mockConfig) => (
              <option key={mockConfig.key} value={mockConfig.key}>
                {mockConfig.key}
              </option>
            ))}
        </FormControl>
      </FormGroup>
    );
  };

  handleMockConfigChange = (
    e: React.FormEvent<FormControl & HTMLSelectElement>
  ) => {
    const { dispatch } = this.props;
    dispatch(
      httpClientActions.setSelectedMockConfig(
        (e.target as HTMLSelectElement).value
      )
    );
  };

  renderSelectedMockConfigModal = () => {
    const { mockConfigList, selectedMockConfig } = this.props;
    const currentMockConfig: IMockConfigValue = getCurrentMockConfig(
      mockConfigList,
      selectedMockConfig
    );
    return (
      <Fragment>
        <Modal
          show={this.state.showSelectedMockConfigModal}
          onHide={this.closeSelectedMockConfigModal}
        >
          <Modal.Header closeButton>
            <Modal.Title>
              {"Mock Configuration: " +
                (selectedMockConfig || "All services mocked")}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div>
              {selectedMockConfig ? (
                !_.isEmpty(currentMockConfig.serviceConfigs) ? (
                  <>
                    <table className="table table-bordered table-hover">
                      <thead>
                        <tr>
                          <th style={{ width: "20%" }}>Service</th>
                          <th style={{ width: "20%" }}>Prefix</th>
                          <th>Target</th>
                        </tr>
                      </thead>
                      <tbody>
                        {currentMockConfig.serviceConfigs.map(
                          ({ service, servicePrefix, url, isMocked }) => (
                            <tr key={service}>
                              <td>{service}</td>
                              <td>{servicePrefix}</td>
                              <td style={{ wordBreak: "break-all" }}>
                                {isMocked ? "MOCKED" : url}
                              </td>
                            </tr>
                          )
                        )}
                      </tbody>
                    </table>
                    <span>
                      Calls to external services not configured here will be
                      served by Mesh Dynamics mock server.
                    </span>
                  </>
                ) : (
                  <span>
                    No service configurations defined. All calls to external
                    services will be served by Mesh Dynamics mock server.
                  </span>
                )
              ) : (
                <span>
                  All calls to external services will be served by Mesh Dynamics
                  mock server.
                </span>
              )}
            </div>
          </Modal.Body>
        </Modal>
      </Fragment>
    );
  };

  openSelectedMockConfigModal = () => {
    this.setState({ showSelectedMockConfigModal: true });
  };

  closeSelectedMockConfigModal = () => {
    this.setState({ showSelectedMockConfigModal: false });
  };

  hideMockConfigModal = () => {
    const { dispatch } = this.props;
    dispatch(httpClientActions.resetMockConfigStatusText());
    this.setState({ showMockConfigModal: false });
  };

  render() {
    return (
      <>
        {/* mock configs */}
        <div style={{ display: "inline-block", padding: 0 }} className="btn">
          {this.renderMockConfigListDD()}
        </div>
        {/* <div style={{ display: "inline-block" }}>
          {this.renderSelectedMockConfigModal()}
        </div> */}
        <span
          className="btn btn-sm cube-btn text-center"
          onClick={() => {
            this.setState({ showMockConfigModal: true });
          }}
          title="Environment Configuration"
        >
          <i className="fas fa-cog" />{" "}
        </span>
        <Modal
          bsSize="large"
          show={this.state.showMockConfigModal}
          onHide={this.hideMockConfigModal}
          className="envModal"
          backdrop="static"
        >
          <MockConfigs hideModal={this.hideMockConfigModal} />
        </Modal>
      </>
    );
  }
}

function mapStateToProps(state: IStoreState) {
  const {
    httpClient: { mockConfigList, selectedMockConfig },
  } = state;
  return {
    mockConfigList,
    selectedMockConfig,
  };
}

const connectedMockConfigSection = connect(mapStateToProps)(MockConfigSection);

export default connectedMockConfigSection;
