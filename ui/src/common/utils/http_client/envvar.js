/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import _ from 'lodash';
import Mustache from "mustache";
import { store } from "../../helpers";

const getCurrentEnvironment = (environmentList, selectedEnvironment) => {
  return _.find(environmentList, { name: selectedEnvironment });
};

// returns current EnvVArs
//There is an issue if store is fetched in this way. You can use this function (or any caller of this function) in reducer.
const getCurrentEnvVars = () => {
  const { httpClient: { environmentList, selectedEnvironment, contextMap } } = store.getState();
  const currentEnvironment = getCurrentEnvironment(environmentList, selectedEnvironment);

  // convert list of envvar objects to a map
  const currentEnvVars = currentEnvironment ? 
  Object.fromEntries(
    currentEnvironment.vars.map(({key, value}) => ([key, value]))
  ) : {};
  const sessionVars = Object.fromEntries(
    Object.entries(contextMap).map(([key, valObj]) => ([key, valObj.value]))
  )
  return {...currentEnvVars, ...sessionVars};
}

// returns a function that can be used to render the given environment vars on an input
const initEnvVars = () => {
  const currentEnvVars = getCurrentEnvVars();
  // return method to check and render Mustache template on an input
  return (input) => {
    if (!input) return input;
    try{
      // get variables in the input string
      let inputVariables = Mustache.parse(input)
              .filter(v => (v[0]==='name') || v[0]==='&' || v[0]==='#')
              .map(v => v[1]);

      // check for the presence of variables in the environment
      inputVariables.forEach((inputVariable) => {
          if (!currentEnvVars.hasOwnProperty(inputVariable)) {
              throw new Error("The variable '" + inputVariable + "' is not defined in the current environment. \nPlease check your environment and the variables being used.")
          }
      })

      return Mustache.render(input, currentEnvVars);
    }
    catch(error){
      console.error("Error while parsing ", error);
    }
    return input;
  }
}

const getRenderEnvVars = () => {
  return initEnvVars();
}

const applyEnvVarsToUrl = (url) => {
  const renderEnvVars = initEnvVars();
  return renderEnvVars(url);
}

const applyEnvVars = (httpRequestURL, httpRequestQueryStringParams, fetchConfig) => {
  const headers = fetchConfig.headers;
  const body = fetchConfig.body;
  let headersRendered = {}, bodyRendered = "";

  // define method to check and render Mustache template
  const renderEnvVars = initEnvVars();

  const httpRequestURLRendered = renderEnvVars(httpRequestURL);

  const httpRequestQueryStringParamsRendered = Object.fromEntries(
      Object.entries(httpRequestQueryStringParams)
          .map(
              ([key, value]) => [renderEnvVars(key), renderEnvVars(value)]
          )
  );

  if (headers) {
      for (let pair of headers.entries()) {
          headersRendered[renderEnvVars(pair[0])] = renderEnvVars(pair[1])
      }
  }

  if (body instanceof URLSearchParams) {
      bodyRendered = new URLSearchParams();
      for (let pair of body.entries()) {
          bodyRendered.append(renderEnvVars(pair[0]), renderEnvVars(pair[1]))
      }
  } else {
      // string
      bodyRendered = renderEnvVars(body)
  }

  const fetchConfigRendered = {
      method: fetchConfig.method,
      headers: headersRendered,
      body: bodyRendered,
  }

  return [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered]
}


export {
  getCurrentEnvironment,
  getCurrentEnvVars,
  getRenderEnvVars,
  applyEnvVars,
  applyEnvVarsToUrl,
}