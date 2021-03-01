import React, { useState } from "react";
import { Modal } from "react-bootstrap";
import { connect } from "react-redux";
import { cubeActions } from "../actions";
import { CubeButton } from "../components/common/CubeButton";
import { IStoreState } from "../reducers/state.types";
interface IGettingStartedProps{
  isGettingStartedHidden: boolean;
  dispatch: any
}
function GettingStarted (props: IGettingStartedProps) {

  const [showDialog, setShowDialog] = useState(!props.isGettingStartedHidden);
  const [doNotShowAgain, setDoNotShowAgain] = useState(props.isGettingStartedHidden);
  const onHideDialog = () => {
    if(doNotShowAgain){
      props.dispatch(cubeActions.setDoNotShowGettingStartedAgain(doNotShowAgain));
    }
    setShowDialog(false)
  };


  const handleHelpClick = (event) => {
    if(PLATFORM_ELECTRON) {
        event.preventDefault();
        window.require('electron').shell.openExternal("https://docs.meshdynamics.io/category/fwuqaiuo4f-api-studio-user-guide");
    }
  }

  const gettingStartedContent = (
    <div>
      Welcome to Mesh Dynamics API Studio. 
      Follow our <a href="https://docs.meshdynamics.io/category/fwuqaiuo4f-api-studio-user-guide" target="_blank" title="" onClick={handleHelpClick}> online guide </a> 
      to get familiar with API Studio. It will walk you through the capabilities of API Studio from sending your first request to an endpoint to testing your service locally with observability for egress requests.
    </div>
  );

  return (
    <Modal show={showDialog} onHide={onHideDialog} style={{marginTop: "calc(50vh - 325px)"}}>
      <Modal.Header>
        <Modal.Title>Getting Started</Modal.Title>
      </Modal.Header>
      <Modal.Body>{gettingStartedContent}</Modal.Body>
      <Modal.Footer>
        <div className="pull-left">
          <label>
            <input type="checkbox" checked={doNotShowAgain} onChange={(event) => setDoNotShowAgain(event.target.checked)} /> Do not show again
          </label>
        </div>
        <div className="pull-right">
            <CubeButton onClick={onHideDialog} label="Close"/>
        </div>
      </Modal.Footer>
    </Modal>
  );
}


function mapStateToProps(state: IStoreState) {
  const {
    cube :{ isGettingStartedHidden }
  } = state;
  return {
    isGettingStartedHidden: !!isGettingStartedHidden
  };
}

const connectedGettingStarted = connect(mapStateToProps)(GettingStarted);

export default connectedGettingStarted;