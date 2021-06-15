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

import React, { Component } from "react";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import Editor from "../../components/Editor/Editor";
export interface IHttpRequestRawDataProps {
  showRawData: boolean;
  rawData: string;
  tabId: string;
  isOutgoingRequest: boolean;
  updateParam: UpdateParamHandler;
  readOnly: boolean;
  paramName: string;
}

export interface IHttpRequestRawDataState {
  showError: boolean;
}

class HttpRequestRawData extends Component<
  IHttpRequestRawDataProps,
  IHttpRequestRawDataState
> {
  private editorRef : CodeMirror.Editor;
  constructor(props: IHttpRequestRawDataProps) {
    super(props);
    this.state = {
      showError: false,
    };
    this.handleChange = this.handleChange.bind(this);
    this.formatHandler = this.formatHandler.bind(this);
  }

  handleChange(value: string) {
    const { tabId, isOutgoingRequest, rawData } = this.props;
    if (value !== rawData) {
      this.props.updateParam(
        isOutgoingRequest,
        tabId,
        this.props.paramName,
        this.props.paramName,
        value
      );
    }
  }
  formatHandler = () => {
    this.setState({ showError: false });
    try {
      this.editorRef &&  (this.editorRef as any).format();
    } catch (error) {
      this.setState({ showError: true });
    }
  };

  hideError = () => {
    this.setState({ showError: false });
  };

  render() {
    const showRawData = this.props.showRawData;
    return showRawData ? (
      <div style={{ height: "100%", minHeight: "100px" }}>
        <div
          style={{
            width: "100%",
            display: this.props.readOnly ? "none" : "block",
          }}
        >
          {this.state.showError ? (
            <>
              <span style={{ color: "red", marginRight: "10px" }}>
                Couldn't format. Please validate the document for any errors.
              </span>
              <i
                className="fa fa-times"
                title="Hide error"
                aria-hidden="true"
                onClick={this.hideError}
              ></i>
            </>
          ) : (
            <></>
          )}
        </div>
        <div style={{ height: "100%" }}>
        <Editor
            value={this.props.rawData}
            onChange={this.handleChange}
            language="json"
            getEditorRef = {(editor)=> this.editorRef = editor}
            readonly={ this.props.readOnly}
          />
        </div>
      </div>
    ) : (
      <div></div>
    );
  }
}

export default HttpRequestRawData;
