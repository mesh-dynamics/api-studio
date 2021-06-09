import "core-js/stable";
import "regenerator-runtime/runtime";
import React from "react";
import ReactDOM from "react-dom";
import Settings from "./components/Settings";
import "../../styles/cube.css";
import "./styles/settings.css";

ReactDOM.render(
    <Settings />,
    document.getElementById( "settings" )
);