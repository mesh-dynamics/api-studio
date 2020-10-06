import { generateRunId } from "../utils/http_client/utils";
import { ipcRenderer } from "./ipc-renderer";
import {store} from "../../common/helpers"

const setDefaultMockContext = () => {
  console.log("Setting default mock context...");

  if (PLATFORM_ELECTRON) {
    const {
      httpClient: { userHistoryCollection },
      cube: { selectedApp },
    } = store.getState();

    if (!userHistoryCollection) {
      console.warn("User history not available, skipping setting default proxy config");
      return;
    }

    const runId = generateRunId();
    const user = JSON.parse(localStorage.getItem("user"));
    const customerId = user.customer_name;
    const mockContext = {
      collectionId: userHistoryCollection.collec, // where to store the mocked/live captured requests (new/existing collection)
      recordingCollectionId: userHistoryCollection.collec, // configurable for preferred collection or all collections or history (default)
      recordingId: userHistoryCollection.id, // where to store the mocked/live captured requests (new/existing collection)
      traceId: "NA",
      selectedApp, // selected app from the app list
      customerName: customerId, // constant
      runId: runId, // timestamp
      // config: mockConfig, // todo hotfix/disable-live-proxy // default + custom config
      spanId: "NA",
    };

    console.log("Setting default mock context: ", mockContext);

    ipcRenderer.send("mock_context_change", mockContext);
  }
};

export { setDefaultMockContext };
