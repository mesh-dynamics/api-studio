import React from 'react';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import {userActions} from "../../actions/user.actions";
import {Redirect} from "react-router";
import "./Login.css";

/*import { userActions } from '../_actions';*/

class LoginPage extends React.Component {
    constructor(props) {
        super(props);

        // reset login status
        //this.props.dispatch(userActions.logout());

        this.state = {
            username: '',
            password: '',
            submitted: false
        };

        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleChange(e) {
        const { name, value } = e.target;
        this.setState({ [name]: value });
    }

    handleSubmit(e) {
        e.preventDefault();

        this.setState({ submitted: true });
        const { username, password } = this.state;
        const { dispatch } = this.props;
        if (username && password) {
            dispatch(userActions.login(username, password));
        }
    }

    render() {
        const { user, loggedIn } = this.props;
        const { username, password, submitted } = this.state;
        return (
            <React.Fragment>
                {
                    loggedIn ? <Redirect to="/" /> :
                        (<div className="flex"><div className="login-widget">
                            <div className="row">
                                <div className="col-md-6 logo-wrapper">
                                    <div>
                                        <img src="/assets/images/cube_star_logo.png" alt="CUBE LOGO"/>
                                        <span className="comp-name">CUBE IO</span>
                                    </div>
                                    <div className="note">
                                        This is a Restricted Access beta. Read our Disclaimer for limitations
                                    </div>
                                </div>
                                <div className="col-md-6 sign-in-wrapper">
                                    <div className="pull-right" style={{width: "80%"}}>
                                        <h2 className="sign-in">Sign In</h2>
                                        <form name="form" onSubmit={this.handleSubmit}>
                                            <div className={'custom-fg form-group' + (submitted && !username ? ' has-error' : '')}>
                                                {/*<label htmlFor="username">Username</label>*/}
                                                <input type="text" placeholder="Enter User ID" className="form-control" name="username" value={username} onChange={this.handleChange} />
                                                {submitted && !username &&
                                                <div className="help-block">Username is required</div>
                                                }
                                            </div>
                                            <div className={'custom-fg form-group' + (submitted && !password ? ' has-error' : '')}>
                                                {/*<label htmlFor="password">Password</label>*/}
                                                <input type="password" placeholder="Enter Password" className="form-control" name="password" value={password} onChange={this.handleChange} />
                                                {submitted && !password &&
                                                <div className="help-block">Password is required</div>
                                                }
                                            </div>
                                            <div className="btn-link forgot-password">
                                                <Link to="/reset" >Forgot your password?</Link>
                                            </div>
                                            <div className="custom-fg form-group">
                                                <button className="btn btn-custom-sign-in width-100">Login</button>
                                                
                                            </div>
                                            <div className="custom-sign-in-divider" />
                                            <div className="create-account-container">
                                                <span>Don't have account?</span>
                                                <Link to="/register" className="btn-link create-account">Create Account</Link>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </div></div>)
                }
            </React.Fragment>

        );
    }
}

const mapStateToProps = (state) => {
    const { user, loggedIn } = state.authentication;
    return {user, loggedIn}
}

const connectedLoginPage = connect(mapStateToProps)(LoginPage);
export { connectedLoginPage as LoginPage }; 

/*{loggingIn && <img src="data:image/gif;base64,R0lGODlhEAAQAPIAAP///wAAAMLCwkJCQgAAAGJiYoKCgpKSkiH/C05FVFNDQVBFMi4wAwEAAAAh/hpDcmVhdGVkIHdpdGggYWpheGxvYWQuaW5mbwAh+QQJCgAAACwAAAAAEAAQAAADMwi63P4wyklrE2MIOggZnAdOmGYJRbExwroUmcG2LmDEwnHQLVsYOd2mBzkYDAdKa+dIAAAh+QQJCgAAACwAAAAAEAAQAAADNAi63P5OjCEgG4QMu7DmikRxQlFUYDEZIGBMRVsaqHwctXXf7WEYB4Ag1xjihkMZsiUkKhIAIfkECQoAAAAsAAAAABAAEAAAAzYIujIjK8pByJDMlFYvBoVjHA70GU7xSUJhmKtwHPAKzLO9HMaoKwJZ7Rf8AYPDDzKpZBqfvwQAIfkECQoAAAAsAAAAABAAEAAAAzMIumIlK8oyhpHsnFZfhYumCYUhDAQxRIdhHBGqRoKw0R8DYlJd8z0fMDgsGo/IpHI5TAAAIfkECQoAAAAsAAAAABAAEAAAAzIIunInK0rnZBTwGPNMgQwmdsNgXGJUlIWEuR5oWUIpz8pAEAMe6TwfwyYsGo/IpFKSAAAh+QQJCgAAACwAAAAAEAAQAAADMwi6IMKQORfjdOe82p4wGccc4CEuQradylesojEMBgsUc2G7sDX3lQGBMLAJibufbSlKAAAh+QQJCgAAACwAAAAAEAAQAAADMgi63P7wCRHZnFVdmgHu2nFwlWCI3WGc3TSWhUFGxTAUkGCbtgENBMJAEJsxgMLWzpEAACH5BAkKAAAALAAAAAAQABAAAAMyCLrc/jDKSatlQtScKdceCAjDII7HcQ4EMTCpyrCuUBjCYRgHVtqlAiB1YhiCnlsRkAAAOwAAAAAAAAAAAA==" />}*/
/*<Link to="/register" className="btn btn-link">Register</Link>*/