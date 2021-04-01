//Use this util file to create new function definition in Typescript and to move existing function to make them typescript compliant

import { IApiTrace, ICollectionDetails, IContextMap, IKeyValuePairs, IStoreState } from "../../reducers/state.types";

import { store } from "../../helpers";
import { cubeService } from "../../services";
import { httpClientActions } from "../../actions/httpClientActions";

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

function searchCollection(collections: ICollectionDetails[], collectionId: string) {
  return (collections || []).find((collec) => collec.collec == collectionId);
}

export async function getCollectionDetailsById(collectionId: string): Promise<ICollectionDetails | undefined> {
  const state = store.getState() as IStoreState;
  const goldenList = state.apiCatalog.goldenList,
    collectionList = state.apiCatalog.collectionList,
    actualGoldens = state.gcBrowse.actualGoldens.recordings,
    userGoldens = state.gcBrowse.userGoldens.recordings,
    cachedGoldens = state.httpClient.collectionsCache;
  const collectionFound =
    searchCollection(goldenList, collectionId) ||
    searchCollection(collectionList, collectionId) ||
    searchCollection(actualGoldens, collectionId) ||
    searchCollection(userGoldens, collectionId) ||
    searchCollection(cachedGoldens, collectionId);
  if (collectionFound) {
    return collectionFound;
  } else {
    const collectionResult = await cubeService.fetchCollectionbyCollectionId(state.authentication.user, collectionId);
    if (collectionResult.numFound > 0) {
      store.dispatch(httpClientActions.addCachedCollections(collectionResult.recordings));
      return collectionResult.recordings[0];
    }
  }
  return undefined;
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

export function isTrueOrUndefined(value: any){
  return value == undefined || value == "true" || value == true;
}
