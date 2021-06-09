import React, { useEffect } from 'react';
import HttpRequestHeaders from './HttpRequestHeaders';
import HttpRequestRawData from './HttpRequestRawData';
import Tippy from '@tippy.js/react';
import { FormGroup, FormControl, InputGroup } from 'react-bootstrap';
import { applyEnvVarsToUrl } from "../../utils/http_client/envvar";
import { IRequestParamData, IGrpcSchema, IGrpcConnect, IGrpcData } from '../../reducers/state.types';
import { UpdateParamHandler, AddOrRemoveHandler } from './HttpResponseHeaders';
import { 
    getGrpcMethodsFromService,
    getGrpcDataForSelectedValues,
    parsePackageAndServiceName,
} from '../../utils/http_client/grpc-utils';
import './GRPCRequestMessage.css';
import { Link } from 'react-router-dom';
import AutoCompleteBox from './components/AutoCompleteBox';

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
    currentSelectedTabId: string
  ) => void;

export interface IGRPCRequestMessage {
    httpURL: string;
    grpcData: IGrpcData;
    paramsType: string;
    appGrpcSchema: IGrpcSchema;
    currentSelectedTabId: string;
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
    clientTabId: string;
    headers: IRequestParamData[];
}

const GRPCRequestMessage = (props: IGRPCRequestMessage) => {
    const { 
        appGrpcSchema, readOnly, disabled, tabId,
        httpURL, isOutgoingRequest, updateParam, currentSelectedTabId,
        addOrRemoveParam, updateAllParams, headers, updateGrpcConnectData,
        paramsType, 
    } = props;

    //Below are for backward compatibility and can be merged with above props after few releases.
    const { service: sericeAndPackageName, endpoint, method: methodName } = props.grpcConnectionSchema || {};
    const grpcData = props.grpcData || {};
    const { packageName, serviceName, servicePackageName} = parsePackageAndServiceName(sericeAndPackageName, appGrpcSchema);
    const methods: string[] = getGrpcMethodsFromService(appGrpcSchema, packageName, serviceName);
    const method = methodName || (methods.length > 0 ? methods[0] : "");
    const grpcDataForSelectedOptions = getGrpcDataForSelectedValues(grpcData, packageName, serviceName, method);

    const handleGRPCRadioClick = (radioValue: string) => {
        updateParam(isOutgoingRequest, tabId, 'paramsType', 'paramsType', radioValue);
    };
    
    const handleServiceChange = (value: string) => {
        const { packageName: selectedPackage, serviceName: selectedService } = parsePackageAndServiceName(value);
        const methodsForSelectedService: string[] = getGrpcMethodsFromService(appGrpcSchema, selectedPackage, selectedService);

        const resetSelectedMethod = methodsForSelectedService.length !== 0 ? methodsForSelectedService[0] : '';

        const tabValue: IGrpcConnect = { service: value, method: resetSelectedMethod, endpoint };

        updateGrpcConnectData(isOutgoingRequest, tabId, tabValue, currentSelectedTabId);

        // const updatedUrl = `${endpoint}.${value}/${resetSelectedMethod}`;
        // updateParam(isOutgoingRequest, tabId, "httpURL", "httpURL", updatedUrl); TODO: keep this for now

    }
    
    const handleMethodChange = (value: string) => {
        
        const tabValue: IGrpcConnect = { service: servicePackageName, method: value, endpoint };
        
        updateGrpcConnectData(isOutgoingRequest, tabId, tabValue, currentSelectedTabId);

        // const updatedUrl = `${endpoint}.${service}/${value}`;
        // updateParam(isOutgoingRequest, tabId, "httpURL", "httpURL", updatedUrl); TODO: keep this for now
    }

    const handleEndpointChange = (value: string) => {

        const tabValue: IGrpcConnect = { service: servicePackageName, method, endpoint: value };

        updateGrpcConnectData(isOutgoingRequest, tabId, tabValue, currentSelectedTabId);

        // const updatedUrl = `${value}.${service}/${method}`;
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

        updatedGrpcData[packageName][serviceName][method]['data'] = value.trim();

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
        <FormGroup bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}} className="autocomplete">
            <InputGroup>
                <AutoCompleteBox 
                    id="grpcUrl"
                    placeholder="https://0.0.0.0:8080"
                    style={{fontSize: "12px"}} 
                    readOnly={readOnly} 
                    disabled={disabled}
                    name="httpURL" 
                    value={endpoint} 
                    onChange={(event) => handleEndpointChange((event.target as HTMLInputElement).value)}
                />
                <InputGroup.Addon className="grpcrm-request-view-added-control">/{servicePackageName}/{method}</InputGroup.Addon>
            </InputGroup>
        </FormGroup>
    );

    useEffect(() => {
        if(appGrpcSchema[packageName] && appGrpcSchema[packageName].length !== 0 && (!serviceName && !method)) {
            handleServiceChange(Object.keys(appGrpcSchema[packageName])[0]);
        }
    }, [appGrpcSchema, packageName, serviceName]);

    const serviceOptions = [];
    for(var packageVal in appGrpcSchema){
         for(const serviceName in appGrpcSchema[packageVal]){
             const displayName = `${packageVal}.${serviceName}`;
            serviceOptions.push(<option key={displayName} value={displayName}>{displayName}</option>)
         }
    }
    return (
        <div className='grpcrm-input-root'>
            <div className='grpcrm-input-container'>
                <div className='grpcrm-dropdown-container'>
                    <span>SERVICE</span>
                    <select
                        value={servicePackageName}
                        className='form-control'
                        disabled={disabled}
                        onChange={(event) => handleServiceChange(event.target.value)}
                    >
                        {serviceOptions}
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
                        {methods.map((method, index) => <option key={`${method}${index}`} value={method}>{method}</option>)}
                    </select>
                </div>
            </div>
            {
                (!appGrpcSchema
                || Object.keys(appGrpcSchema).length === 0)
                && 
                <div className="grpcrm-request-view-proto-error">
                    { props.disabled ? <>&nbsp;</> 
                    : <>No proto files found selected app. Please <Link to={`/configs?tabId=4`}>add proto files from gRPC configuration</Link> section.</>
                    }
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
                            clientTabId={props.clientTabId}
                            isOutgoingRequest={isOutgoingRequest}
                            updateParam={updateParam}
                            readOnly={readOnly}
                            addOrRemoveParam={addOrRemoveParam}
                            updateAllParams={updateAllParams}
                            isResponse={false}
                            showHeaders={true}
                            headers={headers}
                            hideInternalHeaders={props.disabled}
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
