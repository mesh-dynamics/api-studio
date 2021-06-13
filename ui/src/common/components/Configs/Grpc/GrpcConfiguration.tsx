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

import React, { useState } from "react";
import { Grid, Row, Col, Button, FormGroup, Form, FormControl } from "react-bootstrap";
import classNames from "classnames";
import { connect } from "react-redux";
import { IStoreState, IUserAuthDetails } from "../../../reducers/state.types";
import { configsService, IProtoDescriptorFileUploadArgs } from "../../../services/configs.service";
import "./GrpcConfiguration.css";
import Tippy from "@tippy.js/react";

export interface IGrpcConfigurationProps {
  dispatch: any;
  app: string;
  customerId: string;
}

function GrpcConfiguration(props: IGrpcConfigurationProps) {
  const [showImport, setShowImport] = useState(true);
  const [appendExisting, setAppendExisting] = useState(true);
  const [uploadFileType, setUploadFileType] = useState("proto");
  const [fileList, setFileList] = useState<File[]>([]);
  const [message, setMessage] = useState({ message: "", isError: false });

  const showHideImport = () => setShowImport(!showImport);
  const resetMessage = () => setMessage({ message: "", isError: false });

  const onAppendExistingChange = React.useCallback((event: React.ChangeEvent<FormControl & HTMLSelectElement>) => {
    setAppendExisting(event.target.value == "true");
  }, []);

  const onUploadFileTypeChange = React.useCallback((event: React.ChangeEvent<FormControl & HTMLSelectElement>) => {
    setFileList([]);
    setUploadFileType(event.target.value);
  }, []);

  const isDescriptor = uploadFileType == "desc";
  const isProto = !isDescriptor;

  const onFileChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLInputElement>) => {
      const selectedFiles = event.target.files;
      if (selectedFiles) {
        const files: File[] = [];
        for (var i = 0; i < selectedFiles.length; i++) {
          files.push(selectedFiles[i]);
        }
        if(isDescriptor){
          setFileList(files);
        }else{
          setFileList([...fileList, ...files]);
        }
      } else if (isDescriptor) {
        setFileList([]);
      }
      resetMessage();
    },
    [fileList, isDescriptor]
  );
  const onSelectedFileRemove = React.useCallback(
    (event: React.MouseEvent<HTMLElement>) => {
      const index = parseInt((event.target as HTMLElement).getAttribute("data-removeindex")!);
      if (fileList.length > index) {
        setFileList([...fileList.slice(0, index), ...fileList.slice(index + 1)]);
      }
      resetMessage();
    },
    [fileList]
  );
  const onUploadClick = React.useCallback(() => {
    resetMessage();
    //call API, clean selected files
    const formData = new FormData();
    fileList.forEach((file) => formData.append("protoDescriptorFile", file, file.name));
    const uploadArgs: IProtoDescriptorFileUploadArgs = {
      customerId: props.customerId,
      app: props.app,
      formData,
      appendExisting: appendExisting.toString(),
    };
    if (isDescriptor) {
      configsService
        .protoDescriptorCompiledFileUpload(uploadArgs)
        .then((response: any) => {
          setMessage({
            message: response.Message || `Descriptor file has been uploaded`,
            isError: false,
          });
          setFileList([]);
        })
        .catch((error) => {
          console.error(error);
          const message = error.response?.data?.Message;
          setMessage({
            message: message || "Upload failed. Some error occurred",
            isError: true,
          });
        });
    } else {
      configsService
        .protoDescriptorFileUpload(uploadArgs)
        .then((response: any) => {
          setMessage({
            message: response.Message || `Proto ${fileList.length == 1 ? "file has" : "files have"} been uploaded`,
            isError: false,
          });
          setFileList([]);
        })
        .catch((error) => {
          console.error(error);
          const message = error.response?.data?.Message;
          setMessage({
            message: message || "Upload failed. Some error occurred",
            isError: true,
          });
        });
    }
  }, [fileList, appendExisting, isDescriptor]);
  const messageClass = classNames({
    errorMessage: message.isError,
    successMessage: !message.isError,
  });

  const isUploadbuttonDisabled = fileList.length == 0;
  const content = isProto ? "Any previously uploaded descriptor file will be overwritten" : "Any previously uploaded proto file will be overwritten";
  return (
    <div className="grpc-config-section">
      {message.message && <div className={messageClass}>{message.message}</div>}
      <div className={"collapsible-header" + (showImport ? " selected" : "")} onClick={showHideImport}>
        <span>{showImport ? <i className="fa fa-chevron-circle-up" /> : <i className="fa fa-chevron-circle-down" />}</span>
        &nbsp;Import
      </div>
      <div style={{ display: showImport ? "block" : "none" }} className="grpc-import-section">
        <Form inline>
          <Grid>
            <Row>
              <Col xs={12} md={12}>
                <FormGroup controlId={"uploadTypeSelect"} bsSize="sm" style={{marginRight: "10px",  marginTop: "-4px"}}>
                  Upload{" "}
                  <FormControl
                    componentClass="select"
                    style={{ display: "inline", width: "calc(100% - 50px)", minWidth: "115px", marginTop: "-4px" }}
                    value={uploadFileType.toString()}
                    placeholder="Upload type"
                    onChange={onUploadFileTypeChange}
                  >
                    <option disabled>Select type to upload</option>
                    <option value="proto">Proto</option>
                    <option value="desc">Descriptor</option>
                  </FormControl>
                </FormGroup>
                <FormGroup controlId={"fileUploadSelect"} bsSize="sm" style={{ marginRight: "11px" }}>
                  {isProto && (
                    <>
                      <FormControl
                        componentClass="select"
                        style={{ marginTop: "-4px", marginRight: "15px" }}
                        value={appendExisting.toString()}
                        placeholder="Upload type"
                        onChange={onAppendExistingChange}
                      >
                        <option value="true">Append to Existing</option>
                        <option value="false">Override Existing</option>
                      </FormControl>
                      <label>
                        <span className="btn btn-sm cube-btn">
                          <i className="fas fa-plus pointer" /> Add files
                        </span>
                        <input type="file" name="myfile" accept=".proto" multiple onChange={onFileChange} style={{ display: "none" }} />
                      </label>
                    </>
                  )}
                  {isDescriptor && (
                    <>
                      <input type="file" name="myfile" accept=".desc" onChange={onFileChange} style={{ display: "inline" }} />
                    </>
                  )}
                  </FormGroup>
                 <FormGroup controlId={"fileUploadBtn"} bsSize="sm" style={{ marginTop: "-6px"}}>
                  <Button className="btn btn-sm cube-btn" disabled={isUploadbuttonDisabled} onClick={onUploadClick}>
                    <i className="fas fa-upload pointer" /> Upload files
                  </Button>
                  <Tippy content={content} arrow={false} arrowType="round" interactive={true} theme={"light"} size="large" placement="right">
                    <i className="fa fa-info-circle margin-top-10 orange" style={{ display: isUploadbuttonDisabled ? "none" : "inline" }}></i>
                  </Tippy>
                  </FormGroup>
              </Col>
            </Row>

            {isProto && (
              <Row>
                <Col xs={12} md={3}></Col>
                <Col xs={12} md={9}>
                  {fileList.map((file, index) => (
                    <div className="file" key={index}>
                      <span>{file.name}</span>
                      <i className="fa fa-times pointer" data-removeindex={index} title="Remove file from selection" onClick={onSelectedFileRemove}></i>
                    </div>
                  ))}
                </Col>
              </Row>
            )}
          </Grid>
        </Form>
      </div>
    </div>
  );
}

const mapStateToProps = (state: IStoreState) => ({
  app: state.cube.selectedApp,
  customerId: (state.authentication.user as IUserAuthDetails).customer_name,
});

export default connect(mapStateToProps)(GrpcConfiguration);
