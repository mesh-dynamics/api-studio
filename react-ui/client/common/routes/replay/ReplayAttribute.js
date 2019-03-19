import React, { Component } from 'react';
import './ReplayAttribute.css'

class ReplayAttribute extends Component {
    render() {
        return (
            <div>
                <div></div>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
        user, cube
    }
}



const connectedReplayAttribute = connect(mapStateToProps)(ReplayAttribute);
export default connectedReplayAttribute
