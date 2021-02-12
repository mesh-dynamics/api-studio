
//Use this util file to create new function definition in Typescript and to move existing function to make them typescript compliant

import { IApiTrace } from "../../reducers/state.types";

export function sortApiTraceChildren(apiTraces: IApiTrace[]){
    apiTraces.forEach(trace => {
        if(trace.children){
          trace.children = trace.children.sort((trace1, trace2) => trace1.reqTimestamp - trace2.reqTimestamp);
        }
    });
}

export function sortCatalogTraceChildren(apiTraces: any[]){
    apiTraces.forEach(trace => {
        if(trace.res){
          trace.res = trace.res.sort((trace1: IApiTrace, trace2: IApiTrace) => trace1.reqTimestamp - trace2.reqTimestamp);
        }
    });
}
