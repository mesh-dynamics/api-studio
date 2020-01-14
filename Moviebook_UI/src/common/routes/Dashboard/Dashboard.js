import React, { Component } from 'react';
import movieList from "./test-data";
import "./Dashboard.css";
import {connect} from "react-redux";

class Dashboard extends Component {
    goToMovieDetails = (mov) => {
        const { history } = this.props;
        history.push(`/movie?film=${mov.title}`);
    };

    populateGrid = () => {
        let jsxContent = movieList.map((item, index) => {
            return (
                <div key={item.title + index} className="grid-content" onClick={() => this.goToMovieDetails(item)}>
                    <div className={"g-head"}>
                        {item.title}
                    </div>

                    <div className="g-body">
                        <div>Author: <b>{"Will S."}</b></div>
                        <div>Reviews: <b>{"12"}</b></div>
                    </div>
                </div>
            );
        });

        return jsxContent;
    };

    render() {
        return (
            <div style={{padding: "20px"}}>
                <div className="mov-grid">
                    {this.populateGrid()}
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

const connectedDashboard = connect(mapStateToProps)(Dashboard);

export default connectedDashboard;
