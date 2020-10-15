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
