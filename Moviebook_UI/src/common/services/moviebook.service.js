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

const fetchMovieListByCategoryGroup = async (token, genreGroupId) => {
	const url = `${config.apiBaseUrl}/getMovieList?genreGroupId=${genreGroupId}`;

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const fetchMovieList = async (token) => {
	const url = `${config.apiBaseUrl}/getMovieList`;

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const fetchAllCategories = async (token) => {
	const url = `${config.apiBaseUrl}/categories`;

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.get(url, { headers });
};

const createNewCategory = async (token, newCategoryGroup) => {
	const url = `${config.apiBaseUrl}/genre-group`;

	const headers = {
		"Content-Type": "application/json",
		Authorization: `${token}`,
	};

	return await axios.post(url, newCategoryGroup, { headers });
};

const deleteCategoryGroup = async (token, genreGroupId) => {
	const url = `${config.apiBaseUrl}/delete-genre-group/${genreGroupId}`;

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
