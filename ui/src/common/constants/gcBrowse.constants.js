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

const gcbrowseConstants = {
    REQUEST_BEGIN: "gcBrowse/REQUEST_BEGIN",
    REQUEST_SUCCESS: "gcBrowse/REQUEST_SUCCESS",
    REQUEST_FAILURE: "gcBrowse/REQUEST_FAILURE",

    LOAD_GOLDENS: "gcBrowse/LOAD_GOLDENS",
    LOAD_USER_GOLDENS: "gcBrowse/LOAD_USER_GOLDENS",

    SET_MESSAGE: "gcBrowse/SET_MESSAGE",
    CLEAR_MESSAGE: "gcBrowse/CLEAR_MESSAGE",
};

const defaultCollectionItem = {
    app: '',
    archived: false,
    branch: '',
    codeVersion: '',
    collec: '',
    collectionUpdOpSetId: '',
    comment: '',
    cust: '',
    dynamicInjectionConfigVersion: '',
    gitCommitId: '',
    id: '',
    instance: '',
    jarPath: '',
    label: '',
    name: '',
    prntRcrdngId: '',
    recordingType: '',
    rootRcrdngId: '',
    runId: '',
    status: '',
    tags: [],
    templateUpdOpSetId: '',
    templateVer: '',
    timestmp: 0,
    userId: '',
    apiTraces: []
};

export {
    gcbrowseConstants,
    defaultCollectionItem
}