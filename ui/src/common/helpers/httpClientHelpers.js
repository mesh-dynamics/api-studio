import { generateRunId, getCurrentMockConfig, getTracerForCurrentApp } from "../utils/http_client/utils";
import { ipcRenderer } from "./ipc-renderer";
import { store } from "../../common/helpers"

const setDefaultMockContext = (args) => {
  let lookupCollection = args?.lookupCollection, 
  saveToCollection = args?.saveToCollection, 
  mockConfigName = args?.mockConfigName,
  mockConfigList = args?.mockConfigList;

  console.log("Setting default mock context...");

  if (PLATFORM_ELECTRON) {
    const {
      httpClient: { userHistoryCollection, mockContextLookupCollection, mockContextSaveToCollection, mockConfigList: mockConfigListFromStore, selectedMockConfig },
      cube: { selectedApp },
      authentication: { user: { customer_name: customerId } }
    } = store.getState();

    if (!userHistoryCollection) {
      console.warn("User history not available, skipping setting default proxy config");
      return;
    }

    mockConfigList = mockConfigList || mockConfigListFromStore;

    mockConfigName = mockConfigName || selectedMockConfig;
    const mockConfig = getCurrentMockConfig(mockConfigList, mockConfigName);

    const runId = generateRunId();
    
    const collectionId = saveToCollection?.collec || (mockContextSaveToCollection?.collec || userHistoryCollection.collec);
    const recordingId = saveToCollection?.id || (mockContextSaveToCollection?.id || userHistoryCollection.id);

    const tracer = getTracerForCurrentApp()

    const mockContext = {
      collectionId: collectionId, // where to store the mocked captured requests [mockWithRunId]
      recordingId: recordingId, // where to store the live captured requests [storeReqResp]
      recordingCollectionId: lookupCollection || (mockContextLookupCollection || userHistoryCollection.collec), // configurable for preferred collection or all collections or history (default)
      traceId: "",
      selectedApp, // selected app from the app list
      customerName: customerId, // constant
      runId: runId, // timestamp
      config: mockConfig, // mock config
      parentSpanId: "",
      tracer: tracer,
      strictMock: false,
      replayInstance: "",
    };

    console.log("Setting default mock context: ", mockContext);

    ipcRenderer.send("mock_context_change", mockContext);
  }
};

const setStrictMock = (strictMock, replayInstance="", replayCollection="") => {
  if(PLATFORM_ELECTRON) {
    const args = {
        strictMock,
        replayInstance,
        replayCollection,
    }

    console.log("Setting strict mocking: ", args)
    ipcRenderer.send('set_strict_mock', args);
  }
}

export { setDefaultMockContext, setStrictMock };