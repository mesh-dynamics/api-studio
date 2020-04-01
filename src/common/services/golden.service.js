import config from '../config';

const fetchGoldenInsights = async (goldenId, service, apiPath, token) => {
    const requestOptions = {
        method: 'GET',
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": `Bearer ${token}`
        }
    };

    try {
        const response = await fetch(`${config.analyzeBaseUrl}/goldenInsights/${goldenId}?service=${service}&apiPath=${apiPath}`, requestOptions);

        if(response.ok) {
            const data = await response.json();

            return data;
        } else {
            throw new Error("Error Fetching Golden Details");
        }
    } catch (error) {
        console.log("Error Caught", error)
    }
    
};

const fetchGoldenMeta = async (recordingId, token) => {
    const requestOptions = {
        method: 'GET',
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": `Bearer ${token}`
        }
    };

    const response = await fetch(`${config.analyzeBaseUrl}/getGoldenMetaData/${recordingId}`, requestOptions);

    if(response.ok) {
        const data = await response.json();

        return data;
    } else {
        throw new Error("Error Fetching Golden Meta");
    }
};

const postGoldenMeta = async (goldenDetails, token) => {
    const { id, goldenName, branchName, codeVersionNumber, commitId } = goldenDetails;
    //userId, golden_comment, tags to be added in later iterations
    const urlencoded = new URLSearchParams();
    urlencoded.append("golden_name", goldenName);
    urlencoded.append("branch", branchName);
    urlencoded.append("git_commit_id", commitId);
    urlencoded.append("code_version", codeVersionNumber);

    const requestOptions = {
        method: 'POST',
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": `Bearer ${token}`
        },
        body: urlencoded
    };

    const response = await fetch(`${config.recordBaseUrl}/updateGoldenFields/${id}`, requestOptions);

    if(response.ok) {
        const data = await response.json();

        return data;
    } else {
        throw new Error("Error Updating Golden Details");
    }
};

export {
    fetchGoldenMeta,
    postGoldenMeta,
    fetchGoldenInsights
};