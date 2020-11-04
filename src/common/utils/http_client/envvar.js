import _ from 'lodash';
import Mustache from "mustache";
import { store } from "../../helpers";

const getCurrentEnvironment = (environmentList, selectedEnvironment) => {
  return _.find(environmentList, { name: selectedEnvironment });
};

// returns current EnvVArs
const getCurrentEnvVars = () => {
  const { httpClient: { environmentList, selectedEnvironment } } = store.getState();
  const currentEnvironment = getCurrentEnvironment(environmentList, selectedEnvironment);

  // convert list of envvar objects to a map
  const currentEnvVars = currentEnvironment ? 
  Object.fromEntries(
    currentEnvironment.vars.map(({key, value}) => ([key, value]))
  ) : {};
  return currentEnvVars;
}

// returns a function that can be used to render the given environment vars on an input
const initEnvVars = () => {
  const currentEnvVars = getCurrentEnvVars();
  // return method to check and render Mustache template on an input
  return (input) => {
    if (!input) return input;

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
  let headersRendered = new Headers(), bodyRendered = "";

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
          headersRendered.append(renderEnvVars(pair[0]), renderEnvVars(pair[1]))
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