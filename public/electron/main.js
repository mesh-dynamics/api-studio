const listeners = require('./listeners');
const serverProxy = require('./proxy');

const proxyServerOptions = {
    target: {
        protocol: 'https:',
        host: 'demo.dev.cubecorp.io',
        port: 443,
    },
    changeOrigin: true,
};

const mockContext = {
    collectionId: 'cf27823b-3463-4ef5-b26a-0ecb1ed33caf',
    traceId: '2d625c9ffcf592f851eb730c6ac898e6',
    spanId: '2d625c9ffcf592f851eb730c6ac898e6',
    recordingId: 'Recording-8618771',
    service: '',
    apiPath: ''
};


serverProxy.setupProxy(proxyServerOptions, mockContext);

listeners.setupListeners(proxyServerOptions, mockContext);