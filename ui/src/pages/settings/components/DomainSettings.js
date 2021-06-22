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
import isUrl from 'validator/lib/isURL';

const DomainSettings = (props) => {
  const { domain, isSaveButtonDisabled, handleDomainInputChange, handleSaveDomainClick, handleClickIsLocalhost, isLocalHost } = props;

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
            <label>Setup Localhost Backend?
              <input id="domain-input" type="checkbox" className="form-control" style={{height:'15px', margin: '0px 5px', width: '20px'}} checked={isLocalHost} onChange={handleClickIsLocalhost} />
            </label>
          </div>
           
          <div className="input-group input-group-sm">
            <span className="input-group-addon settings-no-border">
              <i className="fa fa-globe" aria-hidden="true"></i>
            </span>
            <input id="domain-input" type="text" readOnly={isLocalHost} className="form-control settings-no-border" placeholder="http://example.com" value={domain} onChange={handleDomainInputChange} />
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
