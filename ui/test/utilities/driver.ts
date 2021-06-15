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
//TODO: based on OS, binary(application) path will get changed. Need to implement for Windows/Linux by detecting current OS environment
const getWebDriver = async () => {
  const options: webdriverio.RemoteOptions = {
    hostname: "localhost",
    port: 9515,
    capabilities: {
      browserName: "chrome",
      "goog:chromeOptions":{        
        binary: "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
      }
    },
    logLevel: "error"
    
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
