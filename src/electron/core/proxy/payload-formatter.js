const logger = require('electron-log');
const _ = require('lodash');
const url = require("url");
const { getParameterCaseInsensitive } = require('../../../shared/utils');

// Specification for request payload state
const PAYLOAD_STATE = {
    WRAPPED_ENCODED: 'WrappedEncoded', // To be used if payload is sent with base64 encoding
    WRAPPED_DECODED: 'WrappedDecoded', // To be used if payload is string. This is the default for live proxy
    UNWRAPPED_DECODED: 'UnwrappedDecoded' // To be used if payload is sent in cube format
};

const convertFormParamsToCubeFormat = (requestDataString) => {
    const formParams = {};
    const fieldParts = requestDataString.split('&');

    fieldParts.map(part => {
        const [fieldName, fieldValue] = part.split('=');
        formParams[fieldName] = [fieldValue];
    })

    return formParams;
};

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
            body: requestData,
            payloadState: PAYLOAD_STATE.WRAPPED_DECODED
        }
    }

    if(contentType && contentType.includes('application/x-www-form-urlencoded')) {
        return {
            formParams: convertFormParamsToCubeFormat(requestData),
            payloadState: PAYLOAD_STATE.UNWRAPPED_DECODED
        }
    }

    if(contentType && contentType && contentType.includes('multipart/form-data')) {

        return {
            body: requestData, // TODO: Handle this in future
            payloadState: PAYLOAD_STATE.WRAPPED_DECODED
        }
    }

    if(contentType && (
            contentType.includes('application/xml') ||
            contentType.includes('text/html') ||
            contentType.includes('text/plain')
            )) {
        return {
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

const extractRequestPayloadDetailsFromProxy = (proxyRes, apiPath, options) => {
    
    const { headers, requestData, mockContext } = options;

    const parsedUrl = url.parse(apiPath);
    const queryParams = extractQueryStringParamsToCubeFormat(parsedUrl.query);

    const { formParams, body, payloadState } = extractRequestBodyAndFormParams(headers, requestData);

    return  {
        hdrs: extractHeadersToCubeFormat(headers, mockContext),
        body,
        formParams,
        queryParams,
        path: parsedUrl.pathname,
        method: proxyRes.req.method.toUpperCase(),
        pathSegments: parsedUrl.pathname.split("/").filter(Boolean),
        payloadState
    }
}



const extractResponsePayloadDetailsFromProxy = (proxyRes, responseBody) => {
    return {
        hdrs: extractHeadersToCubeFormat(proxyRes.headers),
        body: responseBody,
        status: proxyRes.statusCode,
    }
}

module.exports = {
    extractRequestPayloadDetailsFromProxy,
    extractResponsePayloadDetailsFromProxy
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