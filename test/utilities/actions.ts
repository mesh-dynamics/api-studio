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
