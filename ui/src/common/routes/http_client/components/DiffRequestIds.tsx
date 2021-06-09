import React, { useEffect, useState } from "react";
import { Checkbox, FormGroup, Glyphicon, Label, FormControl } from "react-bootstrap";
import { connect } from "react-redux";
import { IReqRespMatchResultResponseRes } from "../../../../common/apiResponse.types";
import { IDiffLayoutData, IStoreState } from "../../../../common/reducers/state.types";
import { cubeService } from "../../../../common/services";
import { validateAndCreateDiffLayoutData, updateResolutionFilterPaths, addCompressToggleData } from "../../../utils/diff/diff-process.js";
import ReactDiffViewer from "../../../utils/diff/diff-main";
import config from "../../../config";
import { getHttpStatus } from "../../../../common/status-code-list.js";

const newStyles = {
  variables: {
    addedBackground: "#e6ffed !important",
    addedColor: "#24292e  !important",
    removedBackground: "#ffeef0  !important",
    removedColor: "#24292e  !important",
    wordAddedBackground: "#acf2bd  !important",
    wordRemovedBackground: "#fdb8c0  !important",
    addedGutterBackground: "#cdffd8  !important",
    removedGutterBackground: "#ffdce0  !important",
    gutterBackground: "#f7f7f7  !important",
    gutterBackgroundDark: "#f3f1f1  !important",
    highlightBackground: "#fffbdd  !important",
    highlightGutterBackground: "#fff5b1  !important",
  },
  line: {
    padding: "10px 2px",
    "&:hover": {
      background: "#f7f7f7",
    },
  },
};

const initialState: IDiffRequestDisplayFilters = {
  showResponseMessageHeaders: false,
  shownResponseMessageHeaders: false,
  showResponseMessageBody: true,
  shownResponseMessageBody: true,
  showRequestMessageHeaders: false,
  shownRequestMessageHeaders: false,
  showRequestMessageQParams: false,
  shownRequestMessageQParams: false,
  showRequestMessageFParams: false,
  shownRequestMessageFParams: false,
  showRequestMessageBody: false,
  shownRequestMessageBody: false,
  selectedService: "All",
  selectedAPI: "All",
  selectedRequestMatchType: "All",
  selectedResponseMatchType: "All",
  selectedResolutionType: "All",
  showAll: true,
  searchFilterPath: "",
  collapseLength: parseInt(config.diffCollapseLength.toString()),
  collapseLengthIncrement: parseInt(config.diffCollapseLengthIncrement.toString()),
  maxLinesLength: parseInt(config.diffMaxLinesLength.toString()),
  maxLinesLengthIncrement: parseInt(config.diffMaxLinesLengthIncrement.toString()),
};

export function DiffRequestIds(props: IDiffRequestIdsProps) {
  const [diffLayoutData, setDiffLayoutData] = useState<IDiffLayoutData[]>([]);
  const [loading, setLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [filtersState, setFiltersState] = useState(initialState);
  const inputElementRef = React.useRef<HTMLInputElement & FormControl>(null);

  const preProcessResults = (results: IReqRespMatchResultResponseRes[]) => {
    const app = undefined,
      replayId = undefined,
      recordingId = undefined,
      templateVersion = undefined;
    let diffLayoutData: IDiffLayoutData[] = validateAndCreateDiffLayoutData(
      results,
      app,
      replayId,
      recordingId,
      templateVersion,
      config.diffCollapseLength,
      config.diffMaxLinesLength
    );
    updateResolutionFilterPaths(diffLayoutData);
    return diffLayoutData;
  };
  const toggleMessageContents = (e: any) => {
    if (e.target.value === "responseHeaders") setFiltersState({ ...filtersState, showResponseMessageHeaders: e.target.checked, shownResponseMessageHeaders: true });
    if (e.target.value === "responseBody") setFiltersState({ ...filtersState, showResponseMessageBody: e.target.checked, shownResponseMessageBody: true });
    if (e.target.value === "requestHeaders") setFiltersState({ ...filtersState, showRequestMessageHeaders: e.target.checked, shownRequestMessageHeaders: true });
    if (e.target.value === "requestQParams") setFiltersState({ ...filtersState, showRequestMessageQParams: e.target.checked, shownRequestMessageQParams: true });
    if (e.target.value === "requestFParams") setFiltersState({ ...filtersState, showRequestMessageFParams: e.target.checked, shownRequestMessageFParams: true });
    if (e.target.value === "requestBody") setFiltersState({ ...filtersState, showRequestMessageBody: e.target.checked, shownRequestMessageBody: true });

    setTimeout(() => {
      const {
        showResponseMessageHeaders,
        showResponseMessageBody,
        showRequestMessageHeaders,
        showRequestMessageQParams,
        showRequestMessageFParams,
        showRequestMessageBody,
      } = filtersState;

      if (
        showResponseMessageHeaders === false &&
        showResponseMessageBody === false &&
        showRequestMessageHeaders === false &&
        showRequestMessageQParams === false &&
        showRequestMessageFParams === false &&
        showRequestMessageBody === false
      ) {
        setFiltersState({ ...filtersState, showResponseMessageBody: true, shownResponseMessageBody: true });
      }
    });
  };
  const handleSearchFilterChange = (e: React.ChangeEvent<HTMLInputElement & FormControl>) => {
    setFiltersState({ ...filtersState, searchFilterPath: e.target.value });
  };

  const increaseCollapseLength = (e: any, jsonPath: string, recordReqId: string, replayReqId: string, typeOfChunkHandler: string) => {
    const { collapseLength, collapseLengthIncrement, maxLinesLength, maxLinesLengthIncrement } = filtersState;
    let newCollapseLength = collapseLength,
      newMaxLinesLength = maxLinesLength;
    if (typeOfChunkHandler === "collapseChunkLength") {
      newCollapseLength = collapseLength + collapseLengthIncrement;
    } else {
      newMaxLinesLength = maxLinesLength + maxLinesLengthIncrement;
    }

    let newDiffLayoutData = diffLayoutData.map((diffItem) => {
      if (diffItem.replayReqId === replayReqId) {
        addCompressToggleData(diffItem.reductedDiffArray, newCollapseLength, newMaxLinesLength);
      }
      return diffItem;
    });

    setFiltersState({
      ...filtersState,
      collapseLength: newCollapseLength,
      maxLinesLength: newMaxLinesLength,
    });
    setDiffLayoutData(newDiffLayoutData);
  };

  useEffect(() => {
    setLoading(true);
    setIsError(false);
    cubeService
      .getReqRespMatchResult(props.lhsReqId, props.rhsReqId)
      .then((serverRes) => {
        const results = serverRes.res && [serverRes.res];
        const diffLayoutData = preProcessResults(results);
        setDiffLayoutData(diffLayoutData);
        setLoading(false);
      })
      .catch((error) => {
        setLoading(false);
        setIsError(true);
        console.error("error: ", error);
      });
  }, [props.lhsReqId, props.rhsReqId]);

  const selectedDiffItem = diffLayoutData ? diffLayoutData[0] : null;
  let filterPaths: string[] = [];
  
  if (loading) {
    return <div>Request match is loading <i className="fa fa-spinner fa-spin"></i></div>;
  } else {
    if (isError) {
      return <div className="red">Some error occurred while request match was loading</div>;
    } else if (!selectedDiffItem) {
      return <div>Diff item not found</div>;
    } else {
      return (
        <div style={{ marginTop: "27px", backgroundColor: "#fff", padding: "9px" }}>
          <div style={{ opacity: 0.6, marginTop: "9px" }}>
            <h4>
              <Glyphicon style={{ visibility: "visible", paddingRight: "5px", fontSize: "14px" }} glyph="random" /> <span>Selected Diff</span>
            </h4>
          </div>
          <FormGroup>
            <Checkbox inline onChange={toggleMessageContents} value="requestHeaders" checked={filtersState.showRequestMessageHeaders}>
              Request Headers
            </Checkbox>
            <Checkbox inline onChange={toggleMessageContents} value="requestQParams" checked={filtersState.showRequestMessageQParams}>
              Request Query Params
            </Checkbox>
            <Checkbox inline onChange={toggleMessageContents} value="requestFParams" checked={filtersState.showRequestMessageFParams}>
              Request Form Params
            </Checkbox>
            <Checkbox inline onChange={toggleMessageContents} value="requestBody" checked={filtersState.showRequestMessageBody}>
              Request Body
            </Checkbox>
            <span style={{ height: "18px", borderRight: "2px solid #333", paddingLeft: "18px", marginRight: "18px" }}></span>
            <Checkbox inline onChange={toggleMessageContents} value="responseHeaders" checked={filtersState.showResponseMessageHeaders}>
              Response Headers
            </Checkbox>
            <Checkbox inline onChange={toggleMessageContents} value="responseBody" checked={filtersState.showResponseMessageBody}>
              Response Body
            </Checkbox>
            <span style={{ height: "18px", borderRight: "2px solid #333", paddingLeft: "18px" }}></span>
            
            <FormControl
              style={{ marginBottom: "12px", marginTop: "10px" }}
              ref={inputElementRef}
              type="text"
              placeholder="Search"
              id="filterPathInputId"
              value={filtersState.searchFilterPath}
              onChange={handleSearchFilterChange}
            />
          </FormGroup>
          <div style={{ marginTop: "9px" }}>
            {(filtersState.showRequestMessageHeaders || filtersState.shownRequestMessageHeaders) && (
              <div style={{ display: filtersState.showRequestMessageHeaders ? "" : "none" }}>
                <h4>
                  <Label bsStyle="primary" style={{ textAlign: "left", fontWeight: 400 }}>
                    Request Headers
                  </Label>
                </h4>
                <div className="headers-diff-wrapper" style={{ border: "1px solid #ccc" }}>
                  <ReactDiffViewer
                    styles={newStyles}
                    oldValue={JSON.stringify(selectedDiffItem.recordedRequestHeaders, undefined, 4)}
                    newValue={JSON.stringify(selectedDiffItem.replayedRequestHeaders, undefined, 4)}
                    splitView={true}
                    disableWordDiff={false}
                    diffArray={selectedDiffItem.reductedDiffArrayReqHeaders}
                    onLineNumberClick={(lineId, e) => {
                      return;
                    }}
                    filterPaths={filterPaths}
                    inputElementRef={inputElementRef}
                    showAll={filtersState.showAll}
                    searchFilterPath={filtersState.searchFilterPath}
                    disableOperationSet={true}
                    enableClientSideDiff={true}
                  />
                </div>
              </div>
            )}
            {(filtersState.showRequestMessageQParams || filtersState.shownRequestMessageQParams) && (
              <div style={{ display: filtersState.showRequestMessageQParams ? "" : "none" }}>
                <h4>
                  <Label bsStyle="primary" style={{ textAlign: "left", fontWeight: 400 }}>
                    Request Query Params
                  </Label>
                </h4>
                <div className="headers-diff-wrapper" style={{ border: "1px solid #ccc" }}>
                  <ReactDiffViewer
                    styles={newStyles}
                    oldValue={JSON.stringify(selectedDiffItem.recordedRequestQParams, undefined, 4)}
                    newValue={JSON.stringify(selectedDiffItem.replayedRequestQParams, undefined, 4)}
                    splitView={true}
                    disableWordDiff={false}
                    diffArray={selectedDiffItem.reductedDiffArrayReqQParams}
                    onLineNumberClick={(lineId, e) => {
                      return;
                    }}
                    filterPaths={filterPaths}
                    inputElementRef={inputElementRef}
                    showAll={filtersState.showAll}
                    searchFilterPath={filtersState.searchFilterPath}
                    disableOperationSet={true}
                    enableClientSideDiff={true}
                  />
                </div>
              </div>
            )}
            {(filtersState.showRequestMessageFParams || filtersState.shownRequestMessageFParams) && (
              <div style={{ display: filtersState.showRequestMessageFParams ? "" : "none" }}>
                <h4>
                  <Label bsStyle="primary" style={{ textAlign: "left", fontWeight: 400 }}>
                    Request Form Params
                  </Label>
                </h4>
                <div className="headers-diff-wrapper" style={{ border: "1px solid #ccc" }}>
                  <ReactDiffViewer
                    styles={newStyles}
                    oldValue={JSON.stringify(selectedDiffItem.recordedRequestFParams, undefined, 4)}
                    newValue={JSON.stringify(selectedDiffItem.replayedRequestFParams, undefined, 4)}
                    splitView={true}
                    disableWordDiff={false}
                    diffArray={selectedDiffItem.reductedDiffArrayReqFParams}
                    onLineNumberClick={(lineId, e) => {
                      return;
                    }}
                    filterPaths={filterPaths}
                    inputElementRef={inputElementRef}
                    showAll={filtersState.showAll}
                    searchFilterPath={filtersState.searchFilterPath}
                    disableOperationSet={true}
                    enableClientSideDiff={true}
                  />
                </div>
              </div>
            )}
            {(filtersState.showRequestMessageBody || filtersState.shownRequestMessageBody) && (
              <div style={{ display: filtersState.showRequestMessageBody ? "" : "none" }}>
                <h4>
                  <Label bsStyle="primary" style={{ textAlign: "left", fontWeight: 400 }}>
                    Request Body
                  </Label>
                </h4>
                <div className="headers-diff-wrapper" style={{ border: "1px solid #ccc" }}>
                  <ReactDiffViewer
                    styles={newStyles}
                    oldValue={JSON.stringify(selectedDiffItem.recordedRequestBody, undefined, 4)}
                    newValue={JSON.stringify(selectedDiffItem.replayedRequestBody, undefined, 4)}
                    splitView={true}
                    disableWordDiff={false}
                    diffArray={selectedDiffItem.reductedDiffArrayReqBody}
                    onLineNumberClick={(lineId, e) => {
                      return;
                    }}
                    filterPaths={filterPaths}
                    inputElementRef={inputElementRef}
                    showAll={filtersState.showAll}
                    searchFilterPath={filtersState.searchFilterPath}
                    disableOperationSet={true}
                    enableClientSideDiff={true}
                  />
                </div>
              </div>
            )}
            {(filtersState.showResponseMessageHeaders || filtersState.shownResponseMessageHeaders) && (
              <div style={{ display: filtersState.showResponseMessageHeaders ? "" : "none" }}>
                <h4>
                  <Label bsStyle="primary" style={{ textAlign: "left", fontWeight: 400 }}>
                    Response Headers
                  </Label>
                </h4>
                <div className="headers-diff-wrapper" style={{ border: "1px solid #ccc" }}>
                  <ReactDiffViewer
                    styles={newStyles}
                    oldValue={JSON.stringify(selectedDiffItem.recordedResponseHeaders, undefined, 4)}
                    newValue={JSON.stringify(selectedDiffItem.replayedResponseHeaders, undefined, 4)}
                    splitView={true}
                    disableWordDiff={false}
                    diffArray={selectedDiffItem.updatedReducedDiffArrayRespHdr}
                    onLineNumberClick={(lineId, e) => {
                      return;
                    }}
                    filterPaths={filterPaths}
                    inputElementRef={inputElementRef}
                    showAll={filtersState.showAll}
                    searchFilterPath={filtersState.searchFilterPath}
                    disableOperationSet={true}
                    enableClientSideDiff={true}
                  />
                </div>
              </div>
            )}
            {
              <div style={{ display: filtersState.showResponseMessageBody ? "" : "none" }}>
                <div className="row">
                  <div className="col-md-6">
                    <h4>
                      <Label bsStyle="primary" style={{ textAlign: "left", fontWeight: 400 }}>
                        Response Body
                      </Label>
                      &nbsp;&nbsp;
                      {selectedDiffItem.recordResponse ? (
                        <span className="font-12">
                          Status:&nbsp;<span className="green">{getHttpStatus(selectedDiffItem.recordResponse.status)}</span>
                        </span>
                      ) : (
                        <span className="font-12" style={{ color: "magenta" }}>
                          No Recorded Data
                        </span>
                      )}
                    </h4>
                  </div>

                  <div className="col-md-6">
                    <h4 style={{ marginLeft: "18%" }}>
                      {selectedDiffItem.replayResponse ? (
                        <span className="font-12">
                          Status:&nbsp;<span className="green">{getHttpStatus(selectedDiffItem.replayResponse.status)}</span>
                        </span>
                      ) : (
                        <span className="font-12" style={{ color: "magenta" }}>
                          No Replayed Data
                        </span>
                      )}
                    </h4>
                  </div>
                </div>
                <div>
                  {selectedDiffItem.missedRequiredFields.map((eachMissedField) => {
                    return (
                      <div>
                        <span style={{ paddingRight: "5px" }}>{eachMissedField.path}:</span>
                        <span>{eachMissedField.fromValue}</span>
                      </div>
                    );
                  })}
                </div>
                {(selectedDiffItem.recordedData || selectedDiffItem.replayedData) && (
                  <div className="diff-wrapper" style={{ border: "1px solid #ccc" }}>
                    <ReactDiffViewer
                      styles={newStyles}
                      oldValue={selectedDiffItem.expJSON}
                      newValue={selectedDiffItem.actJSON}
                      splitView={true}
                      disableWordDiff={false}
                      diffArray={selectedDiffItem.reductedDiffArray}
                      filterPaths={filterPaths}
                      onLineNumberClick={(lineId, e) => {
                        return;
                      }}
                      inputElementRef={inputElementRef}
                      showAll={filtersState.showAll}
                      searchFilterPath={filtersState.searchFilterPath}
                      disableOperationSet={true}
                      handleCollapseLength={increaseCollapseLength}
                      handleMaxLinesLength={increaseCollapseLength}
                      enableClientSideDiff={true}
                    />
                  </div>
                )}
              </div>
            }
          </div>
        </div>
      );
    }
  }
}

export interface IDiffRequestIdsProps {
  lhsReqId: string;
  rhsReqId: string;
}

interface IDiffRequestDisplayFilters {
  showResponseMessageHeaders: boolean;
  shownResponseMessageHeaders: boolean;
  showResponseMessageBody: boolean;
  shownResponseMessageBody: boolean;
  showRequestMessageHeaders: boolean;
  shownRequestMessageHeaders: boolean;
  showRequestMessageQParams: boolean;
  shownRequestMessageQParams: boolean;
  showRequestMessageFParams: boolean;
  shownRequestMessageFParams: boolean;
  showRequestMessageBody: boolean;
  shownRequestMessageBody: boolean;
  selectedService: string;
  selectedAPI: string;
  searchFilterPath: string;
  selectedRequestMatchType: string;
  selectedResponseMatchType: string;
  selectedResolutionType: string;
  showAll: boolean;
  collapseLength: number;
  collapseLengthIncrement: number;
  maxLinesLength: number;
  maxLinesLengthIncrement: number;
}

const mapStateToProps = (state: IStoreState) =>
  ({
    app: state.cube.selectedApp,
    user: state.authentication.user,
  } as Partial<IDiffRequestIdsProps>);

const connectedRequestMatchType = connect(mapStateToProps)(DiffRequestIds);

export default connectedRequestMatchType;
