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

import React, { useState, useEffect, Fragment } from 'react';
import { connect } from 'react-redux';
import { Modal } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import classNames from 'classnames';
import {
    IStoreState,
    ICubeState,
    ICollectionDetails,
    IGoldenCollectionBrowseState
} from '../../reducers/state.types';
import commonUtils from '../../utils/commonUtils';
import gcbrowseActions from '../../actions/gcBrowse.actions';
import './GoldenCollectionBrowse.css';
import { defaultCollectionItem } from '../../constants';
import { CubeButton } from "../../components/common/CubeButton";

declare type SelectedSourceType = "UserGolden" | "Golden";
export interface IGoldenCollectionBrowseProps {
    cube: ICubeState;
    selectedLabel: string;
    selectBtnLabel: string;
    showGrouping?: boolean;
    ddlClassNames?: string;
    showDeleteOption?: boolean;
    showVisibilityOption?: boolean;
    gcBrowse: IGoldenCollectionBrowseState;
    selectedSource: SelectedSourceType;
    selectedGoldenOrCollectionItem: ICollectionDetails;
    handleViewGoldenClick?: () => void;
    deleteGolden: (selectedItemId: string, selectedSource: string) => void,
    fetchGoldensCollections: (selectedSource: SelectedSourceType, start: number, numResults: number, nameFilter: string, versionFilter: string, branchFilter: string, idFilter: string) => void;
    handleChangeCallback: (selectedItem: ICollectionDetails) => void;
}

const numResults = 20

const GoldenCollectionBrowse = (props: IGoldenCollectionBrowseProps) => {
    const {
        cube: { selectedApp },
        gcBrowse: {
            actualGoldens,
            userGoldens,
            isCollectionLoading,
        },
        showGrouping,
        selectedLabel,
        selectBtnLabel,
        selectedSource,
        showDeleteOption,
        showVisibilityOption,
        selectedGoldenOrCollectionItem,
        deleteGolden,
        handleChangeCallback,
        handleViewGoldenClick,
        fetchGoldensCollections
    } = props;

    const [showGoldenCollectionModal, setShowGoldenCollectionModal] = useState(false);

    const [showDeleteGoldenConfirm, setShowDeleteGoldenConfirm] = useState(false);

    const [selectedFilterItem, setSelectedFilterItem] =  useState(defaultCollectionItem as ICollectionDetails);

    const [nameFilter, setNameFilter] = useState('');

    const [branchFilter, setBranchFilter] = useState('');

    const [idFilter, setIdFilter] = useState('');

    const [versionFilter, setVersionFilter] = useState('');

    const [start, setStart] = useState(0);

    const goldenCollectionSelectorClass = classNames("gcBrowse-selected-item-wrapper", {
        ["gcBrowse-selected-group"]: showGrouping
    })

    const findSelectedObjectForCollectionId = (selectedCollectionId: string) => {
        if (selectedSource === 'UserGolden') {
            return userGoldens.recordings.find(item => item.collec === selectedCollectionId);
        }

        if (selectedSource === 'Golden') {
            return actualGoldens.recordings.find(item => item.collec === selectedCollectionId);
        }

        return null;
    }

    /**
     * Handlers are here
     */

    const handleShowGoldenFilter = () => {
        setStart(0)
        setShowGoldenCollectionModal(true);
    };

    const selectHighlighted = () => {
        setShowGoldenCollectionModal(false);
        handleChangeCallback(selectedFilterItem);
    };

    const handleClickFromFilter = (selectedCollectionId: string) => {
        const selectedObject: ICollectionDetails = findSelectedObjectForCollectionId(selectedCollectionId) || defaultCollectionItem;

        if (selectedObject) {
            setSelectedFilterItem(selectedObject);
        }
    };

    const handleDoubleClickFromFilter = (selectedCollectionId: string) => {
        const selectedObject: ICollectionDetails = findSelectedObjectForCollectionId(selectedCollectionId) || defaultCollectionItem;

        if (selectedObject) {
            setShowGoldenCollectionModal(false);
            handleChangeCallback(selectedFilterItem);
        }
    }

    const handleDeleteConfirm = () => {
        deleteGolden(selectedFilterItem.id, selectedSource);
        setShowDeleteGoldenConfirm(false);
    }

    const handleCancelClick = () => {
        setShowGoldenCollectionModal(false);
    }

    // End of handlers

    /**
     * Effects are here
     */
    useEffect(
        () => { fetchGoldensCollections(selectedSource, start, numResults, nameFilter ? nameFilter + "*" : "", versionFilter, branchFilter, idFilter ? idFilter + "*" : "") },
        [selectedSource, selectedApp, start, nameFilter, branchFilter, versionFilter, idFilter]
    );
    // End of Effects


    const renderCollectionTable = () => {
        // let collectionList = cube.testIds;
        let collectionList: ICollectionDetails[] = (selectedSource === 'Golden' ? actualGoldens.recordings : userGoldens.recordings);

        if (!collectionList || collectionList.length == 0) {
            return <tr><td colSpan={5}>NO DATA FOUND</td></tr>
        }

        let trList = collectionList.map(item =>
        (
            <tr
                key={item.collec}
                className={selectedFilterItem.collec === item.collec ? "selected-g-row" : ""}
                onClick={() => handleClickFromFilter(item.collec)} onDoubleClick={()=> handleDoubleClickFromFilter(item.collec)}
            >
                <td>{item.name}</td>
                <td>{item.label}</td>
                <td>{item.id}</td>
                <td>{new Date(item.timestmp * 1000).toLocaleString()}</td>
                <td>{item.userId}</td>
                <td>{item.prntRcrdngId}</td>
            </tr>
        )
        );

        return trList;
    }

    const goToNextPage = () => {
        setStart(start + numResults)
    }

    const goToPrevPage = () => {
        setStart(start - numResults)
    }

    const totalNumResults = (selectedSource === 'Golden' ? actualGoldens.numFound : userGoldens.numFound)
    const numResultsLength = (selectedSource === 'Golden' ? actualGoldens.recordings.length : userGoldens.recordings.length)
    return (
        <div className="margin-top-10">
            <div className="label-n">
                {selectedLabel}&nbsp;
            </div>
            <div className={goldenCollectionSelectorClass}>
                {
                    selectedGoldenOrCollectionItem.id 
                    ? 
                        (
                            <Fragment>
                                <div className="gcBrowse-selected-item-wrapper">
                                    <span className="label-n">NAME:</span>
                                    <span className="value-n gcBrowse-value-n">{`${selectedGoldenOrCollectionItem?.name || ''}`}</span>
                                </div>
                                <div className="gcBrowse-selected-item-wrapper">
                                    <span className="label-n">LABEL:</span>
                                    <span className="value-n gcBrowse-value-n">{`${selectedGoldenOrCollectionItem?.label || ''}`}</span>
                                </div>
                                <div className="gcBrowse-selected-item-wrapper">
                                    <span className="label-n">CREATED ON:</span>
                                    <span className="value-n gcBrowse-value-n">{`${selectedGoldenOrCollectionItem ? new Date(selectedGoldenOrCollectionItem.timestmp * 1000).toLocaleString() : "NA"}`}</span>
                                </div>
                            </Fragment>
                        )
                    :   
                        (
                            <span className="value-n">NA</span>
                        )
                }

                <div className="gcBrowse-button-container">
                    {
                        selectedGoldenOrCollectionItem?.collec
                        && showVisibilityOption
                        &&
                        <Link to={{
                            pathname: "/test_config_view/golden_visibility",
                            search: `recordingId=${selectedGoldenOrCollectionItem?.id || ''}`
                        }}>
                            <span className="cube-btn gcBrowse-action-buttons" onClick={handleViewGoldenClick}>View</span>
                        </Link>
                        
                    }
                    <span
                            className={
                                !showVisibilityOption 
                                ? "cube-btn gcBrowse-action-buttons gcBrowse-button-full"
                                : "cube-btn gcBrowse-action-buttons"
                            } 
                            onClick={handleShowGoldenFilter}
                    >
                        {selectBtnLabel}
                    </span>
                </div>
            </div>
            <Modal show={showGoldenCollectionModal} bsSize="large" onHide={() => { }}>
                <Modal.Header>
                    <Modal.Title>
                        {`${selectedSource === "UserGolden" ? "Collections" : "Test suites"}`}
                        <small className="gcBrowse-modal-heading-color">({selectedApp})</small>
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className="margin-bottom-10 gcBrowse-modal-body-container">
                        <div className="row margin-bottom-10">
                            <div className="col-md-5">
                                <div className="label-n">NAME</div>
                                <div className="value-n">
                                    <input value={nameFilter} onChange={(event) => setNameFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                </div>
                            </div>
                            <div className="col-md-2"></div>
                            <div className="col-md-5">
                                <div className="label-n">BRANCH</div>
                                <div className="value-n">
                                    <input value={branchFilter} onChange={(event) => setBranchFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                </div>
                            </div>
                        </div>
                        <div className="row margin-bottom-10">
                            <div className="col-md-5">
                                <div className="label-n">RECORDING ID</div>
                                <div className="value-n">
                                    <input value={idFilter} onChange={(event) => setIdFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                </div>
                            </div>
                            <div className="col-md-2"></div>
                            <div className="col-md-5">
                                <div className="label-n">CODE VERSION</div>
                                <div className="value-n">
                                    <input value={versionFilter} onChange={(event) => setVersionFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                </div>
                            </div>
                        </div>
                    </div>
                    {
                        isCollectionLoading
                            ?
                            (
                                <div className="gcBrowse-spinner-root">
                                    <div className="gcBrowse-spinner-inner">
                                        <i className="fa fa-spinner fa-spin"></i>
                                    </div>
                                </div>
                            )
                            :
                            (
                                <div className="gcBrowse-modal-body-table-container">
                                    <table className="table table-condensed table-hover table-striped">
                                        <thead>
                                            <tr>
                                                <td className="bold">Name</td>
                                                <td className="bold" style={{ minWidth: "100px" }}>Label</td>
                                                <td className="bold" style={{ minWidth: "175px" }}>ID</td>
                                                <td className="bold">Created on</td>
                                                <td className="bold">Created by</td>
                                                <td className="bold">Parent ID</td>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {renderCollectionTable()}
                                        </tbody>
                                    </table>
                                </div>
                            )
                    }

                </Modal.Body>
                <Modal.Footer>
                    <div className="pull-left">
                        <CubeButton 
                        faIcon="fa-caret-left" 
                        onClick={goToPrevPage} 
                        className={classNames({"disabled": start <= 0})} 
                        style={{marginRight: 0}}
                        />
                        <CubeButton 
                        faIcon="fa-caret-right" 
                        onClick={goToNextPage} 
                        className={classNames({"disabled": start + numResults >= totalNumResults})} 
                        style={{marginLeft: 0}}
                        />
                        <span>{isCollectionLoading ? "Loading..." : <>Displaying <strong>{start} - {start + numResultsLength}</strong> of {totalNumResults}</>}</span>
                    </div>
                    <span onClick={selectHighlighted} className={selectedFilterItem.collec ? "cube-btn" : "disabled cube-btn"}>Select</span>&nbsp;&nbsp;
                    {showDeleteOption && <span onClick={() => setShowDeleteGoldenConfirm(true)} className={selectedFilterItem.collec ? "cube-btn" : "disabled cube-btn"}>Delete</span>}&nbsp;&nbsp;
                    <span onClick={handleCancelClick} className="cube-btn">Cancel</span>
                </Modal.Footer>
            </Modal>
            <Modal show={showDeleteGoldenConfirm} onHide={() => { }}>
                <Modal.Body>
                    <div className="gcBrowse-delete-body-container">
                        <div className="margin-right-10 gcBrowse-delete-body-content">
                            This will delete the {selectedFilterItem.name}. Please confirm.
                        </div>
                        <div style={{ display: "flex", alignItems: "flex-start" }}>
                            <span className="cube-btn margin-right-10" onClick={handleDeleteConfirm}>Confirm</span>
                            <span className="cube-btn" onClick={() => setShowDeleteGoldenConfirm(false)}>No</span>
                        </div>
                    </div>
                </Modal.Body>
            </Modal>

        </div>
    );
};

const mapStateToProps = (state: IStoreState) => ({
    cube: state.cube,
    gcBrowse: state.gcBrowse
})

const mapDispatchToProps = (dispatch: any) => ({
    fetchGoldensCollections:
        (selectedSource: SelectedSourceType, start: number, numResults: number, nameFilter: string, versionFilter: string, branchFilter: string, idFilter: string) =>
            dispatch(gcbrowseActions.fetchGoldensCollections(selectedSource, start, numResults, nameFilter, versionFilter, branchFilter, idFilter)),

    deleteGolden:
        (selectedItemId: string, selectedSource: string) =>
            dispatch(gcbrowseActions.deleteGolden(selectedItemId, selectedSource)),
})

export default connect(mapStateToProps, mapDispatchToProps)(GoldenCollectionBrowse);