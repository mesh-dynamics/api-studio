import React, { Fragment } from "react";
import Modal from "react-bootstrap/lib/Modal";

const DiffModalWrapper = (props) => {
    const { 
        tag,
        cube,
        nameG,
        labelG,
        branch,
        version,
        commitId,
        showNewGolden,
        handleCloseSG,
        saveGoldenError,
        handleCloseDone,
        handleSaveGolden,
        showSaveGoldenModal,
        changeGoldenMetaData,
        handleNewGoldenModalClose,
    } = props;
    
    return (
        <Fragment>
            <Modal show={showNewGolden}>
                <Modal.Header>
                    <Modal.Title>
                        {
                            !cube.newGoldenId 
                            ? "Saving Golden" 
                            : "Golden Saved"
                        }
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p className={cube.newGoldenId ? "" : "hidden"}>Name: {nameG}</p>
                    <p className={cube.newGoldenId ? "hidden" : ""}>Saving Golden Operations</p>
                </Modal.Body>
                <Modal.Footer>
                    <div>
                        {cube.newGoldenId ?
                            <span onClick={handleNewGoldenModalClose} className="cube-btn">Go To Test Config</span>
                        :
                            <span className="modal-footer-text">The golden is being saved in the background and will be available later. </span>
                        }
                        &nbsp;&nbsp;<span onClick={handleCloseDone} className="cube-btn">Close</span>
                    </div>
                </Modal.Footer>
            </Modal>

            <Modal show={showSaveGoldenModal}>
                <Modal.Header>
                    <Modal.Title>Application:&nbsp;{cube.selectedApp}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div style={{padding: "15px 25px"}}>
                        <div className={saveGoldenError ? "error-div" : "hidden"}>
                            <h5 style={{marginTop: 0}}>
                                <i className="fas fa-warning"></i>&nbsp;Error!
                            </h5>
                            {saveGoldenError}
                        </div>

                        <div className="row margin-bottom-10">
                            <div className="col-md-3 bold">
                                Name*:
                            </div>

                            <div className="col-md-9">
                                <input 
                                    required 
                                    type="text" 
                                    value={nameG} 
                                    className="width-100"
                                    placeholder="Enter Golden Name" 
                                    onChange={(event) => changeGoldenMetaData('nameG', event)} 
                                />
                            </div>
                        </div>

                        <div className="row margin-bottom-10">
                            <div className="col-md-3 bold">
                                Label*:
                            </div>

                            <div className="col-md-9">
                                <input
                                    required
                                    type="text"
                                    value={labelG}
                                    className="width-100"
                                    placeholder="Enter Label Name"
                                    onChange={(event) => changeGoldenMetaData('labelG', event)}
                                />
                            </div>
                        </div>

                        <div className="row margin-bottom-10">
                            <div className="col-md-3 bold">
                                Branch:
                            </div>

                            <div className="col-md-9">
                                <input 
                                    type="text" 
                                    value={branch}
                                    className="width-100"
                                    placeholder="Enter Branch Name" 
                                    onChange={(event) => changeGoldenMetaData('branch', event)} 
                                />
                            </div>
                        </div>

                        <div className="row margin-bottom-10">
                            <div className="col-md-3 bold">
                                Version:
                            </div>

                            <div className="col-md-9">
                                <input 
                                    type="text" 
                                    value={version} 
                                    className="width-100"
                                    placeholder="Enter Code Version" 
                                    onChange={(event) => changeGoldenMetaData('version', event)} 
                                />
                            </div>
                        </div>

                        <div className="row margin-bottom-10">
                            <div className="col-md-3 bold">
                                Commit ID:
                            </div>

                            <div className="col-md-9">
                                <input 
                                    type="text" 
                                    value={commitId}
                                    className="width-100"
                                    placeholder="Enter Git Commit ID" 
                                    onChange={(event) => changeGoldenMetaData('commitId', event)} 
                                />
                            </div>
                        </div>

                        <div className="row margin-bottom-10">
                            <div className="col-md-3 bold">
                                Tags:
                            </div>

                            <div className="col-md-9">
                                <input 
                                    value={tag} 
                                    type="text" 
                                    className="width-100"
                                    placeholder="Enter Tags(Comma Separated)" 
                                    onChange={(event) => changeGoldenMetaData('tag', event)} 
                                />
                            </div>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <div>
                        <span onClick={handleCloseSG} className="cube-btn">CANCEL</span>&nbsp;&nbsp;
                        <span onClick={handleSaveGolden} className="cube-btn">SAVE</span>
                    </div>
                </Modal.Footer>
            </Modal>
        </Fragment>
    )
}

export default DiffModalWrapper;