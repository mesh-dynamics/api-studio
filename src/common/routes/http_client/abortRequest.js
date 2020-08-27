import axios from 'axios';
import commonConstants from '../../utils/commonConstants';

export function AbortRequest(){
    var controller = new AbortController();
    this.signal = controller.signal;

    const CancelToken = axios.CancelToken;
    const source = CancelToken.source();
    this.cancelToken = source.token;

    this.stopRequest = function(){
        this.isCancelled = true;
        controller.abort();
        source.cancel(commonConstants.USER_ABORT_MESSAGE);
    }
    this.isCancelled = false;
} 

/*
Usage:

        const ab = new AbortRequest();
        ...
        ab.stopRequest();

        fetch(url, {signal: ab.signal})

        axios.get('/user/12345', {
            cancelToken: ab.cancelToken
            }).catch(function (thrown) {
            if (axios.isCancel(thrown)) {
                console.log('Request canceled', thrown.message);
            } else {
                // handle error
            }
            });
*/