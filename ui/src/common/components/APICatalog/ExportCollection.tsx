import React, { Component } from "react";
import { Button } from "react-bootstrap";
import ExportImportCollectionModal from "../../routes/http_client/ExportImportCollectionModal";
import { connect } from "react-redux";
import {
    IStoreState,
    IUserAuthDetails,
} from "../../reducers/state.types";

interface IExportCollectionProps {
    isGolden: boolean,
    exportImportRecordingId: string;
    exportImportCollectionId: string;
    app: string,
    customerId: string;
}
interface IExportCollectionState {
    showExportImportDialog: boolean;
}

class ExportCollection extends Component<IExportCollectionProps, IExportCollectionState>{
    constructor(props: IExportCollectionProps) {
        super(props);
        this.state = {
            showExportImportDialog: false
        }
    }

    showPopup = () => {
        this.setState({
            showExportImportDialog: true
        });
    };


    hideExportImportDialog = () => {
        this.setState({
            showExportImportDialog: false,
        });
    }

    render() {
        const header = this.props.isGolden ? "Export test suite" : "Export collection";
        const disabled = !this.props.exportImportRecordingId;
        return <><Button
            className="cube-nav-btn"
            disabled={disabled}
            onClick={this.showPopup}
        >
            {header}
        </Button>

            <ExportImportCollectionModal showExportImportDialog={this.state.showExportImportDialog}
                hideExportImportDialog={this.hideExportImportDialog}
                isExportOnly={true}
                exportImportRecordingId={this.props.exportImportRecordingId}
                exportImportCollectionId={this.props.exportImportCollectionId}
                app={this.props.app}
                customerId={this.props.customerId}
            />
        </>
    }
}


const mapStateToProps = (state: IStoreState) => {
    const {
        selectedSource,
        selectedCollection,
        selectedGolden,
    } = state.apiCatalog;


    const {
        actualGoldens: { recordings: goldenList },
        userGoldens: { recordings: collectionList }
    } = state.gcBrowse;

    const customerId = (state.authentication.user as IUserAuthDetails).customer_name;

    const { selectedApp } = state.cube;

    const isGolden = selectedSource == "Golden";

    const searchFromList = isGolden ? goldenList : collectionList;
    const filterByCollection = isGolden ? selectedGolden : selectedCollection;
    const filteredList = (searchFromList || []).filter(collection => collection.collec == filterByCollection);
    const exportImportRecordingId = filteredList.length > 0 ? filteredList[0].id : '';
    const exportImportCollectionId = filteredList.length > 0 ? filteredList[0].collec : '';

    return {
        customerId,
        app: selectedApp,
        isGolden,
        exportImportRecordingId,
        exportImportCollectionId
    };
};

export default connect(mapStateToProps)(ExportCollection);