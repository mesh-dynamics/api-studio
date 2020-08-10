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
