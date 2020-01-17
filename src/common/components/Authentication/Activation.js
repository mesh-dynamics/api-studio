import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { userService } from '../../services/user.service'
import "./Activation.css";

const ACTIVATION = {
    INIT: "INIT",
    SUCCESS: "SUCCESS",
    FAILURE: "FAILURE"
}

const Activation = (props) => {
    const { location: { search }, history }  = props;

    const [activationState, setActivationState] = useState(ACTIVATION.INIT);

    const triggerActivation = async () => {
        try {
            const status = await userService.verifyActivationToken(search);

            if(status.ok) {
                setActivationState(ACTIVATION.SUCCESS);
            } else {
                setActivationState(ACTIVATION.FAILURE);
            }

        } catch(e) {
            setActivationState(ACTIVATION.FAILURE)
        }
        
    }

    const renderActivationInit = () => (
        <div className="activation-message-container">
            <div className="activation-message">
                Your account is being activated. 
                Please do not leave or refresh the page until the process is complete. 
                You will be redirected to login page once the process is complete.
            </div>
        </div>
    );

    const renderActivationSuccess = () => (
        <div className="activation-message-container">
            <div className="activation-message activation-success">
                <span>
                    Your account has been activated. You will be automatically redirected to login page.
                </span>
                <span>
                    If you are not redirected <Link to="/login">click here</Link> to continue manually.
                </span>
            </div>
        </div>
    );

    const renderActivationFailure = () => (
        <div className="activation-message-container">
            <div className="activation-message">
                We could not activate your account. Please contact your system administrator for further instructions.
            </div>
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

// const urlParsed = search
//                             .substring(search.indexOf("?") + 1)
//                             .split("&")
//                             .reduce(
//                                 (memo, param) => ({
//                                 ...memo,
//                                 [param.split("=")[0]]: param.split("=")[1]
//                             }),{}
//                         );