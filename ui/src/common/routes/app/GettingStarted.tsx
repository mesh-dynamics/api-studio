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

import React, { useState } from "react";
import { Modal } from "react-bootstrap";
import { connect } from "react-redux";
import { cubeActions } from "../../actions";
import { CubeButton } from "../../components/common/CubeButton";
import { IStoreState } from "../../reducers/state.types";
import { ExternalLink } from "./ExternalLink";
import "./GettingStarted.css";
interface IGettingStartedProps {
  isGettingStartedHidden: boolean;
  dispatch: any;
}

function GettingStarted(props: IGettingStartedProps) {
  const [showDialog, setShowDialog] = useState(!props.isGettingStartedHidden);
  const [doNotShowAgain, setDoNotShowAgain] = useState(props.isGettingStartedHidden);
  const onHideDialog = () => {
    if (doNotShowAgain) {
      props.dispatch(cubeActions.setDoNotShowGettingStartedAgain(doNotShowAgain));
    }
    setShowDialog(false);
  };

  const gettingStartedContent = (
    <div>
      <div className="text-center">
        <h2>Getting Started</h2>
      </div>
      <div className="link-navigation">
        <ExternalLink link="https://github.com/cube-io-corp/meshd-complete/wiki/1.0-Mesh-Dynamics-Overview">
          <div className="heading">What is API Studio?</div>
          <div className="tagline">Overview of what you can do with API Studio</div>
        </ExternalLink>
      </div>
      <div className="link-navigation">
        <ExternalLink link="https://github.com/cube-io-corp/meshd-complete/wiki/1.4-Send-your-first-API-request">
          <div className="heading">Send API requests</div>
          <div className="tagline">Use the API Editor to send requests, and identify changes in API behavior</div>
        </ExternalLink>
      </div>
      <div className="link-navigation">
        <ExternalLink link="https://github.com/cube-io-corp/meshd-complete/wiki/1.6-Configuring-for-Observability">
          <div className="heading">Configure for ingress-egress observability</div>
          <div className="tagline">Leverage API observability to see the interactions with producer services</div>
        </ExternalLink>
      </div>
      <div className="link-navigation">
        <ExternalLink link="https://github.com/cube-io-corp/meshd-complete/wiki/2.0-Service-Testing-with-Mesh-Dynamics">
          <div className="heading">Create automated service tests easily</div>
          <div className="tagline">Test your services in isolation with auto-created mocks</div>
        </ExternalLink>
      </div>
    </div>
  );

  return (
    <Modal show={showDialog} onHide={onHideDialog}>
      <Modal.Body>{gettingStartedContent}</Modal.Body>
      <Modal.Footer>
        <div className="pull-left">
          <label className="light-label">
            <input type="checkbox" checked={doNotShowAgain} onChange={(event) => setDoNotShowAgain(event.target.checked)} /> Do not show again
          </label>
        </div>
        <div className="pull-right">
          <CubeButton onClick={onHideDialog} label="Close" className="light-button" />
        </div>
      </Modal.Footer>
    </Modal>
  );
}

function mapStateToProps(state: IStoreState) {
  const {
    cube: { isGettingStartedHidden },
  } = state;
  return {
    isGettingStartedHidden: !!isGettingStartedHidden,
  };
}

const connectedGettingStarted = connect(mapStateToProps)(GettingStarted);

export default connectedGettingStarted;
