import { Action, Reducer } from "redux";
import { apiCatalogConstants } from "../constants/api-catalog.constants";
import { IApiCatalogState } from "./state.types";

const initialState:IApiCatalogState = {

    diffRequestLeft: {},
    diffRequestRight: {},
    diffResponseLeft: {},
    diffResponseRight: {},

    compareRequests: [],

    apiFacets: {},
    services: [],
    apiPaths: [],
    instances: [],

    apiTrace: {},
    apiTraceLoading: false,
    goldenCollectionLoading: false,

    selectedSource: "",
    selectedCollection: "",
    selectedGolden: "",

    selectedService: "",
    selectedCaptureService: "",
    selectedCollectionService: "",
    selectedGoldenService: "",
    

    selectedApiPath: "",
    selectedCaptureApi: "",
    selectedCollectionApi: "",
    selectedGoldenApi: "",

    selectedInstance: "",
    selectedCaptureInstance: "",

    startTime: new Date(Date.now() - 86400 * 1000).toISOString(),
    endTime: new Date(Date.now()).toISOString(),

    collectionList: [],
    goldenList: [],

    httpClientRequestIds: {},
    apiCatalogTableState:{
        pageSize: 20,
        currentPage: 0,
        filterData:{},
        oldPagesData:[]
    },
    resizedColumns: []
}

export const apiCatalog : Reducer<IApiCatalogState> = (state = initialState, { type, data }) => {
    switch (type) {
        case apiCatalogConstants.SET_DIFF_DATA:
            return {
                ...state,
                diffRequestLeft: data.requestLeft,
                diffRequestRight: data.requestRight,
                diffResponseLeft: data.responseLeft,
                diffResponseRight: data.responseRight,
            }

        case apiCatalogConstants.PIN_COMPARE_REQUEST: {
            return {
                ...state,
                compareRequests: data,
            }
        }

        case apiCatalogConstants.UNPIN_COMPARE_REQUEST: {
            return {
                ...state,
                compareRequests: data,
            }
        }

        case apiCatalogConstants.SET_RESIZED_COLUMNS: {
            return {
                ...state,
                resizedColumns: data,
            }
        }
        
        case apiCatalogConstants.RESET_COMPARE_REQUEST: {
            return {
                ...state,
                compareRequests: [],
            }
        }

        case apiCatalogConstants.FETCH_API_FACETS: {
            return {
                ...state,
                apiFacets: data.apiFacets,
                services: data.services,
                apiPaths: data.apiPaths,
                instances: data.instances,
            }
        }

        case apiCatalogConstants.SET_FILTER_CHANGE: {
            return {
                ...state,
                selectedSource: data.selectedSource,
                selectedCollection: data.selectedCollection,
                selectedGolden: data.selectedGolden,
                selectedService: data.selectedService,
                selectedGoldenService: data.selectedGoldenService,
                selectedCollectionService: data.selectedCollectionService,
                selectedCaptureService: data.selectedCaptureService,
                selectedCaptureInstance: data.selectedCaptureInstance,

                selectedApiPath: data.selectedApiPath,
                selectedCaptureApi: data.selectedCaptureApi,
                selectedCollectionApi: data.selectedCollectionApi,
                selectedGoldenApi: data.selectedGoldenApi,

                selectedInstance: data.selectedInstance,
                startTime: data.startTime,
                endTime: data.endTime,
                apiPaths: data.apiPaths,
                services: data.services,
                instances: data.instances,
            }
        }

        case apiCatalogConstants.FETCHING_GOLDEN_LIST: {
            return {
                ...state,
                goldenCollectionLoading: true,
            }
        }

        case apiCatalogConstants.UPDATE_COLLECTION_LIST: {
            return {
                ...state,
                collectionList: data,
                goldenCollectionLoading: false,
            }
        }

        case apiCatalogConstants.UPDATE_GOLDEN_LIST: {
            return {
                ...state,
                goldenList: data,
                goldenCollectionLoading: false,
            }
        }

        case apiCatalogConstants.GOLDEN_LIST_FETCH_COMPLETE: {
            return {
                ...state,
                goldenCollectionLoading: false,
            }
        }

        case apiCatalogConstants.RESET_FILTERS: {
            return {
                ...state,
                selectedSource: "",
                selectedCollection: "",
                selectedGolden: "",
                selectedService: "",
                selectedApiPath: "",
                selectedInstance: "",
                startTime: new Date(Date.now() - 86400 * 1000).toISOString(),
                endTime: new Date(Date.now()).toISOString(),   
            }
        }

        case apiCatalogConstants.FETCH_API_TRACE: {
            return {
                ...state,
                apiTrace: data.apiTrace,
                apiCatalogTableState: data.apiCatalogTableState,
                apiTraceLoading: false,
            }
        }

        case apiCatalogConstants.SET_API_TRACE_LOADING: {
            return {
                ...state,
                apiTraceLoading: true,
            }
        }

        case apiCatalogConstants.SET_HTTP_CLIENT_REQUESTIDS: {
            return {
                ...state,
                httpClientRequestIds: data,
            }
        }
        case apiCatalogConstants.RESET_API_CATALOG_TO_INITIAL_STATE: {
            return initialState;
        }

        case apiCatalogConstants.MERGE_STATE: {
            return {
                ...state,
                ...data
            }
        }
        default:
            return state;
    }

}