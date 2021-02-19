/*
Define here the types from API Response data. 
It can be changed based on type of response changing from backend
*/

import { IEventData, ITimelineData } from "./reducers/state.types";

export interface IGetEventsApiResponse{
    numResults: number;
    numFound: number;
    objects:IEventData[]
}

 export interface ITimelineResponse{
    numFound: number
    timelineResults:ITimelineData[]
 }