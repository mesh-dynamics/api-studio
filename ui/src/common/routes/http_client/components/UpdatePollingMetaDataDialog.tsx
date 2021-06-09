import React, { ChangeEvent, useState } from "react";
import { Modal, Grid, Row, Col, FormGroup, FormControl } from "react-bootstrap";
import { CubeButton } from "../../../../common/components/common/CubeButton";
import { IKeyValuePairs } from "../../../../common/reducers/state.types";

export interface IUpdatePollingMetaDataDialogProps {
  showDialog: boolean;
  onHide: () => void;
  metaData: IKeyValuePairs;
  onMetaDataChanged: (pollingMetaData: IKeyValuePairs) => void;
}

export function UpdatePollingMetaDataDialog(props: IUpdatePollingMetaDataDialogProps) {
  
  const [isPollRequest, setIsPollRequest] = useState(props.metaData.isPollRequest == "true");
  const [pollMaxRetries, setPollMaxRetries] = useState(parseInt(props.metaData.pollMaxRetries) || 0);
  const [pollRetryIntervalSec, setPollRetryIntervalSec] = useState(parseInt(props.metaData.pollRetryIntervalSec) || 1);
  const [pollRespJsonPath, setPollRespJsonPath] = useState(props.metaData.pollRespJsonPath || "");
  const [pollRespComparator, setPollRespComparator] = useState(props.metaData.pollRespComparator || "equals");
  const [pollRespCompValue, setPollRespCompValue] = useState(props.metaData.pollRespCompValue || "");

  const onSaveClicked = () => {
    let metaData = {
      isPollRequest: isPollRequest.toString(),
      pollMaxRetries: pollMaxRetries.toString(),
      pollRetryIntervalSec: pollRetryIntervalSec.toString(),
      pollRespJsonPath,
      pollRespComparator,
      pollRespCompValue,
    };
    props.onMetaDataChanged(metaData);
  }

  const onIsPollRequestChange = (event: ChangeEvent<HTMLInputElement>) => setIsPollRequest(event.target.checked);
  const onPollMaxRetriesChanged = (event: ChangeEvent<HTMLInputElement & FormControl>) => setPollMaxRetries(parseInt(event.target.value));
  const onPollRetryIntervalSecChanged = (event: ChangeEvent<HTMLInputElement & FormControl>) => setPollRetryIntervalSec(parseInt(event.target.value));
  const onPollRespJsonPathChanged = (event: ChangeEvent<HTMLInputElement & FormControl>) => setPollRespJsonPath(event.target.value);
  const onPollRespComparatorChanged = (event: ChangeEvent<HTMLSelectElement & FormControl>) => setPollRespComparator(event.target.value);
  const onPollRespCompValueChanged = (event: ChangeEvent<HTMLInputElement & FormControl>) => setPollRespCompValue(event.target.value);

  return (
    <Modal show={props.showDialog} onHide={props.onHide} bsSize="medium">
      <Modal.Header>
        <Modal.Title>Update Polling Data</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div style={{ margin: "5px" }}>
          <FormGroup>
            <Grid className="margin-left-15 text-left">
              <Row style={{ marginBottom: "5px" }}>
                <Col md={5}>
                  <label>Set as Poll request</label>
                </Col>
                <Col md={7}>
                  <input
                    type="checkbox"
                    className="pull-left"
                    checked={isPollRequest}
                    onChange={onIsPollRequestChange}
                  />
                </Col>
              </Row>

              <Row style={{ marginBottom: "5px" }}>
                <Col md={5}>
                  <label>Max tries</label>
                </Col>
                <Col md={7}>
                  <FormControl
                    type="number"
                    readOnly={!isPollRequest}
                    value={pollMaxRetries}
                    onChange={onPollMaxRetriesChanged}
                  />
                </Col>
              </Row>

              <Row style={{ marginBottom: "5px" }}>
                <Col md={5}>
                  <label>Retry interval (in seconds)</label>
                </Col>
                <Col md={7}>
                  <FormControl
                    type="number"
                    readOnly={!isPollRequest}
                    value={pollRetryIntervalSec}
                    onChange={onPollRetryIntervalSecChanged}
                  />
                </Col>
              </Row>

              <Row style={{ marginBottom: "5px" }}>
                <Col md={5}>
                  <label>Response JSON path</label>
                </Col>
                <Col md={7}>
                  <FormControl
                    type="text"
                    value={pollRespJsonPath}
                    readOnly={!isPollRequest}
                    onChange={onPollRespJsonPathChanged}
                  />
                </Col>
              </Row>

              <Row style={{ marginBottom: "5px" }}>
                <Col md={5}>
                  <label>Comparator</label>
                </Col>
                <Col md={7}>
                  <select
                    className="form-control"
                    readOnly={!isPollRequest}
                    value={pollRespComparator}
                    onChange={onPollRespComparatorChanged}
                  >
                    <option value="equals">Equals (=)</option>
                    <option value="lt">Less than (&lt;)</option>
                    <option value="gt">Greater than (&gt;)</option>
                  </select>
                </Col>
              </Row>

              <Row style={{ marginBottom: "5px" }}>
                <Col md={5}>
                  <label>Comparison value</label>
                </Col>
                <Col md={7}>
                  <FormControl
                    type="text"
                    readOnly={!isPollRequest}
                    value={pollRespCompValue}
                    onChange={onPollRespCompValueChanged}
                  />
                </Col>
              </Row>
            </Grid>
          </FormGroup>
        </div>
      </Modal.Body>
      <Modal.Footer>
        <CubeButton onClick={props.onHide} label="CLOSE" />
        <CubeButton onClick={onSaveClicked} label="SAVE" />
      </Modal.Footer>
    </Modal>
  );
}
