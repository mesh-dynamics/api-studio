import React from 'react';
import ChangePassword from './changePassword';


const Account = function (props) {
    return <div className="flex">
        <div className="login-widget">
            <div className="row vertical-align-middle">
                <ChangePassword />
            </div>
        </div>
    </div>
}
export default Account;