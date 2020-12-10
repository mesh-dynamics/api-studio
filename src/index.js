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
import config from "./common/config";
import { ipcRenderer } from "./common/helpers/ipc-renderer";
import MDCircleLogo from '../public/assets/images/md-circle-logo.png';

setupElectronListeners();

const Loading = () => {
  return <div className="flex">
    <div className="login-widget">
      <div className="row vertical-align-middle">
        <div className="col-md-6 logo-wrapper">
          <div>
            <img src={MDCircleLogo} alt="MD LOGO" />
            <span className="comp-name">Mesh Dynamics</span>
          </div>
          <div className="note">
            This is a Restricted Access beta. Read our Disclaimer for
            limitations
          </div>
        </div>
        <div className="col-md-6">
            <div><center>Starting...</center></div>
        </div>
      </div>
    </div>
  </div>;
};

const onBeforeLift = () => {
  if (PLATFORM_ELECTRON) {
    return new Promise((resolve) => {
      ipcRenderer.send("get_config");
      ipcRenderer.on("get_config", (event, appConfig) => {
        ipcRenderer.removeAllListeners("get_config");

        config.apiBaseUrl = `${appConfig.domain}/api`;
        config.recordBaseUrl = `${appConfig.domain}/api/cs`;
        config.replayBaseUrl = `${appConfig.domain}/api/rs`;
        config.analyzeBaseUrl = `${appConfig.domain}/api/as`;

        console.log("Config set to ", config);
        resolve();
      });
    });
  }
  return Promise.resolve();
};

ReactDOM.render(
  <Provider store={store}>
    <PersistGate
      loading={<Loading />}
      persistor={persistor}
      onBeforeLift={onBeforeLift}
    >
      <Root />
    </PersistGate>
  </Provider>,
  document.getElementById("root")
);
