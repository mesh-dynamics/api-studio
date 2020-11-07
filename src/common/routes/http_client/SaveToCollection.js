import React from "react";
import { connect } from "react-redux";
import { FormControl, FormGroup, Modal, Glyphicon } from "react-bootstrap";

import { httpClientActions } from "../../actions/httpClientActions";
import CreateCollection from "./CreateCollection";
import { cubeService } from "../../services";

class SaveToCollection extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showModal: false,
      showSaveStatusModal: false,
      userCollectionId: "",
      modalErroSaveMessage: "",
      modalErroSaveMessageIsError: false,
    };
    this.handleCloseModal = this.handleCloseModal.bind(this);
    this.showSaveModal = this.showSaveModal.bind(this);
    this.handleSaveStatusCloseModal = this.handleSaveStatusCloseModal.bind(
      this
    );

    this.handleChange = this.handleChange.bind(this);
    this.saveTabToCollection = this.saveTabToCollection.bind(this);
    this.createCollectionRef = React.createRef();
  }

  handleCloseModal() {
    this.setState({ showModal: false });
  }

  handleSaveStatusCloseModal() {
    this.setState({ showSaveStatusModal: false });
  }

  handleChange(evt) {
    this.setState({
      [evt.target.name]: evt.target.value,
      modalErroSaveMessage: "",
    });

    this.createCollectionRef.getWrappedInstance &&
      this.createCollectionRef.getWrappedInstance().reset &&
      this.createCollectionRef.getWrappedInstance().reset();
  }

  resetMessage = () => {
    if (this.state.modalErroSaveMessage) {
      this.setState({ modalErroSaveMessage: true });
    }
  };

  showSaveModal() {
    const {
      httpClient: { tabs, userHistoryCollection },
      tabId,
    } = this.props;

    const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
    const recordingId = tabs[tabIndex].recordingIdAddedFromClient;
    if (recordingId && userHistoryCollection.id !== recordingId) {
      this.setState(
        {
          showSaveStatusModal: true,
          modalErroSaveMessage: "Saving...",
          userCollectionId: recordingId,
        },
        () => {
          this.saveTabToCollection();
        }
      );
    } else {
      this.setState({
        showModal: true,
        userCollectionId: "",
        modalErroSaveMessage: "",
        modalErroSaveMessageIsError: false,
      });
    }
  }

  updateEachRequest(req, data, collectionId, recordingId) {
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
  getTabIndexGivenTabId(tabId, tabs) {
    if (!tabs) return -1;
    return tabs.findIndex((e) => e.id === tabId);
  }

  updateTabWithNewData(tabId, response, recordingId) {
    const {
      httpClient: { tabs, userCollections },
    } = this.props;
    const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
    const tabToProcess = tabs[tabIndex];
    if (response.status === "success") {
      try {
        const parsedData =
          response.data.response && response.data.response.length > 0
            ? response.data.response.map((eachOne) => {
                return JSON.parse(eachOne);
              })
            : [];
        const collection = userCollections.find(
          (eachCollection) => eachCollection.id === recordingId
        );
        for (let eachReq of parsedData) {
          if (eachReq.oldReqId === tabToProcess.requestId) {
            this.updateEachRequest(
              tabToProcess,
              eachReq,
              collection.collec,
              collection.id
            );
          } else {
            tabToProcess.outgoingRequests.map((eachOutgoingReq) => {
              if (eachReq.oldReqId === eachOutgoingReq.requestId) {
                this.updateEachRequest(
                  eachOutgoingReq,
                  eachReq,
                  collection.collec,
                  collection.id
                );
              }
              return eachOutgoingReq;
            });
          }
        }
      } catch (err) {
        console.error("Error ", error);
      }
    }
  }

  saveTabToCollection() {
    const recordingId = this.state.userCollectionId;
    const {
      httpClient: { tabs: tabsToProcess },
      dispatch,
      tabId,
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
        data.push(
          this.props.getReqResFromTabData(reqResPair, tabToProcess, runId, type)
        );

        tabToProcess.outgoingRequests.forEach((eachOutgoingTab) => {
          if (
            eachOutgoingTab.eventData &&
            eachOutgoingTab.eventData.length > 0
          ) {
            data.push(
              this.props.getReqResFromTabData(
                eachOutgoingTab.eventData,
                eachOutgoingTab,
                runId,
                type
              )
            );
          }
        });

        this.setState({
          modalErroSaveMessage: "Saving..",
          modalErroSaveMessageIsError: false,
        });

        cubeService.storeUserReqResponse(recordingId, data).then(
          (serverRes) => {
            dispatch(httpClientActions.unsetHasChangedAll(tabId));
            this.updateTabWithNewData(tabId, serverRes, recordingId);
            this.setState({
              modalErroSaveMessage: "Saved Successfully!",
              modalErroSaveMessageIsError: false,
            });
            dispatch(httpClientActions.loadCollectionTrace(recordingId));
          },
          (error) => {
            this.setState({
              modalErroSaveMessage: "Error saving: " + error,
              modalErroSaveMessageIsError: true,
            });
            console.error("error: ", error);
          }
        );
      }
    } catch (error) {
      console.error("Error ", error);
      this.setState({
        modalErroSaveMessage: "Error: Invalid JSON body",
        modalErroSaveMessageIsError: true,
      });
      throw new Error("Error");
    }
  }

  render() {
    const {
      httpClient: { userCollections }, goldenList
    } = this.props;
    const collections = [...userCollections, ...goldenList];
    return (
      <>
        <div
          disabled={this.props.disabled}
          className={
            this.props.disabled
              ? "btn btn-sm cube-btn text-center disabled"
              : "btn btn-sm cube-btn text-center"
          }
          style={{
            padding: "2px 10px",
            display: this.props.visible ? "inline-block" : "none",
          }}
          onClick={this.showSaveModal}
        >
          <Glyphicon glyph="save" /> SAVE
        </div>

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
                  onChange={this.handleChange}
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
            <div
              onClick={this.saveTabToCollection}
              className="btn btn-sm cube-btn text-center"
            >
              Save
            </div>
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

function mapStateToProps(state) {
  const { httpClient, apiCatalog : {goldenList} } = state;
  return {
    httpClient,
    goldenList
  };
}

const connectedSaveToCollection = connect(mapStateToProps)(SaveToCollection);

export default connectedSaveToCollection;
