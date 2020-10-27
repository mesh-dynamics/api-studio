import "core-js/stable";
import "regenerator-runtime/runtime";
import React from "react";
import ReactDOM from "react-dom";
import { PersistGate } from "redux-persist/integration/react";
import { setupElectronListeners } from "./electron/client/electron-listeners";
import Root from "./common/components/Root";
import { store, persistor } from "./common/helpers/store";
import "./styles/bootstrap/dist/css/bootstrap.min.css";
import "./styles/font-awesome-5.5.0/css/all.min.css";
import "./styles/index.css";
import "./styles/cube.css";
import { Provider } from "react-redux";

setupElectronListeners();

ReactDOM.render(
    <Provider store={store}>
        <PersistGate loading={null} persistor={persistor}>
            <Root />
        </PersistGate>
    </Provider>,
    document.getElementById( "root" )
);
