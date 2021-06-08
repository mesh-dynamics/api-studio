
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
