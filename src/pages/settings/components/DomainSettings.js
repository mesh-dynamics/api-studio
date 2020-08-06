import React from 'react';
import isUrl from 'validator/lib/isURL';

const  DomainSettings = (props) => {
    const { domain, handleDomainInputChange, handleSaveDomainClick } = props;

    return (
        <div className="col-md-12 col-sm-12 col-xs-12">
            <div className="panel panel-default">
                <div className="panel-heading clearfix">
                    <i className="icon-calendar"></i>
                    <h3 className="panel-title">Target Domain Settings</h3>
                </div>

                <div className="panel-body">
                    <br />
                    <div className="input-group input-group-sm">
                        <span className="input-group-addon settings-no-border">
                            <i className="fa fa-globe" aria-hidden="true"></i>
                        </span>
                        <input 
                            id="domain-input" 
                            type="text" className="form-control settings-no-border"
                            placeholder="http://example.com"
                            value={domain}
                            onChange={handleDomainInputChange}
                        />
                    </div>
                    {!isUrl(domain) && <span className="settings-error-text">The url is not valid</span>}
                    <br />
                    <div className="settings-action-buttons">
                        <span className="settings-margin-left-10">
                            <button 
                                disabled={!isUrl(domain)}
                                id="save-button" 
                                type="button" 
                                className="btn btn-primary settings-custom-save-button"
                                onClick={handleSaveDomainClick}
                            >
                                Save
                            </button>
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
}
 
export default DomainSettings;