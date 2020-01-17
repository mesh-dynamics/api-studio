import React, { Fragment, useState } from "react";
import { Link } from "react-router-dom";
import { userService } from '../../services/user.service';
import { validateEmail } from '../../utils/lib/validation';
import "./ResetLink.css";

const ResetLink = (props) => {

    const [activationEmailId, setActivationEmailId] = useState('');
    
    const [submitted, setSubmitted] = useState(false);

    const [fetching, setFetching] = useState(false);

    const [resetLinkGenerated, setResetLinkGenerated] = useState(false);

    const emailValidation = validateEmail(activationEmailId);

    const handleResetClick = async () => {
        setSubmitted(true);

        if(emailValidation.isValid) {

            try {
                setFetching(true);

                const status = await userService.sendResetLink(activationEmailId);

                setFetching(false);

                if(status.ok) {
                    setResetLinkGenerated(true);
                } else {
                    setResetLinkGenerated(false);
                }
            } catch(e) {
                setResetLinkGenerated(false);

                setFetching(false);
            }
            
        }
        
    };

    const renderSuccessMessage = () => (
        <div className="reset-link-message-container">
            <div>If the email id provided is registered with us, a reset link will be sent to your email.</div>
            <div className="resend-reset-link">To resend activation link, <span onClick={handleResetClick} className="btn-link reset-link">click here.</span></div>
        </div>
    );

    const renderFailureMessage = () => (
        <div className="reset-link-message-container">
            <div>Failed to generate reset link. Please contact your system administrator or try again later.</div>
        </div>
    );

    const renderResetView = () => (
        <div className={'custom-fg form-group' + (submitted && !activationEmailId ? ' has-error' : '')}>
            <div className="reset-link-description">
                Please enter a valid email id to receive a reset link.
            </div>
            <div className="reset-link-input-container">
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
            <button onClick={handleResetClick} className="btn btn-custom-auth width-100 reset-link-button">Send Reset Link</button>
        </div>
    );

    return (
        <div className="pull-right" style={{width: "80%"}}>
            <h2 className="sign-in">Reset Password</h2>
            
            {!submitted && !resetLinkGenerated && renderResetView()}

            {submitted && !fetching && !resetLinkGenerated && renderFailureMessage()}

            {submitted && !fetching && resetLinkGenerated && renderSuccessMessage()}
            

            <div className="reset-actions">
                    <div className="custom-sign-in-divider" />
                    <div className="account-action-container">
                        <span>Already have an account?</span>
                        <Link to="/login" className="btn-link create-account">Log In</Link>
                    </div>
            </div>
        </div>
    )
}

export default ResetLink;