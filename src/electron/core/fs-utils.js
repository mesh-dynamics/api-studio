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
    const { domain, proxyPort } = readConfig();

    const appDomain = store.get("domain");
    const appProxyPort = store.get("proxyPort")

    // If domain is not set
    if (!appDomain) {
      logger.info("Setting store value for domain as :", domain);
      store.set("domain", domain);
    }

    // If proxy port is not already set
    if (!proxyPort) {
      logger.info("Setting port for proxy server to listen at:", appProxyPort);
      store.set("proxyPort", appProxyPort);
    }

    // To remove any previous traces of old configuration
    store.delete('mockProtocol');
    store.delete('mockHost');
    store.delete('mockPort');
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
    const proxyPort = store.get("proxyPort");
    const parsedUrl = url.parse(appDomain);

    const proxyDestinationServerProtocol = parsedUrl.protocol;
    const proxyDestinationServerHost = parsedUrl.hostname;
    const proxyDestinationServerPort = parsedUrl.port || getDefaultPorts(parsedUrl.protocol);     

    const config = {
      domain: appDomain,
      proxyPort,
      proxyDestination: {
        protocol: proxyDestinationServerProtocol,
        host: proxyDestinationServerHost,
        port: proxyDestinationServerPort,
      },
    };

    logger.info("Returning config from store :", config);

    return config;
};

const updateApplicationConfig = (config) => {
    const { domain, proxyPort } = config;
    
    logger.info("Updating application config to store", domain);

    store.set("domain", domain);
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

// Leave this here for now just in case 
// its needed to debug
// protocol, 
// host, 
// const parsedUrl = url.parse(domain);
// store.set("mockProtocol", parsedUrl.protocol); // read as mockServerProtocol
// store.set("mockHost", parsedUrl.hostname); // read as mockServerHost
// store.set("mockPort", parsedUrl.port || getDefaultPorts(parsedUrl.protocol));

/**
 * const getApplicationConfig = () => {
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
 */
