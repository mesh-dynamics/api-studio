import React, { useState, useEffect, useRef } from "react";
import { connect } from "react-redux";
import IdleTimer from "react-idle-timer";
import Modal from "react-bootstrap/es/Modal";
import authActions from "../../actions/auth.actions";

const AuthTimer = (props) => {

    const { 
        dispatch, 
        auth: { 
            accessViolation,
            user: { 
                expires_in // this is in seconds
            }
        }
    } = props;

    const warningIntervalCount = 60 // in seconds;

    // expires_in is set 604800. Take of 1 min (60 s) off it
    // to display the warning modal and convert to milliseconds
    const idleTimeLimit = (expires_in - warningIntervalCount) * 1000; 

    const [showLogoutWarning, setShowLogoutWarning] = useState(false);

    const [shouldStartTimer, setShouldStartTimer] = useState(false);

    const [logoutWarningInterval, setLogoutWarningInterval] = useState(warningIntervalCount);

    const startTimer = () => {
        setShowLogoutWarning(true);
        setShouldStartTimer(true);
    };

    const resetTimer = () => {
        setShowLogoutWarning(false);
        setShouldStartTimer(false);
        setLogoutWarningInterval(warningIntervalCount);
    }

    useEffect(() => {
        let interval;
        
        if(shouldStartTimer && logoutWarningInterval !== 0) {
            interval = setInterval(() => { 
                setLogoutWarningInterval(logoutWarningInterval - 1) 
            }, 1000);
        }

        if(logoutWarningInterval === 0) {
            dispatch(authActions.logout());
        }

        return () => clearInterval(interval);
    }, [shouldStartTimer, logoutWarningInterval]);

    useEffect(() => {
        let timeout;
        if(accessViolation) {
            timeout = setTimeout(() => { dispatch(authActions.logout()) }, 5000);
        }

        return () => clearTimeout(timeout);
    }, [accessViolation])

    return (
        <div>
            <IdleTimer
                ref={ref => { this.idleTimer = ref }}
                element={document}
                onActive={resetTimer}
                onIdle={startTimer}
                debounce={250}
                timeout={idleTimeLimit} // expires_in - like 60 seconds
            />
            <Modal show={showLogoutWarning}>
                <Modal.Header>
                    <Modal.Title>Logout Warning</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    You have been idle for sometime and are about to be logged out in {logoutWarningInterval} seconds.
                    Press any key to continue your session.
                </Modal.Body>
            </Modal>
            <Modal show={accessViolation}>
                <Modal.Header>
                    <Modal.Title>Unauthorized Access</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    Your login has expired. Redirecting to login page.
                    <br />
                    <br />
                    Please login again to continue. 
                </Modal.Body>
            </Modal>
        </div>
    )
}

const mapStateToProps = (state) => ({
    auth: state.authentication,
});

export default connect(mapStateToProps)(AuthTimer);
