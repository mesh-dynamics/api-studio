import React, { Component } from "react";
import {connect} from "react-redux";
import "./SearchResults.css"
import {Link} from "react-router-dom";

class SearchResults extends Component {
    goToMovieDetails = (mov) => {
        const { history } = this.props;
        history.push(`/movie?film=${mov}`);
    };

    listMovies = () => {
        const {moviebook} = this.props;
        if (!moviebook.movieList || moviebook.movieList.length == 0 || !moviebook.movieList[0].film_id) {
            return (<div>No results found</div>)
        }
        let mList = moviebook.movieList.map(mov => {
            return (
                <div key={mov.film_id} className="border padding-20">
                    <div className="row">
                        <div className="col-md-2">
                            <div className="img-holder"></div>
                        </div>

                        <div className="col-md-10">
                            <h5 onClick={() => this.goToMovieDetails(mov.title)} className="background-grey cursor-pointer">{mov.title}</h5>
                            <div className="margin-bottom-10">
                                <h6>Actors</h6>
                                {mov.display_actors.map(actor => (<div key={actor}>{actor}</div>))}
                            </div>
                            <div>
                                <h6>Details</h6>
                                <div>Author: {mov.book_info.details.author}</div>
                                <div>Pages: {mov.book_info.details.pages}</div>
                                <div>Year: {mov.book_info.details.year}</div>
                                <div>Language: {mov.book_info.details.language}</div>
                                <div className="pull-right">
                                    <Link to={"/store"}>
                                        <span className="mb-btn">RENT</span>
                                    </Link>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            );
        });

        return mList;
    };

    render() {
        return (
            <div>
                <h3>Search Results:</h3>
                <div>
                    {this.listMovies()}
                </div>
            </div>
        );
    }
}

const mapStateToProps = (state) => {
    const moviebook = state.moviebook;
    return {
        moviebook
    }
};

const connectedSearchResults = connect(mapStateToProps)(SearchResults);

export default connectedSearchResults;
