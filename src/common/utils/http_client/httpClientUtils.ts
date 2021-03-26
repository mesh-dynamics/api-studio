//Use this util file to create new function definition in Typescript and to move existing function to make them typescript compliant

import {
  IApiTrace,
  IContextMap,
  IKeyValuePairs,
} from "../../reducers/state.types";

export function sortApiTraceChildren(apiTraces: IApiTrace[]) {
  apiTraces.forEach((trace) => {
    if (trace.children) {
      trace.children = trace.children.sort(
        (trace1, trace2) => trace1.reqTimestamp - trace2.reqTimestamp
      );
    }
  });
}

export function sortCatalogTraceChildren(apiTraces: any[]) {
  apiTraces.forEach((trace) => {
    if (trace.res) {
      trace.res = trace.res.sort(
        (trace1: IApiTrace, trace2: IApiTrace) =>
          trace1.reqTimestamp - trace2.reqTimestamp
      );
    }
  });
}

export function getMergedContextMap(
  existingContextMap: IContextMap,
  newData: IKeyValuePairs<any>
) {
  const newContextMap: IContextMap = {};
  Object.entries(existingContextMap || {}).forEach(([key, value]) => {
    if (value.createdOn > Date.now() - 1000 * 60 * 60 * 24) {
      newContextMap[key] = value;
    }
  });
  Object.entries(newData || {}).forEach(([key, value]) => {
    newContextMap[key] = { value, createdOn: Date.now() };
  });
  return newContextMap;
}

export function getContextMapKeyValues(contextMap: IContextMap){
  const contextMapKeyValues: IKeyValuePairs<any> = {};
  Object.entries(contextMap || {}).forEach(([key, value]) => {
    if (value.createdOn > Date.now() - 1000 * 60 * 60 * 24) {
      contextMapKeyValues[key] = value.value;
    }
  });
  return contextMapKeyValues;
}

export function getDefaultServiceName(){
  return "none";
}
export function joinPaths(path1: string, path2: string){
  let joinWith = "";
  if(!(path1.endsWith("/") || path2.startsWith("/"))){
    joinWith = "/";
  }
  return path1 + joinWith + path2;
}