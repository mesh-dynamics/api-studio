import * as Spectron from "spectron";
import * as assert from "assert";
import * as WebdriverIO from "webdriverio";
import * as driver from "../utilities/driver";

describe("Electron Window Startup Test", () => {
  let app: Spectron.Application;
  let currentEnv: driver.ICurrentEnv;

  beforeAll(async () => {
    console.log("inside before all");
    currentEnv = await driver.currentEnv();
    await currentEnv.appStart();
    app = currentEnv.electronApp!;
  });

  afterAll(async () => {
    console.log("Calling after all");
    await app.stop();
  });
  beforeEach(async () => {
    console.log("inside before each");
    const isVisible = await app.browserWindow.isVisible();
    // Verify the window is visible
    assert.strictEqual(isVisible, true);
  });
  it("Verify Window title", async () => {
    const title = await app.client.getTitle();
    // Verify the window's title
    assert.strictEqual(title, "Mesh Dynamics");
  });
  it("Login Should Success", async () => { 
    await (await app.client.$("[name='username']")).setValue(
      currentEnv.configData.username
    );
    await (await app.client.$("[name='password']")).setValue(
      currentEnv.configData.password
    );
    await (await app.client.$(".btn-custom-auth")).click();
    console.log("Clicke on item");
    await app.client.pause(4000);
    await app.client.waitUntilWindowLoaded();
    console.log("Waiting for move");

    const currentUrl = await app.client.getUrl();
    console.log("Current page ", currentUrl);
  });
});
