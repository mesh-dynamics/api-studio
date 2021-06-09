

import * as webdriverio from 'webdriverio';
// import TestResultsPage from './TestResultsPage';

export default class BasePage{
    public client : webdriverio.BrowserObject;
    constructor(client: webdriverio.BrowserObject){
        this.client = client;
    }

    public async Validate(): Promise<boolean> {
        return false;
    }
    // Add common functions to application here

    public async getApplicationList(){
        //Open Application drawer
        await (await this.client.$('.app-s-b i')).click();
        await this.client.pause(50);
        const listOfAppsPrmoise = (await this.client.$$('.app-list .app-name')).map(domElement => domElement.getText());
        const listOfApps = await Promise.all(listOfAppsPrmoise);
        //Close Application drawer
        await (await this.client.$('.app-s-b i')).click();
        return listOfApps;        
    }

    public async switchApplication(name:string){
        const listOfApps = await this.getApplicationList();
        await (await this.client.$('.app-s-b i')).click();
        await this.client.pause(50);
        const existsAt = listOfApps.indexOf(name);
        // console.log("Index available at ", existsAt, listOfApps);
        await (await this.client.$$('.app-list .app-name'))[existsAt].click();
        await this.client.pause(500);
        (await this.client.$('.app-s-b i')).click();
        await this.client.pause(50);
    }

    public async getApplicationName(){
       return await (await this.client.$('.application-name')).getText() ;
    }
}