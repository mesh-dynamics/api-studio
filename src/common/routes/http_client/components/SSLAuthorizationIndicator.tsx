
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