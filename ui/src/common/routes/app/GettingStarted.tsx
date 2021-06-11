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
        <ExternalLink link="https://docs.meshdynamics.io/article/57fqynxzl2-what-is-api-studio">
          <div className="heading">What is API Studio?</div>
          <div className="tagline">Overview of what you can do with API Studio</div>
        </ExternalLink>
      </div>
      <div className="link-navigation">
        <ExternalLink link="https://docs.meshdynamics.io/article/3ta9d7iyuk-send-api-request">
          <div className="heading">Send API requests</div>
          <div className="tagline">Use the API Editor to send requests, and identify changes in API behavior</div>
        </ExternalLink>
      </div>
      <div className="link-navigation">
        <ExternalLink link="https://docs.meshdynamics.io/article/rbhilmc415-configure-request-execution-flow">
          <div className="heading">Use ingress-egress visibility</div>
          <div className="tagline">Leverage API observability to see the interactions with producer services</div>
        </ExternalLink>
      </div>
      <div className="link-navigation">
        <ExternalLink link="https://docs.meshdynamics.io/category/y5sx8fxbd4-service-testing">
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