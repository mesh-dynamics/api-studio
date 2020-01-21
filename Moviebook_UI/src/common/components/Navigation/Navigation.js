import React, { Component } from 'react'
import {Link} from "react-router-dom";
import {connect} from "react-redux";
import {moviebookActions} from "../../actions/moviebook.action";

class Navigation extends Component {
    constructor (props) {
        super(props)
        this.state = {
            keywords: ""
        };
    }

    searchMovie = () => {
        const {dispatch} = this.props;
        dispatch(moviebookActions.getMovieList(this.state.keywords));
    };

    changeSearchKey = (e) => {
        this.setState({keywords: e.target.value});
    };

    render() {
        return <div>
            <nav className="navbar fixed-top navbar-dark bg-dark">
                <a href="/" className="navbar-brand">MOVIEBOOK</a>
                <form className="form-inline">
                    <input value={this.state.keywords} onChange={this.changeSearchKey} className="form-control mr-sm-2" type="search" placeholder="Search" aria-label="Search" />
                    <Link to={"/search_results"}>
                        <button onClick={this.searchMovie} className="btn btn-outline-success my-2 my-sm-0" type="submit">Search</button>
                    </Link>
                </form>
            </nav>
        </div>
    }
}

const mapStateToProps = state => ({
    ...state
});

export default connect(mapStateToProps)(Navigation);
