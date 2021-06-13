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

import { connect } from "react-redux";
import { IConfigVars, IStoreState } from "../../../reducers/state.types";
import React, { InputHTMLAttributes } from "react";
import "./AutoComplete.css";
import { listOfHeaders, autocomplete } from "../../../helpers/autocompleteHelper";

export interface IAutoCompleteBoxProps extends InputHTMLAttributes<HTMLInputElement> {
  name: string;
  value: string;
  readOnly: boolean;
  id: string;
  headerList?: boolean;
  envVars: IConfigVars[];
  dispatch: any;
}
function AutoCompleteBox(props: IAutoCompleteBoxProps) {
  const inputRef = React.useRef<HTMLInputElement>(null);
  if (inputRef.current) {
    const initiatingAutoCompleteWords = props.headerList ? listOfHeaders : [];
    const anywhereList = props.envVars.map((u) => ({ key: `{{{${u.key}}}}`, value: u.value }));
    new autocomplete(inputRef.current, initiatingAutoCompleteWords, anywhereList);
  }

  let inputProps = {...props };
  delete inputProps.envVars;
  delete inputProps.headerList;
  delete inputProps.dispatch;

  return (
    <input
      {...inputProps}
      autoComplete="off"
      ref={inputRef}
      type="text"
      className="form-control"
    />
  );
}

const mapStateToProps = (state: IStoreState) => {
  let envVars: IConfigVars[] = [];
  const {
    httpClient: { environmentList, selectedEnvironment },
  } = state;
  const selectedEnv = environmentList.find((env) => env.name == selectedEnvironment);
  if (selectedEnv && selectedEnv.vars) {
    envVars = selectedEnv.vars;
  }
  return {
    envVars,
  };
};

export default connect(mapStateToProps)(AutoCompleteBox);
