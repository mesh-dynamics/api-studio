import config from "../config";
import api from "../api";
import { AxiosRequestConfig } from "axios";
// import _ from 'lodash';

export interface IDownloadRuleArgs {
  customerId: string;
  app: string;
  eventTypes: any[];
  collections: any[];
  version: string;
}

export interface IUpoadRuleArgs {
  customerId: string;
  app: string;
  version: string;
  formData: FormData;
  apiConfig: AxiosRequestConfig;
}

//Start: Context Propagation Section

const getPotentialDynamicInjectionConfigs = async (
  args: IDownloadRuleArgs,
  apiConfig: AxiosRequestConfig = {}
) => {
  let apiEventURL = `${config.replayBaseUrl}/getPotentialDynamicInjectionConfigs`;

  let body = {
    customerId: args.customerId,
    app: args.app,
    eventTypes: args.eventTypes,
    collections: args.collections,
  };

  try {
    return api.post(apiEventURL, body, apiConfig);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};

const saveDynamicInjectionConfigFromJson = async (uploadArgs: IUpoadRuleArgs) => {
  let apiEventURL = `${config.replayBaseUrl}/saveDynamicInjectionConfigFromJson`;

  try {
    return api.post(apiEventURL, uploadArgs.formData, uploadArgs.apiConfig);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};
const saveDynamicInjectionConfigFromCsv = async (
  uploadArgs: IUpoadRuleArgs
) => {
  let apiEventURL = `${config.replayBaseUrl}/saveDynamicInjectionConfigFromCsv/${uploadArgs.customerId}/${uploadArgs.app}/${uploadArgs.version}`;

  try {
    return api.post(apiEventURL, uploadArgs.formData, uploadArgs.apiConfig);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};

const getDynamicInjectionConfig = async (
  args: IDownloadRuleArgs
) => {
  let apiEventURL = `${config.replayBaseUrl}/getDynamicInjectionConfig/${args.customerId}/${args.app}/${args.version}`;

  try {
    return api.get(apiEventURL);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};

//End: Context Propagation Section

export const configsService = {
  getPotentialDynamicInjectionConfigs,
  saveDynamicInjectionConfigFromCsv,
  getDynamicInjectionConfig,
  saveDynamicInjectionConfigFromJson,
};
