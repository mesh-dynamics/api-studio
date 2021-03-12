import React, { Fragment } from "react";
import { connect } from "react-redux";
import _ from "lodash";
import { FormControl, FormGroup, Modal, Glyphicon } from "react-bootstrap";

import { getCurrentEnvironment } from "../../utils/http_client/envvar";
import { getCurrentMockConfig } from "../../utils/http_client/utils";
import { httpClientActions } from "../../actions/httpClientActions";

//Remove unused Components later
import {
  IMockConfig,
  IMockConfigValue,
  IEnvironmentConfig,
  IStoreState,
} from "../../reducers/state.types";

import EnvironmentConfigs from "./EnvironmentConfigs";
import "./EnvironmentSection.css";

export interface IEnvironmentSectionState {
  showSelectedEnvModal: boolean;
  showMockConfigModal: boolean;
  tabIndexForEdit: number;
}
export interface IEnvironmentSectionProps {
  environmentList: IEnvironmentConfig[];
  mockConfigList: IMockConfig[];
  selectedMockConfig: string;
  selectedEnvironment: string;
  
  dispatch: any;
}

class EnvironmentSection extends React.Component<
  IEnvironmentSectionProps,
  IEnvironmentSectionState
> {
  constructor(props) {
    super(props);
    this.state = {
      showSelectedEnvModal: false,
      showMockConfigModal: false,
      tabIndexForEdit: 0
    };
  }

  renderEnvListDD = () => {
    const { environmentList, selectedEnvironment } = this.props;
    return (
      <FormGroup bsSize="small">
          <FormControl
            componentClass="select"
            placeholder="Environment"
            value={selectedEnvironment}
            onChange={this.handleEnvChange}
            className="btn-sm md-env-select"
          >
            <option value="NONE" disabled>
              Select Environment
            </option>
            <option value="">No Environment</option>
            {environmentList.length &&
              environmentList.map((env) => (
                <option key={env.name} value={env.name}>
                  {env.name}
                </option>
              ))}
          </FormControl>
      </FormGroup>
    );
  };

  renderMockConfigListDD = () => {
    const { mockConfigList, selectedMockConfig } = this.props;

    return (
      <FormGroup bsSize="small">
        <FormControl
          componentClass="select"
          placeholder="Environment"
          value={selectedMockConfig}
          onChange={this.handleMockConfigChange}
          className="btn-sm md-env-select"
        >
          <option value="NONE" disabled>
            Select Service Config
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

  handleEnvChange = (e: React.FormEvent<FormControl & HTMLSelectElement>) => {
    this.props.dispatch(httpClientActions.setSelectedEnvironment(e.target.value));
  };

  handleMockConfigChange = (e: React.FormEvent<FormControl & HTMLSelectElement>) => {
    this.props.dispatch(httpClientActions.setSelectedMockConfig((e.target as HTMLSelectElement).value));
  };

  handleEnvironmentEdit = () => {
    this.setState({
      showSelectedEnvModal: false,
      showMockConfigModal: true,
      tabIndexForEdit: 0
    })

    this.props.dispatch(httpClientActions.showEnvList(false));
  };

  handleMockConfigEdit = () => {

    this.setState({
      showSelectedEnvModal: false,
      showMockConfigModal: true,
      tabIndexForEdit: 1
    })

    this.props.dispatch(httpClientActions.showMockConfigList(false));
  };

  hideMockConfigModal = () => {
    const { dispatch } = this.props;
    dispatch(httpClientActions.resetMockConfigStatusText());
    this.setState({ showMockConfigModal: false, tabIndexForEdit: 0 });
  };

  renderSelectedEnvModal = () => {
    const {
      environmentList, 
      selectedEnvironment, 
      mockConfigList, 
      selectedMockConfig
    } = this.props;

    const currentEnvironment = getCurrentEnvironment(environmentList, selectedEnvironment);

    const currentMockConfig: IMockConfigValue = getCurrentMockConfig(mockConfigList, selectedMockConfig);

    return (
      <Fragment>
        <span
          title="Environment quick look"
          className="btn btn-sm cube-btn text-center"
          onClick={this.openSelectedEnvModal}
        >
          <i className="fas fa-eye" />
        </span>
        <Modal
          bsSize="large"
          show={this.state.showSelectedEnvModal}
          onHide={this.closeSelectedEnvModal}
        >
          <Modal.Header>
            <div className="md-env-config-header-container">
              <span className="md-env-config-header-label">
                Current Config
              </span>
              <Glyphicon 
                glyph="remove" 
                className="md-env-config-close" 
                onClick={this.closeSelectedEnvModal} 
              />
            </div>
          </Modal.Header>
          <Modal.Body>
            <div className="md-env-modal-section">
              <div className="md-env-modal-section-header">
                <div>
                  Environment: <b>{selectedEnvironment || "No Environment"}</b>
                </div>
                <div>
                  {
                    currentEnvironment && 
                    <span onClick={this.handleEnvironmentEdit} style={{ cursor: "pointer" }}>
                      Edit
                    </span>
                  }
                </div>              
              </div>
              <div className="md-env-modal-section-details">
                {!currentEnvironment && <span>Environment not selected</span>}
                {currentEnvironment && !_.isEmpty(currentEnvironment.vars) && (
                  <table className="table table-hover md-env-modal-table-small-margin">
                    <thead>
                      <tr>
                        <th style={{ width: "20%" }}>Variable</th>
                        <th>Value</th>
                      </tr>
                    </thead>
                    <tbody>
                      {currentEnvironment.vars.map((varEntry) => (
                        <tr key={varEntry.key}>
                          <td>{varEntry.key}</td>
                          <td style={{ wordBreak: "break-all" }}>
                            {varEntry.value}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                {currentEnvironment && _.isEmpty(currentEnvironment.vars) && (<span>No variables defined for this environment</span>)}
              </div>
            </div>
            <div className="md-env-modal-section">
              <div className="md-env-modal-section-header">
                <div>
                  Service Config: <b>{selectedMockConfig || "All services mocked"}</b>
                </div>
                {
                  selectedMockConfig &&
                  <div>
                    <span onClick={this.handleMockConfigEdit} style={{ cursor: "pointer" }}>
                      Edit
                    </span>
                  </div>
                }
                
              </div>
              <div className="md-env-modal-section-details">
                {selectedMockConfig ? (
                  !_.isEmpty(currentMockConfig.serviceConfigs) ? (
                    <>
                      <table className="table table-hover md-env-table-custom">
                        <thead>
                          <tr>
                            <th style={{ width: "20%" }}>Service</th>
                            <th style={{ width: "20%" }}>Prefix</th>
                            <th>Target</th>
                            <th>Mocked</th>
                          </tr>
                        </thead>
                        <tbody>
                          {currentMockConfig.serviceConfigs.map(
                            ({ service, servicePrefix, url, isMocked }) => (
                              <tr key={service}>
                                <td>{service}</td>
                                <td>{servicePrefix}</td>
                                <td>
                                  <span className={isMocked ? "md-env-modal-target-url-light": ""}>
                                    {url}
                                  </span>
                                </td>
                                <td style={{ wordBreak: "break-all" }}>
                                  {
                                    isMocked 
                                    ? <i className="fas fa-check"></i>
                                    : <i className="fas fa-times"></i>
                                  }
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
            </div>
          </Modal.Body>
        </Modal>
      </Fragment>
    );
  };

  renderSelectedMockConfigModal = () => {
    const { showMockConfigModal, tabIndexForEdit } = this.state;
    return (
      <Fragment>
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
          show={showMockConfigModal}
          onHide={this.hideMockConfigModal}
          className="envModal"
          backdrop="static"
        >
          <EnvironmentConfigs 
            tabIndexForEdit={tabIndexForEdit}
            hideModal={this.hideMockConfigModal} 
          />
        </Modal>
      </Fragment>
      
    );
  };

  openSelectedEnvModal = () => {
    this.setState({ showSelectedEnvModal: true });
  };

  closeSelectedEnvModal = () => {
    this.setState({ showSelectedEnvModal: false });
  };

  render() {
    return (
      <>
          <div className="md-env-select-container">
            <span className="md-env-select-label">Environment: </span>
            {this.renderEnvListDD()}
          </div>
          <div className="md-env-select-container">
            <span className="md-env-select-label">Service Config: </span>
            {this.renderMockConfigListDD()}
          </div>
        {/* </div> */}
        <div style={{ display: "inline-block" }}>
          {this.renderSelectedEnvModal()}
        </div>
        <div style={{ display: "inline-block" }}>
          {this.renderSelectedMockConfigModal()}
        </div>
        
      </>
    );
  }
}

const mapStateToProps = (state: IStoreState) => {
  const {
    httpClient: { 
      environmentList, 
      selectedEnvironment, 
      mockConfigList, 
      selectedMockConfig 
    },
  } = state;

  return {
    environmentList,
    selectedEnvironment,
    mockConfigList,
    selectedMockConfig
  };
}

const connectedEnvironmentSection = connect(mapStateToProps)(
  EnvironmentSection
);

export default connectedEnvironmentSection;