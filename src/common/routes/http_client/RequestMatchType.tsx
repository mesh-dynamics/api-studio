import { connect } from "react-redux";
import React, { useState } from "react";
import { cubeService } from "../../services";
import { Modal } from "react-bootstrap";
import HttpRequestMessage from "./HttpRequestMessage";

import _ from "lodash";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import { formatHttpEventToTabObject } from "../../utils/http_client/utils";
import Tippy from "@tippy.js/react";

interface IRequestMatchTypeProps {
  matchType: string;
  matchedRequestId: string;
  app: string;
  user: any; //Replace 'any' with actual User definition from types
  onClick: (matchedRequestId: string) => void;
}

type visibleRadioButton = "showHeaders" | "showQueryParams" | "showBody";

function RequestMatchType(props: IRequestMatchTypeProps) {
  const [matchRequestShowPopup, setMatchRequestShowPopup] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [apiEvent, setApiEvent] = useState<any>();
  const [paramsType, setParamsType] = useState<visibleRadioButton>("showBody");

  let titleText = "",
    fillColor = "red";
  switch (props.matchType) {
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
      const matchId = props.matchedRequestId;
      if (matchId && props.matchType === "FuzzyMatch") {
        const {
          app,
          user: { customer_name: customerId },
        } = props;
        const requestIds = [matchId];
        errorMessage && setErrorMessage("");
        cubeService
          .fetchAPIEventData(customerId, app, requestIds, ["HTTPRequest"])
          .then((result: any) => {
            setApiEvent(result.objects);
            setMatchRequestShowPopup(true);
          })
          .catch((error) => {
            console.error(error);
            apiEvent && setApiEvent(null);
            setErrorMessage("Failed to load request.");
            setMatchRequestShowPopup(true);
          });
      }
    },
    [props.matchedRequestId]
  );

  const onHidePopup = React.useCallback(() => {
    setMatchRequestShowPopup(false);
  }, []);

  const updateParamHandler: UpdateParamHandler = React.useCallback(
    (
      isOutgoingRequest: boolean,
      tabId: string,
      type: string,
      key: string,
      value: string | boolean
    ) => {
      if (key == "paramsTypematchType") {
        //here we need only param radio buttons, not other "key" types
        setParamsType(value as visibleRadioButton);
      }
    },
    [setParamsType]
  );

  let apiEventsFormatted = null;
  if (apiEvent) {
    apiEventsFormatted = formatHttpEventToTabObject(
      props.matchedRequestId,
      [],
      apiEvent
    );
  }

  return (
    <>
      <div onClick={onClick} className={"requestMatch " + props.matchType}>
        <Tippy content={titleText}  arrow={true} placement="bottom">
          <svg width="25" height="13">
            <rect
              width="25"
              height="13"
              style={{ fill: fillColor, strokeWidth: 1, stroke: "gray" }}
            />
          </svg>
        </Tippy>
      </div>
      <Modal show={matchRequestShowPopup} onHide={onHidePopup}>
        <Modal.Header>
          <Modal.Title>Matched Request Details</Modal.Title>
        </Modal.Header>

        <Modal.Body className={"text-center padding-15"}>
          <div className="requestMatchTypeBody">
            {apiEventsFormatted && (
              <HttpRequestMessage
                id="matchType"
                httpMethod={apiEventsFormatted.httpMethod}
                httpURL={apiEventsFormatted.httpURL}
                headers={apiEventsFormatted.headers}
                queryStringParams={apiEventsFormatted.queryStringParams}
                formData={apiEventsFormatted.formData}
                rawData={apiEventsFormatted.rawData} //Check
                rawDataType={apiEventsFormatted.rawDataType}
                paramsType={paramsType}
                bodyType={apiEventsFormatted.bodyType}
                showBody={false}
                showRawData={false}
                showFormData={true}
                showHeaders={false}
                showQueryParams={false}
                updateBodyOrRawDataType={() => {}}
                addOrRemoveParam={() => {}}
                isOutgoingRequest={false}
                tabId=""
                updateAllParams={() => {}}
                updateParam={updateParamHandler}
                readOnly={true}
              ></HttpRequestMessage>
            )}
            {errorMessage && <div style={{ color: "red" }}>{errorMessage}</div>}
          </div>
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

const mapStateToProps = (state) =>
  ({
    app: state.cube.selectedApp,
    user: state.authentication.user,
  } as Partial<IRequestMatchTypeProps>);

const connectedRequestMatchType = connect(mapStateToProps)(RequestMatchType);

export default connectedRequestMatchType;
