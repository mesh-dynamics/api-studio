import React, { useState, useEffect } from 'react';
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

    const renderActivationSuccess = () => (
        <div className="activation-message-container">
            <div className="activation-message flex-column">
                <span>
                    Your account has been activated. You will be automatically redirected to login page.
                </span>
                <span>
                    <Link to="/login" className="activation-link">Click here</Link> to continue manually.
                </span>
            </div>
        </div>
    );

    const renderActivationFailure = () => (
        <div className="activation-message-container">
            {
                statusErrorCode === STATUS_ERROR_CODE.ACCOUNT_ACTIVATED 
                ?
                <div className="activation-message flex-column">
                    <span>
                        Account already activated.
                    </span>
                    <span>
                        <Link className="activation-link" to="/login">Click here</Link> to login.
                    </span>
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

    useEffect(() => {
        if(activationState === ACTIVATION.SUCCESS) {
            setTimeout(() => history.push("/login"), 8000);
        }
    }, [activationState]);

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