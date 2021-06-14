
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

const {series, parallel} = require('async');
const {exec, execSync} = require('child_process');

const args = process.argv.slice(2);
//all, electron, web
console.log(args);

function attachLogger(currentProcess){
    currentProcess.stdout.on('data', (data) => {
        console.log('stdout: ' + data.toString())
      })
    
      currentProcess.stderr.on('data', (data) => {
        console.log('stderr: ' + data.toString())
      })
    
      currentProcess.on('exit', (code) => {
        console.log('child process exited with code ' + code.toString())
      })
}

function RunForElectron(){
    try{

        console.log("Running Electron")
        PLATFORM_ELECTRON = true;
        const currentProcess = exec('jest "test2 copy"');
       
        attachLogger(currentProcess);
    }catch(e){
        console.error(e);
    }
}

function RunForWEB(){
    console.log("Running Web")
    PLATFORM_ELECTRON = false;
    const currentProcess = exec('jest "test2 copy"');
        attachLogger(currentProcess);
}
if(args.indexOf('all') || args.indexOf('electron')){

    RunForElectron();
}
if(args.indexOf('all') || args.indexOf('web')){
    const chromedriverProcess = exec("./node_modules/.bin/chromedriver");
    RunForWEB();
    chromedriverProcess.kill();
}
