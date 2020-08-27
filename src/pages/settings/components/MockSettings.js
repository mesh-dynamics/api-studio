import React from 'react';

const MockSettings = (props) => {
    const {
        port,
        proxyPort,
        protocol,
        host,
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
                    <span>Protocol</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-lock" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="proxy-target-protocol" 
                            type="text" 
                            className="form-control settings-no-border" 
                            placeholder="http or https"
                            value={protocol}
                            onChange={(event) => handleMockSettingsChange('protocol', event.target.value)}
                        />
                    </div>
                    <br />
                    <span>Destination Host</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-globe" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="proxy-target-host" 
                            type="text" 
                            className="form-control settings-no-border"
                            placeholder="demo.prod.cubecorp.io" 
                            value={host}
                            onChange={(event) => handleMockSettingsChange('host', event.target.value)}
                        />
                    </div>
                    <br />
                    <span>Destination Port</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-microchip" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="proxy-target-port" 
                            type="text" 
                            className="form-control settings-no-border" 
                            placeholder="443" 
                            value={port}
                            onChange={(event) => handleMockSettingsChange('port', event.target.value)}
                        />
                    </div>
                    <br />
                    {/* <span>Proxy Port</span>
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-server" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="dev-tool-proxy" 
                            type="text" 
                            className="form-control settings-no-border" 
                            placeholder="9000"
                            value={proxyPort}
                            onChange={(event) => handleMockSettingsChange('proxyPort', event.target.value)}
                        />
                    </div> */}
                    <br />
                    <div className="settings-action-buttons">
                        <span className="settings-margin-left-10">
                            <button 
                                id="save-button" 
                                type="button" 
                                className="btn btn-sm cube-btn text-center"
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