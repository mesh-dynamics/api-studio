import * as driver from "../utilities/driver";
import * as webdriverio from "webdriverio";
import LoginPage from "../pageModels/LoginPage";
import TestResultsPage from "../pageModels/TestResultsPage";
import * as actions from '../utilities/actions';


describe("Common Startup Test", () => {
  let currentEnv: driver.ICurrentEnv;
  let client: webdriverio.BrowserObject;
  let currentPage: TestResultsPage;
  beforeAll(async () => {
    currentEnv = await driver.currentEnv();
    await currentEnv.appStart();
    client = currentEnv.client();
    const loginPage = new LoginPage(client);
    await loginPage.Login(
      currentEnv.configData.username,
      currentEnv.configData.password
    );

    currentPage = await actions.NavigateToTestResults(client);
    expect(currentPage.Validate()).toBeTruthy();
  });
  afterAll(async () => {
    await currentEnv.appStop();
  });
  it("Switch App should work", async () => {
    const list = await currentPage.getApplicationList();
    await currentPage.switchApplication("MovieInfo");
    expect(await currentPage.getApplicationName()).toBe("MovieInfo");
  });

  it("Clicking on diff results", async () => {
    await currentPage.switchApplication("MovieInfo");
    await  (await client.$('.timeline-header-text')).waitForExist({timeout: 4000});
    await(await client.$('.rt-td.freeze-column.rthfc-td-fixed.rthfc-td-fixed-left.rt-expandable i')).click();

    //Click on first clickable diff result Green/Red line
    await(await client.$('.rt-tr-group:nth-child(2) .rt-tr-group .rt-td:nth-child(3) div')).click();
    expect((await client.getUrl()).indexOf("/diff_results") > 0).toBeTruthy();
  });
});
