import { moviebookConstants } from "../constants";
import { moviebookService } from "../services";

const moviebookActions = {
	success: (movieList, date) => ({
		type: moviebookConstants.LIST_MOVIES,
		data: movieList,
		date,
	}),

	setDefaultCategory: () => ({
		type: moviebookConstants.SET_DEFAULT_CATEGORY,
	}),

	updateSelectedCategory: (payload) => ({
		type: moviebookConstants.UPDATE_SELECTED_CATEGORY,
		payload,
	}),

	loadMovieList: (payload) => ({
		type: moviebookConstants.LOAD_MOVIES,
		payload,
	}),

	loadAllCategories: (payload) => ({
		type: moviebookConstants.LOAD_CATEGORIES,
		payload,
	}),

	loadAllCategoryGroups: (payload) => ({
		type: moviebookConstants.LOAD_CATEGORY_GROUPS,
		payload,
	}),

	clearCreateCategoryGroupMessage: () => ({
		type: moviebookConstants.CREATE_CATEGORY_RESET,
	}),

	createCategoryGroupSuccess: (payload) => ({
		type: moviebookConstants.CREATE_CATEGORY_SUCCESS,
		payload,
	}),

	createCategoryGroupFailure: (payload) => ({
		type: moviebookConstants.CREATE_CATEGORY_FAILURE,
		payload,
	}),

	clearEditCategoryGroupMessage: () => ({
		type: moviebookConstants.EDIT_CATEGORY_RESET,
	}),
	editCategoryGroupSuccess: (payload) => ({
		type: moviebookConstants.EDIT_CATEGORY_SUCCESS,
		payload,
	}),
	editCategoryGroupFailure: (payload) => ({
		type: moviebookConstants.EDIT_CATEGORY_FAILURE,
		payload,
	}),

	getMovieList: () => async (dispatch, getState) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();

		try {
			const response = await moviebookService.fetchMovieList(token);

			dispatch(moviebookActions.loadMovieList(response.data));
		} catch (error) {
			console.log("Error fetching movielist", error);
		}
	},

	getMovieDetails: (keywords) => async (dispatch, getState) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();
		try {
			const response = await moviebookService.getMovieDetails(
				token,
				keywords
			);
			dispatch(moviebookActions.success(response.data, Date.now()));
		} catch (error) {
			console.error("Failed to getMovieDetails", Date.now());
		}
	},

	getAllCategoryGroups: () => async (dispatch, getState) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();
		try {
			const response = await moviebookService.fetchAllCategoryGroups(
				token
			);

			dispatch(moviebookActions.loadAllCategoryGroups(response.data));
		} catch (error) {
			console.log("Error Fetching Category Groups", error);
		}
	},

	getMovieForSelectedCategoryGroup: (category) => async (
		dispatch,
		getState
	) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();

		const { name } = category;

		try {
			if (name === "All") {
				dispatch(moviebookActions.getMovieList());
			} else {
				const response = await moviebookService.fetchMovieListByCategoryGroup(
					token,
					name
				);

				dispatch(moviebookActions.loadMovieList(response.data));
			}
		} catch (error) {
			console.log("Error fetching movielist by category", error);
		}
	},

	getAllCategories: () => async (dispatch, getState) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();

		try {
			const response = await moviebookService.fetchAllCategories(token);

			dispatch(moviebookActions.loadAllCategories(response.data));
		} catch (error) {
			console.log("Error fetching categories", error);
		}
	},

	deleteCategoryGroup: (genreGroupId) => async (dispatch, getState) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();

		try {
			const response = await moviebookService.deleteCategoryGroup(
				token,
				genreGroupId
			);
			dispatch(moviebookActions.getAllCategoryGroups());
			// dispatch(moviebookActions.loadAllCategories(response.data));
		} catch (error) {
			console.log("Error Deleting Categories", error);
		}
	},

	createNewCategoryGroup: (newCategoryGroup) => async (
		dispatch,
		getState
	) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();

		try {
			dispatch(moviebookActions.clearCreateCategoryGroupMessage());
			await moviebookService.createNewCategory(token, newCategoryGroup);
			dispatch(
				moviebookActions.createCategoryGroupSuccess(
					"Category created successfully."
				)
			);

			dispatch(moviebookActions.getAllCategoryGroups());
			dispatch(moviebookActions.getMovieList());
		} catch (error) {
			console.log("Error creating new category group.", error);
			dispatch(
				moviebookActions.createCategoryGroupFailure(
					"Error creating new category group."
				)
			);
		}
	},

	updateExistingCategoryGroup: (updateCategoryGroup) => async (
		dispatch,
		getState
	) => {
		const {
			authentication: {
				user: { token },
			},
		} = getState();

		try {
			dispatch(moviebookActions.clearEditCategoryGroupMessage());

			await moviebookService.createNewCategory(
				token,
				updateCategoryGroup
			);

			dispatch(
				moviebookActions.editCategoryGroupSuccess(
					"Category updated successfully."
				)
			);

			dispatch(moviebookActions.getAllCategoryGroups());

			dispatch(
				moviebookActions.getMovieForSelectedCategoryGroup(
					updateCategoryGroup
				)
			);

			dispatch(
				moviebookActions.updateSelectedCategory(updateCategoryGroup)
			);

			// dispatch(moviebookActions.getMovieList());
		} catch (error) {
			console.log("Error updating category group.", error);
			dispatch(
				moviebookActions.editCategoryGroupFailure(
					"Error updating category group."
				)
			);
		}
	},
};

export default moviebookActions;
