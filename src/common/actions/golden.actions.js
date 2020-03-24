import { goldenConstants } from "../constants/golden.constants";
import {
    transformResponseContractToTable,
    transformResponseContractToJson,
    transformRequestContract
} from "../utils/generator/insights-table-generator";
import { fetchGoldenInsights, postGoldenMeta } from "../services/golden.service";

const setMessage = (data) => ({ type: goldenConstants.SET_MESSAGE, data });

const clearMessage = () => ({ type: goldenConstants.RESET_MESSAGE });

const beginFetch = () => ({ type: goldenConstants.BEGIN_FETCH });

const fetchComplete = () => ({ type: goldenConstants.FETCH_COMPLETE });

const resetServiceAndApiPath = () => ({ type: goldenConstants.RESET_GOLDEN_API_PATH_AND_SERVICE });

const setSelectedService = (data) => ({ type: goldenConstants.SET_SELECTED_SERVICE, data });

const setSelectedApiPath = (data) => ({ type: goldenConstants.SET_SELECTED_API_PATH, data });

const loadGoldenContract = (data) => ({ type: goldenConstants.SET_GOLDEN_CONTRACTS, data });

const loadGoldenExamples = (data) => ({ type: goldenConstants.SET_GOLDEN_EXAMPLES, data });

const loadSelectedGolden = (data) => ({ type: goldenConstants.SET_SELECTED_GOLDEN, data });

const setSelectedGolden = (data) => (dispatch) => {
    const { collec, testIds } = data;
    
    const selectedGolden = testIds.find(items => items.collec === collec);

    dispatch(loadSelectedGolden(selectedGolden));
};

const getGoldenData = (goldenId, service, apiPath) => async (dispatch, getState) => {
    const { user: { access_token }} = getState().authentication;

    try {

        dispatch(beginFetch());

        const insights = await fetchGoldenInsights(goldenId, service, apiPath, access_token);

        dispatch(fetchComplete());

        const { request, responseCompareRules, response, requestCompareRules, requestMatchRules} = insights;

        const parsedResponse = response ? JSON.parse(response) : null;

        const requestExample = request ? JSON.parse(request) : "";

        const responseExample =  parsedResponse ? parsedResponse.body: "";

        // needs to have two keys { matchRules, compareRules } 
        // for component to consume

        // rules <compare and match> need to have the following structure 
        // { headers, queryParams, formParams, body}
        const requestContract = {
            matchRules: transformRequestContract(JSON.parse(requestMatchRules)),
            compareRules: transformRequestContract(JSON.parse(requestCompareRules))
        };

        const responseContract = {
            body: {
                // will return transformed array or empty array
                table: transformResponseContractToTable(JSON.parse(responseCompareRules)), 
                // will return transformed json or empty json
                json:  transformResponseContractToJson(JSON.parse(responseCompareRules))
            }
        };

        dispatch(loadGoldenContract({ request: requestContract, response: responseContract }));

        dispatch(loadGoldenExamples({ request: requestExample, response: responseExample }));

    } catch(e) {
        console.log("Failed to fetch golden insights");
        
        dispatch(fetchComplete());

        dispatch(loadGoldenContract({ request: null, response: null }));

        dispatch(loadGoldenExamples({ request: null, response: null }));
    }
}

const updateGoldenMeta = (data) => async (dispatch, getState) => {
    const { user: { access_token }} = getState().authentication;

    try {
        // Clear any previous messages
        dispatch(clearMessage());

        // Send update request to server
        const updatedGoldenDetails = await postGoldenMeta(data, access_token);

        // Load the latest results
        dispatch(loadSelectedGolden(updatedGoldenDetails));
        
        // Convey to user that details have been updated
        dispatch(setMessage("Details Updated Successfully"));

        // Clear the message after 3 seconds
        setTimeout(() => dispatch(clearMessage()), 3000);
    } catch (e) {
        // In case of error
        dispatch(setMessage("Failed to update golden info. Please try again later"));

        // Clear the message after 3 seconds
        setTimeout(() => dispatch(clearMessage()), 8000);
    }
};

export const goldenActions = {
    getGoldenData,
    updateGoldenMeta,
    setSelectedGolden,
    setSelectedService,
    setSelectedApiPath,
    resetServiceAndApiPath
};