import React, { useState, Fragment } from 'react';
import { connect } from "react-redux";
import {
    validatePassword
} from '../../utils/lib/validation';
import config from '../../config';
import api from '../../api';

const ChangePassword = function (props) {
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');

    const [submitted, setSubmitted] = useState(false);

    const [successMessage, setSuccessMessage] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const passwordValidation = validatePassword(newPassword);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setSuccessMessage("");
        setErrorMessage("");
        setSubmitted(true);
        if (oldPassword == newPassword) {
            setErrorMessage("New password can not be same as old password");
        }
        else if (passwordValidation.isValid) {
            const url = "/account/change-password";
            const headers = {
                "Content-Type": "application/json",
                'Access-Control-Allow-Origin': '*'
            };
            try {
                await api.post(`${config.apiBaseUrl}${url}`, JSON.stringify({ newPassword, oldPassword }), { headers })

                setSuccessMessage("Password changed successfully");
                setOldPassword('');
                setNewPassword('');
                setSubmitted(false);
            }
            catch (error) {
                if (error.response && error.response.data && error.response.data.message) {
                    setErrorMessage(error.response.data.message);
                }
                else {
                    setErrorMessage("Some error occurred");
                }
            }
        }
    }

    return <div className="col-md-6">
        <h2 className="create-account-label">Change Password</h2>
        <form name="form" onSubmit={handleSubmit}>
            <div className={"custom-fg form-group "}>
                <div>Username: {props.user.username}</div>
            </div>
            {successMessage && <div className="alert alert-success" role="alert">
                <span>{successMessage}</span>
            </div>}
            {errorMessage && <div className="alert alert-danger" role="alert">
                <span>{errorMessage}</span>
            </div>}
            <div className={"custom-fg form-group "}>
                <input
                    name="password"
                    value={oldPassword}
                    placeholder="Old Password"
                    className="form-control"
                    type={showPassword ? "text" : "password"}
                    onChange={(e) => setOldPassword(e.target.value)}
                />
            </div>
            <div className={"custom-fg form-group " + (submitted && !passwordValidation.isValid ? "has-error" : "")}>
                <input
                    name="newpassword"
                    value={newPassword}
                    placeholder="New Password"
                    className="form-control"
                    type={showPassword ? "text" : "password"}
                    onChange={(e) => setNewPassword(e.target.value)}
                />
                <div className='checkbox-container'>
                    <input
                        type='checkbox'
                        name="showPassword"
                        checked={showPassword}
                        className="checkbox-custom"
                        onChange={() => setShowPassword(!showPassword)}
                    />
                    <span className='checkbox-label'>Show Passwords</span>
                </div>
                {
                    submitted &&
                    !passwordValidation.isValid &&
                    <div className="help-block">
                        {passwordValidation.errorMessages.map(message => <Fragment key={message}><span>{message}</span><br /></Fragment>)}
                    </div>
                }
            </div>
            <div className="custom-fg form-group">
                <button className="btn btn-custom-auth width-100">Change Password</button>
            </div>
        </form>
    </div>

}


function mapStateToProps(state) {
    const { user } = state.authentication;
    const { cube } = state;
    return {
        cube, user
    }
}

const connectedChangePassword = connect(mapStateToProps)(ChangePassword);

export default connectedChangePassword;