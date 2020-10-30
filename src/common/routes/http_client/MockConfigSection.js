import React, { Component, Fragment, createContext } from "react";
import { connect } from "react-redux";
import { getCurrentMockConfig } from "../../utils/http_client/utils";
import { httpClientActions } from "../../actions/httpClientActions";
import _ from 'lodash';

import MockConfigs from "./MockConfigs";
//Remove unused Components later
import {
  FormControl,
  FormGroup,
  Tabs,
  Tab,
  Panel,
  Label,
  Modal,
  Button,
  ControlLabel,
  Glyphicon,
} from "react-bootstrap";

class MockConfigSection extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showMockConfigModal: false,
      showSelectedMockConfigModal: false,
    };
  }

  // mock config

  renderMockConfigListDD = () => {
    const {
      httpClient: { mockConfigList, selectedMockConfig },
    } = this.props;
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
          <option value="NONE" disabled>Select Mock Configuration</option>
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

  handleMockConfigChange = (e) => {
    const { dispatch } = this.props;
    dispatch(httpClientActions.setSelectedMockConfig(e.target.value));
  };

  renderSelectedMockConfigModal = () => {
    const {
      httpClient: { mockConfigList, selectedMockConfig },
    } = this.props;
    const currentMockConfig = getCurrentMockConfig(
      mockConfigList,
      selectedMockConfig
    );
    return (
      <Fragment>
        <span
          title="Current mock configuration quick look"
          className="btn btn-sm cube-btn text-center"
          onClick={this.openSelectedMockConfigModal}
        >
          <i className="fas fa-eye" />
        </span>
        <Modal
          show={this.state.showSelectedMockConfigModal}
          onHide={this.closeSelectedMockConfigModal}
        >
          <Modal.Header closeButton>
            <Modal.Title>
              {"Mock Configuration: " + (selectedMockConfig || "All services mocked")}
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
                          <th>Target</th>
                        </tr>
                      </thead>
                      <tbody>
                        {currentMockConfig.serviceConfigs.map(
                          ({ service, url, isMocked }) => (
                            <tr key={service}>
                              <td>{service}</td>
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
    this.setState({ showMockConfigModal: false });
  };

  render() {
    return (
      <>
        {/* mock configs */}
        <div style={{ display: "inline-block", padding: 0 }} className="btn">
          {this.renderMockConfigListDD()}
        </div>
        <div style={{ display: "inline-block" }}>
          {this.renderSelectedMockConfigModal()}
        </div>
        <span
          className="btn btn-sm cube-btn text-center"
          onClick={() => {
            this.setState({ showMockConfigModal: true });
          }}
          title="Proxy settings"
        >
          <i className="fas fa-cog" />{" "}
        </span>
        <Modal
          bsSize="large"
          show={this.state.showMockConfigModal}
          onHide={this.hideMockConfigModal}
          className="envModal"
        >
          <MockConfigs hideModal={this.hideMockConfigModal} />
        </Modal>
      </>
    );
  }
}

function mapStateToProps(state) {
  const { cube, apiCatalog, httpClient } = state;
  return {
    cube,
    apiCatalog,
    httpClient,
  };
}

const connectedMockConfigSection = connect(mapStateToProps)(MockConfigSection);

export default connectedMockConfigSection;
