import React from "react";
import { connect } from "react-redux";
import {
  FormControl,
  FormGroup,
  Modal,
  Glyphicon,
  MenuItem,
  Button,
  Dropdown,
} from "react-bootstrap";

import Shortcuts from '../../utils/Shortcuts';
import * as httpClientTabUtils from "../../utils/http_client/httpClientTabs.utils.js";
import { httpClientActions } from "../../actions/httpClientActions";
import CreateCollection from "./CreateCollection";
import { cubeService } from "../../services";
import {
  IAppDetails,
  ICollectionDetails,
  IEventData,
  IHttpClientStoreState,
  IHttpClientTabDetails,
  IStoreState,
} from "../../reducers/state.types";
import _ from 'lodash';
import {generateTraceIdDetails, generateSpanId, generateSpecialParentSpanId} from "../../utils/http_client/utils"

export declare type GetReqResFromTabDataHandler = (
  eachPair: IEventData[],
  tabToSave: IHttpClientTabDetails,
  runId: string,
  type: string,
  reqTimestamp?: string,
  resTimestamp?: string,
  urlEnvVal?: string,
  currentEnvironment?: string
) => void;

export interface ISaveToCollectionProps {
  goldenList: ICollectionDetails[];
  httpClient: IHttpClientStoreState;
  tabId: string;
  visible: boolean;
  disabled: boolean;
  dispatch: any; //Need to check proper type for dispatch
  selectedApp: string;
  appsList: IAppDetails[];
}
export interface ISaveToCollectionState {
  showModal: boolean;
  showSaveStatusModal: boolean;
  userCollectionId: string;
  selectedCollectionName: string;
  modalErroSaveMessage: string;
  modalErroSaveMessageIsError: boolean;
  saveInProgress: boolean;
}

class SaveToCollection extends React.Component<
  ISaveToCollectionProps,
  ISaveToCollectionState
  > {
  private createCollectionRef: any;
  constructor(props: ISaveToCollectionProps) {
    super(props);
    this.state = {
      showModal: false,
      showSaveStatusModal: false,
      userCollectionId: "",
      selectedCollectionName: "",
      modalErroSaveMessage: "",
      modalErroSaveMessageIsError: false,
      saveInProgress: true,
    };
    this.handleCloseModal = this.handleCloseModal.bind(this);
    this.showSaveModal = this.showSaveModal.bind(this);
    this.handleSaveStatusCloseModal = this.handleSaveStatusCloseModal.bind(
      this
    );

    this.saveTabToCollection = this.saveTabToCollection.bind(this);
    this.createCollectionRef = React.createRef();
  }

  componentDidMount(){
    Shortcuts.register("ctrl+s", this.showSaveModal);
  }
  
  componentDidUpdate(prevProps: ISaveToCollectionProps, prevState: ISaveToCollectionState) {
    if ((prevState.showModal !== this.state.showModal || prevProps.httpClient.collectionTabState.count != this.props.httpClient.collectionTabState.count)  && this.state.showModal) {

      const {
        httpClient: { collectionTabState, allUserCollections }
      } = this.props;
      if(collectionTabState.numResults < collectionTabState.count && (!allUserCollections || allUserCollections.length != collectionTabState.count)){
        this.props.dispatch(httpClientActions.loadAllUserCollections(collectionTabState.count));
      }
    }
  }

  componentWillUnmount(){
    Shortcuts.unregister("ctrl+s");
  }

  handleCloseModal() {
    this.setState({ showModal: false });
  }

  handleSaveStatusCloseModal() {
    this.setState({ showSaveStatusModal: false });
  }

  handleUserCollection = (
    evt: React.FormEvent<FormControl & HTMLSelectElement>
  ) => {
    const selectedOptions =(evt.target as HTMLSelectElement).selectedOptions[0];
    this.setState({
      userCollectionId: selectedOptions.value,
      selectedCollectionName: selectedOptions.text,
      modalErroSaveMessage: "",
      saveInProgress: selectedOptions.value === "" ? true : false,
    });

    this.createCollectionRef &&
      (this.createCollectionRef as any).getWrappedInstance &&
      (this.createCollectionRef as any).getWrappedInstance().reset &&
      (this.createCollectionRef as any).getWrappedInstance().reset();
  };

  resetMessage = () => {
    if (this.state.modalErroSaveMessage) {
      this.setState({ modalErroSaveMessage: "" });
    }
  };

  showSaveModal() {
    const {
      httpClient: { tabs, userHistoryCollection },
      tabId,
    } = this.props;

    const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
    const recordingId = tabs[tabIndex].recordingIdAddedFromClient;
    if (
      recordingId &&
      userHistoryCollection &&
      userHistoryCollection.id !== recordingId
    ) {
      this.setState(
        {
          showSaveStatusModal: true,
          modalErroSaveMessage: "Saving...",
          userCollectionId: recordingId,
        },
        () => {
          this.saveTabToCollection(false);
        }
      );
    } else {
      this.showSaveToModal();
    }
  }

  showSaveToModal = () => {
    this.setState({
      showModal: true,
      userCollectionId: "",
      selectedCollectionName: "",
      modalErroSaveMessage: "",
      modalErroSaveMessageIsError: false,
    });
  };

  updateEachRequest(
    req: IHttpClientTabDetails,
    data: any,
    collectionId: string,
    recordingId: string
  ) {
    req.requestId = data.newReqId;
    req.collectionIdAddedFromClient = collectionId;
    req.traceIdAddedFromClient = data.newTraceId;
    req.recordingIdAddedFromClient = recordingId;
    req.eventData[0].reqId = data.newReqId;
    req.eventData[0].traceId = data.newTraceId;
    req.eventData[0].collection = collectionId;
    req.eventData[1].reqId = data.newReqId;
    req.eventData[1].traceId = data.newTraceId;
    req.eventData[1].collection = data.collec;
  }
  //As of now it is duplicated as not a big function
  getTabIndexGivenTabId(tabId: string, tabs: IHttpClientTabDetails[]) {
    if (!tabs) return -1;
    return tabs.findIndex((e) => e.id === tabId);
  }

  updateTabWithNewData(tabId: string, response: any, recordingId: string) {
    const {
      httpClient: { tabs, userCollections },
      goldenList, dispatch
    } = this.props;
    const collections = [...userCollections, ...goldenList];
    const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
    const tabToProcess = tabs[tabIndex];
    if (response.status === "success") {
      try {
        const parsedData = response.data.response;
        const collection = collections.find(
          (eachCollection) => eachCollection.id === recordingId
        );

        // update ingress request
        const savedIngressRequestData = parsedData[0] // first item is ingress request data
        dispatch(httpClientActions.updateTabWithNewData(
          tabToProcess.id,
          savedIngressRequestData,
          collection!.collec,
          collection!.id))

        // update outgoing requests
        for(let i = 0; i < tabToProcess.outgoingRequests.length; i++) {
          const tabOutgoingReqIndex = i
          const parsedDataIndex = tabOutgoingReqIndex + 1 // since 0 is ingress data
          dispatch(httpClientActions.updateOutgoingTabWithNewData(
            tabToProcess.id,
            tabToProcess.outgoingRequests[tabOutgoingReqIndex].id,
            parsedData[parsedDataIndex],
            collection!.collec,
            collection!.id
          ))
        }
      } catch (err) {
        console.error("Error ", err);
      }
    }
  }

  saveTabToCollection(generateSpanTraceId=false) {
    this.setState({
      saveInProgress : true,
    });
    const recordingId = this.state.userCollectionId;
    const selectedCollectionName = this.state.selectedCollectionName;
    const {
      httpClient: { tabs: tabsToProcess },
      dispatch,
      tabId,
      selectedApp, 
      appsList
    } = this.props;
    const runId = "";

    const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
    const tabToProcess = tabsToProcess[tabIndex];

    if (!tabToProcess.eventData) {
      return;
    }

    const reqResPair = tabToProcess.eventData;
    const type = "UserGolden";
    try {
      if (reqResPair.length > 0) {
        const data = [];
        const {tracer} = _.find(appsList, {name: selectedApp})
        const rootParentSpanId = generateSpecialParentSpanId(tracer);
        const rootSpanId = generateSpanId(tracer);
        const traceIdDetails = generateTraceIdDetails(tracer, rootSpanId);

        // TODO: Quick fix to rectify mock failure where status is empty string
        // Proper fix is to make status on LHS editable

        /** Fix Start */
        let reqResData;
        if (generateSpanTraceId) 
        reqResData = httpClientTabUtils.getReqResFromTabData(selectedApp, reqResPair, tabToProcess, runId, type, 
          null, null, null, null,
          tracer,
          traceIdDetails,
          rootParentSpanId,
          rootSpanId)
          else {
            reqResData = httpClientTabUtils.getReqResFromTabData(selectedApp, reqResPair, tabToProcess, runId, type)
          }

        const { response: { payload: httpResponsePayload } } = reqResData;

        const responsePayloadObject = httpResponsePayload[1];

        const statusBeforeSave = responsePayloadObject['status'];

        if (!statusBeforeSave) {
          responsePayloadObject['status'] = 200;
          reqResData.response.payload[1] = responsePayloadObject;
        }


        data.push(reqResData);

        /** Fix End */

        tabToProcess.outgoingRequests.forEach((eachOutgoingTab) => {
          if (
            eachOutgoingTab.eventData &&
            eachOutgoingTab.eventData.length > 0
          ) {
            const spanId = generateSpanId(tracer);
            let reqResData;
            if(generateSpanTraceId) {
                reqResData = httpClientTabUtils.getReqResFromTabData(
                  selectedApp,
                  eachOutgoingTab.eventData,
                  eachOutgoingTab,
                  runId,
                  type,
                  null, null, null, null,
                  tracer,
                  traceIdDetails,
                  rootSpanId,
                  spanId,
                )
              } else {
                reqResData = httpClientTabUtils.getReqResFromTabData(
                  selectedApp,
                  eachOutgoingTab.eventData,
                  eachOutgoingTab,
                  runId,
                  type
                )
              }
            data.push(reqResData);
          }
        });

        const collectionNameAddedFromClient = selectedCollectionName || tabToProcess.collectionNameAddedFromClient;
        const message = collectionNameAddedFromClient ? `Saving to collection "${collectionNameAddedFromClient}"` : "Saving...";
        const successMessage = collectionNameAddedFromClient ? `Saved Successfully to "${collectionNameAddedFromClient}"` : "Saved Successfully!";

        this.setState({
          modalErroSaveMessage: message,
          modalErroSaveMessageIsError: false,
        });

        cubeService.storeUserReqResponse(recordingId, data).then(
          (serverRes) => {
            dispatch(httpClientActions.unsetHasChangedAll(tabId));
            this.updateTabWithNewData(tabId, serverRes, recordingId);
            this.setState({
              modalErroSaveMessage: successMessage,
              modalErroSaveMessageIsError: false,
              saveInProgress: false,
            });
            dispatch(httpClientActions.loadCollectionTrace(recordingId));
          },
          (error) => {
            this.setState({
              modalErroSaveMessage: "Error saving: " + error,
              modalErroSaveMessageIsError: true,
              saveInProgress : false,
            });
            console.error("error: ", error);
          }
        );
      }
    } catch (error) {
      console.error("Error ", error);
      this.setState({
        modalErroSaveMessage: "Error saving: " + error,
        modalErroSaveMessageIsError: true,
        saveInProgress: false,
      });
    }
  }

  render() {
    const {
      httpClient: { userCollections, tabs, userHistoryCollection, allUserCollections },
      tabId,
    } = this.props;
    const collections = (allUserCollections && allUserCollections.length > 0 ) ? allUserCollections : userCollections;

    const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
    const recordingId = tabs[tabIndex].recordingIdAddedFromClient;
    
    const showSaveToButton =
      recordingId &&
      userHistoryCollection &&
      userHistoryCollection.id !== recordingId;

    return (
      <>
        {showSaveToButton ? (
          <Dropdown disabled={this.props.disabled} style={{ marginRight: "5px", marginBottom: "5px" }} id="saveDdl">
            <Button
              disabled={this.props.disabled}
              onClick={this.showSaveModal}
              className="cube-btn"
            >
              <Glyphicon glyph="save" /> SAVE
            </Button>
            <Dropdown.Toggle className="cube-btn" />
            <Dropdown.Menu>
              <MenuItem onClick={this.showSaveToModal}> Save to... </MenuItem>
            </Dropdown.Menu>
          </Dropdown>
        ) : (
            <Button
              title="Save"
              onClick={this.showSaveModal}
              disabled={this.props.disabled}
              className="cube-btn text-center"
            >
              <Glyphicon glyph="save" /> SAVE
            </Button>
          )}

        <Modal
          show={this.state.showSaveStatusModal}
          onHide={this.handleSaveStatusCloseModal}
        >
          <Modal.Header closeButton>
            <Modal.Title>Saving to Collection</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <p
              style={{
                marginTop: "10px",
                fontWeight: 500,
                color: this.state.modalErroSaveMessageIsError ? "red" : "",
              }}
            >
              {this.state.modalErroSaveMessage}
            </p>
          </Modal.Body>
          <Modal.Footer>
            <div
              onClick={this.handleSaveStatusCloseModal}
              className="btn btn-sm cube-btn text-center"
            >
              Close
            </div>
          </Modal.Footer>
        </Modal>

        <Modal show={this.state.showModal} onHide={this.handleCloseModal}>
          <Modal.Header closeButton>
            <Modal.Title>Save to collection</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <h5 style={{ textAlign: "center" }}>Create a new collection</h5>
            <CreateCollection
              ref={(ref) => (this.createCollectionRef = ref)}
              resetParentMessage={this.resetMessage}
            />
            <hr />
            <h5 style={{ textAlign: "center" }}>
              Select an exisiting collection
            </h5>
            <div>
              <FormGroup style={{ marginBottom: "0px" }}>
                <FormControl
                  componentClass="select"
                  placeholder="Select"
                  name="userCollectionId"
                  value={this.state.userCollectionId}
                  onChange={this.handleUserCollection}
                >
                  <option value=""></option>
                  {collections &&
                    collections.map((eachUserCollection) => {
                      return (
                        <option
                          key={eachUserCollection.id}
                          value={eachUserCollection.id}
                        >
                          {eachUserCollection.name}
                        </option>
                      );
                    })}
                </FormControl>
              </FormGroup>
            </div>
            <p
              style={{
                marginTop: "10px",
                fontWeight: 500,
                color: this.state.modalErroSaveMessageIsError ? "red" : "",
              }}
            >
              {this.state.modalErroSaveMessage}
            </p>
          </Modal.Body>
          <Modal.Footer>
            <Button
              disabled = {this.state.saveInProgress}
              onClick={() => this.saveTabToCollection(true)}
              className="btn btn-sm cube-btn text-center"
            >
              Save
            </Button>
            <div
              onClick={this.handleCloseModal}
              className="btn btn-sm cube-btn text-center"
            >
              Close
            </div>
          </Modal.Footer>
        </Modal>
      </>
    );
  }
}

function mapStateToProps(state: IStoreState) {
  const {
    httpClient,
    apiCatalog: { goldenList }, cube :{ selectedApp, appsList}
  } = state;
  return {
    httpClient,
    goldenList,
    selectedApp, appsList,
  };
}

const connectedSaveToCollection = connect(mapStateToProps)(SaveToCollection);

export default connectedSaveToCollection;
