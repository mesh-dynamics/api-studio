import React, { useCallback, useState } from "react";
import { Button, Glyphicon, Tab } from "react-bootstrap";
import { connect } from "react-redux";
import { CubeButton } from "../../../../common/components/common/CubeButton";
import { httpClientActions } from "../../../../common/actions/httpClientActions";
import { IHttpClientTabDetails, IKeyValuePairs, IStoreState } from "../../../../common/reducers/state.types";
import { deriveTabNameFromTabObject, getTabByTabId } from "../../../../common/utils/http_client/httpClientUtils";
import { UpdatePollingMetaDataDialog } from "./UpdatePollingMetaDataDialog";
import EditableLabel from "../EditableLabel";
import "./TabInfo.css"
export interface ITabInfoProps {
  tabId: string;
  tab: IHttpClientTabDetails;
  dispatch: any;
}
function TabInfo(props: ITabInfoProps) {
  const [showDialog, setShowDialog] = useState(false);
  const onHidePollingDialog = useCallback(() => {
    setShowDialog(false);
  }, []);
  const onShowPollingDialog = useCallback(() => {
    setShowDialog(true);
  }, []);
  const onMetaDataChanged = (pollingMetaData: IKeyValuePairs) => {
    props.dispatch(httpClientActions.updateMetadata(props.tabId, pollingMetaData));
    onHidePollingDialog();
  };
  let requestMetaData: IKeyValuePairs = {};
  // let apiPath = "New";
  if (props.tab && props.tab.eventData) {
    const requestEvent = props.tab.eventData.find((u) => u.eventType == "HTTPRequest");
    if (requestEvent) {
      requestMetaData = requestEvent.metaData;
    }
  }
  const renderCollection = () => {
    if (props.tab && props.tab.collectionNameAddedFromClient) {
      return (
        <span className="top-bar-collec">
          <b>Collection:</b> {props.tab.collectionNameAddedFromClient} &nbsp;
        </span>
      );
    }
    return null;
  };
  const onEditTitle = (text: string) => {
    const metaData = { name: text };
    props.dispatch(httpClientActions.updateMetadata(props.tabId, metaData));
  };
  const renderName = () => { 
    const name = deriveTabNameFromTabObject(props.tab);
    return (
      <span className="top-bar-name" title="Name of tab" style={{ display: "inline-flex" }}>
        <b>Name:&nbsp;</b> 
        <EditableLabel 
          handleEditComplete={onEditTitle} 
          label={name} 
          textClassName="tab-name-label-text"
        />
      </span>
    ); 
  };
  return (
    <div>
      <CubeButton onClick={onShowPollingDialog} title="Update polling data" faIcon="fa-history" />
      {renderCollection()}
      {renderName()}
      <UpdatePollingMetaDataDialog metaData={requestMetaData} onHide={onHidePollingDialog} showDialog={showDialog} onMetaDataChanged={onMetaDataChanged} />
    </div>
  );
}

function mapStateToProps(state: IStoreState, props: ITabInfoProps) {
  const {
    httpClient: { tabs },
  } = state;
  const tab = getTabByTabId(tabs, props.tabId)!;

  return {
    tab,
  };
}

const connectedTabInfo = connect(mapStateToProps)(TabInfo);

export default connectedTabInfo;
