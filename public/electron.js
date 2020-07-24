const path = require('path');
const find = require('find-process');
const logger = require('electron-log');
const isDev = require('electron-is-dev');
const listeners = require('./electron/listeners');
const serverProxy = require('./electron/proxy');

const proxyServerOptions = {
    target: {
        protocol: 'https:',
        host: 'demo.prod.cubecorp.io',
        port: 443,
    },
    changeOrigin: true,
};

const user = {
    accessToken: "",
    customerName: "",
    tokenType: "",
    userName: ""
};

const mockContext = {
    traceId: '',
    selectedApp: '',
    customerName: '',
    collectionId: '',
    recordingCollectionId: '',
    runId: '',
};

// recordingId: '', // Do not delete

listeners.setupListeners(proxyServerOptions, mockContext, user);

if(isDev) {
    (async () => {
        logger.info('Dev Mode Detected...');

        const electronBinaryPath = path.join(__dirname, '../node_modules/electron');

        const pList = await find('port', 9000);
                
        logger.info('Processes Listening at port 9000 : ', pList);
    
        pList.map((item) => {
            logger.info('Found process active at port 9000 : ', item.pid);
            logger.info('Kill Process...')
            process.kill(item.pid);
        });
        
        setTimeout(() => serverProxy.setupProxy(proxyServerOptions, mockContext, user), 4000);
    })();
} else {
    serverProxy.setupProxy(proxyServerOptions, mockContext, user);
}