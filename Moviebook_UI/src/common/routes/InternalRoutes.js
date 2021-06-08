import React, { Component } from "react";
import { Route } from "react-router";
import Dashboard from "./Dashboard/Dashboard";
import SearchResults from "./SearchResults/SearchResults";
import Stores from "./Stores/Stores";
import MovieDetails from "./MovieDetails/MovieDetails";
import Container from "../components/Container";
// import Categories from "./Categories/Categories";
class RouterComponent extends Component {
  render() {
    return (
      <div style={{ margin: "0 50px", marginTop: "56px" }}>
        <Container />
        <Route exact key="Dashboard" path="/" component={Dashboard} />
        <Route
          exact
          key="SearchResults"
          path="/search_results"
          component={SearchResults}
        />
        <Route exact key="Stores" path="/store" component={Stores} />
        <Route exact key="Movie" path="/movie" component={MovieDetails} />
        {/* <Route exact key="Categories" path="/category" component={Categories} /> */}
      </div>
    );
  }
}

export default RouterComponent;
