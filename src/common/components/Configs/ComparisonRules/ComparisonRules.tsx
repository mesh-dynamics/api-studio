import { AxiosRequestConfig } from "axios";
import React, { useEffect, useState } from "react";
import {
  Grid,
  Row,
  Col,
  Button,
  FormGroup,
  FormControl,
} from "react-bootstrap";
import commonUtils from "../../../utils/commonUtils";
import classNames from "classnames";
import { connect } from "react-redux";
import moment from "moment";
import {
  ICollectionDetails,
  IStoreState,
  ITimelineData,
  IUserAuthDetails,
} from "../../../reducers/state.types";
import {
  configsService,
  IUpoadRuleArgs,
} from "../../../services/configs.service";
import { cubeService } from "../../../services/cube.service";
import { ITimelineResponse } from "src/src/common/apiResponse.types";

export interface IComparisonRulesProps {
  goldenList: ICollectionDetails[];
  dispatch: any;
  app: string;
  user: IUserAuthDetails;
}

function ComparisonRules(props: IComparisonRulesProps) {
  const [collectionId, setCollectionId] = useState("");
  const [timelineData, setTimelineData] = useState<ITimelineData[]>([]);
  const [selectedTestId, setSelectedTestId] = useState<string>("");
  const [fileList, setFileList] = useState<FileList | null>(null);
  const [message, setMessage] = useState({ message: "", isError: false });

  useEffect(() => {
    if (collectionId) {
      cubeService
        .fetchTimelineData(
          props.user,
          props.app,
          "ALL",
          new Date(),
          null,
          30,
          "",
          "",
          collectionId
        )
        .then((response: unknown) => {
          const timeLineData = response as ITimelineResponse; //This could be 700+ and we get default 20 results.
          setTimelineData(timeLineData.timelineResults);
        })
        .catch((error) => {
          console.error(error);
          setMessage({
            message: "Error while fetching timeline data",
            isError: true,
          });
          setTimelineData([]);
        });
    } else {
      setTimelineData([]);
    }
  }, [collectionId]);

  const resetMessage = () => setMessage({ message: "", isError: false });

  const onSelectedCollectionChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLSelectElement>) => {
      const selectedValue = event.target.value;
      resetMessage();
      setCollectionId(selectedValue);
      setSelectedTestId("");
      setTimelineData([]);
    },
    []
  );

  const onSelectedTimeLineChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLSelectElement>) => {
      const selectedValue = event.target.value;
      setSelectedTestId(selectedValue);
      resetMessage();
    },
    []
  );

  const onFileChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLInputElement>) => {
      const selectedFiles = event.target.files;
      setFileList(selectedFiles);
      resetMessage();
    },
    []
  );
  const onUpload = React.useCallback(() => {
    resetMessage();
    if (fileList && fileList.length > 0) {
      const file = fileList[0];
      const type = file.type;
      const apiConfig: AxiosRequestConfig = {
        headers: {
          "Content-type": type,
        },
      };
      const formData = new FormData();
      formData.append("file", file, file.name);
      const uploadArgs: IUpoadRuleArgs = {
        customerId: props.user.customer_name,
        app: props.app,
        version: `Default${props.app}`,
        apiConfig,
        formData,
      };
      if (commonUtils.isCSVMimeType(type)) {
        //It is Learnt rules upload
          configsService
            .saveComparisonRulesConfigFromCsv(uploadArgs)
            .then((response: any) => {
              setMessage({
                message: response.Message || "Config file has been uploaded",
                isError: false,
              });
            })
            .catch((error) => {
              console.error(error);
              const message = error.response?.data?.data?.message;
              setMessage({
                message: message || "Could not save file. Some error occurred",
                isError: true,
              });
            });
      } else if (type == "application/json") {
        //It is Existing rules upload
          configsService
            .saveComparisonRulesConfigFromJson(uploadArgs)
            .then((response: any) => {
              setMessage({
                message: response.Message || "Config file has been uploaded",
                isError: false,
              });
            })
            .catch((error) => {
              console.error(error);
              const message = error.response?.data?.data?.message;
              setMessage({
                message: message || "Could not save file. Some error occurred",
                isError: true,
              });
            });
      } else {
        setMessage({
          message:
            "Only CSV and JSON files are supported. Please select required file type.",
          isError: true,
        });
      }
    } else {
      setMessage({ message: "File is not selected", isError: true });
    }
  }, [fileList]);
  const downloadExistingRules = React.useCallback(() => {
    resetMessage();
    configsService
      .getTemplateSet(
        props.user.customer_name,
        props.app,
        `Default${props.app}`
      )
      .then((response: any) => {
        if (!response.isFileDownloaded) {
          setMessage({
            message: "File could not be downloaded",
            isError: true,
          });
        }
      })
      .catch((error: any) => {
        console.error(error);
        const message = error.response?.data?.data?.message;
        setMessage({
          message: message || "File could not be downloaded",
          isError: true,
        });
      });
  }, []);
  const downloadNewRules = React.useCallback(() => {
    if (!selectedTestId) {
      setMessage({ message: "Timeline is not selected", isError: true });
      return;
    }
    resetMessage();
    configsService
      .getComparisonRulesConfig(selectedTestId)
      .then((response: any) => {
        if (response.isFileDownloaded) {
          setSelectedTestId("");
        } else {
          setMessage({
            message: "File could not be downloaded",
            isError: true,
          });
        }
      })
      .catch((error: any) => {
        console.error(error);

        const message = error.response?.data?.data?.message;
        setMessage({
          message: message || "File could not be downloaded",
          isError: true,
        });
      });
  }, [selectedTestId]);
  const messageClass = classNames({
    errorMessage: message.isError,
    successMessage: !message.isError,
  });
  return (
    <div className="prop-rules">
      {message.message && <div className={messageClass}>{message.message}</div>}
      <Grid>
        <Row className="download-config-grid">
          <Col xs={12} md={3}>
            Existing Rules
          </Col>
          <Col xs={12} md={9}>
            <Button onClick={downloadExistingRules}>Download</Button> (In *.json
            format)
          </Col>
        </Row>
        <Row className="download-config-grid">
          <Col xs={12} md={3}>
            New Rules
          </Col>

          <Col xs={12} md={3}>
            <FormGroup controlId="formControlsSelectCollection">
              <FormControl
                componentClass="select"
                placeholder="select"
                onChange={onSelectedCollectionChange} 
                value={collectionId}
              >
                <option>Select a collection</option>
                {props.goldenList.map((collection, index) => (
                  <option
                    key={index}
                    value={collection.collec}
                  >
                    {collection.name} ({collection.label})
                  </option>
                ))}
              </FormControl>
            </FormGroup>
          </Col>
          <Col xs={12} md={3}>
            <FormGroup controlId="formControlsSelectTimeline">
              <FormControl
                componentClass="select"
                placeholder="select"
                onChange={onSelectedTimeLineChange}
              >
                <option>Select a Test</option>
                {timelineData.map((timeline, index) => (
                  <option
                    key={index}
                    value={timeline.replayId}
                    selected={timeline.replayId == selectedTestId}
                  >
                    {moment(timeline.timestamp).format("lll")}
                  </option>
                ))}
              </FormControl>
            </FormGroup>
          </Col>

          <Col xs={12} md={3}>
            <Button onClick={downloadNewRules}>Learn</Button> (In *.csv format)
          </Col>
        </Row>
        <Row className="download-config-grid">
          <Col xs={12} md={3}>
            Upload Rules
          </Col>
          <Col xs={12} md={6}>
            <FormGroup controlId={"fileUploadSelect"}>
              <FormControl
                type="file"
                onChange={onFileChange}
                accept=".csv,.json"
              />
            </FormGroup>
          </Col>
          <Col xs={12} md={3}>
            <Button onClick={onUpload}>Upload</Button> (JSON and CSV accepted)
          </Col>
        </Row>
      </Grid>
    </div>
  );
}

const mapStateToProps = (state: IStoreState) => ({
  goldenList: state.apiCatalog.goldenList,
  app: state.cube.selectedApp,
  user: state.authentication.user,
});

export default connect(mapStateToProps)(ComparisonRules);