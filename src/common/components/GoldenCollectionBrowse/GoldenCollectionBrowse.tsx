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

declare type SelectedSourceType = "UserGolden" | "Golden";
export interface IGoldenCollectionBrowseProps {
    cube: ICubeState;
    dropdownLabel: string;
    showGrouping?: boolean;
    ddlClassNames?: string;
    showDeleteOption?: boolean;
    showVisibilityOption?: boolean;
    gcBrowse: IGoldenCollectionBrowseState;
    selectedSource: SelectedSourceType;
    selectedGoldenOrCollectionItem: ICollectionDetails;
    handleViewGoldenClick?: () => void;
    deleteGolden: (selectedItemId: string, selectedSource: string) => void,
    fetchGoldensCollections: (selectedSource: SelectedSourceType) => void;
    handleChangeCallback: (selectedItem: ICollectionDetails) => void;
}

const GoldenCollectionBrowse = (props: IGoldenCollectionBrowseProps) => {
    const {
        cube: { selectedApp },
        gcBrowse: {
            actualGoldens,
            userGoldens,
            isCollectionLoading,
        },
        showGrouping,
        dropdownLabel,
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

    const [selectedFilterItem, setSelectedFilterItem] =  useState(defaultCollectionItem);

    const [nameFilter, setNameFilter] = useState('');

    const [branchFilter, setBranchFilter] = useState('');

    const [idFilter, setIdFilter] = useState('');

    const [versionFilter, setVersionFilter] = useState('');

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
        fetchGoldensCollections(selectedSource);
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

    const handleDeleteConfirm = () => {
        deleteGolden(selectedFilterItem.id, selectedSource);
        setShowDeleteGoldenConfirm(false);
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
                className={selectedFilterItem.collec === item.collec ? "selected-g-row" : ""}
                onClick={() => handleClickFromFilter(item.collec)}
            >
                <td>{item.name}</td>
                <td>{item.label}</td>
                <td>{item.id}</td>
                <td>{commonUtils.getFormattedDate(new Date(item.timestmp * 1000))}</td>
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
                SELECTED {dropdownLabel}&nbsp;
            </div>
            <div className={goldenCollectionSelectorClass}>
                {
                    selectedGoldenOrCollectionItem.id 
                    ? 
                        (
                            <Fragment>
                                <div className="gcBrowse-selected-item-wrapper">
                                    <span className="label-n">NAME :</span>
                                    <span className="value-n gcBrowse-value-n">{`${selectedGoldenOrCollectionItem?.name || ''}`}</span>
                                </div>
                                <div className="gcBrowse-selected-item-wrapper">
                                    <span className="label-n">LABEL :</span>
                                    <span className="value-n gcBrowse-value-n">{`${selectedGoldenOrCollectionItem?.label || ''}`}</span>
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
                            <span className="cube-btn gcBrowse-action-buttons" onClick={handleViewGoldenClick}>VIEW GOLDEN</span>
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
                        SELECT {dropdownLabel}
                    </span>
                </div>
            </div>
            <Modal show={showGoldenCollectionModal} bsSize="large" onHide={() => { }}>
                <Modal.Header>
                    <Modal.Title>
                        {`Browse ${selectedSource === "UserGolden" ? "Collections" : "Goldens"}`}
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
                    <span onClick={selectHighlighted} className={selectedFilterItem.collec ? "cube-btn" : "disabled cube-btn"}>Select</span>&nbsp;&nbsp;
                        {showDeleteOption && <span onClick={() => setShowDeleteGoldenConfirm(true)} className={selectedFilterItem.collec ? "cube-btn" : "disabled cube-btn"}>Delete</span>}&nbsp;&nbsp;
                        <span onClick={() => setShowGoldenCollectionModal(false)} className="cube-btn">Cancel</span>
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
        (selectedSource: SelectedSourceType) =>
            dispatch(gcbrowseActions.fetchGoldensCollections(selectedSource)),

    deleteGolden:
        (selectedItemId: string, selectedSource: string) =>
            dispatch(gcbrowseActions.deleteGolden(selectedItemId, selectedSource)),
})

export default connect(mapStateToProps, mapDispatchToProps)(GoldenCollectionBrowse);