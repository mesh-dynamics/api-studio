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
    collectionId: '',
    recordingId: '',
};


serverProxy.setupProxy(proxyServerOptions, mockContext, user);

listeners.setupListeners(proxyServerOptions, mockContext, user);