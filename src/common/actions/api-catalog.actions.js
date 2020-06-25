import { cubeService } from "../services"
import { apiCatalogConstants } from "../constants/api-catalog.constants";
import _ from "lodash";

export const apiCatalogActions = {
    getDiffData:  (app, requestIdLeft, requestIdRight) => async (dispatch) => {
        try {
            const data = await cubeService.fetchAPIEventData(app, [requestIdLeft, requestIdRight], [])
            
            if(data.numFound) {
                const [requestLeft, requestRight] = data.objects.filter(obj => obj.eventType==="HTTPRequest");
                const [responseLeft, responseRight] = data.objects.filter(obj => obj.eventType==="HTTPResponse");
                
                dispatch(apiCatalogActions.successDiffData({requestLeft, requestRight, responseLeft, responseRight}));
            }
        } catch (error) {
            // dispatch(failure)
        }
    },        
    
    successDiffData:  (data) => ({type: apiCatalogConstants.SET_DIFF_DATA, data: data}),

    pinCompareRequest: (data) => (dispatch, getState) => {
        let { compareRequests } = getState().apiCatalog;
        if (compareRequests.length != 2) {
            compareRequests.push(data);
        }
        dispatch(apiCatalogActions.setPinCompareRequest(compareRequests));
    },
    setPinCompareRequest: (data) => ({type: apiCatalogConstants.PIN_COMPARE_REQUEST, data: data}),

    unpinCompareRequest: (data) => (dispatch, getState) => {
        let { compareRequests } = getState().apiCatalog;
        _.remove(compareRequests, {parentReqId: data.parentReqId})
        dispatch(apiCatalogActions.setUnPinCompareRequest(compareRequests));
    },

    setUnPinCompareRequest: (data) => ({type: apiCatalogConstants.UNPIN_COMPARE_REQUEST, data: data}),
   
    resetCompareRequest: () => ({type: apiCatalogConstants.RESET_COMPARE_REQUEST}),
}