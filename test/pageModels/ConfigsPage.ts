import BasePage from "./BasePage";

export default class ConfigsPage extends BasePage{
    
    public async Validate(): Promise<boolean> {
        const currentUrl = await this.client.getUrl();
        return currentUrl.indexOf("/configs") > 0
    }

    public async GoToFirstTestConfig(){
        await (await this.client.$('.tc-grid .grid-content')).waitForDisplayed({
            timeout: 5000,
            timeoutMsg: "Test config didn't appear for App within time"
        });       
        await (await this.client.$('.tc-grid .grid-content')).click();    
    }
}