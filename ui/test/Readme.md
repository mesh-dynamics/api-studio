
Steps for Electron testing
1. npm i (if node done)
2. npm run test:electron

Steps for Chrome Testing:
1. Start ChromeDriver
 ./node_modules/.bin/chromedriver
 2. npm run test:web


* How to manage test files:
 For Common test cases, write in test/common folder, these will run on both platforms
 For Electorn test cases, write in test/electron folder
 For Web test cases, write in test/web folder


* If any error regarding chrome support for specific version, download specific chromeDriver from here:
https://sites.google.com/a/chromium.org/chromedriver/

