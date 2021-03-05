/*
Comments:
1. This can be Date also but always retrieved as string from persist
2. Verify actual data type
3. This is stored as number[], but should be Date


*/

// API Catalog
export interface ILoginCredentials {
  username: string;
  password: string;
}

export interface IUserAuthDetails {
  access_token: string;
  customer_name: string;
  expires_in: number;
  refresh_token: string;
  roles: string[];
  timestamp: string; //1
  token_type: string;
  username: string;
}

export interface IAuthenticationState {
  accessViolation: boolean;
  credentials: ILoginCredentials;
  isFetching: boolean;
  loggedIn: boolean;
  messages: string[]; //2
  role: string;
  user: IUserAuthDetails;
  rememberMe: boolean;
}

export interface IApiCatalogTableState {
  pageSize: number;
  currentPage: number;
  filterData: any; //2
  oldPagesData: any[]; //2
}

export interface IApiCatalogCompareRequest {
  parentRequest: IApiTrace;
  outgoingRequests: IApiTrace[];
  parentReqId: string;
  instance: string;
  checked: boolean;
}

export interface IApiCatalogState {
  apiCatalogTableState: IApiCatalogTableState;
  apiFacets: any; //2
  apiPaths: any[]; //2
  apiTrace: any; //2
  apiTraceLoading: boolean;
  collectionList: ICollectionDetails[];
  lastCollectionListLoaded: string;
  compareRequests: IApiCatalogCompareRequest[];
  diffRequestLeft: any; //2
  diffRequestRight: any; //2
  diffResponseLeft: any; //2
  diffResponseRight: any; //2
  endTime: string | null; //1
  goldenList: ICollectionDetails[];
  lastGoldenListLoaded: string;
  httpClientRequestIds: any; //2
  instances: any[]; //2
  resizedColumns: any[]; //2
  selectedApiPath: string;
  selectedCaptureApi: string;
  selectedCaptureInstance: string;
  selectedCaptureService: string;
  selectedCollection: string;
  selectedCollectionApi: string;
  selectedCollectionService: string;
  selectedGolden: string;
  selectedGoldenApi: string;
  selectedGoldenService: string;
  selectedInstance: string;
  selectedService: string;
  selectedSource: string;
  services: any[]; //2
  startTime: string| null; //1
  goldenCollectionLoading: boolean;
}

// Cube State
export interface IAppDetailsCustomer {
  createdAt: number[]; // 3
  domainUrls: string[];
  email: string;
  id: number;
  name: string;
  updatedAt: number[]; // 3
}

export interface IAppInfo {
  createdAt: number[]; // 3
  name: string;
  updatedAt: number[]; // 3
  id: number;
  customer: IAppDetailsCustomer;
  displayName: string;
  userid: string;
}

export interface IAppDetails {
  //From actual API call some data has been moved to 'app'.
  data: string;
  fileName: string;
  fileType: string;
  app: IAppInfo;
}

export interface IServiceDetails {
  app: IAppDetails;
  createdAt: number[]; // 3
  id: number;
  name: string;
  serviceGroup: IServiceDetails; //It seems to be this one, // 2
  updatedAt: number[]; // 3
}
export interface ICubeGraphData {
  fromService: IServiceDetails;
  id: number;
  toService: IServiceDetails;
  app: IAppDetails;
}

export interface IInstanceDetails {
  app: IAppDetails;
  createdAt: number[]; // 3
  gatewayEndpoint: string;
  id: number;
  loggingURL: string;
  name: string;
}

export interface ITestConfigGatewayService {
  name: string;
}

export interface ITestConfigDetails {
  appId: number;
  appName: string;
  createdAt: number[]; // 3
  description: any; //2
  dynamicInjectionConfigVersion: any; //2
  emailId: string;
  id: number;
  maxRunTimeMin: number;
  mocks: string[];
  paths: string[];
  slackId: any; //2
  tag: any; //2
  testConfigName: string;
  testIntermediateServices: any[]; //2
  testMockServices: string[];
  testPaths: string[];
  testServices: any; //2
  updatedAt: number[]; // 3
  gatewayService: ITestConfigGatewayService;
  criteria: string;
  gatewayServiceName: string;
}

export interface ITimelineDataResult {
  app: string;
  mockReqNotMatched: number;
  path: string;
  recReqNotMatched: number;
  replayId: string;
  replayReqNotMatched: number;
  reqmatched: number;
  reqnotmatched: number;
  reqpartiallymatched: number;
  respmatched: number;
  respmatchexception: number;
  respnotmatched: number;
  resppartiallymatched: number;
  service: string;
}

export interface ITimelineData {
  collection: string;
  goldenLabel: string;
  goldenName: string;
  recordingid: string;
  replayId: string;
  results: ITimelineDataResult[];
  templateVer: string;
  testConfigName: string;
  timestamp: string; // 1
  userName: string;
}

export interface ICubeState {
  analysis: any; //2
  analysisStatus: string;
  analysisStatusObj: any; //2
  appsList: IAppDetails[];
  appsListReqErr: string;
  appsListReqStatus: string;
  collectionTemplateVersion: string | null;
  collectionUpdateOperationSetId: any; //2
  defaultRuleBook: any; //2
  diffData: any; //2
  fcId: any; //2
  gateway: any; //2
  golden: any; //2
  goldenInProg: boolean;
  goldenTimeStamp: any; //2
  graphData: ICubeGraphData[];
  graphDataReqErr: string;
  graphDataReqStatus: string;
  hideGoldenVisibilityView: boolean;
  hideHttpClient: boolean;
  hideServiceGraph: boolean;
  hideTestConfig: boolean;
  hideTestConfigSetup: boolean;
  hideTestConfigView: boolean;
  instances: IInstanceDetails[];
  jiraBugs: any[]; //2
  multiOperationsSet: any[]; //2
  newGoldenId: any; //2
  newOperationSet: any[]; //2
  newTemplateVerInfo: any; //2
  operations: any[]; //2
  pathResultsParams: any; //2
  replayId: any; //2
  replayStatus: string;
  replayStatusObj: any; //2
  fetchingReplayStatus: boolean;
  fetchingAnalysisStatus: boolean;
  report: any; //2
  ruleBook: any; //2
  selectedApp: string | null;
  selectedAppObj: IAppDetails | null;
  selectedGolden: string | null;
  selectedGoldenName: string;
  selectedInstance: string | null;
  selectedTestId: string | null;
  templateOperationSetObject: any; //2
  testConfig: ITestConfigDetails | null;
  testConfigList: ITestConfigDetails[];
  testIds: ICollectionDetails[]; //should be better renamed
  testIdsReqErr: string;
  testIdsReqStatus: string;
  timelineData: ITimelineData[];
  isGettingStartedHidden?: boolean;
}

//Golden State

export interface IRequestContract {
  params: any; //2
  body: any; //2
}
export interface IResponseContract {
  body: any; //2
}
export interface IPathFacets {
  count: number;
  val: string;
}
export interface IServiceFacets {
  count: number;
  val: string;
  path_facets: IPathFacets[];
}
export interface IGoldenState {
  fetchComplete: boolean;
  isFetching: boolean;
  message: string;
  requestContract: IRequestContract;
  requestExamples: IRequestContract;
  responseContract: IResponseContract;
  responseExamples: IResponseContract;
  selectedApi: string;
  selectedGolden: ICollectionDetails;
  selectedService: string;
}

//Http Client State

export interface ICollectionTabState {
  currentPage: number;
  numResults: number;
  count: number;
  timeStamp: number;
}

export interface IPayloadData {
  formParams?: any; //2
  hdrs?: any; //2
  method: string; //enum
  path?: string;
  pathSegments?: string[];
  payloadState?: string;
  queryParams?: any; //2
  body: any;
  status?: number;
}

export interface IQueryParams {
  [key: string]: string[];
}
export interface IKeyValuePairs<T = string> {
  [key: string]: T;
}
export interface IEventData {
  apiPath: string;
  app: string;
  collection: string;
  customerId: string;
  eventType: string;
  instanceId: string;
  metaData: IKeyValuePairs; //2
  parentSpanId: string;
  payload: [string, IPayloadData];
  recordingType: string;
  reqId: string;
  runId: string; //Could be Date
  seqId?: string;
  runType: string;
  service: string;
  spanId?: string;
  timestamp: number;
  traceId: string;
  payloadFields:[];
  grpcConnectionSchema: IGrpcConnect
}
export interface IApiTrace {
  apiPath: string;
  children: IApiTrace[];
  collectionIdAddedFromClient: string;
  id: string;
  isCubeRunHistory: boolean;
  method: string; //Should be Enum
  name: string;
  parentSpanId: string;
  queryParams: IQueryParams;
  recordingIdAddedFromClient: string;
  reqTimestamp: number;
  requestEventId: string;
  service: string;
  spanId: string;
  status: string; //Should be number
  toggled: boolean;
  traceIdAddedFromClient: string;
}

export interface ICubeRunHistory {
  // [key: Date]: IApiTrace;
  [key: string]: IApiTrace[]; //In our app key is a Date, but Date shold not be a key
}
export interface IEnvironmentConfigVars {
  key: string;
  value: string;
  id?: number;
}
export interface IEnvironmentConfig {
  id?: number;
  name: string;
  vars: IEnvironmentConfigVars;
}

export interface IHistoryTabPagingData {
  endTime: string; // 3
}
export interface IHistoryTabData {
  currentPage: number;
  oldPagesData: IHistoryTabPagingData[];
  numResults: number;
  count: number;
}

export interface IRequestParamData {
  description: string;
  id: string;
  name: string;
  selected: boolean;
  value: string;
}

export interface IGrpcData {
  [packageName: string] : {
    [serviceName: string] : {
      [method: string] : {
        data: string
      }
    }
  }
}

export interface IRecordedHistory {
  id: string;
  httpMethod: string; //enum
  httpURL: string;
  httpURLShowOnly: string;
  headers: any[]; //2
  //TODO: There are more fields in this type
}
export interface IHttpClientTabDetails {
  bodyType: string;
  collectionIdAddedFromClient: string;
  collectionNameAddedFromClient: string;
  eventData: IEventData[];
  formData: IRequestParamData[];
  multipartData: IRequestParamData[];
  headers: IRequestParamData[];
  httpMethod: string;
  httpURL: string;
  httpURLShowOnly: string;
  id: string;
  isOutgoingRequest: boolean;
  outgoingRequestIds: string[];
  outgoingRequests: IHttpClientTabDetails[];
  paramsType: string;
  queryStringParams: IRequestParamData[];
  rawData: string;
  rawDataType: string; //could be enum: json/HTML/text
  recordedHistory: IRecordedHistory | null;
  recordedResponseBody: string;
  recordedResponseHeaders: string;
  recordingIdAddedFromClient: string;
  requestId: string;
  responseBody: string;
  responsePayloadState: string;
  responseBodyType: string; //could be enum: json/HTML/text
  responseHeaders: string;
  responseStatus: string; //Could be Enum
  responseStatusText: string;
  responseTrailers: IHttpTrailers;
  service: string;
  showCompleteDiff: boolean;
  showOutgoingRequestsBtn: boolean;
  showSaveBtn: boolean;
  tabName: string;
  traceIdAddedFromClient: string;
  showTrace: boolean;
  selectedTraceTableReqTabId: string;
  requestRunning: boolean;
  abortRequest: any; //2
  selectedTraceTableTestReqTabId: string;
  currentRunId: string;
  hasChanged: boolean;
  isHighlighted: boolean;
  progressState?: string;
  contextMap: IKeyValuePairs;
  grpcData: IGrpcData;
  grpcConnectionSchema: IGrpcConnect;
  hideInternalHeaders: boolean;
}

export interface IHttpTrailers {
  [key: string]: string
}

export interface IUserApiTraceHistory {
  collection: string;
  reqTimestamp: number;
  res: IApiTrace[];
  traceId: string;
}

export interface ICollectionDetails {
  app: string;
  archived: boolean;
  branch: any; //2
  codeVersion: any; //2
  collec: string;
  collectionUpdOpSetId: any; //2
  comment: any; //2
  cust: string;
  dynamicInjectionConfigVersion: any; //2
  gitCommitId: any; //2
  id: string;
  instance: string;
  jarPath: any; //2
  label: string;
  name: string;
  prntRcrdngId: any; //2
  recordingType: string;
  rootRcrdngId: string;
  runId: string;
  status: string;
  tags: any[]; //2
  templateUpdOpSetId: any; //2
  templateVer: string;
  timestmp: number;
  userId: string;
  apiTraces: IApiTrace[];
}

export interface IServiceConfigDetails{
  service:string;
  url:string,
  isMocked:boolean
  servicePrefix: string;
}
export interface IMockConfigValue{
  name: string;
  serviceConfigs: IServiceConfigDetails[]
}
export interface IMockConfig {
  app: string;
  authenticate: boolean;
  configType: string;
  customer: string;
  id: number;
  key: string;
  service: null
  userId: string;
  value: string; //This is JSON.stringify(IMockConfigValue)
}

export interface IGrpcSchema {
  [key: string]: any
}

export interface IGrpcConnect {
  service: string;
  endpoint: string;
  method: string;
}

export interface IHttpClientStoreState {
  active: boolean;
  app: string;
  collectionTabState: ICollectionTabState;
  cubeRunHistory: ICubeRunHistory;
  envStatusIsError: boolean;
  envStatusText: string;
  environmentList: IEnvironmentConfig[];
  historyCursor: any; //2
  historyTabState: IHistoryTabData;
  isCollectionLoading: boolean;
  isHistoryLoading: boolean;
  mockConfigList: IMockConfig[];
  mockConfigStatusIsError: boolean;
  mockConfigStatusText: string;
  mockReqApiPath: string;
  mockReqServiceName: string;
  modalErrorAddMockReqMessage: string;
  selectedEnvironment: string;
  selectedMockConfig: string;
  selectedTabIdToAddMockReq: string;
  selectedTabKey: string;
  showAddMockReqModal: boolean;
  showEnvList: boolean;
  showMockConfigList: boolean;
  tabs: IHttpClientTabDetails[];
  toggleTestAndOutgoingRequests: boolean;
  userApiTraceHistory: IUserApiTraceHistory[];
  userCollectionId: string;
  userCollections: ICollectionDetails[];
  userHistoryCollection: ICollectionDetails | null;
  mockContextLookupCollection: string;
  mockContextSaveToCollection: ICollectionDetails;
  uiPref: IKeyValuePairs<any>;
  historyPathFilterText: string;
  appGrpcSchema: IGrpcSchema;
}

// Navigation State
export interface INavigationLeft {
  replayList: string[];
}

export interface INavigationState {
  footer: any; //2
  left: INavigationLeft;
  sidebar: any; //2
  top: any; //2
}

export interface IGoldenCollectionBrowseSearchResults {
  numFound: number,
  recordings: ICollectionDetails[]
}

export interface IGoldenCollectionBrowseState {
  currentCollectionType: string,
  actualGoldens: IGoldenCollectionBrowseSearchResults,
  userGoldens: IGoldenCollectionBrowseSearchResults,
  isCollectionLoading: boolean,
  messages: string[]
}

export interface IStoreState {
  cube: ICubeState;
  httpClient: IHttpClientStoreState;
  authentication: IAuthenticationState;
  navigation: INavigationState;
  golden: IGoldenState;
  gcBrowse: IGoldenCollectionBrowseState;
  apiCatalog: IApiCatalogState;
}


