import React, { Component } from "react";
import {connect} from "react-redux";

class Stores extends Component {

    render() {
        return (
            <div>
                <h3>Stores:</h3>
                <div>
                    TO Do
                </div>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const moviebook = state.moviebook;
    return {
        moviebook
    }
}

const connectedStores = connect(mapStateToProps)(Stores);

export default connectedStores;
