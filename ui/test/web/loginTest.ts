
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

import * as driver from '../utilities/driver'
import * as webdriverio from 'webdriverio';  

describe("Web Window Startup Test", ()=>{
  let currentDriver: driver.ICurrentEnv;
  let appClient :webdriverio.BrowserObject;
  beforeAll(async()=>{
    currentDriver = await driver.currentEnv();   
    appClient = currentDriver.client();
    await currentDriver.appStart();
    
  }); 
  afterAll(async()=>{
    await currentDriver.appStop();
  })
  
  it("Verify Window title", async()=>{

    const title = await appClient.getTitle();
    // Verify the window's title
    expect(title).toBe("Mesh Dynamics");
  });
  it("After successful login, test result page should be visible", async()=>{
   

    await (await appClient.$("[name='username']")).setValue(currentDriver.configData.username);
    await (await appClient.$("[name='password']")).setValue(currentDriver.configData.password);
    await(await appClient.$('.btn-custom-auth')).click();
    await appClient.pause(4000);
    const currentUrl = await appClient.getUrl();
    expect(currentUrl.indexOf("test_results")> -1).toBeTruthy();

  }); 
})