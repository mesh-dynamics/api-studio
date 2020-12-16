import React, { Component } from "react";
import { connect } from "react-redux";
import Navigation from "./Navigation/Navigation";
class Container extends Component {
  render() {
    return (
      <div className="body">
        <Navigation />
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  ...state,
});

export default connect(mapStateToProps)(Container);

// import { moviebookActions } from "../actions";
// import { RouterComponent } from "../routes";
{
  /* <RouterComponent /> */
}
