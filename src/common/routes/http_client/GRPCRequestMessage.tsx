import React, { useEffect } from 'react';
import HttpRequestHeaders from './HttpRequestHeaders';
import HttpRequestRawData from './HttpRequestRawData';
import Tippy from '@tippy.js/react';
import { FormGroup, FormControl, InputGroup } from 'react-bootstrap';
import { applyEnvVarsToUrl } from "../../utils/http_client/envvar";
import { IRequestParamData, IGrpcSchema, IGrpcConnect } from '../../reducers/state.types';
import { UpdateParamHandler, AddOrRemoveHandler } from './HttpResponseHeaders';
import { 
    getGrpcMethodsFromService,
    getGrpcDataForSelectedValues,
} from '../../utils/http_client/grpc-utils';
import './GRPCRequestMessage.css';
import { Link } from 'react-router-dom';
import { string } from 'prop-types';

export declare type UpdateGRPCDataHandler = (
    isOutgoingRequest: boolean,
    tabId: string,
    type: string,
    key: string,
    value: string | boolean| FileList | null,
    id?: any
  ) => void;

export declare type UpdateGrpcConnectData = (
    isOutgoingRequest: boolean,
    tabId: string,
    value: IGrpcConnect,
  ) => void;

export interface IGRPCRequestMessage {
    httpURL: string;
    grpcData: any; //string; // TODO: add proper definition
    paramsType: string;
    selectedApp: string;
    appGrpcSchema: IGrpcSchema;
    grpcConnectionSchema: IGrpcConnect,
    updateGrpcConnectData: UpdateGrpcConnectData,
    addOrRemoveParam: AddOrRemoveHandler,
    updateParam: UpdateParamHandler;
    updateAllParams: UpdateParamHandler;
    isOutgoingRequest: boolean;
    readOnly: boolean;
    disabled: boolean;
    id: string;
    tabId: string;
    headers: any[] // TODO: get proper interface
}

const GRPCRequestMessage = (props: IGRPCRequestMessage) => {
    const { 
        appGrpcSchema, selectedApp, readOnly, disabled, tabId,
        httpURL, isOutgoingRequest, updateParam,
        addOrRemoveParam, updateAllParams, headers, updateGrpcConnectData,
        paramsType, 
    } = props;

    //Below are for backward compatibility and can be merged with above props after few releases.
    const { service, endpoint, method } = props.grpcConnectionSchema || {};
    const grpcData = props.grpcData || "";

    const services: string[] = appGrpcSchema[selectedApp] ? Object.keys(appGrpcSchema[selectedApp]) : [];

    const methods: string[] = getGrpcMethodsFromService(appGrpcSchema, selectedApp, service);

    const grpcDataForSelectedOptions = getGrpcDataForSelectedValues(grpcData, selectedApp, service, method);

    const handleGRPCRadioClick = (radioValue: string) => {
        updateParam(isOutgoingRequest, tabId, 'paramsType', 'paramsType', radioValue);
    };
    
    const handleServiceChange = (value: string) => {
        const methodsForSelectedService: string[] = getGrpcMethodsFromService(appGrpcSchema, selectedApp, value);

        const resetSelectedMethod = methodsForSelectedService.length !== 0 ? methodsForSelectedService[0] : '';

        const updatedUrl = `${endpoint}.${value}/${resetSelectedMethod}`;

        updateGrpcConnectData(isOutgoingRequest, tabId, { app: selectedApp, service: value, method: resetSelectedMethod, endpoint });

        // updateParam(isOutgoingRequest, tabId, "httpURL", "httpURL", updatedUrl); TODO: keep this for now

    }
    
    const handleMethodChange = (value: string) => {

        const updatedUrl = `${endpoint}.${service}/${value}`;

        updateGrpcConnectData(isOutgoingRequest, tabId, { app: selectedApp, service, method: value, endpoint })

        // updateParam(isOutgoingRequest, tabId, "httpURL", "httpURL", updatedUrl); TODO: keep this for now
    }

    const handleEndpointChange = (value: string) => {

        const updatedUrl = `${value}.${service}/${method}`;

        updateGrpcConnectData(isOutgoingRequest, tabId, { app: selectedApp, service, method, endpoint: value });

        // updateParam(isOutgoingRequest, tabId, "httpURL", "httpURL", updatedUrl); TODO: keep this for now
    };

    const generateUrlTooltip = (url: string) => {
        let urlRendered = url;
        let err = "";
        try {
            urlRendered = applyEnvVarsToUrl(url)
        } catch (e) {
            err = e.toString()
        }

        return urlRendered ? 
                <div>
                    <p style={{fontSize:12}}>{urlRendered}</p>
                        {err && <p style={{fontSize: 9, color: "red"}}>{err}</p>}
                </div>
                : <></>;
    
    };

    const handleGRPCDataUpdate: UpdateGRPCDataHandler = (
            isOutgoingRequest: boolean, 
            tabId: string, 
            type: string, 
            key: string, 
            value: any) => {

        const updatedGrpcData = { ...grpcData };

        updatedGrpcData[selectedApp][service][method]['data'] = value.trim();

        updateParam(isOutgoingRequest, tabId, type, key, updatedGrpcData);
    };

    const onTippyShow =(instance: any) => {  
        if(instance.props.content == null || instance.props.content.innerText == ""){ 
            return false 
        } 
        return;
    };

    const urlRendered = generateUrlTooltip(httpURL);

    const urlTextBox = (
        <FormGroup bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}}>
            <InputGroup>
                <FormControl 
                    type="text" 
                    placeholder="https://0.0.0.0:8080/package"
                    style={{fontSize: "12px"}} 
                    readOnly={readOnly} 
                    disabled={disabled}
                    name="httpURL" 
                    value={endpoint} 
                    onChange={(event) => handleEndpointChange(event.target.value)}
                />
                <InputGroup.Addon className="grpcrm-request-view-added-control">.{service}/{method}</InputGroup.Addon>
            </InputGroup>
        </FormGroup>
    );

    useEffect(() => {
        if(appGrpcSchema[selectedApp] && appGrpcSchema[selectedApp].length !== 0) {
            handleServiceChange(Object.keys(appGrpcSchema[selectedApp])[0]);
        }
    }, [appGrpcSchema, selectedApp]);

    return (
        <div className='grpcrm-input-root'>
            <div className='grpcrm-input-container'>
                <div className='grpcrm-dropdown-container'>
                    <span>SERVICE</span>
                    <select
                        value={service}
                        className='form-control'
                        disabled={disabled}
                        onChange={(event) => handleServiceChange(event.target.value)}
                    >
                        {services.map(service => <option value={service}>{service}</option>)}
                    </select>
                </div>
                <div className='grpcrm-dropdown-container'>
                    <span>METHOD</span>
                    <select
                        value={method}
                        className='form-control'
                        disabled={disabled}
                        onChange={(event) => handleMethodChange(event.target.value)}
                    >
                        {methods.map(method => <option value={method}>{method}</option>)}
                    </select>
                </div>
            </div>
            {
                !appGrpcSchema[selectedApp] 
                && 
                <div className="grpcrm-request-view-proto-error">
                    No proto files found selected app. Please <Link to={`/configs?tabId=4`}>add proto files from gRPC configuration</Link> section.
                </div>
            }
            <div className='grpcrm-endpoint-container'>
                <span>ENDPOINT</span>
                {/* <span>{`ENDPOINT:   ${httpURL}`}</span> */}
                <Tippy 
                    content={urlRendered} 
                    arrow={false} 
                    arrowType="round" 
                    enabled={true} //{!this.props.disabled} 
                    interactive={true} 
                    theme={"google"} 
                    size="large" 
                    placement="bottom-start"
                    onShow={onTippyShow}
                >
                    {urlTextBox}
                </Tippy>
            </div>
            <div className='grpcrm-request-body-container'>
                <div className='grpcrm-request-body-radio-menu'>
                    <span className='grpcrm-request-view-label'>VIEW</span>
                    <div className='grpcrm-request-view-input-container'>
                        <input
                            className='grpcrm-request-view-input-radio'
                            onChange={() => handleGRPCRadioClick('showHeaders')}
                            checked={paramsType === 'showHeaders'}
                            disabled={disabled}
                            type='radio'
                        />
                        <div className='grpcrm-request-view-input-label'>Headers</div>
                    </div>
                    <div className='grpcrm-request-view-input-container'>
                        <input
                            className='grpcrm-request-view-input-radio'
                            onChange={() => handleGRPCRadioClick('showBody')}
                            checked={paramsType === 'showBody'}
                            disabled={disabled}
                            type='radio'
                        />
                        <span className='grpcrm-request-view-input-label'>Body</span>
                    </div>
                </div>
                {
                    paramsType === 'showHeaders' &&
                    <div className="grpcrm-request-view-input-container">
                        <HttpRequestHeaders
                            tabId={tabId}
                            isOutgoingRequest={isOutgoingRequest}
                            updateParam={updateParam}
                            readOnly={readOnly}
                            addOrRemoveParam={addOrRemoveParam}
                            updateAllParams={updateAllParams}
                            isResponse={false}
                            showHeaders={true}
                            headers={headers}
                        />
                    </div>

                }
                {
                    paramsType === 'showBody' &&
                    <div className="">
                        <HttpRequestRawData
                            tabId={tabId}
                            showRawData={true}
                            rawData={grpcDataForSelectedOptions}
                            updateParam={handleGRPCDataUpdate}
                            isOutgoingRequest={isOutgoingRequest}
                            readOnly={readOnly}
                            paramName="grpcData"
                        />
                    </div>
                }
            </div>
        </div>
    )
};

export default GRPCRequestMessage;
