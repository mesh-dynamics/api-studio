const url = require('url');
const logger = require('electron-log');
const {proxyLiveResponseInterceptor, proxyMockResponseInterceptor} = require('./response-interceptor');
// const { parseMultipart } = require('./multipart-parser');
const { proxyRequestInterceptorMockService, proxyRequestInterceptorLiveService } = require('./request-interceptor');


/**
 * Proxy error event handler
 * @param {*} error Error object caught during the event
 */
const proxyErrorHandler = (error) => {
    logger.info('Error caught in proxy \n');
    logger.info('Error : \n', error);
}


// const readRequestBodyFromBuffer = (buffer, contentType) => {
//     if(contentType && contentType.toLowerCase().includes('multipart/form-data; boundary=')) {
//         return parseMultipart(buffer, contentType);
//     }

//     return buffer;
// };

/**
 * 
 * @param {*} resourceUrl URI which starts with service name and resource path
 */
const getServiceNameFromUrl = (resourceUrl) => {
    logger.info('Calculating service name from request url', resourceUrl);

    try {
        const [serviceName] = resourceUrl.split('/').filter(Boolean);

        return serviceName;
    } catch (error) {
        logger.info('Failed to get service from request url');

        return null;
    }
};

/**
 * 
 * @param {*} serviceConfig  List of service configurations
 * @param {*} url The URL from which to get the config using prefix match
 */
const getServiceConfig = (serviceConfigs, url) => {
    if(url.startsWith("/")) {
        url = url.substring(1)
    }

    if(serviceConfigs && url) {        
        const sortedItems = serviceConfigs.filter(item => {
            const servicePrefix = item.servicePrefix || item.service
            const servicePrefixNormalized = servicePrefix.startsWith('/') ? servicePrefix.substring(1) : servicePrefix
            return url.startsWith(servicePrefixNormalized)
        }).sort((i1, i2) => i2.servicePrefix.length - i1.servicePrefix.length);
        if(sortedItems && sortedItems.length > 0) {
            return sortedItems[0];
        }

    }
    
    return null;
};


/**
 * 
 * @param {*} proxyOptionParameters
 * user,proxy,service,headers,mockContext,requestData ,defaultProxyOptions
 */
const selectProxyTargetForService = (proxyOptionParameters) => {
    const {
        user,
        proxy,
        headers,
        mockContext,
        requestData,
        defaultProxyOptions,
        traceDetails,
        url: inputUrl, 
    } = proxyOptionParameters;

    const { config:  { serviceConfigs } } = mockContext;

    const serviceConfigObject = getServiceConfig(serviceConfigs, inputUrl);
    logger.info("Matched service prefix: ", serviceConfigObject ? (serviceConfigObject.servicePrefix || serviceConfigObject.service) : "(none, mocked)")

    const service = serviceConfigObject?.service || getServiceNameFromUrl(inputUrl)
    logger.info("Matched service: ", service)

    logger.info('Selected service config object :', serviceConfigObject);

    logger.info('Removing Existing Listeners');

    proxy.removeAllListeners();

    // Attach error handler
    proxy.on('error', proxyErrorHandler);

    // Block executes when service is detected and live
    if(serviceConfigObject && !serviceConfigObject.isMocked) {
        const parsedUrl = url.parse(serviceConfigObject.url);

        // Update live server options
        const liveServerProxyOptions = {
            target: {
                protocol: parsedUrl.protocol,
                host: parsedUrl.hostname,
                port: parsedUrl.port || (parsedUrl.protocol === 'https:' ? 443 : 80)
            },
            //selfHandleResponse : true, // setting this flag will require the header/body of the final response to be set manually
            changeOrigin: true,
        };
    
        logger.info(`Service : ${service} configured to be live`);

        const outgoingApiPath = getOutgoingApiPath(serviceConfigObject, inputUrl)
        logger.info('Outgoing API path: ', outgoingApiPath)

        logger.info('Attaching REQUEST INTERCEPTOR for live service');
        proxy.on('proxyReq', (proxyReq) => proxyRequestInterceptorLiveService(proxyReq, serviceConfigObject, mockContext, traceDetails, outgoingApiPath));
    
        logger.info('Attaching RESPONSE INTERCEPTOR for live service');
        proxy.on(
                'proxyRes', 
                (proxyRes, req, res) => 
                    proxyLiveResponseInterceptor(
                        proxyRes, 
                        req, 
                        res, 
                        { 
                            user,
                            service, 
                            headers, 
                            mockContext, 
                            requestData,
                            traceDetails,
                            outgoingApiPath,
                        })
            );
    
        return liveServerProxyOptions;                                                                    
    } 

    logger.info(`Service : ${service} configured to be mocked`);
    logger.info('Attaching REQUEST INTERCEPTOR for config injection');

    // Attach request interceptors to inject additional values for mocking
    proxy.on('proxyReq', (proxyReq) => proxyRequestInterceptorMockService(proxyReq, mockContext, user, traceDetails, service, serviceConfigObject));

    logger.info('Attaching RESPONSE INTERCEPTOR for mocked service');
    proxy.on('proxyRes', (proxyRes, req, res) => 
                proxyMockResponseInterceptor(
                    proxyRes, 
                    req, 
                    res, 
                    { 
                        user,
                        service, 
                        headers, 
                        mockContext, 
                        requestData,
                        traceDetails
                    })
        );

    // and return default options
    return defaultProxyOptions;  

};

const getOutgoingApiPath = (serviceConfigObject, receivedPathInProxy) => {
    const { url: configUrl } = serviceConfigObject;

    const parsedConfigUrl = url.parse(configUrl);

    logger.info('Parsed Config Url ', parsedConfigUrl);

    const outgoingProxyApiPath = stripServiceNameFromOutgoingProxyPath(receivedPathInProxy, serviceConfigObject);

    return outgoingProxyApiPath
}

const stripServiceNameFromOutgoingProxyPath = (apiPath, {service, servicePrefix}) => {
    if(apiPath) {
        if (servicePrefix) {
            return apiPath.slice(1) // remove starting slash
        } else {
            return apiPath.slice(service.length + 2) // accounting for slashes
        }
    }

    return '';
}

module.exports = {
    getServiceNameFromUrl,
    // readRequestBodyFromBuffer,
    selectProxyTargetForService
};