import config from "../config";
import axios from "axios";

// TODO: Remove mocky once confirmed
const getMovieDetails = async (token, keywords) => {
	const url = `${config.apiBaseUrl}/listmovies?filmName=${keywords}`;

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const fetchAllCategoryGroups = async (token) => {
	const url = `${config.apiBaseUrl}/genre-groups`;
	// const url = "https://run.mocky.io/v3/59b51440-bcc0-41b6-a3e5-223336dbd062";

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const fetchMovieListByCategoryGroup = async (token, genreName) => {
	const url = `${config.apiBaseUrl}/getMovieList?genreName=${genreName}`;

	// const url = `https://run.mocky.io/v3/0afce8e2-4f08-4a91-80c1-8b4aead88151?genreName=${genreName}`;

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const fetchMovieList = async (token) => {
	const url = `${config.apiBaseUrl}/getMovieList`;
	// const url = "https://run.mocky.io/v3/0afce8e2-4f08-4a91-80c1-8b4aead88151";

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const fetchAllCategories = async (token) => {
	const url = `${config.apiBaseUrl}/categories'`;
	// const url = "https://run.mocky.io/v3/7d0d59dc-a002-401a-a038-241ed02c8a96";

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const createNewCategory = async (token, newCategoryGroup) => {
	const url = `${config.apiBaseUrl}/genre-group`;
	// const url = "https://run.mocky.io/v3/8daff231-11ee-4105-ab15-5ca0e4578f00";

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.post(url, newCategoryGroup, { headers });
};

const deleteCategoryGroup = async (token, genreGroupId) => {
	const url = `${config.apiBaseUrl}/delete-genre-group/${genreGroupId}`;
	// const url = "https://run.mocky.io/v3/b2624689-2649-4adf-8934-83ab2a66a239";

	const headers = {
		Authorization: `${token}`,
	};

	return await axios.delete(url, { headers });
};

export const moviebookService = {
	fetchMovieListByCategoryGroup,
	fetchAllCategoryGroups,
	deleteCategoryGroup,
	fetchAllCategories,
	createNewCategory,
	getMovieDetails,
	fetchMovieList,
};
