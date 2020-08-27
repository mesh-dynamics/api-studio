import "core-js/stable";
import "regenerator-runtime/runtime";
import React from "react";
import ReactDOM from "react-dom";
import { setupElectronListeners } from './electron/client/electron-listeners';
import Root from "./common/components/Root";
import { store } from './common/helpers';
import "./styles/bootstrap/dist/css/bootstrap.min.css";
import "./styles/font-awesome-5.5.0/css/all.min.css";
import "./styles/index.css";
import "./styles/cube.css";

setupElectronListeners();

ReactDOM.render(
    <Root store={store} />,
    document.getElementById( "root" )
);
