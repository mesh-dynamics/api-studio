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

import { store } from "../../helpers";
import { IMockConfig, IMockConfigValue, IServiceConfigDetails, IStoreState } from "../../reducers/state.types";
import { getCurrentMockConfig } from "./utils";

export interface IMockConfigUtilsProps {
  mockConfigList?: IMockConfig[];
  selectedMockConfig?: string;
}

export default class MockConfigUtils {
  private mockConfigList: IMockConfig[];
  private selectedMockConfig: string;
  constructor(props: IMockConfigUtilsProps = {}) {
    const { mockConfigList, selectedMockConfig } = props;

    if (!mockConfigList) {
      this.getCurrentStateMockConfigs();
    } else {
      this.mockConfigList = mockConfigList;
      this.selectedMockConfig = selectedMockConfig || "";
    }
  }

  private getCurrentStateMockConfigs() {
    const {
      httpClient: { mockConfigList, selectedMockConfig },
    } = store.getState() as IStoreState;
    this.mockConfigList = mockConfigList;
    this.selectedMockConfig = selectedMockConfig;
  }

  public getCurrentMockConfig() {
    return getCurrentMockConfig(this.mockConfigList, this.selectedMockConfig) as IMockConfigValue;
  }

  public getCurrentServiceConfigs(): IServiceConfigDetails[] {
    const currentMockConfig = this.getCurrentMockConfig();
    return currentMockConfig.serviceConfigs || [];
  }
  
  public getCurrentService(service: string): IServiceConfigDetails | undefined {
    return this.getCurrentMockConfig().serviceConfigs.find((config) => config.service == service);
  }
}
