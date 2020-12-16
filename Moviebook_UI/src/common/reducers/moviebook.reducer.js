import { moviebookConstants } from "../constants";
import movieList from "../routes/Dashboard/test-data";

const defaultCategoryGroup = {
	genre_group_id: 0,
	name: "All",
	categories: [],
};

const initialState = {
	selectedCategoryGroup: defaultCategoryGroup,
	movieList: [],
	categoryList: [],
	categoryGroups: [defaultCategoryGroup],
	createCategoryStatus: { code: "INIT", message: "" },
	editCategoryStatus: { code: "INIT", message: "" },
};

export function moviebook(state = initialState, action) {
	switch (action.type) {
		case moviebookConstants.LIST_MOVIES:
			return {
				...state,
				movieList: action.data,
			};
		case moviebookConstants.UPDATE_SELECTED_CATEGORY:
			return {
				...state,
				selectedCategoryGroup: action.payload,
			};
		case moviebookConstants.LOAD_MOVIES:
			return {
				...state,
				movieList: action.payload,
			};
		case moviebookConstants.LOAD_CATEGORY_GROUPS:
			return {
				...state,
				categoryGroups: [defaultCategoryGroup, ...action.payload],
			};
		case moviebookConstants.LOAD_CATEGORIES:
			return {
				...state,
				categoryList: action.payload,
			};
		case moviebookConstants.CREATE_CATEGORY_RESET:
			return {
				...state,
				createCategoryStatus: { code: "INIT", message: "" },
			};
		case moviebookConstants.CREATE_CATEGORY_SUCCESS:
			return {
				...state,
				createCategoryStatus: {
					code: "SUCCESS",
					message: action.payload,
				},
			};
		case moviebookConstants.CREATE_CATEGORY_FAILURE:
			return {
				...state,
				createCategoryStatus: {
					code: "FAILURE",
					message: action.payload,
				},
			};
		case moviebookConstants.EDIT_CATEGORY_RESET:
			return {
				...state,
				editCategoryStatus: { code: "INIT", message: "" },
			};
		case moviebookConstants.EDIT_CATEGORY_SUCCESS:
			return {
				...state,
				editCategoryStatus: {
					code: "SUCCESS",
					message: action.payload,
				},
			};
		case moviebookConstants.EDIT_CATEGORY_FAILURE:
			return {
				...state,
				editCategoryStatus: {
					code: "FAILURE",
					message: action.payload,
				},
			};
		default:
			return state;
	}
}
