import config from "../config";
import axios from "axios";

const handleResponseLogin = (response) => {
	return response.json().then((json) => {
		const data = json;
		if (!response.ok) {
			if (response.status === 401) {
				// auto logout if 401 response returned from api
				logout();
				// XXX: react complains here
				//location.reload(true);
			}

			const error = (data && data.message) || response.statusText;
			return Promise.reject(error);
		}

		return data;
	});
};

const logout = async () => {};

const validateCredentials = (username, password) => {
	const urlParams = new URLSearchParams();
	urlParams.append("username", username);
	urlParams.append("password", password);

	const requestOptions = {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: urlParams,
	};

	return fetch(`${config.apiBaseUrl}/login`, requestOptions).then(
		handleResponseLogin
	);
};

export { logout, validateCredentials };
