import React from "react";
import { Badge, Button } from "react-bootstrap";
import { filterInternalHeaders } from "../../utils/http_client/utils";
import { connect } from "react-redux";
import { IRequestParamData, IStoreState } from "../../reducers/state.types";
import { httpClientActions } from "../../actions/httpClientActions";

export interface IHideInternalHeadersButtonProps {
  clientTabId: string;
  hideInternalHeaders: boolean;
  hiddenCount: number;
  headers: IRequestParamData[];
  dispatch: any;
}

function HideInternalHeadersButton(props: IHideInternalHeadersButtonProps) {
  if (!props.clientTabId) {
    return <></>;
  }
  const onButtonClick = () => {
    props.dispatch(httpClientActions.toggleHideInternalHeaders(props.clientTabId));
  };

  return (
    <div className="pull-right">
      <Button bsStyle="link" onClick={onButtonClick} className="font-12">
        {props.hideInternalHeaders
          ? <>Show all headers <Badge title="Hidden headers count">+{props.hiddenCount}</Badge></>
          : "Hide internal headers"}
      </Button>
    </div>
  );
}

const mapStateToProps = (
  state: IStoreState,
  props: IHideInternalHeadersButtonProps
) => {
  const currentTab = state.httpClient.tabs.find((tab) => tab.id == props.clientTabId);
  const hideInternalHeaders = !!currentTab?.hideInternalHeaders;
  let hiddenCount = 0;
  if (hideInternalHeaders && currentTab) {
    hiddenCount =
      props.headers.length -
      filterInternalHeaders(props.headers, hideInternalHeaders).length;
  }
  return {
    hideInternalHeaders,
    hiddenCount,
  };
};

export default connect(mapStateToProps)(HideInternalHeadersButton);
