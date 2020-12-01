import React, { Component } from "react";
import { connect } from "react-redux";
import {
  Form,
  FormGroup,
  Modal,
  FormControl,
  ControlLabel,
} from "react-bootstrap";
import { Button } from "react-bootstrap";
import _ from "lodash";
import "./APICatalog.css";
import {
  ICollectionDetails,
  IStoreState,
  IUserAuthDetails,
} from "../../reducers/state.types";
import { cubeService } from "../../services";
import { apiCatalogActions } from "../../actions/api-catalog.actions";
import { cubeActions } from "../../actions";

export interface IConvertCollectionState {
  isPopupVisible: boolean;
  isSelectorVisible: boolean;
  newCollection: string;
  prevDerivedCollection: string;
  selectedCollection: string;
  selectedGolden: string;
  message: string;
  isErrorMessage: boolean;
  isLoading: boolean;
}
export interface IConvertCollectionProps {
  selectedSource: string;
  selectedCollection: string;
  selectedGolden: string;
  goldenList: ICollectionDetails[];
  collectionList: ICollectionDetails[];
  username: string;
  app: string;
  dispatch: any;
}

class ConvertCollection extends Component<
  IConvertCollectionProps,
  IConvertCollectionState
> {
  constructor(props: IConvertCollectionProps) {
    super(props);
    const derivedCollection = ConvertCollection.getDerivedCollection(props);
    this.state = {
      isPopupVisible: false,
      newCollection: derivedCollection,
      prevDerivedCollection: derivedCollection,

      selectedCollection: this.props.selectedCollection,
      selectedGolden: this.props.selectedGolden,
      isSelectorVisible: false,
      isErrorMessage: false,
      isLoading: false,
      message: "",
    };
  }

  static getSelectedCollections(props: IConvertCollectionProps) {
    const selectedGolden = _.find(props.goldenList, {
      collec: props.selectedGolden,
    });
    const selectedCollection = _.find(props.collectionList, {
      collec: props.selectedCollection,
    });
    return {
      selectedCollectionName: selectedCollection ? selectedCollection.name : "",
      selectedGoldenName: selectedGolden ? selectedGolden.name : "",
    };
  }

  static getDerivedCollection(props: IConvertCollectionProps) {
    const {
      selectedCollectionName,
      selectedGoldenName,
    } = ConvertCollection.getSelectedCollections(props);
    if (props.selectedSource == "Golden") {
      if (selectedGoldenName) {
        return "UC_" + selectedGoldenName;
      }
    } else {
      if (selectedCollectionName) {
        return "GL_" + selectedCollectionName;
      }
    }
    return "";
  }

  static getDerivedStateFromProps(
    props: IConvertCollectionProps,
    state: IConvertCollectionState
  ) {
    let newState: IConvertCollectionState = { ...state };
    const collectionName = ConvertCollection.getDerivedCollection(props);
    if (
      (props.selectedCollection != state.selectedCollection ||
        props.selectedGolden != state.selectedGolden) &&
      collectionName !== state.prevDerivedCollection
    ) {
      newState.newCollection = collectionName;
      newState.prevDerivedCollection = collectionName;
      newState.message = "";
    }
    return newState;
  }

  showPopup = () => {
    this.setState({
      isPopupVisible: true,
      isSelectorVisible: false,
      isErrorMessage: false,
      message: "",
      newCollection: ConvertCollection.getDerivedCollection(this.props),
    });
  };

  showHideCollectionSelector = () => {
    this.setState({ isSelectorVisible: !this.state.isSelectorVisible });
  };
  dismissHandler = () => {
    this.setState({ isPopupVisible: false });
  };

  isGolden = () => {
    return this.props.selectedSource == "Golden";
  };

  onChangeCollectionName = (event: React.FormEvent<FormControl>) => {
    const value = (event.target as any).value;

    this.setState({ newCollection: value, message: "" });
  };

  convertToTestSuite = () => {
    const { username, app } = this.props;
    const selectedGolden = _.find(this.props.goldenList, {
      collec: this.props.selectedGolden,
    });
    const selectedCollection = _.find(this.props.collectionList, {
      collec: this.props.selectedCollection,
    });
    const isGolden = this.isGolden();
    const collectionId = isGolden ? selectedGolden!.id : selectedCollection!.id;
    const copyRecordingData: any = {
      golden_name: this.state.newCollection,
      version: `default${app}`,
      recordingType: isGolden ? "UserGolden" : "Golden",
    };
    if (isGolden) {
      copyRecordingData.label = username;
    }
    this.setState({ message: "", isLoading: true, isErrorMessage: false });

    if (isGolden) {
      const existingCollections = _.find(this.props.collectionList, {
        name: this.state.newCollection,
      });
      if (existingCollections) {
        this.setState({
          message:
            "User collection already exists with same name. Update name to save",
          isLoading: false,
          isErrorMessage: true,
        });
        return;
      }
    }

    cubeService
      .copyRecording(collectionId, copyRecordingData)
      .then(() => {
        this.setState({
          message: isGolden
            ? "Collection copied successfully"
            : "Test suite created successfully",
          isLoading: false,
        });

        this.props.dispatch(cubeActions.getTestIds(app));

        isGolden
          ? this.props.dispatch(
              apiCatalogActions.fetchGoldenCollectionList(app, "UserGolden")
            )
          : this.props.dispatch(
              apiCatalogActions.fetchGoldenCollectionList(app, "Golden")
            );
      })
      .catch((error) => {
        console.error(error);
        const message = error?.response?.data?.data?.message || error.message;
        this.setState({
          message: message || "Some error occurred",
          isLoading: false,
          isErrorMessage: true,
        });
      });
  };

  render() {
    const isGolden = this.isGolden();
    const header = isGolden ? "Convert to collection" : "Save as test suite";
    const inputLabel = isGolden ? "Golden" : "Collection";
    const inputLabelSaveAs = !isGolden ? "Golden" : "Collection";
    const disabled =
      (isGolden && !this.props.selectedGolden) ||
      (!isGolden && !this.props.selectedCollection);

    const {
      selectedCollectionName,
      selectedGoldenName,
    } = ConvertCollection.getSelectedCollections(this.props);
    const existingCollection = isGolden
      ? selectedGoldenName
      : selectedCollectionName;

    return (
      <div>
        <Button
          className="cube-nav-btn"
          disabled={disabled}
          onClick={this.showPopup}
        >
          {header}
        </Button>

        <Modal
          show={this.state.isPopupVisible}
          onHide={this.dismissHandler}
          backdrop="static"
        >
          <Modal.Header>
            <Modal.Title>{header}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div>
              <Form>
                <FormGroup controlId="existingCollectionName">
                  <ControlLabel>Selected {inputLabel}</ControlLabel>
                  <FormControl
                    type="text"
                    readOnly
                    defaultValue={existingCollection}
                  />
                </FormGroup>

                <FormGroup controlId="newCollectionName">
                  <ControlLabel>Saving as {inputLabelSaveAs}</ControlLabel>
                  <FormControl
                    type="text"
                    value={this.state.newCollection}
                    onChange={this.onChangeCollectionName}
                  />
                </FormGroup>
              </Form>
              <div>
                <span
                  style={{
                    marginTop: "10px",
                    fontWeight: 500,
                    color: this.state.isErrorMessage ? "red" : "",
                  }}
                >
                  {" "}
                  {this.state.isLoading ? "Saving.." : this.state.message}
                </span>
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <span
              onClick={this.dismissHandler}
              className="cube-btn  margin-right-10"
            >
              Close
            </span>
            <span onClick={this.convertToTestSuite} className="cube-btn">
              Proceed
            </span>
          </Modal.Footer>
        </Modal>
      </div>
    );
  }
}

const mapStateToProps = (state: IStoreState) => {
  const {
    selectedSource,
    selectedCollection,
    selectedGolden,
    goldenList,
    collectionList,
  } = state.apiCatalog;
  const username = (state.authentication.user as IUserAuthDetails).username;

  return {
    selectedSource,
    selectedCollection,
    selectedGolden,
    goldenList,
    collectionList,
    username,
    app: state.cube.selectedApp,
  };
};

export default connect(mapStateToProps)(ConvertCollection);