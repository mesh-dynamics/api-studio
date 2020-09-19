const url = require('url');
const logger = require('electron-log');
const proxyResponseInterceptor = require('./response-interceptor');
const { proxyRequestInterceptorMockService, proxyRequestInterceptorLiveService } = require('./request-interceptor');


/**
 * Proxy error event handler
 * @param {*} error Error object caught during the event
 */
const proxyErrorHandler = (error) => {
    logger.info('Error caught in proxy \n');
    logger.info('Error : \n', error);
}


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
 * @param {*} service The name service for which configuration needs to be looked up
 */
const getServiceConfig = (serviceConfigs, service) => {
    
    if(serviceConfigs && service) {        
        return serviceConfigs.find(item => item.service === service);
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
        service, 
        mockContext,
        requestData,
        defaultProxyOptions, 
    } = proxyOptionParameters;

    const { config:  { serviceConfigs } } = mockContext;

    const serviceConfigObject = getServiceConfig(serviceConfigs, service); // serviceConfigs.find(item => item.service === service);

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
            selfHandleResponse : true,
            changeOrigin: true,
        };
    
        logger.info(`Service : ${service} configured to be live`);
        
        logger.info('Attaching REQUEST INTERCEPTOR for live service');
        proxy.on('proxyReq', (proxyReq) => proxyRequestInterceptorLiveService(proxyReq, serviceConfigObject));
    
        logger.info('Attaching RESPONSE INTERCEPTOR for live service');
        proxy.on(
                'proxyRes', 
                (proxyRes, req, res) => 
                    proxyResponseInterceptor(
                        proxyRes, 
                        req, 
                        res, 
                        { 
                            user,
                            service, 
                            headers, 
                            mockContext, 
                            requestData
                        })
            );
    
        return liveServerProxyOptions;                                                                    
    } 

    logger.info(`Service : ${service} configured to be mocked`);
    logger.info('Attaching REQUEST INTERCEPTOR for config injection');

    // Attach request interceptors to inject additional values for mocking
    proxy.on('proxyReq', (proxyReq) => proxyRequestInterceptorMockService(proxyReq, mockContext, user));

    // and return default options
    return defaultProxyOptions;  

};

module.exports = {
    getServiceNameFromUrl,
    selectProxyTargetForService
};