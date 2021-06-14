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

import { IApiTrace, IEventData, IGrpcSchema, IHttpClientTabDetails } from "../../reducers/state.types";
import { extractParamsFromRequestEvent } from "./utils";
import { applyGrpcDataToRequestObject, getConnectionSchemaFromMetadataOrApiPath, setGrpcDataFromDescriptor } from "./grpc-utils";
import { getDefaultServiceName, joinPaths } from "./httpClientUtils";
import MockConfigUtils from "./mockConfigs.utils";
import { v4 as uuidv4 } from 'uuid';

/**
 * This Utility class should contain all code for creating a new Tab Object.
 * Progressively the RequestTab Data creation from HttpClientTab and other places should be moved into this library
 * This can help in finding if any field is missing while creating a new object or having common logic across Objects.
 */

export default class TabDataFactory {
  private requestEvent: IEventData;
  private responseEvent: IEventData;
  constructor(requestEvent: IEventData, responseEvent: IEventData) {
    this.requestEvent = requestEvent;
    this.responseEvent = responseEvent;
  }

  getReqObjectForSidebar(node: IApiTrace, collectionName: string, appGrpcSchema: IGrpcSchema): IHttpClientTabDetails {
    const { headers, queryParams, formData, rawData, rawDataType, grpcRawData, multipartData, httpURL } = extractParamsFromRequestEvent(this.requestEvent);

    /* Notes for understanding: 
        grpcRawData from extracted params is actual raw Data for gRPC Requests
        grpcData in reqObject event[Tab data] is the IGrpcData type object with package.service.method.data form, 
        where valueof(package.service.method.data) = grpcRawData (actual rawData)
    */
   let urls = {
    httpURL: httpURL, //"{{{url}}}/" + this.requestEvent.apiPath,
    httpURLShowOnly: this.requestEvent.apiPath,
    requestPathURL: this.requestEvent.metaData.requestPathURL || this.requestEvent.apiPath,
   };
    if(this.requestEvent.service && this.requestEvent.service != getDefaultServiceName()){
        const domain = new MockConfigUtils().getCurrentService(this.requestEvent.service)?.url || this.requestEvent.service;
        urls.httpURL = joinPaths(domain, urls.requestPathURL);
    }
    
    //TODO: Create a separate class to handle below object
    let reqObject: IHttpClientTabDetails = {
      ...urls,
      httpMethod: this.requestEvent.payload[1].method.toLowerCase(),
      headers: headers,
      queryStringParams: queryParams,
      bodyType:
        multipartData && multipartData.length > 0
          ? "multipartData"
          : formData && formData.length > 0
          ? "formData"
          : rawData && rawData.length > 0
          ? "rawData"
          : grpcRawData && grpcRawData.length
          ? "grpcData"
          : "formData",
      formData: formData,
      multipartData,
      rawData: rawData,
      rawDataType: rawDataType,
      paramsType: grpcRawData && grpcRawData.length ? "showBody" : "showQueryParams",
      responseStatus: "NA",
      responseStatusText: "",
      responseHeaders: "",
      responseBody: "",
      responsePayloadState: this.responseEvent?.payload[1].payloadState!,
      recordedResponseHeaders: this.responseEvent && this.responseEvent.payload[1].hdrs ? JSON.stringify(this.responseEvent.payload[1].hdrs, undefined, 4) : "",
      recordedResponseBody: this.responseEvent ? (this.responseEvent.payload[1].body ? JSON.stringify(this.responseEvent.payload[1].body, undefined, 4) : "") : "",
      recordedResponseStatus: this.responseEvent ? this.responseEvent.payload[1].status : "",
      responseBodyType: "json",
      requestId: this.requestEvent.reqId,
      outgoingRequestIds: node.children ? node.children.map((eachChild) => eachChild.requestEventId) : [],
      eventData: [this.requestEvent, this.responseEvent],
      showOutgoingRequestsBtn: node.children && node.children.length > 0,
      showSaveBtn: true,
      recordingIdAddedFromClient: node.recordingIdAddedFromClient,
      collectionIdAddedFromClient: node.collectionIdAddedFromClient,
      collectionNameAddedFromClient: collectionName,
      traceIdAddedFromClient: node.traceIdAddedFromClient,
      outgoingRequests: [],
      showCompleteDiff: false,
      isOutgoingRequest: false,
      service: this.requestEvent.service,
      requestRunning: false,
      showTrace: false,
      grpcData: setGrpcDataFromDescriptor(appGrpcSchema, applyGrpcDataToRequestObject(grpcRawData, this.requestEvent.metaData.grpcConnectionSchema)),
      grpcConnectionSchema: getConnectionSchemaFromMetadataOrApiPath(this.requestEvent.metaData.grpcConnectionSchema, this.requestEvent.apiPath),
      hideInternalHeaders: true,
    };
    return reqObject;
  }

  getReqObjectForAPICatalog(reqId: string, requestIdsObj: any, serviceUrl: string): IHttpClientTabDetails {
    //Find out proper definition for requestIdsObj
    const { headers, queryParams, formData, rawData, rawDataType, grpcRawData, multipartData, httpURL } = extractParamsFromRequestEvent(this.requestEvent);


   let urls = {
    httpURL: httpURL, //"{{{url}}}/" + this.requestEvent.apiPath,
    httpURLShowOnly: this.requestEvent.apiPath,
    requestPathURL: this.requestEvent.metaData.requestPathURL || this.requestEvent.apiPath,
   };
    if(this.requestEvent.service && this.requestEvent.service != getDefaultServiceName()){
        const domain = new MockConfigUtils().getCurrentService(this.requestEvent.service)?.url || this.requestEvent.service;
        urls.httpURL = joinPaths(domain, urls.requestPathURL);
    }

    let reqObject: IHttpClientTabDetails = {
      httpMethod: this.requestEvent.payload[1].method.toLowerCase(),
      ...urls,
      headers: headers,
      queryStringParams: queryParams,
      bodyType: multipartData.length > 0 ? "multipartData" : formData?.length > 0 ? "formData" : rawData?.length > 0 ? "rawData" : grpcRawData?.length ? "grpcData" : "formData",
      formData: formData,
      multipartData: multipartData,
      rawData: rawData,
      rawDataType: rawDataType,
      paramsType: grpcRawData && grpcRawData.length ? "showBody" : "showQueryParams",
      responseStatus: "NA",
      responseStatusText: "",
      responseHeaders: "",
      responseBody: "",
      recordedResponseHeaders: this.responseEvent && this.responseEvent.payload[1].hdrs ? JSON.stringify(this.responseEvent.payload[1].hdrs, undefined, 4) : "",
      recordedResponseBody: this.responseEvent ? (this.responseEvent.payload[1].body ? JSON.stringify(this.responseEvent.payload[1].body, undefined, 4) : "") : "",
      recordedResponseStatus: this.responseEvent ? this.responseEvent.payload[1].status : "",
      responseBodyType: "json",
      requestId: reqId,
      outgoingRequestIds: requestIdsObj[reqId] ? requestIdsObj[reqId] : [],
      eventData: [this.requestEvent, this.responseEvent],
      showOutgoingRequestsBtn: requestIdsObj[reqId] && requestIdsObj[reqId].length > 0,
      showSaveBtn: true,
      outgoingRequests: [],
      showCompleteDiff: false,
      isOutgoingRequest: false,
      service: this.requestEvent.service,
      recordingIdAddedFromClient: "",
      collectionIdAddedFromClient: this.requestEvent.collection,
      traceIdAddedFromClient: this.requestEvent.traceId,
      requestRunning: false,
      showTrace: false,
      // grpcConnectionSchema: this.requestEvent.grpcConnectionSchema,
      grpcData: applyGrpcDataToRequestObject(grpcRawData, this.requestEvent.metaData.grpcConnectionSchema),
      grpcConnectionSchema: getConnectionSchemaFromMetadataOrApiPath(this.requestEvent.metaData.grpcConnectionSchema, this.requestEvent.apiPath),
      hideInternalHeaders: true,
    };

    return reqObject;
  }

  getReqObjForOutgoingRequest(recordingId: string, collectionId: string, traceId: string, appGrpcSchema: IGrpcSchema): IHttpClientTabDetails {
    const { headers, queryParams, formData, rawData, rawDataType, multipartData, httpURL, grpcRawData } = extractParamsFromRequestEvent(this.requestEvent);
    let reqObject: IHttpClientTabDetails = {
      httpMethod: this.requestEvent.payload[1].method.toLowerCase(),
      httpURL: httpURL,
      httpURLShowOnly: httpURL,
      requestPathURL: httpURL,
      headers: headers,
      queryStringParams: queryParams,
      bodyType:
        multipartData && multipartData.length > 0
          ? "multipartData"
          : formData && formData.length > 0
          ? "formData"
          : rawData && rawData.length > 0
          ? "rawData"
          : grpcRawData
          ? "grpcData"
          : "formData",
      formData: formData,
      multipartData,
      rawData: rawData,
      rawDataType: rawDataType,
      paramsType: grpcRawData ? "showBody" : "showQueryParams",
      responseStatus: "NA",
      responseStatusText: "",
      responseHeaders: "",
      responseBody: "",
      responsePayloadState: this.responseEvent?.payload[1].payloadState,
      recordedResponseHeaders: this.responseEvent && this.responseEvent.payload[1].hdrs ? JSON.stringify(this.responseEvent.payload[1].hdrs, undefined, 4) : "",
      recordedResponseBody: this.responseEvent ? (this.responseEvent.payload[1].body ? JSON.stringify(this.responseEvent.payload[1].body, undefined, 4) : "") : "",
      recordedResponseStatus: this.responseEvent ? this.responseEvent.payload[1].status : "",
      responseBodyType: "json",
      showOutgoingRequestsBtn: false,
      isOutgoingRequest: true,
      showSaveBtn: true,
      outgoingRequests: [],
      service: this.requestEvent.service,
      recordingIdAddedFromClient: recordingId,
      collectionIdAddedFromClient: collectionId,
      traceIdAddedFromClient: traceId,
      requestRunning: false,
      showTrace: false,
      grpcData: setGrpcDataFromDescriptor(appGrpcSchema, applyGrpcDataToRequestObject(grpcRawData, this.requestEvent.metaData.grpcConnectionSchema, this.requestEvent.apiPath)),
      grpcConnectionSchema: getConnectionSchemaFromMetadataOrApiPath(this.requestEvent.metaData.grpcConnectionSchema, this.requestEvent.apiPath),
      hideInternalHeaders: true,
    };
    return reqObject;
  }

  getReqObjAfterResponse(reqId: string, isOutgoingRequest: boolean, existingId: string){
    const httpResponseEventPayload  = (this.responseEvent && this.responseEvent.payload) ?  this.responseEvent.payload[1] : null;
    const { headers, queryParams, formData, rawData, rawDataType, grpcRawData, multipartData, httpURL }  = extractParamsFromRequestEvent(this.requestEvent);
    
    let reqObject : IHttpClientTabDetails = {
        id: existingId || uuidv4(),
        httpMethod: this.requestEvent.payload[1].method.toLowerCase(),
        httpURL: httpURL,
        httpURLShowOnly: this.requestEvent.apiPath,
        requestPathURL: this.requestEvent.apiPath,
        headers: headers,
        queryStringParams: queryParams,
        bodyType: multipartData && multipartData.length > 0 ? "multipartData" : formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : grpcRawData && grpcRawData.length > 0 ? "grpcData" : "formData",
        formData: formData,
        multipartData,
        rawData: rawData,
        rawDataType: rawDataType,
        paramsType: grpcRawData ? "showBody" : "showQueryParams",
        responseStatus: "NA",
        responseStatusText: "",
        responseHeaders: "",
        responseBody: "",
        responsePayloadState: httpResponseEventPayload?.payloadState,
        recordedResponseHeaders: httpResponseEventPayload?.hdrs ? JSON.stringify(httpResponseEventPayload.hdrs, undefined, 4) : "",
        recordedResponseBody: this.responseEvent ? httpResponseEventPayload?.body ? JSON.stringify(httpResponseEventPayload.body, undefined, 4) : "" : "",
        recordedResponseStatus: this.responseEvent ? httpResponseEventPayload?.status : "",
        responseBodyType: "json",
        requestId: reqId,
        outgoingRequestIds: [],
        eventData: [this.requestEvent, this.responseEvent],
        showOutgoingRequestsBtn: false,
        showSaveBtn: true,
        outgoingRequests: [],
        showCompleteDiff: false,
        isOutgoingRequest: isOutgoingRequest,
        service: this.requestEvent.service,
        recordingIdAddedFromClient: "",
        collectionIdAddedFromClient: this.requestEvent.collection,
        traceIdAddedFromClient: this.requestEvent.traceId,
        apiPath: this.requestEvent.apiPath,
        requestRunning: false,
        showTrace: null,
        metaData: this.responseEvent ? this.responseEvent.metaData : {},
        grpcData: applyGrpcDataToRequestObject(grpcRawData, this.requestEvent.metaData.grpcConnectionSchema, this.requestEvent.apiPath),
        grpcConnectionSchema: getConnectionSchemaFromMetadataOrApiPath(this.requestEvent.metaData.grpcConnectionSchema, this.requestEvent.apiPath)
    };
    return reqObject;
  }
}
