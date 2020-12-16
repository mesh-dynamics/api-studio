import React, { useState } from "react";
import { connect } from "react-redux";
import { InputGroup, FormControl, Button } from "react-bootstrap";
import { authActions } from "../../actions";

import "./LoginPage.css";

const EMPTY_STRING = "";

// MARY.SMITH@sakilacustomer.org
// password123@
const LoginPage = (props) => {
	const {
		authentication: { messages },
		login,
	} = props;

	const [username, setUsername] = useState("MARY.SMITH@sakilacustomer.org");

	const [password, setPassword] = useState("password123@");

	const [loginErrorMessage, setLoginErrorMessage] = useState(EMPTY_STRING);

	const credentialsPopulated = () => {
		if (username === EMPTY_STRING) {
			setLoginErrorMessage("Username cannot be empty.");
			return false;
		}

		if (password === EMPTY_STRING) {
			setLoginErrorMessage("Password cannot be empty.");
			return false;
		}

		return true;
	};

	const handleLoginClick = () => {
		setLoginErrorMessage(EMPTY_STRING);
		if (credentialsPopulated()) {
			login(username, password);
		}
	};

	return (
		<div className="login-root">
			<div className="login-container">
				<div className="login-input-section">
					<span className="login-header">
						Welcome to Moviebook App!
					</span>
					<div className="login-input-wrapper">
						<InputGroup className="mb-3">
							<InputGroup.Prepend>
								<InputGroup.Text id="inputGroup-sizing-sm">
									Username
								</InputGroup.Text>
							</InputGroup.Prepend>
							<FormControl
								onChange={(event) =>
									setUsername(event.target.value)
								}
								value={username}
								placeholder="Email or User Id"
								aria-label="Email or User Id"
								aria-describedby="basic-addon2"
							/>
						</InputGroup>
						<InputGroup className="mb-3">
							<InputGroup.Prepend>
								<InputGroup.Text
									className="login-password-text"
									id="inputGroup-sizing-sm"
								>
									Password
								</InputGroup.Text>
							</InputGroup.Prepend>
							<FormControl
								onChange={(event) =>
									setPassword(event.target.value)
								}
								value={password}
								type="password"
								placeholder="Password"
								aria-label="Password"
								aria-describedby="basic-addon2"
							/>
						</InputGroup>
						{loginErrorMessage && (
							<div className="login-error-message">
								<span>{loginErrorMessage}</span>
							</div>
						)}
						{messages.length > 0 && (
							<div className="login-error-message">
								<span>{messages[0]}</span>
							</div>
						)}
						<Button onClick={handleLoginClick} variant="primary">
							Login
						</Button>
					</div>
				</div>
			</div>
		</div>
	);
};

const mapStateToProps = (state) => ({
	authentication: state.authentication,
});

const mapDispatchToProps = (dispatch) => ({
	login: (username, password) =>
		dispatch(authActions.login(username, password)),
});

export default connect(mapStateToProps, mapDispatchToProps)(LoginPage);
