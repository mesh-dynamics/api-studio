
import React, { useState } from "react";
import {
    Modal,
    FormGroup,
    FormControl
} from "react-bootstrap";
import _ from "lodash";
import config from "../../config";

import { cubeService } from "../../services";
import api from "../../api";

import { IGetEventsApiResponse } from "../../apiResponse.types";
import commonUtils from "../../utils/commonUtils";


interface IExportImportCollectionModalProps {
    showExportImportDialog: boolean,
    hideExportImportDialog: () => void,
    isExportOnly: boolean;
    exportImportRecordingId: string,
    exportImportCollectionId: string,
    app: string,
    customerId: string;
    refreshCollection?: (collectionId: string) => void;
}


export default function ExportImportCollectionModal(props: IExportImportCollectionModalProps) {
    const [isError, setIsError] = useState<boolean>(false);
    const [modalImportMessage, setModalImportMessage] = useState<string>("");
    const [importCollectionFile, setImportCollectionFile] = useState<FileList | null>(null);

    const exportCollection = async () => {
        const apiEventURL = `${config.recordBaseUrl}/getEvents`;
        const limit = 1000;
        let offset = 0;
        let nextPageExists = false;
        const jsonData: any[] = [];
        try {
            do {

                let body = {
                    customerId: props.customerId,
                    app: props.app,
                    eventTypes: [],
                    reqIds: [],
                    offset, limit,
                    collection: props.exportImportCollectionId,
                };

                setModalImportMessage("Loading...");
                setIsError(false);
                const response: unknown = await api.post(apiEventURL, body);
                const result = response as IGetEventsApiResponse;
                if (result && result.numResults > 0) {
                    const httpRequests = result.objects.filter(u => u.eventType == "HTTPRequest").map(u => { return u; });
                    const httpResponses = result.objects.filter(u => u.eventType == "HTTPResponse").map(u => { return u; });
                    httpRequests.forEach(request => {
                        const response = httpResponses.filter(u => u.reqId == request.reqId);
                        if (response.length > 0) {
                            jsonData.push({ request, response: response[0] });
                        }
                    })
                }
                setModalImportMessage("File will be downloaded shortly");
                setIsError(false);
                if (result.numFound > limit + offset) {
                    offset = limit + offset;
                    nextPageExists = true;
                } else {
                    nextPageExists = false;
                }

            }
            while (nextPageExists);
        }
        catch (error: any) {
            setModalImportMessage("Some error occurred while fetching data");
            setIsError(true);
            console.error(error);
        }
        commonUtils.downloadAFileToClient(`Collection_${props.exportImportCollectionId}.json`, JSON.stringify(jsonData));
    };

    const importCollection = async () => {
        const fileList = importCollectionFile;
        const recordingId = props.exportImportRecordingId;
        const collectionId = props.exportImportCollectionId;


        setModalImportMessage("In Progress");
        setIsError(false);
        if (fileList && fileList.length > 0) {
            const file = fileList[0];
            var reader = new FileReader();

            // This event listener will happen when the reader has read the file
            reader.addEventListener('load', function () {
                var result: any[] = JSON.parse(reader.result as string);
                result.forEach((event) => {
                    event.request.collection = collectionId;
                    event.response.collection = collectionId;
                    event.request.customerId = props.customerId;
                    event.response.customerId = props.customerId;
                    event.request.app = props.app;
                    event.response.app = props.app;
                });

                cubeService.storeUserReqResponse(recordingId, result).then(
                    (serverRes) => {
                        props.refreshCollection!(collectionId);
                        setModalImportMessage("Collection imported successfully");
                        setIsError(false);
                    },
                    (error) => {
                        setModalImportMessage("Collection imported failed");
                        setIsError(true);
                        console.error("error: ", error);
                    }
                );

            });

            reader.readAsText(file);
        } else {
            setModalImportMessage("Please select a valid file");
            setIsError(true);
        }
    };

    const onFileChange = (event: React.ChangeEvent<FormControl & HTMLInputElement>) => {
        const selectedFiles = event.target.files;
        setImportCollectionFile(selectedFiles);
    }

    const hideDialog = () => {
        setImportCollectionFile(null);
        setModalImportMessage("");
        setIsError(false);
        props.hideExportImportDialog();
    }

    return <Modal show={props.showExportImportDialog} onHide={hideDialog}>
        <Modal.Header closeButton>
            <Modal.Title>Import/Export Collection</Modal.Title>
        </Modal.Header>
        <Modal.Body>
            <div style={{ display: "block", justifyContent: "center" }}>
                <div style={{ textAlign: 'center' }}>
                    <span className={isError ? "red" : "green"}>{modalImportMessage}</span>
                </div>

                <div style={{ display: "flex", flex: 1, alignItems: "flex-start", justifyContent: "space-between", marginBottom: '10px' }}>
                    <span>Click to export events </span>
                    <span
                        className="cube-btn"
                        onClick={exportCollection}
                    >
                        Export
          </span>
                </div>
                {!props.isExportOnly && <>
                    <hr style={{ opacity: '0.5' }} />
                    <div style={{ display: "flex", flex: 1, alignItems: "flex-start", justifyContent: "space-between" }}>
                        <FormGroup controlId={"fileUploadSelect"}>
                            <FormControl
                                type="file"
                                onChange={onFileChange}
                                accept=".json"
                            />
                        </FormGroup>
                        <span
                            className="cube-btn"
                            onClick={importCollection}
                        >
                            Import
          </span>
                    </div>
                </>}
            </div>
        </Modal.Body>
        <Modal.Footer>
            <span onClick={hideDialog} className="cube-btn">Close</span>
        </Modal.Footer>
    </Modal>
}