import BasePage from "./BasePage";

export default class HttpClientPage extends BasePage{
    
    public async Validate(): Promise<boolean> {
        const currentUrl = await this.client.getUrl();
        return currentUrl.indexOf("/http_client") > 0
    }
}