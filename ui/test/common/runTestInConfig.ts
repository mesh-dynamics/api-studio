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

import * as driver from "../utilities/driver";
import * as webdriverio from "webdriverio";
import LoginPage from "../pageModels/LoginPage";
import * as actions from "../utilities/actions";
import { ConfigsPage } from "../pageModels";

//TODO: Move this function to utilities

describe("Common Startup Test", () => {
  let currentEnv: driver.ICurrentEnv;
  let client: webdriverio.BrowserObject;
  let currentPage: ConfigsPage;
  beforeAll(async () => {
    currentEnv = await driver.currentEnv();
    await currentEnv.appStart();
    client = currentEnv.client();
    const loginPage = new LoginPage(client);
    await loginPage.Login(
      currentEnv.configData.username,
      currentEnv.configData.password
    );
    client.pause(1000);
    await loginPage.switchApplication("MovieInfo");
  });
  beforeEach(async () => {
    currentPage = await actions.NavigateToConfigPage(client);
    expect(currentPage.Validate()).toBeTruthy();
  });
  afterAll(async () => {
    await currentEnv.appStop();
  });

  it("Clicking on config should load test_config_view page", async () => {
    await client.pause(4000);
    await currentPage.GoToFirstTestConfig();
    await client.waitUntil(
      async () =>
         (await client.getUrl()).indexOf("/test_config_view") > 0,
      {
        timeoutMsg:
          "Page failed to navigate to test_config_view from Test configs",
        timeout: 5000,
      }
    );
    expect(
      (await (await client.getUrl()).indexOf("/test_config_view")) > 0
    ).toBeTruthy();
  });

  it("Change instance and run a request", async () => {
    await currentPage.GoToFirstTestConfig();
    await (await client.$("#ddlInstance")).selectByVisibleText("prod");
    await (await client.$("#ddlTestId")).selectByIndex(2);
    await (await client.$("#btnRunTest")).click();
    await (await client.$("#btnRunTestViewResults")).waitForDisplayed({
      timeout: 5 * 60 * 1000,
      timeoutMsg: "View Results button didn't appear within 5 minutes",
    });
    await (await client.$("#btnRunTestViewResults")).click();
    await client.waitUntil(
      async () => (await (await client.getUrl()).indexOf("/test_results")) > 0,
      {
        timeoutMsg:
          "Page failed to navigate to test_results from 'View results' button",
        timeout: 5000,
      }
    );
  });
});
