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