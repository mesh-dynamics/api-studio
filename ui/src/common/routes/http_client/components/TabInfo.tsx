/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
