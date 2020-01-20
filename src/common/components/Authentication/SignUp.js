import React, { useState, Fragment } from 'react';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import ReCaptcha from 'react-google-recaptcha';
import { userActions } from '../../actions/user.actions';
import { 
    validateName, 
    validateEmail, 
    validatePassword
} from '../../utils/lib/validation';
import "./SignUp.css";

const MESSAGE = {
    SUCCESS: "Your account has been successfully created. A verifcation link has been sent to your email. Please click on the link to verify and activate your account.",
    ERROR: "Failed to create user account. Please contact your system administrator and try again later."
};

const SignUp = (props) => {

    const { createUser, verifyToken, history } = props;

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

    const firstNameValidation = validateName(firstName, "Firstname");

    const lastNameValidation =  validateName(lastName, "Lastname");

    const emailValidation = validateEmail(email);
    
    const passwordValidation = validatePassword(password);

    const isValid = () => {
        return reCaptchaIsValid
            && firstNameValidation.isValid
            && lastNameValidation.isValid
            && emailValidation.isValid
            && passwordValidation.isValid
            && reCaptchaToken !== null
            && reCaptchaToken !== ''
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
                
                const status = await createUser(user);
                
                setHasServerValidated(true);

                if(status.ok) {

                    setAccountCreatedSuccessfully(true);

                } else {
                    setAccountCreatedSuccessfully(false);
                }
            } catch(e) {
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

    

    const renderReCaptchaError = () => (
        // (!reCaptchaToken !== null || reCaptchaToken === '')
        // ? (
            <div className="error-text">
                <span>Invalid ReCaptcha. Please try again.</span>
            </div>);
        // ) : null);

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
                    <input 
                        type='checkbox' 
                        name="showPassword" 
                        checked={showPassword} 
                        className="checkbox-custom" 
                        onChange={() => setShowPassword(!showPassword)}
                    /> 
                    <span className='checkbox-label'>Show Password</span>
                </div>
                {
                    submitted && 
                    !passwordValidation.isValid && 
                    <div className="help-block">
                        {passwordValidation.errorMessages.map(message => <Fragment key={message}><span>{message}</span><br /></Fragment>)}
                    </div>
                }
            </div>
            <div className="custom-fg form-group recaptcha-container">
                <ReCaptcha 
                    sitekey="6Lf4x84UAAAAAE1eQicOrCrxVreqAWhpyV3KERpo" 
                    onChange={handleReCaptchaChange} 
                />
                {!reCaptchaIsValid ? renderReCaptchaError() : null}

                {submitted && reCaptchaToken === '' && renderReCaptchaError()}

                {!accountCreatedSuccessfully && submitted && hasServerValidated ? renderFormErrorMessage(): null}
            </div>
            <div className="custom-fg form-group">
                <button className="btn btn-custom-auth width-100">Create Account</button>
            </div>
            <div className="custom-sign-in-divider" />
            <div className="account-action-container">
                <span>Already have an account?</span>
                <Link to="/login" className="btn-link create-account">Log In</Link>
            </div>
        </form>
    </div>
    );

    

    const renderSuccessMessage = () => (
        <div className="form-success-message">
            {MESSAGE.SUCCESS}
            <span className="form-redirection-text">
                You will be redirected to login screen automatically.
            </span>
        </div>
    );

    return (
        submitted && accountCreatedSuccessfully 
        ? renderSuccessMessage() 
        : renderForm()
    );
}

const mapDispatchToProps = (dispatch) => ({
    createUser: (user) => userActions.createUser(user),

    verifyToken: (token) => userActions.verifyToken(token),
});

SignUp.propTypes = {
    createUser: PropTypes.func.isRequired,
    
};

export default connect(null, mapDispatchToProps)(SignUp);
