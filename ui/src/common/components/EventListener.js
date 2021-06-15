
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

function EventListener (obj = {}) {
    const listeners = {}
    Object.assign(obj, { on, off, trigger })
    
    return obj

    function on (eventName, cb) {
        (listeners[eventName] || (listeners[eventName] = [])).push(cb)
        return obj
    }

    function off (eventName, cb) {
        const callbacks = (listeners[eventName] || (listeners[eventName] = []))
        for(let i = 0; i < callbacks.length; ) {
            if (cb === callbacks[i]) {
                callbacks.splice(i, 1)
                continue
            }

            i += 1
        }

        return obj
    }

    function trigger (eventName) {
        const callbacks = (listeners[eventName] || (listeners[eventName] = []))
        callbacks.forEach(cb => cb())
        return obj
    }
}

export default EventListener