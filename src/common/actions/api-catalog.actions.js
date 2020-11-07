import { cubeService } from "../services"
import { apiCatalogConstants } from "../constants/api-catalog.constants";
import {
    getServiceList,
    getIncomingAPIList,
    getInstanceList,
    getDefaultTraceApiFilters,
    getLastApiTraceEndTimeFromApiTrace
} from "../utils/api-catalog/api-catalog-utils";
import _ from "lodash";

export const apiCatalogActions = {
    getDiffData: (app, requestIdLeft, requestIdRight) => async (dispatch, getState) => {
        const { user: { customer_name: customerId } } = getState().authentication;
        try {
            const data = await cubeService.fetchAPIEventData(customerId, app, [requestIdLeft, requestIdRight], [])

            if (data.numFound) {
                const [requestLeft, requestRight] = data.objects.filter(obj => obj.eventType === "HTTPRequest");
                const [responseLeft, responseRight] = data.objects.filter(obj => obj.eventType === "HTTPResponse");

                dispatch(apiCatalogActions.successDiffData({ requestLeft, requestRight, responseLeft, responseRight }));
            }
        } catch (error) {
            // dispatch(failure)
        }
    },

    successDiffData: (data) => ({ type: apiCatalogConstants.SET_DIFF_DATA, data: data }),

    pinCompareRequest: (data) => (dispatch, getState) => {
        let { compareRequests } = getState().apiCatalog;
        if (compareRequests.length != 2) {
            compareRequests.push(data);
        }
        dispatch(apiCatalogActions.setPinCompareRequest(compareRequests));
    },
    setPinCompareRequest: (data) => ({ type: apiCatalogConstants.PIN_COMPARE_REQUEST, data: data }),

    unpinCompareRequest: (data) => (dispatch, getState) => {
        let { compareRequests } = getState().apiCatalog;
        _.remove(compareRequests, { parentReqId: data.parentReqId })
        dispatch(apiCatalogActions.setUnPinCompareRequest(compareRequests));
    },

    setUnPinCompareRequest: (data) => ({ type: apiCatalogConstants.UNPIN_COMPARE_REQUEST, data: data }),

    resetCompareRequest: () => ({ type: apiCatalogConstants.RESET_COMPARE_REQUEST }),
    setResizedColumns: (data) => ({ type: apiCatalogConstants.SET_RESIZED_COLUMNS, data: data }),

    fetchAPIFacets: (app, selectedSource, selectedGoldenCollection, startTime, endTime, selectedService, selectedApiPath,) => async (dispatch, getState) => {
        const { authentication: { user: { customer_name: customerId } }} = getState();
        const apiFacets = await cubeService.fetchAPIFacetData(customerId, app, selectedSource, selectedGoldenCollection, startTime, endTime);
        const services = getServiceList(apiFacets);
        const apiPaths = getIncomingAPIList(apiFacets, selectedService);
        const instances = getInstanceList(apiFacets, selectedService, selectedApiPath);
        dispatch({ type: apiCatalogConstants.FETCH_API_FACETS, data: { apiFacets, services, apiPaths, instances } })
    },

    fetchGoldenCollectionList: (app, recordingType) => (dispatch, getState) => {
        const { user } = getState().authentication;

        dispatch({ type: apiCatalogConstants.FETCHING_GOLDEN_LIST });

        cubeService.fetchCollectionList(user, app, recordingType)
            .then((data) => {
                const result = data.recordings;
                if (recordingType === "UserGolden") {
                    dispatch({
                        type: apiCatalogConstants.UPDATE_COLLECTION_LIST,
                        data: result || []
                    });
                } else if (recordingType === "Golden") {
                    dispatch({
                        type: apiCatalogConstants.UPDATE_GOLDEN_LIST,
                        data: result || [],
                    });
                }
            })
            .catch(() => {
                // TODO: Add sentry (or something similar) log for api call failure
                dispatch({ type: apiCatalogConstants.GOLDEN_LIST_FETCH_COMPLETE });
            });
    },

    handleFilterChange: (metadata, value) => (dispatch, getState) => {
        const state = getState();
        const { selectedApp } = state.cube;
        let {selectedSource, selectedCollection, selectedGolden,
            selectedService, selectedGoldenService, selectedCollectionService, 
            selectedCaptureService, selectedApiPath, selectedCaptureApi,
            selectedCollectionApi, selectedGoldenApi, selectedInstance, selectedCaptureInstance,
            startTime, endTime, apiPaths, instances, apiFacets, services} = state.apiCatalog;

        switch (metadata) {
            case "selectedSource":
                if(selectedSource != value){
                    apiPaths = [];
                    services = [];
                }
                selectedSource = value;
                
                // since the source has changed, we set the selections from the respective stored values
                if (selectedSource==="UserGolden") {
                    startTime = null;
                    endTime = null;
                    selectedService = selectedCollectionService;
                    selectedApiPath = selectedCollectionApi;
                    selectedInstance = ""

                    dispatch(apiCatalogActions.fetchGoldenCollectionList(selectedApp, "UserGolden"));
                    selectedCollection && dispatch(apiCatalogActions.fetchAPIFacets(selectedApp, "UserGolden", selectedCollection, null, null, selectedService, selectedApiPath)); 
                } else if (selectedSource==="Golden") {
                    startTime = null;
                    endTime = null;
                    selectedService = selectedGoldenService;
                    selectedApiPath = selectedGoldenApi;
                    selectedInstance = ""

                    dispatch(apiCatalogActions.fetchGoldenCollectionList(selectedApp, "Golden"));
                    selectedGolden && dispatch(apiCatalogActions.fetchAPIFacets(selectedApp, "Golden", selectedGolden, null, null, selectedService, selectedApiPath)); 
                } else if (selectedSource==="Capture") {
                    startTime = new Date(Date.now() - 86400 * 1000).toISOString()
                    endTime = new Date(Date.now()).toISOString()
                    selectedService = selectedCaptureService;
                    selectedApiPath = selectedCaptureApi;
                    selectedInstance = selectedCaptureInstance

                    dispatch(apiCatalogActions.fetchAPIFacets(selectedApp, "Capture", null, startTime, endTime, selectedService, selectedApiPath));   
                }
                break;

            case "selectedCollection":
                selectedCollection = value;

                selectedService = ""
                selectedCollectionService = selectedService
                
                selectedApiPath = ""
                selectedCollectionApi = selectedApiPath

                dispatch(apiCatalogActions.fetchAPIFacets(selectedApp, "UserGolden", selectedCollection, null, null, selectedService, selectedApiPath)); 
                break;

            case "selectedGolden":
                selectedGolden = value;
                
                selectedService = ""
                selectedGoldenService = selectedService

                selectedApiPath = ""
                selectedGoldenApi = selectedApiPath

                dispatch(apiCatalogActions.fetchAPIFacets(selectedApp, "Golden", selectedGolden, null, null, selectedService, selectedApiPath)); 
                break;

            case "selectedService":
                selectedService = value;

                apiPaths = getIncomingAPIList(apiFacets, selectedService)
                selectedApiPath = "";

                
                if (selectedSource==="Golden") {
                    selectedGoldenService = selectedService;
                    selectedGoldenApi = selectedApiPath;
                } else if (selectedSource==="UserGolden") {
                    selectedCollectionService = selectedService;
                    selectedCollectionApi = selectedApiPath;
                } else if (selectedSource==="Capture") {
                    selectedCaptureService = selectedService;
                    selectedCaptureApi = selectedApiPath;
                
                    instances = getInstanceList(apiFacets, selectedService, selectedApiPath)
                    selectedInstance = !_.isEmpty(instances) ? instances[0].val : "";
                    selectedCaptureInstance = selectedInstance
                }
                break;

            case "selectedApiPath":
                selectedApiPath = value;
                if (selectedSource==="Golden") {
                    selectedGoldenApi = selectedApiPath;
                } else if (selectedSource==="UserGolden") {
                    selectedCollectionApi = selectedApiPath;
                } else if (selectedSource==="Capture") {
                    selectedCaptureApi = selectedApiPath;
                }
                break;

            case "selectedInstance": // used only in capture
                selectedInstance = value;
                selectedCaptureInstance = selectedInstance;
                break;

            case "startTime": // used only in capture
                startTime = new Date(value).toISOString();
                
                selectedService = ""
                selectedCaptureService = selectedService
                
                selectedApiPath = ""
                selectedCaptureApi = selectedApiPath
                
                selectedInstance = ""
                selectedCaptureInstance = selectedInstance
                
                dispatch(apiCatalogActions.fetchAPIFacets(selectedApp, selectedSource, null, startTime, endTime, selectedService, selectedApiPath));
                break;

            case "endTime": // used only in capture
                endTime = new Date(value).toISOString();
                selectedService = ""
                selectedCaptureService = selectedService
                
                selectedApiPath = ""
                selectedCaptureApi = selectedApiPath
                
                selectedInstance = ""
                selectedCaptureInstance = selectedInstance
                dispatch(apiCatalogActions.fetchAPIFacets(selectedApp, selectedSource, null, startTime, endTime, selectedService, selectedApiPath));
                break;

            default:
                break;
        }

        dispatch({
            type: apiCatalogConstants.SET_FILTER_CHANGE,
            data: { selectedSource, selectedCollection, selectedGolden,
                selectedService,selectedGoldenService, selectedCollectionService,
                selectedCaptureService, selectedApiPath,selectedCaptureApi,
                selectedCollectionApi,selectedGoldenApi, selectedInstance, selectedCaptureInstance, 
                startTime, endTime,apiPaths, instances, services }
        })

        // fetch api trace only if all necessary filters are selected
        switch (selectedSource) {
            case "UserGolden":
                if(!(selectedCollection )) {
                    return;
                }
                break
            case "Golden":
                if(!(selectedGolden)) {
                    return;
                }
                break
            case "Capture":
                if(!(startTime && endTime && selectedService && selectedApiPath)) {
                    return;
                }
                break
        }
        dispatch(apiCatalogActions.fetchAPITrace(selectedSource, selectedCollection, selectedGolden, selectedService, selectedApiPath, selectedInstance, startTime, endTime));
    },

    resetFilters: () => ({ type: apiCatalogConstants.RESET_FILTERS }),

    fetchAPITrace: (selectedSource, selectedCollection, selectedGolden, selectedService, selectedApiPath, selectedInstance, startTime, endTime) => async (dispatch, getState) => {
        const state = getState();
        const { 
            cube: { selectedApp } , 
            apiCatalog: { apiCatalogTableState }, 
            authentication: { user: { customer_name: customerId } }
        } = state;

        // assign collection name param for the request based on source
        let goldenCollection = null;
        switch (selectedSource) {
            case "Capture":
                goldenCollection = null;
                break;
            case "UserGolden":
                goldenCollection = selectedCollection;
                break;
            case "Golden":
                goldenCollection = selectedGolden;
                break;
        }
        

        dispatch({type: apiCatalogConstants.SET_API_TRACE_LOADING})
        const filterData = { 
            ...getDefaultTraceApiFilters(),
            app: selectedApp, startTime, endTime, 
            service: selectedService, apiPath: selectedApiPath, 
            instance: selectedInstance, recordingType: selectedSource,
            collectionName: goldenCollection,
            numResults: apiCatalogTableState.pageSize
        };
        const apiTrace = await cubeService.fetchAPITraceData(customerId, filterData);
        const apiTraces = apiTrace.response;
        const currentEndTime = getLastApiTraceEndTimeFromApiTrace(apiTraces);

        const changedApiCatalogTableState = {
            ...apiCatalogTableState, 
            currentPage: 0,
            filterData:filterData,
            oldPagesData:[{endTime: currentEndTime}],
            totalPages: Math.ceil(apiTrace.numFound / apiCatalogTableState.pageSize)
        };

        dispatch({ type: apiCatalogConstants.FETCH_API_TRACE , data: { apiTrace, apiCatalogTableState: changedApiCatalogTableState } })
    },

    fetchApiTraceByPage : (nextPage) => async(dispatch, getState)=>{
        const {
            apiCatalog:{ apiCatalogTableState },
            authentication: { user: { customer_name: customerId } }
        } = getState();
        let nextFilterData = {...apiCatalogTableState.filterData};
        let nextApiCatalogTableState = {...apiCatalogTableState};
        if(nextPage > apiCatalogTableState.currentPage){
            nextFilterData.endTime = apiCatalogTableState.oldPagesData[apiCatalogTableState.currentPage].endTime;
            nextApiCatalogTableState.currentPage++;           
        }else if (nextPage == 0){
            nextApiCatalogTableState.currentPage = 0;
            nextApiCatalogTableState.oldPagesData = [];
        }
        else if(nextPage < apiCatalogTableState.currentPage){
            nextFilterData.endTime = apiCatalogTableState.oldPagesData[apiCatalogTableState.currentPage - 2].endTime;
            nextApiCatalogTableState.oldPagesData.pop();
            nextApiCatalogTableState.currentPage--;
        }


        dispatch({type: apiCatalogConstants.SET_API_TRACE_LOADING});
        const apiTrace = await cubeService.fetchAPITraceData(customerId, nextFilterData);

        const apiTraces = apiTrace.response;
        if(nextPage > apiCatalogTableState.currentPage || nextPage == 0){
            const endTime = getLastApiTraceEndTimeFromApiTrace(apiTraces);
            nextApiCatalogTableState.oldPagesData.push({endTime});
        }

        nextApiCatalogTableState.currentPage = nextPage;

        dispatch({ type: apiCatalogConstants.FETCH_API_TRACE , data: { apiTrace, apiCatalogTableState: nextApiCatalogTableState } })
    },

    setPageSize : (numResults) => async(dispatch, getState)=>{
        const { 
            apiCatalog: {apiCatalogTableState},
            authentication: { user: { customer_name: customerId } }
        } = getState();
        
        const filterData = {...apiCatalogTableState.filterData,  numResults};

        dispatch({type: apiCatalogConstants.SET_API_TRACE_LOADING});

        const apiTrace = await cubeService.fetchAPITraceData(customerId, filterData);
        const apiTraces = apiTrace.response;
        const currentEndTime = getLastApiTraceEndTimeFromApiTrace(apiTraces);
        
        const changedApiCatalogTableState = {
            ...apiCatalogTableState, 
            currentPage: 0,
            filterData:filterData,
            pageSize: numResults,
            oldPagesData:[{endTime: currentEndTime}],
            totalPages: Math.ceil(apiTrace.numFound / numResults)
        };
        
        dispatch({ type: apiCatalogConstants.FETCH_API_TRACE , data: { apiTrace, apiCatalogTableState: changedApiCatalogTableState } })
    },

    setHttpClientRequestIds: (requestIdMap) => ({type: apiCatalogConstants.SET_HTTP_CLIENT_REQUESTIDS, data: requestIdMap}),

    resetApiCatalogToInitialState: () => ({ type: apiCatalogConstants.RESET_API_CATALOG_TO_INITIAL_STATE })
}