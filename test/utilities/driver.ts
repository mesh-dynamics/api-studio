import * as webdriverio from "webdriverio";
import * as Spectron from "spectron";
import * as testconfig from "./testsconfig";

export interface ICurrentEnv {
  electronApp: Spectron.Application | undefined;
  webApp: webdriverio.BrowserObject | undefined;
  appStart: ()=>void;
  appStop: ()=> void;
  client: () => webdriverio.BrowserObject;
  configData: any;
}

const getWebDriver = async () => {
  const options: webdriverio.RemoteOptions = {
    hostname: "localhost",
    port: 9515,
    capabilities: {
      browserName: "chrome"
    },
    logLevel: "error",
  };

  let client = await webdriverio.remote(options);
  return client;
};
const getElectronApplication = async () => {
  return new Spectron.Application({
    path: "/Applications/Mesh Dynamics.app/Contents/MacOS/Mesh Dynamics",
  });
};

const currentEnv = async (): Promise<ICurrentEnv> => {
  let electronApp : Spectron.Application | undefined = undefined,
    webApp : webdriverio.BrowserObject | undefined = undefined,
    appStart,
    appStop,
    client: () => webdriverio.BrowserObject,
    configData: any;
  if (PLATFORM_ELECTRON) {
    electronApp = await getElectronApplication();
    appStart = async () => await electronApp!.start();
    appStop = async () => await electronApp!.stop();
    client = () => electronApp!.client;
    configData = {...testconfig.electron, 
      password: testconfig.electron.password || process.env.PASSWORD,
      username: testconfig.electron.username || process.env.USERNAME
    };
  } else {
    configData = {...testconfig.dev, 
      password: testconfig.dev.password || process.env.PASSWORD,
      username: testconfig.dev.username || process.env.USERNAME
    };
    webApp = await getWebDriver();
    appStart = async () => await webApp!.url(configData.url);
    appStop = async () => await webApp!.closeWindow();
    client = () => webApp!;
  }
  return { electronApp , webApp, appStart, appStop, client, configData };
};

export { getWebDriver, getElectronApplication, currentEnv };
