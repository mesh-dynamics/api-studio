/**
 * PROXY SERVER CODE
 */
const http = require('http');
const formidable = require('formidable');
const find = require('find-process');
const httpProxy = require('http-proxy');
const logger = require('electron-log');
const { getApplicationConfig } = require('../fs-utils');
const { getServiceNameFromUrl, selectProxyTargetForService } = require('./proxy-utils');

/**
 * This function will setup the proxy server
 * @param {*} mockContext 
 * @param {*} user 
 */
const setupProxy = (mockContext, user) => {

    const { mock } = getApplicationConfig();

    const defaultProxyOptions = {
        target: {
            protocol: mock.protocol, //`${mock.protocol}:`, // Do not forget the darn colon
            host: mock.host,
            port: mock.port,
        },
        changeOrigin: true,
    };
    
    const proxy = httpProxy.createProxyServer({});

    const server = http.createServer((req, res) => {

        logger.info('Request URL received at proxy server :', req.url);

        logger.info('Request Method received at proxy server :', req.method);

        const service = getServiceNameFromUrl(req.url);

        const headers = req.headers;

        // const form = formidable({ multiples: true });

        logger.info('Headers Received :', headers);

        logger.info('Detected Service :', service);
        
        // logger.info('Received data in request : \n', fields);
    
        const proxyOptionParameters = {
            user,
            proxy,
            service,
            headers,
            mockContext,
            requestData: fields,
            defaultProxyOptions
        };
    
        logger.info('Configuring Target...');
    
        const proxyServerOptions = selectProxyTargetForService(proxyOptionParameters);
        
        logger.info('Selected proxy options : \n', proxyServerOptions);
    
        proxy.web(req, res, proxyServerOptions);
    })

    /**
     * Cleanup and Setup Proxy Listening
     */
    find('port', mock.proxyPort)
        .then((pList) => {
            pList.map((item) => {
                logger.info('Killing Process...', item.pid);
                process.kill(item.pid);
                logger.info('Killed process...', item.pid);
            });
            setTimeout(() => server.listen(mock.proxyPort), 3000);
        })
};

module.exports = setupProxy;

// form.parse(req, (err, fields) => {
//     if(err) {
//         logger.info('Error parsing body',  err);
//         throw err;
//     }

//     logger.info('Headers Received :', headers);

//     logger.info('Detected Service :', service);
    
//     logger.info('Received data in request : \n', fields);

//     const proxyOptionParameters = {
//         user,
//         proxy,
//         service,
//         headers,
//         mockContext,
//         requestData: fields,
//         defaultProxyOptions
//     };

//     logger.info('Configuring Target...');

//     const proxyServerOptions = selectProxyTargetForService(proxyOptionParameters);
    
//     logger.info('Selected proxy options : \n', proxyServerOptions);

//     proxy.web(req, res, proxyServerOptions);
// });