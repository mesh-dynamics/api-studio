import React, { useEffect } from "react";
import { Switch, Route } from "react-router-dom";
import { connect } from "react-redux";
import { Redirect } from "react-router";
import SignIn from "./SignIn";
import SignUp from "./SignUp";
import ResetLink from "./ResetLink";
import ResetPassword from "./ResetPassword";
import ResendActivation from "./ResendActivation";
import "./Login.css";
import MDCircleLogo from "../../../../public/assets/images/md-circle-logo.png";
import authActions from "../../actions/auth.actions";

const LoginPage = function (props) {
  const {
    authentication: { loggedIn },
    dispatch,
  } = props;
  const isLocalhost = config.apiBaseUrl.includes("//localhost");
  useEffect(() => {
    if (isLocalhost && !loggedIn) {
      dispatch(authActions.localhostLogin());
    }
  }, [loggedIn]);
  return (
    <div>
      {loggedIn ? (
        <Redirect to="/" />
      ) : (
        <div className="flex">
          <div className="login-widget">
            <div className="row vertical-align-middle">
              <div className="col-md-6 logo-wrapper">
                <div>
                  <img src={MDCircleLogo} alt="MD LOGO" />
                  <span className="comp-name">Mesh Dynamics</span>
                </div>
                <div className="note">This is a Restricted Access beta. Read our Disclaimer for limitations</div>
              </div>
              <div className="col-md-6 sign-in-wrapper">
                {isLocalhost ? (
                  <>
                    <div style={{ width: "100%", fontSize: "15px" }}>
                      Trying to login in local mode. <i className="fa fa-spinner fa-spin" />
                    </div>
                  </>
                ) : (
                  <Switch>
                    <Route exact path="/login" component={SignIn} />
                    <Route path="/login/register" component={SignUp} />
                    <Route path="/login/reset-link" component={ResetLink} />
                    <Route path="/login/resend-activation-link" component={ResendActivation} />
                    <Route path="/login/reset-password" component={ResetPassword} />
                    <Route path="/login/*" component={SignIn} />
                  </Switch>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const mapStateToProps = (state) => ({
  authentication: state.authentication,
});

const connectedLoginPage = connect(mapStateToProps)(LoginPage);
export { connectedLoginPage as LoginPage };
