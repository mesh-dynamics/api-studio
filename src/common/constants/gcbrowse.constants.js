const gcbrowseConstants = {
    REQUEST_BEGIN: "gcBrowse/REQUEST_BEGIN",
    REQUEST_SUCCESS: "gcBrowse/REQUEST_SUCCESS",
    REQUEST_FAILURE: "gcBrowse/REQUEST_FAILURE",

    LOAD_GOLDENS: "gcBrowse/LOAD_GOLDENS",
    LOAD_USER_GOLDENS: "gcBrowse/LOAD_USER_GOLDENS",

    SET_MESSAGE: "gcBrowse/SET_MESSAGE",
    CLEAR_MESSAGE: "gcBrowse/CLEAR_MESSAGE",
};

const defaultCollectionItem = {
    app: '',
    archived: false,
    branch: '',
    codeVersion: '',
    collec: '',
    collectionUpdOpSetId: '',
    comment: '',
    cust: '',
    dynamicInjectionConfigVersion: '',
    gitCommitId: '',
    id: '',
    instance: '',
    jarPath: '',
    label: '',
    name: '',
    prntRcrdngId: '',
    recordingType: '',
    rootRcrdngId: '',
    runId: '',
    status: '',
    tags: [],
    templateUpdOpSetId: '',
    templateVer: '',
    timestmp: 0,
    userId: '',
    apiTraces: []
};

export {
    gcbrowseConstants,
    defaultCollectionItem
}