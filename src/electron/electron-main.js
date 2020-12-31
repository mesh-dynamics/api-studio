const setupProxy = require('./core/proxy/server');
const { setupListeners } = require('./core/listeners');
const { setupApplicationConfig } = require('./core/fs-utils');

/**
 * This will setup the application config into fs for persistence.
 */
setupApplicationConfig();

const user = {
    accessToken: "",
    customerName: "",
    tokenType: "",
    userName: ""
};

const mockContext = {
    spanId: 'sample-span-id',
    traceId: 'sample-trace-id',
    selectedApp: 'sample-selected-app',
    customerName: 'sample-customer-name',
    collectionId: 'sample-collection-id',
    recordingCollectionId: 'sample-recording-collection-id',
    recordingId: 'sample-recording-id',
    runId: 'sample-recording-collection-id',
    config: {},
    tracer = 'meshd',
    parentSpanId = 'sample-parent-span-id',
};

/**
 * Set up auto update, ipc and main window listeners
 */
setupListeners(mockContext, user);

/**
 * Setup server proxy
 */
setupProxy(mockContext, user);

// DO NOT DELETE
// const replayContext = {
//     port: 8090,
// };
// const replayDriver = require('./electron/replay-driver');
// replayDriver.setupReplayDriver(replayContext); Recording-118804835
// name: "sample-config", 
//         serviceConfigs: [
//             { service: "sampleService1", url: "http://localhost:8091", isMocked: false },
//             { service: "sampleService2", url: "http://localhost:8092", isMocked: true }
//         ]