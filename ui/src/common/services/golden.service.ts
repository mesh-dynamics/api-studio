/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import config from '../config';
import api from '../api';

// Overriding default Content-Type in the calls below
const fetchGoldenInsights = async (goldenId: string, service: string, apiPath: string) => {
    const requestOptions = {
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        }
    };

    try {
        return await api.get(`${config.analyzeBaseUrl}/goldenInsights/${goldenId}?service=${service}&apiPath=${apiPath}`, requestOptions);
    } catch (error) {
        console.log("Error Fetching Golden Insights\n", error)
        throw new Error("Error Fetching Golden Insights");
    }
    
};

const fetchGoldenMeta = async (recordingId: string) => {
    const requestOptions = {
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        }
    };

    try {
        return await api.get(`${config.analyzeBaseUrl}/getGoldenMetaData/${recordingId}`, requestOptions);
    } catch(e) {
        console.log("Error Fetching Golden Meta:\n", e)
        throw new Error("Error Fetching Golden Meta");
    }
};

const postGoldenMeta = async (goldenDetails: any) => {
    const { id, goldenName, labelName, branchName, codeVersionNumber, commitId } = goldenDetails;
    //userId, golden_comment, tags to be added in later iterations
    const urlencoded = new URLSearchParams();
    urlencoded.append("golden_name", goldenName);
    urlencoded.append("label", labelName),
    urlencoded.append("branch", branchName);
    urlencoded.append("git_commit_id", commitId);
    urlencoded.append("code_version", codeVersionNumber);

    const requestOptions = {
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        }
    };

    try {
        return await api.post(`${config.recordBaseUrl}/updateGoldenFields/${id}`, urlencoded, requestOptions);
    } catch (error) {
        console.log("Error updating test suite", error);
        throw new Error("Error Updating test suite Details");
    }
};

const updateGoldenName = async (id: string, goldenName: string ) => {
    const urlencoded = new URLSearchParams();
    urlencoded.append("golden_name", goldenName);

    const requestOptions = {
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        }
    };

    try {
        return await api.post(`${config.recordBaseUrl}/updateGoldenFields/${id}`, urlencoded, requestOptions);
    } catch (error) {
        console.log("Error updating test suite", error);
        throw new Error("Error updating test suite details");
    }
};

export {
    fetchGoldenMeta,
    postGoldenMeta,
    updateGoldenName,
    fetchGoldenInsights
};