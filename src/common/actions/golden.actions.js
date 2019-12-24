import { goldenConstants } from "../constants/golden.constants";
import { fetchGoldenContract, fetchGoldenExamples } from "../services/golden.service";

const setSelectedService = (data) => ({ type: goldenConstants.SET_SELECTED_SERVICE, data });

const setSelectedApiPath = (data) => ({ type: goldenConstants.SET_SELECTED_API_PATH, data });

const loadGoldenContract = (data) => ({ type: goldenConstants.SET_GOLDEN_CONTRACTS, data });

const loadGoldenExamples = (data) => ({ type: goldenConstants.SET_GOLDEN_EXAMPLES, data });

const getGoldenData = (goldenId, servce, api, selectedPageNumber) => async (dispatch) => {
    const contract = await fetchGoldenContract(goldenId, servce, api);
    const example = await fetchGoldenExamples(goldenId, servce, api, selectedPageNumber);

    dispatch(loadGoldenContract(contract));
    dispatch(loadGoldenExamples(example));
}

export const goldenActions = {
    getGoldenData,
    setSelectedService,
    setSelectedApiPath
};