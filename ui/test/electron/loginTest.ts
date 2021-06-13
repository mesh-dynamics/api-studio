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

import * as Spectron from "spectron";
import * as assert from "assert";
import * as WebdriverIO from "webdriverio";
import * as driver from "../utilities/driver";
import LoginPage from "../pageModels/LoginPage";

describe("Electron Window Startup Test", () => {
  let app: Spectron.Application;
  let currentEnv: driver.ICurrentEnv;

  beforeAll(async () => {
    currentEnv = await driver.currentEnv();
    await currentEnv.appStart();
    app = currentEnv.electronApp!;
  });

  afterAll(async () => {
    await app.stop();
  });
  beforeEach(async () => {
    const isVisible = await app.browserWindow.isVisible();
    // Verify the window is visible
    assert.strictEqual(isVisible, true);
  });
  it("Verify Window title", async () => {
    const title = await app.client.getTitle();
    assert.strictEqual(title, "Mesh Dynamics");
  });
  it("Login Should Success", async () => { 
    const loginPage = new LoginPage(app.client);
    expect(await loginPage.Validate()).toBe(true);
    await loginPage.Login(currentEnv.configData.username, currentEnv.configData.password);
    const currentUrl = await app.client.getUrl();
    expect(currentUrl.indexOf('/http_client') > 0).toBe(true);
  });
});
