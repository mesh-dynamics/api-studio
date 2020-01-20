import {moviebookConstants} from "../constants";
import {moviebookService} from "../services";

const getMovieList = (keywords) => {
    return async dispatch => {
        try {
            let movieList = await moviebookService.getMovieList(keywords);
            dispatch(success(movieList, Date.now()));
        } catch (error) {
            console.error("Failed to getMovieList", Date.now());
        }
    }

    function success(movieList, date) { return { type: moviebookConstants.LIST_MOVIES, data: movieList, date: date }; }
};

export const moviebookActions = {
    getMovieList
};
