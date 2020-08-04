import { apiCatalogConstants } from "../constants/api-catalog.constants";

const initialState = {

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
}

export const apiCatalog = (state = initialState, { type, data }) => {
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
                instances: data.instances,
            }
        }

        case apiCatalogConstants.UPDATE_COLLECTION_LIST: {
            return {
                ...state,
                collectionList: data,
            }
        }

        case apiCatalogConstants.UPDATE_GOLDEN_LIST: {
            return {
                ...state,
                goldenList: data,
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
        
        default:
            return state;
    }

}