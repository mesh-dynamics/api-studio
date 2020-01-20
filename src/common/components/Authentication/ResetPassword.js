import React, { useState, useEffect, Fragment } from "react";
import { Link } from "react-router-dom";
import { validatePassword } from "../../utils/lib/validation";
import { userService } from '../../services/user.service'
import "./ResetPassword.css";

const RESET_STATE = {
    INIT: "INIT",
    SUCCESS: "SUCCESS",
    FAILURE: "FAILURE"
};

const ResetPassword = (props) => {

    const { location: { search }, history }  = props;

    const urlParsed = search
                            .substring(search.indexOf("?") + 1)
                            .split("&")
                            .reduce(
                                (memo, param) => ({
                                ...memo,
                                [param.split("=")[0]]: param.split("=")[1]
                            }),{}
                        );

    const [password, setPassword] = useState("");

    const [resetState, setResetState] = useState(RESET_STATE.INIT);

    const [fetching, setFetching] = useState(false);

    const [showPassword, setShowPassword] = useState(false);

    const [submitted, setSubmitted] = useState(false);

    const passwordValidation = validatePassword(password);

    const handleResetClick = async () => {                    
        setSubmitted(true);

        if(passwordValidation.isValid) {
            try {
                setFetching(true);
                
                const status = await userService.resetPassword(urlParsed.key || '', password);

                setFetching(false);
                
                if (status.ok) {
                    setResetState(RESET_STATE.SUCCESS);
                } else {
                    setResetState(RESET_STATE.FAILURE);
                }
            } catch(e) {
                setResetState(RESET_STATE.FAILURE);
                setFetching(false);
            }
        }
    };

    const renderPasswordInput = () => (
        <Fragment>
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
            <button onClick={handleResetClick} className="btn btn-custom-auth width-100 reset-link-button">Reset Password</button>
        </Fragment>
    );

    const renderResetFailure = () => (
        <div className="reset-password-message">
            Failed to reset your password. Please try again after sometime or contact your system administrator.
        </div>
    );

    const renderResetSuccess = () => (
        <div className="reset-password-message">
            Your password has been reset successfully. <Link className="btn-link" to="/login">Click here </Link> to login.
        </div>
    )

    useEffect(() => {
        if(!search) {
            history.push("/login")
        }
    }, [search]);

    return (
        <div className="pull-right" style={{width: "80%"}}>
            <h2 className="sign-in">Set New Password</h2>

            {!submitted && resetState === RESET_STATE.INIT && renderPasswordInput()}

            {submitted && resetState === RESET_STATE.INIT && renderPasswordInput()}

            {submitted && !fetching && resetState === RESET_STATE.FAILURE && renderResetFailure()}
            
            {submitted && !fetching && resetState === RESET_STATE.SUCCESS && renderResetSuccess()}

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


export default ResetPassword;