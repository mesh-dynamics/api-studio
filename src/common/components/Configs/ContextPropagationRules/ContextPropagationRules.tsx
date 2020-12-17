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
import classNames from "classnames";
import { connect } from "react-redux";
import {
  ICollectionDetails,
  IGoldenState_SelectedGolden,
  IStoreState,
  IUserAuthDetails,
} from "../../../reducers/state.types";
import {
  configsService,
  IUpoadRuleArgs,
} from "../../../services/configs.service";
import "./ContextPropagationRules.css";
import { fetchGoldenMeta } from "../../../services/golden.service";

export interface IContextPropagationRulesProps {
  collectionList: ICollectionDetails[];
  goldenList: ICollectionDetails[];
  dispatch: any;
  app: string;
  customerId: string;
}

function ContextPropagationRules(props: IContextPropagationRulesProps) {
  const [collectionId, setCollectionId] = useState("");
  const [recordingId, setRecordingId] = useState("");
  const [services, setServices] = useState<string[]>([]);
  const [selectedServices, setSelectedServices] = useState<string>("");
  const [fileList, setFileList] = useState<FileList | null>(null);
  const [message, setMessage] = useState({ message: "", isError: false });

  const collectionList = React.useMemo(() => {
    return [...props.goldenList, ...props.collectionList];
  }, [props.goldenList, props.collectionList]);

  useEffect(() => {
    if (recordingId) {
      fetchGoldenMeta(recordingId)
        .then((metaData: any) => {
          const serviceList = (metaData as IGoldenState_SelectedGolden).serviceFacets.map(
            (service) => service.val
          );
          setServices(serviceList);
        })
        .catch((error) => {
          console.error(error);
          setServices([]);
        });
    } else {
      setServices([]);
      setSelectedServices("");
    }
  }, [recordingId]);

  const resetMessage = () => setMessage({ message: "", isError: false });

  const onSelectedCollectionChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLSelectElement>) => {
      const selectedValue = event.target.value;
      resetMessage();
      setCollectionId(selectedValue.split(";")[0]);
      setRecordingId(selectedValue.split(";")[1]);
      setSelectedServices("");
      setServices([]);
    },
    []
  );

  const onSelectedServiceChange = React.useCallback(
    (event: React.ChangeEvent<FormControl & HTMLSelectElement>) => {
      const selectedValue = event.target.value;
      setSelectedServices(selectedValue);
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
        customerId: props.customerId,
        app: props.app,
        version: `Default${props.app}`,
        apiConfig,
        formData,
      };
      switch (type) {
        case "text/csv":
          //It is Learnt rules upload

          configsService
            .saveDynamicInjectionConfigFromCsv(uploadArgs)
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

          break;
        case "application/json":
          //It is Existing rules upload
          configsService
            .saveDynamicInjectionConfigFromJson(uploadArgs)
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
          break;
        default:
          setMessage({
            message:
              "Only CSV and JSON files are supported. Please select required file type.",
            isError: true,
          });
          break;
      }
    } else {
      setMessage({ message: "File is not selected", isError: true });
    }
  }, [fileList]);
  const downloadExistingRules = React.useCallback(() => {
    resetMessage();
    configsService
      .getDynamicInjectionConfig({
        app: props.app,
        customerId: props.customerId,
        eventTypes: [],
        collections: [],
        version: `Default${props.app}`,
      })
      .then((response: any) => {
        if (response.isFileDownloaded) {
          setCollectionId("");
          setRecordingId("");
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
  }, []);
  const downloadNewRules = React.useCallback(() => {
    if (!collectionId) {
      setMessage({ message: "Collection is not selected", isError: true });
      return;
    }
    if (!selectedServices) {
      setMessage({ message: "Service is not selected", isError: true });
      return;
    }
    resetMessage();
    configsService
      .getPotentialDynamicInjectionConfigs({
        app: props.app,
        customerId: props.customerId,
        eventTypes: [],
        collections: [collectionId],
        version: `Default${props.app}`,
        indexOrderAsc: "true",
        services: [selectedServices],
      })
      .then((response: any) => {
        if (response.isFileDownloaded) {
          setCollectionId("");
          setRecordingId("");
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
  }, [collectionId, selectedServices]);
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
              >
                <option>Select a collection</option>
                {collectionList.map((collection, index) => (
                  <option
                    key={index}
                    value={`${collection.collec};${collection.id}`}
                    selected={collection.collec == collectionId}
                  >
                    {collection.name}
                  </option>
                ))}
              </FormControl>
            </FormGroup>
          </Col>
          <Col xs={12} md={3}>
            <FormGroup controlId="formControlsSelectServices">
              <FormControl
                componentClass="select"
                placeholder="select"
                onChange={onSelectedServiceChange}
              >
                <option>Select a Service</option>
                {services.map((service, index) => (
                  <option
                    key={index}
                    value={service}
                    selected={service == selectedServices}
                  >
                    {service}
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
  collectionList: state.apiCatalog.collectionList,
  app: state.cube.selectedApp,
  customerId: (state.authentication.user as IUserAuthDetails).customer_name,
});

export default connect(mapStateToProps)(ContextPropagationRules);
