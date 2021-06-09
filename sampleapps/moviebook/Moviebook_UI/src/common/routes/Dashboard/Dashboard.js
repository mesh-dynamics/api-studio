import React, { Component } from "react";
import "./Dashboard.css";
import { connect } from "react-redux";
import { moviebookActions } from "../../actions";

class Dashboard extends Component {
  componentDidMount() {
    this.props.getMovieList();
  }

  goToMovieDetails = (mov) => {
    const { history } = this.props;
    history.push(`/movie?film=${mov.title}`);
  };

  populateGrid = () => {
    const {
      moviebook: { movieList },
    } = this.props;

    let jsxContent = movieList.map((item, index) => {
      return (
        <div
          key={item.title + index}
          className="grid-content"
          onClick={() => this.goToMovieDetails(item)}
        >
          <div className={"g-head"}>{item.title}</div>

          <div className="g-body">
            <div>
              Author: <b>{"Will S."}</b>
            </div>
            <div>
              Reviews: <b>{"12"}</b>
            </div>
          </div>
        </div>
      );
    });

    return jsxContent;
  };

  render() {
    return (
      <div style={{ padding: "20px" }}>
        <div className="mov-grid">{this.populateGrid()}</div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  moviebook: state.moviebook,
});

const mapDispatchToProps = (dispatch) => ({
  getMovieList: () => dispatch(moviebookActions.getMovieList()),
});

export default connect(mapStateToProps, mapDispatchToProps)(Dashboard);
