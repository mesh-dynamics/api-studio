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
        handleSaveMockSettingsClick
    } = props;

    return (
        <div className="col-md-12 col-sm-12 col-xs-12">
            <div className="panel panel-default">
                <div className="settings-panel">
                    <i className="icon-calendar"></i>
                    <span>Mock Settings</span>
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
                    {proxyPort === gRPCProxyPort && <span className="settings-error-text">Invalid configuration. Port conflict detected.</span>}
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
                            placeholder="Default Port: 9003"
                            value={httpsProxyPort}
                            onChange={(event) => handleMockSettingsChange('httpsProxyPort', event.target.value)}
                        />
                    </div>
                    {(!httpsProxyPort || !isPort(String(httpsProxyPort))) && <span className="settings-error-text">Port provided is not valid</span>}
                    {(proxyPort === httpsProxyPort || gRPCProxyPort === httpsProxyPort) && <span className="settings-error-text">Invalid configuration. Port conflict detected.</span>}
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
                    <span class="settings-left-padding">Generate Root Certificate</span>
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