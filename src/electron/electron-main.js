// const find = require('find-process');
// const logger = require('electron-log');
const { setupProxy } = require('./core/proxy');
const { setupListeners } = require('./core/listeners');
// const replayDriver = require('./electron/replay-driver');
const { setupApplicationConfig } = require('./core/fs-utils');

/**
 * This will setup the application config into
 * fs for persistence.
 */
setupApplicationConfig();

const user = {
    accessToken: "",
    customerName: "",
    tokenType: "",
    userName: ""
};

const mockContext = {
    traceId: 'sample-trace-id',
    selectedApp: 'sample-selected-app',
    customerName: 'sample-customer-name',
    collectionId: 'sample-collection-id',
    recordingCollectionId: 'sample-recording-collection-id',
    runId: 'sample-recording-collection-id'
};

// const { mock: { proxyPort } } = getApplicationConfig();

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

// replayDriver.setupReplayDriver(replayContext);