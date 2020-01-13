import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import ReCaptcha from 'react-google-recaptcha';
import { userActions } from '../../actions/user.actions';
import "./SignUp.css";

const SignUp = (props) => {

    const { createUser } = props;

    const [firstName, setFirstName] = useState('');

    const [lastName, setLastName] = useState('');

    const [email, setEmail] = useState('');

    const [password, setPassword] = useState('')

    const [showPassword, setShowPassword] = useState(false);

    const [submitted, setSubmitted] = useState(false);
    
    const handleSubmit = (event) => {
        event.preventDefault();

        setSubmitted(true);

        const user = {
            "customerId": 1,
            "name": "test",
            "email": "test-user@testcustomer.com",
            "password": "password",
            "roles": ["ROLE_USER"]
        };

        createUser(user);
    };

    const handleReCaptchaChange = (value) => {
        console.log("ReCaptcha", value);
    };

    return (
        <div className="pull-right" style={{width: "80%"}}>
            <h2 className="create-account-label">Create an account</h2>
            <form name="form" onSubmit={handleSubmit}>
                <div className={"custom-fg form-group " + (submitted && !firstName ? "has-error" : "")}>
                    <input 
                        type="text" 
                        name="firstname" 
                        value={firstName} 
                        placeholder="Firstname" 
                        className="form-control"
                        onChange={(e) => setFirstName(e.target.value)} 
                    />
                    {submitted && !firstName && <div className="help-block">Firstname is required</div>}
                </div>
                <div className={"custom-fg form-group " + (submitted && !lastName ? "has-error" : "")}>
                    <input 
                        type="text"
                        name="lastname" 
                        value={lastName}
                        placeholder="Lastname" 
                        className="form-control"
                        onChange={(e) => setLastName(e.target.value)} 
                    />
                    {submitted && !lastName && <div className="help-block">Lastname is required</div>}
                </div>
                <div className={"custom-fg form-group " + (submitted && !email ? "has-error" : "")}>
                    <input 
                        type="email" 
                        name="email" 
                        value={email}
                        placeholder="Email" 
                        className="form-control"
                        onChange={(e) => setEmail(e.target.value)} 
                    />
                    {submitted && !email && <div className="help-block">Email is required</div>}
                </div>
                <div className={"custom-fg form-group " + (submitted && !password ? "has-error" : "")}>
                    <input 
                        name="password" 
                        value={password}
                        placeholder="Password" 
                        className="form-control"
                        type={showPassword ? "text" : "password"}
                        onChange={(e) => setPassword(e.target.value)}
                    />
                    {submitted && !password && <div className="help-block">Password is required</div>}
                </div>
                <div className='custom-fg form-group checkbox-container'>
                    <input 
                        type='checkbox' 
                        name="showPassword" 
                        checked={showPassword} 
                        className="checkbox-custom" 
                        onChange={() => setShowPassword(!showPassword)}
                    /> 
                    <span className='checkbox-label'>Show Password</span>
                </div>
                <div className='custom-fg form-group'>
                    <ReCaptcha 
                        sitekey="6Lf4x84UAAAAAE1eQicOrCrxVreqAWhpyV3KERpo" 
                        onChange={handleReCaptchaChange} 
                    />
                </div>
                <div className="custom-fg form-group">
                    <button className="btn btn-custom-auth width-100">Create Account</button>
                </div>
                <div className="custom-sign-in-divider" />
                <div className="create-account-container">
                    <span>Already have an account?</span>
                    <Link to="/auth" className="btn-link create-account">Log In</Link>
                </div>
            </form>
        </div>
    )
}

const mapDispatchToProps = (dispatch) => ({
    createUser: (user) => dispatch(userActions.createUser(user)),
});

SignUp.propTypes = {
    createUser: PropTypes.func.isRequired,
};

export default connect(null, mapDispatchToProps)(SignUp);

// const mapStateToProps = (state) => ({
//     authentication: state.authentication,
// });
