import React from 'react';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import "./SignUp.css";

const SignUp = (props) => {
    return (
        <div className="pull-right" style={{width: "80%"}}>
            <h2 className="create-account-label">Create an account</h2>
            <form name="form" onSubmit={this.handleSubmit}>
                <div className='custom-fg form-group'>
                    <input type="text" placeholder="Firstname" className="form-control" name="firstname" value="" onChange={this.handleChange} />
                    <div className="help-block">Firstname is required</div>
                </div>
                <div className='custom-fg form-group'>
                    <input type="password" placeholder="Lastname" className="form-control" name="lastname" value="" onChange={this.handleChange} />
                    <div className="help-block">Lastname is required</div>
                </div>
                <div className='custom-fg form-group'>
                    <input type="password" placeholder="Email" className="form-control" name="email" value="" onChange={this.handleChange} />
                    <div className="help-block">Email is required</div>
                </div>
                <div className='custom-fg form-group'>
                    <input type="password" placeholder="Password" className="form-control" name="password" value="" onChange={this.handleChange} />
                    <div className="help-block">Password is required</div>
                </div>
                <div className='custom-fg form-group checkbox-container'>
                    <input className="checkbox-custom" type='checkbox' name="showPassword" /> 
                    <span className='checkbox-label'>Show Password</span>
                </div>

                <div className="custom-fg form-group">
                    <button className="btn btn-custom-sign-in width-100">Create Account</button>
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

export default SignUp;