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

import React, { Component } from 'react';
import { Modal } from 'react-bootstrap';
import isUrl from 'validator/lib/isURL';
import isPort from 'validator/lib/isPort';
import DomainSettings from './DomainSettings';
import MockSettings from './MockSettings';
import { ipcRenderer } from '../../../common/helpers/ipc-renderer';
import { domain } from 'process';

class Settings extends Component {
  state = {
    successAlertModalVisible: false,
    domainSettingsModalVisible: false,
    mockSettingsModalVisible: false,
    config: {
      domain: "",
      proxyPort: "",
      gRPCProxyPort: "",
      httpsProxyPort: "",
      generateCertificate: false,
    },
  };

  componentWillMount() {
    ipcRenderer.on("get_config", (event, config) => {
      ipcRenderer.removeAllListeners("get_config");
      console.log("config: ", config);
      this.setState({ config });
    });

    ipcRenderer.on("config_update_success", (event) => {
      this.setState({ successAlertModalVisible: true });
    });
  }

  componentWillUnmount() {
    ipcRenderer.removeAllListeners("get_config");
  }

  componentDidMount() {
    ipcRenderer.send("get_config");
  }

  hasInvalidInputFields = (domain, proxyPort, gRPCProxyPort, httpsProxyPort) => {
    return (
      !domain ||
      !proxyPort ||
      !gRPCProxyPort ||
      !httpsProxyPort ||
      !isUrl(domain, { require_tld: false }) ||
      !isPort(String(proxyPort)) ||
      !isPort(String(gRPCProxyPort)) ||
      !isPort(String(httpsProxyPort)) ||
      proxyPort == gRPCProxyPort ||
      proxyPort == httpsProxyPort ||
      gRPCProxyPort == httpsProxyPort
    );
  };

  handleDomainInputChange = (event) => {
    const domain = event.target.value;
    this.setState({ config: { ...this.state.config, domain } });
  };

  handleSaveToConfig = () => {
    ipcRenderer.send("save_target_domain", this.state.config);

    this.setState({
      domainSettingsModalVisible: false,
      mockSettingsModalVisible: false,
    });
  };

  handleBackClick = () => {
    ipcRenderer.send("return_main_window");
  };

  handleMockSettingsChange = (name, value) => {
    this.setState({
      config: {
        ...this.state.config,
        [name]: value,
      },
    });
  };

  handleAlertModalDismissClick = () => this.setState({ successAlertModalVisible: false });

  handleHideDomainSettingsModal = () => this.setState({ domainSettingsModalVisible: false });

  handleHideMockSettingsModal = () => this.setState({ mockSettingsModalVisible: false });

  handleSaveDomainClick = () => this.setState({ domainSettingsModalVisible: true });

  handleSaveMockSettingsClick = () => this.setState({ mockSettingsModalVisible: true });

  render() {
    const {
      config: { domain, proxyPort, gRPCProxyPort, httpsProxyPort, generateCertificate },
      domainSettingsModalVisible,
      mockSettingsModalVisible,
      successAlertModalVisible,
    } = this.state;

    const isSaveButtonDisabled = this.hasInvalidInputFields(domain, proxyPort, gRPCProxyPort, httpsProxyPort);

    return (
      <div className="settings-parent-container">
        <div className="row settings-width-100">
          <div className="col-md-12 col-sm-12 col-xs-12 settings-header">
            <div className="settings-page-header">Application Settings</div>
            <button onClick={this.handleBackClick} className="settings-btn-link">
              <i className="fa fa-chevron-left" aria-hidden="true"></i>
              <span className="settings-margin-left-10">Back</span>
            </button>
          </div>
        </div>
        <div className="row settings-width-100">
          <DomainSettings
            domain={domain}
            isSaveButtonDisabled={isSaveButtonDisabled}
            handleDomainInputChange={this.handleDomainInputChange}
            handleSaveDomainClick={this.handleSaveDomainClick}
          />
          <MockSettings
            proxyPort={proxyPort}
            gRPCProxyPort={gRPCProxyPort}
            httpsProxyPort={httpsProxyPort}
            generateCertificate={generateCertificate}
            isSaveButtonDisabled={isSaveButtonDisabled}
            handleMockSettingsChange={this.handleMockSettingsChange}
            handleSaveMockSettingsClick={this.handleSaveMockSettingsClick}
          />
          <Modal show={domainSettingsModalVisible} onHide={this.handleHideDomainSettingsModal} container={this} aria-labelledby="contained-modal-title">
            <Modal.Header closeButton>
              <Modal.Title id="contained-modal-title">Confirm</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              Updating this value will forward all requests to the domain that you have selected. The application will relaunch to apply the changes. Please click on update to
              proceed.
            </Modal.Body>
            <Modal.Footer>
              <button className="btn btn-sm cube-btn text-center settings-cancel-button" onClick={this.handleHideDomainSettingsModal}>
                Cancel
              </button>
              <button className="btn btn-sm cube-btn text-center" onClick={this.handleSaveToConfig}>
                Update
              </button>
            </Modal.Footer>
          </Modal>
          <Modal show={mockSettingsModalVisible} onHide={this.handleHideMockSettingsModal} container={this} aria-labelledby="contained-modal-title">
            <Modal.Header closeButton>
              <Modal.Title id="contained-modal-title">Confirm</Modal.Title>
            </Modal.Header>
            <Modal.Body>Updating these config will forward all mock requests through the target port that you have selected. Please click on update to proceed.</Modal.Body>
            <Modal.Footer>
              <button className="btn btn-sm cube-btn text-center settings-cancel-button" onClick={this.handleHideMockSettingsModal}>
                Cancel
              </button>
              <button className="btn btn-sm cube-btn text-center" onClick={this.handleSaveToConfig}>
                Update
              </button>
            </Modal.Footer>
          </Modal>
          <Modal show={successAlertModalVisible} onHide={this.handleAlertModalDismissClick} container={this}>
            <Modal.Body>Successfully updated config</Modal.Body>
            <Modal.Footer>
              <button className="btn btn-sm cube-btn text-center settings-cancel-button" onClick={this.handleAlertModalDismissClick}>
                Dismiss
              </button>
            </Modal.Footer>
          </Modal>
        </div>
      </div>
    );
  }
}

export default Settings;
