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

import React, { Fragment, useState } from "react";
import Modal from "react-bootstrap/lib/Modal";
import { validateGoldenName } from "../../utils/lib/golden-utils";

const DiffModalWrapper = (props) => {
    const { 
        tag,
        cube,
        nameG,
        labelG,
        branch,
        version,
        commitId,
        templateName,
        templateLabel,
        showNewGolden,
        handleCloseSG,
        saveGoldenError,
        handleCloseDone,
        handleSaveGolden,
        showSaveGoldenModal,
        changeGoldenMetaData,
        handleNewGoldenModalClose,
    } = props;

    const { goldenNameIsValid, goldenNameErrorMessage } = validateGoldenName(nameG);
    
    return (
        <Fragment>
            <Modal show={showNewGolden}>
                <Modal.Header>
                    <Modal.Title>
                        {
                            !cube.newGoldenId 
                            ? "Saving test suite" 
                            : "Test suite Saved"
                        }
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p className={cube.newGoldenId ? "" : "hidden"}>Name: {nameG}</p>
                    <p className={cube.newGoldenId ? "hidden" : ""}>Saving test suite Operations</p>
                </Modal.Body>
                <Modal.Footer>
                    <div>
                        {cube.newGoldenId ?
                            <span onClick={handleNewGoldenModalClose} className="cube-btn">Go To Test Config</span>
                        :
                            <span className="modal-footer-text">The test suite is being saved in the background and will be available later. </span>
                        }
                            &nbsp;&nbsp;
                            <span onClick={handleCloseDone} className={cube.goldenInProg ? "cube-btn disabled" : "cube-btn"}>
                                {cube.goldenInProg && <i className="fa fa-spinner fa-spin"></i>}
                                &nbsp;&nbsp;Close
                            </span>
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
                                    placeholder="Enter test suite Name" 
                                    onChange={(event) => changeGoldenMetaData('nameG', event.target.value.replace(/  /g, " "))} 
                                />
                            </div>
                            <div style={{ textAlign: "center", marginLeft: "20px" }}>
                                <span style={{ color: "#c24b4b"}}>{goldenNameErrorMessage}</span>
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
                                    onChange={(event) => changeGoldenMetaData('labelG', event.target.value)}
                                />
                            </div>
                        </div>
                        
                        <div className="row margin-bottom-10">
                            <div className="col-md-3 bold">
                                Comparison Rules:
                            </div>

                            <div className="col-md-1 bold">
                                Name:
                            </div>

                            <div className="col-md-3">
                                {templateName}
                            </div>
                            <div className="col-md-1 bold">
                                Label:
                            </div>

                            <div className="col-md-3">
                                {templateLabel}
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
                                    onChange={(event) => changeGoldenMetaData('branch', event.target.value)} 
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
                                    onChange={(event) => changeGoldenMetaData('version', event.target.value)} 
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
                                    onChange={(event) => changeGoldenMetaData('commitId', event.target.value)} 
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
                                    onChange={(event) => changeGoldenMetaData('tag', event.target.value)} 
                                />
                            </div>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <div>
                        <span onClick={handleCloseSG} className="cube-btn">CANCEL</span>&nbsp;&nbsp;
                        <span onClick={handleSaveGolden} className={goldenNameIsValid ? "cube-btn" : "cube-btn disabled"}>SAVE</span>
                    </div>
                </Modal.Footer>
            </Modal>
        </Fragment>
    )
}

export default DiffModalWrapper;