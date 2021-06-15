
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

import Tippy from '@tippy.js/react';
import React from 'react';
export interface ISSLAuthorizationIndicatorProps{
    authorized?:{
        authorized: boolean;
        authorizationError: string
    }
}

export default function SSLAuthorizationIndicator(props: ISSLAuthorizationIndicatorProps){
    if(props.authorized && props.authorized.authorized == false){
        return <Tippy content={<>SSL Error Occurred <span className="red">{props.authorized.authorizationError || ""}</span></>} arrow={true} theme="light" placement="bottom">
            <i className="fas fa-exclamation-triangle margin-right-5 red"></i>
        </Tippy>
    }

    return <></>
}