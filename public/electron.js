const listeners = require('./electron/listeners');
const serverProxy = require('./electron/proxy');

const proxyServerOptions = {
    target: {
        protocol: 'https:',
        host: 'demo.dev.cubecorp.io',
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
    collectionId: 'cf27823b-3463-4ef5-b26a-0ecb1ed33caf',
    traceId: '2d625c9ffcf592f851eb730c6ac898e6',
    spanId: '2d625c9ffcf592f851eb730c6ac898e6',
    recordingId: 'Recording-8618771',
};


serverProxy.setupProxy(proxyServerOptions, mockContext, user);

listeners.setupListeners(proxyServerOptions, mockContext, user);