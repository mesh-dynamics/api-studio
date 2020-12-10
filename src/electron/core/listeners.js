/**
 * This file is set up proxy and listeners
 */
const { app, BrowserWindow, ipcMain, Menu, dialog } = require('electron');
const { autoUpdater } = require('electron-updater');
autoUpdater.logger = require('electron-log')
const isDev = require('electron-is-dev');
const logger = require('electron-log');
const find = require('find-process');
const path = require('path');
const aws4 = require('aws4');
const os = require('os');
const menu = require('./menu');
const {Base64Binary} = require('../../shared/utils')
const { updateApplicationConfig, getApplicationConfig } = require('./fs-utils');

autoUpdater.autoInstallOnAppQuit = true;
autoUpdater.autoDownload = false; 
autoUpdater.channel = 'latest';

let mainWindow;
let deeplinkingUrl;
let releaseDirectory = '/'; // Default is root of bucket
let downloadInfo = null;
const reqMap = {};

const browserWindowOptions = {
    width: 1280,
    height: 720,
    webPreferences: {
    nodeIntegration: true,
    },
};

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

const awsSigingOptions = {
    service: 's3',
    region: 'us-east-2',
    method: 'GET',
    host:  'meshdynamics-devtool.s3.amazonaws.com',
    path: ''
};

const awsSigningCredentials = {
    accessKeyId: '',
    secretAccessKey: ''
};

const mainWindowIndex = `file://${path.join(__dirname, `../../../dist/index.html`)}`;

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

    app.on('window-all-closed', async function () {
        try {
            const { mock: { proxyPort }} = config;

            const proxyPortList = await find('port', proxyPort);
            
            logger.info(`Processes Listening at port ${proxyPort} : `, proxyPortList);

            proxyPortList.map((item) => {
                logger.info(`Found process active at port ${proxyPort}`, item.pid);
                logger.info('Kill Process...')
                process.kill(item.pid);
            });
    
            if (process.platform !== 'darwin') {
                app.quit();
            }
            
        } catch (error) {
            logger.info('Error Exiting App', error);
        }
    });

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

    ipcMain.on('set_updater_config', (event, updaterConfig) => {
        logger.info('Received Updater Config', );

        const { releaseType, accessKeyId, secretAccessKey } = updaterConfig; // (develop, staging, master, customer)

        releaseDirectory = `/${releaseType}`;

        const metaFilePath = `/${releaseType}/${releaseMetaDataFile(releaseType)}`;

        awsSigingOptions.path = metaFilePath;

        awsSigningCredentials.accessKeyId = accessKeyId;

        awsSigningCredentials.secretAccessKey = secretAccessKey;

        logger.info('Current release directory is set to :', releaseDirectory);

        logger.info('Updated AWS Signing Options \n', awsSigingOptions);

        // Check for updates once the configs are set
        if(!isDev) {
            autoUpdater.checkForUpdates();
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
            config
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
    });

    ipcMain.on('restart_app', () => {
        logger.info('Performing a quit and install');
        autoUpdater.quitAndInstall();
    });

    ipcMain.on('save_target_domain', (event, config) => {
        // The whole config object is required
        logger.info('Received target domain as :', config);

        logger.info('Writing to config file...');

        // utils.writeTargetToConfig(config);
        updateApplicationConfig(config);

        event.sender.send('config_update_success');
        
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
        if(!downloadInfo) {
            mainWindow.webContents.send('error_downloading_update');
        }

        const filePath =  `${releaseDirectory}/${downloadInfo.path}`;

        awsSigingOptions.path = filePath;

        // Sign the headers
        aws4.sign(awsSigingOptions, awsSigningCredentials);

        logger.info("Updater Signing Options \n", awsSigingOptions);

        // Update the headers
        autoUpdater.requestHeaders = awsSigingOptions.headers;
        
        // Trigger Download
        autoUpdater.downloadUpdate();
    });

    ipcMain.on('drive_request_initiate', (event, args) => {
        const { net, session } = require('electron');
        const method = args.method, url = args.url, headers = JSON.parse(args.headers);
        let body = "";
        const isGrpc = args.bodyType == "grpcData";
        if(args.body) body = args.body;
        let fetchedResponseHeaders = {}, responseStatus, responseStatusText, resTimestamp, responseBody = "";
        console.log(JSON.stringify({
            url,
            method,
            headers,
            ...(body && {body})
        }, undefined, 4));
        
        /* 
        session.defaultSession.cookies.get({})
            .then((cookies) => {
                console.log(cookies)
            }).catch((error) => {
                console.log(error)
            });

        const cookie = { url: 'http://www.github.com', name: 'dummy_name', value: 'dummy' };
        session.defaultSession.cookies.set(cookie)
            .then(() => {
                // success
            }, (error) => {
                console.error(error)
            });

        session.defaultSession.cookies.get({ url: 'http://www.github.com' })
            .then((cookies) => {
                console.log(cookies)
            }).catch((error) => {
                console.log(error)
            });
        */

        const request = net.request({
            url: args.url,
            method: method
            // useSessionCookies: true,
            // session: session.defaultSession
        });
        reqMap[args.tabId + args.runId] = request;

        for(let eachHeader in headers) {
            if(eachHeader.toLowerCase() === "content-length") continue;
            request.setHeader(eachHeader, headers[eachHeader]);
        }

        console.log(`Request Initiated`);
        request.on('response', (response) => {
            console.log(`RESPONSE STATUS: ${response.statusCode}`);
            console.log(`RESPONSE HEADERS: ${JSON.stringify(response.headers)}`);

            const resISODate = new Date().toISOString();
            resTimestamp = new Date(resISODate).getTime();
            
            responseStatus = response.statusCode;
            responseStatusText = response.statusMessage;

            for (const header in response.headers) {
                fetchedResponseHeaders[header] = response.headers[header];
            }

            response.on('aborted', (error) => {
                console.log('RESPONSE ABORTED: ', error);
                event.sender.send('request_aborted', true, args.tabId, args.runId);
            });

            response.on('error', (error) => {
                console.log('RESPONSE ERROR: ', error) ;
            });

            response.on('data', (chunk) => {
                responseBody += chunk.toString();
            });

            response.on('end', () => {
                console.log('RESPONSE END');
                event.sender.send('drive_request_completed', args.tabId, args.runId, resTimestamp, fetchedResponseHeaders, responseStatus, responseStatusText, responseBody);
            });
        });

        request.on('finish', (error) => { 
            console.log('Request is Finished: ', error);
        }); 
        request.on('abort', (error) => { 
            console.log('Request is Aborted: ', error);
        }); 
        request.on('error', (error) => {
            console.log('Request ERROR: ', error);
            event.sender.send('drive_request_error', args.tabId, args.runId, error);
        }); 
        request.on('close', (error) => { 
            console.log('Last Transaction has occured: ', error);
            delete reqMap[args.tabId];
        });
        if(method && method.toLowerCase() !== "get" && method.toLowerCase() !== "head") {
            console.log("Request Body Added ",  body);
            // if(isGrpc){
            //     const byteArray = Base64Binary.decode(body);
            //     request.write(byteArray);
            // }else{
                request.write(body);
            // }
        }
        request.end();
    });

    ipcMain.on('request_abort', (event, args) => {
        const netRequest = reqMap[args.tabId + args.runId]; 
        if(netRequest) {
            netRequest.abort();
        } else {
            event.sender.send('request_aborted', false, args.tabId, args.runId);
        }
    });


    /**
     * AUTO Updater events
     */
    autoUpdater.on('checking-for-update', () => {
        logger.info('Update checking triggered');
        
        aws4.sign(awsSigingOptions, awsSigningCredentials);

        logger.info("AWS Header Options", awsSigingOptions);

        autoUpdater.requestHeaders = awsSigingOptions.headers;

        logger.info('Update Check Complete');
    });

    autoUpdater.on('update-available', (info) => {
        logger.info('Update available');
        
        logger.info("\n\n", info);

        downloadInfo = info;

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
