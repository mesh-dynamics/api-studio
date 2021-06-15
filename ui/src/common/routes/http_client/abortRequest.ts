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