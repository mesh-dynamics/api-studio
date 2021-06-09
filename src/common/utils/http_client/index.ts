import * as httpUtils from './utils.js';
import * as tsUtils from './httpClientUtils';

//Going forward, all utils can be combined into single import statement

export default {
    ...httpUtils,
    ...tsUtils
}