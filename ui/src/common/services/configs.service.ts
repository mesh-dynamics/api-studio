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

import config from "../config";
import api from "../api";
import { AxiosRequestConfig } from "axios";
// import _ from 'lodash';

export interface IDownloadRuleArgs {
  customerId: string;
  app: string;
  eventTypes: any[];
  collections: string[];
  version: string;
  sortingOrder?: {
    "timestamp": boolean
  };
  services?: string[];
}

export interface IUpoadRuleArgs {
  customerId: string;
  app: string;
  version: string;
  formData: FormData;
  apiConfig: AxiosRequestConfig;
  name: string;
}
export interface IProtoDescriptorFileUploadArgs{
  customerId: string;
  app: string;
  formData: FormData;
  appendExisting:string
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
    sortingOrder: args.sortingOrder,
    services: args.services,
  };

  try {
    return api.post(apiEventURL, body, apiConfig);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};

const saveDynamicInjectionConfigFromJson = async (
  uploadArgs: IUpoadRuleArgs
) => {
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

const getDynamicInjectionConfig = async (args: IDownloadRuleArgs) => {
  let apiEventURL = `${config.replayBaseUrl}/getDynamicInjectionConfig/${args.customerId}/${args.app}/${args.version}`;

  try {
    return api.get(apiEventURL);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};

//End: Context Propagation Section

//Start: Grpc Configuration Section
const protoDescriptorFileUpload = async(args: IProtoDescriptorFileUploadArgs ) =>{
  let apiURL = `${config.recordBaseUrl}/protoDescriptorFileUpload/${args.customerId}/${args.app}?appendExisting=${args.appendExisting}`;

  try {
    return api.post(apiURL, args.formData);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
}
const protoDescriptorCompiledFileUpload = async(args: IProtoDescriptorFileUploadArgs ) =>{
  let apiURL = `${config.recordBaseUrl}/protoDescriptorCompiledFileUpload/${args.customerId}/${args.app}`;

  try {
    return api.post(apiURL, args.formData);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
}
//End: Grpc Configuration Section


//Start: API Token Section
const getApiToken = async() =>{
  let apiURL = `${config.apiBaseUrl}/access_token`;

  try {
    return api.post(apiURL);
  } catch (e) {
    console.error("Error fetching API Token");
    throw e;
  }
}
//End: API Token Section

//Start: Comparison Rules

const getComparisonRulesConfig = async (replayId: string) => { 
  let apiEventURL = `${config.analyzeBaseUrl}/learnComparisonRules/?replayId=${replayId}`;

  try {
    return api.get(apiEventURL);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};

const getTemplateSet = async(customerId: string, app: string, version: string) =>{
  let apiEventURL = `${config.analyzeBaseUrl}/getTemplateSet/${customerId}/${app}?templateSetName=${version}`;

  try {
    return api.get(apiEventURL);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
}


const saveComparisonRulesConfigFromJson = async (
  uploadArgs: IUpoadRuleArgs
) => {
  let apiEventURL = `${config.analyzeBaseUrl}/saveTemplateSet/${uploadArgs.customerId}/${uploadArgs.app}/${uploadArgs.name}`;

  try {
    return api.post(apiEventURL, uploadArgs.formData, uploadArgs.apiConfig);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};
const saveComparisonRulesConfigFromCsv = async (
  uploadArgs: IUpoadRuleArgs
) => {
  let apiEventURL = `${config.analyzeBaseUrl}/learnComparisonRules/${uploadArgs.customerId}/${uploadArgs.app}/${uploadArgs.name}`;

  try {
    return api.post(apiEventURL, uploadArgs.formData, uploadArgs.apiConfig);
  } catch (e) {
    console.error("Error fetching API Event data");
    throw e;
  }
};

//End: Comparison Rules


export const configsService = {
  getPotentialDynamicInjectionConfigs,
  saveDynamicInjectionConfigFromCsv,
  getDynamicInjectionConfig,
  saveDynamicInjectionConfigFromJson,
  protoDescriptorFileUpload,
  getApiToken,
  getComparisonRulesConfig,
  getTemplateSet,
  saveComparisonRulesConfigFromJson,
  saveComparisonRulesConfigFromCsv,
  protoDescriptorCompiledFileUpload
};
