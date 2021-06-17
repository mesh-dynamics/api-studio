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
import React, { useEffect, useState } from "react";
import { Modal, Grid, Row, Col } from "react-bootstrap";

import _ from "lodash";
import Tippy from "@tippy.js/react";
import { IMetaData, IStoreState, IUserAuthDetails } from "../../reducers/state.types";
import { getCollectionDetailsById } from "../../utils/http_client/httpClientUtils";
import { DiffRequestIds } from "./components/DiffRequestIds";

interface IRequestMatchTypeProps {
  metaData: IMetaData;
  app: string;
  user: IUserAuthDetails;
  originalReqId: string;
  onClick: (matchedRequestId: string) => void;
}

function RequestMatchType(props: IRequestMatchTypeProps) {
  const [matchRequestShowPopup, setMatchRequestShowPopup] = useState(false);
  const [collectionName, setCollectionName] = useState("");

  const isCollectionMatched = props.metaData.collectionMatched == "true";
  const traceIdMatched = props.metaData.traceIdMatched == "true";
  const payloadKeyMatched = props.metaData.payloadKeyMatched == "true";

  let matchType = "NoMatch";
  if (isCollectionMatched && traceIdMatched && payloadKeyMatched) {
    matchType = "ExactMatch";
  } else if (isCollectionMatched || traceIdMatched || payloadKeyMatched) {
    matchType = "FuzzyMatch";
  }
  let titleText = "",
    fillColor = "red";
  switch (matchType) {
    case "ExactMatch":
      titleText = "Exact Match";
      fillColor = "green";
      break;
    case "FuzzyMatch":
      titleText = "Fuzzy Match (Click to see matched request details)";
      fillColor = "orange";
      break;
    case "NoMatch":
      titleText = "No Match";
      fillColor = "red";
      break;
  }

  const onClick = React.useCallback(
    (event: React.MouseEvent<Element>) => {
      setMatchRequestShowPopup(true);
    },
    [props.metaData.matchedRequestId]
  );

  useEffect(() => {
    const collectionId = props.metaData.matchedCollectionName;
    setCollectionName("");
    if (collectionId && matchRequestShowPopup) {
      getCollectionDetailsById(collectionId).then((collection) => {
        const collectionName = collection ? `${collection.name} (${collection.label})` : `Collection Id ${collectionId} not found`;
        setCollectionName(collectionName);
      });
    }
  }, [props.metaData.matchedCollectionName, props.originalReqId, matchRequestShowPopup]);

  const onHidePopup = React.useCallback(() => {
    setMatchRequestShowPopup(false);
  }, []);

  const getIcon = (value: boolean) => {
    return value ? (
      <i className="fa fa-check" style={{ color: "green", border: "1px #9ec59e solid" }}></i>
    ) : (
      <i className="fa fa-times" style={{ color: "red", border: "1px #ff9393 solid" }}></i>
    );
  };

  const {originalReqId, metaData: {matchedRequestId} } = props;

  return (
    <>
      <div onClick={onClick} className={"requestMatch " + matchType}>
        <Tippy content={titleText} arrow={true} placement="bottom">
          <svg width="25" height="13">
            <rect width="25" height="13" style={{ fill: fillColor, strokeWidth: 1, stroke: "gray" }} />
          </svg>
        </Tippy>
      </div>
      <Modal show={matchRequestShowPopup} onHide={onHidePopup} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Matched Request Details</Modal.Title>
        </Modal.Header>

        <Modal.Body className={"text-center padding-15"}>
          <Grid className="margin-left-15 text-left">
            <Row>
              <Col md={6}>
                <label>Matched with current collection</label>
              </Col>
              <Col md={6}>{getIcon(isCollectionMatched)}</Col>
            </Row>
            {collectionName && (
              <Row>
                <Col md={6} style={{ paddingLeft: "42px" }}>
                  <span className="text-muted">- Matched collection name</span>
                </Col>
                <Col md={6}>{collectionName}</Col>
              </Row>
            )}

            <Row>
              <Col md={6}>
                <label>Matched with trace id</label>
              </Col>
              <Col md={6}>{getIcon(traceIdMatched)}</Col>
            </Row>

            <Row>
              <Col md={6}>
                <label>Request matched exactly</label>
              </Col>
              <Col md={6}>{getIcon(payloadKeyMatched)}</Col>
            </Row>
          </Grid>
          {originalReqId && matchedRequestId && matchRequestShowPopup && !payloadKeyMatched && (
            <div className="requestMatchTypeBody">
              <DiffRequestIds lhsReqId={originalReqId} rhsReqId={matchedRequestId} />
            </div>
          )}
        </Modal.Body>

        <Modal.Footer>
          <span onClick={onHidePopup} className="cube-btn">
            CLOSE
          </span>
        </Modal.Footer>
      </Modal>
    </>
  );
}

const mapStateToProps = (state: IStoreState) =>
  ({
    app: state.cube.selectedApp,
    user: state.authentication.user,
  } as Partial<IRequestMatchTypeProps>);

const connectedRequestMatchType = connect(mapStateToProps)(RequestMatchType);

export default connectedRequestMatchType;
