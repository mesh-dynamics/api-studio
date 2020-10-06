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

export interface ICreateCollectionProps {
  modalButton?: boolean;
  dispatch: any;
  httpClient: any;
  cube: any;
}
interface ICreateCollectionState {
  newCollectionName: string;
  modalCreateCollectionMessage: string;
  showCreateCollectionModal: boolean;
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
    });
  }

  handleCreateCollection() {
    const { newCollectionName } = this.state;
    const {
      dispatch,
      httpClient: { userCollections },
      cube: { selectedApp },
    } = this.props;

    if (
      userCollections
        .map((collection: any) => collection.name.toLowerCase())
        .indexOf(newCollectionName.toLowerCase()) > -1
    ) {
      this.setState({
        modalCreateCollectionMessage: "Collection name already exists.",
      });
    } else {
      const app = selectedApp;

      try {
        cubeService.createUserCollection(newCollectionName, app).then(() => {
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

function mapStateToProps(state: any) {
  const { cube, apiCatalog, httpClient } = state;
  return {
    cube,
    apiCatalog,
    httpClient,
  };
}

export default connect(mapStateToProps)(CreateCollection);
