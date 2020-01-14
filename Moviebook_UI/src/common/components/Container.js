import React, { Component } from 'react';
import { connect } from 'react-redux';
import {moviebookActions} from "../actions";
import RoterComponent from "../routes";
import Navigation from "./Navigation/Navigation";

class Container extends Component {
    render() {
        return (
            <div className="body">
                <Navigation />
                <RoterComponent />
            </div>
        );
    }

    simpleAction = (event) => {
        const {dispatch} = this.props;
        dispatch(moviebookActions.getMovieList());
    }
}

const mapStateToProps = state => ({
    ...state
});

export default connect(mapStateToProps)(Container);
