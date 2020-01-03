import { goldenConstants } from "../constants/golden.constants";

const initialState = {
    selectedService: "",
    selectedApi: "",
    requestContract: {},
    responseContract: {},
    requestExamples: {},
    responseExamples: {}
}

export const golden = (state = initialState, { type, data }) => {
    switch (type) {

    case goldenConstants.SET_SELECTED_SERVICE:
        return { 
            ...state,  
            selectedService: data
        }
    case goldenConstants.SET_SELECTED_API_PATH:
        return {
            ...state,
            selectedApi: data
        }
    case goldenConstants.SET_GOLDEN_CONTRACTS:
        return {
            ...state,
            requestContract: data.request,
            responseContract: data.response
        }
    case goldenConstants.SET_GOLDEN_EXAMPLES:
        return {
            ...state,
            requestExamples: data.request,
            responseExamples: data.response
        };
    default:
        return state
    }
}
