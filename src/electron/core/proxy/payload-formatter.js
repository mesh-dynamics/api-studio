const logger = require('electron-log');
const _ = require('lodash');
const url = require("url");

const extractHeadersToCubeFormat = (headersReceived) => {
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


const convertFormParamsToCubeFormat = (requestData) => {
    const formParams = {};
    
    if(requestData) {
        Object.keys(requestData).map(key => formParams[key] = [requestData[key]]);

        return formParams;
    }
    
    return formParams;
};


const extractRequestBodyAndFormParams = (headers, requestData) => {
    const contentType = headers['content-type'] || headers['CONTENT-TYPE'];

    logger.info('Detected content type in intercepted proxied response:', contentType);

    if(contentType && contentType.includes('json')) {
        return {
            body: requestData
        }
    }

    if(contentType && contentType.includes('application/x-www-form-urlencoded')) {
        return {
            formParams: convertFormParamsToCubeFormat(requestData),
        }
    }

    if(contentType && contentType.includes('multipart/form-data')) {
        return {
            formParams: convertFormParamsToCubeFormat(requestData),
        }
    }

    return {
        body: _.isObject(requestData) 
            ? JSON.stringify(requestData) 
            : requestData.toString()
    }
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
    
    const { headers, requestData } = options;

    const { query } = url.parse(apiPath);

    const queryParams = extractQueryStringParamsToCubeFormat(query);

    const { formParams, body } = extractRequestBodyAndFormParams(headers, requestData);

    return  {
        hdrs: extractHeadersToCubeFormat(headers),
        body,
        formParams,
        queryParams,
        path: apiPath,
        method: proxyRes.req.method.toUpperCase(),
        pathSegments: apiPath.split("/").filter(Boolean)
    }
}

const extractAndFormatProxyResponseBody = (headers, responseBody) => {
    
    const contentType = headers['content-type'] || headers['Content-Type'];

    if(contentType.includes('json')) {
        return JSON.parse(responseBody);
    }

    // Add more content type code here when required

    return responseBody;
}

const extractResponsePayloadDetailsFromProxy = (proxyRes, responseBody) => {
    return {
        hdrs: extractHeadersToCubeFormat(proxyRes.headers),
        body: extractAndFormatProxyResponseBody(proxyRes.headers, responseBody),
        status: proxyRes.statusCode,
        statusCode: String(proxyRes.statusMessage),
    }
}

module.exports = {
    extractRequestPayloadDetailsFromProxy,
    extractResponsePayloadDetailsFromProxy
};