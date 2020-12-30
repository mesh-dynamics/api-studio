import React, { useState, useEffect, ReactEventHandler, SyntheticEvent } from 'react';
import { connect } from 'react-redux';
import { Modal } from 'react-bootstrap';
import { Link } from "react-router-dom";
import classNames from 'classnames';

import {
    IStoreState,
    ICubeState,
    ICollectionDetails,
    IGoldenCollectionBrowseState
} from '../../reducers/state.types';
import gcbrowseActions from '../../actions/gcbrowse.actions';
import './GoldenCollectionBrowse.css';

declare type SelectedSourceType = "UserGolden" | "Golden";

export interface IGoldenCollectionBrowseProps {
    cube: ICubeState;
    dropdownLabel: string;
    showDeleteOption?: boolean;
    showVisibilityOption?: boolean;
    gcbrowse: IGoldenCollectionBrowseState;
    selectedSource: SelectedSourceType;
    handleViewGoldenClick?: () => void;
    clearSelectedGoldenCollection: () => void;
    deleteGolden: (selectedItemId: string, selectedSource: string) => void,
    fetchGoldensCollections: (selectedSource: SelectedSourceType) => void;
    updateSelectedGoldenCollection: (selectedItem: ICollectionDetails) => void;
    handleChangeCallback: (selectedItem: ICollectionDetails) => void;
}

const GoldenCollectionBrowse = (props: IGoldenCollectionBrowseProps) => {
    const {
        cube: { selectedApp },
        gcbrowse: {
            selectedCollectionItem,
            actualGoldens,
            userGoldens,
            isCollectionLoading,
            messages,
        },
        dropdownLabel,
        selectedSource,
        showDeleteOption,
        showVisibilityOption,
        deleteGolden,
        handleChangeCallback,
        handleViewGoldenClick,
        fetchGoldensCollections,
        clearSelectedGoldenCollection,
        updateSelectedGoldenCollection
    } = props;

    const [showGoldenCollectionModal, setShowGoldenCollectionModal] = useState(false);

    const [showDeleteGoldenConfirm, setShowDeleteGoldenConfirm] = useState(false);

    const [nameFilter, setNameFilter] = useState('');

    const [branchFilter, setBranchFilter] = useState('');

    const [idFilter, setIdFilter] = useState('');

    const [versionFilter, setVersionFilter] = useState('');

    /**
    * Utility functions are here
    */
    const getFormattedDate = (date: any) => {
        // TODO: Fix this garbage
        let year = date.getFullYear();

        let month = (1 + date.getMonth()).toString();
        month = month.length > 1 ? month : '0' + month;

        let day = date.getDate().toString();
        day = day.length > 1 ? day : '0' + day;

        return month + '/' + day + '/' + year;
    }

    const findSelectedObjectForCollectionId = (selectedCollectionId: string) => {
        if (selectedSource === 'UserGolden') {
            return userGoldens.recordings.find(item => item.collec === selectedCollectionId);
        }

        if (selectedSource === 'Golden') {
            return actualGoldens.recordings.find(item => item.collec === selectedCollectionId);
        }

        return null;
    }
    // End of utils

    /**
     * Handler are here
     */

    const handleShowGoldenFilter = () => {
        fetchGoldensCollections(selectedSource);
        setShowGoldenCollectionModal(true);
    };

    const selectHighlighted = () => {
        setShowGoldenCollectionModal(false);
        handleChangeCallback(selectedCollectionItem);
    };

    const handleClickFromFilter = (selectedCollectionId: string) => {
        const selectedObject: any = findSelectedObjectForCollectionId(selectedCollectionId);

        if (selectedObject) {
            updateSelectedGoldenCollection(selectedObject);
        }
    };

    const handleDropdownOptionChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        const selectedCollectionId = event.target.value;

        const selectedObject: any = findSelectedObjectForCollectionId(selectedCollectionId);

        if (selectedObject) {
            updateSelectedGoldenCollection(selectedObject);
            handleChangeCallback(selectedObject);
        }
    }

    const handleDeleteConfirm = () => {
        deleteGolden(selectedCollectionItem.id, selectedSource);
        setShowDeleteGoldenConfirm(false);
        clearSelectedGoldenCollection();
    }

    // End of handlers

    /**
     * Effects are here
     */
    useEffect(
        () => { fetchGoldensCollections(selectedSource) },
        [selectedSource, selectedApp]
    );
    // End of Effects


    /**
     * render jsx items are here
     */
    const renderDropdownOptions = () => {
        let options: ICollectionDetails[] = (selectedSource === 'UserGolden' ? userGoldens.recordings : actualGoldens.recordings);

        return (
            <select
                id="ddlTestId"
                className="r-att"
                onChange={handleDropdownOptionChange}
                value={selectedCollectionItem.collec || "DEFAULT"}
                placeholder={'Select...'}
            >
                <option value="DEFAULT" disabled>
                    {`Select ${selectedSource === 'UserGolden' ? 'Collection' : 'Golden'}`}
                </option>
                {
                    options.map(
                        (item, index) => (
                            <option
                                key={item.collec + index}
                                value={item.collec}
                                hidden={index > 10 && selectedCollectionItem.collec != item.collec}
                            >
                                {`${item.name} ${item.label}`}
                            </option>)
                    )
                }
            </select>
        );
    };

    const renderCollectionTable = () => {
        // let collectionList = cube.testIds;
        let collectionList: ICollectionDetails[] = (selectedSource === 'Golden' ? actualGoldens.recordings : userGoldens.recordings);

        if (nameFilter) {
            collectionList = collectionList.filter(item => item.name.toLowerCase().includes(nameFilter.toLowerCase()));
        }

        if (branchFilter) {
            collectionList = collectionList.filter(item => item.branch && item.branch.toLowerCase().includes(branchFilter.toLowerCase()));
        }

        if (versionFilter) {
            collectionList = collectionList.filter(item => item.codeVersion && item.codeVersion.toLowerCase().includes(versionFilter.toLowerCase()));
        }

        if (idFilter) {
            collectionList = collectionList.filter(item => item.id.toLowerCase().includes(idFilter.toLowerCase()));
        }

        if (!collectionList || collectionList.length == 0) {
            return <tr><td colSpan={5}>NO DATA FOUND</td></tr>
        }

        let trList = collectionList.map(item =>
        (
            <tr
                key={item.collec}
                className={selectedCollectionItem.collec === item.collec ? "selected-g-row" : ""}
                onClick={() => handleClickFromFilter(item.collec)}
            >
                <td>{item.name}</td>
                <td>{item.label}</td>
                <td>{item.id}</td>
                <td>{getFormattedDate(new Date(item.timestmp * 1000))}</td>
                <td>{item.userId}</td>
                <td>{item.prntRcrdngId}</td>
            </tr>
        )
        );

        return trList;
    }


    return (
        <div className="margin-top-10">
            <div className="label-n">
                {dropdownLabel}&nbsp;
                <i
                    onClick={handleShowGoldenFilter}
                    title={`Browse ${selectedSource === "UserGolden" ? "Collection" : "Golden"}`}
                    className="link fas fa-folder-open pull-right font-15"
                ></i>
                {
                    selectedCollectionItem.collec
                    && showVisibilityOption
                    && (
                        <Link to={{
                            pathname: "/test_config_view/golden_visibility",
                            search: `recordingId=${selectedCollectionItem.id}`
                        }}>
                            <span className="pull-right gcd-view-golden-icon-container" onClick={handleViewGoldenClick}>
                                <i className="fas fa-eye margin-right-10 gcd-view-golden-icon" aria-hidden="true"></i>
                            </span>
                        </Link>
                    )
                }
            </div>
            <div className="value-n">
                {renderDropdownOptions()}
            </div>
            <Modal show={showGoldenCollectionModal} bsSize="large" onHide={() => { }}>
                <Modal.Header>
                    <Modal.Title>
                        {`Browse ${selectedSource === "UserGolden" ? "Collections" : "Goldens"}`}
                        <small className="gcbrowse-modal-heading-color">({selectedApp})</small>
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className="margin-bottom-10 gcbrowse-modal-body-container">
                        <div className="row margin-bottom-10">
                            <div className="col-md-5">
                                <div className="label-n">NAME</div>
                                <div className="value-n">
                                    <input value={nameFilter} onChange={(event) => setNameFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                    {/* <input onChange={(event) => handleInputChange("goldenNameFilter", event)} className="width-100 h-20px" type="text"/> */}
                                </div>
                            </div>
                            <div className="col-md-2"></div>
                            <div className="col-md-5">
                                <div className="label-n">BRANCH</div>
                                <div className="value-n">
                                    <input value={branchFilter} onChange={(event) => setBranchFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                    {/* <input onChange={(event) => handleInputChange("goldenBranchFilter", event)} className="width-100 h-20px" type="text"/> */}
                                </div>
                            </div>
                        </div>
                        <div className="row margin-bottom-10">
                            <div className="col-md-5">
                                <div className="label-n">RECORDING ID</div>
                                <div className="value-n">
                                    <input value={idFilter} onChange={(event) => setIdFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                    {/* <input onChange={(event) => handleInputChange("goldenIdFilter", event)} className="width-100 h-20px" type="text"/> */}
                                </div>
                            </div>
                            <div className="col-md-2"></div>
                            <div className="col-md-5">
                                <div className="label-n">CODE VERSION</div>
                                <div className="value-n">
                                    <input value={versionFilter} onChange={(event) => setVersionFilter(event.target.value)} className="width-100 h-20px" type="text" />
                                    {/* <input onChange={(event) => handleInputChange("goldenVersionFilter", event)} className="width-100 h-20px" type="text"/> */}
                                </div>
                            </div>
                        </div>
                    </div>
                    {
                        isCollectionLoading
                            ?
                            (
                                <div className="gcbrowse-spinner-root">
                                    <div className="gcbrowse-spinner-inner">
                                        <i className="fa fa-spinner fa-spin"></i>
                                    </div>
                                </div>
                            )
                            :
                            (
                                <div className="gcbrowse-modal-body-table-container">
                                    <table className="table table-condensed table-hover table-striped">
                                        <thead>
                                            <tr>
                                                <td className="bold">Name</td>
                                                <td className="bold" style={{ minWidth: "100px" }}>Label</td>
                                                <td className="bold" style={{ minWidth: "175px" }}>ID</td>
                                                <td className="bold">Date</td>
                                                <td className="bold">Created By</td>
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
                    <span onClick={selectHighlighted} className={selectedCollectionItem ? "cube-btn" : "disabled cube-btn"}>Select</span>&nbsp;&nbsp;
                        {showDeleteOption && <span onClick={() => setShowDeleteGoldenConfirm(true)} className={selectedCollectionItem ? "cube-btn" : "disabled cube-btn"}>Delete</span>}&nbsp;&nbsp;
                        <span onClick={() => setShowGoldenCollectionModal(false)} className="cube-btn">Cancel</span>
                </Modal.Footer>
            </Modal>
            <Modal show={showDeleteGoldenConfirm} onHide={() => { }}>
                <Modal.Body>
                    <div className="gcbrowse-delete-body-container">
                        <div className="margin-right-10 gcbrowse-delete-body-content">
                            This will delete the {selectedCollectionItem.name}. Please confirm.
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
    gcbrowse: state.gcbrowse
})

const mapDispatchToProps = (dispatch: any) => ({
    fetchGoldensCollections:
        (selectedSource: SelectedSourceType) =>
            dispatch(gcbrowseActions.fetchGoldensCollections(selectedSource)),

    updateSelectedGoldenCollection:
        (selectedItem: ICollectionDetails) =>
            dispatch(gcbrowseActions.updateSelectedGoldenCollection(selectedItem)),

    deleteGolden:
        (selectedItemId: string, selectedSource: string) =>
            dispatch(gcbrowseActions.deleteGolden(selectedItemId, selectedSource)),

    clearSelectedGoldenCollection:
        () => dispatch(gcbrowseActions.clearSelectedGoldenCollection()),
})

export default connect(mapStateToProps, mapDispatchToProps)(GoldenCollectionBrowse);