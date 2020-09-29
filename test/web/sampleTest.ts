
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
  it("Login Should Success", async()=>{
   

    await (await appClient.$("[name='username']")).setValue(currentDriver.configData.username);
    await (await appClient.$("[name='password']")).setValue(currentDriver.configData.password);
    await(await appClient.$('.btn-custom-auth')).click();
    await appClient.pause(4000);
    const currentUrl = await appClient.getUrl();
    expect(currentUrl.indexOf("test_results")> -1).toBeTruthy();

  }); 
})