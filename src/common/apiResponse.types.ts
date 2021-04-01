/*
Define here the types from API Response data. 
It can be changed based on type of response changing from backend
*/

import { IEventData, ITimelineData, ICollectionDetails, IApiTrace, IPayloadData, IDiffCompOperation } from "./reducers/state.types";

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