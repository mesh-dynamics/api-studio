import { apiCatalogConstants } from "../constants/api-catalog.constants";

const initialState = {
    
    diffRequestLeft: {},
    diffRequestRight: {},
    diffResponseLeft: {},
    diffResponseRight: {},

    compareRequests: [],

}

export const apiCatalog = (state=initialState, {type, data}) => {
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

        default:
            return state;
    }
    
}