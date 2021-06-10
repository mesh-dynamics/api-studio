import React from "react";
import isUrl from "validator/lib/isURL";

const DomainSettings = (props) => {
  const { domain, isSaveButtonDisabled, handleDomainInputChange, handleSaveDomainClick } = props;

  return (
    <div className="col-md-12 col-sm-12 col-xs-12">
      <div className="panel panel-default">
        <div className="settings-panel">
          <i className="icon-calendar"></i>
          <span>Target Domain Settings</span>
        </div>

        <div className="panel-body">
          <br />
          <div className="input-group input-group-sm">
            <span className="input-group-addon settings-no-border">
              <i className="fa fa-globe" aria-hidden="true"></i>
            </span>
            <input id="domain-input" type="text" className="form-control settings-no-border" placeholder="http://example.com" value={domain} onChange={handleDomainInputChange} />
          </div>
          {!isUrl(domain, { require_tld: false }) && <span className="settings-error-text">The url is not valid</span>}
          <br />
          <div className="settings-action-buttons">
            <span className="settings-margin-left-10">
              <button id="save-button" type="button" onClick={handleSaveDomainClick} disabled={isSaveButtonDisabled} className="btn btn-sm cube-btn text-center">
                Save
              </button>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DomainSettings;
