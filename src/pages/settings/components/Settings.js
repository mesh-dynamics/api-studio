import React, { Component, useEffect } from 'react';
import { Modal, Button } from 'react-bootstrap';
import DomainSettings from './DomainSettings';
import MockSettings from './MockSettings';
import { ipcRenderer } from '../../../common/helpers/ipc-renderer';

class Settings extends Component {
    state = {
        successAlertModalVisible: false,
        domainSettingsModalVisible: false,
        mockSettingsModalVisible: false,
        config: {
            domain: '',
            mock: {
                port: '',
                proxyPort: '',
                protocol: '',
                host: '',
            }
        }
    };

    componentWillMount() {
        ipcRenderer.on('get_config', (event, config) => {
            ipcRenderer.removeAllListeners('get_config');
            this.setState({ config });
        });

        ipcRenderer.on('config_update_success', (event) => {
            this.setState({ successAlertModalVisible: true })
        });
    }

    componentWillUnmount() {
        ipcRenderer.removeAllListeners('get_config');
    }

    componentDidMount() {
        ipcRenderer.send('get_config');
    }

    handleDomainInputChange = (event) => {
        const domain = event.target.value;

        this.setState({ config: { ...this.state.config, domain } })
    }

    handleSaveToConfig = () => {
        ipcRenderer.send('save_target_domain', this.state.config);

        this.setState({
            domainSettingsModalVisible: false,
            mockSettingsModalVisible: false,
        });
    }

    handleBackClick = () => {
        ipcRenderer.send('return_main_window');
    }

    handleMockSettingsChange = (name, value) => {

        this.setState({
            config: { 
                ...this.state.config, 
                mock: {
                    ...this.state.config.mock,
                    [name]: value
                }  
            } 
        });
    }

    handleAlertModalDismissClick = () => this.setState({ successAlertModalVisible: false })

    handleHideDomainSettingsModal = () => this.setState({ domainSettingsModalVisible: false })

    handleHideMockSettingsModal = () => this.setState({ mockSettingsModalVisible: false })

    handleSaveDomainClick = () => this.setState({ domainSettingsModalVisible: true })

    handleSaveMockSettingsClick = () => this.setState({ mockSettingsModalVisible: true })

    render() {
        const { 
            config: {
                domain,
                mock: {
                    port,
                    proxyPort,
                    protocol,
                    host,
                }
            }, 
            domainSettingsModalVisible, 
            mockSettingsModalVisible,
            successAlertModalVisible,
        } = this.state;

        return(
            <div className="settings-parent-container">
                <div className="row settings-width-100">
                    <div className="col-md-12 col-sm-12 col-xs-12 settings-back-button">
                        <div className="settings-page-header">Application Settings</div>
                        <Button onClick={this.handleBackClick} bsStyle="primary" className="settings-custom-back-button">
                            <i className="fa fa-chevron-left" aria-hidden="true"></i>
                            <span className="settings-margin-left-10">Back</span>
                        </Button>
                    </div>
                </div>
                <div className="row settings-width-100">
                    <DomainSettings 
                        domain={domain} 
                        handleDomainInputChange={this.handleDomainInputChange}
                        handleSaveDomainClick={this.handleSaveDomainClick}
                    />
                    <MockSettings
                        port={port}
                        proxyPort={proxyPort}
                        protocol={protocol}
                        host={host}
                        handleMockSettingsChange={this.handleMockSettingsChange}
                        handleSaveMockSettingsClick={this.handleSaveMockSettingsClick}
                    />
                    <Modal
                        show={domainSettingsModalVisible}
                        onHide={this.handleHideDomainSettingsModal}
                        container={this}
                        aria-labelledby="contained-modal-title"
                    >
                        <Modal.Header closeButton>
                            <Modal.Title id="contained-modal-title">
                                Confirm
                            </Modal.Title>
                        </Modal.Header>
                        <Modal.Body>
                            Updating this value will forward all requests to the domain that you have selected.
                            The application will relaunch to apply the changes. Please click on update to proceed.
                        </Modal.Body>
                        <Modal.Footer>
                            <Button onClick={this.handleHideDomainSettingsModal}>Cancel</Button>
                            <Button bsStyle="primary" onClick={this.handleSaveToConfig}>Update</Button>
                        </Modal.Footer>
                    </Modal>
                    <Modal
                        show={mockSettingsModalVisible}
                        onHide={this.handleHideMockSettingsModal}
                        container={this}
                        aria-labelledby="contained-modal-title"
                    >
                        <Modal.Header closeButton>
                            <Modal.Title id="contained-modal-title">
                                Confirm
                            </Modal.Title>
                        </Modal.Header>
                        <Modal.Body>
                            Updating these config will forward all mock requests to the target host that you have selected.
                            Please click on update to proceed.
                        </Modal.Body>
                        <Modal.Footer>
                            <Button onClick={this.handleHideMockSettingsModal}>Cancel</Button>
                            <Button bsStyle="primary" onClick={this.handleSaveToConfig}>Update</Button>
                        </Modal.Footer>
                    </Modal>
                    <Modal
                        show={successAlertModalVisible}
                        onHide={this.handleAlertModalDismissClick}
                        container={this}
                    >
                        <Modal.Body>
                            Successfully updated config
                        </Modal.Body>
                        <Modal.Footer>
                            <Button onClick={this.handleAlertModalDismissClick}>Dismiss</Button>
                        </Modal.Footer>
                    </Modal>
                </div>
            </div>
        );
    }

}

export default Settings;