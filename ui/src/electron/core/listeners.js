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
 * This file is set up proxy and listeners
 */
const { app, BrowserWindow, ipcMain, Menu, dialog } = require('electron');
const { fetch, AbortController, setup, context,  } = require("fetch-h2");
const { autoUpdater } = require('electron-updater');
const {connect} = require('tls');
const URLParse = require('url-parse');
autoUpdater.logger = require('electron-log')
const isDev = require('electron-is-dev');
const logger = require('electron-log');
const find = require('find-process');
const path = require('path');
const { URLSearchParams } = require('url');
const os = require('os');
const AnyProxy = require('anyproxy');
const exec = require('child_process').exec;
const menu = require('./menu');
const { updateApplicationConfig, getApplicationConfig, store } = require('./fs-utils');
const { clearRestrictedHeaders, Deferred } = require('../../shared/utils');
const {getHttp2FetchUrl} = require('./proxy/h2server.utility')

autoUpdater.autoInstallOnAppQuit = true;
autoUpdater.autoDownload = false; 
autoUpdater.channel = 'latest';

let mainWindow;
let deeplinkingUrl;
let releaseDirectory = '/'; // Default is root of bucket
const reqMap = {};

autoUpdater.updateConfigPath = path.join(__dirname, '../../../dist/auto-updator-config.yml');

const browserWindowOptions = {
    width: 1280,
    height: 720,
    webPreferences: {
    nodeIntegration: true,
    },
};

if(!global.fetchRequest){
    global.fetchRequest = [];
}
if(!global.requestResponse){
    global.requestResponse = [];
}

global.requestApi = {
    push: (name, value)=>{
        global.fetchRequest[name] = value;
    },
    get: (name)=>{
        return global.fetchRequest[name];
    },
    remove:(name)=>{
        delete global.fetchRequest[name];
    },
}
global.responseApi = {
    push: (name, value)=>{
        global.requestResponse[name] = value;
    },
    get: (name)=>{
        return global.requestResponse[name];
    },
    remove: (name)=>{
        delete global.requestResponse[name];
    }
}

const formatMultipartData = (key, param, bodyFormParams)=>{
    switch(param.type){
        case "file":
            if(param && param.value && param.value && param.value.split(',').length > 1){
                var decodedFile = Buffer.from(param.value.split(',')[1], 'base64');
                bodyFormParams.append(key, decodedFile, param.filename);
            }
            break;
        case "field":
        default:
            bodyFormParams.append(key, param.value);    
    }
}

const releaseMetaDataFile = (releaseType) => {
    const platform = os.platform();

    const filePath = {
        win32: 'latest.yml',
        darwin:  'latest-mac.yml',
        linux: 'latest-linux.yml'
        // In Case this is required.
        // win32: `${releaseType}.yml`,
        // darwin: `${releaseType}-mac.yml`,
        // linux: `${releaseType}-linux.yml`
    };

    return filePath[platform];
};


const mainWindowIndex = `file://${path.join(__dirname, `../../../dist/index.html`)}`;

const killProcessByPort = async function (port){
    const processList = await find('port', port);

    logger.info(`Processes running on port ${port} :`, processList);

    processList.map((item) => {
        logger.info('Killing Process...', item.pid);
        process.kill(item.pid);
    });
}

const stopChangedPorts = async function(config){
    const { proxyPort, cubeUIBackendPort, replayDriverPort,  gRPCProxyPort, httpsProxyPort } = config;
    const oldReplayDriverPort = store.get("replayDriverPort");
    const oldCubeUIBackendPort = store.get("cubeUIBackendPort");
    const oldProxyPort = store.get("proxyPort");
    const oldHttpsProxyPort = store.get("httpsProxyPort");
    const oldgRPCProxyPort = store.get("gRPCProxyPort");

    if(oldProxyPort && oldProxyPort.toString() != proxyPort.toString()){
        await killProcessByPort(oldProxyPort);
    }
    if(oldCubeUIBackendPort && oldCubeUIBackendPort.toString() != cubeUIBackendPort.toString()){
        await killProcessByPort(oldCubeUIBackendPort);
    }
    if(oldReplayDriverPort && oldReplayDriverPort.toString() != replayDriverPort.toString()){
        await killProcessByPort(oldReplayDriverPort);
    }
    if(oldHttpsProxyPort && oldHttpsProxyPort.toString() != httpsProxyPort.toString()){
        await killProcessByPort(oldHttpsProxyPort);
    }
    if(oldgRPCProxyPort && oldgRPCProxyPort.toString() != gRPCProxyPort.toString()){
        await killProcessByPort(oldgRPCProxyPort);
    }
}

const setupListeners = (mockContext, user, replayContext) => {
    logger.info('Setup listeners');

    // Loading config
    const config = getApplicationConfig();

    logger.info('Config loaded :', config);

    /**
    * ELECTRON WINDOW AND EVENT LISTENER SETUP
    */

    const createWindow = () => {
        mainWindow = new BrowserWindow(browserWindowOptions);

        mainWindow.loadURL(mainWindowIndex);
        
        mainWindow.on('closed', function () {
            mainWindow = null;
        });

        if (process.platform == 'win32') {
            // Keep only command line / deep linked arguments
            deeplinkingUrl = process.argv.slice(1)
        }

        // Set up menu
        Menu.setApplicationMenu(menu.createMenuTemplate(mainWindow));
    }

    /**
     * App window events
     */
    app.on('ready', async () => {
        logger.info('App is ready... Creating Window');
        
        createWindow();

        logger.info('Window created... Checking for updates...')

        // autoUpdater.checkForUpdatesAndNotify();
        // autoUpdater.checkForUpdates();
    });

    async function closePortsBeforeQuit() {
         
        try {
            const { proxyPort, cubeUIBackendPort, replayDriverPort,  gRPCProxyPort, httpsProxyPort } = config;
            const REPLAY_DRIVER_PORT = replayDriverPort || 9992;
            
            await killProcessByPort(REPLAY_DRIVER_PORT);
            await killProcessByPort(proxyPort);
            await killProcessByPort(cubeUIBackendPort);
            await killProcessByPort(gRPCProxyPort);
            await killProcessByPort(httpsProxyPort);
            if (process.platform !== 'darwin') {
                app.quit();
            }
            
        } catch (error) {
            logger.info('Error Exiting App', error);
        }
    }

    app.on('window-all-closed', async function(){
        await closePortsBeforeQuit();

    }  );
    app.on('before-quit', function(event){
        event.preventDefault();
        setTimeout(async()=>{
            await closePortsBeforeQuit();
        }, 0)
        
    }   );
    

    app.on('activate', function () {
        if (mainWindow === null) {
            createWindow();
        }
    });
    
    if (!app.isDefaultProtocolClient('meshd')) {
        // Define custom protocol handler. Deep linking works on packaged versions of the application!
        app.setAsDefaultProtocolClient('meshd')
    }

    app.on('will-finish-launching', () => {
        // Protocol handler for osx
        app.on('open-url', (event, url) => {
            event.preventDefault();
            deeplinkingUrl = url;
            logger.info('open-url# ' + deeplinkingUrl)
        })
    });


    /**
     * IPC Events
     */
    ipcMain.on('app_version', (event) => {
        logger.info('Sending App version to IPC Renderer');
        event.sender.send('app_version', { version: app.getVersion() });
    });
    
    ipcMain.on('check_for_updates', (event, updaterConfig) => {
        logger.log("Checking for Update event triggered");

        if(!isDev) {
            autoUpdater.checkForUpdatesAndNotify();
        }
    });

    ipcMain.on('set_user', (event, arg) => {
        const { access_token, customer_name, token_type, username } = arg;

        logger.info('Updating user info...\n');
        
        user.accessToken = access_token;
        user.customerName = customer_name;
        user.tokenType = token_type;
        user.userName = username;

        logger.info('Updated user info: \n', user);
    });

    ipcMain.on('mock_context_change', (event, arg) => {
        const { 
            recordingId, collectionId, traceId, selectedApp, 
            customerName, recordingCollectionId, runId, spanId,
            config, tracer, parentSpanId,
        } = arg;

        logger.info('Current mock context :', JSON.stringify(mockContext));
        logger.info('Changing mock context to : ', JSON.stringify(arg));

        mockContext.traceId = traceId;
        mockContext.selectedApp = selectedApp;
        mockContext.customerName = customerName;
        mockContext.collectionId = collectionId;
        mockContext.recordingCollectionId = recordingCollectionId;        
        mockContext.runId = runId;
        mockContext.spanId = spanId;
        mockContext.recordingId = recordingId;
        mockContext.config = config;
        mockContext.tracer = tracer;
        mockContext.parentSpanId = parentSpanId;
        
        logger.info('Updated context is : ', JSON.stringify(mockContext));
    });

    ipcMain.on('reset_context_to_default', (event) => {
        logger.info('Resetting mock context to default')
        
        mockContext.spanId = 'sample-span-id';
        mockContext.traceId = 'sample-trace-id';
        mockContext.selectedApp = 'sample-selected-app';
        mockContext.customerName = 'sample-customer-name';
        mockContext.collectionId = 'sample-collection-id';
        mockContext.recordingCollectionId = 'sample-recording-collection-id';
        mockContext.recordingId ='sample-recording-id';
        mockContext.runId = 'sample-recording-collection-id';
        mockContext.config = {}
        mockContext.tracer = 'meshd'
        mockContext.parentSpanId = 'sample-parent-span-id'
        mockContext.strictMock = false
        mockContext.replayInstance = 'sample-replay-instance'
        mockContext.replayCollection = 'sample-replay-collection'
    });

    ipcMain.on('set_strict_mock', (event, args) => {
        const {strictMock, replayInstance, replayCollection} = args
        logger.info("setting strict mock mode: ", JSON.stringify(args))
        mockContext.strictMock = strictMock
        mockContext.replayInstance = replayInstance
        mockContext.replayCollection = replayCollection
    });

    ipcMain.on('restart_app', () => {
        logger.info('Performing a quit and install');
        autoUpdater.quitAndInstall();
    });

    ipcMain.on('save_target_domain', async(event, config) => {
        // The whole config object is required
        logger.info('Received target domain as :', config);

        logger.info('Writing to config file...');

        await stopChangedPorts(config);

        // utils.writeTargetToConfig(config);
        updateApplicationConfig(config);

        event.sender.send('config_update_success');

        const ifExist = AnyProxy.utils.certMgr.isRootCAFileExists();

        if(!ifExist && config.generateCertificate) {
            AnyProxy.utils.certMgr.generateRootCA((error, keyPath) => {
                // let users to trust this CA before using proxy
                if (!error) {
                    const certDir = require('path').dirname(keyPath);
                    logger.info('The cert is generated at', certDir);
                } else {
                    logger.info('error when generating rootCA', error);
                }
            });
        }
        app.relaunch();
        app.exit();
    });

    ipcMain.on('get_config', (event) => {
        logger.info('Sending Config to IPC Renderer');
        event.sender.send('get_config', config);
    });

    ipcMain.on('return_main_window', (event) => {
        logger.info('Returing to main window');
        mainWindow.loadURL(mainWindowIndex);
    });

    ipcMain.on('download_update', () => {
        // Trigger Download
        autoUpdater.downloadUpdate();
    });

  
    ipcMain.on('drive_request_initiate', (event, args) => {
        const bodyType = args.bodyType;
        const data = global.fetchRequest[args.tabId + args.runId];
        logger.info(`Request received at ipc `,   data.fetchConfigRendered, bodyType);
        const isGrpc = (bodyType == "grpcData")
        let url = args.url;
        if (isGrpc) {
            url = getHttp2FetchUrl(url)
        }

        const abortController = new AbortController();
        reqMap[args.tabId + args.runId] = abortController;
        data.fetchConfigRendered.signal = abortController.signal;
        clearRestrictedHeaders(data.fetchConfigRendered.headers);
        data.fetchConfigRendered.allowForbiddenHeaders = true;
        
        const body = data.fetchConfigRendered.body
        if(body && !(typeof body === 'string' || body instanceof String)) {
            // convert body from js Buffer to nodejs Buffer
            data.fetchConfigRendered.body = Buffer.from(body)
        }

        let responseTrailersPromise;
        if(isGrpc) {
            responseTrailersPromise = new Deferred();
            data.fetchConfigRendered.onTrailers = (trailers) => {
                const trailersObject = trailers.toJSON();
                logger.info("Received response trailers: ", trailersObject);
                responseTrailersPromise.resolve(trailersObject);
            };
        }

        const ctx = context({
            session: {
                rejectUnauthorized: data.fetchConfigRendered.isAllowCertiValidation
            }
        });

        var authorizedPromise = new Deferred();

        const {host, port, protocol} = URLParse(url);
        if(!data.fetchConfigRendered.isAllowCertiValidation && protocol == "https:"){
            // Reference: https://github.com/grantila/fetch-h2/blob/d0c863c9a9d786ce518bbaae9e53adb4599964d1/lib/context-https.ts#L57
            const currentPort = port || (protocol == "https:" ? '443': '80');
            try{

                const socket = connect( parseInt( currentPort, 10 ), host, {},
                ( ) =>{
                    const { authorized, authorizationError } = socket;
                    authorizedPromise.resolve({authorized, authorizationError});
                });
                socket.once( "error", ()=>{
                    authorizedPromise.resolve({authorized: false, authorizationError: socket.authorizationError})
                });
            }catch(error){
                console.error(error);
                authorizedPromise.resolve({authorized: false, authorizationError: "ERROR"});
            }
        }else{
            authorizedPromise.resolve({authorized: true});
        }

        ctx.fetch(url, data.fetchConfigRendered).then(async response => {
            logger.info(`RESPONSE STATUS: ${response.statusCode}`);
            
            let responseTrailers = {};
            if(isGrpc) {
                responseTrailers = await responseTrailersPromise.promise; // wait for trailers to be received
            }

            response.authorized = await authorizedPromise.promise;

            global.requestResponse[args.tabId + args.runId] = response;
            delete global.fetchRequest[args.tabId + args.runId];
            event.sender.send('drive_request_completed', args.tabId, args.runId, JSON.stringify(responseTrailers));
        }).catch(error=>{
            logger.info('Request ERROR: ', error);
            event.sender.send('drive_request_error', args.tabId, args.runId, error);
        });
    });

    ipcMain.on('request_abort', (event, args) => {
        const abortController = reqMap[args.tabId + args.runId];
        if(abortController) {
            abortController.abort();
        }
    });

    ipcMain.on('clear_local_storage_complete', () => {
        logger.info('Clearing local storage success');

        app.relaunch();
        app.exit();
    });


    /**
     * AUTO Updater events
     */

    autoUpdater.on('update-available', (info) => {
        logger.info('Update available');
        
        logger.info("\n\n", info);

        // Notify Renderer Process
        mainWindow.webContents.send('update_available');
    });

    autoUpdater.on('update-not-available', arg => {
        logger.info('Update not available');
        logger.info(arg);
    });

    autoUpdater.on('download-progress', (arg) => {
        logger.info('Download is in progress...');
        logger.info(arg);
        try {
            mainWindow.webContents.send('download_progress', Math.round(arg.percent));
        } catch (error) {
            logger.info('Error while downloading \n', error);
        }
        
    });

    autoUpdater.on('error', error => {
        logger.info('An error has occured::');
        logger.info(error.message);
        logger.info(error.stack);
    });

    autoUpdater.on('update-downloaded', info => {
        logger.info('Update downloaded...')
        mainWindow.webContents.send('update_downloaded');
    });

    process.on('uncaughtException', (err) => {
        const messageBoxOptions = {
            type: "error",
            title: "An exception has occured.",
            message: err.message
        };

        logger.info('Uncaught exception in main thread', err);

        dialog.showMessageBox(messageBoxOptions);
    })
};

module.exports = {
    setupListeners
};
