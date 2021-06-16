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
import { connect } from "react-redux";
import { httpClientActions } from "../../actions/httpClientActions";
import {
  Form,
  FormControl,
  ControlLabel,
  FormGroup,
  Button,
} from "react-bootstrap";
import { IStoreState } from "../../reducers/state.types";
import Tippy from "@tippy.js/react";

export interface IHistoryTabFilterProps {
  dispatch: any;
  historyPathFilterText: string;
  isCollectionLoading: boolean;
}

function HistoryTabFilter(props: IHistoryTabFilterProps) {
  const onHistoryPathFilterTextChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLInputElement>) => {
      const target = event.target as HTMLInputElement;
      props.dispatch(
        httpClientActions.updateHistoryPathFilterText(target.value)
      );
    },
    []
  );
  const onSearchClick = (
    event: React.MouseEvent<Button> & React.FormEvent<HTMLFormElement>
  ) => {
    !props.isCollectionLoading &&
      props.dispatch(httpClientActions.historyTabFirstPage());
    event.preventDefault();

    return false;
  };
  return (
    <Form inline className="margin-bottom-5" onSubmit={onSearchClick}>
      <FormGroup controlId="formInlineName" bsSize="sm">
        <Tippy
          content={"Filter History by api path"}
          arrow={true}
          enabled={true}
          arrowType="round"
          size="large"
          placement="bottom"
        >
          <input
            className="form-control"
            type="text"
            placeholder="Filter by path"
            value={props.historyPathFilterText}
            onChange={onHistoryPathFilterTextChange}
          />
        </Tippy>
        <Button
          className="btn cube-btn"
          type="button"
          onClick={onSearchClick}
          style={{ marginLeft: "2px", marginBottom: "0px" }}
        >
          <i className="fa fa-search"></i>
        </Button>
      </FormGroup>
    </Form>
  );
}

const mapStateToProps = (state: IStoreState) => ({
  historyPathFilterText: state.httpClient.historyPathFilterText,
  isCollectionLoading: state.httpClient.isCollectionLoading,
});

export default connect(mapStateToProps)(HistoryTabFilter);
