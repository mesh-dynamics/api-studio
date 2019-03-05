import React, { Component } from 'react'
// import { Row, Col, Clearfix, Button, Glyphicon } from 'react-bootstrap'
// import { XPanel, PageTitle } from '../../components'
import { connect } from 'react-redux';
// import ServiceGraph from './ServiceGraph';

class analysis extends Component {
    render() {
        return <div>hi</div>
    }
}

function mapStateToProps(state) {
    const { user } = state.authentication;

    return {
        user,
    }
}

const connectedReplayAnalysis = connect(mapStateToProps)(analysis);

export default connectedReplayAnalysis