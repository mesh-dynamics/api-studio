import { connect } from "react-redux";
import React from "react";

import MockConfigUtils from "../../../utils/http_client/mockConfigs.utils";
import { getDefaultServiceName } from "../../../utils/http_client/httpClientUtils";
import { IMockConfig, IRequestParamData, IStoreState } from "../../../reducers/state.types";
import { Button, FormControl } from "react-bootstrap";
import Tippy from "@tippy.js/react";
import { httpClientActions } from "../../../actions/httpClientActions";
export interface IServiceSelectorProps {
  onChange: (service: string) => void;
  selectedService: string | undefined;
  selectedMockConfig: string;
  mockConfigList: IMockConfig[];
  readOnly: boolean;
  serviceSelectionSuggestion: React.ReactChild;
  dispatch: any;
}
function ServiceSelector(props: IServiceSelectorProps) {
  const mockConfigUtils = new MockConfigUtils({
    selectedMockConfig: props.selectedMockConfig,
    mockConfigList: props.mockConfigList,
  });
  const serviceList = mockConfigUtils.getCurrentMockConfig().serviceConfigs.map((config) => config.service);
  const defaultService = getDefaultServiceName();
  serviceList.unshift(defaultService);
  let selectedService = props.selectedService;
  if (!selectedService) {
    selectedService = defaultService;
  } else if (!serviceList.includes(selectedService)) {
    serviceList.push(selectedService);
  }

  const onAddService = () => {
    props.dispatch(httpClientActions.updateAddToService(props.selectedService));
  };

  const handleChange = React.useCallback((event: React.ChangeEvent<HTMLSelectElement & FormControl>) => {
    !props.readOnly && props.onChange(event.target.value);
  }, []);

  const getTipForService = () => {
    /**
     * The tip will be available for either case:
     *  1. service is set in Event, but service is not defined in configuration : Tip to add service to service config
     *  2. service is not set (noservice) and the httpURL matches from one of service's targetURL : Tip to select a service
     */
    if (props.readOnly) {
      return null;
    }

    let content = null;

    if (props.serviceSelectionSuggestion) {
      content = props.serviceSelectionSuggestion;
    } else {
      const currentService = mockConfigUtils.getCurrentService(props.selectedService);
      const isServiceDefined = !!(props.selectedService == getDefaultServiceName() || currentService?.url);
      if (!isServiceDefined) {
        content = (
          <div>
            <div className="font-12">Service <b>{props.selectedService}</b> is not defined in selected service config.</div>
            <Button className="btn btn-sm cube-btn left margin-top-5" style={{padding: "2px 10px", fontSize : "10px"}} onClick={onAddService}
            title={`Add ${props.selectedService} to service configurations`}>
              <i className="fa fa-plus"></i> Add
            </Button>
          </div>
        );
      }
    }

    if (content) {
      return (
        <Tippy content={content} arrow={false} hideOnClick={true} arrowType="round" interactive={true} maxWidth="380px" theme={"light"} size="large" placement="right" zIndex={50}>
          <i className="fa fa-info-circle margin-top-10 red margin-left-5"></i>
        </Tippy>
      );
    }
    return null;
  };

  return (
    <>
      <FormControl
        componentClass="select"
        placeholder="Service"
        style={{ fontSize: "12px", minWidth: "150px", marginLeft: "10px" }}
        name="service"
        disabled={props.readOnly}
        readOnly={props.readOnly}
        value={selectedService}
        onChange={handleChange}
      >
        <option disabled={true}>Select Service</option>
        {serviceList.map((service) => (
          <option value={service}>{service == getDefaultServiceName() ? "No service selected" : service}</option>
        ))}
      </FormControl>
      {getTipForService()}
    </>
  );
}

const mapStateToProps = (state: IStoreState) => {
  const {
    httpClient: { selectedMockConfig, mockConfigList },
  } = state;
  //Progressively, get as much possible props from redux state by tabId, rather then passing as props between components
  return {
    selectedMockConfig,
    mockConfigList,
  };
};

export default connect(mapStateToProps)(ServiceSelector);
