import React, { ChangeEvent, Component, FormEvent } from "react";

import HttpRequestFormData from "./HttpRequestFormData";
import HttpRequestMultipartData, { IMultipartData } from "./HttpRequestMultipartData";
import HttpRequestRawData from "./HttpRequestRawData";

import { FormGroup, FormControl } from "react-bootstrap";

import classNames from "classnames";

import {
  UpdateParamHandler,
  AddOrRemoveHandler,
  IFormData,
} from "./HttpResponseHeaders";
import { UpdateBodyOrRawDataTypeHandler } from "./HttpResponseHeaders";

//TODO: These params can be reduced from HttpClientTabs. Example: showRawData and rawData can be combined
export interface IHttpRequestBodyProps {
  showBody: boolean;
  tabId: string;
  rawData: string;
  grpcData: string;
  isOutgoingRequest: boolean;
  readOnly: boolean;
  bodyType: string;
  updateParam: UpdateParamHandler;
  addOrRemoveParam: AddOrRemoveHandler;
  updateAllParams: UpdateParamHandler;
  formData: IFormData[];
  multipartData: IMultipartData[];
  id: string;
  rawDataType: string;
  updateBodyOrRawDataType: UpdateBodyOrRawDataTypeHandler;
}

export interface IHttpRequestBodyState {
  bodyDataType: string;
  rawDataRef: HttpRequestRawData | null;
  grpcDataRef: HttpRequestRawData | null;
}
class HttpRequestBody extends Component<
  IHttpRequestBodyProps,
  IHttpRequestBodyState
> {
  private bodyDataTypes = ["formData","multipartData", "grpcData", "rawData"];
  constructor(props: IHttpRequestBodyProps) {
    super(props);
    this.state = {
      bodyDataType: this.props.bodyType || "formData",
      rawDataRef: null,
      grpcDataRef: null,
    };
  }

  handleBodyOrRawDataType = (
    event: FormEvent<FormControl> | ChangeEvent<HTMLInputElement>
  ) => {
    const { tabId, isOutgoingRequest } = this.props;
    const target = event.target as HTMLInputElement;
    const typeToUpdate =
      target.name === "bodyType" + this.props.id.trim()
        ? "bodyType"
        : "rawDataType";
    this.props.updateBodyOrRawDataType(
      isOutgoingRequest,
      tabId,
      typeToUpdate === "bodyType" ? "bodyType" : "rawDataType",
      target.value
    );
    if (typeToUpdate === "bodyType") {
      if (this.bodyDataTypes.indexOf(target.value) !== -1) {
        this.setState({
          bodyDataType: target.value,
        });
      }
    }
  };
  formatHandler = () => {
    const isRawData = this.state.bodyDataType == "rawData";
    const isGrpcData = this.state.bodyDataType == "grpcData";
    isRawData && this.state.rawDataRef && this.state.rawDataRef.formatHandler();
    isGrpcData &&
      this.state.grpcDataRef &&
      this.state.grpcDataRef.formatHandler();
  };
  render() {
    const isRawDataHighlighted =
      this.props.rawData && this.props.rawData.trim();
    const rawDataLabelClass = classNames({
      "request-data-label": true,
      filled: isRawDataHighlighted,
    });
    const isGrpcDataHighlighted =
      this.props.grpcData && this.props.grpcData.trim();
    const grpcDataLabelClass = classNames({
      "request-data-label": true,
      filled: isGrpcDataHighlighted,
    });
    const isFormDataExists =
      this.props.formData.findIndex((header) => header.name !== "") > -1;
    const isMultipartDataExists =
      this.props.multipartData.findIndex((data) => data.name !== "") > -1;

      const formDataLabelClass = classNames({
        "request-data-label": true,
        filled: isFormDataExists,
      });

      const multipartDataLabelClass = classNames({
        "request-data-label": true,
        filled: isMultipartDataExists,
      });
    
    const isFormData = this.state.bodyDataType == "formData";
    const isMultipartData = this.state.bodyDataType == "multipartData";
    const isRawData = this.state.bodyDataType == "rawData";
    const isGrpcData = this.state.bodyDataType == "grpcData";

    return (
      <>
        <div
          className=""
          style={{
            marginBottom: "12px",
            minHeight: "40px",
            display: this.props.showBody ? "" : "none",
          }}
        >
          <div
            className=""
            style={{
              display: "inline-block",
              paddingRight: "18px",
              opacity: "0.7",
              fontSize: "12px",
              width: "50px",
            }}
          >
            BODY
          </div>
          <div className={formDataLabelClass}>
            <input
              type="radio"
              value="formData"
              name={"bodyType" + this.props.id.trim()}
              checked={isFormData}
              onChange={this.handleBodyOrRawDataType}
            />
            x-www-form-urlencoded
          </div>
          <div className={multipartDataLabelClass}>
            <input
              type="radio"
              value="multipartData"
              name={"bodyType" + this.props.id.trim()}
              checked={isMultipartData}
              onChange={this.handleBodyOrRawDataType}
            />
            Form Data
          </div>
          <div className={rawDataLabelClass}>
            <input
              type="radio"
              value="rawData"
              name={"bodyType" + this.props.id.trim()}
              checked={isRawData}
              onChange={this.handleBodyOrRawDataType}
            />
            Raw Data
          </div>
          <div className={grpcDataLabelClass}>
            <input
              type="radio"
              value="grpcData"
              name={"bodyType" + this.props.id.trim()}
              checked={isGrpcData}
              onChange={this.handleBodyOrRawDataType}
            />
            gRPC Data
          </div>
          <div
            className=""
            style={{
              display: isRawData ? "inline-block" : "none",
              paddingRight: "5px",
              fontSize: "12px",
            }}
          >
            <FormGroup bsSize="small">
              <FormControl
                componentClass="select"
                placeholder="Method"
                style={{ fontSize: "12px" }}
                readOnly={this.props.readOnly}
                name="rawDataType"
                value={this.props.rawDataType}
                onChange={this.handleBodyOrRawDataType}
              >
                <option value="txt">Text</option>
                <option value="js">JavaScript</option>
                <option value="json">JSON</option>
                <option value="html">HTML</option>
                <option value="xml">XML</option>
              </FormControl>
            </FormGroup>
          </div>
          <div
            style={{
              float: "right",
              display:
                isFormData || isMultipartData || this.props.readOnly ? "none" : "inline-block",
            }}
          >
            <span
              className="btn btn-sm cube-btn text-center"
              style={{ padding: "2px 10px", display: "inline-block" }}
              title="Format document"
              onClick={this.formatHandler}
            >
              <i className="fa fa-align-center" aria-hidden="true"></i> Format
            </span>
          </div>
          <div style={{ clear: "both" }} />
        </div>
        <div
          style={{
            display: this.props.showBody === true ? "" : "none",
            height: "calc(100% - 70px)",
            minHeight: "100px",
          }}
        >
          <HttpRequestFormData
            tabId={this.props.tabId}
            showFormData={isFormData}
            formData={this.props.formData}
            addOrRemoveParam={this.props.addOrRemoveParam}
            updateParam={this.props.updateParam}
            isOutgoingRequest={this.props.isOutgoingRequest}
            updateAllParams={this.props.updateAllParams}
            readOnly={this.props.readOnly}
          ></HttpRequestFormData>
          <HttpRequestMultipartData
            tabId={this.props.tabId}
            showMultipartData={isMultipartData}
            multipartData={this.props.multipartData}
            addOrRemoveParam={this.props.addOrRemoveParam}
            updateParam={this.props.updateParam}
            isOutgoingRequest={this.props.isOutgoingRequest}
            updateAllParams={this.props.updateAllParams}
            readOnly={this.props.readOnly}
          ></HttpRequestMultipartData>
          <HttpRequestRawData
            tabId={this.props.tabId}
            showRawData={isRawData}
            rawData={this.props.rawData}
            updateParam={this.props.updateParam}
            isOutgoingRequest={this.props.isOutgoingRequest}
            readOnly={this.props.readOnly}
            ref={(item) =>
              !this.state.rawDataRef && this.setState({ rawDataRef: item })
            }
            paramName="rawData"
          ></HttpRequestRawData>
          <HttpRequestRawData
            tabId={this.props.tabId}
            showRawData={isGrpcData}
            rawData={this.props.grpcData}
            updateParam={this.props.updateParam}
            isOutgoingRequest={this.props.isOutgoingRequest}
            readOnly={this.props.readOnly}
            ref={(item) =>
              !this.state.grpcDataRef && this.setState({ grpcDataRef: item })
            }
            paramName="grpcData"
          ></HttpRequestRawData>
        </div>
      </>
    );
  }
}

export default HttpRequestBody;
