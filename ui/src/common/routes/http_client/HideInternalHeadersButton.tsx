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

import React from "react";
import { Badge, Button } from "react-bootstrap";
import { filterInternalHeaders } from "../../utils/http_client/utils";
import { connect } from "react-redux";
import { IRequestParamData, IStoreState } from "../../reducers/state.types";
import { httpClientActions } from "../../actions/httpClientActions";

export interface IHideInternalHeadersButtonProps {
  clientTabId: string;
  hideInternalHeaders: boolean;
  hiddenCount: number;
  headers: IRequestParamData[];
  dispatch: any;
}

function HideInternalHeadersButton(props: IHideInternalHeadersButtonProps) {
  if (!props.clientTabId) {
    return <></>;
  }
  const onButtonClick = () => {
    props.dispatch(httpClientActions.toggleHideInternalHeaders(props.clientTabId));
  };

  return (
    <div className="pull-right">
      <Button bsStyle="link" onClick={onButtonClick} className="font-12">
        {props.hideInternalHeaders
          ? <>Show all headers <Badge title="Hidden headers count">+{props.hiddenCount}</Badge></>
          : "Hide internal headers"}
      </Button>
    </div>
  );
}

const mapStateToProps = (
  state: IStoreState,
  props: IHideInternalHeadersButtonProps
) => {
  const currentTab = state.httpClient.tabs.find((tab) => tab.id == props.clientTabId);
  const hideInternalHeaders = !!currentTab?.hideInternalHeaders;
  let hiddenCount = 0;
  if (hideInternalHeaders && currentTab) {
    hiddenCount =
      props.headers.length -
      filterInternalHeaders(props.headers, hideInternalHeaders).length;
  }
  return {
    hideInternalHeaders,
    hiddenCount,
  };
};

export default connect(mapStateToProps)(HideInternalHeadersButton);
