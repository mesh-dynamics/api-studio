import React from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import PropTypes from "prop-types";
import { Provider, connect } from "react-redux";
import { history } from './common/helpers';
import Container from "./common/components/Container";

/*function App() {
  return (
      <Provider store={store}>
        <Router history={history}>
          <div className="App">
            <header className="App-header">
              <img src={logo} className="App-logo" alt="logo" />
              <p>
                Edit <code>src/App.js</code> and save to reload.
              </p>
              <a
                className="App-link"
                href="https://reactjs.org"
                target="_blank"
                rel="noopener noreferrer"
              >
                Learn React
              </a>
            </header>
          </div>
        </Router>
      </Provider>
  );
}

export default App;*/

const App = ( { store } ) => (
    <Provider store={store}>
      <Router history={history}>
        <div className="App">
          <Container />
        </div>
      </Router>
    </Provider>
);

App.propTypes = {
  store: PropTypes.oneOfType( [
    PropTypes.func.isRequired,
    PropTypes.object.isRequired,
  ] ).isRequired,
};


export default App;
