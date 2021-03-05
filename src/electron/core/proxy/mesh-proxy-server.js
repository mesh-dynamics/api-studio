/**
 * MESH PROXY SERVER CODE
 */
const find = require('find-process');
const URL = require('url');
const axios = require('axios');
const https = require('https');
const logger = require('electron-log');
const AnyProxy = require('anyproxy');
const exec = require('child_process').exec;
const mime = require('mime');
const { getApplicationConfig } = require('../fs-utils');
const { 
    getServiceNameFromUrl, 
    selectProxyTargetForService,
    getServiceConfig
} = require('./proxy-utils');
const {
    getTraceDetails,
    generateTraceKeys
} = require("./trace-utils")
const { storeReqResEvent } = require("./h2server.utility");

/**
 * This function will setup the proxy server
 * @param {*} mockContext 
 * @param {*} user 
 */
const setupMeshProxy = (mockContext, user) => {

    const { proxyDestination, proxyPort, httpsProxyPort } = getApplicationConfig();

    const {
        config: { serviceConfigs },
    } = mockContext;

    const mockTarget = {
        protocol: proxyDestination.protocol, //`${mock.protocol}:`, // Do not forget the darn colon
        host: proxyDestination.host,
        port: proxyDestination.port,
    };

    const traceIdDetailsMap = {};
    const staticMimeTypes = ["gif" , "html" , "css" , "javascript" , "ttf" , "svg" , "png" , "text", "img", "js", "txt", "woff", "woff2", "map", "ico"];

    const getExtension  = (headers, httpMessageType) => {
        if (httpMessageType === "Request") {
            const acceptHeader = headers["Accept"] ? headers["Accept"] : headers["accept"];
            logger.info("Accept Header : ", acceptHeader);
            if(acceptHeader) return mime.getExtension(acceptHeader);
            else return null;
        } else {
            const contentTypeHeader = headers["Content-Type"] ? headers["Content-Type"] : headers["content-type"];
            logger.info("Content-Type Header : ", contentTypeHeader);
            if(contentTypeHeader) return mime.getExtension(contentTypeHeader);
            else return null;
        }
    };

    const getFileExtension = (path) => {
        logger.info("url path : ", path);
        return path.split(".").pop();
    };

    const proxyOptions = {
        port: httpsProxyPort,
        rule: {
            // introduction
            summary: 'Rule summary',
            // intercept before send request to server
            *beforeSendRequest(requestDetail) {
                const newRequestOptions = requestDetail.requestOptions;
                const parsedUrl = URL.parse(requestDetail.url);
                const mimeType = getExtension(newRequestOptions.headers, "Request"),
                    fileExtension = getFileExtension(parsedUrl.pathname);
                logger.info("Request url : ", requestDetail.url);
                logger.info("Response mimeType : ", mimeType);
                logger.info("Response fileExtension : ", fileExtension);
                if(staticMimeTypes.indexOf(mimeType) === -1 && staticMimeTypes.indexOf(fileExtension) === -1) {
                    logger.info("Received HTTP request at proxy: ", 
                    { 
                        "method": newRequestOptions.method, 
                        "path": requestDetail.url, 
                        "headers": newRequestOptions.headers,
                        "body": requestDetail.requestData.toString()
                    });
                    const serviceConfigObject = getServiceConfig(serviceConfigs, parsedUrl.path);
                    logger.info(
                        "Matched service prefix: ",
                        serviceConfigObject
                          ? serviceConfigObject.servicePrefix || serviceConfigObject.service
                          : "(none, mocked)"
                    );
                    const matchedService = serviceConfigObject?.service || "NA";
                    const isLive = serviceConfigObject && !serviceConfigObject.isMocked;
                    logger.info("isLive: ", isLive);
                    const traceDetails = getTraceDetails(mockContext, newRequestOptions.headers);
                    const service = matchedService;
                    logger.info("Matched service: ", matchedService);
                    logger.info("Selected service config object :", serviceConfigObject);
                    const {
                        traceKeys: { traceIdKey, spanIdKey, parentSpanIdKeys },
                        traceIdDetails: { traceId, traceIdForEvent },
                        spanId,
                        parentSpanId,
                    } = traceDetails;

                    if (spanIdKey && !(spanIdKey in newRequestOptions.headers)) {
                        logger.info(`Setting spanId header (${spanIdKey}): `, spanId);
                        newRequestOptions.headers[spanIdKey] = spanId;
                        traceIdDetailsMap[spanId] = traceDetails;
                    }

                    parentSpanIdKeys.forEach((key) => {
                        if (!(key in newRequestOptions.headers)) {
                            logger.info(`Setting parentSpanId header (${key}): `, parentSpanId);
                            newRequestOptions.headers[key] = parentSpanId;
                        }
                    });

                    if (traceIdKey && !(traceIdKey in newRequestOptions.headers)) {
                        logger.info(`Setting traceId header (${traceIdKey}): `, traceId);
                        newRequestOptions.headers[traceIdKey] = traceId;
                    }
                    if(isLive === true) {
                        logger.info("Live service");
                        return {
                            requestOptions: newRequestOptions
                        }
                    } else if(isLive === false) {
                        // mocked
                        logger.info("Mocked service");
                        const { accessToken, tokenType } = user;
                        const token = `${tokenType} ${accessToken}`;
                        newRequestOptions.headers.authorization = token;
                        delete newRequestOptions.headers.Referer;
                        newRequestOptions.headers.Host = mockTarget.host;
                        let axiosUrl;
                        const {
                            runId,
                            selectedApp,
                            collectionId,
                            customerName,
                            recordingCollectionId,
                            strictMock,
                            replayInstance,
                        } = mockContext;
                        const mockApiPrefix = "api/msc/mockWithRunId";
                        axiosUrl = `${mockTarget.protocol}//${mockTarget.host}:${mockTarget.port}/${mockApiPrefix}/${collectionId}/${recordingCollectionId}/${customerName}/${selectedApp}/${traceIdForEvent}/${runId}/${service}/${parsedUrl.path.substring(1)}`;

                        return new Promise((resolve, reject) => {

                            axios({
                                method: newRequestOptions.method,
                                url: axiosUrl,
                                headers: newRequestOptions.headers,
                                ...(["GET", "HEAD"].indexOf(newRequestOptions.method.toUpperCase()) < 0 && {data: requestDetail.requestData}),
                            })
                            .then(function (response) {
                                logger.info("Response from target : ", JSON.stringify(response.data));
                                logger.info("Target response HTTP status: ", response.status);
                                logger.info("Target response HTTP headers: ", response.headers);
                                const localResponse = {
                                    statusCode: response.status,
                                    header: response.headers,
                                    body: JSON.stringify(response.data)
                                };
                                resolve({ response: localResponse });
                            })
                            .catch(function (error) {
                                logger.info("Request url : ", requestDetail.url);
                                logger.info("Response Error from target: ", error.stack);
                                resolve({ response: error });
                            })
                        });
                    } else {
                        return null
                    }
                }
            },
            // deal response before send to client
            *beforeSendResponse(requestDetail, responseDetail) {
                const newResponse = responseDetail.response;
                const parsedUrl = URL.parse(requestDetail.url);
                const mimeType = getExtension(newResponse.header, "Response"),
                    fileExtension = getFileExtension(parsedUrl.pathname);
                logger.info("Request url : ", requestDetail.url);
                logger.info("Response mimeType : ", mimeType);
                logger.info("Response fileExtension : ", fileExtension);
                if(staticMimeTypes.indexOf(mimeType) === -1 && staticMimeTypes.indexOf(fileExtension) === -1) {
                    const serviceConfigObject = getServiceConfig(serviceConfigs, parsedUrl.path);
                    const matchedService = serviceConfigObject?.service || "NA";
                    const isLive = serviceConfigObject && !serviceConfigObject.isMocked;
                    logger.info("isLive: ", isLive);
                    if(isLive !== true) return null;
                    // Only to get trace keys
                    const traceKeys = generateTraceKeys(mockContext.tracer);
                    const service = matchedService;
                    const {spanIdKey} = traceKeys;

                    const spanId = requestDetail.requestOptions.headers[spanIdKey];

                    let responseBody = Buffer.concat(newResponse.rawBody).toString();
                    logger.info("Response from target : ", responseBody);
                    logger.info("Target response HTTP status: ", newResponse.statusCode);
                    logger.info("Target response HTTP headers: ", newResponse.header);

                    // live: store event into collection
                    const responseFields = {
                        body: responseBody,
                        headers: newResponse.header,
                        status: newResponse.statusCode,
                        statusCode: newResponse.statusCode,
                        outgoingApiPath: parsedUrl.path.substring(1), 
                        service: service,
                        method: requestDetail.requestOptions.method
                    };
        
                    const options = {
                        mockContext,
                        user,
                        traceDetails: traceIdDetailsMap[spanId],
                        service: responseFields.service,
                        outgoingApiPath: responseFields.outgoingApiPath,
                        requestData: requestDetail.requestData,
                        headers: requestDetail.requestOptions.headers, //These are request headers, used to form requestEvent
                    };
                    
                    storeReqResEvent(responseFields, options);
                }
                
            }
        },
        throttle: 10000,
        forceProxyHttps: true,
        wsIntercept: false,
        silent: true
    };


    const ifExist = AnyProxy.utils.certMgr.isRootCAFileExists();

    if(ifExist) {
        const proxyServer = new AnyProxy.ProxyServer(proxyOptions);
    
        proxyServer.on('ready', () => {
            logger.info("mitm proxy started")
            // logger.info('proxyServer recorder :', proxyServer.recorder);
        });
        
        proxyServer.on('error', (err) => {
            logger.info("mitm proxy error: ", e);
        });
        

        /**
         * Cleanup and Setup Proxy Listening
         */
        find('port', proxyOptions.port)
            .then((pList) => {
                pList.map((item) => {
                    logger.info('Killing Process...', item.pid);
                    process.kill(item.pid);
                    logger.info('Killed process...', item.pid);
                });
                setTimeout(() => {
                    try {
                        return proxyServer && proxyServer.start();
                    } catch (e) {
                        logger.info("error starting proxy: ", e);
                    }
                }, 3000);
            })
    } else {
        logger.info("mitm proxy didnt start due to lack of root certificate");
    }
};

module.exports = setupMeshProxy;