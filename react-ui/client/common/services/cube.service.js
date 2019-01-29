import config from '../config';

export const cubeService = {
    fetchAppsList,
    getTestIds
};

async function fetchAppsList() {
    let response, json;
    let url = `${config.apiUrl}/api/getApps`;
    let appsList = {};
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "Content-Type": "application/json",
            })
        });
        if (response.ok) {
            json = await response.json();
            appsList = json.apps;
        } else {
            console.log("Response not ok in fetchAppsList", response);
            throw new Error("Response not ok fetchAppsList");
        }
    } catch (e) {
        console.log("fetchAppsList has errors!", e);
        throw e;
    }
    console.log('fetchAppsList success: ', JSON.stringify(appsList, null, 4));
    return appsList;

}

async function getTestIds (options) {
    let response, json;
    try {
        let url = `${config.apiUrl}/api/getTestIds`;
        let dataLen = JSON.stringify(options).length.toString();
        response = await fetch(url, {
            method: "post",
            body: JSON.stringify(options),
            headers: new Headers({
                "Content-Type": "application/json",
                "Content-Length": dataLen
            })
        });
        if (response.ok) {
            json = await response.json();
            console.log(`return JSON: `, JSON.stringify(json));;
            return json;
        } else {
            throw new Error("Response not ok getTestIds");
        }
    } catch (e) {
        throw e;
    }
}
