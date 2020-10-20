import BasePage from "./BasePage";


export default class TestResultsPage extends BasePage{

    public async Validate(): Promise<boolean> {
        const currentUrl = await this.client.getUrl();
        return currentUrl.indexOf("/test_results") > 0
    }

}