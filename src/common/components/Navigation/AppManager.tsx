import React, { Component } from "react";
import { connect } from "react-redux";
import {
  Modal,
  FormGroup,
  ControlLabel,
  FormControl,
  Button,
} from "react-bootstrap";
import { cubeActions } from "../../actions";
import { cubeConstants } from "../../constants";
import AppDetailBox from "./AppDetailBox";
import {
  IAppDetails,
  ICubeState,
  IStoreState,
  IUserAuthDetails,
} from "../../reducers/state.types";
import classNames from "classnames";
import { cubeService } from "../../services";
import _ from "lodash";

export interface IAppManagerState {
  isNewAppModalVisible: boolean;
  isAddAppDisabled: boolean;
  isDeleteConfirmModalVisible: boolean;

  isMenuVisible: boolean;
  savingModalMessageIsError: boolean;
  loading: boolean;
  appDisplayName: string;
  messageHeading: string;
  actionedApp?: IAppDetails;
  appImage: FileList | null;
  savingMessage: string;
  isMessageVisible: boolean;
  isErrorMessage: boolean;
  message: string;
  appModalType: "NewApp" | "UpdateApp";
}
export interface IAppManagerProps {
  cube: ICubeState;
  user: IUserAuthDetails;
  dispatch: any;
}

class AppManager extends Component<IAppManagerProps, IAppManagerState> {
  constructor(props: IAppManagerProps) {
    super(props);
    this.state = {
      isAddAppDisabled: false,
      isDeleteConfirmModalVisible: false,
      isMenuVisible: false,
      isNewAppModalVisible: false,
      loading: false,
      appDisplayName: "",
      savingMessage: "",
      messageHeading: "",
      appImage: null,
      savingModalMessageIsError: false,
      isMessageVisible: false,
      isErrorMessage: false,
      message: "",
      appModalType: "NewApp",
    };
  }

  toggleMenu = () => {
    if (this.state.isMenuVisible) {
    }
    this.setState({ isMenuVisible: !this.state.isMenuVisible });
  };

  handleRemoveApp = (app: IAppDetails) => {
    this.setState({
      isDeleteConfirmModalVisible: true,
      actionedApp: app,
    });
  };
  cancelDeletion = () => {
    this.setState({ isDeleteConfirmModalVisible: false });
  };
  proceedToDelete = () => {
    this.setState({
      isDeleteConfirmModalVisible: false,
      isMessageVisible: true,
      message: "Deleting...",
      messageHeading: "Delete status",
    });
    cubeService
      .removeAnApp(
        this.state.actionedApp?.app.displayName,
        this.props.user.customer_name
      )
      .then(() => {
        this.setState({
          isMessageVisible: true,
          isErrorMessage: false,
          message: "App has been deleted",
        });
        this.props.dispatch(cubeActions.refreshAppList());
      })
      .catch((error) => {
        this.setState({
          isMessageVisible: true,
          isErrorMessage: true,
          message: error.message,
        });
      });
  };

  deleteConfirmation = () => {
    const { actionedApp, isDeleteConfirmModalVisible } = this.state;
    return (
      <Modal show={isDeleteConfirmModalVisible} onHide={this.cancelDeletion}>
        <Modal.Header>
          <Modal.Title>Confirm App Deletion</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div>
            Please confirm to delete the App permanently{" "}
            <span style={{ color: "gray", fontWeight: "bolder" }}>
              {actionedApp?.app.displayName}
            </span>
            .
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            onClick={this.proceedToDelete}
            className="cube-btn"
          >
            Confirm
          </Button>
          <Button onClick={this.cancelDeletion} className="cube-btn">
            Cancel
          </Button>
        </Modal.Footer>
      </Modal>
    );
  };
  dismissMessage = () => {
    this.setState({ isMessageVisible: false });
  };
  showMessage = () => {
    const {
      isMessageVisible,
      isErrorMessage,
      message,
      messageHeading,
    } = this.state;
    return (
      <Modal show={isMessageVisible} onHide={this.dismissMessage}>
        <Modal.Header>
          <Modal.Title>{messageHeading}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className={isErrorMessage ? "errorMessage" : ""}>{message}</p>
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this.dismissMessage} className="cube-btn">
            Ok
          </Button>
        </Modal.Footer>
      </Modal>
    );
  };

  handleChangeForApps = (selectedApp: IAppDetails) => {
    const { dispatch } = this.props;
    const { cube } = this.props;
    if (selectedApp.app.name !== cube.selectedApp) {
      dispatch(cubeActions.setSelectedApp(selectedApp.app.name));
      this.setState({ isMenuVisible: false });
      setTimeout(() => {
        const { cube } = this.props;
        dispatch(cubeActions.clearGolden());
        dispatch(cubeActions.clearTimeline());
        dispatch(cubeActions.getGraphDataByAppId(cube.selectedAppObj!.app.id));
        dispatch(cubeActions.getTimelineData(selectedApp.app.name));
        dispatch(cubeActions.getTestConfigByAppId(cube.selectedAppObj!.app.id));
        dispatch(cubeActions.getTestIds(selectedApp.app.name));
        dispatch(cubeActions.setSelectedTestIdAndVersion("", ""));
      });
    }
  };

  btnClickNewApp = () => {
    this.setState({
      isNewAppModalVisible: true,
      appDisplayName: "",
      isAddAppDisabled: false,
      isMenuVisible: false,
      appModalType: "NewApp",
      savingMessage: "",
    });
  };

  hideNewAppPopup = () => {
    this.setState({ isNewAppModalVisible: false });
  };
  showSavingMessage = (message: string, isError: boolean = true) => {
    this.setState({
      loading: false,
      savingModalMessageIsError: isError,
      savingMessage: message,
    });
  };
  appendFile = (formData: FormData) => {
    const files = this.state.appImage;
    if (files && files.length > 0) {
      formData.append("file", files[0], files[0].name);
    }
  };
  btnAddNewAppClick = () => {
    const {
      cube: { appsList },
    } = this.props;
    const appName = this.state.appDisplayName.trim();
    if (!appName) {
      this.showSavingMessage("App name is not valid");
      return;
    }
    const sameNameApp = appsList.find((app) => app.app.displayName == appName);
    if (sameNameApp) {
      this.showSavingMessage("An app already exists with same name");
      return;
    }

    const appData = {
      displayName: appName,
      customerName: this.props.user.customer_name,
    };

    const formData = new FormData();
    this.appendFile(formData);
    formData.append("app", JSON.stringify(appData));

    this.setState({ loading: true, savingModalMessageIsError: false });

    cubeService
      .addNewApp(formData)
      .then(() => {
        this.props.dispatch(cubeActions.refreshAppList());
        this.setState({
          loading: false,
          savingMessage: "App Created Successfully",
          isAddAppDisabled: true,
        });
      })
      .catch((error) => {
        console.error(error);
        this.showSavingMessage(error.message);
      });
  };
  handleAppNameChange = (
    event: React.FormEvent<FormControl & HTMLInputElement>
  ) => {
    this.setState({ appDisplayName: event.currentTarget.value });
  };
  handleImageInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ appImage: event.target.files });
  };

  newAppModal = () => {
    const {
      loading,
      savingModalMessageIsError,
      savingMessage,
      appModalType,
    } = this.state;
    const isNewAppModal = appModalType == "NewApp";
    return (
      <Modal
        show={this.state.isNewAppModalVisible}
        onHide={this.hideNewAppPopup}
      >
        <Modal.Header>
          <Modal.Title>
            {isNewAppModal ? "Add New App" : "Update App"}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="addNewAppDiv">
            <FormGroup>
              <ControlLabel>Name</ControlLabel>
              <FormControl
                componentClass="input"
                placeholder="App Name"
                name="newAppDisplayName"
                value={this.state.appDisplayName}
                onChange={this.handleAppNameChange}
              />
            </FormGroup>
            <FormGroup>
              <ControlLabel>App Icon (Upload Image file)</ControlLabel>
              <input
                type="file"
                name="appImage"
                accept="image/*"
                onChange={this.handleImageInputChange}
              />
            </FormGroup>
            {loading ? (
              <p>Loading...</p>
            ) : savingMessage ? (
              <p className={savingModalMessageIsError ? "errorMessage" : ""}>
                {savingMessage}
              </p>
            ) : (
              <></>
            )}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            onClick={this.hideNewAppPopup}
            className="cube-btn"
          >
            Close
          </Button>
          {isNewAppModal ? (
            <Button
              onClick={this.btnAddNewAppClick}
              className="cube-btn"
              disabled={this.state.isAddAppDisabled}
            >
              Add
            </Button>
          ) : (
            <Button
              onClick={this.onAppUpdateBtnClick}
              className="cube-btn"
              disabled={this.state.isAddAppDisabled}
            >
              Update
            </Button>
          )}
        </Modal.Footer>
      </Modal>
    );
  };

  onAppUpdate = (app: IAppDetails) => {
    this.setState({
      isNewAppModalVisible: true,
      appDisplayName: app.app.displayName,
      actionedApp: app,
      appModalType: "UpdateApp",
      savingMessage: "",
      loading: false,
    });
  };
  onAppUpdateBtnClick = () => {
    const { actionedApp, appDisplayName, appImage } = this.state;
    const appName = appDisplayName.trim();
    if (!appName) {
      this.showSavingMessage("App name is not valid");
      return;
    }

    const existingApp = this.props.cube.appsList.find(
      (app) =>
        app.app.displayName == appName && app.app.id != actionedApp!.app.id
    );
    if (existingApp) {
      this.showSavingMessage("An app already exists with same name");
      return;
    }

    const formData = new FormData();
    const appData = {
      customerName: this.props.user.customer_name,
      id: actionedApp!.app.id,
      displayName: appName,
    };
    formData.append("app", JSON.stringify(appData));
    this.appendFile(formData);
    this.setState({ loading: true, savingModalMessageIsError: false });

    //Show failure/success messages to user
    cubeService
      .updateApp(formData)
      .then(() => {
        this.props.dispatch(cubeActions.refreshAppList());
        this.setState({
          loading: false,
          savingMessage: "App Updated Successfully",
          isAddAppDisabled: true,
        });
      })
      .catch((error) => {
        console.error(error);
        this.showSavingMessage(error.message);
      });
  };

  createAppList = () => {
    const {
      cube: { appsList, appsListReqStatus, selectedApp },
    } = this.props;

    if (
      appsList.length === 0 &&
      appsListReqStatus === cubeConstants.REQ_LOADING
    ) {
      return "Loading...";
    }

    return (
      <>
        <div
          className="btn btn-sm cube-btn text-center"
          onClick={this.btnClickNewApp}
          style={{ padding: "2px 10px", marginTop: "10px", width: "100%" }}
        >
          <span className="glyphicon glyphicon-plus"></span> New App
        </div>
        {appsList.map((item) => (
          <AppDetailBox
            app={item}
            key={item.app.name}
            isSelected={selectedApp == item.app.name}
            onDeleteApp={this.handleRemoveApp}
            onAppSelect={this.handleChangeForApps}
            onAppEdit={this.onAppUpdate}
          />
        ))}
      </>
    );
  };

  render() {
    const navigatorClass = classNames({
      "app-navigator": true,
      "is-open": this.state.isMenuVisible,
    });
    const appMenuIcon = classNames({
      fas: true,
      "fa-angle-down": !this.state.isMenuVisible,
      "fa-angle-up": this.state.isMenuVisible,
    });
    return (
      <div className={navigatorClass}>
        <div onClick={this.toggleMenu} className="app-selector">
          <i
            className={appMenuIcon}
            style={{ float: "right", margin: "10px" }}
          ></i>
          <div className="label-n">APPLICATION</div>
          <div className="application-name">
            {this.props.cube.selectedAppObj?.app.displayName}
          </div>
        </div>
        {this.state.isMenuVisible && this.createAppList()}
        {this.newAppModal()}
        {this.showMessage()}
        {this.deleteConfirmation()}
      </div>
    );
  }
}

function mapStateToProps(state: IStoreState) {
  const { user } = state.authentication;
  const cube = state.cube;
  return {
    user,
    cube,
  };
}

const connectedAppManager = connect(mapStateToProps)(AppManager);

export default connectedAppManager;
