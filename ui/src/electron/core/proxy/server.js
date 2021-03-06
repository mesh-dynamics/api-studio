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
    selectProxyTargetForService,
} = require('./proxy-utils');
const {
    getTraceDetails
} = require("./trace-utils")


/**
 * This function will setup the proxy server
 * @param {*} mockContext 
 * @param {*} user 
 */
const setupProxy = (mockContext, user) => {

    const { proxyDestination, proxyPort } = getApplicationConfig();

    const defaultProxyOptions = {
        target: {
            protocol: proxyDestination.protocol, //`${mock.protocol}:`, // Do not forget the darn colon
            host: proxyDestination.host,
            port: proxyDestination.port,
        },
        changeOrigin: true,
    };
    

    const server = http.createServer((req, res) => {
        const proxy = httpProxy.createProxyServer({});

        res.setHeader('Access-Control-Allow-Origin', '*');
        res.setHeader("Access-Control-Allow-Methods", "GET, PUT, PATCH, POST, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "*");

        let buffer = [];
        // Event handlers for reading request body
        
        // 'data' event on request
        req.on('data', (chunk) => buffer.push(chunk));

        // 'end' event on request
        req.on('end', () => {

            const { url, method, headers } = req;
            const bufferData = Buffer.concat(buffer);
            req.body = bufferData;

            // readRequestBodyFromBuffer,

            // const contentType = headers['content-type'];

            // const requestBody = readRequestBodyFromBuffer(buffer, contentType);

            //Here we create a new stream with the buffered body on it
            const bufferStream = new stream.PassThrough();
            
            bufferStream.end(bufferData);
            
            req.bodyStream = bufferStream;
            
            //const service = getServiceNameFromUrl(url);

            logger.info(`Request Details at proxy 
                            URL: ${url} 
                            METHOD: ${method}  
                            HEADERS: ${JSON.stringify(headers, undefined, 4)} 
                            REQUEST BODY: ${buffer}
                        `);

            logger.info('Removing restricted headers at proxy server');

            delete headers['connection'];
            delete headers['date'];
            delete headers['expect'];
            delete headers['from'];
            delete headers['referer'];
            delete headers['upgrade'];
            delete headers['via'];
            delete headers['warning'];
            delete headers['transfer-encoding'];
            delete headers['accept-encoding'];

            logger.info('Logging request headers after removing restricted headers', JSON.stringify(headers, undefined, 4));

            // if traceId & spanId isn't present in the mock context, 
            // check in the incoming request headers
            // if not present in the headers, generate new values
           const traceDetails = getTraceDetails(mockContext, headers);
            const proxyOptionParameters = {
                user,
                proxy,
                headers,
                mockContext,
                requestData: bufferData.toString(),
                defaultProxyOptions,
                traceDetails,
                url,
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
    find('port', proxyPort)
        .then((pList) => {
            pList.map((item) => {
                logger.info('Killing Process...', item.pid);
                process.kill(item.pid);
                logger.info('Killed process...', item.pid);
            });
            setTimeout(() => server.listen(proxyPort), 3000);
        })
};

module.exports = setupProxy;
        
// logger.info('Received data in request : \n', fields);
// protocol: 'http:',
// host: 'localhost',
// port: 8091
    
        
    
        