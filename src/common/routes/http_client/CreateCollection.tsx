import React, { Component } from "react";
import { connect } from "react-redux";
import { httpClientActions } from "../../actions/httpClientActions";
import {
  Modal,
  Glyphicon,
  FormGroup,
  FormControl,
  ControlLabel,
} from "react-bootstrap";
import _ from "lodash";

import { apiCatalogActions } from "../../actions/api-catalog.actions";
import { cubeService } from "../../services";
import {
  ICubeState,
  IHttpClientStoreState,
  IStoreState,
  ITemplateSetNameLabel,
  IUserAuthDetails,
} from "../../reducers/state.types";

export interface ICreateCollectionProps {
  modalButton?: boolean;
  dispatch: any;
  httpClient: IHttpClientStoreState;
  cube: ICubeState;
  user: IUserAuthDetails;
  resetParentMessage?: () => void;
}
interface ICreateCollectionState {
  newCollectionName: string;
  modalCreateCollectionMessage: string;
  showCreateCollectionModal: boolean;
  selectedTemplateSetNameLabel: ITemplateSetNameLabel | null;
}

class CreateCollection extends Component<
  ICreateCollectionProps,
  ICreateCollectionState
> {
  constructor(props: ICreateCollectionProps) {
    super(props);
    this.state = {
      newCollectionName: "",
      modalCreateCollectionMessage: "",
      showCreateCollectionModal: false,
      selectedTemplateSetNameLabel: null,
    };

    this.handleCollectionNameChange = this.handleCollectionNameChange.bind(
      this
    );
    this.handleCreateCollection = this.handleCreateCollection.bind(this);
    this.onCloseCreateCollectionModal = this.onCloseCreateCollectionModal.bind(
      this
    );
    this.handleCreateCollectionModalShow = this.handleCreateCollectionModalShow.bind(
      this
    );
    this.getCreateCollectionModal = this.getCreateCollectionModal.bind(this);
  }

  componentDidMount() {
    const {cube: {templateSetNameLabelsList, selectedApp}} = this.props;
    const selectedTemplateSetNameLabel = templateSetNameLabelsList.find(({name}) => (name===`Default${selectedApp}`)) || null // set default if available
    this.setState({selectedTemplateSetNameLabel})
  }

  handleCreateCollectionModalShow() {
    this.setState({
      newCollectionName: "",
      modalCreateCollectionMessage: "",
      showCreateCollectionModal: true,
    });
  }

  onCloseCreateCollectionModal() {
    this.setState({
      newCollectionName: "",
      modalCreateCollectionMessage: "",
      showCreateCollectionModal: false,
    });
  }

  handleCollectionNameChange(evt: React.ChangeEvent<HTMLInputElement>) {
    this.setState({
      newCollectionName: evt.target.value,
      modalCreateCollectionMessage: "",
    });
    this.props.resetParentMessage && this.props.resetParentMessage();
  }

  reset() {
    if (this.state.modalCreateCollectionMessage) {
      this.setState({ modalCreateCollectionMessage: "" });
    }
  }

  handleCreateCollection() {
    const { newCollectionName, selectedTemplateSetNameLabel } = this.state;
    const {
      user,
      dispatch,
      httpClient: { userCollections },
      cube: { selectedApp },
    } = this.props;

    const collectionName = newCollectionName.trim();

    if (!collectionName) {
      this.setState({
        modalCreateCollectionMessage: "Collection name can not be empty.",
      });
    } else if (
      userCollections
        .map((collection: any) => collection.name.toLowerCase())
        .indexOf(collectionName.toLowerCase()) > -1
    ) {
      this.setState({
        modalCreateCollectionMessage: "Collection name already exists.",
      });
    } else if(!selectedTemplateSetNameLabel) {
      this.setState({
        modalCreateCollectionMessage: "Please select template set",
      });
    }else {
      const app = selectedApp;
      const {name: templateSetName, label: templateSetLabel} = selectedTemplateSetNameLabel;
      try {
        this.setState({ modalCreateCollectionMessage: "Saving.." });
        cubeService
          .createUserCollection(user, collectionName, app, templateSetName, templateSetLabel)
          .then(() => {
            dispatch(httpClientActions.loadUserCollections());
            dispatch(
              apiCatalogActions.fetchGoldenCollectionList(app, "UserGolden")
            );
            this.setState({
              newCollectionName: "",
              modalCreateCollectionMessage:
                "Created Successfully!" +
                (this.props.modalButton
                  ? ""
                  : " Please select this newly created collection from below dropdown and click save."),
            });
          }).catch(error=>{
            const message = error.response?.data || error.message;
            this.setState({
              modalCreateCollectionMessage: "Error saving: " + message,
            });
            console.error("Error ", error);
            throw new Error("Error");
          });
      } catch (error) {
        this.setState({
          modalCreateCollectionMessage: "Error saving: " + error,
        });
        console.error("Error ", error);
        throw new Error("Error");
      }
    }
  }

  handleTemplateSetNameLabelChange = (e: React.FormEvent<FormControl & HTMLSelectElement>) => {
    const targetOption = e.target.options[e.target.selectedIndex]
    const templateSetName = targetOption.getAttribute("data-name")
    const templateSetLabel = targetOption.getAttribute("data-label")

    const { cube: {templateSetNameLabelsList}, dispatch} = this.props;
    const selectedTemplateSetNameLabel = templateSetNameLabelsList.find(({name, label}) => (name===templateSetName && label===templateSetLabel)) || null;
    //dispatch(cubeActions.setSelectedTemplateSetNameLabel(selectedTemplateSetNameLabel))
    this.setState({selectedTemplateSetNameLabel})
  }

  renderTemplateSetNameLabelSelection = () => {
      const { cube: {templateSetNameLabelsList} } = this.props;
      const { selectedTemplateSetNameLabel} = this.state;

      const options = (templateSetNameLabelsList || []).map(({name, label}) => {
        return <option key={`${name}-${label}`} value={`${name}-${label}`} data-name={name} data-label={label}>{name} {label && label}</option>
      })

      const {name, label} = selectedTemplateSetNameLabel
                            || {name: "", label: ""}

      return (
        <FormControl
          componentClass="select"
          placeholder="Template Set"
          value={`${name}-${label}`}
          onChange={this.handleTemplateSetNameLabelChange}
          className="btn-sm md-env-select"
        >
          <option disabled value="-">Select Template Set</option>
          {options}
        </FormControl>
      )
  }

  getCreateCollectionBody() {
    const { newCollectionName, modalCreateCollectionMessage } = this.state;
    return (
      <div>
        <div>
          <FormGroup>
            <ControlLabel>Name</ControlLabel>
            <FormControl
              componentClass="input"
              placeholder="Name"
              name="newCollectionName"
              value={newCollectionName}
              onChange={this.handleCollectionNameChange}
            />

            <div className="margin-top-5">
              <ControlLabel>Template Set</ControlLabel>
              {this.renderTemplateSetNameLabelSelection()}
            </div>
          </FormGroup>
        </div>
        <p style={{ fontWeight: 500 }}>{modalCreateCollectionMessage}</p>
      </div>
    );
  }

  getCreateCollectionModal() {
    const { showCreateCollectionModal } = this.state;
    return (
      <Modal
        show={showCreateCollectionModal}
        onHide={this.onCloseCreateCollectionModal}
        backdrop="static"
      >
        <Modal.Header closeButton>
          <Modal.Title>Create a new collection</Modal.Title>
        </Modal.Header>
        <Modal.Body>{this.getCreateCollectionBody()}</Modal.Body>
        <Modal.Footer>
          <div
            onClick={this.handleCreateCollection}
            className="btn btn-sm cube-btn text-center"
          >
            Create Collection
          </div>
          <div
            onClick={this.onCloseCreateCollectionModal}
            className="btn btn-sm cube-btn text-center"
          >
            Close
          </div>
        </Modal.Footer>
      </Modal>
    );
  }

  render() {
    if (this.props.modalButton) {
      return (
        <>
          <div
            className="btn btn-sm cube-btn text-center"
            style={{ padding: "2px 10px", display: "inline-block" }}
            onClick={this.handleCreateCollectionModalShow}
          >
            <Glyphicon glyph="plus" /> New Collection
          </div>
          {this.getCreateCollectionModal()}
        </>
      );
    } else {
      return (
        <>
          {this.getCreateCollectionBody()}
          <div>
            <div
              onClick={this.handleCreateCollection}
              className="btn btn-sm cube-btn text-center"
            >
              Create
            </div>
          </div>
        </>
      );
    }
  }
}

function mapStateToProps(state: IStoreState) {
  const { cube, apiCatalog, httpClient, authentication } = state;
  return {
    cube,
    apiCatalog,
    httpClient,
    user: authentication.user,
  };
}

export default connect(mapStateToProps, null, null, { withRef: true })(
  CreateCollection
);
