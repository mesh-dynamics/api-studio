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

  public getCurrentServiceConfigs () : IServiceConfigDetails[] {
      const currentMockConfig = this.getCurrentMockConfig();
      if(currentMockConfig.name){
          return currentMockConfig.serviceConfigs;
      }
      return [];
  }
}
