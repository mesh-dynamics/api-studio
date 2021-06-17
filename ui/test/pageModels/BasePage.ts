

/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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