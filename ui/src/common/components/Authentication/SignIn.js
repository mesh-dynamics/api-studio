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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import authActions from '../../actions/auth.actions';

const SignIn = (props) => {

    const { 
        login, 
        authentication: { 
            messages, 
            credentials,
            rememberMe
        }, 
        rememberCredentials,
        forgetCredentials,
        toggleRememberMe,
    }  = props;

    const persistedUsername = credentials?.username || ''; // Persist Migration

    const persistedPassword = credentials?.password || '';

    const persistedRememberMe = rememberMe || false;

    const [username, setUsername] = useState(persistedUsername);

    const [password, setPassword] = useState(persistedPassword);

    const [submitted, setSubmitted] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();

        setSubmitted(true);

        if(persistedRememberMe){ 
            rememberCredentials(username, password);
        } else {
            forgetCredentials();
        }
        
        if (username && password) {
            login(username, password)
        }
    }

    return (
        <div className="pull-right" style={{width: "80%"}}>
            <h2 className="sign-in">Sign In</h2>
            <form name="form" onSubmit={handleSubmit}>
                <div className={'custom-fg form-group' + (submitted && !username ? ' has-error' : '')}>
                    {/*<label htmlFor="username">Username</label>*/}
                    <input type="text" placeholder="Enter User ID" className="form-control" name="username" value={username} onChange={(e) => setUsername(e.target.value)} />
                    {submitted && !username &&
                    <div className="help-block">Username is required</div>
                    }
                </div>
                <div className={'custom-fg form-group' + (submitted && !password ? ' has-error' : '')}>
                    {/*<label htmlFor="password">Password</label>*/}
                    <input type="password" placeholder="Enter Password" className="form-control" name="password" value={password} onChange={(e) => setPassword(e.target.value)} />
                    {
                        submitted && !password &&
                        <div className="help-block">Password is required</div>
                    }
                    {
                        PLATFORM_ELECTRON
                        && 
                        <div className="login-remember-container">
                            <input className="login-remember-input" type="checkbox" checked={persistedRememberMe} onChange={toggleRememberMe} />
                            <span className="login-remember-label">Remember Me</span>
                        </div>
                    }
                </div>
                <div className="btn-link forgot-password">
                    <Link to="/login/reset-link">Forgot your password?</Link>
                </div>
                {
                    messages.length !== 0 &&  
                    <div className="login-error">
                        {messages.join("\n")}
                    </div>
                }
                <div className="custom-fg form-group">
                    <button className="btn btn-custom-auth width-100">Login</button>
                </div>
                <div className="custom-sign-in-divider" />
                <div className="account-action-container">
                    <span>Don't have account?</span>
                    <div className="btn-link">
                        <Link to="/login/register" className="create-account">Create Account</Link>
                    </div>
                </div>
            </form>
        </div>
    )
}

const mapStateToProps = (state) => ({
    authentication: state.authentication
});

const mapDispatchToProps = (dispatch) => ({
    login: (username, password) => dispatch(authActions.login(username, password)),

    rememberCredentials: (username, password) => dispatch(authActions.rememberCredentials({ username, password })),

    toggleRememberMe: () => dispatch(authActions.toggleRememberMe()),

    forgetCredentials: () => dispatch(authActions.forgetCredentials())
})

SignIn.propTypes = {
    authentication: PropTypes.object.isRequired,
    login: PropTypes.func.isRequired
};

export default connect(mapStateToProps, mapDispatchToProps)(SignIn);