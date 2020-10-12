/**
 * PROXY SERVER CODE
 */
const http = require('http');
const stream = require('stream');
const find = require('find-process');
const httpProxy = require('http-proxy');
const logger = require('electron-log');
const { getApplicationConfig } = require('../fs-utils');
const { 
    getServiceNameFromUrl, 
    selectProxyTargetForService
} = require('./proxy-utils');

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
        res.setHeader('Access-Control-Allow-Origin', '*');
        res.setHeader("Access-Control-Allow-Methods", "GET, PUT, PATCH, POST, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "*");

        let buffer = '';
        // Event handlers for reading request body
        
        // 'data' event on request
        req.on('data', (data) => buffer+=data);

        // 'end' event on request
        req.on('end', () => {

            const { url, method, headers } = req;

            req.body = buffer;

            // readRequestBodyFromBuffer,

            // const contentType = headers['content-type'];

            // const requestBody = readRequestBodyFromBuffer(buffer, contentType);

            //Here we create a new stream with the buffered body on it
            const bufferStream = new stream.PassThrough();
            
            bufferStream.end(new Buffer.from(buffer));
            
            req.bodyStream = bufferStream;
            
            const service = getServiceNameFromUrl(url);

            logger.info(`Request Details at proxy 
                            URL: ${url} 
                            METHOD: ${method} 
                            SERVICE: ${service} 
                            HEADERS: ${JSON.stringify(headers)} 
                            REQUEST BODY: ${buffer}
                        `);

            const proxyOptionParameters = {
                user,
                proxy,
                service,
                headers,
                mockContext,
                requestData: buffer,
                defaultProxyOptions,
            };

            logger.info('Configuring Target...');
    
            const proxyServerOptions = selectProxyTargetForService(proxyOptionParameters);
        
            logger.info('Selected proxy options : \n', proxyServerOptions);
    
            proxy.web(req, res, { ...proxyServerOptions, buffer: bufferStream });    
    
        })

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
        
// logger.info('Received data in request : \n', fields);
// protocol: 'http:',
// host: 'localhost',
// port: 8091
    
        
    
        