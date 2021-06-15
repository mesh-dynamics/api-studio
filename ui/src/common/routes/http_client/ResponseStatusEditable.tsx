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

import React, { useState, ChangeEvent, useEffect } from "react";
import { connect } from "react-redux";
import { httpClientActions } from "../../actions/httpClientActions";
import { FormControl } from "react-bootstrap";
import { IStoreState } from "../../reducers/state.types";

import { getHttpStatusColor, getGrpcStatusColor } from "../../utils/http_client/utils";
import statusCodeList, { getHttpStatus } from "../../status-code-list";
import classNames from "classnames";
import {getGrpcStatusText, gRPCStatusCodes} from "../../../shared/grpc-status-codes"

export interface IResponseStatusEditableProps {
  tabId: string;
  clientTabId: string;
  isRecordingStatus: boolean;
  status: string;
  requestRunning: boolean;
  dispatch: any;
  isGrpc: boolean;
}

function ResponseStatusEditable(props: IResponseStatusEditableProps) {
  const [inEditMode, setInEditMode] = useState(false);
  const [inputRef, setInputRef] = useState<HTMLInputElement>(null);

  const onDocumentClick = React.useCallback(
    (event: MouseEvent) => {
      if (!(event.target == inputRef)) {
        setInEditMode(false);
      }
    },
    [setInEditMode, inputRef]
  );

  useEffect(() => {
    if (inEditMode) {
      document.addEventListener("click", onDocumentClick);
    } else {
      document.removeEventListener("click", onDocumentClick);
    }
    return () => {
      document.removeEventListener("click", onDocumentClick);
    };
  }, [inEditMode, inputRef]);

  const onLabelClick = () => {
    props.isRecordingStatus && setInEditMode(true);
  };

  const onChangeStatus = (
    event: ChangeEvent<FormControl & HTMLSelectElement>
  ) => {
    props.dispatch(
      httpClientActions.updateHttpStatusInTab(
        props.tabId,
        props.clientTabId,
        event.target.value,
        getHttpStatus(event.target.value)
      )
    );
  };
  const {isGrpc} = props;

  if (inEditMode) {
    return (
      <div className="editable-options">
        <FormControl
          bsSize="sm"
          inputRef={(instance) => setInputRef(instance)}
          componentClass="select"
          value={props.status}
          onChange={onChangeStatus}
        >
          <option disabled={true}>Update Status</option>
          {isGrpc ? <>
            {
              Object.entries(gRPCStatusCodes).map(([code, text]) => (
                <option key={code} value={code}>
                  {text} ({code})
                </option>
              ))
            }
            </> 
            : <>
              {statusCodeList.map((statusCode) => (
                <option key={statusCode.status} value={statusCode.status}>
                  {statusCode.value}
                </option>
              ))}
            </>
          }
        </FormControl>
      </div>
    );
  }

  const statusText = isGrpc ? getGrpcStatusText(props.status) : getHttpStatus(props.status)
  const classes = classNames({
    isEditableLabel: props.isRecordingStatus,
  });
  return (
    <b
      style={{
        color: isGrpc ? getGrpcStatusColor(props.status) : getHttpStatusColor(props.status),
      }}
      className={classes}
      onClick={onLabelClick}
    >
      {" "}
      {props.requestRunning
        ? "WAITING..."
        : statusText
      }
    </b>
  );
}

const mapStateToProps = (state: IStoreState) => ({
  cube: state.cube,
  apiCatalog: state.apiCatalog,
  httpClient: state.httpClient,
  user: state.authentication.user,
});

export default connect(mapStateToProps)(ResponseStatusEditable);
