import React, { useState, ChangeEvent, useEffect } from "react";
import { connect } from "react-redux";
import { httpClientActions } from "../../actions/httpClientActions";
import { FormControl } from "react-bootstrap";
import { IStoreState } from "../../reducers/state.types";

import { getStatusColor } from "../../utils/http_client/utils";
import statusCodeList, { getHttpStatus } from "../../status-code-list";
import classNames from "classnames";

export interface IResponseStatusEditableProps {
  tabId: string;
  clientTabId: string;
  isRecordingStatus: boolean;
  status: string;
  requestRunning: boolean;
  dispatch: any;
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
          {statusCodeList.map((statusCode) => (
            <option key={statusCode.status} value={statusCode.status}>
              {statusCode.value}
            </option>
          ))}
        </FormControl>
      </div>
    );
  }

  const classes = classNames({
    isEditableLabel: props.isRecordingStatus,
  });
  return (
    <b
      style={{
        color: props.status && getStatusColor(props.status),
      }}
      className={classes}
      onClick={onLabelClick}
    >
      {" "}
      {props.requestRunning
        ? "WAITING..."
        : props.status
        ? getHttpStatus(props.status)
        : "NA"}
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
