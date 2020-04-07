import ReduceDiff from '../ReduceDiff';
import generator from '../generator/json-path-generator';
import sortJson from "../sort-json";
import _ from 'lodash';
import config from "../../config";

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

const validateAndCleanHTTPMessageParts = (messagePart) => {
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

const getDiffForMessagePart = (replayedPart, recordedPart, serverSideDiff, prefix, service, path, app, replayId, recordingId, templateVersion) => {
    if (!serverSideDiff || serverSideDiff.length === 0) return null; 
    let actpart = JSON.stringify(replayedPart, undefined, 4);
    let expPart = JSON.stringify(recordedPart, undefined, 4);
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
            recordingId: recordingId
        }
    });
    return updatedReductedDiffArrayMsgPart;
}

const validateAndCreateDiffLayoutData = (replayList, app, replayId, recordingId, templateVersion, collapseLength) => {
    let diffLayoutData = replayList.map((item) => {
        let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders, prefix = "/body",
            recordedRequestHeaders, replayedRequestHeaders, recordedRequestQParams, replayedRequestQParams, recordedRequestFParams, replayedRequestFParams,recordedRequestBody, replayedRequestBody, reductedDiffArrayReqHeaders, reductedDiffArrayReqBody, reductedDiffArrayReqQParams, reductedDiffArrayReqFParams;
        let isJson = true;
        
        // add eventype to each diff object
        if (item.respCompDiff) {
            item.respCompDiff.forEach(diff => {
                diff.eventType = "Response";
            })
        }
        if (item.reqCompDiff) {
            item.reqCompDiff.forEach(diff => {
                diff.eventType = "Request";
            })
        }

        // processing Response    
        // recorded response body and headers
        if (item.recordResponse) {
            recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
            // check if the content type is JSON and attempt to parse it
            let recordedResponseMime = recordedResponseHeaders["content-type"] ? recordedResponseHeaders["content-type"][0] : "";
            isJson = recordedResponseMime.toLowerCase().indexOf("json") > -1;
            if (item.recordResponse.body && isJson) {
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
            let replayedResponseMime = replayedResponseHeaders["content-type"] ? replayedResponseHeaders["content-type"][0] : "";
            isJson = replayedResponseMime.toLowerCase().indexOf("json") > -1;
            if (item.replayResponse.body && isJson) {
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

        let actRespHdrJSON = JSON.stringify(replayedResponseHeaders, undefined, 4);
        let expRespHdrJSON = JSON.stringify(recordedResponseHeaders, undefined, 4);
        

        // use the backend diff and the two JSONs to generate diff array that will be passed to the diff renderer
        if (diff && diff.length > 0) {
            // skip calculating the diff array in case of non json data 
            // pass diffArray as null so that the diff library can render it directly
            if (isJson) { 
                let reduceDiff = new ReduceDiff(prefix, actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
            }
            let expJSONPaths = generator(recordedData, "", "", prefix);
            missedRequiredFields = diff.filter((eachItem) => {
                return eachItem.op === "noop" && eachItem.resolution.indexOf("ERR_REQUIRED") > -1 && !expJSONPaths.has(eachItem.path);
            })

            let reduceDiffHdr = new ReduceDiff("/hdrs", actRespHdrJSON, expRespHdrJSON, diff);
            reducedDiffArrayRespHdr = reduceDiffHdr.computeDiffArray();

        } else if (diff && diff.length == 0) {
            if (_.isEqual(expJSON, actJSON)) {
                let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
            }
        }
        let updatedReductedDiffArray = reductedDiffArray && reductedDiffArray.map((eachItem) => {
            return {
                ...eachItem,
                recordReqId: item.recordReqId,
                replayReqId: item.replayReqId,
                service: item.service,
                app: app,
                templateVersion: templateVersion,
                apiPath: item.path,
                replayId: replayId,
                recordingId: recordingId
            }
        });

        let updatedReductedDiffArrayWithCollapsible = addCompressToggleData(updatedReductedDiffArray, collapseLength);

        let updatedReducedDiffArrayRespHdr = reducedDiffArrayRespHdr && reducedDiffArrayRespHdr.map((eachItem) => {
            return {
                ...eachItem,
                service: item.service,
                app: app,
                templateVersion: templateVersion,
                apiPath: item.path,
                replayId: replayId,
                recordingId: recordingId
            }
        });

        // process Requests
        // recorded request header and body
        // parse and clean up body string
        if (item.recordRequest) {
            recordedRequestHeaders = validateAndCleanHTTPMessageParts(item.recordRequest.hdrs);
            recordedRequestBody = validateAndCleanHTTPMessageParts(item.recordRequest.body);
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
            replayedRequestBody = validateAndCleanHTTPMessageParts(item.replayRequest.body);
            replayedRequestQParams = validateAndCleanHTTPMessageParts(item.replayRequest.queryParams);
            replayedRequestFParams = validateAndCleanHTTPMessageParts(item.replayRequest.formParams);
        } else {
            replayedRequestHeaders = "";
            replayedRequestBody = "";
            replayedRequestQParams = "";
            replayedRequestFParams = "";
        }

        reductedDiffArrayReqHeaders = getDiffForMessagePart(replayedRequestHeaders, recordedRequestHeaders, item.reqCompDiff, "/hdrs", item.service, item.path, app, replayId, recordingId, templateVersion);
        reductedDiffArrayReqQParams = getDiffForMessagePart(replayedRequestQParams, recordedRequestQParams, item.reqCompDiff, "/queryParams", item.service, item.path, app, replayId, recordingId, templateVersion);
        reductedDiffArrayReqFParams = getDiffForMessagePart(replayedRequestFParams, recordedRequestFParams, item.reqCompDiff, "/queryParams", item.service, item.path, app, replayId, recordingId, templateVersion);
        reductedDiffArrayReqBody = getDiffForMessagePart(replayedRequestBody, recordedRequestBody, item.reqCompDiff, "/body", item.service, item.path, app, replayId, recordingId, templateVersion);

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

const addCompressToggleData = (diffData, collapseLength, diffCollapseStartIndex) => {
    let indx  = 0, atleastADiff = false;;
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
    let toggleDrawChunk  = false;
    for (let eachChunk of diffData) {
        if(eachChunk.collapseChunk === true && toggleDrawChunk === false) {
            toggleDrawChunk = true;
            eachChunk["drawChunk"] = true;
        } else if(eachChunk.collapseChunk === true && toggleDrawChunk === true) {
            eachChunk["drawChunk"] = false;
        } else if(eachChunk.collapseChunk === false) {
            toggleDrawChunk = false;
            eachChunk["drawChunk"] = false;
        }
    }
    return diffData;
}

const roughSizeOfObject = (object) => {

    var objectList = [];
    var stack = [ object ];
    var bytes = 0;

    while ( stack.length ) {
        var value = stack.pop();

        if ( typeof value === 'boolean' ) {
            bytes += 4;
        }
        else if ( typeof value === 'string' ) {
            bytes += value.length * 2;
        }
        else if ( typeof value === 'number' ) {
            bytes += 8;
        }
        else if
        (
            typeof value === 'object'
            && objectList.indexOf( value ) === -1
        )
        {
            objectList.push( value );

            for( var i in value ) {
                stack.push( value[ i ] );
            }
        }
    }
    return bytes;
}

const pruneResults = (diffLayoutData, fromBeginning) => {
    let accumulatedObjectSize = 0;
    const diffObjectSizeThreshold = config.diffObjectSizeThreshold;
    const maxDiffResultsPerPage = config.maxDiffResultsPerPage;
    let len = diffLayoutData.length;
    let i;
    if (fromBeginning) { // prune from top of the list
        i = 0;
        while (accumulatedObjectSize <= diffObjectSizeThreshold && i < len && i < maxDiffResultsPerPage) {
            accumulatedObjectSize += roughSizeOfObject(diffLayoutData[i]);
            i++;
        }
        let diffLayoutDataPruned = diffLayoutData.slice(0, i)
        return {diffLayoutDataPruned, i};
    } else { // prune from bottom of the list
        i = 0;
        while (accumulatedObjectSize <= diffObjectSizeThreshold && i < len && i < maxDiffResultsPerPage) {
            accumulatedObjectSize += roughSizeOfObject(diffLayoutData[len-i-1]);
            i++;
        }
        let diffLayoutDataPruned = diffLayoutData.slice(len - i, len);
        return {diffLayoutDataPruned, i} 
    }
}

export {
    validateAndCreateDiffLayoutData, 
    addCompressToggleData,
    pruneResults
}