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

import React from 'react';
import isPort from 'validator/lib/isPort';

const MockSettings = (props) => {
    const {
        proxyPort,
        gRPCProxyPort,
        httpsProxyPort,
        generateCertificate,
        isSaveButtonDisabled,
        handleMockSettingsChange,
        handleSaveMockSettingsClick,
        isLocalHost,
        replayDriverPort,
        cubeUIBackendPort
    } = props;

    return (
        <div className="col-md-12 col-sm-12 col-xs-12">
            <div className="panel panel-default">
                <div className="settings-panel">
                    <i className="icon-calendar"></i>
                    <span>PORTs Settings</span>
                </div>

                <div className="panel-body">
                    <br />
                    <span>Proxy Port</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-server" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="dev-tool-proxy" 
                            type="text" 
                            className="form-control settings-no-border" 
                            placeholder="Default Port: 9000"
                            value={proxyPort}
                            onChange={(event) => handleMockSettingsChange('proxyPort', event.target.value)}
                        />
                    </div>
                    {(!proxyPort || !isPort(String(proxyPort))) && <span className="settings-error-text">Port provided is not valid</span>}
                    <br />
                    <span>gRPC Proxy Port</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-server" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="dev-tool-grpc-proxy" 
                            type="text" 
                            className="form-control settings-no-border" 
                            placeholder="Default Port: 9001"
                            value={gRPCProxyPort}
                            onChange={(event) => handleMockSettingsChange('gRPCProxyPort', event.target.value)}
                        />
                    </div>
                    {(!gRPCProxyPort || !isPort(String(gRPCProxyPort))) && <span className="settings-error-text">Port provided is not valid</span>}
                   
                    <br />
                    <span>HTTPS Proxy Port</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-server" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="dev-tool-https-proxy" 
                            type="text" 
                            className="form-control settings-no-border" 
                            placeholder="Default Port: 9002"
                            value={httpsProxyPort}
                            onChange={(event) => handleMockSettingsChange('httpsProxyPort', event.target.value)}
                        />
                    </div>
                    {(!httpsProxyPort || !isPort(String(httpsProxyPort))) && <span className="settings-error-text">Port provided is not valid</span>}
                    <br />
                    <span>UI Backend Port</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-server" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="dev-tool-https-proxy" 
                            type="text" 
                            readOnly={!isLocalHost}
                            className="form-control settings-no-border" 
                            placeholder="Default Port: 9003"
                            value={cubeUIBackendPort}
                            onChange={(event) => handleMockSettingsChange('cubeUIBackendPort', event.target.value)}
                        />
                    </div>
                    {(!cubeUIBackendPort || !isPort(String(cubeUIBackendPort))) && <span className="settings-error-text">Port provided is not valid</span>}
                    <br />
                    <span>Local Replay Port</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-server" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="dev-tool-https-proxy" 
                            type="text" 
                            className="form-control settings-no-border" 
                            placeholder="Default Port: 9004"
                            value={replayDriverPort}
                            onChange={(event) => handleMockSettingsChange('replayDriverPort', event.target.value)}
                        />
                    </div>
                    {(!replayDriverPort || !isPort(String(replayDriverPort))) && <span className="settings-error-text">Port provided is not valid</span>}
                    {
                        (
                            (proxyPort == gRPCProxyPort) 
                            || (proxyPort == httpsProxyPort) 
                            || (gRPCProxyPort == httpsProxyPort)
                        )
                        && <span className="settings-error-text">Invalid configuration. Port conflict detected.</span>
                    }
                    <br />
                    <span>
                        <input 
                            id="dev-tool-generate-certificate" 
                            type="checkbox" 
                            className="settings-no-border"
                            checked={generateCertificate}
                            onChange={(event) => handleMockSettingsChange('generateCertificate', event.target.checked)}
                        />
                    </span>
                    <span className="settings-left-padding">Generate Root Certificate</span>
                    <div className="settings-action-buttons">
                        <span className="settings-margin-left-10">
                            <button 
                                id="save-button" 
                                type="button" 
                                className="btn btn-sm cube-btn text-center"
                                disabled={isSaveButtonDisabled}
                                onClick={handleSaveMockSettingsClick}
                            >Save</button>
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default MockSettings;