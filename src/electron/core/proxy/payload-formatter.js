const logger = require('electron-log');
const _ = require('lodash');
const url = require("url");
const queryString = require("querystring");
const { getParameterCaseInsensitive } = require('../../../shared/utils');

// Specification for request payload state
const PAYLOAD_STATE = {
    WRAPPED_ENCODED: 'WrappedEncoded', // To be used if payload is sent with base64 encoding
    WRAPPED_DECODED: 'WrappedDecoded', // To be used if payload is string. This is the default for live proxy
    UNWRAPPED_DECODED: 'UnwrappedDecoded' // To be used if payload is sent in cube format
};

const isgRPC = (responseProps) => responseProps.headers["content-type"] == 'application/grpc';

const convertFormParamsToCubeFormat = (requestDataString) => {
    const formParams = {};
    const fieldParts = requestDataString.split('&');

    fieldParts.map(part => {
        const [fieldName, fieldValue] = part.split('=');
        const value = queryString.unescape(fieldValue);
        if(formParams[fieldName]){
            formParams[fieldName] = [...formParams[fieldName] , value];
        }else{
            formParams[fieldName] = [value];
        }
    })

    return formParams;
};

function getMatching(string, regex) {
    // Helper function when using non-matching groups
    const matches = string.match(regex)
    if (!matches || matches.length < 2) {
      return null
    }
    return matches[1]
}

const formParamParser = (rawData, boundary) =>{
    //Ref: https://medium.com/javascript-in-plain-english/parsing-post-data-3-different-ways-in-node-js-e39d9d11ba8
    let result = { files: [], fields: {}}
        const rawDataArray = rawData.split(boundary)
        for (let item of rawDataArray) {
          // Use non-matching groups to exclude part of the result
          let name = getMatching(item, /(?:name=")(.+?)(?:")/)
          if (!name || !(name = name.trim())) continue
          let value = getMatching(item, /(?:\r\n\r\n)([\S\s]*)(?:\r\n--$)/)
          if (!value) continue
          let filename = getMatching(item, /(?:filename=")(.*?)(?:")/)
          if (filename && (filename = filename.trim())) {
            // Add the file information in a files array
            let file = {}
            file.name = name;
            file.value = value
            file['filename'] = filename
            let contentType = getMatching(item, /(?:Content-Type:)(.*?)(?:\r\n)/)
            if (contentType && (contentType = contentType.trim())) {
              file['Content-Type'] = contentType
            }
            result.files.push(file)
          } else {
            // Key/Value pair
            result.fields[name] = value
          }
        }
        return result;
}

const convertMultipartParamsToCubeFormat = (requestDataString, contentType) => {
    const formParams = {};
    const contentParts = contentType.split(';').map(contentPart => contentPart.trim());
    if(contentParts.length > 1){
        const boundaryParts = contentParts[1].split("=");
        if(boundaryParts.length > 1 && boundaryParts[0] == "boundary") {
            const boundary = boundaryParts[1];
            const parts = formParamParser(requestDataString, boundary);
            Object.entries(parts.fields).forEach(([key, value]) => {
                const fieldData = { 
                        "value": queryString.unescape(value),
                        "type":"field"
                    }; 
                    if(formParams[key]){
                        formParams[key] = [...formParams[key], fieldData];
                    }else{
                        formParams[key] = [fieldData];
                    }
            });
            parts.files.forEach(part => {
                const fieldData = { 
                    "value": part.value,
                    "type":"file",
                    "content-type": part['Content-Type'],
                    "filename": part.filename,
                };
                if(formParams[part.name]){
                    formParams[part.name] = [...formParams[part.name], fieldData];
                }else{
                    formParams[part.name] = [fieldData];
                }
            })
        }
    }

    return formParams;
}

const extractHeadersToCubeFormat = (headersReceived, context) => {
    let headers = {};

    if (_.isArray(headersReceived)) {
        headersReceived.forEach(each => {
            if (each.name && each.value) headers[each.name] = each.value.split(",");
        });
    } else if (_.isObject(headersReceived)) {
        Object.keys(headersReceived).map((eachHeader) => {
            if (eachHeader && headersReceived[eachHeader]) {
                if(_.isArray(headersReceived[eachHeader])) headers[eachHeader] = headersReceived[eachHeader];
                if(_.isString(headersReceived[eachHeader])) headers[eachHeader] = [headersReceived[eachHeader]];
            }
        })
    }

    return headers;
}

const extractRequestBodyAndFormParams = (headers, requestData) => {
    const contentType = getParameterCaseInsensitive(headers, 'content-type');

    logger.info('Detected content type in intercepted proxied request:', contentType);

    if(contentType && contentType.includes('json')) {
        return {
            bodyType: 'rawData',
            body: requestData,
            payloadState: PAYLOAD_STATE.WRAPPED_DECODED
        }
    }

    if(contentType && contentType.includes('application/x-www-form-urlencoded')) {
        return {
            bodyType: 'formData',
            body: convertFormParamsToCubeFormat(requestData),
            payloadState: PAYLOAD_STATE.UNWRAPPED_DECODED
        }
    }

    if(contentType && contentType.includes('multipart/form-data')) {

        return {
            bodyType: 'multipartData',
            body: convertMultipartParamsToCubeFormat(requestData, contentType),
            payloadState: PAYLOAD_STATE.WRAPPED_DECODED
        }
    }

    if(contentType && contentType.includes('application/grpc')) {

        return {
            bodyType: 'grpcData',
            body: requestData, 
            payloadState: PAYLOAD_STATE.WRAPPED_ENCODED
        }
    }

    if(contentType && (
            contentType.includes('application/xml') ||
            contentType.includes('text/html') ||
            contentType.includes('text/plain')
            )) {
        return {
            bodyType: 'rawData',
            body: requestData,
            payloadState: PAYLOAD_STATE.WRAPPED_DECODED
        }
    }

    return {};
}

const extractQueryStringParamsToCubeFormat = (queryString) => {
    const queryParams = {};

    if(queryString) {
        const queryParamArray = queryString.split('&');

        queryParamArray.map(eachKeyValuePair => {
            const [key, value] = eachKeyValuePair.split('=');
            queryParams[key] = [value];
        });
    }

    return queryParams;
};

const extractGrpcMetaData = (options) => {
    let metaData = {}
    if(isgRPC(options)){
        metaData = {
            grpcConnectionSchema: JSON.stringify({
                "app":options.mockContext.selectedApp,
                "service":options.grpcService,
                "method": options.grpcMethod,
                "endpoint":options.grpcEndpoint
            })
        }
    }
    return metaData;
}

const extractRequestPayloadDetailsFromProxy = (responseProps, apiPath, options) => {
    
    const { headers, requestData, mockContext } = options;

    const parsedUrl = url.parse(apiPath);
    const queryParams = extractQueryStringParamsToCubeFormat(parsedUrl.query);

    const { formParams, body, payloadState, bodyType } = extractRequestBodyAndFormParams(headers, requestData);
    const grpcMetadata = extractGrpcMetaData(options);
    return  {
        hdrs: extractHeadersToCubeFormat(headers, mockContext),
        body,
        formParams,
        queryParams,
        path: parsedUrl.pathname,
        method: responseProps.method,
        pathSegments: parsedUrl.pathname.split("/").filter(Boolean),
        metaData: { bodyType,
            ...grpcMetadata },
        payloadState
    }
}



const extractResponsePayloadDetailsFromProxy = (responseProps, responseBody) => {
    
    const additionalProps = isgRPC(responseProps) ? { path: responseProps.outgoingApiPath, trls: extractHeadersToCubeFormat(responseProps.trailers) } : {};
    return {
        hdrs: extractHeadersToCubeFormat(responseProps.headers),
        body: responseBody,
        status: responseProps.statusCode,
        method: responseProps.method,
        payloadState: PAYLOAD_STATE.WRAPPED_ENCODED,
        ...additionalProps
    }
}

module.exports = {
    extractRequestPayloadDetailsFromProxy,
    extractResponsePayloadDetailsFromProxy,
    PAYLOAD_STATE
};


// Not To be deleted
// const convertMultiPartToCubeFormat = (requestDataString) => {
//     const formParams = {};
//     const fieldParts = requestDataString.split('&');
    
//     fieldParts.map(part => {
//         const [fieldPart, content] = part.split('#content=');
//         const [fieldName, fileName] = fieldPart.split('=');

//         formParams[fieldName] = [fileName, content].filter(Boolean);
//     })

//     return formParams;
// };

// const extractAndFormatProxyResponseBody = (headers, responseBody) => {
    
// const contentType = headers['content-type'] || headers['Content-Type'];


// if(contentType.includes('json')) {
//     return JSON.parse(responseBody);
// }

// Add more content type code here when required

// return responseBody;
// }

// Not To be deleted
// if(contentType && contentType.includes('application/x-www-form-urlencoded')) {
//     return {
//         formParams: convertFormParamsToCubeFormat(requestData)
//     }
// }

// if(contentType && contentType.includes('multipart/form-data')) {
//     return {
//         formParams: convertMultiPartToCubeFormat(requestData)
//     }
// }