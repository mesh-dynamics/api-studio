import React, { Component } from 'react';
import { connect } from 'react-redux';

class AppSelection extends Component {
    constructor(props) {
        super(props);
    }

    render() {

    }
}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
        user, cube
    }
}



const connectedAppSelection = connect(mapStateToProps)(AppSelection);
export default connectedAppSelection