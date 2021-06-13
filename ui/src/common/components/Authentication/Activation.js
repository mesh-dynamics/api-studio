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

import React, { useState, useEffect, Fragment } from 'react';
import { Link } from 'react-router-dom';
import { verifyActivationToken } from '../../services/auth.service';
import "./Activation.css";

const ACTIVATION = {
    INIT: "INIT",
    SUCCESS: "SUCCESS",
    FAILURE: "FAILURE"
}

const STATUS_ERROR_CODE = {
    ACCOUNT_ACTIVATED: 409,
    SERVER_ERROR: 500
};

const Activation = (props) => {
    const { location: { search }, history }  = props;

    const [statusErrorCode, setStatusErrorCode] = useState(null);

    const [activationState, setActivationState] = useState(ACTIVATION.INIT);

    const triggerActivation = async () => {
        try {
            const response = await verifyActivationToken(search);

            if(response.ok) {
                setActivationState(ACTIVATION.SUCCESS);
            } else {
                setActivationState(ACTIVATION.FAILURE);
                setStatusErrorCode(response.status);
            }

        } catch(e) {
            setActivationState(ACTIVATION.FAILURE);
            setStatusErrorCode(STATUS_ERROR_CODE.SERVER_ERROR);
        }
        
    }

    const renderActivationInit = () => (
        <div className="activation-message-container">
            <div className="activation-message">
                Your account is being activated. 
                Please do not leave or refresh the page until the process is complete. 
            </div>
        </div>
    );

    const renderRedirectionResources = () => (
        <Fragment>
            <span className="activation-success-content">
                <Link to="/login" className="activation-link">Click here</Link> to login to Test Studio.
            </span>
            <div className='activation-launch-button-container'>
                <a className="activation-launch-app-button" href='meshd://api-studio'>Launch API Studio</a>
            </div>
            <span className="activation-success-content">
                Don't have API Studio? Download <a className='activation-link' href='https://www.meshdynamics.io/download' target='_blank'>here</a>
            </span>
        </Fragment>
    );

    const renderActivationSuccess = () => (
        <div className="activation-message-container">
            <div className="activation-message flex-column">
                <span className="activation-success-header">
                    Your account has been activated.
                </span>
                {renderRedirectionResources()}
            </div>
        </div>
    );

    const renderActivationFailure = () => (
        <div className="activation-message-container">
            {
                statusErrorCode === STATUS_ERROR_CODE.ACCOUNT_ACTIVATED 
                ?
                <div className="activation-message flex-column">
                    <span className="activation-success-header">
                        Account already activated.
                    </span>
                    {renderRedirectionResources()}
                </div>
                :
                <div className="activation-message flex-column">
                    <span>
                        We could not activate your account. Please contact your system administrator for further instructions
                    </span>
                    <span>
                        or <Link className="activation-link" to="/login/resend-activation-link">click here</Link> to resend activation link.
                    </span>
                </div>
            }
        </div>
    );

    const renderActivationStatus = () => {
        const status  = {
            [ACTIVATION.INIT]: () => renderActivationInit(),
            [ACTIVATION.SUCCESS]: () => renderActivationSuccess(),
            [ACTIVATION.FAILURE]: () => renderActivationFailure()
        }

        return status[activationState]();
    };

    useEffect(() => {
        if(!search) {
            history.push("/login");
        }
        
        if(search && activationState === ACTIVATION.INIT) {
            triggerActivation();
        }

    }, [search, activationState]);

    // Keep for now just in case its needed
    // useEffect(() => {
        // if(activationState === ACTIVATION.SUCCESS) {
            // setTimeout(() => history.push("/login"), 8000);
    //     }
    // }, [activationState]);

    return (
        <div className="activation-root">
            {renderActivationStatus()}
            <div className="activation-copyright">
                All rights reserved with meshdynamics.io &copy; 
            </div>
        </div>
    )
}



export default Activation;