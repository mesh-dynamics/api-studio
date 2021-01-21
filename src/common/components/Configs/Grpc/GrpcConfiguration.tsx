import React, { useState } from "react";
import {
  Grid,
  Row,
  Col,
  Button,
  FormGroup,
  Form,
  FormControl,
} from "react-bootstrap";
import classNames from "classnames";
import { connect } from "react-redux";
import { IStoreState, IUserAuthDetails } from "../../../reducers/state.types";
import {
  configsService,
  IProtoDescriptorFileUploadArgs,
} from "../../../services/configs.service";
import "./GrpcConfiguration.css";

export interface IGrpcConfigurationProps {
  dispatch: any;
  app: string;
  customerId: string;
}

function GrpcConfiguration(props: IGrpcConfigurationProps) {
  const [showImport, setShowImport] = useState(true);
  const [appendExisting, setAppendExisting] = useState(true);
  const [fileList, setFileList] = useState<File[]>([]);
  const [message, setMessage] = useState({ message: "", isError: false });
  
  const showHideImport = () => setShowImport(!showImport);
  const resetMessage = () => setMessage({ message: "", isError: false });

  const onAppendExistingChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLSelectElement>) => {
      setAppendExisting(event.target.value == "true");
    },
    []
  );

  const onFileChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLInputElement>) => {
      const selectedFiles = event.target.files;
      if (selectedFiles) {
        const files: File[] = [];
        for (var i = 0; i < selectedFiles.length; i++) {
          files.push(selectedFiles[i]);
        }
        setFileList([...fileList, ...files]);
      }
      resetMessage();
    },
    [fileList]
  );
  const onSelectedFileRemove = React.useCallback(
    (event: React.MouseEvent<HTMLElement>) => {
      const index = parseInt(
        (event.target as HTMLElement).getAttribute("data-removeindex")!
      );
      if (fileList.length > index) {
        setFileList([
          ...fileList.slice(0, index),
          ...fileList.slice(index + 1),
        ]);
      }
      resetMessage();
    },
    [fileList]
  );
  const onUploadClick = React.useCallback(() => {
    resetMessage();
    //call API, clean selected files
    const formData = new FormData();
    fileList.forEach((file) =>
      formData.append("protoDescriptorFile", file, file.name)
    );
    const uploadArgs: IProtoDescriptorFileUploadArgs = {
      customerId: props.customerId,
      app: props.app,
      formData,
      appendExisting : appendExisting.toString()
    };

    configsService
      .protoDescriptorFileUpload(uploadArgs)
      .then((response: any) => {
        setMessage({
          message:
            response.Message ||
            `Proto ${
              fileList.length == 1 ? "file has" : "files have"
            } been uploaded`,
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
  }, [fileList, appendExisting]);
  const messageClass = classNames({
    errorMessage: message.isError,
    successMessage: !message.isError,
  });
  return (
    <div className="grpc-config-section">
      {message.message && <div className={messageClass}>{message.message}</div>}
      <div
        className={"collapsible-header" + (showImport ? " selected" : "")}
        onClick={showHideImport}
      >
        <span>
          {showImport ? (
            <i className="fa fa-chevron-circle-up" />
          ) : (
            <i className="fa fa-chevron-circle-down" />
          )}
        </span>
        &nbsp;Import
      </div>
      <div
        style={{ display: showImport ? "block" : "none" }}
        className="grpc-import-section"
      >
        <Grid>
          <Row>
            <Col xs={12} md={2}>
              Proto:
            </Col>
            <Col xs={12} md={10}>
              <Form inline>
                <FormGroup controlId={"fileUploadSelect"} bsSize="sm">
                  <FormControl
                    componentClass="select"
                    style={{marginTop:"-4px", marginRight:"10px"}}
                    value={appendExisting.toString()}
                    placeholder="Upload type"
                    onChange={onAppendExistingChange}
                  >
                    <option value="true">Append to Existing</option>
                    <option value="false">Override Existing</option>
                  </FormControl>
                  <label>
                    <span className="btn btn-sm cube-btn margin-right-10">
                      <i className="fas fa-plus pointer" /> Add files
                    </span>
                    <input
                      type="file"
                      name="myfile"
                      accept=".proto"
                      multiple
                      onChange={onFileChange}
                      style={{ display: "none" }}
                    />
                  </label>

                  <Button
                    className="btn btn-sm cube-btn"
                    disabled={fileList.length == 0}
                    onClick={onUploadClick}
                  >
                    <i className="fas fa-upload pointer" /> Upload files
                  </Button>
                </FormGroup>
              </Form>
            </Col>
          </Row>

          <Row>
            <Col xs={12} md={2}></Col>
            <Col xs={12} md={10}>
              {fileList.map((file, index) => (
                <div className="file" key={index}>
                  <span>{file.name}</span>
                  <i
                    className="fa fa-times pointer"
                    data-removeindex={index}
                    title="Remove file from selection"
                    onClick={onSelectedFileRemove}
                  ></i>
                </div>
              ))}
            </Col>
          </Row>
        </Grid>
      </div>
    </div>
  );
}

const mapStateToProps = (state: IStoreState) => ({
  app: state.cube.selectedApp,
  customerId: (state.authentication.user as IUserAuthDetails).customer_name,
});

export default connect(mapStateToProps)(GrpcConfiguration);
