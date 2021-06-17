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

/*
Define here the types from API Response data. 
It can be changed based on type of response changing from backend
*/

import { IEventData, ITimelineData, ICollectionDetails, IApiTrace, IPayloadData, IDiffCompOperation, IAppInfo } from "./reducers/state.types";

export interface IGetEventsApiResponse{
    numResults: number;
    numFound: number;
    objects:IEventData[]
}

export interface IGetApiTraceResponse {
    response: IApiTrace[];
    numFound: number;
}

 export interface ITimelineResponse{
    numFound: number;
    timelineResults:ITimelineData[];
 }

 export interface ICollectionListApiResponse {
    numFound: number;
    recordings: ICollectionDetails[]
 }

 export interface IReqRespMatchResultResponseRes{
        instanceId: null
        recordResponseTruncated: null
        replayResponseTruncated: null
        numMatch: number;
        path: string;
        recordReqId:  string;
        recordReqTime?: number;
        recordRequest: IPayloadData;
        recordRespTime?: number;
        recordResponse: IPayloadData;
        recordTraceId:  string;
        recordedParentSpanId:  string;
        recordedSpanId:  string;
        replayReqId: string;
        replayReqTime?: number;
        replayRequest: IPayloadData;
        replayRespTime?: number;
        replayResponse: IPayloadData;
        replayTraceId:  string;
        replayedParentSpanId:  string;
        replayedSpanId:  string;
        reqCompDiff: IDiffCompOperation[];
        reqCompResType:  string;
        reqMatchResType:  string;
        respCompDiff: IDiffCompOperation[];
        respCompResType:  string;
        service:  string;
    
 }
 export interface IReqRespMatchResultResponse {
    res:IReqRespMatchResultResponseRes
 }

 export interface IServiceGroup{
   app: IAppInfo,
   createdAt: string,
   updatedAt: string,
   id: number,
   name: string,
 }
 export interface IService{
   app: IAppInfo,
   createdAt: string,
   updatedAt: string,
   id: number,
   name: string,
   serviceGroups:IServiceGroup
 }
export interface IServiceListResponse{
   service: IService,
   prefixes:any[]
}

export interface IPathListResponse{
   service: IService,
   paths: string[]
}