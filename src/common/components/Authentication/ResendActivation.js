import React, { Fragment, useState } from "react";
import { Link } from "react-router-dom";
import { resendActivationToken } from '../../services/auth.service';
import { validateEmail } from '../../utils/lib/validation';
import "./ResendActivation.css";

const ACTIVATION_LINK_REQUEST = {
    SUBMITTED: "SUBMITTED",
    NOT_SUBMITTED: "NOT_SUBMITTED"
};

const ResendActivation = (props) => {

    const [activationEmailId, setActivationEmailId] = useState('');
    
    const [submitted, setSubmitted] = useState(false);

    const [errorMessage, setErrorMessage] = useState('');

    const [activationLinkGenerated, setActivationLinkGenerated] = useState(false);

    const [activationResendSubmitted, setActivationResendSubmitted] = useState(ACTIVATION_LINK_REQUEST.NOT_SUBMITTED);

    const emailValidation = validateEmail(activationEmailId);

    const handleErrorStatus = (statusCode) => {

        switch(statusCode) {
            case 409:
                setErrorMessage('Account Already Activated. Click on the link below to login.');
                break;
            case 403:
                setErrorMessage('The email provided is not registered with us. Please provide a valid email id or contact your system administrator for more information.')
                break;
            default:
                setErrorMessage('Failed to generate activation link. Please contact your system administrator or try again later.');
        }
        
        setActivationResendSubmitted(ACTIVATION_LINK_REQUEST.SUBMITTED);
        
        setActivationLinkGenerated(false);
    };

    const handleResendClick = async () => {
        setSubmitted(true);

        if(emailValidation.isValid) {

            try {

                const response = await resendActivationToken(activationEmailId);

                if(response.ok) {
                    setActivationResendSubmitted(ACTIVATION_LINK_REQUEST.SUBMITTED);

                    setActivationLinkGenerated(true);
                } else {

                    handleErrorStatus(response.status);
                }
            } catch(e) {

                setActivationResendSubmitted(ACTIVATION_LINK_REQUEST.NOT_SUBMITTED);

                setActivationLinkGenerated(false);
            }
            
        }
        
    };

    const renderSuccessMessage = () => (
        <div className="activation-resend-message-container">
            <div>If the email id provided is registered with us, an activation link will be sent to your email.</div>
        </div>
    );

    const renderFailureMessage = () => (
        <div className="activation-resend-message-container">
            <div>{errorMessage}</div>
        </div>
    );

    const renderResetView = () => (
        <div className={'custom-fg form-group' + (submitted && !activationEmailId ? ' has-error' : '')}>
            <div className="activation-resend-description">
                Please enter a valid email id registered with us to send the activation link.
            </div>
            <div className="activation-resend-input-container">
                <input 
                    type="text" 
                    placeholder="Enter email" 
                    className="form-control" 
                    name="username" 
                    value={activationEmailId} 
                    onChange={(e) => setActivationEmailId(e.target.value)} 
                />
                {
                    submitted && 
                    !emailValidation.isValid && 
                    <div className="error-text">
                        {emailValidation.errorMessages.map(message => <Fragment key={message}><span>{message}</span><br /></Fragment>)}
                    </div>
                }
            </div>
            <button onClick={handleResendClick} className="btn btn-custom-auth width-100 activation-resend-button">Send Activation Link</button>
        </div>
    );

    return (
        <div className="pull-right" style={{width: "80%"}}>
            <h2 className="sign-in">Resend Activation Link</h2>

            {!submitted && activationResendSubmitted === ACTIVATION_LINK_REQUEST.NOT_SUBMITTED && !activationLinkGenerated && renderResetView()}

            {submitted && activationResendSubmitted === ACTIVATION_LINK_REQUEST.NOT_SUBMITTED && !activationLinkGenerated && renderResetView()}

            {submitted && activationResendSubmitted === ACTIVATION_LINK_REQUEST.SUBMITTED && !activationLinkGenerated && renderFailureMessage()}

            {submitted && activationResendSubmitted === ACTIVATION_LINK_REQUEST.SUBMITTED && activationLinkGenerated && renderSuccessMessage()}

            <div className="activation-action">
                    <div className="custom-sign-in-divider" />
                    <div className="account-action-container">
                        <span>Already have an account?</span>
                        <Link to="/login" className="btn-link create-account">Log In</Link>
                    </div>
            </div>
        </div>
    )
}

export default ResendActivation;