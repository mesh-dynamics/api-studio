import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import { userActions } from "../../actions/user.actions";

const SignIn = (props) => {

    const [username, setUsername] = useState('');

    const [password, setPassword] = useState('');

    const [submitted, setSubmitted] = useState(false);

    const { authentication: { user, loggedIn }, login }  = props;

    const handleSubmit = (e) => {
        e.preventDefault();

        setSubmitted(true);

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
                    {submitted && !password &&
                    <div className="help-block">Password is required</div>
                    }
                </div>
                <div className="btn-link forgot-password">
                    <Link to="/reset" >Forgot your password?</Link>
                </div>
                <div className="custom-fg form-group">
                    <button className="btn btn-custom-auth width-100">Login</button>
                </div>
                <div className="custom-sign-in-divider" />
                <div className="create-account-container">
                    <span>Don't have account?</span>
                    <Link to="/login/register" className="btn-link create-account">Create Account</Link>
                </div>
            </form>
        </div>
    )
}

const mapStateToProps = (state) => ({
    authentication: state.authentication
});

const mapDispatchToProps = (dispatch) => ({
    login: (username, password) => dispatch(userActions.login(username, password)),
})

SignIn.propTypes = {
    authentication: PropTypes.object.isRequired,
    login: PropTypes.func.isRequired
};

export default connect(mapStateToProps, mapDispatchToProps)(SignIn);