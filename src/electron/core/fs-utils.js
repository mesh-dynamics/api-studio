const fs = require("fs");
const path = require("path");
const yaml = require("js-yaml");
const logger = require("electron-log");
const isDev = require("electron-is-dev");
const Store = require("electron-store");
const url = require("url");

const store = new Store();

const packagedPathPrefix = path.join(__dirname, "../../../../");
const devModePathPrefix = path.join(__dirname, "../../../");
const resourceRootPath = isDev ? devModePathPrefix : packagedPathPrefix;
const DEFAULT_HTTP_PORT = 80;
const SECURE_HTTPS_PORT = 443;

const writeTargetToConfig = (config) => {
    const configFilePath = isDev
      ? path.join(devModePathPrefix, "/config/mesh-dynamics.yml")
      : path.join(packagedPathPrefix, "/config/mesh-dynamics.yml");

    const yamlStr = yaml.safeDump(config);

    fs.writeFile(configFilePath, yamlStr, function (err) {
      if (err) {
        logger.info("Error writing to file", err);
        return;
      }

      logger.info(`File write complete ${configFilePath} :`, yamlStr);
    });
};

const readConfig = () => {
    const configFilePath = isDev
      ? path.join(devModePathPrefix, "/config/mesh-dynamics.yml")
      : path.join(packagedPathPrefix, "/config/mesh-dynamics.yml");

    logger.info("Config file at :", configFilePath);

    try {
      return yaml.safeLoad(fs.readFileSync(configFilePath, "utf8"));
    } catch (error) {
      console.log("Error reading config file : ", error);
    }
};

const setupApplicationConfig = () => {
    const { domain, mock } = readConfig();

    const appDomain = store.get("domain");
    const mockProtocol = store.get("mockProtocol");
    const mockHost = store.get("mockHost");
    const mockPort = store.get("mockPort");
    const proxyPort = store.get("proxyPort");

    // If domain is not set
    if (!appDomain) {
      logger.info("Setting store value for domain as :", domain);
      store.set("domain", domain);
    }

    // If protocol for mock server is not set
    if (!mockProtocol) {
      logger.info("Setting protocol for mock as :", mock.protocol);
      store.set("mockProtocol", mock.protocol);
    }

    // If host for mock server is not defined
    if (!mockHost) {
      logger.info("Setting host for mock as :", mock.host);
      store.set("mockHost", mock.host);
    }

    // If port for mock server is not set
    if (!mockPort) {
      logger.info("Setting port for mock as :", mock.port);
      store.set("mockPort", mock.port);
    }

    // If proxy port is not already set
    if (!proxyPort) {
      logger.info("Setting port for proxy server to listen at:", mock.proxyPort);
      store.set("proxyPort", mock.proxyPort);
    }
};

const getDefaultPorts = (protocol) => {
  if(protocol === 'https:') {
    return SECURE_HTTPS_PORT;
  }

  if(protocol === 'http:') {
    return DEFAULT_HTTP_PORT;
  }

  return SECURE_HTTPS_PORT;
};

const getApplicationConfig = () => {
    logger.info("Reading application config from store");

    const appDomain = store.get("domain");
    const defaultConfiguredMockPort = store.get("mockPort");
    const parsedUrl = url.parse(appDomain);
    // const mockProtocol = store.get("mockProtocol");
    // const mockHost = store.get("mockHost");
    const mockProtocol = parsedUrl.protocol;
    const mockHost = parsedUrl.hostname;
    const mockPort = parsedUrl.port || defaultConfiguredMockPort 
    const proxyPort = store.get("proxyPort");

    const config = {
      domain: appDomain,
      mock: {
        protocol: mockProtocol,
        host: mockHost,
        port: mockPort,
        proxyPort,
      },
    };

    logger.info("Returning config from store :", config);

    return config;
};

const updateApplicationConfig = (config) => {
    // protocol, 
    // host, 
    const { 
      domain, 
      mock: {
        port, // This is mapped to UI text input which is currently not visible 
        proxyPort 
      } 
    } = config;
    
    logger.info("Updating application config to store", domain);

    store.set("domain", domain);
    const parsedUrl = url.parse(domain);
    
    store.set("mockProtocol", parsedUrl.protocol); // read as mockServerProtocol
    store.set("mockHost", parsedUrl.hostname); // read as mockServerHost
    store.set("mockPort", parsedUrl.port || getDefaultPorts(parsedUrl.protocol));
    store.set("proxyPort", proxyPort);

    logger.info("Updated store with latest config");
};

module.exports = {
    updateApplicationConfig,
    setupApplicationConfig,
    getApplicationConfig,
    writeTargetToConfig,
    resourceRootPath,
    readConfig,
    store,
};
