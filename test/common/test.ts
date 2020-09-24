
import * as driver from '../utilities/driver'
import * as webdriverio from 'webdriverio';
 
describe("Common Startup Test", ()=>{
  let currentEnv: driver.ICurrentEnv;
  let client: webdriverio.BrowserObject;
  beforeAll(async()=>{
    currentEnv = await driver.currentEnv();
    await currentEnv.appStart();
    client = currentEnv.client();

  });
  afterAll(async()=>{
    await currentEnv.appStop();
  });
  it("Verify Window title", async()=>{

    const title = await client.getTitle();
    // Verify the window's title
    expect(title).toBe("Mesh Dynamics");
  });
  it("Login Should Success", async()=>{
     await (await client.$("[name='username']")).setValue(currentEnv.configData.username);
    await (await client.$("[name='password']")).setValue(currentEnv.configData.password);
    await(await client.$('.btn-custom-auth')).click();
    console.log("Clicke on item");
    await client.pause(4000);
    console.log("Waiting for move");
    
    const currentUrl = await client.getUrl();
    console.log("Current page ", currentUrl);


  }); 
})