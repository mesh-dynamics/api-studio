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
    const { domain, proxyPort, replayDriverPort, gRPCProxyPort, httpsProxyPort, generateCertificate, cubeUIBackendPort, redisPort } = readConfig();

    const appDomain = store.get("domain");
    const appProxyPort = store.get("proxyPort");
    const appReplayDriverPort = store.get("replayDriverPort");
    const appCubeUIBackendPort = store.get("cubeUIBackendPort");
    const appRedisPort = store.get("redisPort");
    const appGRPCProxyPort = store.get("gRPCProxyPort");
    const appHttpsProxyPort = store.get("httpsProxyPort");
    const appGenerateCertificate = store.get("generateCertificate");

    // If domain is not set
    if (!appDomain) {
      logger.info("Setting store value for domain as :", domain);
      store.set("domain", domain);
    }

    // If proxy port is not already set
    if (!appProxyPort) {
      logger.info("Setting default port for proxy server to listen at:", proxyPort);
      store.set("proxyPort", proxyPort);
    }

    // If replay driver port is not already set
    if(!appReplayDriverPort) {
      logger.info("Setting default port for local replay driver at:", replayDriverPort);
      store.set("replayDriverPort", replayDriverPort);
    }

    // If backend port is not already set
    if(!appCubeUIBackendPort) {
      logger.info("Setting default port for cube UI backend at:", cubeUIBackendPort);
      store.set("cubeUIBackendPort", cubeUIBackendPort);
    }

    if(!appRedisPort) {
      logger.info("Setting default port for Redis at:", redisPort);
      store.set("redisPort", redisPort);
    }

    if(!appGRPCProxyPort) {
      logger.info("Setting default port for gRPC Proxy Port at:", gRPCProxyPort);
      store.set("gRPCProxyPort", gRPCProxyPort);
    }

    if(!appHttpsProxyPort) {
      logger.info("Setting default port for https Proxy Port at:", httpsProxyPort);
      store.set("httpsProxyPort", httpsProxyPort);
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
    const replayDriverPort =  store.get("replayDriverPort");
    const gRPCProxyPort = store.get("gRPCProxyPort");
    const httpsProxyPort = store.get("httpsProxyPort");
    const cubeUIBackendPort = store.get("cubeUIBackendPort");
    const redisPort = store.get("redisPort");
    const generateCertificate = store.get("generateCertificate");
    const parsedUrl = url.parse(appDomain);

    const proxyDestinationServerProtocol = parsedUrl.protocol;
    const proxyDestinationServerHost = parsedUrl.hostname;
    const proxyDestinationServerPort = parsedUrl.port || getDefaultPorts(parsedUrl.protocol);     

    const config = {
      domain: appDomain,
      proxyPort,
      replayDriverPort,
      gRPCProxyPort,
      cubeUIBackendPort,
      redisPort,
      httpsProxyPort,
      generateCertificate,
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
    const { domain, proxyPort, gRPCProxyPort, httpsProxyPort, generateCertificate, cubeUIBackendPort, redisPort, replayDriverPort } = config;
    
    logger.info("Updating application config domain to :", domain);
    store.set("domain", domain);

    logger.info("Updating application config proxyPort to :", proxyPort);

    store.set("proxyPort", Number(proxyPort));

    logger.info("Updating application config gRPCProxyPort to :", gRPCProxyPort);

    store.set("gRPCProxyPort", Number(gRPCProxyPort));

    logger.info("Updating application config replayDriverPort to :", replayDriverPort);

    store.set("replayDriverPort", Number(replayDriverPort));

    logger.info("Updating application config cubeUIBackendPort to :", cubeUIBackendPort);

    store.set("cubeUIBackendPort", Number(cubeUIBackendPort));

    logger.info("Updating application config redisPort to :", redisPort);

    store.set("redisPort", Number(redisPort));

    logger.info("Updating application config httpsProxyPort to :", httpsProxyPort);

    store.set("httpsProxyPort", Number(httpsProxyPort));

    logger.info("Updating application config generateCertificate to :", generateCertificate);

    store.set("generateCertificate", Boolean(generateCertificate));
    
    logger.info("Config updated with latest input values");
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
