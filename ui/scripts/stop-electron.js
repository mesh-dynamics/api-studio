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

const find = require('find-process');
const logger = require('electron-log');

(
    async () => {
        try {
            const pList = await find('name', 'electron', true);

            logger.info('Processes running on electron', pList);
    
            pList.map((item) => {
                logger.info('Killing Process...', item.pid);
                process.kill(item.pid);
            });
        } catch(error) {
            logger.info('Error Killing process', error);
        }
        
    }
)()
