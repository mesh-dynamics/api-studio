import React, { useState, Fragment, useEffect } from 'react';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import ReCaptcha from 'react-google-recaptcha';
import authActions from '../../actions/auth.actions';
import { 
    validateName, 
    validateEmail, 
    validatePassword
} from '../../utils/lib/validation';
import { getCaptchaConfig } from '../../services/auth.service';
import { isCaptchaEnabled, getDomainNameFromHostname } from '../../utils/lib/common-utils';
import "./SignUp.css";
const MESSAGE = {
    SUCCESS: "Your account has been successfully created. A verifcation link has been sent to your email. Please click on the link to verify and activate your account.",
    ERROR: "Failed to create user account. Please contact your system administrator or try again later."
};

const SignUp = (props) => {

    const { createUser, verifyToken } = props;

    const [firstName, setFirstName] = useState('');

    const [lastName, setLastName] = useState('');

    const [email, setEmail] = useState('');

    const [password, setPassword] = useState('');

    const [showPassword, setShowPassword] = useState(false);

    const [submitted, setSubmitted] = useState(false);

    const [reCaptchaIsValid, setReCaptchaIsValid] = useState(true);

    const [reCaptchaToken, setRecaptchaToken] = useState('');

    const [accountCreatedSuccessfully, setAccountCreatedSuccessfully] = useState(false);

    const [hasServerValidated, setHasServerValidated] = useState(false);

    const [requiresCaptchaValidation, setRequiresCaptchaValidation ] = useState(false);

    const [isCreatingUser, setIsCreatingUser] = useState(false);

    const firstNameValidation = validateName(firstName, "Firstname");

    const lastNameValidation =  validateName(lastName, "Lastname");

    const emailValidation = validateEmail(email);
    
    const passwordValidation = validatePassword(password);

    const isValid = () => {
        if(PLATFORM_ELECTRON) {
            return reCaptchaIsValid
                && firstNameValidation.isValid
                && lastNameValidation.isValid
                && emailValidation.isValid
                && passwordValidation.isValid
        }

        if(requiresCaptchaValidation) {
            return reCaptchaIsValid
                && firstNameValidation.isValid
                && lastNameValidation.isValid
                && emailValidation.isValid
                && passwordValidation.isValid
                && reCaptchaToken !== null
                && reCaptchaToken !== ''            
        }

        return reCaptchaIsValid
            && firstNameValidation.isValid
            && lastNameValidation.isValid
            && emailValidation.isValid
            && passwordValidation.isValid
    };

    const fetchCaptchaConfig = async () => {
        try {
            const domain = getDomainNameFromHostname(window.location.hostname);

            const response = await getCaptchaConfig(domain);

            const responseBody = await response.json();

            if(responseBody.status > 400) {
                setRequiresCaptchaValidation (false);
            } else {
                const captchaEnabled = isCaptchaEnabled(responseBody);

                setRequiresCaptchaValidation (captchaEnabled);
            }
        } catch(error) {

            setRequiresCaptchaValidation (false);
        }
    };
    
    const handleSubmit = async (event) => {
        event.preventDefault();

        setSubmitted(true);

        if(isValid()) {
            const user = {
                name: `${firstName} ${lastName}`,
                email,
                password,
            };
    
            try {

                setIsCreatingUser(true);
                
                const status = await createUser(user);

                setIsCreatingUser(false);
                
                setHasServerValidated(true);

                if(status.ok) {

                    setAccountCreatedSuccessfully(true);

                } else {
                    setAccountCreatedSuccessfully(false);
                }
            } catch(e) {
                setIsCreatingUser(false);
                setHasServerValidated(true);
                setAccountCreatedSuccessfully(false);
            }
        }

        
    };

    const handleReCaptchaChange = async (value) => {
        setRecaptchaToken(value);
        try {
            // Verify token returns 200 for success 
            // and 4xx for failure. Body is empty
            const status = await verifyToken(value);

            if(status.ok) {
                setReCaptchaIsValid(true);
            } else {
                setReCaptchaIsValid(false);    
            }
            
        } catch(e) {
            setReCaptchaIsValid(false);
        }
    };

    useEffect(() => {
        fetchCaptchaConfig();
    }, []);

    const renderReCaptchaError = () => (
        <div className="error-text">
            <span>Invalid ReCaptcha. Please try again.</span>
        </div>
    );

    const renderFormErrorMessage = () => (
        <div className="form-error-message">
            {MESSAGE.ERROR}
        </div>
    );

    const renderForm = () => (
        <div className="pull-right" style={{width: "80%"}}>
            <h2 className="create-account-label">Create an account</h2>
            <form name="form" onSubmit={handleSubmit}>
            <div className={"custom-fg form-group " + (submitted && !firstNameValidation.isValid ? "has-error" : "")}>
                <input 
                    type="text" 
                    name="firstname" 
                    value={firstName} 
                    placeholder="Firstname" 
                    className="form-control"
                    onChange={(e) => setFirstName(e.target.value.trim())} 
                />
                {
                    submitted && 
                    !firstNameValidation.isValid && 
                    <div className="help-block">
                        {firstNameValidation.errorMessages.map(message => <Fragment key={message}><span>{message}</span><br /></Fragment>)}
                    </div>
                }
            </div>
            <div className={"custom-fg form-group " + (submitted && !lastNameValidation.isValid ? "has-error" : "")}>
                <input 
                    type="text"
                    name="lastname" 
                    value={lastName}
                    placeholder="Lastname" 
                    className="form-control"
                    onChange={(e) => setLastName(e.target.value)} 
                />
                {
                    submitted && 
                    !lastNameValidation.isValid && 
                    <div className="help-block">
                        {lastNameValidation.errorMessages.map(message => <Fragment key={message}><span>{message}</span><br /></Fragment>)}
                    </div>
                }
            </div>
            <div className={"custom-fg form-group " + (submitted && !emailValidation.isValid ? "has-error" : "")}>
                <input 
                    type="text" 
                    name="email" 
                    value={email}
                    placeholder="Email" 
                    className="form-control"
                    onChange={(e) => setEmail(e.target.value)} 
                />
                {
                    submitted && 
                    !emailValidation.isValid && 
                    <div className="help-block">
                        {emailValidation.errorMessages.map(message => <Fragment key={message}><span>{message}</span><br /></Fragment>)}
                    </div>
                }
            </div>
            <div className={"custom-fg form-group " + (submitted && !passwordValidation.isValid ? "has-error" : "")}>
                <input 
                    name="password" 
                    value={password}
                    placeholder="Password" 
                    className="form-control"
                    type={showPassword ? "text" : "password"}
                    onChange={(e) => setPassword(e.target.value)}
                />
                <div className='checkbox-container'>
                    <label className='checkbox-label'> 
                    <input 
                        type='checkbox' 
                        name="showPassword" 
                        id="showPassword" 
                        checked={showPassword} 
                        className="checkbox-custom" 
                        onChange={() => setShowPassword(!showPassword)}
                    /> 
                    &nbsp; Show Password</label>
                </div>
                {
                    submitted && 
                    !passwordValidation.isValid && 
                    <div className="help-block">
                        {passwordValidation.errorMessages.map(message => <Fragment key={message}><span>{message}</span><br /></Fragment>)}
                    </div>
                }
            </div>
            {
                
                <div className="custom-fg form-group recaptcha-container">
                {
                    (!PLATFORM_ELECTRON && requiresCaptchaValidation)
                    &&
                    (
                        <Fragment>
                            <ReCaptcha 
                                sitekey="6Lf4x84UAAAAAE1eQicOrCrxVreqAWhpyV3KERpo" 
                                onChange={handleReCaptchaChange} 
                            />
                        
                            {!reCaptchaIsValid ? renderReCaptchaError() : null}

                            {submitted && reCaptchaToken === '' && renderReCaptchaError()}
                        </Fragment>
                    )
                }
                        
                {!accountCreatedSuccessfully && submitted && hasServerValidated ? renderFormErrorMessage(): null}

                </div>
            }
            <div className="custom-fg form-group">
                <button disabled={isCreatingUser} className="btn btn-custom-auth width-100">
                    Create Account
                    {
                        isCreatingUser
                        &&
                        <span className="spinner-container">
                            <i className="fa fa-spinner fa-spin"></i>
                        </span>
                    }
                    
                </button>
            </div>
            
            <div className="custom-sign-in-divider" />
            <div className="account-action-container flex-column">
                <div>
                    <span>Already have an account?</span>
                    <Link to="/login" className="btn-link create-account">Log In</Link>
                </div>
                <div className="resend-container">
                    <span>
                        Account not activated?
                    </span>
                    <Link to="/login/resend-activation-link" className="btn-link create-account">Click Here</Link>
                </div>    
            </div>
        </form>
    </div>
    );

    

    const renderSuccessMessage = () => (
        <div>
            <div className="form-success-message">
                {MESSAGE.SUCCESS}
            </div>
            <div>
                <Link to="/login" className="btn-link create-account">Back to Login</Link>
            </div>
        </div>
    );

    return (
        submitted && accountCreatedSuccessfully 
        ? renderSuccessMessage() 
        : renderForm()
    );
}

const mapDispatchToProps = (dispatch) => ({
    createUser: (user) => authActions.createUser(user),

    verifyToken: (token) => authActions.verifyToken(token),
});

SignUp.propTypes = {
    createUser: PropTypes.func.isRequired,
    
};

export default connect(null, mapDispatchToProps)(SignUp);
