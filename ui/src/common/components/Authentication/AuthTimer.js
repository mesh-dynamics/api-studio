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
        timestamp,
        expires_in, // this is in seconds
      },
    },
  } = props;

  const idleTimerRef = useRef();

  const warningIntervalCount = 60; // in seconds;
  const refreshBeforeExpiryInterval = 10 * 60; //in seconds.

  // expires_in is set 604800. Take of 1 min (60 s) off it
  // to display the warning modal and convert to milliseconds
  const idleTimeLimit = (expires_in - warningIntervalCount) * 1000;

  const [showLogoutWarning, setShowLogoutWarning] = useState(false);

  const [shouldStartTimer, setShouldStartTimer] = useState(false);

  const [logoutWarningInterval, setLogoutWarningInterval] = useState(
    warningIntervalCount
  );

  const startTimer = () => {
    setShowLogoutWarning(true);
    setShouldStartTimer(true);
  };

  const resetTimer = () => {
    setShowLogoutWarning(false);
    setShouldStartTimer(false);
    setLogoutWarningInterval(warningIntervalCount);
  };

  useEffect(() => {
    let interval;

    if (shouldStartTimer && logoutWarningInterval !== 0) {
      interval = setInterval(() => {
        setLogoutWarningInterval(logoutWarningInterval - 1);
      }, 1000);
    }

    if (logoutWarningInterval === 0) {
      dispatch(authActions.logout());
    }

    return () => clearInterval(interval);
  }, [shouldStartTimer, logoutWarningInterval]);

  useEffect(() => {
    let timeout;
    if (accessViolation) {
      timeout = setTimeout(() => {
        dispatch(authActions.logout());
      }, 5000);
    }

    return () => clearTimeout(timeout);
  }, [accessViolation]);

  useEffect(() => {
    //This is to get new accesstoken using refresh token before expiry
    let timeout;

    if (expires_in) {
      idleTimerRef && idleTimerRef.current && idleTimerRef.current.reset(); // If refresh token is successful, then reset the idle timer.
      const tokenCreatedBeforeMilliseconds = new Date() - new Date(timestamp);
      const beforeExpiryInterval =
        (expires_in - refreshBeforeExpiryInterval) * 1000;
      const refreshTokenAfterMillisecond =
        beforeExpiryInterval - tokenCreatedBeforeMilliseconds;

      // setTimeout accepts negative values of interval. In case of < 4ms, it will execute in 4ms.
      timeout = setTimeout(() => {
        dispatch(authActions.refreshToken());
      }, refreshTokenAfterMillisecond);
    }
    return () => clearTimeout(timeout);
  }, [timestamp, expires_in]);

  useEffect(() => {
    const minTimeToElapsedAfterLogin = 10 * 60 * 1000; //10 minutes
    let timeout;

    //On opening Devtool first time, refresh token. expires_in doesn't changes, but only refresh if this is present.
    if (expires_in) {
      const tokenCreatedBeforeMilliseconds = new Date() - new Date(timestamp);
      if (tokenCreatedBeforeMilliseconds > minTimeToElapsedAfterLogin) {
        timeout = setTimeout(() => {
          dispatch(authActions.refreshToken());
        }, 0);
      }
    }
    return () => clearTimeout(timeout);
  }, [expires_in]);

  return (
    <div>
      <IdleTimer
        ref={idleTimerRef}
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
          {`You have been idle for sometime and are about to be logged out in ${logoutWarningInterval} seconds. Press any key to continue your session.`}
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
  );
};

const mapStateToProps = (state) => ({
  auth: state.authentication,
});

export default connect(mapStateToProps)(AuthTimer);
