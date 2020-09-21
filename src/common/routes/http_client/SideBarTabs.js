import React, { Component } from "react";
import { connect } from "react-redux";
import { httpClientActions } from "../../actions/httpClientActions";
import { Tabs, Tab, Panel, Label, Modal } from "react-bootstrap";
import { v4 as uuidv4 } from "uuid";
import _ from "lodash";
import * as moment from "moment";
import arrayToTree from "array-to-tree";
import { Treebeard, decorators } from "react-treebeard";
import config from "../../config";
import TreeNodeContainer from "./TreeNodeContainer";
import TreeNodeToggle from "./TreeNodeToggle";
import CollectionTreeCSS from "./CollectionTreeCSS";

import { cubeActions } from "../../actions";
import { cubeService } from "../../services";
import api from "../../api";

class SideBarTabs extends Component {
  constructor(props) {
    super(props);
    this.state = {
      showDeleteGoldenConfirmation: false,
      itemToDelete: {},
    };

    this.onToggle = this.onToggle.bind(this);
    this.handlePanelClick = this.handlePanelClick.bind(this);
    this.handleTreeNodeClick = this.handleTreeNodeClick.bind(this);
    this.renderTreeNodeHeader = this.renderTreeNodeHeader.bind(this);
    this.persistPanelState = [];
  }

  getExpendedState = (uniqueid) => {
    const isExpanded = this.persistPanelState[uniqueid];
    if (isExpanded) {
      this.handlePanelClick(uniqueid);
    }
    return isExpanded;
  };

  onPanelToggle = (isToggled, event) => {
    this.persistPanelState[
      event.target.parentElement.getAttribute("data-unique-id")
    ] = isToggled;
  };

  deleteItem = async () => {
    const {
      dispatch,
      httpClient: { userCollections },
    } = this.props;
    const { itemToDelete } = this.state;
    try {
      if (itemToDelete.requestType == "collection") {
        await cubeService.deleteGolden(itemToDelete.id);
        dispatch(httpClientActions.deleteUserCollection(itemToDelete.id));
      } else if (itemToDelete.requestType == "request") {
        if (itemToDelete.isParent) {
          await cubeService.deleteEventByTraceId(
            itemToDelete.id,
            itemToDelete.collectionId
          );
        } else {
          await cubeService.deleteEventByRequestId(itemToDelete.id);
        }
        if (itemToDelete.isCubeHistory) {
          dispatch(httpClientActions.deleteCubeRunHistory(itemToDelete.id));
        } else {
          this.handlePanelClick(itemToDelete.collectionId, true);
        }
      }
    } catch (error) {
      console.error("Error caught in softDelete Golden: " + error);
    }
    this.setState({
      showDeleteGoldenConfirmation: false,
    });
  };

  handlePanelClick(selectedCollectionId, forceLoad) {
    if (!selectedCollectionId) return;
    const user = JSON.parse(localStorage.getItem("user"));
    const {
      httpClient: { userCollections },
    } = this.props;
    const {
      cube: { selectedApp },
    } = this.props;
    const app = selectedApp;
    const { dispatch } = this.props;
    const customerId = encodeURIComponent(user.customer_name);
    const selectedCollection = userCollections.find(
      (eachCollection) => eachCollection.collec === selectedCollectionId
    );
    const apiTracesForACollection = selectedCollection.apiTraces;
    try {
      if (!apiTracesForACollection || forceLoad) {
        api
          .get(
            `${config.apiBaseUrl}/as/getApiTrace/${customerId}/${app}?depth=100&collection=${selectedCollectionId}`
          )
          .then(
            (res) => {
              const apiTraces = [];
              res.response.sort((a, b) => {
                return b.res[0].reqTimestamp - a.res[0].reqTimestamp;
              });
              res.response.map((eachApiTrace) => {
                eachApiTrace.res.map((eachApiTraceEvent) => {
                  eachApiTraceEvent["name"] = eachApiTraceEvent["apiPath"];
                  eachApiTraceEvent["id"] = eachApiTraceEvent["requestEventId"];
                  eachApiTraceEvent["toggled"] = false;
                  eachApiTraceEvent["recordingIdAddedFromClient"] =
                    selectedCollection.id;
                  eachApiTraceEvent["traceIdAddedFromClient"] =
                    eachApiTrace.traceId;
                  eachApiTraceEvent["collectionIdAddedFromClient"] =
                    eachApiTrace.collection;
                });
                const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
                  customID: "spanId",
                  parentProperty: "parentSpanId",
                });
                apiTraces.push({
                  ...apiFlatArrayToTree[0],
                });
              });

              selectedCollection.apiTraces = apiTraces;
              dispatch(httpClientActions.addUserCollections(userCollections));
            },
            (err) => {
              console.error("err: ", err);
            }
          );
      }
    } catch (error) {
      console.error("Error ", error);
      throw new Error("Error");
    }
  }

  onDeleteBtnClick = (event) => {
    event.stopPropagation();
    const requestType = event.target.getAttribute("data-type");
    const id = event.target.getAttribute("data-id");
    const name = event.target.getAttribute("data-name");
    const collectionId = event.target.getAttribute("data-collection-id");
    const isParent = event.target.getAttribute("data-isparent") == "true";
    const isCubeHistory =
      event.target.getAttribute("data-cubehistory") == "true";

    this.setState({
      showDeleteGoldenConfirmation: true,
      itemToDelete: {
        requestType,
        id,
        name,
        collectionId,
        isParent,
        isCubeHistory,
      },
    });
  };

  onToggle(node, toggled) {
    const {
      httpClient: { historyCursor },
    } = this.props;
    const { dispatch } = this.props;
    if (historyCursor) {
      dispatch(
        httpClientActions.setInactiveHistoryCursor(historyCursor, false)
      );
      /* if (!_.includes(cursor.children, node)) {
            cursor.toggled = false;
            cursor.active = false;
        } */
    }
    node.active = true;
    if (node.children) {
      node.toggled = toggled;
      if (node.isCubeRunHistory) {
        node.children.forEach((u) => (u.isCubeRunHistory = true));
      }
    }
    if (node.requestEventId) {
      this.persistPanelState[node.requestEventId] = toggled;
    }
    dispatch(httpClientActions.setActiveHistoryCursor(node));
  }

  handleTreeNodeClick(node) {
    this.openTab(node);
  }

  openTab(node) {
    const {
      cube: { selectedApp },
    } = this.props;
    const reqIdArray = [node.requestEventId];
    if (reqIdArray && reqIdArray.length > 0) {
      const user = JSON.parse(localStorage.getItem("user"));
      const apiEventURL = `${config.recordBaseUrl}/getEvents`;
      let body = {
        customerId: user.customer_name,
        app: selectedApp,
        eventTypes: [],
        services: [node.service],
        traceIds: [node.traceIdAddedFromClient],
        reqIds: reqIdArray,
        paths: [node.apiPath],
        collection: node.collectionIdAddedFromClient,
      };
      api.post(apiEventURL, body).then((result) => {
        if (result && result.numResults > 0) {
          for (let eachReqId of reqIdArray) {
            const reqResPair = result.objects.filter(
              (eachReq) => eachReq.reqId === eachReqId
            );
            if (reqResPair.length === 1) {
              reqResPair.push(
                result.objects.find(
                  (eachReq) => eachReq.eventType === "HTTPResponse"
                )
              );
            }
            if (reqResPair.length > 0) {
              const httpRequestEventTypeIndex =
                reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
              const httpResponseEventTypeIndex =
                httpRequestEventTypeIndex === 0 ? 1 : 0;
              const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
              const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
              let headers = [],
                queryParams = [],
                formData = [],
                rawData = "",
                rawDataType = "";
              for (let eachHeader in httpRequestEvent.payload[1].hdrs) {
                headers.push({
                  id: uuidv4(),
                  name: eachHeader,
                  value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                  description: "",
                  selected: true,
                });
              }
              for (let eachQueryParam in httpRequestEvent.payload[1]
                .queryParams) {
                queryParams.push({
                  id: uuidv4(),
                  name: eachQueryParam,
                  value:
                    httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                  description: "",
                  selected: true,
                });
              }
              for (let eachFormParam in httpRequestEvent.payload[1]
                .formParams) {
                formData.push({
                  id: uuidv4(),
                  name: eachFormParam,
                  value: httpRequestEvent.payload[1].formParams[
                    eachFormParam
                  ].join(","),
                  description: "",
                  selected: true,
                });
                rawDataType = "";
              }
              if (httpRequestEvent.payload[1].body) {
                if (!_.isString(httpRequestEvent.payload[1].body)) {
                  try {
                    rawData = JSON.stringify(
                      httpRequestEvent.payload[1].body,
                      undefined,
                      4
                    );
                    rawDataType = "json";
                  } catch (err) {
                    console.error(err);
                  }
                } else {
                  rawData = httpRequestEvent.payload[1].body;
                  rawDataType = "text";
                }
              }
              
              //TODO: Create a separate class to handle below object
              let reqObject = {
                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                httpURL: "{{{url}}}/" + httpRequestEvent.apiPath,
                httpURLShowOnly: httpRequestEvent.apiPath,
                headers: headers,
                queryStringParams: queryParams,
                bodyType:
                  formData && formData.length > 0
                    ? "formData"
                    : rawData && rawData.length > 0
                    ? "rawData"
                    : "formData",
                formData: formData,
                rawData: rawData,
                rawDataType: rawDataType,
                paramsType: "showQueryParams",
                responseStatus: "NA",
                responseStatusText: "",
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: httpResponseEvent
                  ? JSON.stringify(
                      httpResponseEvent.payload[1].hdrs,
                      undefined,
                      4
                    )
                  : "",
                recordedResponseBody: httpResponseEvent
                  ? httpResponseEvent.payload[1].body
                    ? JSON.stringify(
                        httpResponseEvent.payload[1].body,
                        undefined,
                        4
                      )
                    : ""
                  : "",
                recordedResponseStatus: httpResponseEvent
                  ? httpResponseEvent.payload[1].status
                  : "",
                responseBodyType: "json",
                requestId: httpRequestEvent.reqId,
                outgoingRequestIds: node.children
                  ? node.children.map((eachChild) => eachChild.requestEventId)
                  : [],
                eventData: reqResPair,
                showOutgoingRequestsBtn:
                  node.children && node.children.length > 0,
                showSaveBtn: true,
                recordingIdAddedFromClient: node.recordingIdAddedFromClient,
                collectionIdAddedFromClient: node.collectionIdAddedFromClient,
                traceIdAddedFromClient: node.traceIdAddedFromClient,
                outgoingRequests: [],
                showCompleteDiff: false,
                isOutgoingRequest: false,
                service: httpRequestEvent.service,
                requestRunning: false,
                showTrace: null,
              };
              //todo: Test below
              const savedTabId = this.props.onAddTab(
                null,
                reqObject,
                selectedApp
              );
              this.props.showOutgoingRequests(
                savedTabId,
                node.traceIdAddedFromClient,
                node.collectionIdAddedFromClient,
                node.recordingIdAddedFromClient
              );
            }
          }
        }
      });
    }
  }

  renderTreeNodeHeader(props) {
    const isParent = props.node.isCubeRunHistory
      ? !!(props.node.children && props.node.children.length > 0)
      : props.node.parentSpanId == "NA";
    return (
      <div style={props.style.base} className="treeNodeItem">
        <div style={props.style.title}>
          <div
            style={{ paddingLeft: "9px", backgroundColor: "", display: "flex" }}
          >
            <div
              style={{
                flexDirection: "column",
                width: "36px",
                verticalAlign: "top",
              }}
            >
              <Label
                bsStyle="default"
                style={{ fontWeight: "600", fontSize: "9px" }}
              >
                {props.node.method}
              </Label>
            </div>
            <div
              style={{
                flex: "1",
                wordBreak: "break-word",
                verticalAlign: "top",
                fontSize: "12px",
              }}
              onClick={() => this.handleTreeNodeClick(props.node)}
            >
              <span
                style={{
                  paddingLeft: "5px",
                  marginLeft: "5px",
                  borderLeft: "2px solid #fc6c0a",
                  whiteSpace: "nowrap",
                  textOverflow: "ellipsis",
                  overflow: "hidden",
                }}
              >
                {props.node.name +
                  " " +
                  moment(props.node.reqTimestamp * 1000).format("hh:mm:ss")}
              </span>
            </div>
            <div className="collection-options">
              <i
                className="fas fa-trash pointer"
                data-id={
                  isParent
                    ? props.node.traceIdAddedFromClient
                    : props.node.requestEventId
                }
                data-isparent={isParent}
                data-name={props.node.name}
                title="Delete"
                data-collection-id={props.node.collectionIdAddedFromClient}
                data-cubehistory={props.node.isCubeRunHistory === true}
                data-type="request"
                onClick={this.onDeleteBtnClick}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderTreeNodeContainer(props) {
    return <TreeNodeContainer {...props} />;
  }

  renderTreeNodeToggle(props) {
    return <TreeNodeToggle {...props} />;
  }

  render() {
    //Remove unused vars
    const {
      httpClient: {
        cubeRunHistory,
        userCollections,
        collectionName,
        collectionLabel,
        modalErroSaveMessage,
        modalErroSaveMessageIsError,
        modalErroCreateCollectionMessage,
        tabs,
        selectedTabKey,
        showSaveModal,
        showAddMockReqModal,
        mockRequestServiceName,
        mockRequestApiPath,
        modalErrorAddMockReqMessage,
      },
    } = this.props;

    const { showDeleteGoldenConfirmation, itemToDelete } = this.state;

    return (
      <>
        <Tabs defaultActiveKey={1} id="uncontrolled-tab-example">
          <Tab eventKey={1} title="History">
            <div className="margin-top-10">
              <div className="value-n"></div>
            </div>
            <div className="margin-top-10">
              {Object.keys(cubeRunHistory).map((k, i) => {
                return (
                  <Panel
                    key={k + "_" + i}
                    id="collapsible-panel-example-2"
                    defaultExpanded
                  >
                    <Panel.Heading style={{ paddingLeft: "9px" }}>
                      <Panel.Title toggle style={{ fontSize: "13px" }}>
                        {k}
                      </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                      <Panel.Body>
                        {cubeRunHistory[k].map((eachTabRun) => {
                          eachTabRun.isCubeRunHistory = true;
                          /* return (
                                            <div key={eachTabRun.reqTimestamp} style={{padding: "5px", backgroundColor: ""}}>
                                                <div style={{display: "inline-block", width: "21%"}}>
                                                    <Label bsStyle="default" style={{fontWeight: "600"}}>{eachTabRun.method.toUpperCase()}</Label>
                                                </div>
                                                <div style={{paddingLeft: "5px", display: "inline-block", wordBreak: "break-word", width: "78%", verticalAlign: "middle", fontSize: "12px", color: "#9CA5AB" , cursor: "pointer"}}>
                                                    {eachTabRun.apiPath}
                                                </div>
                                            </div>
                                        ); */
                          return (
                            <Treebeard
                              key={eachTabRun.id}
                              data={eachTabRun}
                              style={CollectionTreeCSS}
                              onToggle={this.onToggle}
                              decorators={{
                                ...decorators,
                                Header: this.renderTreeNodeHeader,
                                Container: this.renderTreeNodeContainer,
                                Toggle: this.renderTreeNodeToggle,
                              }}
                            />
                          );
                        })}
                      </Panel.Body>
                    </Panel.Collapse>
                  </Panel>
                );
              })}
            </div>
          </Tab>
          <Tab eventKey={2} title="Collections">
            <div className="margin-top-10">
              <div className="value-n"></div>
            </div>
            <div className="margin-top-10">
              {userCollections &&
                userCollections.map((eachCollec) => {
                  return (
                    <Panel
                      id="collapsible-panel-example-2"
                      className="collection-panel-div"
                      key={eachCollec.collec}
                      value={eachCollec.collec}
                      onClick={() => this.handlePanelClick(eachCollec.collec)}
                      defaultExpanded={this.getExpendedState(eachCollec.collec)}
                      onToggle={this.onPanelToggle}
                    >
                      <Panel.Heading
                        style={{ paddingLeft: "9px", position: "relative" }}
                      >
                        <Panel.Title
                          toggle
                          style={{ fontSize: "13px" }}
                          data-unique-id={eachCollec.collec}
                        >
                          {eachCollec.name}
                        </Panel.Title>
                        <div className="collection-options">
                          <i
                            className="fas fa-trash pointer"
                            data-id={eachCollec.rootRcrdngId}
                            data-name={eachCollec.name}
                            title="Delete"
                            data-type="collection"
                            onClick={this.onDeleteBtnClick}
                          />
                        </div>
                      </Panel.Heading>
                      <Panel.Collapse>
                        <Panel.Body>
                          {eachCollec.apiTraces &&
                            eachCollec.apiTraces.map((eachApiTrace) => {
                              if (
                                this.persistPanelState[
                                  eachApiTrace.requestEventId
                                ]
                              ) {
                                eachApiTrace.toggled = true;
                              }
                              return (
                                <Treebeard
                                  key={eachApiTrace.id}
                                  data={eachApiTrace}
                                  style={CollectionTreeCSS}
                                  onToggle={this.onToggle}
                                  decorators={{
                                    ...decorators,
                                    Header: this.renderTreeNodeHeader,
                                    Container: this.renderTreeNodeContainer,
                                    Toggle: this.renderTreeNodeToggle,
                                  }}
                                />
                              );
                            })}
                        </Panel.Body>
                      </Panel.Collapse>
                    </Panel>
                  );
                })}
            </div>
          </Tab>
        </Tabs>

        <Modal show={showDeleteGoldenConfirmation}>
          <Modal.Body>
            <div style={{ display: "flex", flex: 1, justifyContent: "center" }}>
              <div
                className="margin-right-10"
                style={{
                  display: "flex",
                  flexDirection: "column",
                  fontSize: 20,
                }}
              >
                This will delete the {itemToDelete.name}. Please confirm.
              </div>
              <div style={{ display: "flex", alignItems: "flex-start" }}>
                <span
                  className="cube-btn margin-right-10"
                  onClick={() => this.deleteItem()}
                >
                  Confirm
                </span>
                <span
                  className="cube-btn"
                  onClick={() =>
                    this.setState({
                      showDeleteGoldenConfirmation: false,
                      itemToDelete: {},
                    })
                  }
                >
                  No
                </span>
              </div>
            </div>
          </Modal.Body>
        </Modal>
      </>
    );
  }
}

function mapStateToProps(state) {
  const { cube, apiCatalog, httpClient } = state;
  return {
    cube,
    apiCatalog,
    httpClient,
  };
}

export default connect(mapStateToProps)(SideBarTabs);
