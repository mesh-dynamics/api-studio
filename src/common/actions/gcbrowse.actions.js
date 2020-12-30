import { gcbrowseConstants } from '../constants';
import { cubeService } from '../services';
import { cubeActions } from './cube.actions';


const gcbrowseActions = {
    beginFetch: () => ({ type: gcbrowseConstants.REQUEST_BEGIN }),

    fetchSuccess: () => ({ type: gcbrowseConstants.REQUEST_SUCCESS }),

    fetchFailure: (payload) => ({ type: gcbrowseConstants.REQUEST_FAILURE, payload }),

    loadActualGoldens: (payload) => ({ type: gcbrowseConstants.LOAD_GOLDENS, payload}),

    loadUserGoldens: (payload) => ({ type: gcbrowseConstants.LOAD_USER_GOLDENS, payload }),

    clearSelectedGoldenCollection: () => ({ type: gcbrowseConstants.CLEAR_SELECTED_ITEM }),

    updateSelectedGoldenCollection: (payload) => ({ type: gcbrowseConstants.UPDATE_SELECTED_ITEM, payload }),

    fetchGoldensCollections: (selectedSource) => async (dispatch, getState) => {
        const { cube: { selectedApp }, authentication: { user }} = getState();

        dispatch(gcbrowseActions.beginFetch());

        try {
            const results = await cubeService.fetchCollectionList(user, selectedApp, selectedSource);

            dispatch(gcbrowseActions.fetchSuccess());

            switch(selectedSource) {
                case 'UserGolden':
                    dispatch(gcbrowseActions.loadUserGoldens(results));
                case 'Golden':
                    dispatch(gcbrowseActions.loadActualGoldens(results));
                default:
                    // Do not do anything (for now) if it is not the right source type
            }
        } catch (error) {
            dispatch(gcbrowseActions.fetchFailure('Failed to fetch collections from server'));
        }
    },

    deleteGolden: (selectedItemId, selectedSource) => async () => {
        try {
            await cubeService.deleteGolden(selectedItemId);
            dispatch(cubeActions.removeSelectedGoldenFromTestIds(selectedItemId));
            dispatch(gcbrowseActions.fetchGoldensCollections(selectedSource))
        } catch (error) {
            console.log("Error caught in softDelete Golden: " + error);
        }
    }
};

export default gcbrowseActions;

        /* Delete this */
        // deleteGolden = async ()  => {
        //     const { cube, dispatch} = this.props;
        //     try {
        //         await cubeService.deleteGolden(cube.selectedGolden);
        //         dispatch(cubeActions.removeSelectedGoldenFromTestIds(cube.selectedGolden));
        //     } catch (error) {
        //         console.error("Error caught in softDelete Golden: " + error);
        //     }
        //     this.setState({
        //         showDeleteGoldenConfirmation: false,
        //         selectedGoldenFromFilter:"",
        //     });
        // }