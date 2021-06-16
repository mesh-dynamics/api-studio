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
import { TestResultsPage, HttpClientPage, ConfigsPage } from "../pageModels";

export async function NavigateToTestResults(client: webdriverio.BrowserObject) {
  (await client.$('.q-links-top a[href^="/test_results"]')).click();
  await client.waitUntil(
    async () => (await (await client.getUrl()).indexOf("/test_results")) > 0,
    {
      timeoutMsg: "Failed to navigate to Test Results page",
      timeout: 5000,
    }
  );
  return new TestResultsPage(client);
}

export async function NavigateToHttpClient(client: webdriverio.BrowserObject) {
  (await client.$('.q-links-top a[href^="/http_client"]')).click();
  await client.waitUntil(
    async () => (await (await client.getUrl()).indexOf("/http_client")) > 0,
    {
      timeoutMsg: "Failed to navigate to Http client page",
      timeout: 5000,
    }
  );
  return new HttpClientPage(client);
}
export async function NavigateToConfigPage(client: webdriverio.BrowserObject) {
  await (await client.$('.q-links-top a[href^="/configs"]')).click();
  await client.waitUntil(
    async () => (await (await client.getUrl()).indexOf("/configs")) > 0,
    {
      timeoutMsg: "Failed to navigate to Config page",
      timeout: 5000,
    }
  );
  return new ConfigsPage(client);
}
