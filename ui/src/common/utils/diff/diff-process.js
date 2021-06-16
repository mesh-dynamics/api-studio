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

// import sortJson from 'sort-json';
import ReduceDiff from '../ReduceDiff';
import generator from '../generator/json-path-generator';
import sortJson from "../sort-json";
import _ from 'lodash';
import config from "../../config";
import { getParameterCaseInsensitive, isJsonOrGrpcMime } from '../../../shared/utils';

const cleanEscapedString = (str) => {
    // preserve newlines, etc - use valid JSON
    str = str.replace(/\\n/g, "\\n")
        .replace(/\\'/g, "\\'")
        .replace(/\\"/g, '\\"')
        .replace(/\\&/g, "\\&")
        .replace(/\\r/g, "\\r")
        .replace(/\\t/g, "\\t")
        .replace(/\\b/g, "\\b")
        .replace(/\\f/g, "\\f");
    // remove non-printable and other non-valid JSON chars
    str = str.replace(/[\u0000-\u0019]+/g, "");
    return str;
}

const validateAndCleanHTTPMessageParts = (messagePart, headers) => {
    if(headers) {
        let contentType = getParameterCaseInsensitive(headers, "content-type");
        let contentTypeString = contentType ? (_.isArray(contentType) ? contentType[0] : contentType) : "",
            isMultipart = contentTypeString?.toLowerCase().indexOf("multipart") > -1;
        if(isMultipart) {
            return messagePart;
        }
    }
    let cleanedMessagepart = "";
    if (messagePart &&_.isObject(messagePart)) {
        cleanedMessagepart = messagePart;
    } else if (messagePart) {
        try {
            cleanedMessagepart = JSON.parse(messagePart);
        } catch (e) {
            cleanedMessagepart = JSON.parse('"' + cleanEscapedString(_.escape(messagePart)) + '"')
        }
    } else {
        cleanedMessagepart = messagePart || JSON.parse('""');
    }

    return cleanedMessagepart;
}

const getDiffForMessagePart = (replayedPart, recordedPart, serverSideDiff, prefix, service, path, app, replayId, recordingId, templateVersion, eventType, setPaths) => {
    if (!serverSideDiff) return null; 
    let actpart = JSON.stringify(sortJson(replayedPart), undefined, 4);
    let expPart = JSON.stringify(sortJson(recordedPart), undefined, 4);
    let reducedDiffArrayMsgPart = new ReduceDiff(prefix, actpart, expPart, serverSideDiff);
    let reductedDiffArrayMsgPart = reducedDiffArrayMsgPart.computeDiffArray()
    let updatedReductedDiffArrayMsgPart = reductedDiffArrayMsgPart && reductedDiffArrayMsgPart.map((eachItem) => {
        return {
            ...eachItem,
            service,
            app: app,
            templateVersion: templateVersion,
            apiPath: path,
            replayId: replayId,
            recordingId: recordingId,
            eventType: eventType,
        }
    });
    const updatedDiffArrayMsgPartProcessedSets = processSetsInDiff(updatedReductedDiffArrayMsgPart, setPaths)
    return updatedDiffArrayMsgPartProcessedSets;
}

const validateAndCreateDiffLayoutData = (replayList, app, replayId, recordingId, templateVersion, collapseLength, maxLinesLength) => {
    let diffLayoutData = replayList.map((item) => {
        let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders, prefix = "/body",
            recordedRequestHeaders, replayedRequestHeaders, recordedRequestQParams, replayedRequestQParams, recordedRequestFParams, replayedRequestFParams,recordedRequestBody, replayedRequestBody, reductedDiffArrayReqHeaders, reductedDiffArrayReqBody, reductedDiffArrayReqQParams, reductedDiffArrayReqFParams;
        let isJsonOrGrpc = true;

        // processing Response    
        // recorded response body and headers
        if (item.recordResponse) {
            recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
            // check if the content type is JSON and attempt to parse it
            let recordedResponseContentType = getParameterCaseInsensitive(recordedResponseHeaders, "content-type");
            let recordedResponseMime = recordedResponseContentType ? (_.isArray(recordedResponseContentType) ? recordedResponseContentType[0] : recordedResponseContentType) : "";
            isJsonOrGrpc = isJsonOrGrpcMime(recordedResponseMime);

            if (_.isString(item.recordResponse.body) && item.recordResponse.body && isJsonOrGrpc > -1) {
                try {
                    recordedData = JSON.parse(item.recordResponse.body);
                } catch (e) {
                    recordedData = JSON.parse('"' + cleanEscapedString(_.escape(item.recordResponse.body)) + '"')
                }
            }
            else {
                // in case the content type isn't json, display the entire body if present, or else an empty string
                recordedData = item.recordResponse.body ? item.recordResponse.body : '""';
            }
        } else {
            recordedResponseHeaders = "";
            recordedData = "";
        }   

        // same as above but for replayed response
        if (item.replayResponse) {
            replayedResponseHeaders = item.replayResponse.hdrs ? item.replayResponse.hdrs : [];
            // check if the content type is JSON and attempt to parse it
            let replayedResponseContentType = getParameterCaseInsensitive(replayedResponseHeaders, "content-type");
            let replayedResponseMime = replayedResponseContentType ? (_.isArray(replayedResponseContentType) ? replayedResponseContentType[0] : replayedResponseContentType) : "";
            isJsonOrGrpc = isJsonOrGrpcMime(replayedResponseMime);
            if (_.isString(item.replayResponse.body) && item.replayResponse.body && isJsonOrGrpc > -1) {
                try {
                    replayedData = JSON.parse(item.replayResponse.body);
                } catch (e) {
                    replayedData = JSON.parse('"' + cleanEscapedString(_.escape(item.replayResponse.body)) + '"')
                }
            }
            else {
                // in case the content type isn't json, display the entire body if present, or else an empty string
                replayedData = item.replayResponse.body ? item.replayResponse.body : '""';
            }
        } else {
            replayedResponseHeaders = "";
            replayedData = "";
        }
        let diff;
        
        if (item.respCompDiff && item.respCompDiff.length !== 0) {
            diff = item.respCompDiff;
        } else {
            diff = [];
        }
        let actJSON = JSON.stringify(sortJson(replayedData), undefined, 4),
            expJSON = JSON.stringify(sortJson(recordedData), undefined, 4);
        let reductedDiffArray = null, missedRequiredFields = [], reducedDiffArrayRespHdr = null;

        let actRespHdrJSON = JSON.stringify(sortJson(replayedResponseHeaders), undefined, 4);
        let expRespHdrJSON = JSON.stringify(sortJson(recordedResponseHeaders), undefined, 4);
        

        // use the backend diff and the two JSONs to generate diff array that will be passed to the diff renderer
        if (diff && diff.length > 0) {
            // skip calculating the diff array in case of non json data 
            // pass diffArray as null so that the diff library can render it directly
            if (isJsonOrGrpc) { 
                let reduceDiff = new ReduceDiff(prefix, actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
            }
            let expJSONPaths = generator(recordedData, "", "", prefix);
            missedRequiredFields = diff
            .filter(
                (eachItem) => (
                    eachItem.op === "noop" 
                    && eachItem.resolution.includes("ERR_Required") 
                    && !expJSONPaths.has(eachItem.path)
                    )
                )
            .map(filteredFields => {
                if(item.respCompDiff.find(eachDiffItem => eachDiffItem.path === filteredFields.path)) {
                    return {
                        ...filteredFields,
                        eventType: "Response"
                    }
                }

                return {
                    ...filteredFields,
                    eventType: "Request"
                }
            });

            let reduceDiffHdr = new ReduceDiff("/hdrs", actRespHdrJSON, expRespHdrJSON, diff);
            reducedDiffArrayRespHdr = reduceDiffHdr.computeDiffArray();

        } else if (diff && diff.length == 0) {
            if (_.isEqual(expJSON, actJSON)) {
                let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
                let reduceDiffHdr = new ReduceDiff("/hdrs", actRespHdrJSON, expRespHdrJSON, diff);
                reducedDiffArrayRespHdr = reduceDiffHdr.computeDiffArray();
            }
        }
        let updatedReductedDiffArray = reductedDiffArray && reductedDiffArray.map((eachItem) => {
            if(eachItem.jsonPath) 
            return {
                ...eachItem,
                recordReqId: item.recordReqId,
                replayReqId: item.replayReqId,
                service: item.service,
                app: app,
                templateVersion: templateVersion,
                apiPath: item.path,
                replayId: replayId,
                recordingId: recordingId,
                eventType: "Response",
            }
        });

        let updatedDiffArrayProcessedSets = processSetsInDiff(updatedReductedDiffArray, item.respSetPaths)

        let updatedReductedDiffArrayWithCollapsible = addCompressToggleData(updatedDiffArrayProcessedSets, collapseLength, maxLinesLength);
        
        let updatedReducedDiffArrayRespHdr = reducedDiffArrayRespHdr && reducedDiffArrayRespHdr.map((eachItem) => {
            return {
                ...eachItem,
                service: item.service,
                app: app,
                templateVersion: templateVersion,
                apiPath: item.path,
                replayId: replayId,
                recordingId: recordingId,
                eventType: "Response",
            }
        });

        // process Requests
        // recorded request header and body
        // parse and clean up body string
        if (item.recordRequest) {
            recordedRequestHeaders = validateAndCleanHTTPMessageParts(item.recordRequest.hdrs);
            recordedRequestBody = validateAndCleanHTTPMessageParts(item.recordRequest.body, item.recordRequest.hdrs);
            recordedRequestQParams = validateAndCleanHTTPMessageParts(item.recordRequest.queryParams);
            recordedRequestFParams = validateAndCleanHTTPMessageParts(item.recordRequest.formParams);
        } else {
            recordedRequestHeaders = "";
            recordedRequestBody = "";
            recordedRequestQParams = "";
            recordedRequestFParams = "";
        }

        // replayed request header and body
        // same as above
        if (item.replayRequest) {
            replayedRequestHeaders = validateAndCleanHTTPMessageParts(item.replayRequest.hdrs);
            replayedRequestBody = validateAndCleanHTTPMessageParts(item.replayRequest.body, item.replayRequest.hdrs);
            replayedRequestQParams = validateAndCleanHTTPMessageParts(item.replayRequest.queryParams);
            replayedRequestFParams = validateAndCleanHTTPMessageParts(item.replayRequest.formParams);
        } else {
            replayedRequestHeaders = "";
            replayedRequestBody = "";
            replayedRequestQParams = "";
            replayedRequestFParams = "";
        }

        const reqEventType = "Request";
        reductedDiffArrayReqHeaders = getDiffForMessagePart(replayedRequestHeaders, recordedRequestHeaders, item.reqCompDiff, "/hdrs", item.service, item.path, app, replayId, recordingId, templateVersion, reqEventType, item.reqSetPaths);
        reductedDiffArrayReqQParams = getDiffForMessagePart(replayedRequestQParams, recordedRequestQParams, item.reqCompDiff, "/queryParams", item.service, item.path, app, replayId, recordingId, templateVersion, reqEventType, item.reqSetPaths);
        reductedDiffArrayReqFParams = getDiffForMessagePart(replayedRequestFParams, recordedRequestFParams, item.reqCompDiff, "/queryParams", item.service, item.path, app, replayId, recordingId, templateVersion, reqEventType, item.reqSetPaths);
        reductedDiffArrayReqBody = getDiffForMessagePart(replayedRequestBody, recordedRequestBody, item.reqCompDiff, "/body", item.service, item.path, app, replayId, recordingId, templateVersion, reqEventType, item.reqSetPaths);

        return {
            ...item,
            recordedResponseHeaders,
            replayedResponseHeaders,
            recordedData,
            replayedData,
            actJSON,
            expJSON,
            parsedDiff: diff,
            reductedDiffArray: updatedReductedDiffArrayWithCollapsible,
            missedRequiredFields,
            show: true,
            recordedRequestHeaders,
            replayedRequestHeaders,
            recordedRequestQParams,
            replayedRequestQParams,
            recordedRequestFParams,
            replayedRequestFParams,
            recordedRequestBody,
            replayedRequestBody,
            updatedReducedDiffArrayRespHdr,
            reductedDiffArrayReqHeaders,
            reductedDiffArrayReqQParams,
            reductedDiffArrayReqFParams,
            reductedDiffArrayReqBody
        }
    });
    return diffLayoutData;
}

const addCompressToggleData = (diffData, collapseLength, maxLinesLength, diffCollapseStartIndex) => {
    let indx  = 0, atleastADiff = false;
    if(!diffData) return diffData;
    for (let i = config.diffCollapseStartIndex; i < diffData.length; i++) {
        let diffDataChunk = diffData[i];
        if(diffDataChunk.serverSideDiff !== null || (diffDataChunk.added || diffDataChunk.removed)) {
            let j = i - 1, chunkTopLength = 0;
            diffDataChunk["collapseChunk"] = false;
            atleastADiff = true;
            while (diffData[j] && diffData[j].serverSideDiff === null && chunkTopLength < collapseLength) {
                diffData[j]["collapseChunk"] = false;
                chunkTopLength++;
                j--;
            }
            let k = i + 1, chunkBottomLength = 0;
            while (diffData[k] && diffData[k].serverSideDiff === null && chunkBottomLength < collapseLength) {
                diffData[k]["collapseChunk"] = false;
                chunkBottomLength++;
                k++;
            }
        } else {
            if(!diffDataChunk.hasOwnProperty("collapseChunk")) diffDataChunk["collapseChunk"] = true;
        }
    }
    if(!atleastADiff) {
        for (let m = 0; m < collapseLength; m++) {
            let tempDiffDataChunk = diffData[m];
            if(tempDiffDataChunk) tempDiffDataChunk["collapseChunk"] = false;
            if(m >= diffData.length) break;
        }
    }
    let toggleDrawChunk  = false, arbitratryCount = 0;
    let jsonPath, previousChunk, showMaxChunkToggle = false, arrayCount = 0, activatedCount;
    for (let eachChunk of diffData) {
        eachChunk["showMaxChunk"] = false;
        eachChunk["showMaxChunkToggle"] = false;
        if(arbitratryCount >= maxLinesLength && !showMaxChunkToggle) {
            eachChunk["showMaxChunk"] = true;
            showMaxChunkToggle = true;
            activatedCount = arrayCount;
        }
        if(showMaxChunkToggle) {
            eachChunk["showMaxChunkToggle"] = true;
        }
        if(jsonPath === eachChunk.jsonPath && showMaxChunkToggle && activatedCount === arrayCount) {
            previousChunk["showMaxChunk"] = true;
        }
        if(eachChunk.collapseChunk === true && toggleDrawChunk === false) {
            toggleDrawChunk = true;
            eachChunk["drawChunk"] = true;
            arbitratryCount++;
        } else if(eachChunk.collapseChunk === true && toggleDrawChunk === true) {
            eachChunk["drawChunk"] = false;
        } else if(eachChunk.collapseChunk === false) {
            toggleDrawChunk = false;
            eachChunk["drawChunk"] = false;
            if(jsonPath !== eachChunk.jsonPath) {
                arbitratryCount++;
            }
        } else if (!eachChunk.collapseChunk) {
            arbitratryCount++;
        }
        jsonPath = eachChunk.jsonPath;
        previousChunk = eachChunk;
        arrayCount++;
    }
    return diffData;
}

const updateResolutionFilterPaths = (diffLayoutData) => {
    // const selectedResolutionType = this.state.filter.selectedResolutionType;
    const selectedResolutionType = "All";
    diffLayoutData &&
      diffLayoutData.forEach((item) => {
        item.filterPaths = [];
        for (let jsonPathParsedDiff of item.parsedDiff) {
          // add path to the filter list if the resolution is All or matches the current selected one,
          // and if the selected type is 'All Errors' it is an error type
          if (
            selectedResolutionType === "All" ||
            selectedResolutionType === jsonPathParsedDiff.resolution ||
            (selectedResolutionType === "ERR" && jsonPathParsedDiff.resolution.indexOf("ERR_") > -1)
          ) {
            // add only the json paths we want to show in the diff
            let path = jsonPathParsedDiff.path;
            item.filterPaths.push(path);
          }
        }
      });
  };

const processSetsInDiff = (reductedDiffArray, setPaths) => {
    // convert the paths to match into regex
    // sort by length before conversion to match the longest path
    const setPathRegexList = setPaths?.sort((a, b) => (b.length - a.length))
                                        .map(setPath => (new RegExp(`${setPath.replace("*", ".*?")}`))) || []
    return reductedDiffArray.filter(item => item?.jsonPath)
        .map(item => {
            let value = item.value
            const jsonPath = item.jsonPath.replace("<BEGIN>", "")
            for(const setPathRegex of setPathRegexList) {
                const matches = jsonPath.match(setPathRegex)
                if(matches) {
                    const restPath = jsonPath.substr(matches[0].length).split("/")
                    if(restPath.length == 1){
                        // no suffix in path, i.e. start of set
                        // change '{' to '['
                        let i = value.lastIndexOf("{")
                        if(i > -1) {
                            value = value.substr(0, i) + "[" + value.substr(i + 1)
                        } else {
                            i = value.lastIndexOf("}")
                            if(i > -1) {
                                value = value.substr(0, i) + "]" + value.substr(i + 1)
                            } else{
                                console.error("could not find begin or end of the set")
                            }
                        }
                    } else {
                        if(restPath.length == 2) {
                            // start of object in set
                            // drop the key

                            value = value.replace(/\".*\":\s?/, "")
                        }
                    }
                    break // matched the longest prefix
                }
            }
            return {
                ...item, value
            }
        })
}
export {
    validateAndCreateDiffLayoutData, 
    addCompressToggleData,
    updateResolutionFilterPaths,
    processSetsInDiff,
}