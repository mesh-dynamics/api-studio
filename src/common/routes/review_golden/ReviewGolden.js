import React, { Component } from 'react';
import {connect} from "react-redux";

class ReviewGolden extends Component {
    render() {
        return (<div className="content-wrapper">Review Golden Updates</div>)
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedReviewGolden = connect(mapStateToProps)(ReviewGolden);

export default connectedReviewGolden;
